"""
Program 40: Date & Time Toolkit

Works through Python's datetime module: parsing and formatting dates, adding
working days with timedelta, weekday names, weekend checks, and the difference
between naive and timezone-aware datetimes. Uses fixed dates so output is
always the same.

Concepts: date, datetime, timedelta, strptime/strftime, timezone.
"""

from datetime import date, datetime, timedelta, timezone

ISO_FORMAT = "%Y-%m-%d"


def parse(text: str) -> date:
    return datetime.strptime(text, ISO_FORMAT).date()


def is_weekend(day: date) -> bool:
    return day.weekday() >= 5


def add_working_days(start: date, count: int) -> date:
    """Add `count` weekdays to start, skipping Saturdays and Sundays."""
    current = start
    added = 0
    while added < count:
        current += timedelta(days=1)
        if not is_weekend(current):
            added += 1
    return current


def formatted(day: date) -> str:
    return day.strftime("%A, %d %B %Y")


def main() -> None:
    project_start = parse("2026-01-05")            # a Monday
    print(f"Project start : {formatted(project_start)} (weekday={project_start.weekday()})")
    assert project_start.strftime("%A") == "Monday"
    assert not is_weekend(project_start)

    weekend_day = parse("2026-01-10")
    print(f"2026-01-10    : {formatted(weekend_day)} (weekend={is_weekend(weekend_day)})")
    assert is_weekend(weekend_day)

    sprint_end = add_working_days(project_start, 10)
    print(f"+10 workdays  : {formatted(sprint_end)}")
    assert sprint_end == parse("2026-01-19"), "10 working days from Mon 5 Jan is Mon 19 Jan"

    release = sprint_end + timedelta(days=7)
    elapsed = (release - project_start).days
    print(f"+7 calendar d : {formatted(release)} ({elapsed} days after start)")
    assert elapsed == 21

    naive = datetime(2026, 1, 5, 9, 0)
    aware = naive.replace(tzinfo=timezone.utc)
    india = aware.astimezone(timezone(timedelta(hours=5, minutes=30)))
    print(f"\nNaive         : {naive.isoformat()} (tzinfo={naive.tzinfo})")
    print(f"UTC           : {aware.isoformat()}")
    print(f"Asia/Kolkata  : {india.isoformat()}")
    assert naive.tzinfo is None and aware.tzinfo is not None
    assert india.hour == 14 and india.minute == 30

    print("\nAll date calculations verified.")


if __name__ == "__main__":
    main()
