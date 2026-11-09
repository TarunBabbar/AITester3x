import java.util.Arrays;
import java.util.Random;

/**
 * 116 - Fenwick tree (binary indexed tree).
 *
 * A single array of partial sums supports prefix sums, range sums and point
 * updates in O(log n). Each index stores the sum of a block whose length is the
 * lowest set bit of the index, which is also what the update walk uses.
 *
 * The tree also answers "which index first reaches this prefix sum?" by binary
 * lifting, which needs non-negative values to be meaningful.
 *
 * Compile and run:
 *   javac FenwickTree.java
 *   java FenwickTree
 */
public class FenwickTree {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private final long[] tree;
    private final int size;

    FenwickTree(int[] values) {
        this.size = values.length;
        this.tree = new long[size + 1];
        for (int index = 0; index < size; index++) {
            add(index, values[index]);
        }
    }

    int size() {
        return size;
    }

    boolean isEmpty() {
        return size == 0;
    }

    void add(int index, int delta) {
        checkIndex(index);
        for (int node = index + 1; node <= size; node += node & -node) {
            tree[node] += delta;
        }
    }

    /** Sum of values[0..index], inclusive. */
    long prefixSum(int index) {
        checkIndex(index);
        long total = 0;
        for (int node = index + 1; node > 0; node -= node & -node) {
            total += tree[node];
        }
        return total;
    }

    /** Sum of values[left..right], inclusive. */
    long rangeSum(int left, int right) {
        checkIndex(left);
        checkIndex(right);
        check(left <= right, "left " + left + " is after right " + right);
        long total = prefixSum(right);
        return left == 0 ? total : total - prefixSum(left - 1);
    }

    long total() {
        return size == 0 ? 0 : prefixSum(size - 1);
    }

