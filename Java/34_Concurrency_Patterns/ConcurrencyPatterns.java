import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

/**
 * 34 - Concurrency patterns: a producer/consumer pair over a bounded queue, a
 * ReentrantLock, CompletableFuture composition and a parallel stream.
 *
 * Compile and run:
 *   javac ConcurrencyPatterns.java
 *   java ConcurrencyPatterns
 */
public class ConcurrencyPatterns {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) throws Exception {
        // ---- producer / consumer over a bounded queue ----------------------
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(5);
        int itemCount = 20;
        List<Integer> consumed = new ArrayList<>();

        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= itemCount; i++) {
                    queue.put(i);            // blocks while the queue is full
                }
                queue.put(-1);               // poison pill ends the consumer
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        });
        Thread consumer = new Thread(() -> {
            try {
                while (true) {
                    int value = queue.take();    // blocks while the queue is empty
                    if (value < 0) {
                        break;
                    }
                    consumed.add(value);
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        });

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();                         // join makes the list safe to read

        check(consumed.size() == itemCount, "every produced item was consumed");
        check(consumed.get(0) == 1 && consumed.get(itemCount - 1) == itemCount,
                "a single producer and consumer keep FIFO order");
        System.out.println("queue        : " + consumed.size() + " items, first="
                + consumed.get(0) + " last=" + consumed.get(itemCount - 1));

        // ---- ReentrantLock guarding shared state ---------------------------
        ReentrantLock lock = new ReentrantLock();
        int[] counter = {0};
        try (ExecutorService pool = Executors.newFixedThreadPool(4)) {
            for (int i = 0; i < 1_000; i++) {
                pool.submit(() -> {
                    lock.lock();
                    try {
                        counter[0]++;
                    } finally {
                        lock.unlock();       // always release in a finally block
                    }
                });
            }
        }
        check(counter[0] == 1_000, "the lock-protected counter is exact");
        System.out.println("reentrant lock: " + counter[0]);

        // ---- CompletableFuture composition ---------------------------------
        CompletableFuture<String> combined = CompletableFuture
                .supplyAsync(() -> "order-42")
                .thenCompose(id -> CompletableFuture.supplyAsync(() -> "price of " + id))
                .thenCombine(CompletableFuture.supplyAsync(() -> 17.27),
                        (description, amount) -> description + " = " + amount);
        check(combined.join().equals("price of order-42 = 17.27"), "thenCompose + thenCombine");
        System.out.println("composed     : " + combined.join());

        CompletableFuture<Integer> first = CompletableFuture.supplyAsync(() -> 1);
        CompletableFuture<Integer> second = CompletableFuture.supplyAsync(() -> 2);
        CompletableFuture<Integer> third = CompletableFuture.supplyAsync(() -> 3);
        CompletableFuture.allOf(first, second, third).join();
        check(first.join() + second.join() + third.join() == 6, "allOf waits for all three");

        CompletableFuture<String> recovered = CompletableFuture
                .<String>supplyAsync(() -> {
                    throw new IllegalStateException("upstream failed");
                })
                .handle((value, error) -> {
                    if (error == null) {
                        return value;
                    }
                    // supplyAsync wraps the throwable, so unwrap before reading it.
                    Throwable cause = error.getCause() == null ? error : error.getCause();
                    return "default-" + cause.getMessage();
                });
        check(recovered.join().equals("default-upstream failed"), "handle recovers from a failure");
        System.out.println("handled      : " + recovered.join());

        // ---- parallel stream ------------------------------------------------
        long sum = IntStream.rangeClosed(1, 1_000_000).asLongStream().parallel().sum();
        check(sum == 500_000_500_000L, "1..1,000,000 sums to 500000500000");
        System.out.println("parallel sum : " + sum);
        System.out.println("All checks passed.");
    }
}
