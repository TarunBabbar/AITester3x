import java.util.Arrays;
import java.util.Random;

/**
 * 113 - Longest increasing subsequence.
 *
 * Three views of the same problem: patience sorting with binary search in
 * O(n log n), the classic O(n^2) dynamic program, and exhaustive enumeration for
 * tiny arrays. The fast version also rebuilds one actual subsequence using the
 * parent links that patience sorting leaves behind.
 *
 * Values must be strictly increasing, so duplicates do not extend a run.
 *
 * Compile and run:
 *   javac LongestIncreasingSubsequence.java
 *   java LongestIncreasingSubsequence
 */
public class LongestIncreasingSubsequence {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /** Length only: each value replaces the smallest tail that can end a longer run. */
    static int lisLength(int[] values) {
        int[] tails = new int[values.length];
        int length = 0;

        for (int value : values) {
            int low = 0;
            int high = length;
            while (low < high) {
                int middle = (low + high) >>> 1;
                if (tails[middle] < value) {
                    low = middle + 1;
                } else {
                    high = middle;
                }
            }
            tails[low] = value;
            if (low == length) {
                length++;
            }
        }

        return length;
    }

    /** One actual longest increasing subsequence, rebuilt from parent links. */
    static int[] lis(int[] values) {
        if (values.length == 0) {
            return new int[0];
        }

        int[] tailsIndex = new int[values.length];
        int[] parent = new int[values.length];
        Arrays.fill(parent, -1);
        int length = 0;

        for (int index = 0; index < values.length; index++) {
            int low = 0;
            int high = length;
            while (low < high) {
                int middle = (low + high) >>> 1;
                if (values[tailsIndex[middle]] < values[index]) {
                    low = middle + 1;
                } else {
                    high = middle;
                }
            }

            if (low > 0) {
                parent[index] = tailsIndex[low - 1];
            }
            tailsIndex[low] = index;
            if (low == length) {
                length++;
            }
        }

        int[] result = new int[length];
        int node = tailsIndex[length - 1];
        for (int position = length - 1; position >= 0; position--) {
            result[position] = values[node];
            node = parent[node];
        }
        return result;
    }

    static int lisQuadratic(int[] values) {
        if (values.length == 0) {
            return 0;
        }

        int[] best = new int[values.length];
        Arrays.fill(best, 1);
        int answer = 1;

        for (int right = 0; right < values.length; right++) {
            for (int left = 0; left < right; left++) {
                if (values[left] < values[right]) {
                    best[right] = Math.max(best[right], best[left] + 1);
                }
            }
            answer = Math.max(answer, best[right]);
        }

        return answer;
    }

    /** Exhaustive, only for tiny arrays. */
    static int bruteForce(int[] values) {
        int answer = 0;
        int count = values.length;

        for (int mask = 0; mask < (1 << count); mask++) {
            int previous = Integer.MIN_VALUE;
            int length = 0;
            boolean increasing = true;

            for (int index = 0; index < count; index++) {
                if ((mask & (1 << index)) == 0) {
                    continue;
                }
                if (values[index] <= previous) {
                    increasing = false;
                    break;
                }
                previous = values[index];
                length++;
            }

            if (increasing) {
                answer = Math.max(answer, length);
            }
        }

        return answer;
    }

    static boolean isSubsequence(int[] values, int[] candidate) {
        int position = 0;
        for (int value : values) {
            if (position < candidate.length && value == candidate[position]) {
                position++;
            }
        }
        return position == candidate.length;
    }

