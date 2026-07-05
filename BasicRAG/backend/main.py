"""RAG Explorer — FastAPI backend."""

import logging
import threading
from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

from config import GROQ_API_KEY, PDF_DIR, TOP_K
from embedder import Embedder
from ingestion_watcher import IngestionWatcher, ingest_existing_pdfs
from llm_service import LLMService
from vector_store import VectorStore

# ── Logging ──────────────────────────────────────────────────────────────
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger("rag-explorer")

# ── Globals ──────────────────────────────────────────────────────────────
vector_store = VectorStore()
embedder = Embedder()
llm_service = LLMService()
watcher: IngestionWatcher | None = None


# ── Lifespan ─────────────────────────────────────────────────────────────
@asynccontextmanager
async def lifespan(app: FastAPI):
    global watcher

    logger.info("🚀 RAG Explorer starting up...")

    # Ingest any existing PDFs
    logger.info("Checking for existing PDFs...")
    ingest_existing_pdfs(vector_store, embedder)

    # Start file watcher
    watcher = IngestionWatcher(vector_store, embedder)
    t = threading.Thread(target=watcher.start, daemon=True)
    t.start()

    yield

    # Shutdown
    if watcher:
        watcher.stop()
    logger.info("👋 RAG Explorer shut down.")


# ── App ──────────────────────────────────────────────────────────────────
app = FastAPI(
    title="RAG Explorer API",
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# ── Schemas ──────────────────────────────────────────────────────────────
class QueryRequest(BaseModel):
    query: str


class QueryResponse(BaseModel):
    query: str
    query_embedding: list[float] | None = None
    retrieved_chunks: list[dict]
    final_prompt: str | None = None
    answer: str
    total_chunks_in_store: int


class StatsResponse(BaseModel):
    total_chunks: int
    sources: list[str]


# ── Routes ───────────────────────────────────────────────────────────────
@app.get("/api/health")
async def health():
    return {"status": "ok"}


@app.get("/api/stats", response_model=StatsResponse)
async def stats():
    return StatsResponse(
        total_chunks=vector_store.count(),
        sources=vector_store.get_all_sources(),
    )


@app.post("/api/query", response_model=QueryResponse)
async def query(request: QueryRequest):
    if not request.query.strip():
        raise HTTPException(status_code=400, detail="Query cannot be empty")

    count = vector_store.count()
    if count == 0:
        return QueryResponse(
            query=request.query,
            retrieved_chunks=[],
            answer="No documents have been ingested yet. Please add PDF files to the data/pdf folder.",
            total_chunks_in_store=0,
        )

    # 1. Embed the query
    query_emb = embedder.embed(request.query)

    # 2. Semantic search
    chunks = vector_store.search(query_emb, k=TOP_K)

    # 3. Build prompt
    prompt = llm_service.build_prompt(request.query, chunks)

    # 4. Generate answer
    answer = llm_service.generate(prompt)

    return QueryResponse(
        query=request.query,
        query_embedding=query_emb[:8],  # first 8 dims for preview
        retrieved_chunks=chunks,
        final_prompt=prompt,
        answer=answer,
        total_chunks_in_store=count,
    )


@app.post("/api/reindex")
async def reindex():
    """Delete and re-ingest all PDFs."""
    vector_store.delete_collection()
    total = ingest_existing_pdfs(vector_store, embedder)
    return {"message": f"Re-indexed {total} chunks from existing PDFs."}


# ── Entrypoint ───────────────────────────────────────────────────────────
if __name__ == "__main__":
    import uvicorn
    from config import HOST, PORT

    uvicorn.run("main:app", host=HOST, port=PORT, reload=True)