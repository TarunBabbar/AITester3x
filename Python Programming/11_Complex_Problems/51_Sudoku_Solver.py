"""
Program 51: A Sudoku Solver

Solves a 9x9 Sudoku with bitmask candidates and a most-constrained-cell-first
search, rejects any puzzle that is incomplete or contradictory, counts how many
solutions a puzzle has, and proves each answer by checking the finished grid.

The interesting part is the search order. Filling the grid left to right makes
some puzzles crawl; always filling the empty cell with the fewest legal digits
(the "most constrained cell" rule) collapses the search tree instead. A bitmap
of the digits already used in a row, column and box makes "fewest" cheap, and
lets a cell with no candidates be spotted immediately as a dead end.

The other half is not trusting the solver. Every result is re-checked: the grid
must be complete, every given must survive, a puzzle advertised as unique is
counted and found to have exactly one completion, and a grid is only called
minimal after confirming that removing any single remaining clue would make it
ambiguous. The puzzles themselves are built here, so nothing is assumed.

Concepts: backtracking, bitmasks, MRV heuristics, solution counting, input
           validation, verifying a result rather than trusting it.

    python 51_Sudoku_Solver.py
"""

from __future__ import annotations

from dataclasses import dataclass, field
from random import Random

SIZE = 9
BOX = 3
CELLS = SIZE * SIZE
ALL_DIGITS = 0b1111111110  # bits 1 through 9 set, bit 0 unused
EMPTY_CHARS = ".0_"


def parse(text: str) -> list[int]:
    """Turn a puzzle into 81 digits, 0 for a blank. Whitespace is ignored."""
    cells: list[int] = []
    for char in text:
        if char.isspace():
            continue
        if char in EMPTY_CHARS:
            cells.append(0)
        elif "1" <= char <= "9":
            cells.append(int(char))
        else:
            raise ValueError(f"unexpected character {char!r} in puzzle")
    if len(cells) != CELLS:
        raise ValueError(f"a puzzle needs {CELLS} cells, found {len(cells)}")
    return cells


def format_grid(cells: list[int]) -> str:
    """Render 81 digits as nine rows, blanks shown as dots."""
    if len(cells) != CELLS:
        raise ValueError(f"a grid needs {CELLS} cells, found {len(cells)}")
    lines: list[str] = []
    for row in range(SIZE):
        line = "".join(
            str(cells[row * SIZE + col]) if cells[row * SIZE + col] else "."
            for col in range(SIZE)
        )
        lines.append(line)
    return "\n".join(lines)


