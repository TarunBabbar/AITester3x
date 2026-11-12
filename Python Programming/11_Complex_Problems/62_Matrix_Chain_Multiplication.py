"""
Program 62: Matrix Chain Multiplication

Chooses the order in which to multiply a chain of matrices so the number of
scalar multiplications is smallest. The cost of multiplying an a x b matrix by a
b x c matrix is a * b * c, so the order matters a lot even though the result does
not. The interval dynamic program fills a cost table by chain length, a memoised
recursion recomputes the same values, and exhaustive parenthesisation checks the
small cases.

Concepts: dynamic programming, intervals, memoisation, reconstruction.
"""

from __future__ import annotations

import random
from functools import lru_cache


def _validate(dimensions: list[int]) -> None:
    if len(dimensions) < 2:
        raise ValueError("need at least two dimensions for one matrix")
    for dimension in dimensions:
        if not isinstance(dimension, int) or isinstance(dimension, bool):
            raise TypeError("dimensions must be integers")
        if dimension <= 0:
            raise ValueError("dimensions must be positive")


def matrix_chain(dimensions: list[int]) -> tuple[int, list[list[int]]]:
    """Minimum cost and the split table for matrices 0..n-1."""
    _validate(dimensions)
    count = len(dimensions) - 1

    cost = [[0] * count for _ in range(count)]
    split = [[-1] * count for _ in range(count)]

    for length in range(2, count + 1):
        for start in range(0, count - length + 1):
            end = start + length - 1
            best = None
            for middle in range(start, end):
                candidate = (
                    cost[start][middle]
                    + cost[middle + 1][end]
                    + dimensions[start] * dimensions[middle + 1] * dimensions[end + 1]
                )
                if best is None or candidate < best:
                    best = candidate
                    split[start][end] = middle
            cost[start][end] = best if best is not None else 0

    return cost[0][count - 1], split


def optimal_order(dimensions: list[int]) -> tuple[int, str]:
    """Minimum cost and a fully parenthesised multiplication order."""
    cost, split = matrix_chain(dimensions)
    count = len(dimensions) - 1
    names = [f"M{index + 1}" for index in range(count)]

    def build(start: int, end: int) -> str:
        if start == end:
            return names[start]
        middle = split[start][end]
        return f"({build(start, middle)} x {build(middle + 1, end)})"

    return cost, build(0, count - 1)


def memoized_cost(dimensions: list[int]) -> int:
    _validate(dimensions)
    last = len(dimensions) - 2

    @lru_cache(maxsize=None)
    def solve(start: int, end: int) -> int:
        if start == end:
            return 0
        return min(
            solve(start, middle)
            + solve(middle + 1, end)
            + dimensions[start] * dimensions[middle + 1] * dimensions[end + 1]
            for middle in range(start, end)
        )

    return solve(0, last)


def brute_cost(dimensions: list[int], start: int = 0, end: int | None = None) -> int:
    _validate(dimensions)
    if end is None:
        end = len(dimensions) - 2
    if start == end:
        return 0
    return min(
        brute_cost(dimensions, start, middle)
        + brute_cost(dimensions, middle + 1, end)
        + dimensions[start] * dimensions[middle + 1] * dimensions[end + 1]
        for middle in range(start, end)
    )


def main() -> None:
    print("--- known cases ---")
    cases = [
        ([40, 20, 30, 10, 30], 26000),
        ([30, 35, 15, 5, 10, 20, 25], 15125),
        ([10, 20, 30], 6000),
        ([10, 20], 0),
        ([5, 10, 3, 12, 5, 50, 6], 2010),
    ]
    for dimensions, expected in cases:
        cost, order = optimal_order(dimensions)
        print(f"  {dimensions} -> {cost} via {order}")
        assert cost == expected, f"{dimensions}: {cost} != {expected}"
        assert order.count("(") == order.count(")")
        assert order.count("x") == len(dimensions) - 2

    print("\n--- single and double matrices ---")
    assert matrix_chain([10, 20])[0] == 0
    assert optimal_order([10, 20]) == (0, "M1")
    assert matrix_chain([10, 20, 30])[0] == 6000
    print("  one matrix costs nothing; two use the only order")

    print("\n--- the three implementations agree ---")
    assert memoized_cost([40, 20, 30, 10, 30]) == 26000
    assert brute_cost([40, 20, 30, 10, 30]) == 26000
    assert memoized_cost([30, 35, 15, 5, 10, 20, 25]) == 15125
    assert brute_cost([30, 35, 15, 5, 10, 20, 25]) == 15125
    print("  dynamic programming, memoisation, and brute force all agree")

    print("\n--- validation ---")
    invalid: list[list[int]] = [[], [10], [10, 0], [10, -2], [1.5, 2]]
    for dimensions in invalid:
        try:
            matrix_chain(dimensions)
            raise AssertionError(f"{dimensions} should be rejected")
        except (ValueError, TypeError):
            pass
    print("  short, non-positive, and non-integer dimension lists rejected")

    print("\n--- random cross-check ---")
    rng = random.Random(62)
    checked = 0

    for _ in range(150):
        count = rng.randint(1, 6)
        dimensions = [rng.randint(1, 20) for _ in range(count + 1)]

        dynamic = matrix_chain(dimensions)[0]
        memoized = memoized_cost(dimensions)
        exhaustive = brute_cost(dimensions)

        assert dynamic == memoized == exhaustive, f"{dimensions}: {dynamic}, {memoized}, {exhaustive}"
        assert dynamic >= 0

        _, order = optimal_order(dimensions)
        assert order.count("(") == order.count(")")
        assert order.count("x") == count - 1
        checked += 1

    print(f"  {checked} random chains agree across all three methods")

    print("\n--- a longer chain ---")
    dimensions = [rng.randint(1, 100) for _ in range(101)]
    cost, _ = matrix_chain(dimensions)
    print(f"  100 matrices -> {cost} scalar multiplications")
    assert cost > 0
    assert memoized_cost(dimensions) == cost

    print("\n--- the order is respected by the cost ---")
    # A single chain of equal blocks: multiplying left to right costs the same
    # as any other order when every matrix is square and identical.
    square = [10] * 6
    cost_square, order_square = optimal_order(square)
    print(f"  five 10x10 matrices -> {cost_square} via {order_square}")
    assert cost_square == 4 * 10 * 10 * 10
    assert order_square.count("x") == 4

    print("\nAll checks passed.")


if __name__ == "__main__":
    main()
