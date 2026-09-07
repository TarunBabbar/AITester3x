"""
Program 15: Expense Tracker

Records expenses with category and amount, stores them in JSON, and prints
a summary grouped by category.

Concepts: JSON persistence, dataclasses, grouping with dicts, argparse.
"""

import argparse
import json
from collections import defaultdict
from dataclasses import dataclass, asdict
from pathlib import Path

STORE = Path(__file__).with_name("expenses_store.json")

CATEGORIES = ["food", "transport", "shopping", "bills", "other"]


@dataclass
class Expense:
    description: str
    amount: float
    category: str


def load_expenses() -> list[Expense]:
    if not STORE.exists():
        return []
    return [Expense(**item) for item in json.loads(STORE.read_text(encoding="utf-8"))]


def save_expenses(expenses: list[Expense]) -> None:
    STORE.write_text(json.dumps([asdict(e) for e in expenses], indent=2), encoding="utf-8")


def add(expenses: list[Expense], description: str, amount: float, category: str) -> None:
    if category not in CATEGORIES:
        print(f"Unknown category {category!r}; use {', '.join(CATEGORIES)}")
        return
    expenses.append(Expense(description, amount, category))
    save_expenses(expenses)
    print(f"Added: {description} - ${amount:.2f} ({category})")


def summary(expenses: list[Expense]) -> None:
    if not expenses:
        print("No expenses yet. Use: add <description> <amount> --category <cat>")
        return

    by_category: dict[str, float] = defaultdict(float)
    total = 0.0
    for expense in expenses:
        by_category[expense.category] += expense.amount
        total += expense.amount

    print(f"{'Category':<12} {'Amount':>10}")
    print("-" * 24)
    for category in sorted(by_category, key=lambda c: -by_category[c]):
        print(f"{category:<12} ${by_category[category]:>9.2f}")
    print("-" * 24)
    print(f"{'TOTAL':<12} ${total:>9.2f}")
    print(f"\n{len(expenses)} expense(s) recorded.")


def main() -> None:
    parser = argparse.ArgumentParser(description="Simple expense tracker")
    sub = parser.add_subparsers(dest="command", required=True)

    p_add = sub.add_parser("add", help="add an expense")
    p_add.add_argument("description")
    p_add.add_argument("amount", type=float)
    p_add.add_argument("--category", default="other", choices=CATEGORIES)

    sub.add_parser("summary", help="show category summary")

    args = parser.parse_args()
    expenses = load_expenses()

    if args.command == "add":
        add(expenses, args.description, args.amount, args.category)
    elif args.command == "summary":
        summary(expenses)


if __name__ == "__main__":
    main()