@dataclass
class Sudoku:
    """A grid plus the bitmasks that make the search fast."""

    cells: list[int]
    rows: list[int] = field(default_factory=lambda: [0] * SIZE)
    cols: list[int] = field(default_factory=lambda: [0] * SIZE)
    boxes: list[int] = field(default_factory=lambda: [0] * SIZE)
    givens: int = 0
    placements: int = 0
    backtracks: int = 0

    def __post_init__(self) -> None:
        # Copy the caller's list: solving mutates cells, and it must not reach
        # back into whatever grid was handed in.
        self.cells = list(self.cells)
        if len(self.cells) != CELLS:
            raise ValueError(f"a grid needs {CELLS} cells, found {len(self.cells)}")
        for index, value in enumerate(self.cells):
            if value == 0:
                continue
            if not 1 <= value <= 9:
                raise ValueError(f"cell {index} holds {value!r}, which is not a digit 1-9")
            if self.rows[index // SIZE] & (1 << value):
                raise ValueError(f"{value} appears twice in row {index // SIZE + 1}")
            if self.cols[index % SIZE] & (1 << value):
                raise ValueError(f"{value} appears twice in column {index % SIZE + 1}")
            box = self.box_of(index)
            if self.boxes[box] & (1 << value):
                raise ValueError(f"{value} appears twice in box {box + 1}")
            self._place(index, value)
        self.givens = sum(1 for value in self.cells if value)

    @staticmethod
    def box_of(index: int) -> int:
        """Which of the nine 3x3 boxes a flat cell index falls in."""
        row, col = divmod(index, SIZE)
        return (row // BOX) * BOX + col // BOX

    def _place(self, index: int, digit: int) -> None:
        bit = 1 << digit
        self.cells[index] = digit
        self.rows[index // SIZE] |= bit
        self.cols[index % SIZE] |= bit
        self.boxes[self.box_of(index)] |= bit
        self.placements += 1

    def _unplace(self, index: int, digit: int) -> None:
        bit = 1 << digit
        self.cells[index] = 0
        self.rows[index // SIZE] &= ~bit
        self.cols[index % SIZE] &= ~bit
        self.boxes[self.box_of(index)] &= ~bit
        self.placements -= 1

    def candidates(self, index: int) -> int:
        """Bitmask of the digits that can legally go in one cell."""
        used = self.rows[index // SIZE] | self.cols[index % SIZE] | self.boxes[self.box_of(index)]
        return ALL_DIGITS & ~used

    def best_cell(self) -> int | None:
        """The empty cell with the fewest candidates, or None when the grid is full."""
        best_index: int | None = None
        best_count = 10
        for index, value in enumerate(self.cells):
            if value:
                continue
            count = bin(self.candidates(index)).count("1")
            if count < best_count:
                best_index, best_count = index, count
                if count <= 1:
                    break
        return best_index

    def is_solved(self) -> bool:
        """A complete grid where every row, column and box uses all nine digits."""
        return (
            all(self.cells)
            and all(mask == ALL_DIGITS for mask in self.rows)
            and all(mask == ALL_DIGITS for mask in self.cols)
            and all(mask == ALL_DIGITS for mask in self.boxes)
        )

    def solve(self) -> bool:
        """Fill the grid in place, most constrained cell first. True if solved."""
        index = self.best_cell()
        if index is None:
            return True
        mask = self.candidates(index)
        while mask:
            bit = mask & -mask
            mask ^= bit
            digit = bit.bit_length() - 1
            self._place(index, digit)
            if self.solve():
                return True
            self._unplace(index, digit)
            self.backtracks += 1
        return False

    def count_solutions(self, limit: int = 2) -> int:
        """How many completions exist, stopping as soon as `limit` have been found."""
        index = self.best_cell()
        if index is None:
            return 1
        mask = self.candidates(index)
        found = 0
        while mask and found < limit:
            bit = mask & -mask
            mask ^= bit
            digit = bit.bit_length() - 1
            self._place(index, digit)
            found += self.count_solutions(limit - found)
            self._unplace(index, digit)
        return found


def dig(solution: list[int], random: Random) -> list[int]:
    """Strip clues from a solved grid while it keeps exactly one completion.

    Removing a clue can only add solutions, never remove them, so once removing
    a clue makes the puzzle ambiguous it will stay ambiguous however much else
    is later stripped. The result is therefore minimal: no remaining clue can
    be dropped.
    """
    puzzle = list(solution)
    order = list(range(CELLS))
    random.shuffle(order)
    for index in order:
        removed = puzzle[index]
        puzzle[index] = 0
        if Sudoku(puzzle).count_solutions(limit=2) != 1:
            puzzle[index] = removed
    return puzzle


WIKIPEDIA = """
53..7....
6..195...
.98....6.
8...6...3
4..8.3..1
7...2...6
.6....28.
...419..5
....8..79
"""

# One blank cell (top-left) that no digit can fill, because 1-8 sit in its row
# and 9 sits at the top of its column. Legal clues, no solution.
UNSOLVABLE = """
.12345678
9........
.........
.........
.........
.........
.........
.........
.........
"""


def main() -> None:
    # ---- parsing and formatting round trip ----------------------------------
    cells = parse(WIKIPEDIA)
    assert format_grid(cells) == format_grid(parse(format_grid(cells))), "parse and format agree"
    assert parse("0" * CELLS) == [0] * CELLS, "zeros are blanks"
    assert parse("." * CELLS) == [0] * CELLS, "and so are dots"
    assert parse("_" * CELLS) == [0] * CELLS, "and underscores"
    assert len(format_grid(cells).splitlines()) == SIZE, "a grid prints as nine rows"
    print(f"parsing      : {CELLS} cells, {len(format_grid(cells).splitlines())} rows round-tripped")

    # ---- validation ---------------------------------------------------------
    for bad, why in (("1" * 10, "too few cells"), ("x" + "." * (CELLS - 1), "a stray character")):
        try:
            parse(bad)
            raise AssertionError(f"{why} should have been rejected")
        except ValueError as error:
            print(f"validation   : {error}")

    try:
        Sudoku(parse("11" + "." * (CELLS - 2)))
        raise AssertionError("a repeated digit in a row should have been rejected")
    except ValueError as error:
        print(f"validation   : {error}")

    try:
        Sudoku([10] + [0] * (CELLS - 1))
        raise AssertionError("a digit out of range should have been rejected")
    except ValueError as error:
        print(f"validation   : {error}")

    assert Sudoku.box_of(0) == 0 and Sudoku.box_of(8) == 2 and Sudoku.box_of(80) == 8, "box numbering"
    assert Sudoku.box_of(30) == 4, "the centre cell is in the centre box"
    print("boxes        : the nine 3x3 boxes are numbered left to right, top to bottom")

    # ---- the candidates of a givens-free grid are all nine digits -----------
    blank = Sudoku([0] * CELLS)
    assert blank.candidates(40) == ALL_DIGITS, "an empty grid offers every digit"
    assert bin(blank.candidates(40)).count("1") == 9, "all nine of them"
    assert blank.best_cell() == 0, "with every cell tied, the first is chosen"
    print("candidates   : an untouched cell holds all nine digits")

    # ---- solving the classic puzzle -----------------------------------------
    puzzle = parse(WIKIPEDIA)
    board = Sudoku(puzzle)
    assert board.givens == 30, f"the puzzle has 30 clues, counted {board.givens}"
    assert board.count_solutions(limit=2) == 1, "and exactly one solution"
    assert board.cells == puzzle, "counting a solution leaves the grid untouched"

    assert board.solve(), "the puzzle is solvable"
    assert board.is_solved(), "and the finished grid is complete and legal"
    for index, value in enumerate(puzzle):
        if value:
            assert board.cells[index] == value, f"clue at cell {index} was overwritten"
    solution = list(board.cells)
    print("solving      : 30 clues in, a full grid out")
    print(format_grid(solution))
    print(f"effort       : {board.placements} placements after {board.backtracks} backtracks")

    # the solver's answer survives an independent check of every row, column and box
    for row in range(SIZE):
        assert sorted(solution[row * SIZE + col] for col in range(SIZE)) == list(range(1, 10))
    for col in range(SIZE):
        assert sorted(solution[row * SIZE + col] for row in range(SIZE)) == list(range(1, 10))
    for box in range(SIZE):
        indices = [i for i in range(CELLS) if Sudoku.box_of(i) == box]
        assert sorted(solution[i] for i in indices) == list(range(1, 10))
    print("checking     : every row, column and box holds 1-9 exactly once")

    # ---- an unsolvable grid is reported, not looped over --------------------
    stuck = Sudoku(parse(UNSOLVABLE))
    assert stuck.best_cell() is not None, "there is still an empty cell"
    assert bin(stuck.candidates(0)).count("1") == 0, "with no digit it can take"
    assert stuck.count_solutions(limit=2) == 0, "so there are no solutions"
    assert not stuck.solve(), "and the solver says so"
    print("unsolvable   : a cell with no candidates ends the search immediately")

    # ---- ambiguity is measured, not assumed ---------------------------------
    empty = Sudoku([0] * CELLS)
    assert empty.count_solutions(limit=2) == 2, "an empty grid has far more than one solution"
    assert empty.count_solutions(limit=1000) >= 1000, "and the limit caps how many are counted"
    assert empty.solve() and empty.is_solved(), "yet it still has a completion"
    print("ambiguous    : an empty grid reports many solutions and still yields one")

    # ---- digging a puzzle out of the answer ---------------------------------
    random = Random(20261104)
    minimal = dig(solution, random)
    removed = CELLS - sum(1 for value in minimal if value)
    assert removed > 0, "digging should remove at least one clue"
    assert removed < 81, "and leave at least one behind"

    dug = Sudoku(list(minimal))
    assert dug.count_solutions(limit=2) == 1, "the dug puzzle still has one solution"
    assert dug.solve() and dug.is_solved(), "and it solves to a legal grid"
    assert dug.cells == solution, "to the very grid it was carved from"
    print(f"digging      : {len(solution)} clues reduced to {81 - removed}, all still giving one answer")

    # a minimal puzzle: no single remaining clue can be removed
    redundant = 0
    for index, value in enumerate(minimal):
        if not value:
            continue
        trial = list(minimal)
        trial[index] = 0
        if Sudoku(trial).count_solutions(limit=2) == 1:
            redundant += 1
    assert redundant == 0, f"{redundant} clues could still be removed"
    print("minimality   : removing any single clue makes it ambiguous")

    # ---- the choosing order actually matters --------------------------------
    board = Sudoku(puzzle)
    hardest = min(
        (bin(board.candidates(i)).count("1"), i)
        for i in range(CELLS)
        if not board.cells[i]
    )
    assert hardest[0] <= 9 and not board.cells[hardest[1]], "the tightest cell is a real empty cell"
    assert board.count_solutions(limit=2) == 1, "and looking at it changes nothing"
    noun = "option" if hardest[0] == 1 else "options"
    print(f"heuristic    : the tightest empty cell ({hardest[1]}) has only {hardest[0]} {noun}")

    print("All checks passed.")


if __name__ == "__main__":
    main()
