"""
Program 8: Word Counter

Analyzes a text file: total words, unique words, character count, and the
top most-frequent words. Creates a demo file if none is given.

Concepts: collections.Counter, string punctuation, file reading, sorting.
"""

import re
import sys
from collections import Counter
from pathlib import Path

DEMO_FILE = Path(__file__).with_name("sample_text.txt")


def make_demo_file(path: Path = DEMO_FILE) -> None:
    """Create a small sample text file if it doesn't exist."""
    if path.exists():
        return
    text = (
        "Python is fun. Python is powerful. "
        "Learning Python opens doors. "
        "Practice makes perfect, and practice builds skill. "
        "Python, Python, Python!\n"
    )
    path.write_text(text, encoding="utf-8")
    print(f"Created demo file: {path.name}")


def analyze(path: Path) -> None:
    text = path.read_text(encoding="utf-8")

    words = re.findall(r"[A-Za-z']+", text.lower())
    total = len(words)
    unique = len(set(words))
    chars = len(text)

    print(f"File      : {path.name}")
    print(f"Characters: {chars}")
    print(f"Words     : {total}")
    print(f"Unique    : {unique}")
    print(f"Avg length: {sum(len(w) for w in words) / total:.1f}" if total else "Avg length: 0")

    print("\nMost frequent words:")
    for word, count in Counter(words).most_common(5):
        print(f"  {word:<12} {count}")


def main() -> None:
    path = Path(sys.argv[1]) if len(sys.argv) > 1 else DEMO_FILE
    if not path.exists() and path == DEMO_FILE:
        make_demo_file()
    if not path.exists():
        print(f"File not found: {path}")
        return
    analyze(path)


if __name__ == "__main__":
    main()
