import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * 110 - An interval tree: a binary search tree of closed ranges, ordered by
 * start point and augmented with the largest end point in each subtree.
 *
 * The extra field is what makes overlap queries fast: if a subtree's largest
 * end is still before the query start, nothing in that subtree can overlap, so
 * the whole branch is skipped. Queries therefore cost O(log n + matches) on a
 * balanced tree instead of O(n).
 *
 * The invariants are checked after every structural change, and overlap and
 * point queries are compared against a brute-force scan on random data.
 *
 * Compile and run:
 *   javac IntervalTree.java
 *   java IntervalTree
 */
public class IntervalTree {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /** A closed range [start, end]. */
    static final class Interval implements Comparable<Interval> {
        final long start;
        final long end;

        Interval(long start, long end) {
            if (start > end) {
                throw new IllegalArgumentException("start " + start + " is after end " + end);
            }
            this.start = start;
            this.end = end;
        }

        boolean overlaps(Interval other) {
            return start <= other.end && other.start <= end;
        }

        boolean contains(long point) {
            return start <= point && point <= end;
        }

        @Override
        public int compareTo(Interval other) {
            int byStart = Long.compare(start, other.start);
            return byStart != 0 ? byStart : Long.compare(end, other.end);
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Interval)) {
                return false;
            }
            Interval interval = (Interval) other;
            return start == interval.start && end == interval.end;
        }

        @Override
        public int hashCode() {
            return Long.hashCode(start) * 31 + Long.hashCode(end);
        }

        @Override
        public String toString() {
            return "[" + start + ", " + end + "]";
        }
    }

    private static final class Node {
        Interval interval;
        Node left;
        Node right;
        long maxEnd;                        // largest end anywhere in this subtree

        Node(Interval interval) {
            this.interval = interval;
            this.maxEnd = interval.end;
        }
    }

    private Node root;
    private int size;
    private int probes;                     // nodes touched by the last query

    int size() {
        return size;
    }

    boolean isEmpty() {
        return size == 0;
    }

    int height() {
        return height(root);
    }

    private static int height(Node node) {
        if (node == null) {
            return 0;
        }
        return 1 + Math.max(height(node.left), height(node.right));
    }

    /** Nodes visited by the most recent overlap query, as a pruning measure. */
    int lastProbeCount() {
        return probes;
    }

    boolean add(long start, long end) {
        return add(new Interval(start, end));
    }

    boolean add(Interval interval) {
        int before = size;
        root = insert(root, interval);
        return size > before;
    }

    private Node insert(Node node, Interval interval) {
        if (node == null) {
            size++;
            return new Node(interval);
        }

        int comparison = interval.compareTo(node.interval);
        if (comparison < 0) {
            node.left = insert(node.left, interval);
        } else if (comparison > 0) {
            node.right = insert(node.right, interval);
        } else {
            return node;                    // already stored
        }

        refresh(node);
        return node;
    }

    boolean remove(long start, long end) {
        return remove(new Interval(start, end));
    }

    boolean remove(Interval interval) {
        int before = size;
        root = delete(root, interval);
        return size < before;
    }

    private Node delete(Node node, Interval interval) {
        if (node == null) {
            return null;
        }

        int comparison = interval.compareTo(node.interval);
        if (comparison < 0) {
            node.left = delete(node.left, interval);
        } else if (comparison > 0) {
            node.right = delete(node.right, interval);
        } else if (node.left == null) {
            size--;
            return node.right;
        } else if (node.right == null) {
            size--;
            return node.left;
        } else {
            Node successor = leftmost(node.right);
            node.interval = successor.interval;
            node.right = delete(node.right, successor.interval);
        }

        refresh(node);
        return node;
    }

    private static Node leftmost(Node node) {
        Node current = node;
        while (current.left != null) {
            current = current.left;
        }
        return current;
    }

    private static long maxEnd(Node node) {
        return node == null ? Long.MIN_VALUE : node.maxEnd;
    }

    private static void refresh(Node node) {
        node.maxEnd = Math.max(node.interval.end, Math.max(maxEnd(node.left), maxEnd(node.right)));
    }

    boolean contains(long start, long end) {
        Node current = root;
        Interval target = new Interval(start, end);
        while (current != null) {
            int comparison = target.compareTo(current.interval);
            if (comparison == 0) {
                return true;
            }
            current = comparison < 0 ? current.left : current.right;
        }
        return false;
    }

    List<Interval> overlapping(long start, long end) {
        List<Interval> found = new ArrayList<>();
        probes = 0;
        collectOverlaps(root, new Interval(start, end), found);
        return found;
    }

    private void collectOverlaps(Node node, Interval query, List<Interval> found) {
        if (node == null) {
            return;
        }
        probes++;
        if (node.maxEnd < query.start) {
            return;                         // every interval in this subtree ends too early
        }

        collectOverlaps(node.left, query, found);
        if (node.interval.overlaps(query)) {
            found.add(node.interval);
        }
        if (node.interval.start <= query.end) {
            collectOverlaps(node.right, query, found);
        }
    }

    List<Interval> containing(long point) {
        return overlapping(point, point);
    }

    List<Interval> inOrder() {
        List<Interval> ordered = new ArrayList<>();
        collectInOrder(root, ordered);
        return ordered;
    }

    private static void collectInOrder(Node node, List<Interval> ordered) {
        if (node == null) {
            return;
        }
        collectInOrder(node.left, ordered);
        ordered.add(node.interval);
        collectInOrder(node.right, ordered);
    }

    /** Sorted in-order, and every subtree's maxEnd is exactly its largest end. */
    boolean invariantsHold() {
        List<Interval> ordered = inOrder();
        if (ordered.size() != size) {
            return false;
        }
        for (int index = 1; index < ordered.size(); index++) {
            if (ordered.get(index - 1).compareTo(ordered.get(index)) >= 0) {
                return false;
            }
        }
        return maxEndsAreCorrect(root);
    }

    private static boolean maxEndsAreCorrect(Node node) {
        if (node == null) {
            return true;
        }
        long expected = Math.max(node.interval.end, Math.max(maxEnd(node.left), maxEnd(node.right)));
        return node.maxEnd == expected && maxEndsAreCorrect(node.left) && maxEndsAreCorrect(node.right);
    }

    private static void sort(List<Interval> intervals) {
        intervals.sort(Comparator.naturalOrder());
    }

    private static List<Interval> bruteForce(List<Interval> intervals, long start, long end) {
        Interval query = new Interval(start, end);
        List<Interval> found = new ArrayList<>();
        for (Interval interval : intervals) {
            if (interval.overlaps(query)) {
                found.add(interval);
            }
        }
        sort(found);
        return found;
    }

    public static void main(String[] args) {
        // ---- an empty tree ---------------------------------------------------
        IntervalTree empty = new IntervalTree();
        check(empty.isEmpty() && empty.size() == 0, "nothing in it");
        check(empty.height() == 0, "no height");
        check(empty.overlapping(0, 10).isEmpty(), "no overlaps to find");
        check(empty.containing(5).isEmpty(), "no point coverage");
        check(!empty.remove(0, 1), "nothing to remove");
        check(empty.invariantsHold(), "an empty tree is valid");
        System.out.println("empty        : size 0, no matches");

        // ---- basics ----------------------------------------------------------
        IntervalTree tree = new IntervalTree();
        check(tree.add(15, 20), "new interval");
        check(tree.add(10, 30), "new interval");
        check(tree.add(17, 19), "new interval");
        check(tree.add(5, 20), "new interval");
        check(tree.add(12, 15), "new interval");
        check(tree.add(30, 40), "new interval");
        check(!tree.add(10, 30), "a duplicate is rejected");
        check(tree.size() == 6, "six intervals: " + tree.size());
        check(tree.invariantsHold(), "invariants hold after inserts");
        System.out.println("basic        : " + tree.inOrder() + " height " + tree.height());

        List<Interval> overlap = tree.overlapping(14, 16);
        sort(overlap);
        check(overlap.size() == 4, "expected four overlaps, got " + overlap);
        System.out.println("overlap [14, 16]: " + overlap);

        List<Interval> touch = tree.overlapping(20, 25);
        sort(touch);
        check(touch.contains(new Interval(15, 20)) && touch.contains(new Interval(10, 30)),
                "closed ranges touch at 20: " + touch);
        System.out.println("overlap [20, 25]: " + touch);

        check(tree.containing(18).size() == 4, "four intervals cover 18");
        check(tree.containing(41).isEmpty(), "nothing covers 41");
        check(tree.contains(10, 30), "contains the stored interval");
        check(!tree.contains(10, 31), "does not contain a widened one");

        // ---- removal ---------------------------------------------------------
        check(tree.remove(10, 30), "remove the wide interval");
        check(!tree.remove(10, 30), "removing it twice reports nothing");
        check(!tree.contains(10, 30), "and it is gone");
        check(tree.size() == 5, "five left: " + tree.size());
        check(tree.invariantsHold(), "invariants hold after removal");
        check(tree.containing(18).size() == 3, "three intervals still cover 18");
        System.out.println("removal      : " + tree.inOrder());

        while (!tree.isEmpty()) {
            Interval next = tree.inOrder().get(0);
            check(tree.remove(next), "removing " + next);
            check(tree.invariantsHold(), "invariants hold while emptying");
        }
        check(tree.size() == 0 && tree.height() == 0, "the tree is empty again");
        check(tree.overlapping(0, 100).isEmpty(), "no matches when empty");
        System.out.println("emptying     : back to size 0");

        // ---- random data against brute force ---------------------------------
        Random random = new Random(20261106);
        IntervalTree randomTree = new IntervalTree();
        List<Interval> stored = new ArrayList<>();
        int attempts = 0;
        while (stored.size() < 400 && attempts < 4_000) {
            attempts++;
            long start = random.nextInt(300);
            long end = start + random.nextInt(25);
            Interval candidate = new Interval(start, end);
            if (randomTree.add(candidate)) {
                stored.add(candidate);
            }
        }
        check(randomTree.size() == stored.size(), "sizes agree: " + randomTree.size());
        check(randomTree.invariantsHold(), "invariants hold on random data");
        System.out.println("random build : " + stored.size() + " intervals, height " + randomTree.height());

        for (int trial = 0; trial < 300; trial++) {
            long start = random.nextInt(340) - 20;
            long end = start + random.nextInt(60);
            List<Interval> expected = bruteForce(stored, start, end);
            List<Interval> actual = randomTree.overlapping(start, end);
            sort(actual);
            check(actual.equals(expected),
                    "overlap mismatch for [" + start + ", " + end + "]: " + actual + " vs " + expected);
        }

        for (int trial = 0; trial < 200; trial++) {
            long point = random.nextInt(340) - 20;
            List<Interval> expected = bruteForce(stored, point, point);
            List<Interval> actual = randomTree.containing(point);
            sort(actual);
            check(actual.equals(expected), "point mismatch at " + point);
        }
        System.out.println("random query : 300 overlap and 200 point queries match brute force");

        // ---- deleting half, then comparing again -----------------------------
        int removed = 0;
        for (Interval interval : new ArrayList<>(stored)) {
            if ((interval.start + interval.end) % 2 == 0) {
                check(randomTree.remove(interval), "removing " + interval);
                stored.remove(interval);
                removed++;
            }
        }
        check(randomTree.size() == stored.size(), "sizes agree after " + removed + " removals");
        check(randomTree.invariantsHold(), "invariants hold after removals");

        for (int trial = 0; trial < 200; trial++) {
            long start = random.nextInt(340) - 20;
            long end = start + random.nextInt(60);
            List<Interval> expected = bruteForce(stored, start, end);
            List<Interval> actual = randomTree.overlapping(start, end);
            sort(actual);
            check(actual.equals(expected), "overlap mismatch after removals");
        }
        System.out.println("random delete: removed " + removed + ", " + stored.size()
                + " left, queries still agree");

        // ---- pruning ---------------------------------------------------------
        IntervalTree pruned = new IntervalTree();
        for (int index = 0; index < 1_000; index++) {
            long start = random.nextInt(500);
            pruned.add(new Interval(start, start + 2));
        }
        check(pruned.invariantsHold(), "invariants hold for the pruning tree");

        long broadProbes = 0;
        long narrowProbes = 0;
        int samples = 200;
        for (int trial = 0; trial < samples; trial++) {
            pruned.overlapping(0, 500);
            broadProbes += pruned.lastProbeCount();
            pruned.overlapping(250, 251);
            narrowProbes += pruned.lastProbeCount();
        }
        double broadAverage = broadProbes / (double) samples;
        double narrowAverage = narrowProbes / (double) samples;
        check(narrowAverage < broadAverage, "a narrow query should visit fewer nodes");
        check(narrowAverage < pruned.size(), "and should skip part of the tree");
        System.out.printf("pruning      : broad query visits %.1f nodes, narrow visits %.1f (of %d)%n",
                broadAverage, narrowAverage, pruned.size());

        // ---- edge geometry ---------------------------------------------------
        IntervalTree edges = new IntervalTree();
        edges.add(Long.MIN_VALUE, Long.MIN_VALUE + 10);
        edges.add(-5, 5);
        edges.add(Long.MAX_VALUE - 10, Long.MAX_VALUE);
        check(edges.invariantsHold(), "invariants hold at the extremes");
        check(edges.containing(Long.MIN_VALUE).size() == 1, "the smallest point");
        check(edges.containing(Long.MAX_VALUE).size() == 1, "the largest point");
        check(edges.overlapping(-1, 1).size() == 1, "a middle point");
        check(edges.overlapping(100, 200).isEmpty(), "nothing between the extremes");
        System.out.println("extremes     : " + edges.inOrder());

        // ---- a single interval ----------------------------------------------
        IntervalTree single = new IntervalTree();
        single.add(7, 7);
        check(single.height() == 1, "one node is height one");
        check(single.containing(7).size() == 1, "the single point is covered");
        check(single.containing(8).isEmpty(), "nothing else is");
        check(single.overlapping(0, 7).size() == 1, "touching on the left");
        check(single.overlapping(7, 7).size() == 1, "tight query");
        check(single.remove(7, 7) && single.isEmpty(), "remove the only node");
        System.out.println("single       : [7, 7] handled");

        System.out.println("All checks passed.");
    }
}
