import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * 54 - A binary heap from scratch: sift up and sift down, heap sort in place,
 * and top-K selection using a heap as a sieve. Verified against Arrays.sort
 * and java.util.PriorityQueue.
 *
 * Compile and run:
 *   javac BinaryHeap.java
 *   java BinaryHeap
 */
public class BinaryHeap {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /** A min-heap in an array: children of i are 2i+1 and 2i+2. */
    static final class MinHeap {
        private int[] heap = new int[8];
        private int size;

        void add(int value) {
            if (size == heap.length) {
                heap = Arrays.copyOf(heap, size * 2);
            }
            heap[size] = value;
            siftUp(size);
            size++;
        }

        int peek() {
            if (size == 0) {
                throw new NoSuchElementException("the heap is empty");
            }
            return heap[0];
        }

        int poll() {
            int smallest = peek();
            size--;
            heap[0] = heap[size];
            siftDown(0, size);
            return smallest;
        }

        int size() {
            return size;
        }

        boolean isEmpty() {
            return size == 0;
        }

        /** The heap property: no child may be smaller than its parent. */
        boolean isValid() {
            for (int child = 1; child < size; child++) {
                if (heap[(child - 1) / 2] > heap[child]) {
                    return false;
                }
            }
            return true;
        }

        int[] snapshot() {
            return Arrays.copyOf(heap, size);
        }

        private void siftUp(int index) {
            while (index > 0) {
                int parent = (index - 1) / 2;
                if (heap[parent] <= heap[index]) {
                    return;
                }
                swap(parent, index);
                index = parent;
            }
        }

        private void siftDown(int index, int limit) {
            while (true) {
                int left = 2 * index + 1;
                int right = left + 1;
                int smallest = index;
                if (left < limit && heap[left] < heap[smallest]) {
                    smallest = left;
                }
                if (right < limit && heap[right] < heap[smallest]) {
                    smallest = right;
                }
                if (smallest == index) {
                    return;
                }
                swap(index, smallest);
                index = smallest;
            }
        }

        private void swap(int a, int b) {
            int temporary = heap[a];
            heap[a] = heap[b];
            heap[b] = temporary;
        }
    }

    /** Sift down, but for a MAX-heap: used by the in-place heap sort. */
    private static void siftDownMax(int[] values, int index, int limit) {
        while (true) {
            int left = 2 * index + 1;
            int right = left + 1;
            int largest = index;
            if (left < limit && values[left] > values[largest]) {
                largest = left;
            }
            if (right < limit && values[right] > values[largest]) {
                largest = right;
            }
            if (largest == index) {
                return;
            }
            int temporary = values[index];
            values[index] = values[largest];
            values[largest] = temporary;
            index = largest;
        }
    }

    static void heapSort(int[] values) {
        for (int index = values.length / 2 - 1; index >= 0; index--) {
            siftDownMax(values, index, values.length);
        }
        for (int end = values.length - 1; end > 0; end--) {
            int largest = values[0];
            values[0] = values[end];
            values[end] = largest;
            siftDownMax(values, 0, end);
        }
    }

    /** The k largest values, found with a min-heap of size k as a sieve. */
    static List<Integer> topK(int[] values, int k) {
        MinHeap heap = new MinHeap();
        for (int value : values) {
            if (heap.size() < k) {
                heap.add(value);
            } else if (value > heap.peek()) {
                heap.poll();
                heap.add(value);
            }
        }
        List<Integer> result = new ArrayList<>();
        while (!heap.isEmpty()) {
            result.add(heap.poll());
        }
        result.sort(Comparator.reverseOrder());
        return result;
    }

    public static void main(String[] args) {
        // ---- the heap itself -------------------------------------------------
        MinHeap heap = new MinHeap();
        for (int value : new int[] {5, 3, 8, 1, 9, 2, 7}) {
            heap.add(value);
            check(heap.isValid(), "the heap property holds after adding " + value);
        }
        check(heap.size() == 7, "seven values");
        check(heap.peek() == 1, "the smallest sits on top");

        List<Integer> drained = new ArrayList<>();
        while (!heap.isEmpty()) {
            drained.add(heap.poll());
            check(heap.isValid(), "still a heap after each poll");
        }
        check(drained.equals(List.of(1, 2, 3, 5, 7, 8, 9)), "polling yields sorted order");
        System.out.println("heap poll   : " + drained);

        try {
            new MinHeap().poll();
            throw new AssertionError("polling an empty heap should fail");
        } catch (NoSuchElementException expected) {
            System.out.println("empty heap  : " + expected.getMessage());
        }

        // ---- heap sort against Arrays.sort ------------------------------------
        int[][] samples = {
                {5, 2, 9, 1, 5, 6, -3, 8},
                {},
                {7},
                {3, 3, 3},
                {5, 4, 3, 2, 1},
                {1, 2, 3, 4, 5},
        };
        for (int[] sample : samples) {
            int[] expected = sample.clone();
            Arrays.sort(expected);
            int[] actual = sample.clone();
            heapSort(actual);
            check(Arrays.equals(actual, expected), "heap sort matches Arrays.sort for " + sample.length + " items");
        }
        System.out.println("heap sort   : agrees with Arrays.sort on " + samples.length + " arrays");

        int[] random = new int[2_000];
        Random seeded = new Random(7);
        for (int i = 0; i < random.length; i++) {
            random[i] = seeded.nextInt(10_000);
        }
        int[] expected = random.clone();
        Arrays.sort(expected);
        int[] sorted = random.clone();
        heapSort(sorted);
        check(Arrays.equals(sorted, expected), "heap sort handles 2,000 random values");

        // ---- top-K -----------------------------------------------------------
        List<Integer> leaders = topK(random, 5);
        check(leaders.size() == 5, "five leaders requested");
        check(leaders.equals(List.of(expected[1999], expected[1998], expected[1997],
                expected[1996], expected[1995])), "the five largest, in order");
        check(topK(new int[] {4, 4, 4}, 2).equals(List.of(4, 4)), "duplicates are kept");
        check(topK(new int[] {1, 2}, 5).size() == 2, "asking for more than there is returns everything");
        System.out.println("top 5       : " + leaders);

        // ---- the standard library's heap for comparison ----------------------
        PriorityQueue<Integer> libraryHeap = new PriorityQueue<>();
        for (int value : new int[] {5, 3, 8, 1, 9}) {
            libraryHeap.add(value);
        }
        List<Integer> fromLibrary = new ArrayList<>();
        while (!libraryHeap.isEmpty()) {
            fromLibrary.add(libraryHeap.poll());
        }
        check(fromLibrary.equals(List.of(1, 3, 5, 8, 9)), "PriorityQueue agrees with our heap");
        System.out.println("PriorityQueue: " + fromLibrary);
        System.out.println("All checks passed.");
    }
}
