import java.util.Arrays;
import java.util.Random;

/**
 * 18 - Sorting algorithms: bubble, insertion, merge and quick sort, verified
 * against Arrays.sort and compared on a larger random array.
 *
 * Compile and run:
 *   javac SortingAlgorithms.java
 *   java SortingAlgorithms
 */
public class SortingAlgorithms {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static void bubbleSort(int[] values) {
        for (int pass = 0; pass < values.length - 1; pass++) {
            boolean swapped = false;
            for (int i = 0; i < values.length - 1 - pass; i++) {
                if (values[i] > values[i + 1]) {
                    int temporary = values[i];
                    values[i] = values[i + 1];
                    values[i + 1] = temporary;
                    swapped = true;
                }
            }
            if (!swapped) {
                break; // already sorted
            }
        }
    }

    static void insertionSort(int[] values) {
        for (int i = 1; i < values.length; i++) {
            int key = values[i];
            int j = i - 1;
            while (j >= 0 && values[j] > key) {
                values[j + 1] = values[j];
                j--;
            }
            values[j + 1] = key;
        }
    }

    static void mergeSort(int[] values) {
        if (values.length >= 2) {
            mergeSort(values, 0, values.length - 1);
        }
    }

    private static void mergeSort(int[] values, int low, int high) {
        if (low >= high) {
            return;
        }
        int middle = (low + high) >>> 1;
        mergeSort(values, low, middle);
        mergeSort(values, middle + 1, high);
        merge(values, low, middle, high);
    }

    private static void merge(int[] values, int low, int middle, int high) {
        int[] left = Arrays.copyOfRange(values, low, middle + 1);
        int[] right = Arrays.copyOfRange(values, middle + 1, high + 1);
        int i = 0;
        int j = 0;
        int target = low;
        while (i < left.length && j < right.length) {
            values[target++] = left[i] <= right[j] ? left[i++] : right[j++];
        }
        while (i < left.length) {
            values[target++] = left[i++];
        }
        while (j < right.length) {
            values[target++] = right[j++];
        }
    }

    static void quickSort(int[] values) {
        if (values.length >= 2) {
            quickSort(values, 0, values.length - 1);
        }
    }

    private static void quickSort(int[] values, int low, int high) {
        if (low >= high) {
            return;
        }
        int pivotIndex = partition(values, low, high);
        quickSort(values, low, pivotIndex - 1);
        quickSort(values, pivotIndex + 1, high);
    }

    private static int partition(int[] values, int low, int high) {
        int pivot = values[high];
        int store = low;
        for (int i = low; i < high; i++) {
            if (values[i] < pivot) {
                int temporary = values[i];
                values[i] = values[store];
                values[store] = temporary;
                store++;
            }
        }
        int temporary = values[store];
        values[store] = values[high];
        values[high] = temporary;
        return store;
    }

    static long timeMillis(Runnable algorithm) {
        long start = System.nanoTime();
        algorithm.run();
        return (System.nanoTime() - start) / 1_000_000;
    }

    public static void main(String[] args) {
        int[][] samples = {
                {5, 2, 9, 1, 5, 6, -3, 8},
                {},
                {7},
                {3, 3, 3, 2, 1},
                {1, 2, 3, 4, 5},
        };

        for (int[] sample : samples) {
            int[] expected = sample.clone();
            Arrays.sort(expected);

            int[] bubble = sample.clone();
            bubbleSort(bubble);
            int[] insertion = sample.clone();
            insertionSort(insertion);
            int[] merge = sample.clone();
            mergeSort(merge);
            int[] quick = sample.clone();
            quickSort(quick);

            check(Arrays.equals(bubble, expected), "bubble sort matches Arrays.sort");
            check(Arrays.equals(insertion, expected), "insertion sort matches Arrays.sort");
            check(Arrays.equals(merge, expected), "merge sort matches Arrays.sort");
            check(Arrays.equals(quick, expected), "quick sort matches Arrays.sort");
        }
        System.out.println("All four algorithms agree with Arrays.sort on "
                + samples.length + " sample arrays.");

        int size = 2_000;
        int[] large = new int[size];
        Random random = new Random(42); // fixed seed keeps the run repeatable
        for (int i = 0; i < size; i++) {
            large[i] = random.nextInt(100_000);
        }
        int[] expected = large.clone();
        Arrays.sort(expected);

        int[] bubble = large.clone();
        long bubbleMs = timeMillis(() -> bubbleSort(bubble));
        int[] insertion = large.clone();
        long insertionMs = timeMillis(() -> insertionSort(insertion));
        int[] merge = large.clone();
        long mergeMs = timeMillis(() -> mergeSort(merge));
        int[] quick = large.clone();
        long quickMs = timeMillis(() -> quickSort(quick));

        check(Arrays.equals(bubble, expected) && Arrays.equals(insertion, expected)
                && Arrays.equals(merge, expected) && Arrays.equals(quick, expected),
                "every algorithm sorted the random array correctly");

        System.out.printf("bubble    : %4d ms%n", bubbleMs);
        System.out.printf("insertion : %4d ms%n", insertionMs);
        System.out.printf("merge     : %4d ms%n", mergeMs);
        System.out.printf("quick     : %4d ms%n", quickMs);
        System.out.println("All checks passed.");
    }
}
