"""
Program 20: Sorting Algorithms Benchmark

Generates a random list and times bubble, insertion, merge, and quick
sort on the same data, then reports wall-clock timings for each.

Concepts: algorithm complexity, recursion, timing, randomized data.
"""

import random
import statistics
import time

SIZE = 10_000


def bubble_sort(items: list[int]) -> list[int]:
    """O(n^2) in-place sort by swapping adjacent out-of-order pairs."""
    result = items[:]
    n = len(result)
    for i in range(n):
        swapped = False
        for j in range(0, n - i - 1):
            if result[j] > result[j + 1]:
                result[j], result[j + 1] = result[j + 1], result[j]
                swapped = True
        if not swapped:
            break
    return result


def insertion_sort(items: list[int]) -> list[int]:
    """O(n^2) sort that builds the sorted list one element at a time."""
    result = items[:]
    for i in range(1, len(result)):
        key = result[i]
        j = i - 1
        while j >= 0 and result[j] > key:
            result[j + 1] = result[j]
            j -= 1
        result[j + 1] = key
    return result


def merge_sort(items: list[int]) -> list[int]:
    """O(n log n) divide-and-conquer sort."""
    if len(items) <= 1:
        return items[:]
    mid = len(items) // 2
    left = merge_sort(items[:mid])
    right = merge_sort(items[mid:])
    merged: list[int] = []
    i = j = 0
    while i < len(left) and j < len(right):
        if left[i] <= right[j]:
            merged.append(left[i])
            i += 1
        else:
            merged.append(right[j])
            j += 1
    return merged + left[i:] + right[j:]


def quick_sort(items: list[int]) -> list[int]:
    """O(n log n) average-case sort using a random pivot."""
    if len(items) <= 1:
        return items[:]
    pivot = items[len(items) // 2]
    less = [x for x in items if x < pivot]
    equal = [x for x in items if x == pivot]
    greater = [x for x in items if x > pivot]
    return quick_sort(less) + equal + quick_sort(greater)


def time_sort(name: str, func: callable) -> float:
    """Run one sort, verify it, and return elapsed seconds."""
    start = time.perf_counter()
    result = func(data)
    elapsed = time.perf_counter() - start
    assert result == expected, f"{name} produced wrong order"
    print(f"  {name:<12} {elapsed:8.4f}s")
    return elapsed


def main() -> None:
    global data, expected
    data = [random.randint(0, 99_999) for _ in range(SIZE)]
    expected = sorted(data)

    print(f"Sorting {SIZE:,} random integers; verifying against sorted().\n")
    times = {
        "Bubble": time_sort("Bubble", bubble_sort),
        "Insertion": time_sort("Insertion", insertion_sort),
        "Merge": time_sort("Merge", merge_sort),
        "Quick": time_sort("Quick", quick_sort),
    }

    fastest = min(times, key=times.get)
    print(f"\nFastest: {fastest} ({times[fastest]:.4f}s)")
    print(f"O(n^2) Bubble/Insertion are expected to lag far behind.")


if __name__ == "__main__":
    main()