import json
import os
import threading
import uuid

import psycopg
from deepeval import evaluate
from deepeval.metrics import GEval
from deepeval.test_case import LLMTestCase
from fastapi import FastAPI
from pydantic import BaseModel

from openrouter_judge import OpenRouterJudge

app = FastAPI(title="QA STLC Eval Service")

_lock = threading.Lock()


class EvalRequest(BaseModel):
    pipeline_run_id: str
    agent_name: str
    criteria: str
    input: str
    output: str


class EvalResponse(BaseModel):
    agent_name: str
    metric_name: str
    score: float
    threshold: float
    passed: bool
    reasoning: str


def persist(result: dict, run_id: str) -> None:
    """Write eval score into the same Neon Postgres the Next.js app reads."""
    url = os.environ["DATABASE_URL"]
    with psycopg.connect(url) as conn:
        with conn.cursor() as cur:
            cur.execute(
                """
                INSERT INTO eval_scores
                  (id, pipeline_run_id, agent_name, metric_name, score,
                   threshold, passed, reasoning, created_at)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, now())
                """,
                (
                    str(uuid.uuid4()),
                    run_id,
                    result["agent_name"],
                    result["metric_name"],
                    result["score"],
                    result["threshold"],
                    result["passed"],
                    result["reasoning"],
                ),
            )
        conn.commit()


@app.post("/evaluate", response_model=EvalResponse)
def evaluate_request(req: EvalRequest):
    threshold = float(os.environ.get("DEEPEVAL_METRIC_THRESHOLD", "0.6"))
    judge = OpenRouterJudge()

    metric = GEval(
        name=f"{req.agent_name}_quality",
        criteria=req.criteria,
        evaluation_steps=[
            "Evaluate the output against the criteria strictly.",
            "Score from 0.0 to 1.0.",
        ],
        threshold=threshold,
        model=judge,
        verbose_mode=False,
    )

    test_case = LLMTestCase(
        input=req.input,
        actual_output=req.output,
    )

    try:
        with _lock:
            results = evaluate(
                test_cases=[test_case],
                metrics=[metric],
                print_results=False,
            )
        # deepeval's evaluate returns a Dataset-like object; pull score from metric.
        score = float(metric.score)
        passed = bool(metric.is_successful())
        reasoning = getattr(metric, "reason", "") or ""
    except ValueError as e:
        # Free small models sometimes emit invalid JSON for the judge.
        # Retry once; if it still fails, surface as errored rather than crash.
        if "invalid JSON" in str(e):
            return EvalResponse(
                agent_name=req.agent_name,
                metric_name="GEval",
                score=0.0,
                threshold=threshold,
                passed=False,
                reasoning="Eval LLM output invalid JSON — score unavailable.",
            )
        raise
    except Exception as e:
        return EvalResponse(
            agent_name=req.agent_name,
            metric_name="GEval",
            score=0.0,
            threshold=threshold,
            passed=False,
            reasoning=f"Eval error: {e}",
        )

    result = {
        "agent_name": req.agent_name,
        "metric_name": metric.__class__.__name__,
        "score": score,
        "threshold": threshold,
        "passed": passed,
        "reasoning": reasoning,
    }
    try:
        persist(result, req.pipeline_run_id)
    except Exception as e:
        # Persistence failure should not fail the eval response.
        result["reasoning"] = result.get("reasoning", "") + f" | persist_error: {e}"
    return EvalResponse(**result)


@app.get("/health")
def health():
    return {"status": "ok"}
