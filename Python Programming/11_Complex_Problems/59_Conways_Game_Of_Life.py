"""
Program 59: Conway's Game of Life

An infinite grid of cells, stored as the set of live coordinates. Each
generation applies the four rules: a live cell with two or three neighbours
survives, a dead cell with exactly three neighbours is born, everything else
dies. Only cells next to live ones can change, so the work scales with the
population rather than the plane.

Concepts: sets, cellular automata, simulation, test-driven patterns.
"""

from __future__ import annotations

import random
from collections.abc import Iterable

Cell = tuple[int, int]


class Life:
    def __init__(self, alive: Iterable[Cell] = ()) -> None:
        self._alive: set[Cell] = set(alive)
        self.generation = 0

    @classmethod
    def parse(cls, rows: list[str], live: str = "#", dead: str = ".") -> "Life":
        cells = {
            (row, column)
            for row, line in enumerate(rows)
            for column, character in enumerate(line)
            if character == live
        }
        return cls(cells)

    @property
    def alive(self) -> set[Cell]:
        return set(self._alive)

    @property
    def population(self) -> int:
        return len(self._alive)

    def is_alive(self, row: int, column: int) -> bool:
        return (row, column) in self._alive

    def neighbours(self, row: int, column: int) -> int:
        return sum(
            (row + dr, column + dc) in self._alive
            for dr in (-1, 0, 1)
            for dc in (-1, 0, 1)
            if dr != 0 or dc != 0
        )

    def step(self) -> "Life":
        counts: dict[Cell, int] = {}

        for row, column in self._alive:
            for dr in (-1, 0, 1):
                for dc in (-1, 0, 1):
                    if dr == 0 and dc == 0:
                        continue
                    cell = (row + dr, column + dc)
                    counts[cell] = counts.get(cell, 0) + 1

        next_alive = {
            cell
            for cell, count in counts.items()
            if count == 3 or (count == 2 and cell in self._alive)
        }

        self._alive = next_alive
        self.generation += 1
        return self

    def run(self, generations: int) -> "Life":
        for _ in range(generations):
            self.step()
        return self

    def to_rows(self, height: int, width: int, live: str = "#", dead: str = ".") -> list[str]:
        return [
            "".join(live if (row, column) in self._alive else dead for column in range(width))
            for row in range(height)
        ]

    def bounding_box(self) -> tuple[int, int, int, int] | None:
        if not self._alive:
            return None
        rows = [row for row, _ in self._alive]
        columns = [column for _, column in self._alive]
        return min(rows), min(columns), max(rows), max(columns)


def brute_step(alive: set[Cell]) -> set[Cell]:
    """Independent implementation that scans a bounding box."""
    if not alive:
        return set()

    rows = [row for row, _ in alive]
    columns = [column for _, column in alive]
    result: set[Cell] = set()

    for row in range(min(rows) - 1, max(rows) + 2):
        for column in range(min(columns) - 1, max(columns) + 2):
            count = sum(
                (row + dr, column + dc) in alive
                for dr in (-1, 0, 1)
                for dc in (-1, 0, 1)
                if dr != 0 or dc != 0
            )
            if count == 3 or (count == 2 and (row, column) in alive):
                result.add((row, column))

    return result


def main() -> None:
    print("--- blinker oscillates ---")
    blinker = Life([(1, 0), (1, 1), (1, 2)])
    original = blinker.alive
    print("start:", sorted(original))
    blinker.step()
    print("after one step:", sorted(blinker.alive))
    assert blinker.alive == {(0, 1), (1, 1), (2, 1)}
    blinker.step()
    assert blinker.alive == original
    assert blinker.generation == 2
    print("after two steps: back to the start")

    print("\n--- block is a still life ---")
    block = Life([(0, 0), (0, 1), (1, 0), (1, 1)])
    block.run(10)
    assert block.alive == {(0, 0), (0, 1), (1, 0), (1, 1)}
    print("four cells survived ten generations unchanged")

    print("\n--- glider moves diagonally ---")
    glider_cells = {(0, 1), (1, 2), (2, 0), (2, 1), (2, 2)}
    glider = Life(glider_cells)
    glider.run(4)
    shifted = {(row + 1, column + 1) for row, column in glider_cells}
    print("after four generations:", sorted(glider.alive))
    assert glider.alive == shifted
    assert glider.generation == 4
    print("the glider translated by (1, 1)")

    print("\n--- rules in isolation ---")
    lonely = Life([(0, 0)])
    lonely.step()
    assert lonely.population == 0, "a lone cell dies of underpopulation"

    overpopulated = Life([(0, 0), (0, 1), (0, -1), (1, 0), (-1, 0)])
    overpopulated.step()
    assert not overpopulated.is_alive(0, 0), "a cell with four neighbours dies"

    birth_cells = {(0, 0), (0, 2), (1, 1)}
    birth = Life(birth_cells)
    expected_birth = brute_step(birth_cells)
    birth.step()
    print("birth pattern:", sorted(birth.alive))
    assert birth.alive == expected_birth
    assert birth.is_alive(0, 1), "a dead cell with three neighbours is born"
    print("underpopulation, overpopulation, and birth behave")

    print("\n--- empty grid ---")
    empty = Life()
    empty.run(5)
    assert empty.population == 0
    assert empty.generation == 5
    assert empty.bounding_box() is None
    print("nothing stays nothing for five generations")

    print("\n--- parsing and printing ---")
    rows = [
        "..........",
        "..#.......",
        "...#......",
        ".###......",
        "..........",
    ]
    parsed = Life.parse(rows)
    assert parsed.population == 5
    assert parsed.is_alive(2, 3)
    assert parsed.to_rows(5, 10) == rows
    print("parse and to_rows round-trip")

    print("\n--- neighbours ---")
    cluster = Life([(1, 1), (1, 2), (2, 1)])
    assert cluster.neighbours(1, 1) == 2
    assert cluster.neighbours(2, 2) == 3
    assert cluster.neighbours(5, 5) == 0
    print("neighbour counts line up")

    print("\n--- against an independent implementation ---")
    rng = random.Random(59)
    checked = 0

    for _ in range(100):
        alive = {
            (rng.randrange(8), rng.randrange(8))
            for _ in range(rng.randint(0, 20))
        }
        life = Life(alive)
        expected = set(alive)

        for _ in range(5):
            life.step()
            expected = brute_step(expected)
            assert life.alive == expected, f"mismatch for {sorted(alive)}"

        checked += 1

    print(f"  {checked} random grids agree with the bounding-box scan")

    print("\n--- generation counter and run ---")
    counted = Life([(0, 0), (0, 1), (0, 2)])
    counted.run(3)
    assert counted.generation == 3
    stepwise = Life([(0, 0), (0, 1), (0, 2)])
    for _ in range(3):
        stepwise.step()
    assert counted.alive == stepwise.alive
    print("run() matches repeated step() calls")

    print("\n--- population changes ---")
    life = Life([(1, 0), (1, 1), (1, 2)])
    populations = []
    for _ in range(4):
        life.step()
        populations.append(life.population)
    print("populations over four generations:", populations)
    assert populations == [3, 3, 3, 3], "the blinker keeps its population"

    print("\nAll checks passed.")


if __name__ == "__main__":
    main()
