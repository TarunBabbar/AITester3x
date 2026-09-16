"""
Program 42: Generators & itertools Pipeline

Builds lazy data pipelines with generator functions and the itertools module:
an infinite Fibonacci stream trimmed with islice, running totals, grouping
consecutive runs, pairwise, combinations/permutations and chain. Also proves
generators are lazy and can only be consumed once.

Concepts: generators, yield, itertools, laziness, memory efficiency.
"""

import itertools


def fibonacci():
    """An infinite generator of Fibonacci numbers."""
    a, b = 0, 1
    while True:
        yield a
        a, b = b, a + b


def running_total(numbers):
    """Yield the cumulative sum after each number."""
    total = 0
    for number in numbers:
        total += number
        yield total


def counted(items):
    """Yield items while recording how many were actually pulled."""
    counted.pulled = 0
    for item in items:
        counted.pulled += 1
        yield item


counted.pulled = 0


def main() -> None:
    first_ten = list(itertools.islice(fibonacci(), 10))
    print(f"First 10 Fibonacci numbers : {first_ten}")
    assert first_ten == [0, 1, 1, 2, 3, 5, 8, 13, 21, 34]

    totals = list(running_total([10, -5, 3, 2]))
    print(f"Running totals             : {totals}")
    assert totals == [10, 5, 8, 10]

    readings = ["ok", "ok", "warn", "warn", "warn", "ok"]
    runs = [(level, len(list(group))) for level, group in itertools.groupby(readings)]
    print(f"Consecutive runs           : {runs}")
    assert runs == [("ok", 2), ("warn", 3), ("ok", 1)]

    pairs = list(itertools.pairwise([1, 2, 3, 4]))
    print(f"Neighbour pairs            : {pairs}")
    assert pairs == [(1, 2), (2, 3), (3, 4)]

    combos = list(itertools.combinations("ABC", 2))
    perms = list(itertools.permutations([1, 2, 3], 2))
    print(f"Combinations of 2 from ABC : {combos}")
    print(f"Permutations of 2 from 123 : {perms}")
    assert combos == [("A", "B"), ("A", "C"), ("B", "C")]
    assert len(perms) == 6

    chained = list(itertools.chain([1, 2], (3, 4), range(5, 7)))
    evens = list(itertools.islice(itertools.count(0, 2), 5))
    print(f"Chained sources            : {chained}")
    print(f"First 5 even numbers       : {evens}")
    assert chained == [1, 2, 3, 4, 5, 6]
    assert evens == [0, 2, 4, 6, 8]

    tracker = counted(range(100))
    assert counted.pulled == 0, "a generator does nothing until it is consumed"
    first_three = list(itertools.islice(tracker, 3))
    print(f"\nLaziness: pulled {counted.pulled} items to produce {first_three}")
    assert first_three == [0, 1, 2]
    assert counted.pulled == 3

    exhausted = running_total([1, 2, 3])
    assert list(exhausted) == [1, 3, 6]
    assert list(exhausted) == [], "a generator cannot be replayed"
    print("Exhaustion: a consumed generator yields nothing the second time.")

    print("\nAll generator and itertools checks passed.")


if __name__ == "__main__":
    main()
