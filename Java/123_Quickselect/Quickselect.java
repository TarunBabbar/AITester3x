import java.util.Arrays;
import java.util.Random;

/**
 * 123 - Quickselect.
 *
 * Selection without a full sort: the array is partitioned three ways around a
 * median-of-three pivot and only the side holding the target index is kept.
 * Equal values end up in the middle, so duplicate-heavy inputs stay linear.
 * The input array is copied, and k is one-based.
 *
 * Compile and run:
 *   javac Quickselect.java
 *   java Quickselect
 */
public class Quickselect {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static void swap(int[] values, int left, int right) {
        int temporary = values[left];
        values[left] = values[right];
        values[right] = temporary;
    }

    static int medianOfThree(int[] values, int left, int right) {
        int middle = (left + right) / 2;
        int a = values[left];
        int b = values[middle];
        int c = values[right];
        if ((a <= b && b <= c) || (c <= b && b <= a)) {
            return b;
        }
        if ((b <= a && a <= c) || (c <= a && a <= b)) {
            return a;
        }
        return c;
    }

    static int selectInPlace(int[] values, int target) {
        int left = 0;
        int right = values.length - 1;

        while (true) {
            if (left == right) {
                return values[left];
            }

            int pivot = medianOfThree(values, left, right);
            int lower = left;
            int index = left;
            int upper = right;

            while (index <= upper) {
                if (values[index] < pivot) {
                    swap(values, lower, index);
                    lower++;
                    index++;
                } else if (values[index] > pivot) {
                    swap(values, index, upper);
                    upper--;
                } else {
                    index++;
                }
            }

            if (target < lower) {
                right = lower - 1;
            } else if (target > upper) {
                left = upper + 1;
            } else {
                return values[target];
            }
        }
    }

    static void validate(int[] values, int k) {
        if (values == null) {
            throw new IllegalArgumentException("values must not be null");
        }
        if (values.length == 0) {
            throw new IllegalArgumentException("values must not be empty");
        }
        if (k < 1 || k > values.length) {
            throw new IllegalArgumentException("k must be between 1 and " + values.length);
        }
    }

    static int kthSmallest(int[] values, int k) {
        validate(values, k);
        return selectInPlace(values.clone(), k - 1);
    }

    static int kthLargest(int[] values, int k) {
        validate(values, k);
        return selectInPlace(values.clone(), values.length - k);
    }

    static double median(int[] values) {
        validate(values, 1);
        int upper = kthSmallest(values, values.length / 2 + 1);
        if (values.length % 2 == 1) {
            return upper;
        }
        return (kthSmallest(values, values.length / 2) + (double) upper) / 2.0;
    }

    static boolean rejects(Runnable action) {
        try {
            action.run();
            return false;
        } catch (IllegalArgumentException error) {
            return true;
        }
    }

    static int[] randomValues(Random random, int length, int spread) {
        int[] values = new int[length];
        for (int index = 0; index < length; index++) {
            values[index] = random.nextInt(spread) - spread / 2;
        }
        return values;
    }

