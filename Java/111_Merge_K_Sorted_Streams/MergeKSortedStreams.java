import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * 111 - Merging k sorted streams with a heap.
 *
 * Each stream is represented by a cursor at its current element. The heap holds
 * one cursor per stream, ordered by that element, so the next value in the merged
 * output is always the smallest of k candidates: O(n log k) instead of collecting
 * everything and sorting in O(n log n).
 *
 * The same cursor idea answers "what is the kth smallest value across all
 * streams?" without merging them.
 *
 * Compile and run:
 *   javac MergeKSortedStreams.java
 *   java MergeKSortedStreams
 */
public class MergeKSortedStreams {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class Cursor {
        final int[] values;
        int index;

        Cursor(int[] values) {
            this.values = values;
        }

        boolean hasNext() {
            return index < values.length;
        }

        int next() {
            return values[index++];
        }
    }

    private static PriorityQueue<Cursor> queueOf(int[]... lists) {
        PriorityQueue<Cursor> queue = new PriorityQueue<>(
                Comparator.comparingInt(cursor -> cursor.values[cursor.index]));
        for (int[] list : lists) {
            if (list.length > 0) {
                queue.add(new Cursor(list));
            }
        }
        return queue;
    }

    static int[] mergeSorted(int[]... lists) {
        int total = 0;
        for (int[] list : lists) {
            total += list.length;
        }

        int[] result = new int[total];
        PriorityQueue<Cursor> queue = queueOf(lists);
        int position = 0;

        while (!queue.isEmpty()) {
            Cursor cursor = queue.poll();
            result[position++] = cursor.next();
            if (cursor.hasNext()) {
                queue.add(cursor);
            }
        }

        return result;
    }

    static int kthSmallest(int[][] lists, int k) {
        int total = 0;
        for (int[] list : lists) {
            total += list.length;
        }
        if (k < 1 || k > total) {
            throw new IllegalArgumentException("k=" + k + " is outside 1.." + total);
        }

        PriorityQueue<Cursor> queue = queueOf(lists);
        int value = 0;
        for (int step = 0; step < k; step++) {
            Cursor cursor = queue.poll();
            value = cursor.next();
            if (cursor.hasNext()) {
                queue.add(cursor);
            }
        }
        return value;
    }

    static int[] mergeNaive(int[]... lists) {
        int total = 0;
        for (int[] list : lists) {
            total += list.length;
        }
        int[] result = new int[total];
        int position = 0;
        for (int[] list : lists) {
            System.arraycopy(list, 0, result, position, list.length);
            position += list.length;
        }
        Arrays.sort(result);
        return result;
    }

    public static void main(String[] args) {
        // ---- basic merge -----------------------------------------------------
        int[] merged = mergeSorted(
                new int[] {1, 4, 7},
                new int[] {2, 5, 8},
                new int[] {3, 6, 9});
        check(Arrays.equals(merged, new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9}), Arrays.toString(merged));
        System.out.println("basic        : " + Arrays.toString(merged));

        // ---- uneven and empty streams ----------------------------------------
        int[] uneven = mergeSorted(
                new int[] {10},
                new int[] {},
                new int[] {1, 2, 3, 20, 30},
                new int[] {15, 25});
        check(Arrays.equals(uneven, new int[] {1, 2, 3, 10, 15, 20, 25, 30}), Arrays.toString(uneven));
        check(mergeSorted().length == 0, "no streams means no values");
        check(mergeSorted(new int[] {}, new int[] {}).length == 0, "empty streams are skipped");
        System.out.println("uneven       : " + Arrays.toString(uneven));

        // ---- duplicates and negatives ----------------------------------------
        int[] duplicates = mergeSorted(
                new int[] {-5, 0, 5},
                new int[] {-5, 5},
                new int[] {0});
        check(Arrays.equals(duplicates, new int[] {-5, -5, 0, 0, 5, 5}), Arrays.toString(duplicates));
        System.out.println("duplicates   : " + Arrays.toString(duplicates));

