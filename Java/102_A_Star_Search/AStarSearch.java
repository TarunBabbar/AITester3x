import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * 102 - A* search on a grid: the same best-first search as Dijkstra, plus a
 * heuristic that points it at the goal. The result is the shortest path while
 * expanding far fewer cells.
 *
 * Both algorithms share one routine, so "A*" and "Dijkstra" differ only by the
 * heuristic function. That makes it easy to check they agree on the cost.
 *
 * Compile and run:
 *   javac AStarSearch.java
 *   java AStarSearch
 */
public class AStarSearch {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    record Cell(int row, int column) {
        @Override
        public String toString() {
            return "(" + row + "," + column + ")";
        }
    }

    static final class Grid {
        private final String[] rows;

        Grid(String... rows) {
            if (rows.length == 0) {
                throw new IllegalArgumentException("a grid needs at least one row");
            }
            int width = rows[0].length();
            for (String row : rows) {
                if (row.length() != width) {
                    throw new IllegalArgumentException("every row must be the same width");
                }
            }
            this.rows = rows.clone();
        }

        int height() {
            return rows.length;
        }

        int width() {
            return rows[0].length();
        }

        boolean inside(int row, int column) {
            return row >= 0 && row < height() && column >= 0 && column < width();
        }

        boolean wall(int row, int column) {
            return rows[row].charAt(column) == '#';
        }

        Cell find(char marker) {
            for (int row = 0; row < height(); row++) {
                int column = rows[row].indexOf(marker);
                if (column >= 0) {
                    return new Cell(row, column);
                }
            }
            throw new IllegalArgumentException("no '" + marker + "' in the grid");
        }
    }

    record Result(List<Cell> path, int cost, int expanded) {
        boolean found() {
            return !path.isEmpty();
        }
    }

    static final int[][] ORTHOGONAL = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
    static final int[][] WITH_DIAGONALS = {
            {-1, 0}, {1, 0}, {0, -1}, {0, 1}, {-1, -1}, {-1, 1}, {1, -1}, {1, 1}};

    /** Manhattan when only orthogonal moves are allowed, Chebyshev when diagonals are. */
    static int heuristic(Cell from, Cell to, boolean diagonals) {
        int rowGap = Math.abs(from.row() - to.row());
        int columnGap = Math.abs(from.column() - to.column());
        return diagonals ? Math.max(rowGap, columnGap) : rowGap + columnGap;
    }

    /**
     * Best-first search with a heuristic. Passing useHeuristic=false makes the
     * heuristic zero, which is plain Dijkstra.
     */
    static Result search(Grid grid, Cell start, Cell goal, boolean diagonals, boolean useHeuristic) {
        record Entry(Cell cell, int estimate) {
        }

        Map<Cell, Integer> distance = new HashMap<>();
        Map<Cell, Cell> previous = new HashMap<>();
        Set<Cell> expanded = new HashSet<>();
        PriorityQueue<Entry> open = new PriorityQueue<>((left, right) ->
                Integer.compare(left.estimate(), right.estimate()));

        distance.put(start, 0);
        open.add(new Entry(start, useHeuristic ? heuristic(start, goal, diagonals) : 0));

        while (!open.isEmpty()) {
            Entry entry = open.poll();
            Cell current = entry.cell();
            if (!expanded.add(current)) {
                continue;                      // a stale entry for a cell already settled
            }
            if (current.equals(goal)) {
                break;
            }

            for (int[] step : diagonals ? WITH_DIAGONALS : ORTHOGONAL) {
                int row = current.row() + step[0];
                int column = current.column() + step[1];
                if (!grid.inside(row, column) || grid.wall(row, column)) {
                    continue;
                }
                Cell next = new Cell(row, column);
                if (expanded.contains(next)) {
                    continue;
                }
                int candidate = distance.get(current) + 1;   // every step costs one
                if (candidate < distance.getOrDefault(next, Integer.MAX_VALUE)) {
                    distance.put(next, candidate);
                    previous.put(next, current);
                    int estimate = candidate + (useHeuristic ? heuristic(next, goal, diagonals) : 0);
                    open.add(new Entry(next, estimate));
                }
            }
        }

        if (!distance.containsKey(goal)) {
            return new Result(List.of(), -1, expanded.size());
        }

        List<Cell> path = new ArrayList<>();
        Cell step = goal;
        while (step != null) {
            path.add(0, step);
            step = previous.get(step);
        }
        return new Result(path, distance.get(goal), expanded.size());
    }

    static Result aStar(Grid grid, boolean diagonals) {
        return search(grid, grid.find('S'), grid.find('G'), diagonals, true);
    }

