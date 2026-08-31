"""
Program 1: Task Scheduler with Cron-Style Parsing and Priority Queue

A command-line task scheduler that:
- Parses cron-style expressions ("*/5 * * * *") into next-run datetimes.
- Stores tasks in a heap-based priority queue ordered by next run time.
- Supports task priority (high/medium/low) to break ties.
- Persists tasks to JSON and reloads them across sessions.
- Runs a REPL: add, list, run-due, remove, save, quit.

Concepts: dataclasses, heapq, datetime/zoneinfo, JSON persistence,
          parser design, generators.
"""

from __future__ import annotations

import heapq
import json
import re
from dataclasses import dataclass, field, asdict
from datetime import datetime, timedelta
from pathlib import Path
from zoneinfo import ZoneInfo

DEFAULT_FILE = Path(__file__).with_name("tasks_scheduler.json")

# Map cron field index -> (min, max)
CRON_FIELDS = [
    (0, 59),    # minute
    (0, 23),    # hour
    (1, 31),    # day of month
    (1, 12),    # month
    (0, 6),     # day of week (0 = Sunday)
]

PRIORITY_RANK = {"high": 0, "medium": 1, "low": 2}


class CronParseError(ValueError):
    """Raised when a cron expression cannot be parsed."""


def parse_cron_field(field_expr: str, lo: int, hi: int) -> set[int]:
    """Parse one cron field ('*', '5', '*/15', '1-5', '1,3,7') into a set of ints."""
    values: set[int] = set()
    for part in field_expr.split(","):
        part = part.strip()
        if not part:
            raise CronParseError(f"Empty field part in {field_expr!r}")
        if part == "*":
            values.update(range(lo, hi + 1))
            continue
        step = 1
        if "/" in part:
            part, step_str = part.split("/", 1)
            step = int(step_str)
            if step <= 0:
                raise CronParseError(f"Step must be positive: {step_str!r}")
            if part == "*":
                part = f"{lo}-{hi}"
        if "-" in part:
            start_str, end_str = part.split("-", 1)
            start, end = int(start_str), int(end_str)
            if start < lo or end > hi:
                raise CronParseError(f"Range {part!r} out of bounds [{lo}-{hi}]")
            values.update(range(start, end + 1, step))
        else:
            value = int(part)
            if value < lo or value > hi:
                raise CronParseError(f"Value {value} out of bounds [{lo}-{hi}]")
            values.add(value)
    if not values:
        raise CronParseError(f"No values parsed from {field_expr!r}")
    return values


def parse_cron(expr: str) -> tuple[set[int], set[int], set[int], set[int], set[int]]:
    """Parse a 5-field cron expression into minute/hour/dom/month/dow sets."""
    parts = expr.split()
    if len(parts) != 5:
        raise CronParseError(f"Expected 5 fields, got {len(parts)}: {expr!r}")
    return tuple(
        parse_cron_field(part, lo, hi)
        for part, (lo, hi) in zip(parts, CRON_FIELDS)
    )


def next_run(expr: str, after: datetime) -> datetime:
    """Compute the next datetime matching a cron expression strictly after `after`.

    Brute-force minute walk: simple, obviously correct, and fast enough for a
    scheduler (worst case ~4 months of minutes for an annual schedule, well
    under a second). Optimizing this with calendar arithmetic is a classic
    source of subtle bugs, so we keep the naive version.
    """
    minutes, hours, doms, months, dows = parse_cron(expr)
    candidate = after.replace(second=0, microsecond=0) + timedelta(minutes=1)
    while True:
        if (candidate.month in months and candidate.day in doms
                and candidate.weekday() in dows
                and candidate.hour in hours and candidate.minute in minutes):
            return candidate
        candidate += timedelta(minutes=1)


@dataclass(order=True)
class ScheduledTask:
    """Heap entry; ordering is (next_run, priority_rank, created_seq)."""
    next_run: datetime
    priority_rank: int
    seq: int
    name: str = field(compare=False)
    cron: str = field(compare=False)
    priority: str = field(compare=False)

    def to_dict(self) -> dict:
        data = asdict(self)
        data["next_run"] = self.next_run.isoformat()
        return data

    @classmethod
    def from_dict(cls, data: dict) -> "ScheduledTask":
        data["next_run"] = datetime.fromisoformat(data["next_run"])
        return cls(**data)


