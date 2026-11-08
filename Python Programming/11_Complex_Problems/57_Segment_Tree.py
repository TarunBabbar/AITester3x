"""
Program 57: Segment Tree for Range Queries

Stores an array in a binary tree of ranges so range sums and range minimums
cost O(log n), and point updates cost O(log n) as well. The tree is built in
linear time and every operation is checked against a brute-force scan after
random updates.

Concepts: trees, divide and conquer, range queries, point updates.
"""

from __future__ import annotations

import random

INFINITY = float("inf")


class SegmentTree:
    def __init__(self, values: list[int]) -> None:
        if not values:
            raise ValueError("segment tree needs at least one value")

        self._length = len(values)
        size = 1
        while size < self._length:
            size <<= 1
        self._size = size

        self._sum = [0] * (2 * size)
        self._min = [INFINITY] * (2 * size)

        for index, value in enumerate(values):
            self._sum[size + index] = value
            self._min[size + index] = value

        for node in range(size - 1, 0, -1):
            self._sum[node] = self._sum[2 * node] + self._sum[2 * node + 1]
            self._min[node] = min(self._min[2 * node], self._min[2 * node + 1])

    def __len__(self) -> int:
        return self._length

    def get(self, index: int) -> int:
        self._check_index(index)
        return int(self._sum[self._size + index])

    def update(self, index: int, value: int) -> None:
        self._check_index(index)
        node = self._size + index
        self._sum[node] = value
        self._min[node] = value

        node //= 2
        while node >= 1:
            self._sum[node] = self._sum[2 * node] + self._sum[2 * node + 1]
            self._min[node] = min(self._min[2 * node], self._min[2 * node + 1])
            node //= 2

    def range_sum(self, left: int, right: int) -> int:
        """Sum over the half-open range [left, right)."""
        self._check_range(left, right)
        left += self._size
        right += self._size
        total = 0

        while left < right:
            if left & 1:
                total += self._sum[left]
                left += 1
            if right & 1:
                right -= 1
                total += self._sum[right]
            left //= 2
            right //= 2

        return int(total)

    def range_min(self, left: int, right: int) -> float:
        """Minimum over the half-open range [left, right); inf when empty."""
        self._check_range(left, right)
        left += self._size
        right += self._size
        smallest = INFINITY

        while left < right:
            if left & 1:
                smallest = min(smallest, self._min[left])
                left += 1
            if right & 1:
                right -= 1
                smallest = min(smallest, self._min[right])
            left //= 2
            right //= 2

        return smallest

    def to_list(self) -> list[int]:
        return [self.get(index) for index in range(self._length)]

    def _check_index(self, index: int) -> None:
        if not 0 <= index < self._length:
            raise IndexError(f"index {index} is outside 0..{self._length - 1}")

    def _check_range(self, left: int, right: int) -> None:
        if not 0 <= left <= right <= self._length:
            raise IndexError(f"range [{left}, {right}) is outside 0..{self._length}")


def brute_sum(values: list[int], left: int, right: int) -> int:
    return sum(values[left:right])


def brute_min(values: list[int], left: int, right: int) -> float:
    window = values[left:right]
    return min(window) if window else INFINITY