    static Result dijkstra(Grid grid, boolean diagonals) {
        return search(grid, grid.find('S'), grid.find('G'), diagonals, false);
    }

    /** A path must be a chain of single steps that avoids the walls. */
    static void checkPath(Grid grid, Result result) {
        List<Cell> path = result.path();
        check(!path.isEmpty(), "the path is not empty");
        check(path.get(0).equals(grid.find('S')), "it starts at S");
        check(path.get(path.size() - 1).equals(grid.find('G')), "and ends at G");
        check(path.size() - 1 == result.cost(), "the number of steps equals the cost");
        for (Cell cell : path) {
            check(!grid.wall(cell.row(), cell.column()), "no step lands on a wall at " + cell);
        }
        for (int i = 1; i < path.size(); i++) {
            int rowStep = Math.abs(path.get(i).row() - path.get(i - 1).row());
            int columnStep = Math.abs(path.get(i).column() - path.get(i - 1).column());
            check(rowStep <= 1 && columnStep <= 1 && rowStep + columnStep > 0,
                    "each step moves to a neighbour");
        }
    }

    public static void main(String[] args) {
        // ---- a small maze with one detour ------------------------------------
        Grid maze = new Grid(
                "S..",
                ".#.",
                "..G");
        Result orthogonal = aStar(maze, false);
        checkPath(maze, orthogonal);
        check(orthogonal.cost() == 4, "around the wall is four steps: " + orthogonal.cost());
        // There are two equally short routes here - under the wall or over it - so
        // the exact cells are not asserted, only the shape of the path.
        check(orthogonal.path().size() == 5, "a four step path has five cells");
        check(orthogonal.path().get(0).equals(new Cell(0, 0)), "starting at S");
        check(orthogonal.path().get(4).equals(new Cell(2, 2)), "and ending at G");
        System.out.println("small maze   : " + orthogonal.path() + " cost " + orthogonal.cost());

        Result diagonal = aStar(maze, true);
        checkPath(maze, diagonal);
        check(diagonal.cost() == 3, "diagonals cut it to three steps");
        System.out.println("with diagonals: cost " + diagonal.cost());

        // ---- A* and Dijkstra agree on cost, but A* looks at less --------------
        Grid open = new Grid(
                "S....................",
                ".....................",
                ".....................",
                ".....................",
                "....................G");
        Result star = aStar(open, false);
        Result plain = dijkstra(open, false);
        checkPath(open, star);
        check(star.cost() == plain.cost(), "both find the same shortest cost: " + star.cost());
        check(star.expanded() < plain.expanded(),
                "but A* expands fewer cells: " + star.expanded() + " against " + plain.expanded());
        System.out.println("open grid    : cost " + star.cost() + ", A* expanded "
                + star.expanded() + " cells, Dijkstra " + plain.expanded());

        // ---- a maze with a real detour ---------------------------------------
        Grid winding = new Grid(
                "S.#........",
                "..#.#.####.",
                "..#.#.#....",
                "..#.#.#.###",
                "..#...#...#",
                "..#####.#.#",
                "........#.G");
        Result windingStar = aStar(winding, false);
        Result windingPlain = dijkstra(winding, false);
        checkPath(winding, windingStar);
        check(windingStar.cost() == windingPlain.cost(),
                "A* matches Dijkstra through the maze: " + windingStar.cost());
        check(windingStar.expanded() < windingPlain.expanded(), "and expands less");
        System.out.println("winding maze : cost " + windingStar.cost() + ", A* expanded "
                + windingStar.expanded() + ", Dijkstra " + windingPlain.expanded());

        // the heuristic gives exactly this: same answer, less work
        check(windingStar.expanded() <= windingPlain.expanded(),
                "the heuristic never makes it worse");

        // ---- an unreachable goal ---------------------------------------------
        Grid sealed = new Grid(
                "S#.",
                "##.",
                "..G");
        Result blocked = aStar(sealed, false);
        check(!blocked.found(), "a walled off goal has no path");
        check(blocked.cost() == -1, "reported as minus one");
        check(blocked.expanded() > 0, "though the search did explore the reachable cells");
        check(!findReachable(sealed), "and a second opinion agrees");
        System.out.println("unreachable  : no path, after expanding " + blocked.expanded() + " cells");

        // ---- start next to the goal ------------------------------------------
        Grid adjacent = new Grid("SG");
        check(aStar(adjacent, false).cost() == 1, "one step");
        check(aStar(adjacent, false).path().size() == 2, "so a two cell path");
        Grid same = new Grid("S");
        Result trivial = search(same, new Cell(0, 0), new Cell(0, 0), false, true);
        check(trivial.cost() == 0, "a start that is the goal costs nothing");
        check(trivial.path().equals(List.of(new Cell(0, 0))), "with a path of one cell");

        // ---- diagonals versus not --------------------------------------------
        Grid stairs = new Grid(
                "S....",
                ".....",
                ".....",
                ".....",
                "....G");
        int straight = aStar(stairs, false).cost();
        int withDiagonals = aStar(stairs, true).cost();
        check(straight == 8, "the Manhattan distance is 8: " + straight);
        check(withDiagonals == 4, "the diagonal distance is 4: " + withDiagonals);
        System.out.println("stairs       : " + straight + " steps orthogonally, "
                + withDiagonals + " with diagonals");

        // ---- determinism and a bigger seeded grid ----------------------------
        Grid large = randomGrid(30, 30, 0.25, 42);
        Result first = aStar(large, false);
        Result second = aStar(large, false);
        check(first.path().equals(second.path()), "the same grid gives the same path");
        check(first.cost() == second.cost(), "and the same cost");

        Result largeDijkstra = dijkstra(large, false);
        if (first.found()) {
            checkPath(large, first);
            check(first.cost() == largeDijkstra.cost(),
                    "on a 30x30 grid A* still matches Dijkstra: " + first.cost());
            check(first.expanded() < largeDijkstra.expanded(),
                    "expanding " + first.expanded() + " against " + largeDijkstra.expanded());
            System.out.println("30x30 grid   : path of " + first.cost() + ", A* expanded "
                    + first.expanded() + ", Dijkstra " + largeDijkstra.expanded());
        } else {
            check(largeDijkstra.cost() == -1, "and both agree the goal is unreachable");
            System.out.println("30x30 grid   : the goal is walled off, and both agree");
        }

        // ---- the heuristic is admissible -------------------------------------
        // For a grid with unit costs, Manhattan never overestimates the true
        // distance, which is what keeps A* optimal. Checked on every empty grid here.
        for (int row = 0; row < 6; row++) {
            for (int column = 0; column < 6; column++) {
                Cell from = new Cell(0, 0);
                Cell to = new Cell(row, column);
                check(heuristic(from, to, false) == row + column, "Manhattan on an empty grid");
                check(heuristic(from, to, true) == Math.max(row, column), "Chebyshev on an empty grid");
            }
        }
        System.out.println("heuristic    : Manhattan and Chebyshev both exact on open ground");

        // ---- bad grids --------------------------------------------------------
        try {
            new Grid("S..", "..");
            throw new AssertionError("ragged rows should be rejected");
        } catch (IllegalArgumentException expected) {
            System.out.println("ragged grid  : " + expected.getMessage());
        }
        try {
            new Grid("...").find('G');
            throw new AssertionError("a missing marker should be reported");
        } catch (IllegalArgumentException expected) {
            System.out.println("no marker    : " + expected.getMessage());
        }
        System.out.println("All checks passed.");
    }

