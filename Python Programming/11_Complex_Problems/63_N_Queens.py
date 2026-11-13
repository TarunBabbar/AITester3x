"""
Program 63: N-Queens

Places N queens on an N x N board so that no two share a row, column, or
diagonal. The enumeration uses backtracking with three sets; the counting
version uses three bitmasks, which lets it reach N = 12 quickly. Known solution
counts and board validation keep both implementations honest.

Concepts: backtracking, recursion, bit manipulation, symmetry of attacks.
"""

from __future__ import annotations


def solve_n_queens(size: int) -> list[list[int]]:
    """Every solution as a list where entry r is the column of the queen in row r."""
    if size < 0:
        raise ValueError("board size must not be negative")

    solutions: list[list[int]] = []
    columns: set[int] = set()
    down_diagonals: set[int] = set()
    up_diagonals: set[int] = set()
    current: list[int] = []

    def place(row: int) -> None:
        if row == size:
            solutions.append(list(current))
            return

        for column in range(size):
            if (
                column in columns
                or (row - column) in down_diagonals
                or (row + column) in up_diagonals
            ):
                continue

            columns.add(column)
            down_diagonals.add(row - column)
            up_diagonals.add(row + column)
            current.append(column)

            place(row + 1)

            current.pop()
            columns.remove(column)
            down_diagonals.remove(row - column)
            up_diagonals.remove(row + column)

    place(0)
    return solutions


def count_solutions(size: int) -> int:
    """Bitmask backtracking: at most N = 12 in reasonable time."""
    if size < 0:
        raise ValueError("board size must not be negative")
    if size == 0:
        return 1

    full = (1 << size) - 1
    total = 0

    def place(columns: int, down: int, up: int) -> None:
        nonlocal total
        if columns == full:
            total += 1
            return

        available = full & ~(columns | down | up)
        while available:
            bit = available & -available
            available -= bit
            place(columns | bit, (down | bit) << 1, (up | bit) >> 1)

    place(0, 0, 0)
    return total


def is_valid(solution: list[int]) -> bool:
    size = len(solution)
    if sorted(solution) != list(range(size)):
        return False

    for row in range(size):
        for other in range(row + 1, size):
            if abs(solution[row] - solution[other]) == other - row:
                return False

    return True


def describe(solution: list[int]) -> list[str]:
    size = len(solution)
    return [
        "".join("Q" if column == solution[row] else "." for column in range(size))
        for row in range(size)
    ]


def main() -> None:
    print("--- known counts ---")
    known = {
        0: 1,
        1: 1,
        2: 0,
        3: 0,
        4: 2,
        5: 10,
        6: 4,
        7: 40,
        8: 92,
        9: 352,
        10: 724,
        11: 2680,
        12: 14200,
    }
    for size, expected in known.items():
        actual = count_solutions(size)
        print(f"  N={size:2}: {actual}")
        assert actual == expected, f"N={size}: {actual} != {expected}"

    print("\n--- enumeration matches the count ---")
    for size in range(0, 9):
        solutions = solve_n_queens(size)
        assert len(solutions) == known[size], f"N={size} enumeration"
        assert len({tuple(solution) for solution in solutions}) == len(solutions), "solutions are unique"
        for solution in solutions:
            assert len(solution) == size
            assert is_valid(solution), f"invalid solution {solution}"
    print("  N=0..8: every solution is valid and unique")

    print("\n--- small boards ---")
    assert solve_n_queens(0) == [[]]
    assert solve_n_queens(1) == [[0]]
    assert solve_n_queens(2) == []
    assert solve_n_queens(3) == []
    assert solve_n_queens(4) == [[1, 3, 0, 2], [2, 0, 3, 1]]
    print("  N=0 has the empty board, N=1 one square, N=2 and N=3 impossible")

    print("\n--- boards ---")
    for row in describe([1, 3, 0, 2]):
        print(f"  {row}")
    rendered = describe([1, 3, 0, 2])
    assert len(rendered) == 4
    assert all(len(row) == 4 for row in rendered)
    assert all(row.count("Q") == 1 for row in rendered)
    for row_index, row in enumerate(rendered):
        assert row.index("Q") == [1, 3, 0, 2][row_index]

    print("\n--- invalid boards are rejected ---")
    assert not is_valid([0, 0, 0, 0]), "same column"
    assert not is_valid([0, 1, 2, 3]), "same diagonal"
    assert not is_valid([1, 0]), "same diagonal, size two"
    assert is_valid([0]), "single queen is valid"
    print("  shared columns and diagonals are caught")

    print("\n--- solutions are independent copies ---")
    solutions = solve_n_queens(4)
    solutions[0][0] = 99
    assert solve_n_queens(4)[0] == [1, 3, 0, 2]
    print("  mutating one result does not affect a new call")

    print("\n--- validation ---")
    for size in (-1, -5):
        try:
            solve_n_queens(size)
            raise AssertionError("negative size must be rejected")
        except ValueError:
            pass
        try:
            count_solutions(size)
            raise AssertionError("negative size must be rejected")
        except ValueError:
            pass
    print("  negative sizes rejected")

    print("\n--- the symmetrical eight queens ---")
    eight = solve_n_queens(8)
    solution_set = {tuple(solution) for solution in eight}
    reflections = {
        tuple(7 - column for column in solution)
        for solution in eight
    }
    assert len(reflections) == 92
    assert reflections == solution_set
    print("  all 92 solutions reflect to another solution in the set")

    print("\n--- counting twelve ---")
    assert count_solutions(12) == 14200
    print("  N=12 has 14,200 solutions")

    print("\nAll checks passed.")


if __name__ == "__main__":
    main()
