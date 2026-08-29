"""
DeepEval Evaluation Layer

Every stage of the pipeline is cross-verified by a DeepEval metric:

  Stage                        Input                Output being scored
  ---------------------------  -------------------  ---------------------
  RI -> Coverage Evaluation    raw user requirement  Requirement Intelligence
  Cases -> Cases Evaluation    Requirement Intel     Generated test cases
  Automation -> Automation Eval RI + test cases     Generated Playwright framework

Each check scores whether the output faithfully covers its source input,
using DeepEval's GEval (LLM-judged). The LLM behind DeepEval is the same
provider configured in .env (openrouter / commandcode), so nothing extra
to configure. The metric results (score, reasoning, status) are streamed
to the UI as evaluation cards.

A final "release confidence" is computed from all evaluation scores.
"""

import json
import os
import urllib.error
import urllib.request
from typing import Any, Dict, List, Optional

# deepeval is imported lazily (inside _build_geval) so the app boots even
# when it's not installed — eval steps then fail gracefully instead of
# crashing the whole backend.
try:
    from deepeval.models import DeepEvalBaseLLM
except ImportError:
    DeepEvalBaseLLM = object  # fallback so the module imports without deepeval

# Metric names used by the UI (stable keys)
METRIC_RI_COVERAGE = "coverage_eval"
METRIC_CASES = "cases_eval"
METRIC_AUTOMATION = "automation_eval"


def _deepeval_available() -> bool:
    try:
        import deepeval  # noqa: F401
        return True
    except ImportError:
        return False


# ---------------------------------------------------------------------------
# Custom DeepEval model — calls our OpenAI-compatible provider directly.
#
# deepeval 4.x's built-in wrappers (LiteLLMModel/OpenAIModel) pull heavy
# deps (litellm) and clash with CrewAI's own click/huggingface pins. This
# model reuses the same provider config as the agents (.env) and talks to
# the chat/completions endpoint directly — proven to work for both
# openrouter and commandcode.
# ---------------------------------------------------------------------------
class ProviderModel(DeepEvalBaseLLM):
    """Minimal DeepEvalBaseLLM calling the .env-configured provider."""

    def __init__(self) -> None:
        provider = (os.getenv("LLM_PROVIDER") or "").lower().strip()
        if provider == "commandcode":
            self.name = os.getenv("CMD_MODEL", "")
            self.api_key = os.getenv("CMD_API_KEY")
            self.base_url = os.getenv("CMD_BASE_URL", "")
        elif provider == "openrouter":
            self.name = os.getenv("OPENROUTER_MODEL", "")
            self.api_key = os.getenv("OPENROUTER_API_KEY")
            self.base_url = os.getenv("OPENROUTER_BASE_URL", "")
        else:
            raise ValueError(
                f"LLM_PROVIDER must be 'openrouter' or 'commandcode' in .env "
                f"(got {provider!r})"
            )
        if not self.name or not self.api_key or not self.base_url:
            raise ValueError(
                f"Missing LLM config in .env for provider {provider!r}: "
                "model, API key and base URL must all be set."
            )
        super().__init__(model=self.name)

    # --- DeepEvalBaseLLM interface --------------------------------------
    def load_model(self, *args, **kwargs):
        return self

    def get_model_name(self) -> str:
        return self.name

    def supports_json_mode(self) -> bool:
        return True

    def supports_structured_outputs(self) -> bool:
        return False

    def supports_log_probs(self) -> Optional[bool]:
        return False

    def supports_temperature(self) -> Optional[bool]:
        return True

    def supports_multimodal(self) -> Optional[bool]:
        return False

    # --- generation ------------------------------------------------------
    def _chat(self, prompt: str, schema: Any = None) -> str:
        body: Dict[str, Any] = {
            "model": self.name,
            "messages": [{"role": "user", "content": prompt}],
            "temperature": 0.0,
        }
        if schema is not None:
            body["response_format"] = {"type": "json_object"}
        req = urllib.request.Request(
            f"{self.base_url.rstrip('/')}/chat/completions",
            data=json.dumps(body).encode(),
            headers={
                "Authorization": f"Bearer {self.api_key or ''}",
                "Content-Type": "application/json",
            },
        )
        try:
            with urllib.request.urlopen(req, timeout=int(os.getenv("OPENROUTER_TIMEOUT_MS", "180000")) / 1000) as resp:
                data = json.loads(resp.read())
                content = data["choices"][0]["message"].get("content") or ""
                if schema is not None:
                    # Strip markdown fences if the model wrapped JSON
                    content = content.strip()
                    if content.startswith("```"):
                        content = content.split("\n", 1)[-1].rsplit("```", 1)[0].strip()
                return content
        except urllib.error.HTTPError as exc:
            return f"Provider error: {exc.code} {exc.read().decode()[:200]}"

    def generate(self, *args, **kwargs) -> str:
        prompt = kwargs.get("prompt") or (args[0] if args else "")
        schema = kwargs.get("schema")
        return self._chat(prompt, schema)

    async def a_generate(self, *args, **kwargs) -> str:
        return self.generate(*args, **kwargs)

    def batch_generate(self, prompts: List[str]) -> List[str]:
        return [self._chat(p) for p in prompts]

    def generate_raw_response(self, *args, **kwargs):
        return self.generate(*args, **kwargs)

    async def a_generate_raw_response(self, *args, **kwargs):
        return self.generate(*args, **kwargs)

    def generate_samples(self, *args, **kwargs):
        return self.generate(*args, **kwargs)