class TaskScheduler:
    def __init__(self, tz: ZoneInfo | None = None) -> None:
        self.tz = tz or ZoneInfo("UTC")
        self.heap: list[ScheduledTask] = []
        self.seq = 0
        self._by_name: dict[str, ScheduledTask] = {}

    def add(self, name: str, cron: str, priority: str = "medium") -> None:
        if name in self._by_name:
            raise ValueError(f"Task {name!r} already exists")
        if priority not in PRIORITY_RANK:
            raise ValueError(f"Unknown priority {priority!r}; use high/medium/low")
        now = datetime.now(self.tz)
        first = next_run(cron, now)
        task = ScheduledTask(first, PRIORITY_RANK[priority], self.seq, name, cron, priority)
        self.seq += 1
        heapq.heappush(self.heap, task)
        self._by_name[name] = task
        print(f"Added {name!r}: next run {first:%Y-%m-%d %H:%M} ({cron}, {priority})")

    def due(self, at: datetime | None = None) -> list[ScheduledTask]:
        """Pop all tasks whose next run is <= now; reschedule them for the next match."""
        now = at or datetime.now(self.tz)
        fired: list[ScheduledTask] = []
        while self.heap and self.heap[0].next_run <= now:
            task = heapq.heappop(self.heap)
            fired.append(task)
            task.next_run = next_run(task.cron, now)
            heapq.heappush(self.heap, task)
        return fired

    def remove(self, name: str) -> bool:
        task = self._by_name.pop(name, None)
        if task is None:
            return False
        self.heap.remove(task)      # O(n) but fine for interactive use
        heapq.heapify(self.heap)
        return True

    def list_tasks(self) -> list[ScheduledTask]:
        return sorted(self.heap)

    def save(self, path: Path = DEFAULT_FILE) -> None:
        payload = {
            "seq": self.seq,
            "tasks": [t.to_dict() for t in self.heap],
        }
        path.write_text(json.dumps(payload, indent=2), encoding="utf-8")
        print(f"Saved {len(self.heap)} tasks to {path}")

    def load(self, path: Path = DEFAULT_FILE) -> None:
        if not path.exists():
            return
        payload = json.loads(path.read_text(encoding="utf-8"))
        self.seq = payload["seq"]
        for item in payload["tasks"]:
            task = ScheduledTask.from_dict(item)
            heapq.heappush(self.heap, task)
            self._by_name[task.name] = task
        print(f"Loaded {len(self.heap)} tasks from {path}")


HELP = """Commands:
  add <name> '<cron>' [high|medium|low]   add a task (cron: '*/5 * * * *')
  list                                   show tasks sorted by next run
  run-due                                execute and reschedule due tasks
  remove <name>                          delete a task
  save                                   persist to JSON
  load                                   reload from JSON
  help                                   this help
  quit                                   exit"""


def repl() -> None:
    scheduler = TaskScheduler()
    scheduler.load()
    print("Task Scheduler REPL. Type 'help' for commands.")
    while True:
        try:
            line = input("sched> ").strip()
        except (EOFError, KeyboardInterrupt):
            print("\nBye.")
            break
        if not line:
            continue
        parts = line.split(None, 2)
        cmd = parts[0].lower()
        try:
            if cmd == "quit":
                scheduler.save()
                break
            elif cmd == "help":
                print(HELP)
            elif cmd == "list":
                for t in scheduler.list_tasks():
                    print(f"  {t.next_run:%Y-%m-%d %H:%M}  {t.priority:6s}  {t.name}  ({t.cron})")
            elif cmd == "run-due":
                for t in scheduler.due():
                    print(f"FIRED {t.name!r} at {t.next_run:%H:%M} -> rescheduled to {t.next_run}")
            elif cmd == "add" and len(parts) >= 3:
                # parts = ["add", name, "'cron' [priority]"] after split(None, 2)
                rest = parts[2]
                cron_match = re.search(r"'([^']*)'", rest)
                if not cron_match:
                    raise ValueError("cron expression must be quoted, e.g. add backup '0 2 * * *' high")
                cron = cron_match.group(1)
                tail = rest[cron_match.end():].strip()
                priority = tail.split()[-1] if tail and tail.split()[-1] in PRIORITY_RANK else "medium"
                scheduler.add(parts[1], cron, priority)
            elif cmd == "remove" and len(parts) == 2:
                print("Removed" if scheduler.remove(parts[1]) else f"No task {parts[1]!r}")
            elif cmd == "save":
                scheduler.save()
            elif cmd == "load":
                scheduler.load()
            else:
                print("Unknown command or bad arguments. Type 'help'.")
        except (CronParseError, ValueError) as exc:
            print(f"Error: {exc}")


if __name__ == "__main__":
    # Self-test: verify cron parsing and next-run computation without the REPL.
    tests = [
        ("*/15 * * * *", datetime(2026, 8, 31, 10, 7), datetime(2026, 8, 31, 10, 15)),
        ("0 9 * * 1-5", datetime(2026, 8, 31, 12, 0), datetime(2026, 9, 1, 9, 0)),
        ("30 2 1 * *", datetime(2026, 8, 31, 12, 0), datetime(2026, 9, 1, 2, 30)),
        ("0 0 1 1 *", datetime(2026, 8, 31, 12, 0), datetime(2027, 1, 1, 0, 0)),
        # Jan 1 2027 is a Friday (weekday 4); "0 0 1 1 4" fires there.
        ("0 0 1 1 4", datetime(2026, 8, 31, 12, 0), datetime(2027, 1, 1, 0, 0)),
    ]
    for expr, after, expected in tests:
        got = next_run(expr, after)
        status = "OK " if got == expected else "FAIL"
        print(f"[{status}] {expr} after {after} -> {got}")
    assert all(next_run(e, a) == x for e, a, x in tests), "cron tests failed"

    # Heap ordering demo
    sched = TaskScheduler()
    sched.add("backup", "0 2 * * *", "high")
    sched.add("report", "*/5 * * * *", "medium")
    sched.add("cleanup", "30 3 * * 0", "low")
    print("\nQueue order (soonest first):")
    for t in sched.list_tasks():
        print(f"  {t.next_run:%Y-%m-%d %H:%M}  {t.priority:6s}  {t.name}")