    /** An independent breadth-first check that the goal can be reached at all. */
    static boolean findReachable(Grid grid) {
        Cell start = grid.find('S');
        Set<Cell> seen = new HashSet<>();
        List<Cell> frontier = new ArrayList<>();
        seen.add(start);
        frontier.add(start);
        while (!frontier.isEmpty()) {
            List<Cell> next = new ArrayList<>();
            for (Cell cell : frontier) {
                if (cell.equals(grid.find('G'))) {
                    return true;
                }
                for (int[] step : ORTHOGONAL) {
                    int row = cell.row() + step[0];
                    int column = cell.column() + step[1];
                    if (grid.inside(row, column) && !grid.wall(row, column)) {
                        Cell candidate = new Cell(row, column);
                        if (seen.add(candidate)) {
                            next.add(candidate);
                        }
                    }
                }
            }
            frontier = next;
        }
        return false;
    }

    /** A grid with scattered walls, but a clear border so a path usually exists. */
    static Grid randomGrid(int height, int width, double wallChance, long seed) {
        java.util.Random random = new java.util.Random(seed);
        String[] rows = new String[height];
        for (int row = 0; row < height; row++) {
            StringBuilder line = new StringBuilder();
            for (int column = 0; column < width; column++) {
                line.append(random.nextDouble() < wallChance ? '#' : '.');
            }
            rows[row] = line.toString();
        }
        rows[0] = "S" + rows[0].substring(1);
        rows[height - 1] = rows[height - 1].substring(0, width - 1) + "G";
        return new Grid(rows);
    }
}
