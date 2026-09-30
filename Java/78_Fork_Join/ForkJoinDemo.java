import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 78 - Fork/join: a divide and conquer task pool where idle threads steal work.
 * A RecursiveTask sums a range and a RecursiveAction sorts an array in parallel.
 *
 * Compile and run:
 *   javac ForkJoinDemo.java
 *   java ForkJoinDemo
 */
public class ForkJoinDemo {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /** Below this size a task just does the work instead of splitting again. */
    static final int THRESHOLD = 1_000;

    static final class SumTask extends RecursiveTask<Long> {
        private final long from;
        private final long to;
        private final AtomicInteger tasksRun;

        SumTask(long from, long to, AtomicInteger tasksRun) {
            this.from = from;
            this.to = to;
            this.tasksRun = tasksRun;
        }

        @Override
        protected Long compute() {
            tasksRun.incrementAndGet();
            if (to - from <= THRESHOLD) {
                long total = 0;
                for (long value = from; value < to; value++) {
                    total += value;
                }
                return total;
            }
            long middle = (from + to) / 2;
            SumTask left = new SumTask(from, middle, tasksRun);
            SumTask right = new SumTask(middle, to, tasksRun);

            left.fork();                        // hand the left half to the pool
            long rightTotal = right.compute();  // do the right half on this thread
            long leftTotal = left.join();       // then wait for the stolen half
            return leftTotal + rightTotal;
        }
    }

    /** Parallel merge sort: split, sort both halves, merge. */
    static final class SortTask extends RecursiveAction {
        private final int[] values;
        private final int low;
        private final int high;

        SortTask(int[] values, int low, int high) {
            this.values = values;
            this.low = low;
            this.high = high;
        }

        @Override
        protected void compute() {
            if (high - low <= THRESHOLD) {
                Arrays.sort(values, low, high);
                return;
            }
            int middle = (low + high) >>> 1;
            SortTask left = new SortTask(values, low, middle);
            SortTask right = new SortTask(values, middle, high);
            invokeAll(left, right);             // fork both, then wait for both

            int[] leftCopy = Arrays.copyOfRange(values, low, middle);
            int[] rightCopy = Arrays.copyOfRange(values, middle, high);
            int i = 0;
            int j = 0;
            int target = low;
            while (i < leftCopy.length && j < rightCopy.length) {
                values[target++] = leftCopy[i] <= rightCopy[j] ? leftCopy[i++] : rightCopy[j++];
            }
            while (i < leftCopy.length) {
                values[target++] = leftCopy[i++];
            }
            while (j < rightCopy.length) {
                values[target++] = rightCopy[j++];
            }
        }
    }

    static long sequentialSum(long from, long to) {
        long total = 0;
        for (long value = from; value < to; value++) {
            total += value;
        }
        return total;
    }

    public static void main(String[] args) throws Exception {
        long limit = 1_000_000;
        long expected = sequentialSum(0, limit);
        check(expected == 499_999_500_000L, "the sequential sum is the usual formula result");

        // ---- parallel sum ----------------------------------------------------
        AtomicInteger tasksRun = new AtomicInteger();
        try (ForkJoinPool pool = new ForkJoinPool()) {
            check(pool.getParallelism() >= 1, "the pool has at least one worker");
            System.out.println("pool        : parallelism=" + pool.getParallelism()
                    + ", workers=" + pool.getPoolSize());

            long parallel = pool.invoke(new SumTask(0, limit, tasksRun));
            check(parallel == expected, "the parallel sum matches the sequential one");
            check(tasksRun.get() > 1, "the range really was split, not done in one task");
            System.out.println("sum         : " + parallel + " in " + tasksRun.get() + " tasks");
        }

        // a range that fits in one chunk is a single task
        AtomicInteger single = new AtomicInteger();
        try (ForkJoinPool pool = new ForkJoinPool()) {
            check(pool.invoke(new SumTask(0, 500, single)) == sequentialSum(0, 500), "a small range sums");
            check(single.get() == 1, "and it does not bother splitting");
        }

        // ---- the shared pool -------------------------------------------------
        AtomicInteger commonTasks = new AtomicInteger();
        check(ForkJoinPool.commonPool().invoke(new SumTask(0, limit, commonTasks)) == expected,
                "the common pool works the same way");
        System.out.println("common pool : " + ForkJoinPool.commonPool().getParallelism() + " threads");

        // ---- parallel sort ---------------------------------------------------
        int[] random = new int[200_000];
        Random seeded = new Random(42);
        for (int i = 0; i < random.length; i++) {
            random[i] = seeded.nextInt(1_000_000);
        }
        int[] expectedSorted = random.clone();
        Arrays.sort(expectedSorted);

        try (ForkJoinPool pool = new ForkJoinPool()) {
            pool.invoke(new SortTask(random, 0, random.length));
        }
        check(Arrays.equals(random, expectedSorted), "the parallel merge sort matches Arrays.sort");
        System.out.println("sorted      : " + random.length + " values, first=" + random[0]
                + " last=" + random[random.length - 1]);

        // ---- boundary cases --------------------------------------------------
        for (int size : new int[] {0, 1, 2, THRESHOLD, THRESHOLD + 1}) {
            int[] values = new int[size];
            for (int i = 0; i < size; i++) {
                values[i] = size - i;      // reverse order, the worst case for merging
            }
            int[] copy = values.clone();
            Arrays.sort(copy);
            try (ForkJoinPool pool = new ForkJoinPool()) {
                pool.invoke(new SortTask(values, 0, values.length));
            }
            check(Arrays.equals(values, copy), "sorting " + size + " values matches Arrays.sort");
        }
        System.out.println("boundaries  : empty, single, and around the threshold all sort correctly");

        // a task submitted directly is awaited through the Future interface
        try (ForkJoinPool pool = new ForkJoinPool(2)) {
            AtomicInteger submitted = new AtomicInteger();
            check(pool.submit(new SumTask(0, 10_000, submitted)).get() == sequentialSum(0, 10_000),
                    "submit returns a future whose get() yields the result");
        }
        System.out.println("All checks passed.");
    }
}
