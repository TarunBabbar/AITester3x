import java.util.Arrays;

/**
 * 92 - A Sudoku solver: bitmask constraint tracking, then a backtracking search
 * that always fills the most constrained cell first. It can also count
 * solutions, so a puzzle can be shown to be well posed with exactly one answer.
 *
 * Compile and run:
 *   javac SudokuSolver.java
 *   java SudokuSolver
 */
public class SudokuSolver {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static final int SIZE = 9;
    static final int ALL_DIGITS = 0b1111111110;   // bits 1 through 9

    static final class Sudoku {
        private final int[][] grid = new int[SIZE][SIZE];
        private final int[] rowMask = new int[SIZE];
        private final int[] columnMask = new int[SIZE];
        private final int[] boxMask = new int[SIZE];
        private int solutions;
        private int limit = 1;
        private int[][] solution;

        static Sudoku parse(String text) {
            String symbols = text.replaceAll("[^0-9.]", "");
            if (symbols.length() != SIZE * SIZE) {
                throw new IllegalArgumentException("a puzzle needs 81 cells, got " + symbols.length());
            }
            Sudoku sudoku = new Sudoku();
            for (int index = 0; index < symbols.length(); index++) {
                char symbol = symbols.charAt(index);
                if (symbol == '.' || symbol == '0') {
                    continue;
                }
                int row = index / SIZE;
                int column = index % SIZE;
                int digit = symbol - '0';
                int bit = 1 << digit;
                if ((sudoku.rowMask[row] & bit) != 0
                        || (sudoku.columnMask[column] & bit) != 0
                        || (sudoku.boxMask[box(row, column)] & bit) != 0) {
                    throw new IllegalArgumentException(
                            "the puzzle already breaks the rules at row " + row + ", column " + column);
                }
                sudoku.place(row, column, digit);
            }
            return sudoku;
        }

        private static int box(int row, int column) {
            return row / 3 * 3 + column / 3;
        }

        private void place(int row, int column, int digit) {
            int bit = 1 << digit;
            grid[row][column] = digit;
            rowMask[row] |= bit;
            columnMask[column] |= bit;
            boxMask[box(row, column)] |= bit;
        }

        private void remove(int row, int column) {
            int digit = grid[row][column];
            int bit = 1 << digit;
            grid[row][column] = 0;
            rowMask[row] &= ~bit;
            columnMask[column] &= ~bit;
            boxMask[box(row, column)] &= ~bit;
        }

        private int candidatesAt(int row, int column) {
            return ~(rowMask[row] | columnMask[column] | boxMask[box(row, column)]) & ALL_DIGITS;
        }

        int emptyCells() {
            int count = 0;
            for (int row = 0; row < SIZE; row++) {
                for (int column = 0; column < SIZE; column++) {
                    if (grid[row][column] == 0) {
                        count++;
                    }
                }
            }
            return count;
        }

        /** Solve in place, returning true when a complete grid was found. */
        boolean solve() {
            limit = 1;
            solutions = 0;
            solution = null;
            search();
            if (solution != null) {
                // The search undoes its choices on the way out, so the recorded
                // answer is what gets put back into the grid.
                for (int row = 0; row < SIZE; row++) {
                    for (int column = 0; column < SIZE; column++) {
                        grid[row][column] = solution[row][column];
                    }
                }
                rebuildMasks();
            }
            return solutions > 0;
        }

        /** Count solutions, stopping at the cap so "more than one" stays cheap. */
        int countSolutions(int cap) {
            limit = Math.max(1, cap);
            solutions = 0;
            solution = null;
            search();
            return solutions;
        }

        private void rebuildMasks() {
            Arrays.fill(rowMask, 0);
            Arrays.fill(columnMask, 0);
            Arrays.fill(boxMask, 0);
            for (int row = 0; row < SIZE; row++) {
                for (int column = 0; column < SIZE; column++) {
                    int digit = grid[row][column];
                    if (digit != 0) {
                        int bit = 1 << digit;
                        rowMask[row] |= bit;
                        columnMask[column] |= bit;
                        boxMask[box(row, column)] |= bit;
                    }
                }
            }
        }

        private void search() {
            if (solutions >= limit) {
                return;
            }

            // Most constrained cell first prunes far more than scanning in order.
            int bestRow = -1;
            int bestColumn = -1;
            int bestCount = SIZE + 1;
            for (int row = 0; row < SIZE; row++) {
                for (int column = 0; column < SIZE; column++) {
                    if (grid[row][column] != 0) {
                        continue;
                    }
                    int count = Integer.bitCount(candidatesAt(row, column));
                    if (count < bestCount) {
                        bestCount = count;
                        bestRow = row;
                        bestColumn = column;
                    }
                }
            }

            if (bestRow == -1) {
                solutions++;                 // nothing empty: this is a complete grid
                if (solution == null) {
                    solution = new int[SIZE][SIZE];
                    for (int row = 0; row < SIZE; row++) {
                        solution[row] = grid[row].clone();
                    }
                }
                return;
            }
            if (bestCount == 0) {
                return;                      // no candidate fits, so this branch is dead
            }

            int mask = candidatesAt(bestRow, bestColumn);
            for (int digit = 1; digit <= SIZE && solutions < limit; digit++) {
                if ((mask & (1 << digit)) == 0) {
                    continue;
                }
                place(bestRow, bestColumn, digit);
                search();
                remove(bestRow, bestColumn);
            }
        }

