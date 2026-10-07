"""
Program 48: Interval Scheduling

Merging overlapping intervals, inserting a new one into a sorted list, picking
the largest set of non-overlapping meetings, and finding the gaps between them.

The greedy choice is the interesting part. Sorting by END time and taking every
interval that starts after the last one finished is optimal; sorting by start
time or by shortest length looks just as reasonable and is not. Both wrong
versions are implemented here so the difference is demonstrated rather than
asserted, and the greedy is checked against an exhaustive search on small inputs.

Concepts: dataclasses, sorting with keys, greedy algorithms, sweep lines,
           exhaustive search for a reference answer.

    python 48_Interval_Scheduling.py
"""

from __future__ import annotations

from dataclasses import dataclass
from itertools import combinations
from random import Random


@dataclass(frozen=True, order=True)
class Interval:
    """A half-open span [start, end)."""

    start: int
    end: int

    def __post_init__(self) -> None:
        if self.end < self.start:
            raise ValueError(f"interval ends before it starts: {self.start}-{self.end}")

    def __len__(self) -> int:
        return self.end - self.start

    def overlaps(self, other: Interval) -> bool:
        return self.start < other.end and other.start < self.end

    def touches(self, other: Interval) -> bool:
        return self.end == other.start or other.end == self.start


def merge(intervals: list[Interval]) -> list[Interval]:
    """Combine everything that overlaps, or merely touches, into as few spans as possible.

    Touching intervals are merged because this is used for busy/quiet reporting,
    where [9, 10] and [10, 11] are one stretch of the day.
    """
    if not intervals:
        return []
    ordered = sorted(intervals)
    merged = [ordered[0]]
    for interval in ordered[1:]:
        last = merged[-1]
        if interval.start <= last.end:
            if interval.end > last.end:
                merged[-1] = Interval(last.start, interval.end)
        else:
            merged.append(interval)
    return merged


def insert(intervals: list[Interval], new: Interval) -> list[Interval]:
    """Add one interval to a list that is already sorted and merged."""
    return merge([*intervals, new])


def busiest(intervals: list[Interval]) -> int:
    """The most intervals alive at any one moment, found by sweeping the endpoints."""
    events: list[tuple[int, int]] = []
    for interval in intervals:
        events.append((interval.start, 1))
        events.append((interval.end, -1))
    # Endings before starts at the same instant, so touching is not concurrent.
    events.sort(key=lambda event: (event[0], event[1]))
    running = peak = 0
    for _, delta in events:
        running += delta
        peak = max(peak, running)
    return peak


def gaps(intervals: list[Interval], day_start: int, day_end: int) -> list[Interval]:
    """The quiet stretches between merged intervals, trimmed to the working day."""
    if day_end < day_start:
        raise ValueError("the day ends before it starts")
    busy = merge([item for item in intervals if item.end > day_start and item.start < day_end])
    free: list[Interval] = []
    cursor = day_start
    for interval in busy:
        if interval.start > cursor:
            free.append(Interval(cursor, min(interval.start, day_end)))
        cursor = max(cursor, interval.end)
    if cursor < day_end:
        free.append(Interval(cursor, day_end))
    return free


# ---------------------------------------------------------------- the greedy
def most_meetings(intervals: list[Interval]) -> list[Interval]:
    """Optimal: earliest finishing first, then take whatever fits."""
    chosen: list[Interval] = []
    for interval in sorted(intervals, key=lambda item: (item.end, item.start, item.start + item.end)):
        if not chosen or chosen[-1].end <= interval.start:
            chosen.append(interval)
    return chosen


def most_meetings_by_start(intervals: list[Interval]) -> list[Interval]:
    """Wrong: earliest start first. A long meeting at 9am blocks the rest of the day."""
    chosen: list[Interval] = []
    for interval in sorted(intervals, key=lambda item: (item.start, item.end)):
        if not chosen or chosen[-1].end <= interval.start:
            chosen.append(interval)
    return chosen