    /**
     * Smallest index whose prefix sum is at least target, or -1 when no such
     * index exists. Requires non-negative values.
     */
    int findByOrder(long target) {
        if (target <= 0) {
            throw new IllegalArgumentException("target must be positive");
        }

        int position = 0;
        long remaining = target;
        int step = Integer.highestOneBit(size);

        for (; step > 0; step >>= 1) {
            int next = position + step;
            if (next <= size && tree[next] < remaining) {
                position = next;
                remaining -= tree[next];
            }
        }

        return position == size ? -1 : position;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("index " + index + " is outside 0.." + (size - 1));
        }
    }

    public static void main(String[] args) {
        // ---- building --------------------------------------------------------
        FenwickTree tree = new FenwickTree(new int[] {1, 3, 5, 7, 9});
        System.out.println("size: " + tree.size() + " | total: " + tree.total());
        check(tree.size() == 5, "five values");
        check(!tree.isEmpty(), "not empty");
        check(tree.total() == 25, "total sum");
        check(tree.prefixSum(0) == 1, "prefix of one");
        check(tree.prefixSum(2) == 9, "prefix of three");
        check(tree.prefixSum(4) == 25, "prefix of all");

        // ---- range sums ------------------------------------------------------
        check(tree.rangeSum(0, 0) == 1, "single first");
        check(tree.rangeSum(4, 4) == 9, "single last");
        check(tree.rangeSum(1, 3) == 15, "middle range");
        check(tree.rangeSum(0, 4) == 25, "whole range");
        System.out.println("range [1, 3] = " + tree.rangeSum(1, 3));

        // ---- updates ---------------------------------------------------------
        tree.add(2, 10);
        check(tree.prefixSum(2) == 19, "updated prefix");
        check(tree.total() == 35, "updated total");
        tree.add(0, -1);
        check(tree.prefixSum(0) == 0, "negative delta");
        check(tree.rangeSum(0, 2) == 18, "range after updates");
        System.out.println("after updates total = " + tree.total() + ", range [0, 2] = " + tree.rangeSum(0, 2));

        // ---- single element and empty ---------------------------------------
        FenwickTree single = new FenwickTree(new int[] {42});
        check(single.prefixSum(0) == 42 && single.total() == 42, "single value");
        single.add(0, 8);
        check(single.total() == 50, "single value updated");

        FenwickTree empty = new FenwickTree(new int[0]);
        check(empty.isEmpty() && empty.size() == 0, "empty tree");
        check(empty.total() == 0, "empty total");
        System.out.println("single/empty : handled");

        // ---- order statistics ------------------------------------------------
        FenwickTree counts = new FenwickTree(new int[] {1, 3, 5, 7});
        check(counts.findByOrder(1) == 0, "first item starts at cumulative 1");
        check(counts.findByOrder(2) == 1, "cumulative 4 reaches index 1");
        check(counts.findByOrder(4) == 1, "exact boundary lands on the item");
        check(counts.findByOrder(9) == 2, "cumulative 9 reaches index 2");
        check(counts.findByOrder(16) == 3, "last item");
        check(counts.findByOrder(17) == -1, "beyond the end");
        try {
            counts.findByOrder(0);
            throw new AssertionError("target 0 should be rejected");
        } catch (IllegalArgumentException expected) {
            check(expected.getMessage().contains("positive"), expected.getMessage());
        }
        System.out.println("order stats  : 1->0, 2->1, 9->2, 16->3, 17->-1");

        // ---- validation ------------------------------------------------------
        try {
            tree.prefixSum(-1);
            throw new AssertionError("negative index should fail");
        } catch (IndexOutOfBoundsException expected) {
            check(expected.getMessage().contains("-1"), expected.getMessage());
        }
        try {
            tree.prefixSum(5);
            throw new AssertionError("index equal to size should fail");
        } catch (IndexOutOfBoundsException expected) {
            check(expected.getMessage().contains("5"), expected.getMessage());
        }
        try {
            tree.rangeSum(3, 2);
            throw new AssertionError("reversed range should fail");
        } catch (AssertionError expected) {
            check(expected.getMessage().contains("after"), expected.getMessage());
        }
        System.out.println("validation   : bad indexes and reversed ranges rejected");

        // ---- against a plain array -------------------------------------------
        Random random = new Random(20261106);
        int[] model = new int[40];
        for (int index = 0; index < model.length; index++) {
            model[index] = random.nextInt(21) - 10;
        }
        FenwickTree randomTree = new FenwickTree(model);

        for (int step = 0; step < 2_000; step++) {
            if (random.nextBoolean()) {
                int index = random.nextInt(model.length);
                int delta = random.nextInt(21) - 10;
                model[index] += delta;
                randomTree.add(index, delta);
            } else {
                int left = random.nextInt(model.length);
                int right = left + random.nextInt(model.length - left);
                long expected = 0;
                for (int index = left; index <= right; index++) {
                    expected += model[index];
                }
                check(randomTree.rangeSum(left, right) == expected,
                        "range mismatch at step " + step + " for [" + left + ", " + right + "]");
            }
        }
        System.out.println("random check : 2000 updates and queries match a plain array");

        // ---- non-negative order statistics ----------------------------------
        int[] frequencies = new int[20];
        long cumulative = 0;
        for (int index = 0; index < frequencies.length; index++) {
            frequencies[index] = random.nextInt(5);
            cumulative += frequencies[index];
        }
        FenwickTree histogram = new FenwickTree(frequencies);

        for (long target = 1; target <= cumulative; target++) {
            int found = histogram.findByOrder(target);
            check(found >= 0, "target inside the total should resolve");
            long before = found == 0 ? 0 : histogram.prefixSum(found - 1);
            check(before < target, "the index is the first to reach the target");
            check(histogram.prefixSum(found) >= target, "and it does reach it");
        }
        check(histogram.findByOrder(cumulative + 1) == -1, "one past the total is missing");
        System.out.println("order stats  : every target inside the histogram resolves correctly");

        // ---- large tree ------------------------------------------------------
        int[] largeValues = new int[100_000];
        for (int index = 0; index < largeValues.length; index++) {
            largeValues[index] = random.nextInt(10);
        }

        long started = System.nanoTime();
        FenwickTree large = new FenwickTree(largeValues);
        long largeTotal = large.total();
        long elapsed = System.nanoTime() - started;

        long expectedTotal = 0;
        for (int value : largeValues) {
            expectedTotal += value;
        }
        check(largeTotal == expectedTotal, "large total");
        System.out.printf("large input  : 100,000 values, total %d in %.1f ms%n",
                largeTotal, elapsed / 1_000_000.0);

        check(Arrays.stream(largeValues).sum() == (int) largeTotal, "int and long totals agree");

        System.out.println("All checks passed.");
    }
}
