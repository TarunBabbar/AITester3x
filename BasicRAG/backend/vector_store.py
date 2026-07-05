"""ChromaDB vector store wrapper."""

import chromadb
from chromadb.config import Settings
from chromadb.errors import NotFoundError

from config import CHROMA_DIR, CHROMA_COLLECTION_NAME, TOP_K


class VectorStore:
    """Local ChromaDB instance for storing document chunks and their embeddings."""

    def __init__(self):
        self._client = chromadb.PersistentClient(
            path=str(CHROMA_DIR),
            settings=Settings(anonymized_telemetry=False),
        )
        self._collection = None

    @property
    def collection(self):
        if self._collection is None:
            try:
                self._collection = self._client.get_collection(CHROMA_COLLECTION_NAME)
            except (ValueError, NotFoundError):
                self._collection = self._client.create_collection(CHROMA_COLLECTION_NAME)
        return self._collection

    def add_chunks(
        self,
        ids: list[str],
        embeddings: list[list[float]],
        texts: list[str],
        metadatas: list[dict],
    ) -> None:
        """Add chunks with embeddings to the vector store."""
        self.collection.add(
            ids=ids,
            embeddings=embeddings,
            documents=texts,
            metadatas=metadatas,
        )

    def search(self, query_embedding: list[float], k: int = TOP_K) -> list[dict]:
        """Search for the top-k most similar chunks to a query embedding."""
        results = self.collection.query(
            query_embeddings=[query_embedding],
            n_results=k,
            include=["documents", "metadatas", "distances"],
        )

        if not results["ids"] or not results["ids"][0]:
            return []

        retrieved = []
        for i in range(len(results["ids"][0])):
            retrieved.append(
                {
                    "id": results["ids"][0][i],
                    "text": results["documents"][0][i],
                    "metadata": results["metadatas"][0][i],
                    "score": round(1.0 - results["distances"][0][i], 4),  # cosine similarity
                }
            )

        return retrieved

    def count(self) -> int:
        """Return the total number of chunks in the collection."""
        return self.collection.count()

    def get_all_sources(self) -> list[str]:
        """Return unique source filenames in the store."""
        results = self.collection.get(include=["metadatas"])
        if not results or not results["metadatas"]:
            return []
        sources = set()
        for m in results["metadatas"]:
            if "source" in m:
                sources.add(m["source"])
        return sorted(sources)

    def delete_collection(self) -> None:
        """Delete and recreate the collection."""
        try:
            self._client.delete_collection(CHROMA_COLLECTION_NAME)
        except ValueError:
            pass
        self._collection = None