def most_meetings_by_length(intervals: list[Interval]) -> list[Interval]:
    """Wrong: shortest first. A short meeting in the middle can split two long ones."""
    chosen: list[Interval] = []
    for interval in sorted(intervals, key=lambda item: (len(item), item.start)):
        if all(not interval.overlaps(kept) for kept in chosen):
            chosen.append(interval)
    return chosen


def best_by_exhaustion(intervals: list[Interval]) -> int:
    """Try every subset and keep the largest legal one. Only sane for small inputs."""
    if len(intervals) > 16:
        raise ValueError("exhaustive search is for small inputs only")
    best = 0
    for size in range(len(intervals), 0, -1):
        if size <= best:
            break
        for candidate in combinations(intervals, size):
            if all(not a.overlaps(b) for a, b in combinations(candidate, 2)):
                best = size
                break
    return best


def main() -> None:
    # ---- merging ------------------------------------------------------------
    day = [Interval(1, 3), Interval(2, 6), Interval(8, 10), Interval(15, 18)]
    assert merge(day) == [Interval(1, 6), Interval(8, 10), Interval(15, 18)], merge(day)
    assert merge([]) == [], "nothing merges to nothing"
    assert merge([Interval(5, 7)]) == [Interval(5, 7)], "a single interval is left alone"
    assert merge([Interval(1, 10), Interval(2, 3)]) == [Interval(1, 10)], "a nested interval disappears"
    assert merge([Interval(1, 3), Interval(3, 5)]) == [Interval(1, 5)], "touching intervals merge"
    assert merge([Interval(1, 3), Interval(4, 5)]) == [Interval(1, 3), Interval(4, 5)], "a gap does not"
    assert merge([Interval(3, 4), Interval(1, 2)]) == [Interval(1, 2), Interval(3, 4)], "input order does not matter"
    print(f"merge        : {len(day)} intervals became {len(merge(day))}")

    # merging never leaves overlaps behind, and doing it twice changes nothing
    for size in range(0, 12):
        sample = [Interval(index * 3, index * 3 + 1 + index % 4) for index in range(size)]
        merged = merge(sample)
        for left, right in zip(merged, merged[1:]):
            assert left.end < right.start, f"merged intervals must not touch: {left} {right}"
        assert merge(merged) == merged, "merging is idempotent"
        covered = sum(len(item) for item in merged)
        assert covered <= sum(len(item) for item in sample), "merging cannot invent time"
        if sample:
            assert covered >= max(len(item) for item in sample), "and cannot lose an interval"
    print("merge        : idempotent, with no overlaps left, for 0 to 11 intervals")

    # ---- inserting ----------------------------------------------------------
    booked = [Interval(1, 3), Interval(6, 9)]
    assert insert(booked, Interval(2, 5)) == [Interval(1, 5), Interval(6, 9)], insert(booked, Interval(2, 5))
    assert insert(booked, Interval(4, 5)) == [Interval(1, 3), Interval(4, 5), Interval(6, 9)], \
        "one that fits in the gap is simply added"
    assert insert(booked, Interval(3, 6)) == [Interval(1, 9)], "one that touches both bridges them"
    assert insert(booked, Interval(10, 11)) == [Interval(1, 3), Interval(6, 9), Interval(10, 11)], "or lands after"
    assert insert(booked, Interval(1, 9)) == [Interval(1, 9)], "a covering interval swallows the rest"
    print(f"insert       : 3-6 into {booked} bridges them into {insert(booked, Interval(3, 6))}")

    # ---- the greedy, and the two reasonable-looking wrong answers ------------
    meetings = [
        Interval(1, 4), Interval(3, 5), Interval(0, 6), Interval(5, 7), Interval(3, 9),
        Interval(5, 9), Interval(6, 10), Interval(8, 11), Interval(8, 12), Interval(2, 14),
        Interval(12, 16),
    ]
    best = most_meetings(meetings)
    by_start = most_meetings_by_start(meetings)
    by_length = most_meetings_by_length(meetings)
    assert best == [Interval(1, 4), Interval(5, 7), Interval(8, 11), Interval(12, 16)], best
    assert len(by_start) < len(best), f"by start: {by_start} is worse than {best}"
    assert len(by_length) <= len(best), f"by length: {by_length} is no better than {best}"
    assert len(best) == best_by_exhaustion(meetings), "and exhaustive search agrees it is optimal"
    print(f"greedy       : {len(best)} meetings: {best}")
    print(f"by start     : {len(by_start)} meetings - worse, the 0-6 meeting wins and blocks the day")
    print(f"by length    : {len(by_length)} meetings: {by_length}")

    # the greedy is optimal on every small random instance, per exhaustive search
    random = Random(20260913)
    checked = 0
    for _ in range(60):
        size = random.randint(1, 9)
        instance = []
        for _ in range(size):
            start = random.randint(0, 20)
            instance.append(Interval(start, start + random.randint(1, 6)))
        greedy = most_meetings(instance)
        assert all(not a.overlaps(b) for a, b in combinations(greedy, 2)), "the greedy output is legal"
        assert len(greedy) == best_by_exhaustion(instance), f"not optimal on {instance}"
        checked += 1
    print(f"optimality   : greedy matched exhaustive search on {checked} random instances")

    # ---- the sweep line -----------------------------------------------------
    assert busiest([]) == 0, "nothing is happening"
    assert busiest([Interval(1, 4)]) == 1, "one meeting is one"
    assert busiest([Interval(9, 11), Interval(10, 12), Interval(11, 13)]) == 2, "the middle overlaps"
    assert busiest([Interval(9, 11), Interval(11, 13)]) == 1, "but back to back does not"
    assert busiest([Interval(1, 5), Interval(2, 6), Interval(3, 7)]) == 3, "all three at once"
    print(f"busiest      : peak concurrency {busiest(meetings)} across the day")

    # ---- finding the quiet parts --------------------------------------------
    booked = [Interval(9, 10), Interval(12, 13)]
    assert gaps(booked, 9, 17) == [Interval(10, 12), Interval(13, 17)], gaps(booked, 9, 17)
    assert gaps([], 9, 17) == [Interval(9, 17)], "an empty day is entirely free"
    assert gaps([Interval(9, 17)], 9, 17) == [], "a fully booked day has none"
    assert gaps([Interval(8, 10)], 9, 17) == [Interval(10, 17)], "a meeting already underway is trimmed"
    assert gaps([Interval(16, 20)], 9, 17) == [Interval(9, 16)], "and one that runs past the end"
    print(f"gaps         : around {booked} the free time is {gaps(booked, 9, 17)}")

    # gaps and busy time always add up to the length of the day
    for _ in range(40):
        size = random.randint(0, 8)
        instance = []
        for _ in range(size):
            start = random.randint(9, 16)
            instance.append(Interval(start, min(start + random.randint(1, 5), 17)))
        instance = [item for item in instance if len(item) > 0]
        free_time = sum(len(item) for item in gaps(instance, 9, 17))
        busy_time = sum(len(item) for item in merge([i for i in instance if i.end > 9 and i.start < 17]))
        assert free_time + busy_time == 8, f"free {free_time} + busy {busy_time} should be 8 hours"
    print("gaps         : free plus busy always adds up to the eight hour day")

    # ---- validation ---------------------------------------------------------
    for start, end in ((5, 3), (1, 0)):
        try:
            Interval(start, end)
            raise AssertionError(f"{start}-{end} should have been rejected")
        except ValueError as error:
            assert "before it starts" in str(error)
    try:
        gaps([], 17, 9)
        raise AssertionError("a day that ends before it starts should be rejected")
    except ValueError as error:
        print(f"validation   : {error}")

    assert Interval(1, 3).overlaps(Interval(2, 4)), "overlap is symmetric"
    assert Interval(2, 4).overlaps(Interval(1, 3)), "both ways round"
    assert not Interval(1, 3).overlaps(Interval(3, 5)), "touching is not overlapping"
    assert Interval(1, 3).touches(Interval(3, 5)), "but it is touching"
    assert len(Interval(3, 8)) == 5, "length comes from __len__"
    assert sorted([Interval(3, 4), Interval(1, 9)]) == [Interval(1, 9), Interval(3, 4)], "ordering is by start"
    print("comparison   : overlaps, touches, length and ordering all behave")
    print("All checks passed.")


if __name__ == "__main__":
    main()
