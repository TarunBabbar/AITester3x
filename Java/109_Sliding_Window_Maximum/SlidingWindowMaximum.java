import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Random;

/**
 * 109 - Sliding window maximum (and minimum) with a monotonic deque.
 *
 * A window of size k slides over an array. Recomputing the maximum of each
 * window costs O(nk); keeping the indices in a deque whose values are strictly
 * decreasing means the front is always the current maximum and each index
 * enters and leaves the deque at most once, so the cost is O(n).
 *
 * The extremes are checked against a brute-force scan, then the same window
 * technique is used on the "longest subarray with sum at most limit" problem.
 *
 * Compile and run:
 *   javac SlidingWindowMaximum.java
 *   java SlidingWindowMaximum
 */
public class SlidingWindowMaximum {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /** Indices in the deque, with values strictly decreasing from front to back. */
    static int[] windowExtreme(int[] values, int k, boolean maximum) {
        if (values.length == 0 || k <= 0) {
            return new int[0];
        }
        if (k > values.length) {
            throw new IllegalArgumentException("window " + k + " larger than input " + values.length);
        }

        int[] result = new int[values.length - k + 1];
        Deque<Integer> deque = new ArrayDeque<>();

        for (int index = 0; index < values.length; index++) {
            while (!deque.isEmpty() && deque.peekFirst() <= index - k) {
                deque.pollFirst();
            }
            while (!deque.isEmpty()
                    && (maximum
                        ? values[deque.peekLast()] <= values[index]
                        : values[deque.peekLast()] >= values[index])) {
                deque.pollLast();
            }
            deque.offerLast(index);

            if (index >= k - 1) {
                result[index - k + 1] = values[deque.peekFirst()];
            }
        }

        return result;
    }

    static int[] maxSlidingWindow(int[] values, int k) {
        return windowExtreme(values, k, true);
    }

    static int[] minSlidingWindow(int[] values, int k) {
        return windowExtreme(values, k, false);
    }

    /** The obvious O(nk) answer, used to prove the deque version. */
    static int[] bruteForce(int[] values, int k, boolean maximum) {
        if (values.length == 0 || k <= 0) {
            return new int[0];
        }
        int[] result = new int[values.length - k + 1];
        for (int start = 0; start + k <= values.length; start++) {
            int best = values[start];
            for (int index = start + 1; index < start + k; index++) {
                best = maximum ? Math.max(best, values[index]) : Math.min(best, values[index]);
            }
            result[start] = best;
        }
        return result;
    }

    /**
     * Longest stretch of a non-negative array whose sum is at most the limit.
     * The right edge grows every step; the left edge only moves forward when
     * the sum is too large, which is what keeps the work linear.
     */
    static int longestAtMost(int[] values, long limit) {
        int left = 0;
        long sum = 0;
        int best = 0;

        for (int right = 0; right < values.length; right++) {
            if (values[right] < 0) {
                throw new IllegalArgumentException("values must be non-negative");
            }
            sum += values[right];
            while (sum > limit) {
                sum -= values[left];
                left++;
            }
            best = Math.max(best, right - left + 1);
        }

        return best;
    }

    static int longestAtMostBrute(int[] values, long limit) {
        int best = 0;
        for (int start = 0; start < values.length; start++) {
            long sum = 0;
            for (int end = start; end < values.length; end++) {
                sum += values[end];
                if (sum > limit) {
                    break;
                }
                best = Math.max(best, end - start + 1);
            }
        }
        return best;
    }