        String render() {
            StringBuilder out = new StringBuilder();
            for (int row = 0; row < SIZE; row++) {
                for (int column = 0; column < SIZE; column++) {
                    out.append((char) ('0' + grid[row][column]));
                }
                if (row < SIZE - 1) {
                    out.append('\n');
                }
            }
            return out.toString();
        }
    }

    /** Every row, column and box holds 1 to 9 exactly once. */
    static boolean isSolved(String grid) {
        String digits = grid.replaceAll("[^0-9]", "");
        if (digits.length() != 81 || digits.contains("0")) {
            return false;
        }
        for (int unit = 0; unit < SIZE; unit++) {
            int rowMask = 0;
            int columnMask = 0;
            int boxMask = 0;
            for (int offset = 0; offset < SIZE; offset++) {
                rowMask |= 1 << (digits.charAt(unit * SIZE + offset) - '0');
                columnMask |= 1 << (digits.charAt(offset * SIZE + unit) - '0');
                int boxRow = unit / 3 * 3 + offset / 3;
                int boxColumn = unit % 3 * 3 + offset % 3;
                boxMask |= 1 << (digits.charAt(boxRow * SIZE + boxColumn) - '0');
            }
            if (rowMask != ALL_DIGITS || columnMask != ALL_DIGITS || boxMask != ALL_DIGITS) {
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) {
        // the puzzle used in most textbooks, with its unique answer
        String puzzle = """
                530070000
                600195000
                098000060
                800060003
                400803001
                700020006
                060000280
                000419005
                000080079""";
        String answer = """
                534678912
                672195348
                198342567
                859761423
                426853791
                713924856
                961537284
                287419635
                345286179""";

        Sudoku sudoku = Sudoku.parse(puzzle);
        int blanks = sudoku.emptyCells();
        check(blanks == 51, "the puzzle starts with 51 empty cells");
        check(blanks + 30 == 81, "so 30 clues were given");
        check(!isSolved(sudoku.render()), "and it is not solved yet");

        check(sudoku.solve(), "it can be solved");
        check(sudoku.emptyCells() == 0, "the grid is full");
        check(isSolved(sudoku.render()), "the answer obeys every rule");
        check(sudoku.render().equals(answer), "and matches the known solution");
        System.out.println("solved       :\n" + sudoku.render());

        // ---- the answer is unique --------------------------------------------
        check(Sudoku.parse(puzzle).countSolutions(2) == 1, "the puzzle has exactly one solution");
        System.out.println("unique       : the search stopped at the second solution, and there was none");

        // ---- a sparse puzzle: many solutions ---------------------------------
        Sudoku sparse = Sudoku.parse("." + "0".repeat(80));
        check(sparse.countSolutions(2) == 2, "an empty grid has more than one answer, so the cap stops it");
        check(sparse.solve(), "and it can still produce one");

        // ---- puzzles that break the rules are rejected on the way in ---------
        String duplicateRow = "55" + ".".repeat(79);
        try {
            Sudoku.parse(duplicateRow);
            throw new AssertionError("two 5s in the first row should be rejected");
        } catch (IllegalArgumentException expected) {
            System.out.println("bad row      : " + expected.getMessage());
        }

        String duplicateBox = "5" + ".".repeat(9) + "5" + ".".repeat(70);
        try {
            Sudoku.parse(duplicateBox);
            throw new AssertionError("two 5s in the first box should be rejected");
        } catch (IllegalArgumentException expected) {
            System.out.println("bad box      : " + expected.getMessage());
        }

        try {
            Sudoku.parse("123");
            throw new AssertionError("a short puzzle should be rejected");
        } catch (IllegalArgumentException expected) {
            System.out.println("short puzzle : " + expected.getMessage());
        }

        // ---- a well formed puzzle with no solution ---------------------------
        // Row 0 is missing only a 9, but column 8 already has one further down.
        StringBuilder impossible = new StringBuilder();
        for (int row = 0; row < SIZE; row++) {
            for (int column = 0; column < SIZE; column++) {
                if (row == 0 && column < 8) {
                    impossible.append((char) ('1' + column));
                } else if (row == 5 && column == 8) {
                    impossible.append('9');
                } else {
                    impossible.append('.');
                }
            }
        }
        check(Sudoku.parse(impossible.toString()).emptyCells() == 72, "it parses with 72 blanks");
        check(!Sudoku.parse(impossible.toString()).solve(), "but it cannot be completed");
        check(Sudoku.parse(impossible.toString()).countSolutions(2) == 0, "and it has no solution at all");
        System.out.println("no solution  : well formed, but the search finds nothing");

        // ---- a hard puzzle ---------------------------------------------------
        String hard = """
                000000010
                400000000
                020000000
                000050407
                008000300
                001090000
                300400200
                050100000
                000806000""";
        Sudoku hardSudoku = Sudoku.parse(hard);
        check(hardSudoku.solve(), "the hard puzzle is solved");
        check(isSolved(hardSudoku.render()), "and the answer is valid");
        System.out.println("hard         : solved, first row " + hardSudoku.render().substring(0, 9));

        // ---- a finished grid is left alone -----------------------------------
        Sudoku finished = Sudoku.parse(answer);
        check(finished.emptyCells() == 0, "nothing is empty");
        check(finished.solve(), "solving a finished grid succeeds trivially");
        check(finished.render().equals(answer), "and changes nothing");
        System.out.println("All checks passed.");
    }
}
