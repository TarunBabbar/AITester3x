"""
Program 5: To-Do List Manager

A small CLI to-do app with priorities and JSON persistence. Tasks are saved
to a file next to the script and reloaded on every start.

Concepts: dataclasses, JSON persistence, argparse, sorting, simple CRUD.
"""

import argparse
import json
import sys
from dataclasses import dataclass, asdict
from pathlib import Path

STORE = Path(__file__).with_name("todo_store.json")
PRIORITIES = {"high": 0, "medium": 1, "low": 2}


@dataclass
class Task:
    title: str
    priority: str = "medium"
    done: bool = False


def load_tasks() -> list[Task]:
    if not STORE.exists():
        return []
    data = json.loads(STORE.read_text(encoding="utf-8"))
    return [Task(**item) for item in data]


def save_tasks(tasks: list[Task]) -> None:
    STORE.write_text(json.dumps([asdict(t) for t in tasks], indent=2), encoding="utf-8")


def add(tasks: list[Task], title: str, priority: str) -> None:
    if priority not in PRIORITIES:
        print(f"Unknown priority {priority!r}; use high/medium/low")
        return
    tasks.append(Task(title, priority))
    save_tasks(tasks)
    print(f"Added: {title} ({priority})")


def list_tasks(tasks: list[Task]) -> None:
    if not tasks:
        print("No tasks yet. Use: add <title> [priority]")
        return
    ordered = sorted(tasks, key=lambda t: (t.done, PRIORITIES[t.priority]))
    for i, task in enumerate(ordered, 1):
        mark = "[x]" if task.done else "[ ]"
        print(f"{i:>2}. {mark} {task.title:<30} {task.priority}")


def done(tasks: list[Task], number: int) -> None:
    if 1 <= number <= len(tasks):
        tasks[number - 1].done = True
        save_tasks(tasks)
        print(f"Marked task {number} done.")
    else:
        print(f"No task number {number}.")


def remove(tasks: list[Task], number: int) -> None:
    if 1 <= number <= len(tasks):
        removed = tasks.pop(number - 1)
        save_tasks(tasks)
        print(f"Removed: {removed.title}")
    else:
        print(f"No task number {number}.")


def main() -> None:
    parser = argparse.ArgumentParser(description="Simple to-do list manager")
    sub = parser.add_subparsers(dest="command", required=True)

    p_add = sub.add_parser("add", help="add a task")
    p_add.add_argument("title")
    p_add.add_argument("--priority", default="medium", choices=["high", "medium", "low"])

    p_done = sub.add_parser("done", help="mark a task done")
    p_done.add_argument("number", type=int)

    p_rm = sub.add_parser("remove", help="remove a task")
    p_rm.add_argument("number", type=int)

    sub.add_parser("list", help="show all tasks")

    args = parser.parse_args()
    tasks = load_tasks()

    if args.command == "add":
        add(tasks, args.title, args.priority)
    elif args.command == "done":
        done(tasks, args.number)
    elif args.command == "remove":
        remove(tasks, args.number)
    elif args.command == "list":
        list_tasks(tasks)
    else:
        parser.print_help()


if __name__ == "__main__":
    main()
