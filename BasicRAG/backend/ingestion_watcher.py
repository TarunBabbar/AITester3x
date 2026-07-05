"""Watchdog-based PDF folder monitor for automatic ingestion."""

import logging
import time
from pathlib import Path

from watchdog.events import FileSystemEventHandler
from watchdog.observers import Observer

from chunker import chunk_pdf
from config import PDF_DIR
from embedder import Embedder
from vector_store import VectorStore

logger = logging.getLogger(__name__)


class PdfHandler(FileSystemEventHandler):
    """Handles PDF file creation events and triggers ingestion."""

    def __init__(self, vector_store: VectorStore, embedder: Embedder):
        self.vector_store = vector_store
        self.embedder = embedder
        self._processing = set()  # avoid double-processing

    def on_created(self, event):
        if not event.is_directory and event.src_path.lower().endswith(".pdf"):
            self._ingest(event.src_path)

    def on_moved(self, event):
        if not event.is_directory and event.dest_path.lower().endswith(".pdf"):
            self._ingest(event.dest_path)

    def _ingest(self, file_path: str):
        path = Path(file_path)
        if not path.exists() or path.name in self._processing:
            return

        logger.info(f"📄 New PDF detected: {path.name}")
        self._processing.add(path.name)

        try:
            # Small delay to ensure file is fully written
            time.sleep(1)

            chunks = chunk_pdf(path)
            if not chunks:
                logger.warning(f"No text extracted from {path.name}")
                return

            logger.info(f"  → {len(chunks)} chunks extracted")

            texts = [c.text for c in chunks]
            metadatas = [c.metadata for c in chunks]
            ids = [f"{path.stem}_{c.page_number}_{c.chunk_index}" for c in chunks]

            embeddings = self.embedder.embed_batch(texts)
            self.vector_store.add_chunks(ids, embeddings, texts, metadatas)

            total = self.vector_store.count()
            logger.info(f"  ✅ Ingested {len(chunks)} chunks from '{path.name}' (total: {total})")
        except Exception as e:
            logger.error(f"  ❌ Failed to ingest {path.name}: {e}")
        finally:
            self._processing.discard(path.name)


class IngestionWatcher:
    """Watches the PDF folder and ingests new documents automatically."""

    def __init__(self, vector_store: VectorStore, embedder: Embedder):
        self.observer = Observer()
        self.handler = PdfHandler(vector_store, embedder)

    def start(self):
        self.observer.schedule(self.handler, str(PDF_DIR), recursive=False)
        self.observer.start()
        logger.info(f"👀 Watching {PDF_DIR} for new PDF files...")

    def stop(self):
        self.observer.stop()
        self.observer.join()
        logger.info("🛑 Ingestion watcher stopped.")


def ingest_existing_pdfs(vector_store: VectorStore, embedder: Embedder) -> int:
    """Ingest all PDFs already present in the data/pdf folder. Returns count of ingested PDFs."""
    pdf_files = sorted(Path(PDF_DIR).glob("*.pdf"))
    if not pdf_files:
        return 0

    total_chunks = 0
    for pdf_path in pdf_files:
        logger.info(f"📄 Ingesting existing PDF: {pdf_path.name}")
        try:
            chunks = chunk_pdf(pdf_path)
            if not chunks:
                continue

            texts = [c.text for c in chunks]
            metadatas = [c.metadata for c in chunks]
            ids = [f"{pdf_path.stem}_{c.page_number}_{c.chunk_index}" for c in chunks]

            embeddings = embedder.embed_batch(texts)
            vector_store.add_chunks(ids, embeddings, texts, metadatas)
            total_chunks += len(chunks)
            logger.info(f"  → {len(chunks)} chunks ingested from '{pdf_path.name}'")
        except Exception as e:
            logger.error(f"  ❌ Failed to ingest {pdf_path.name}: {e}")

    total = vector_store.count()
    logger.info(f"✅ Ingestion complete. Total chunks in store: {total}")
    return total_chunks