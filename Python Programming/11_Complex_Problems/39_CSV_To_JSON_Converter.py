"""
Program 39: CSV to JSON Converter

Reads a CSV file of employees, converts each field to a sensible Python type
(ints and booleans instead of strings), writes the records out as JSON, and
round-trips the file to prove nothing was lost. Everything happens in a
temporary folder, so no files are left behind.

Concepts: csv module, json module, pathlib, type conversion, file I/O.
"""

import csv
import json
import tempfile
from pathlib import Path

SAMPLE_CSV = """\
name,department,salary,active
Alice,Engineering,95000,true
Bob,Sales,72000,false
Carol,Engineering,101000,true
Dan,Support,54000,true
"""


def cast(value: str):
    """Convert a CSV string to bool, int or leave it as text."""
    text = value.strip()
    if text.lower() in {"true", "false"}:
        return text.lower() == "true"
    try:
        return int(text)
    except ValueError:
        return text


def read_employees(path: Path) -> list[dict]:
    with path.open(newline="", encoding="utf-8") as handle:
        return [
            {key: cast(value) for key, value in row.items()}
            for row in csv.DictReader(handle)
        ]


def write_json(records: list[dict], path: Path) -> None:
    path.write_text(json.dumps(records, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    with tempfile.TemporaryDirectory() as folder:
        csv_path = Path(folder) / "employees.csv"
        json_path = Path(folder) / "employees.json"

        csv_path.write_text(SAMPLE_CSV, encoding="utf-8")
        employees = read_employees(csv_path)
        write_json(employees, json_path)

        reloaded = json.loads(json_path.read_text(encoding="utf-8"))

    print(json.dumps(reloaded, indent=2))

    assert len(employees) == 4
    assert reloaded == employees, "JSON round trip must preserve the records"
    assert isinstance(employees[0]["salary"], int)
    assert isinstance(employees[0]["active"], bool)
    assert employees[1]["active"] is False

    average = sum(person["salary"] for person in employees) / len(employees)
    busiest = max(
        {person["department"] for person in employees},
        key=lambda dept: sum(p["department"] == dept for p in employees),
    )
    print(f"\nAverage salary : {average:,.0f}")
    print(f"Largest team   : {busiest}")
    assert busiest == "Engineering"


if __name__ == "__main__":
    main()
