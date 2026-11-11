import java.util.Random;

/**
 * 118 - Maximum subarray, four ways.
 *
 * Kadane scans once, keeping the best sum ending at each position. Divide and
 * conquer splits the array and combines the best left part, right part and
 * crossing part. A prefix-minimum scan is a third view of the same recurrence,
 * and brute force checks the small cases.
 *
 * The circular variant (the array wraps around) either takes the best ordinary
 * subarray, or the whole total minus the minimum subarray in between.
 *
 * Compile and run:
 *   javac MaximumSubarray.java
 *   java MaximumSubarray
 */
public class MaximumSubarray {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static final class Result {
        final int sum;
        final int start;     // inclusive
        final int end;       // inclusive

        Result(int sum, int start, int end) {
            this.sum = sum;
            this.start = start;
            this.end = end;
        }

        int length() {
            return end - start + 1;
        }

        @Override
        public String toString() {
            return "sum=" + sum + " [" + start + ", " + end + "]";
        }
    }

    static Result bestRange(int[] values) {
        if (values.length == 0) {
            throw new IllegalArgumentException("array must not be empty");
        }

        int bestSum = values[0];
        int bestStart = 0;
        int bestEnd = 0;
        int currentSum = values[0];
        int currentStart = 0;

        for (int index = 1; index < values.length; index++) {
            if (currentSum < 0) {
                currentSum = values[index];
                currentStart = index;
            } else {
                currentSum += values[index];
            }

            if (currentSum > bestSum) {
                bestSum = currentSum;
                bestStart = currentStart;
                bestEnd = index;
            }
        }

        return new Result(bestSum, bestStart, bestEnd);
    }

    static int kadane(int[] values) {
        return bestRange(values).sum;
    }

    static int divideAndConquer(int[] values) {
        if (values.length == 0) {
            throw new IllegalArgumentException("array must not be empty");
        }
        return divide(values, 0, values.length - 1);
    }

    private static int divide(int[] values, int left, int right) {
        if (left == right) {
            return values[left];
        }

        int middle = (left + right) >>> 1;

        int bestLeft = Integer.MIN_VALUE;
        int sum = 0;
        for (int index = middle; index >= left; index--) {
            sum += values[index];
            bestLeft = Math.max(bestLeft, sum);
        }

        int bestRight = Integer.MIN_VALUE;
        sum = 0;
        for (int index = middle + 1; index <= right; index++) {
            sum += values[index];
            bestRight = Math.max(bestRight, sum);
        }

        return Math.max(
                bestLeft + bestRight,
                Math.max(divide(values, left, middle), divide(values, middle + 1, right)));
    }

    static int prefixMinimum(int[] values) {
        if (values.length == 0) {
            throw new IllegalArgumentException("array must not be empty");
        }

        int prefix = 0;
        int minimumPrefix = 0;
        int best = Integer.MIN_VALUE;

        for (int value : values) {
            prefix += value;
            best = Math.max(best, prefix - minimumPrefix);
            minimumPrefix = Math.min(minimumPrefix, prefix);
        }

        return best;
    }

    static int bruteForce(int[] values) {
        if (values.length == 0) {
            throw new IllegalArgumentException("array must not be empty");
        }

        int best = Integer.MIN_VALUE;
        for (int start = 0; start < values.length; start++) {
            int sum = 0;
            for (int end = start; end < values.length; end++) {
                sum += values[end];
                best = Math.max(best, sum);
            }
        }
        return best;
    }

    static int maxCircular(int[] values) {
        if (values.length == 0) {
            throw new IllegalArgumentException("array must not be empty");
        }

        int straight = kadane(values);
        int total = 0;
        int maximumElement = Integer.MIN_VALUE;

        for (int value : values) {
            total += value;
            maximumElement = Math.max(maximumElement, value);
        }

        if (maximumElement <= 0) {
            return maximumElement;
        }

        int minimumEnding = 0;
        int minimumSum = Integer.MAX_VALUE;
        for (int value : values) {
            minimumEnding = Math.min(value, minimumEnding + value);
            minimumSum = Math.min(minimumSum, minimumEnding);
        }

        return Math.max(straight, total - minimumSum);
    }

    static int bruteCircular(int[] values) {
        int best = Integer.MIN_VALUE;
        int size = values.length;

        for (int length = 1; length <= size; length++) {
            for (int start = 0; start < size; start++) {
                int sum = 0;
                for (int offset = 0; offset < length; offset++) {
                    sum += values[(start + offset) % size];
                }
                best = Math.max(best, sum);
            }
        }

        return best;
    }

