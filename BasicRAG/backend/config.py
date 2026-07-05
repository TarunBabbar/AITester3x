"""Configuration for the RAG Explorer backend."""

import os
from pathlib import Path

# Paths
BASE_DIR = Path(__file__).parent.parent
DATA_DIR = BASE_DIR / "data"
PDF_DIR = DATA_DIR / "pdf"
CHROMA_DIR = DATA_DIR / "chroma_db"

# Ensure directories exist
PDF_DIR.mkdir(parents=True, exist_ok=True)
CHROMA_DIR.mkdir(parents=True, exist_ok=True)

# Embedding model
EMBEDDING_MODEL_NAME = "nomic-embed-text-v1.5"

# Chunking config
CHUNK_SIZE = 512
CHUNK_OVERLAP = 64

# Retrieval config
TOP_K = 4

# ChromaDB
CHROMA_COLLECTION_NAME = "rag_documents"

# Groq
GROQ_API_KEY = os.environ.get("GROQ_API_KEY", "")
GROQ_MODEL = "llama-3.1-8b-instant"  # Fast, high-quality model on Groq
# Alternatives: "mixtral-8x7b-32768", "gemma2-9b-it", "llama-3.3-70b-versatile"

# Server
HOST = "0.0.0.0"
PORT = 8000