    static boolean isStrictlyIncreasing(int[] values) {
        for (int index = 1; index < values.length; index++) {
            if (values[index - 1] >= values[index]) {
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) {
        // ---- known answers ---------------------------------------------------
        int[] sample = {10, 9, 2, 5, 3, 7, 101, 18};
        int length = lisLength(sample);
        int[] rebuilt = lis(sample);
        check(length == 4, "sample length should be 4, got " + length);
        check(rebuilt.length == length, "reconstructed length");
        check(isStrictlyIncreasing(rebuilt), "reconstructed sequence is increasing: " + Arrays.toString(rebuilt));
        check(isSubsequence(sample, rebuilt), "reconstruction is a subsequence");
        System.out.println("sample       : length " + length + ", one LIS " + Arrays.toString(rebuilt));

        int[] second = {0, 1, 0, 3, 2, 3};
        check(lisLength(second) == 4, "second sample length");
        check(lis(second).length == 4, "second sample reconstruction");
        System.out.println("second       : length " + lisLength(second) + ", " + Arrays.toString(lis(second)));

        // ---- edge cases ------------------------------------------------------
        check(lisLength(new int[0]) == 0, "empty array");
        check(lis(new int[0]).length == 0, "empty reconstruction");
        check(lisLength(new int[] {7}) == 1, "single value");
        check(Arrays.equals(lis(new int[] {7}), new int[] {7}), "single reconstruction");
        check(lisLength(new int[] {7, 7, 7, 7}) == 1, "duplicates do not extend");
        check(lisLength(new int[] {5, 4, 3, 2, 1}) == 1, "decreasing array");
        check(lisLength(new int[] {1, 2, 3, 4, 5}) == 5, "increasing array");
        check(lisLength(new int[] {1, 2, 2, 3}) == 3, "one duplicate in the middle");
        check(lisLength(new int[] {-5, -1, -9, -3}) == 2, "negatives");
        check(lisLength(new int[] {Integer.MIN_VALUE, 0, Integer.MAX_VALUE}) == 3, "int extremes");
        System.out.println("edge cases   : empty, single, duplicates, monotone, negatives, extremes");

        int[] negatives = {-5, -1, -9, -3};
        int[] negativeRun = lis(negatives);
        check(negativeRun.length == 2, "negative reconstruction length");
        check(isStrictlyIncreasing(negativeRun), "negative reconstruction increasing");
        check(isSubsequence(negatives, negativeRun), "negative reconstruction subsequence");
        System.out.println("negatives    : " + Arrays.toString(negativeRun));

        // ---- all three agree on small arrays ---------------------------------
        Random random = new Random(20261106);
        int checked = 0;

        for (int trial = 0; trial < 400; trial++) {
            int size = random.nextInt(13);
            int[] values = new int[size];
            for (int index = 0; index < size; index++) {
                values[index] = random.nextInt(21) - 10;
            }

            int fast = lisLength(values);
            int slow = lisQuadratic(values);
            int exhaustive = bruteForce(values);
            check(fast == slow, "log vs quadratic for " + Arrays.toString(values));
            check(fast == exhaustive, "log vs exhaustive for " + Arrays.toString(values));

            if (size > 0) {
                int[] candidate = lis(values);
                check(candidate.length == fast, "reconstructed length for " + Arrays.toString(values));
                check(isStrictlyIncreasing(candidate), "candidate increasing for " + Arrays.toString(values));
                check(isSubsequence(values, candidate), "candidate subsequence for " + Arrays.toString(values));
            }
            checked++;
        }
        System.out.println("random check : " + checked + " small arrays agree three ways");

        // ---- larger random arrays --------------------------------------------
        int[] large = new int[200_000];
        for (int index = 0; index < large.length; index++) {
            large[index] = random.nextInt(1_000_000);
        }

        long started = System.nanoTime();
        int largeLength = lisLength(large);
        long elapsed = System.nanoTime() - started;

        check(largeLength > 0 && largeLength <= large.length, "plausible length");
        check(lis(large).length == largeLength, "large reconstruction length matches");
        System.out.printf("large input  : 200,000 values, LIS length %d in %.1f ms%n",
                largeLength, elapsed / 1_000_000.0);

        // A sorted array is the worst case for length and must return everything.
        int[] sorted = new int[2_000];
        for (int index = 0; index < sorted.length; index++) {
            sorted[index] = index;
        }
        check(lisLength(sorted) == sorted.length, "sorted array");
        check(lis(sorted).length == sorted.length, "sorted reconstruction");

        System.out.println("All checks passed.");
    }
}