    public static void main(String[] args) {
        // ---- known cases -----------------------------------------------------
        int[] sample = {-2, 1, -3, 4, -1, 2, 1, -5, 4};
        Result result = bestRange(sample);
        System.out.println("sample       : " + result);
        check(result.sum == 6, "expected 6, got " + result.sum);
        check(result.start == 3 && result.end == 6, "expected indices 3..6");

        int rangeSum = 0;
        for (int index = result.start; index <= result.end; index++) {
            rangeSum += sample[index];
        }
        check(rangeSum == result.sum, "range sum must equal the reported total");
        System.out.println("range        : "
                + java.util.Arrays.toString(java.util.Arrays.copyOfRange(sample, result.start, result.end + 1))
                + " sums to " + rangeSum);

        // ---- all negative ----------------------------------------------------
        int[] negatives = {-8, -3, -6, -2, -5, -4};
        check(kadane(negatives) == -2, "the least negative value wins");
        check(bestRange(negatives).length() == 1, "a single element is best");
        System.out.println("negatives    : " + bestRange(negatives));

        // ---- tiny arrays -----------------------------------------------------
        check(kadane(new int[] {5}) == 5, "single positive");
        check(kadane(new int[] {-5}) == -5, "single negative");
        check(kadane(new int[] {0}) == 0, "single zero");
        check(kadane(new int[] {0, 0, 0}) == 0, "all zeros");
        check(kadane(new int[] {1, 2, 3, 4}) == 10, "all positive");
        check(kadane(new int[] {-1, -2, -3}) == -1, "all negative");
        check(kadane(new int[] {2, -1, 2}) == 3, "split around a negative");
        System.out.println("tiny arrays  : single values, zeros, and monotone runs");

        // ---- empty input -----------------------------------------------------
        int emptyRejected = 0;
        int[][] emptyCalls = {new int[0]};
        for (int[] values : emptyCalls) {
            for (int variant = 0; variant < 5; variant++) {
                try {
                    switch (variant) {
                        case 0 -> kadane(values);
                        case 1 -> divideAndConquer(values);
                        case 2 -> prefixMinimum(values);
                        case 3 -> bruteForce(values);
                        default -> maxCircular(values);
                    }
                } catch (IllegalArgumentException expected) {
                    emptyRejected++;
                }
            }
        }
        check(emptyRejected == 5, "every algorithm rejects an empty array");
        System.out.println("empty input  : rejected by every algorithm");

        // ---- circular --------------------------------------------------------
        check(maxCircular(new int[] {5, -3, 5}) == 10, "wrapping gives 5 + 5");
        check(maxCircular(new int[] {-2, -3, -1}) == -1, "all negative circular uses one element");
        check(maxCircular(new int[] {1, 2, 3}) == 6, "the whole array is best");
        check(maxCircular(new int[] {3, -1, 2, -1}) == 4, "straight and wrapping tie at 4");
        check(maxCircular(new int[] {-5, 2, -1, 2}) == 3, "circular wraps the ends");
        System.out.println("circular     : known wrap-around cases pass");

        // ---- randomised cross-check -----------------------------------------
        Random random = new Random(20261106);
        int checked = 0;

        for (int trial = 0; trial < 500; trial++) {
            int size = 1 + random.nextInt(40);
            int[] values = new int[size];
            for (int index = 0; index < size; index++) {
                values[index] = random.nextInt(41) - 20;
            }

            int fast = kadane(values);
            check(divideAndConquer(values) == fast, "divide and conquer mismatch");
            check(prefixMinimum(values) == fast, "prefix minimum mismatch");
            check(bruteForce(values) == fast, "brute force mismatch");

            Result range = bestRange(values);
            check(range.sum == fast, "range total mismatch");
            int sum = 0;
            for (int index = range.start; index <= range.end; index++) {
                sum += values[index];
            }
            check(sum == fast, "reported indices do not sum to the answer");

            check(maxCircular(values) == bruteCircular(values), "circular mismatch");
            checked++;
        }
        System.out.println("random check : " + checked + " arrays agree across all variants");

        // ---- a larger input --------------------------------------------------
        int[] large = new int[200_000];
        for (int index = 0; index < large.length; index++) {
            large[index] = random.nextInt(2_001) - 1_000;
        }

        long started = System.nanoTime();
        Result largeResult = bestRange(large);
        long elapsed = System.nanoTime() - started;

        check(largeResult.sum == divideAndConquer(large), "large divide and conquer");
        check(largeResult.sum == prefixMinimum(large), "large prefix scan");
        System.out.printf("large input  : 200,000 values, best %d starting at %d in %.1f ms%n",
                largeResult.sum, largeResult.start, elapsed / 1_000_000.0);

        System.out.println("All checks passed.");
    }
}