        // ---- int extremes ----------------------------------------------------
        int[] extremes = mergeSorted(
                new int[] {Integer.MIN_VALUE, 0},
                new int[] {Integer.MAX_VALUE},
                new int[] {Integer.MIN_VALUE, -1});
        check(Arrays.equals(extremes,
                new int[] {Integer.MIN_VALUE, Integer.MIN_VALUE, -1, 0, Integer.MAX_VALUE}),
                Arrays.toString(extremes));
        System.out.println("extremes     : " + Arrays.toString(extremes));

        // ---- kth smallest ----------------------------------------------------
        int[][] lists = {
            {1, 5, 9},
            {2, 6, 10},
            {3, 7, 11, 12},
        };
        check(kthSmallest(lists, 1) == 1, "first smallest");
        check(kthSmallest(lists, 5) == 6, "fifth smallest");
        check(kthSmallest(lists, 10) == 12, "last smallest");
        try {
            kthSmallest(lists, 0);
            throw new AssertionError("k=0 should be rejected");
        } catch (IllegalArgumentException expected) {
            System.out.println("k out of range: " + expected.getMessage());
        }
        try {
            kthSmallest(lists, 11);
            throw new AssertionError("k above the total should be rejected");
        } catch (IllegalArgumentException expected) {
            check(expected.getMessage().contains("1..10"), expected.getMessage());
        }
        System.out.println("kth smallest : 1st=1, 5th=6, 10th=12");

        // ---- randomised cross-check ------------------------------------------
        Random random = new Random(20261106);
        int checked = 0;
        for (int trial = 0; trial < 300; trial++) {
            int streamCount = random.nextInt(8);
            int[][] streams = new int[streamCount][];
            for (int stream = 0; stream < streamCount; stream++) {
                int length = random.nextInt(12);
                streams[stream] = new int[length];
                for (int index = 0; index < length; index++) {
                    streams[stream][index] = random.nextInt(100) - 50;
                }
                Arrays.sort(streams[stream]);
            }

            int[] actual = mergeSorted(streams);
            int[] expected = mergeNaive(streams);
            check(Arrays.equals(actual, expected), "merge mismatch for " + streamCount + " streams");

            int total = expected.length;
            if (total > 0) {
                for (int sample = 0; sample < 3; sample++) {
                    int k = random.nextInt(total) + 1;
                    check(kthSmallest(streams, k) == expected[k - 1], "kth smallest mismatch at k=" + k);
                }
            }
            checked++;
        }
        System.out.println("random check : " + checked + " stream sets match the naive merge");

        // ---- a larger merge ---------------------------------------------------
        int[][] many = new int[100][1_000];
        for (int stream = 0; stream < many.length; stream++) {
            for (int index = 0; index < many[stream].length; index++) {
                many[stream][index] = random.nextInt(1_000_000);
            }
            Arrays.sort(many[stream]);
        }

        long started = System.nanoTime();
        int[] large = mergeSorted(many);
        long elapsed = System.nanoTime() - started;

        check(large.length == 100_000, "all values merged");
        check(kthSmallest(many, 1) == large[0], "smallest agrees");
        check(kthSmallest(many, large.length) == large[large.length - 1], "largest agrees");
        for (int index = 1; index < large.length; index++) {
            check(large[index - 1] <= large[index], "output must be sorted");
        }
        System.out.printf("large merge  : %d streams, %d values in %.1f ms%n",
                many.length, large.length, elapsed / 1_000_000.0);

        // ---- one stream, already sorted --------------------------------------
        int[] single = {1, 2, 3, 4};
        check(Arrays.equals(mergeSorted(single), single), "a single stream is unchanged");
        check(kthSmallest(new int[][] {single}, 4) == 4, "kth smallest in one stream");

        System.out.println("All checks passed.");
    }
}
