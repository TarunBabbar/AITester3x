"""
Program 4: CSV Sales Report Generator

Reads a sales CSV (date, product, quantity, price), computes per-product and
overall stats, and writes a readable summary report. Includes a demo CSV
generator so the script works out of the box.

Concepts: csv module, dict grouping, statistics, f-string formatting,
          file writing, argparse-lite (positional args).
"""

import csv
import sys
from collections import defaultdict
from pathlib import Path

SAMPLE_CSV = Path(__file__).with_name("sales_demo.csv")
REPORT_TXT = Path(__file__).with_name("sales_report.txt")


def make_sample_csv(path: Path = SAMPLE_CSV) -> None:
    """Create a small demo sales file if one doesn't exist."""
    if path.exists():
        return
    rows = [
        ("2026-08-01", "Laptop", 2, 1200),
        ("2026-08-01", "Mouse", 10, 25),
        ("2026-08-02", "Laptop", 1, 1200),
        ("2026-08-02", "Keyboard", 5, 60),
        ("2026-08-03", "Mouse", 8, 25),
        ("2026-08-03", "Monitor", 3, 300),
        ("2026-08-04", "Laptop", 3, 1100),
        ("2026-08-04", "Keyboard", 2, 60),
    ]
    with open(path, "w", newline="", encoding="utf-8") as fh:
        writer = csv.writer(fh)
        writer.writerow(["date", "product", "quantity", "price"])
        writer.writerows(rows)
    print(f"Created demo data: {path.name}")


def read_sales(path: Path) -> list[dict]:
    """Read CSV rows as dicts with numeric fields converted."""
    sales = []
    with open(path, newline="", encoding="utf-8") as fh:
        for row in csv.DictReader(fh):
            row["quantity"] = int(row["quantity"])
            row["price"] = float(row["price"])
            row["revenue"] = row["quantity"] * row["price"]
            sales.append(row)
    return sales


def build_report(sales: list[dict]) -> str:
    """Compute stats and return the report text."""
    per_product = defaultdict(lambda: {"units": 0, "revenue": 0.0})
    total_revenue = 0.0
    total_units = 0

    for row in sales:
        key = row["product"]
        per_product[key]["units"] += row["quantity"]
        per_product[key]["revenue"] += row["revenue"]
        total_revenue += row["revenue"]
        total_units += row["quantity"]

    lines = ["SALES REPORT", "=" * 40, ""]
    lines.append(f"Records : {len(sales)}")
    lines.append(f"Units   : {total_units}")
    lines.append(f"Revenue : ${total_revenue:,.2f}")
    lines.append("")
    lines.append("Per product:")
    for product, stats in sorted(per_product.items(), key=lambda kv: -kv[1]["revenue"]):
        lines.append(
            f"  {product:<10} {stats['units']:>5} units  ${stats['revenue']:>12,.2f}"
        )

    best = max(per_product.items(), key=lambda kv: kv[1]["revenue"])
    lines.append("")
    lines.append(f"Top product: {best[0]} (${best[1]['revenue']:,.2f})")
    return "\n".join(lines) + "\n"


def main() -> None:
    csv_path = Path(sys.argv[1]) if len(sys.argv) > 1 else SAMPLE_CSV
    if csv_path == SAMPLE_CSV:
        make_sample_csv()

    sales = read_sales(csv_path)
    report = build_report(sales)
    print(report)
    REPORT_TXT.write_text(report, encoding="utf-8")
    print(f"Report saved to {REPORT_TXT.name}")


if __name__ == "__main__":
    main()