def main() -> None:
    print("--- building ---")
    values = [5, 2, 8, 1, 9, 3, 7]
    tree = SegmentTree(values)
    print("values:", values, "| length:", len(tree))

    assert len(tree) == len(values)
    assert tree.to_list() == values
    assert tree.range_sum(0, len(values)) == sum(values)
    assert tree.range_min(0, len(values)) == min(values)
    print("full range sum:", tree.range_sum(0, len(values)), "| min:", tree.range_min(0, len(values)))

    print("\n--- sums ---")
    expected_sums = {
        (0, 1): 5,
        (0, 3): 15,
        (1, 4): 11,
        (2, 5): 18,
        (3, 4): 1,
        (4, 7): 19,
        (0, 0): 0,
        (6, 7): 7,
    }
    for (left, right), expected in expected_sums.items():
        actual = tree.range_sum(left, right)
        print(f"  sum[{left}, {right}) = {actual}")
        assert actual == expected, f"sum[{left}, {right}): {actual} != {expected}"

    print("\n--- minimums ---")
    expected_mins = {
        (0, 3): 2,
        (0, 4): 1,
        (4, 7): 3,
        (2, 4): 1,
        (0, 7): 1,
        (5, 6): 3,
    }
    for (left, right), expected in expected_mins.items():
        actual = tree.range_min(left, right)
        print(f"  min[{left}, {right}) = {actual}")
        assert actual == expected, f"min[{left}, {right}): {actual} != {expected}"

    print("\n--- point updates ---")
    tree.update(3, 100)
    assert tree.get(3) == 100
    assert tree.range_sum(0, 4) == 5 + 2 + 8 + 100
    assert tree.range_min(0, 4) == 2
    print("  after updating index 3 to 100: sum[0,4) =", tree.range_sum(0, 4), "min[0,4) =", tree.range_min(0, 4))

    tree.update(0, -10)
    assert tree.range_min(0, 7) == -10
    assert tree.range_sum(0, 7) == sum(tree.to_list())
    print("  after updating index 0 to -10: whole-range min =", tree.range_min(0, 7))

    tree.update(3, 1)
    assert tree.get(3) == 1
    print("  restored index 3")

    print("\n--- empty ranges ---")
    assert tree.range_sum(2, 2) == 0
    assert tree.range_min(2, 2) == INFINITY
    assert tree.range_sum(0, 0) == 0
    print("  empty sum is 0, empty minimum is infinity")

    print("\n--- errors ---")
    error_cases = [
        lambda: SegmentTree([]),
        lambda: tree.get(-1),
        lambda: tree.get(len(values)),
        lambda: tree.update(len(values), 1),
        lambda: tree.range_sum(3, 2),
        lambda: tree.range_sum(-1, 2),
        lambda: tree.range_sum(0, len(values) + 1),
    ]
    for attempt in error_cases:
        try:
            attempt()
            raise AssertionError("expected an error")
        except (ValueError, IndexError):
            pass
    print("  empty input, bad indexes, and bad ranges are rejected")

    print("\n--- single element ---")
    single = SegmentTree([42])
    assert single.range_sum(0, 1) == 42
    assert single.range_min(0, 1) == 42
    single.update(0, 7)
    assert single.range_sum(0, 1) == 7
    print("  one-element tree supports queries and updates")

    print("\n--- powers of two and boundaries ---")
    for length in [2, 3, 4, 5, 8, 9, 16, 17]:
        sample = list(range(length))
        boundary_tree = SegmentTree(sample)
        assert boundary_tree.range_sum(0, length) == sum(sample)
        assert boundary_tree.range_min(0, length) == min(sample)
        assert boundary_tree.range_sum(length - 1, length) == sample[-1]
    print("  lengths around powers of two behave")

    print("\n--- against a plain list ---")
    rng = random.Random(57)
    model = [rng.randint(-20, 20) for _ in range(50)]
    random_tree = SegmentTree(model)

    for step in range(1_000):
        if rng.random() < 0.4:
            index = rng.randrange(len(model))
            value = rng.randint(-50, 50)
            model[index] = value
            random_tree.update(index, value)
        else:
            left = rng.randrange(len(model) + 1)
            right = rng.randrange(left, len(model) + 1)
            assert random_tree.range_sum(left, right) == brute_sum(model, left, right)
            assert random_tree.range_min(left, right) == brute_min(model, left, right)
        assert random_tree.to_list() == model, f"contents drifted at step {step}"

    print("  1000 random queries and updates match a brute-force scan")

    print("\n--- negative values ---")
    negatives = SegmentTree([-5, -1, -9, -3])
    assert negatives.range_min(0, 4) == -9
    assert negatives.range_sum(0, 4) == -18
    assert negatives.range_min(1, 3) == -9
    print("  min tracks the most negative value, not zero")

    print("\nAll checks passed.")


if __name__ == "__main__":
    main()