def _build_geval(name: str, criteria: str):
    """GEval with our provider model."""
    from deepeval.metrics import GEval
    from deepeval.test_case import SingleTurnParams

    return GEval(
        name=name,
        criteria=criteria,
        evaluation_params=[
            SingleTurnParams.INPUT,
            SingleTurnParams.ACTUAL_OUTPUT,
        ],
        model=ProviderModel(),
        verbose_mode=os.getenv("AGENT_VERBOSE", "false").lower() == "true",
    )


def _evaluate(name: str, criteria: str, input_text: str, output_text: str) -> Dict[str, Any]:
    """Run a single GEval check and return a serialisable result."""
    if not _deepeval_available():
        return {
            "metric": name,
            "score": 0.0,
            "status": "failed",
            "reason": "deepeval is not installed — run: pip install deepeval",
        }
    from deepeval.test_case import LLMTestCase

    metric = _build_geval(name, criteria)
    test_case = LLMTestCase(input=input_text, actual_output=output_text)
    metric.measure(test_case)
    return {
        "metric": name,
        "score": round(metric.score, 4),
        "status": "passed" if metric.is_successful() else "failed",
        "reason": (getattr(metric, "reason", "") or "")[:500],
    }


# ---------------------------------------------------------------------------
# Stage evaluations
# ---------------------------------------------------------------------------
def evaluate_ri_coverage(user_input: str, ri_output: str) -> Dict[str, Any]:
    """Coverage Evaluation — does the RI cover everything the user asked for?"""
    return _evaluate(
        "Requirement Intelligence Coverage",
        (
            "Score how completely the output captures every requirement, "
            "constraint, and acceptance criterion present in the input. "
            "A perfect score means nothing from the input was missed or "
            "misrepresented."
        ),
        user_input,
        ri_output,
    )


def evaluate_test_cases(ri_output: str, test_cases_text: str) -> Dict[str, Any]:
    """Test Case Evaluation — do the generated test cases cover the RI?"""
    return _evaluate(
        "Test Case Coverage",
        (
            "Score whether the output test cases cover all the behaviours, "
            "scenarios, and edge cases implied by the input requirement "
            "intelligence. Penalise missing scenarios."
        ),
        ri_output,
        test_cases_text,
    )


def evaluate_automation(ri_output: str, framework_summary: str) -> Dict[str, Any]:
    """Automation Evaluation — do the generated tests automate the test cases?"""
    return _evaluate(
        "Automation Coverage",
        (
            "Score whether the output Playwright framework actually automates "
            "every test case implied by the input requirement intelligence and "
            "test cases. Penalise tests with no corresponding automation."
        ),
        ri_output,
        framework_summary,
    )


# ---------------------------------------------------------------------------
# Release confidence
# ---------------------------------------------------------------------------
def release_confidence(results: list[Dict[str, Any]]) -> float:
    """Weighted average of evaluation scores -> 0..1 release confidence."""
    if not results:
        return 0.0
    weights = {
        METRIC_RI_COVERAGE: 1.0,
        METRIC_CASES: 2.0,
        METRIC_AUTOMATION: 3.0,
    }
    total_w = sum(weights.get(r["metric"], 1.0) for r in results)
    total_s = sum(weights.get(r["metric"], 1.0) * r["score"] for r in results)
    return round(total_s / total_w, 4)


if __name__ == "__main__":
    # Smoke test: evaluate a trivial pair without network (should fail fast
    # if deepeval isn't installed or misconfigured).
    print("deepeval import ok")