    public static void main(String[] args) {
        // ---- known selections -----------------------------------------------
        int[] sample = {7, 1, 5, 3, 9, 2};
        check(kthSmallest(sample, 1) == 1, "smallest");
        check(kthSmallest(sample, 2) == 2, "second smallest");
        check(kthSmallest(sample, 6) == 9, "largest");
        check(kthLargest(sample, 1) == 9, "largest through kthLargest");
        check(kthLargest(sample, 6) == 1, "smallest through kthLargest");
        System.out.println("known        : " + Arrays.toString(sample) + " | third smallest "
                + kthSmallest(sample, 3));

        // ---- sorted and reversed inputs -------------------------------------
        int[] sorted = {1, 2, 3, 4, 5};
        for (int k = 1; k <= sorted.length; k++) {
            check(kthSmallest(sorted, k) == k, "sorted k=" + k);
        }
        int[] reversed = {5, 4, 3, 2, 1};
        for (int k = 1; k <= reversed.length; k++) {
            check(kthSmallest(reversed, k) == k, "reversed k=" + k);
        }
        System.out.println("orderings    : sorted and reversed inputs return every order statistic");

        // ---- duplicates ------------------------------------------------------
        int[] repeated = {5, 1, 5, 1, 5};
        int[] repeatedExpected = {1, 1, 5, 5, 5};
        for (int k = 1; k <= repeated.length; k++) {
            check(kthSmallest(repeated, k) == repeatedExpected[k - 1], "duplicates k=" + k);
        }
        int[] equal = new int[1_000];
        Arrays.fill(equal, 7);
        check(kthSmallest(equal, 1) == 7 && kthSmallest(equal, 500) == 7, "all equal");
        check(kthSmallest(equal, 1_000) == 7, "all equal at the end");
        System.out.println("duplicates   : three-way partitioning keeps repeated values in the middle");

        // ---- input is untouched ---------------------------------------------
        int[] original = {9, -3, 14, 0, 7, 7, -8};
        int[] before = original.clone();
        kthSmallest(original, 3);
        kthLargest(original, 2);
        check(Arrays.equals(original, before), "selecting does not reorder the input");
        System.out.println("input        : " + Arrays.toString(original) + " after selecting");

        // ---- medians ---------------------------------------------------------
        check(median(new int[] {9, 1, 5}) == 5.0, "odd median");
        check(median(new int[] {4, 1, 3, 2}) == 2.5, "even median");
        check(median(new int[] {-5}) == -5.0, "single value");
        check(median(new int[] {1, 2}) == 1.5, "two values");
        System.out.println("median       : [4, 1, 3, 2] -> " + median(new int[] {4, 1, 3, 2}));

        // ---- random cross-check ---------------------------------------------
        Random random = new Random(123);
        for (int trial = 0; trial < 300; trial++) {
            int length = 1 + random.nextInt(40);
            int[] values = randomValues(random, length, 41);
            int[] sortedValues = values.clone();
            Arrays.sort(sortedValues);
            int k = 1 + random.nextInt(length);
            check(kthSmallest(values, k) == sortedValues[k - 1], "smallest mismatch at trial " + trial);
            check(kthLargest(values, k) == sortedValues[length - k], "largest mismatch at trial " + trial);
        }
        System.out.println("random       : 300 arrays match a full sort for smallest and largest");

        // ---- a large input ---------------------------------------------------
        int[] large = new int[200_000];
        for (int index = 0; index < large.length; index++) {
            large[index] = random.nextInt(1_000_000) - 500_000;
        }
        long sortedStarted = System.nanoTime();
        int[] largeSorted = large.clone();
        Arrays.sort(largeSorted);
        long sortedElapsed = System.nanoTime() - sortedStarted;

        long selectedStarted = System.nanoTime();
        int middle = kthSmallest(large, large.length / 2);
        long selectedElapsed = System.nanoTime() - selectedStarted;

        check(kthSmallest(large, 1) == largeSorted[0], "large minimum");
        check(middle == largeSorted[largeSorted.length / 2 - 1], "large median");
        check(kthSmallest(large, large.length) == largeSorted[largeSorted.length - 1], "large maximum");
        check(kthLargest(large, 1) == largeSorted[largeSorted.length - 1], "large maximum through kthLargest");
        System.out.printf("large input  : 200,000 values, middle %d in %.1f ms | sort %.1f ms%n",
                middle, selectedElapsed / 1_000_000.0, sortedElapsed / 1_000_000.0);

        // ---- validation ------------------------------------------------------
        check(rejects(() -> kthSmallest(new int[] {}, 1)), "empty rejected");
        check(rejects(() -> kthSmallest(new int[] {1, 2, 3}, 0)), "k=0 rejected");
        check(rejects(() -> kthSmallest(new int[] {1, 2, 3}, 4)), "k too large rejected");
        check(rejects(() -> kthSmallest(null, 1)), "null rejected");
        check(rejects(() -> kthLargest(new int[] {}, 1)), "empty rejected for kthLargest");
        check(rejects(() -> kthLargest(new int[] {1, 2}, 3)), "k too large for kthLargest");
        check(rejects(() -> median(new int[] {})), "empty median rejected");
        check(rejects(() -> median(null)), "null median rejected");
        System.out.println("validation   : empty inputs, null arrays, and out-of-range k rejected");

        System.out.println("All checks passed.");
    }
}
