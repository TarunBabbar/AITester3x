"""Embedding generation using sentence-transformers with Nomic Embed."""

import numpy as np
from sentence_transformers import SentenceTransformer

from config import EMBEDDING_MODEL_NAME


class Embedder:
    """Generates embeddings using a local sentence-transformers model."""

    def __init__(self):
        self._model: SentenceTransformer | None = None

    @property
    def model(self) -> SentenceTransformer:
        if self._model is None:
            self._model = SentenceTransformer(EMBEDDING_MODEL_NAME, trust_remote_code=True)
        return self._model

    def embed(self, text: str) -> list[float]:
        """Generate an embedding vector for a single text string."""
        emb = self.model.encode(text, normalize_embeddings=True)
        return emb.tolist()

    def embed_batch(self, texts: list[str]) -> list[list[float]]:
        """Generate embeddings for a batch of texts."""
        embs = self.model.encode(texts, normalize_embeddings=True, show_progress_bar=False)
        return [e.tolist() for e in embs]

    @property
    def dimension(self) -> int:
        """Return the embedding dimension."""
        return self.model.get_sentence_embedding_dimension()