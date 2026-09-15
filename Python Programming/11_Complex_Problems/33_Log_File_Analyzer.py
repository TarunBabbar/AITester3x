"""
Program 33: Log File Analyzer

Writes a small sample application log to a temporary file, then parses it:
counts lines per level (INFO, WARN, ERROR), ranks the most common messages,
summarises the sources and reports the time span covered.

Concepts: file I/O, regex named groups, collections.Counter, pathlib, tempfile.
"""

import re
import tempfile
from collections import Counter
from pathlib import Path

LOG_LINE = re.compile(
    r"^(?P<time>\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})\s+"
    r"(?P<level>INFO|WARN|ERROR)\s+"
    r"\[(?P<source>[^\]]+)\]\s+"
    r"(?P<message>.+)$"
)

SAMPLE_LOG = """\
2026-09-01 09:00:01 INFO  [auth] user alice logged in
2026-09-01 09:00:05 INFO  [api] GET /users 200
2026-09-01 09:00:07 WARN  [api] GET /users took 1200ms
2026-09-01 09:00:09 ERROR [db] connection timeout
2026-09-01 09:00:11 INFO  [api] POST /orders 201
2026-09-01 09:00:15 ERROR [db] connection timeout
2026-09-01 09:00:18 WARN  [cache] cache miss for key user:42
2026-09-01 09:00:21 INFO  [auth] user bob logged in
2026-09-01 09:00:24 ERROR [api] POST /orders 500
2026-09-01 09:00:30 WARN  [api] GET /users took 2100ms
this line is malformed and should be counted
2026-09-01 09:00:33 INFO  [api] GET /health 200
"""


def parse_log(text: str) -> tuple[list[dict], int]:
    """Return (parsed entries, number of unparseable lines)."""
    entries: list[dict] = []
    malformed = 0
    for line in text.splitlines():
        if not line.strip():
            continue
        match = LOG_LINE.match(line.strip())
        if match:
            entries.append(match.groupdict())
        else:
            malformed += 1
    return entries, malformed


def analyze(entries: list[dict], malformed: int) -> dict:
    """Summarise parsed log entries."""
    times = sorted(entry["time"] for entry in entries)
    return {
        "total": len(entries),
        "malformed": malformed,
        "levels": Counter(entry["level"] for entry in entries),
        "sources": Counter(entry["source"] for entry in entries),
        "messages": Counter(entry["message"] for entry in entries),
        "span": (times[0], times[-1]) if times else (None, None),
    }


def report(summary: dict) -> None:
    print(f"Parsed {summary['total']} lines ({summary['malformed']} malformed)")
    print(f"Time span: {summary['span'][0]} -> {summary['span'][1]}")

    print("\nLines per level:")
    for level in ("INFO", "WARN", "ERROR"):
        print(f"  {level:<6} {summary['levels'][level]}")

    print("\nSources:")
    for source, count in summary["sources"].most_common():
        print(f"  {source:<8} {count}")

    print("\nMost common messages:")
    for message, count in summary["messages"].most_common(3):
        print(f"  {count}x  {message}")


def main() -> None:
    with tempfile.TemporaryDirectory() as folder:
        log_path = Path(folder) / "app.log"
        log_path.write_text(SAMPLE_LOG, encoding="utf-8")
        entries, malformed = parse_log(log_path.read_text(encoding="utf-8"))
        summary = analyze(entries, malformed)

    report(summary)

    assert summary["total"] == 11
    assert summary["malformed"] == 1
    assert summary["levels"]["INFO"] == 5
    assert summary["levels"]["WARN"] == 3
    assert summary["levels"]["ERROR"] == 3
    assert summary["messages"].most_common(1) == [("connection timeout", 2)]
    assert summary["span"] == ("2026-09-01 09:00:01", "2026-09-01 09:00:33")
    print("\nAll assertions passed.")


if __name__ == "__main__":
    main()
