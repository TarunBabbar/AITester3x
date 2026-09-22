import java.util.ArrayList;
import java.util.List;

/**
 * 24 - Backtracking: N-Queens, permutations, subset sum and a maze solver.
 * Each one builds a candidate, checks it, and undoes the choice if it fails.
 *
 * Compile and run:
 *   javac BacktrackingAlgorithms.java
 *   java BacktrackingAlgorithms
 */
public class BacktrackingAlgorithms {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    // ------------------------------------------------------------ N-Queens
    static int queenSolutions(int size) {
        return placeQueens(new int[size], 0, size);
    }

    private static int placeQueens(int[] columns, int row, int size) {
        if (row == size) {
            return 1;
        }
        int solutions = 0;
        for (int column = 0; column < size; column++) {
            if (isSafe(columns, row, column)) {
                columns[row] = column;                              // choose
                solutions += placeQueens(columns, row + 1, size);   // explore
                columns[row] = -1;                                  // un-choose
            }
        }
        return solutions;
    }

    private static boolean isSafe(int[] columns, int row, int column) {
        for (int earlier = 0; earlier < row; earlier++) {
            if (columns[earlier] == column
                    || Math.abs(columns[earlier] - column) == Math.abs(earlier - row)) {
                return false;   // same column, or same diagonal
            }
        }
        return true;
    }

    static List<Integer> firstQueenSolution(int size) {
        int[] columns = new int[size];
        if (searchFirst(columns, 0, size)) {
            List<Integer> result = new ArrayList<>();
            for (int column : columns) {
                result.add(column);
            }
            return result;
        }
        return List.of();
    }

    private static boolean searchFirst(int[] columns, int row, int size) {
        if (row == size) {
            return true;
        }
        for (int column = 0; column < size; column++) {
            if (isSafe(columns, row, column)) {
                columns[row] = column;
                if (searchFirst(columns, row + 1, size)) {
                    return true;
                }
                columns[row] = -1;
            }
        }
        return false;
    }

    // -------------------------------------------------------- permuations
    static List<List<Integer>> permutations(int[] values) {
        List<List<Integer>> out = new ArrayList<>();
        permute(values, 0, out);
        return out;
    }

    private static void permute(int[] values, int index, List<List<Integer>> out) {
        if (index == values.length) {
            List<Integer> snapshot = new ArrayList<>();
            for (int value : values) {
                snapshot.add(value);
            }
            out.add(snapshot);
            return;
        }
        for (int i = index; i < values.length; i++) {
            swap(values, index, i);
            permute(values, index + 1, out);
            swap(values, index, i);   // restore
        }
    }

    private static void swap(int[] values, int a, int b) {
        int temporary = values[a];
        values[a] = values[b];
        values[b] = temporary;
    }

    // --------------------------------------------------------- subset sum
    static boolean subsetSum(int[] values, int target) {
        return canReach(values, 0, target);
    }

    private static boolean canReach(int[] values, int index, int remaining) {
        if (remaining == 0) {
            return true;
        }
        if (remaining < 0 || index == values.length) {
            return false;
        }
        // take it, or leave it
        return canReach(values, index + 1, remaining - values[index])
                || canReach(values, index + 1, remaining);
    }

    // --------------------------------------------------------------- maze
    // 0 = open, 1 = wall. Walk from top-left to bottom-right.
    static int[][] MAZE = {
            {0, 0, 0, 1, 0},
            {1, 1, 0, 1, 0},
            {0, 0, 0, 0, 0},
            {0, 1, 1, 1, 1},
            {0, 0, 0, 0, 0},
    };

    static List<int[]> solveMaze(int[][] maze) {
        List<int[]> path = new ArrayList<>();
        if (walk(maze, 0, 0, path)) {
            return path;
        }
        return List.of();
    }

    private static boolean walk(int[][] maze, int row, int column, List<int[]> path) {
        if (row < 0 || column < 0 || row >= maze.length || column >= maze[0].length
                || maze[row][column] != 0) {
            return false;   // off the grid, or already visited / a wall
        }
        path.add(new int[] {row, column});
        if (row == maze.length - 1 && column == maze[0].length - 1) {
            return true;
        }
        maze[row][column] = 2;   // mark as visited
        if (walk(maze, row + 1, column, path) || walk(maze, row, column + 1, path)
                || walk(maze, row - 1, column, path) || walk(maze, row, column - 1, path)) {
            return true;
        }
        path.remove(path.size() - 1);   // dead end - un-choose
        return false;
    }

    public static void main(String[] args) {
        check(queenSolutions(4) == 2, "4-queens has 2 solutions");
        check(queenSolutions(6) == 4, "6-queens has 4 solutions");
        check(queenSolutions(8) == 92, "8-queens has 92 solutions");
        System.out.println("N-Queens     : n=4 -> 2, n=6 -> 4, n=8 -> 92");
        System.out.println("first n=6    : columns " + firstQueenSolution(6));

        List<List<Integer>> permutations = permutations(new int[] {1, 2, 3});
        check(permutations.size() == 6, "three items give six permutations");
        check(permutations.get(0).equals(List.of(1, 2, 3)), "the identity permutation comes first");
        System.out.println("permutations : " + permutations.size() + " of {1,2,3}");

        int[] values = {3, 34, 4, 12, 5, 2};
        check(subsetSum(values, 9), "9 is reachable (4 + 5)");
        check(!subsetSum(values, 30), "30 is not reachable");
        System.out.println("subset sum   : 9 -> reachable, 30 -> not reachable");

        List<int[]> path = solveMaze(MAZE);
        check(!path.isEmpty(), "the maze has a route");
        int[] last = path.get(path.size() - 1);
        check(last[0] == 4 && last[1] == 4, "the route ends bottom-right");
        System.out.println("maze route   : " + path.size() + " cells, ends at ("
                + last[0] + "," + last[1] + ")");
        System.out.println("All checks passed.");
    }
}
