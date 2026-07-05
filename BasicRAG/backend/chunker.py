"""PDF parsing and text chunking utilities."""

import re
from dataclasses import dataclass, field
from pathlib import Path

from pypdf import PdfReader

from config import CHUNK_SIZE, CHUNK_OVERLAP


@dataclass
class Chunk:
    text: str
    source_file: str
    page_number: int
    chunk_index: int

    @property
    def metadata(self) -> dict:
        return {
            "source": self.source_file,
            "page": self.page_number,
            "chunk_index": self.chunk_index,
        }


def parse_pdf(file_path: str | Path) -> list[tuple[int, str]]:
    """Read a PDF and return list of (page_number, page_text) tuples."""
    reader = PdfReader(str(file_path))
    pages = []
    for i, page in enumerate(reader.pages, start=1):
        text = page.extract_text()
        if text and text.strip():
            pages.append((i, text.strip()))
    return pages


def split_into_chunks(text: str, chunk_size: int = CHUNK_SIZE, overlap: int = CHUNK_OVERLAP) -> list[str]:
    """Split text into overlapping chunks of approximately chunk_size characters.

    Splits on sentence boundaries when possible for more natural chunks.
    """
    if not text:
        return []

    # Try splitting by sentences first
    sentences = re.split(r"(?<=[.!?])\s+", text)
    chunks = []
    current_chunk = []

    for sentence in sentences:
        current_chunk.append(sentence)
        current_text = " ".join(current_chunk)

        if len(current_text) >= chunk_size:
            chunks.append(current_text)
            # Keep last few sentences for overlap
            overlap_text = ""
            kept = []
            for s in reversed(current_chunk):
                candidate = " ".join([s] + (kept[::-1] if kept else []))
                if len(candidate) < overlap:
                    kept.insert(0, s)
                else:
                    break
            current_chunk = kept
            # If we consumed all, rebuild from last sentence
            if not current_chunk:
                current_chunk = [sentences[-1]] if sentences else []

    # Add remaining text
    if current_chunk:
        remaining = " ".join(current_chunk)
        if remaining.strip():
            chunks.append(remaining)

    return chunks if chunks else [text]


def chunk_pdf(file_path: str | Path) -> list[Chunk]:
    """Parse a PDF and return a list of Chunk objects."""
    file_path = Path(file_path)
    pages = parse_pdf(file_path)
    chunks = []

    for page_number, page_text in pages:
        page_chunks = split_into_chunks(page_text)
        for idx, chunk_text in enumerate(page_chunks):
            chunks.append(
                Chunk(
                    text=chunk_text,
                    source_file=file_path.name,
                    page_number=page_number,
                    chunk_index=idx,
                )
            )

    return chunks