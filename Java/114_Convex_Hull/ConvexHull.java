import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

/**
 * 114 - Convex hull with the monotone chain.
 *
 * Sorts the points, builds the lower and upper chains with a stack that pops
 * while the last turn is not a strict left turn, and joins them. Collinear
 * points in the middle of an edge are dropped, so the result is the minimal
 * strictly convex polygon in counter-clockwise order, starting at the
 * lexicographically smallest point.
 *
 * Gift wrapping is implemented as an independent check, along with containment
 * and strict-convexity tests on every hull produced from random points.
 *
 * Compile and run:
 *   javac ConvexHull.java
 *   java ConvexHull
 */
public class ConvexHull {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static final class Point implements Comparable<Point> {
        final long x;
        final long y;

        Point(long x, long y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public int compareTo(Point other) {
            int byX = Long.compare(x, other.x);
            return byX != 0 ? byX : Long.compare(y, other.y);
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Point)) {
                return false;
            }
            Point point = (Point) other;
            return x == point.x && y == point.y;
        }

        @Override
        public int hashCode() {
            return Long.hashCode(x) * 31 + Long.hashCode(y);
        }

        @Override
        public String toString() {
            return "(" + x + ", " + y + ")";
        }
    }

    /** Positive when origin -> first -> second turns left. */
    static long cross(Point origin, Point first, Point second) {
        return (first.x - origin.x) * (second.y - origin.y)
                - (first.y - origin.y) * (second.x - origin.x);
    }

    static long distanceSquared(Point first, Point second) {
        long dx = first.x - second.x;
        long dy = first.y - second.y;
        return dx * dx + dy * dy;
    }

    static List<Point> uniqueSorted(Point[] points) {
        TreeSet<Point> unique = new TreeSet<>();
        for (Point point : points) {
            unique.add(point);
        }
        return new ArrayList<>(unique);
    }

    static List<Point> monotoneChain(Point[] points) {
        List<Point> sorted = uniqueSorted(points);
        if (sorted.size() <= 2) {
            return sorted;
        }

        List<Point> lower = new ArrayList<>();
        for (Point point : sorted) {
            while (lower.size() >= 2
                    && cross(lower.get(lower.size() - 2), lower.get(lower.size() - 1), point) <= 0) {
                lower.remove(lower.size() - 1);
            }
            lower.add(point);
        }

        List<Point> upper = new ArrayList<>();
        for (int index = sorted.size() - 1; index >= 0; index--) {
            Point point = sorted.get(index);
            while (upper.size() >= 2
                    && cross(upper.get(upper.size() - 2), upper.get(upper.size() - 1), point) <= 0) {
                upper.remove(upper.size() - 1);
            }
            upper.add(point);
        }

        lower.remove(lower.size() - 1);
        upper.remove(upper.size() - 1);
        lower.addAll(upper);
        return lower;
    }

    static List<Point> giftWrap(Point[] points) {
        List<Point> unique = uniqueSorted(points);
        if (unique.size() <= 2) {
            return unique;
        }

        Point start = unique.get(0);
        List<Point> hull = new ArrayList<>();
        Point current = start;

        do {
            hull.add(current);
            Point next = null;
            for (Point candidate : unique) {
                if (candidate.equals(current)) {
                    continue;
                }
                if (next == null) {
                    next = candidate;
                    continue;
                }
                long turn = cross(current, next, candidate);
                if (turn < 0 || (turn == 0 && distanceSquared(current, candidate) > distanceSquared(current, next))) {
                    next = candidate;
                }
            }
            current = next;
        } while (current != null && !current.equals(start));

        return hull;
    }

    /** Every input point lies on or to the left of every hull edge. */
    static boolean containsAll(List<Point> hull, Point[] points) {
        if (hull.isEmpty()) {
            return points.length == 0;
        }
        if (hull.size() == 1) {
            for (Point point : points) {
                if (!point.equals(hull.get(0))) {
                    return false;
                }
            }
            return true;
        }
        if (hull.size() == 2) {
            Point first = hull.get(0);
            Point second = hull.get(1);
            for (Point point : points) {
                if (cross(first, second, point) != 0) {
                    return false;
                }
            }
            return true;
        }

        for (int index = 0; index < hull.size(); index++) {
            Point from = hull.get(index);
            Point to = hull.get((index + 1) % hull.size());
            for (Point point : points) {
                if (cross(from, to, point) < 0) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Strictly left turns all the way round. */
    static boolean isStrictlyConvex(List<Point> hull) {
        if (hull.size() < 3) {
            return true;
        }
        for (int index = 0; index < hull.size(); index++) {
            Point first = hull.get(index);
            Point second = hull.get((index + 1) % hull.size());
            Point third = hull.get((index + 2) % hull.size());
            if (cross(first, second, third) <= 0) {
                return false;
            }
        }
        return true;
    }

    static boolean allOnHull(List<Point> hull, Point[] points) {
        for (Point point : hull) {
            boolean found = false;
            for (Point candidate : points) {
                if (candidate.equals(point)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) {
        // ---- a square with an interior point ---------------------------------
        Point[] square = {
            new Point(0, 0),
            new Point(2, 0),
            new Point(2, 2),
            new Point(0, 2),
            new Point(1, 1),
        };
        List<Point> squareHull = monotoneChain(square);
        check(squareHull.size() == 4, "square hull has four corners: " + squareHull);
        check(squareHull.equals(giftWrap(square)), "both algorithms agree on the square");
        check(containsAll(squareHull, square), "the interior point is inside");
        check(isStrictlyConvex(squareHull), "the square is convex");
        System.out.println("square       : " + squareHull);

        // ---- collinear points are dropped -----------------------------------
        Point[] line = {new Point(0, 0), new Point(1, 1), new Point(2, 2), new Point(3, 3)};
        List<Point> lineHull = monotoneChain(line);
        check(lineHull.size() == 2, "a line reduces to its endpoints: " + lineHull);
        check(lineHull.equals(giftWrap(line)), "gift wrapping agrees on the line");
        check(containsAll(lineHull, line), "all points lie on the segment");
        System.out.println("collinear    : " + lineHull);

        // ---- duplicates and tiny inputs --------------------------------------
        Point[] duplicates = {new Point(1, 1), new Point(1, 1), new Point(1, 1)};
        check(monotoneChain(duplicates).size() == 1, "duplicates collapse to one point");
        check(monotoneChain(new Point[0]).isEmpty(), "no points");
        check(monotoneChain(new Point[] {new Point(5, 5)}).size() == 1, "one point");
        List<Point> pair = monotoneChain(new Point[] {new Point(0, 0), new Point(4, 4)});
        check(pair.size() == 2, "two points stay two points");
        System.out.println("degenerate   : empty, one, two, and duplicate inputs handled");

        // ---- a triangle with points on an edge -------------------------------
        Point[] triangle = {
            new Point(0, 0),
            new Point(4, 0),
            new Point(4, 4),
            new Point(2, 0),
            new Point(1, 1),
        };
        List<Point> triangleHull = monotoneChain(triangle);
        check(triangleHull.size() == 3, "the midpoint of an edge is not a vertex: " + triangleHull);
        check(triangleHull.equals(giftWrap(triangle)), "both algorithms agree on the triangle");
        check(containsAll(triangleHull, triangle), "every point is inside the triangle");
        System.out.println("triangle     : " + triangleHull);

        // ---- randomised comparison -------------------------------------------
        Random random = new Random(20261106);
        int checked = 0;

        for (int trial = 0; trial < 300; trial++) {
            int size = 1 + random.nextInt(40);
            Point[] points = new Point[size];
            for (int index = 0; index < size; index++) {
                points[index] = new Point(random.nextInt(21) - 10, random.nextInt(21) - 10);
            }

            List<Point> fast = monotoneChain(points);
            List<Point> slow = giftWrap(points);

            check(fast.equals(slow), "hulls differ for " + size + " points: " + fast + " vs " + slow);
            check(containsAll(fast, points), "point outside hull for trial " + trial);
            check(isStrictlyConvex(fast), "hull is not strictly convex for trial " + trial);
            check(allOnHull(fast, points), "hull contains a point that was not an input");
            checked++;
        }
        System.out.println("random check : " + checked + " point sets agree with gift wrapping");

        // ---- coordinates far from the origin ---------------------------------
        Point[] faraway = {
            new Point(-1_000_000, -1_000_000),
            new Point(1_000_000, -1_000_000),
            new Point(1_000_000, 1_000_000),
            new Point(-1_000_000, 1_000_000),
            new Point(0, 0),
        };
        List<Point> farHull = monotoneChain(faraway);
        check(farHull.size() == 4, "large coordinates keep four corners");
        check(containsAll(farHull, faraway), "the centre is inside");
        check(farHull.equals(giftWrap(faraway)), "gift wrapping agrees at scale");
        System.out.println("large coords : " + farHull.get(0) + " ... " + farHull.get(3));

        // ---- a large random input --------------------------------------------
        Point[] large = new Point[50_000];
        for (int index = 0; index < large.length; index++) {
            large[index] = new Point(random.nextInt(2_000_001) - 1_000_000, random.nextInt(2_000_001) - 1_000_000);
        }

        long started = System.nanoTime();
        List<Point> largeHull = monotoneChain(large);
        long elapsed = System.nanoTime() - started;

        check(largeHull.size() >= 3, "a large random set has a real hull");
        check(isStrictlyConvex(largeHull), "large hull is strictly convex");
        check(containsAll(largeHull, large), "large hull contains every point");
        System.out.printf("large input  : 50,000 points, %d hull vertices in %.1f ms%n",
                largeHull.size(), elapsed / 1_000_000.0);

        System.out.println("All checks passed.");
    }
}
