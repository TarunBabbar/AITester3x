import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * 11 - Concurrency: threads, synchronization, atomic variables, executors and futures.
 *
 * Compile and run:
 *   javac ConcurrencyThreads.java
 *   java ConcurrencyThreads
 */
public class ConcurrencyThreads {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static class Counter {
        private int value = 0;

        synchronized void increment() {
            value++;
        }

        synchronized int value() {
            return value;
        }
    }

    static class AtomicCounter {
        private final AtomicInteger value = new AtomicInteger();

        void increment() {
            value.incrementAndGet();
        }

        int value() {
            return value.get();
        }
    }

    public static void main(String[] args) throws Exception {
        Counter counter = new Counter();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            Thread thread = new Thread(() -> {
                for (int j = 0; j < 1_000; j++) {
                    counter.increment();
                }
            });
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        check(counter.value() == 8_000, "synchronized counter reaches 8000");
        System.out.println("synchronized counter : " + counter.value());

        AtomicCounter atomic = new AtomicCounter();
        try (ExecutorService pool = Executors.newFixedThreadPool(4)) {
            for (int i = 0; i < 1_000; i++) {
                pool.submit(atomic::increment);
            }
        }
        check(atomic.value() == 1_000, "atomic counter reaches 1000");
        System.out.println("atomic counter       : " + atomic.value());

        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Future<Integer> future = pool.submit(() -> IntStream.rangeClosed(1, 100).sum());
            check(future.get() == 5050, "the future returns 5050");
            System.out.println("future sum           : " + future.get());
        }

        CompletableFuture<String> chain = CompletableFuture
                .supplyAsync(() -> "hello")
                .thenApply(String::toUpperCase)
                .thenApply(text -> text + "!")
                .exceptionally(error -> "failed: " + error.getMessage());
        check(chain.join().equals("HELLO!"), "the async chain builds HELLO!");
        System.out.println("completable future   : " + chain.join());

        CompletableFuture<Integer> recovered = CompletableFuture
                .<Integer>supplyAsync(() -> {
                    throw new IllegalStateException("boom");
                })
                .exceptionally(error -> -1);
        check(recovered.join() == -1, "exceptionally recovers from the failure");
        System.out.println("recovered value      : " + recovered.join());

        System.out.println("All checks passed.");
    }
}
