import os
import requests
from deepeval.models import DeepEvalBaseLLM


class OpenRouterJudge(DeepEvalBaseLLM):
    """DeepEval LLM wrapper that routes judge calls through OpenRouter."""

    def __init__(self):
        self.model_name = os.environ["DEEPEVAL_JUDGE_MODEL"]
        self.api_key = os.environ["OPENROUTER_API_KEY"]
        self.base_url = os.environ.get(
            "OPENROUTER_BASE_URL", "https://openrouter.ai/api/v1"
        )

    def load_model(self):
        return self.model_name

    def generate(self, prompt: str) -> str:
        resp = requests.post(
            f"{self.base_url}/chat/completions",
            headers={"Authorization": f"Bearer {self.api_key}"},
            json={
                "model": self.model_name,
                "messages": [{"role": "user", "content": prompt}],
                "temperature": 0,
            },
            timeout=120,
        )
        resp.raise_for_status()
        return resp.json()["choices"][0]["message"]["content"]

    async def a_generate(self, prompt: str) -> str:
        return self.generate(prompt)

    def get_model_name(self):
        return self.model_name
