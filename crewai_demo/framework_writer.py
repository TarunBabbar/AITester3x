"""
Framework Writer

Takes the raw text produced by the Framework Agent (Agent 4) and saves the
individual files to disk. The agent is instructed to prefix each code block
with a filename line:

    // FILE: package.json
    ```json
    { ... }
    ```

This module parses those blocks, validates the filenames (no path
traversal), writes them under output/, and reports what was written.
"""

import re
from pathlib import Path
from typing import Dict, List, Tuple

# Where the generated framework lands. gitignored.
OUTPUT_DIR = Path(__file__).resolve().parent / "output"


# ---------------------------------------------------------------------------
# Parsing
# ---------------------------------------------------------------------------
FILE_MARKER = re.compile(r"^\s*//\s*FILE:\s*(.+?)\s*$", re.IGNORECASE)


def parse_files(raw: str) -> List[Tuple[str, str]]:
    """Extract (filename, content) pairs from the agent's markdown output.

    The model can place the marker in two ways; both are handled:

        // FILE: package.json          <-- marker BEFORE the fence
        ```json
        { "name": "sample" }
        ```

        ```typescript                  <-- marker INSIDE the fence
        // FILE: package.json
        import { test } from '@playwright/test';
        ```

    Fences toggle on any ``` line; a marker inside a fence names the
    current block, and a marker outside names the next one.
    """
    files: List[Tuple[str, str]] = []
    current_name: str | None = None
    in_fence = False
    buf: List[str] = []

    for line in raw.splitlines():
        stripped = line.strip()

        if not in_fence:
            marker = FILE_MARKER.match(line)
            if marker:
                current_name = marker.group(1).strip().strip("`").strip()
                continue
            if stripped.startswith("```"):
                in_fence = True
                buf = []
                continue
        else:
            if stripped.startswith("```"):
                files.append((current_name or "", "\n".join(buf).strip()))
                current_name = None
                in_fence = False
                buf = []
            else:
                marker = FILE_MARKER.match(line)
                if marker:
                    current_name = marker.group(1).strip().strip("`").strip()
                else:
                    buf.append(line)

    return files


# ---------------------------------------------------------------------------
# Writing
# ---------------------------------------------------------------------------
def _safe_name(root: Path, name: str) -> bool:
    """Reject absolute paths and anything that escapes the root dir."""
    if not name:
        return False
    candidate = (root / name).resolve()
    return candidate.is_relative_to(root.resolve())


def write_framework(raw: str, run_id: str = "default") -> Dict[str, object]:
    """Write all parseable files from raw agent output into output/<run_id>/."""
    files = parse_files(raw)
    written: List[str] = []
    skipped: List[str] = []

    run_dir = OUTPUT_DIR / run_id
    run_dir.mkdir(parents=True, exist_ok=True)

    for name, content in files:
        if not _safe_name(run_dir, name):
            skipped.append(name)
            continue
        target = (run_dir / name).resolve()
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(content, encoding="utf-8")
        written.append(name)

    return {
        "written": written,
        "skipped": skipped,
        "output_dir": str(run_dir),
        "total_files": len(files),
    }


if __name__ == "__main__":
    # Smoke test with a hand-written sample.
    sample = """// FILE: package.json
```json
{ "name": "sample", "version": "1.0.0" }
```
// FILE: tests/example.spec.ts
```typescript
import { test } from '@playwright/test';
```
"""
    print(write_framework(sample))