    public static void main(String[] args) {
        // ---- the standard example -------------------------------------------
        int[] sample = {1, 3, -1, -3, 5, 3, 6, 7};
        int[] maxima = maxSlidingWindow(sample, 3);
        int[] minima = minSlidingWindow(sample, 3);
        check(Arrays.equals(maxima, new int[] {3, 3, 5, 5, 6, 7}), Arrays.toString(maxima));
        check(Arrays.equals(minima, new int[] {-1, -3, -3, -3, 3, 3}), Arrays.toString(minima));
        System.out.println("sample       : " + Arrays.toString(maxima) + " max, "
                + Arrays.toString(minima) + " min");

        // ---- edge cases ------------------------------------------------------
        check(Arrays.equals(maxSlidingWindow(sample, 1), sample), "k=1 returns the input");
        check(Arrays.equals(maxSlidingWindow(sample, sample.length), new int[] {7}), "k=n returns one value");
        check(maxSlidingWindow(new int[0], 3).length == 0, "empty input returns empty");
        check(maxSlidingWindow(sample, 0).length == 0, "k=0 returns empty");
        try {
            maxSlidingWindow(sample, sample.length + 1);
            throw new AssertionError("a window larger than the input should be rejected");
        } catch (IllegalArgumentException expected) {
            System.out.println("k > n       : " + expected.getMessage());
        }

        // ---- duplicates, negatives, and the extremes of int ------------------
        int[] duplicates = {5, 5, 5, 5};
        check(Arrays.equals(maxSlidingWindow(duplicates, 2), new int[] {5, 5, 5}), "all equal");
        check(Arrays.equals(minSlidingWindow(duplicates, 2), new int[] {5, 5, 5}), "all equal");

        int[] extremes = {Integer.MAX_VALUE, Integer.MIN_VALUE, 0, Integer.MIN_VALUE, Integer.MAX_VALUE};
        check(Arrays.equals(maxSlidingWindow(extremes, 2),
                new int[] {Integer.MAX_VALUE, 0, 0, Integer.MAX_VALUE}), "int extremes, max");
        check(Arrays.equals(minSlidingWindow(extremes, 2),
                new int[] {Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE}),
                "int extremes, min");
        System.out.println("extremes    : " + Arrays.toString(maxSlidingWindow(extremes, 2)));

        // ---- randomised cross-check -----------------------------------------
        Random random = new Random(20261106);
        int checkedWindows = 0;
        for (int trial = 0; trial < 1_000; trial++) {
            int length = random.nextInt(41);
            int[] values = new int[length];
            for (int index = 0; index < length; index++) {
                values[index] = random.nextInt(101) - 50;
            }
            int k = length == 0 ? 0 : random.nextInt(length) + 1;

            int[] actualMax = maxSlidingWindow(values, k);
            int[] expectedMax = bruteForce(values, k, true);
            int[] actualMin = minSlidingWindow(values, k);
            int[] expectedMin = bruteForce(values, k, false);

            check(Arrays.equals(actualMax, expectedMax),
                    "max mismatch for " + Arrays.toString(values) + " k=" + k);
            check(Arrays.equals(actualMin, expectedMin),
                    "min mismatch for " + Arrays.toString(values) + " k=" + k);
            checkedWindows++;
        }
        System.out.println("random check : " + checkedWindows + " arrays match the brute-force scan");

        // ---- longest subarray with a bounded sum ------------------------------
        int[] positive = {1, 2, 1, 0, 1, 1, 0};
        check(longestAtMost(positive, 4) == 5, "expected 5, got " + longestAtMost(positive, 4));
        check(longestAtMost(positive, 0) == 1, "only a single zero fits in a zero budget");
        check(longestAtMost(positive, 100) == positive.length, "everything fits");
        check(longestAtMost(new int[0], 10) == 0, "empty array");
        System.out.println("bounded sum : limit 4 -> " + longestAtMost(positive, 4) + " elements");

        for (int trial = 0; trial < 500; trial++) {
            int length = random.nextInt(31);
            int[] values = new int[length];
            for (int index = 0; index < length; index++) {
                values[index] = random.nextInt(11);
            }
            long limit = random.nextInt(31);
            check(longestAtMost(values, limit) == longestAtMostBrute(values, limit),
                    "bounded sum mismatch for " + Arrays.toString(values) + " limit=" + limit);
        }
        System.out.println("random check : 500 bounded-sum arrays match the brute-force scan");

        // ---- a large input, to show the linear behaviour ---------------------
        int[] large = new int[200_000];
        for (int index = 0; index < large.length; index++) {
            large[index] = random.nextInt(1_000_000);
        }

        long started = System.nanoTime();
        int[] largeMaxima = maxSlidingWindow(large, 1_000);
        long elapsed = System.nanoTime() - started;

        check(largeMaxima.length == large.length - 1_000 + 1, "one value per window");
        for (int spot = 0; spot < 20; spot++) {
            int start = random.nextInt(largeMaxima.length);
            int expected = large[start];
            for (int offset = 1; offset < 1_000; offset++) {
                expected = Math.max(expected, large[start + offset]);
            }
            check(largeMaxima[start] == expected, "spot check at " + start);
        }
        System.out.printf("large input  : 200,000 values, k=1000, %d windows in %.1f ms%n",
                largeMaxima.length, elapsed / 1_000_000.0);

        System.out.println("All checks passed.");
    }
}
