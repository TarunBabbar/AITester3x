"""LLM query service using Groq."""

import logging

from groq import Groq

from config import GROQ_API_KEY, GROQ_MODEL

logger = logging.getLogger(__name__)


class LLMService:
    """Handles communication with Groq's LLM API."""

    def __init__(self):
        if not GROQ_API_KEY:
            logger.warning("GROQ_API_KEY not set. LLM responses will be mocked.")
            self._client = None
        else:
            self._client = Groq(api_key=GROQ_API_KEY)

    def build_prompt(self, query: str, chunks: list[dict]) -> str:
        """Build the full prompt with context chunks for the LLM."""
        context_parts = []
        for i, chunk in enumerate(chunks, start=1):
            meta = chunk["metadata"]
            context_parts.append(
                f"[Source: {meta.get('source', 'unknown')}, Page {meta.get('page', '?')}]\n{chunk['text']}"
            )

        context = "\n\n---\n\n".join(context_parts)

        prompt = f"""You are a helpful assistant that answers questions based on the provided context.

CONTEXT:
{context}

USER QUESTION:
{query}

INSTRUCTIONS:
- Answer the question using ONLY the information provided in the context above.
- If the context does not contain enough information to answer, say so clearly.
- Cite the source document and page number when referencing specific information.
- Be concise and accurate.

ANSWER:"""
        return prompt

    def generate(self, prompt: str) -> str:
        """Send prompt to Groq and return the generated response."""
        if self._client is None:
            return (
                "[Groq API key not configured. Set the GROQ_API_KEY environment variable "
                "to get real LLM responses.]"
            )

        try:
            completion = self._client.chat.completions.create(
                model=GROQ_MODEL,
                messages=[{"role": "user", "content": prompt}],
                temperature=0.3,
                max_tokens=1024,
            )
            return completion.choices[0].message.content.strip()
        except Exception as e:
            logger.error(f"Groq API error: {e}")
            return f"[Error generating response: {e}]"