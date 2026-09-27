import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 51 - Virtual threads (Java 21+): cheap threads that block without wasting a
 * platform thread, plus the executor and factory that create them.
 *
 * Compile and run:
 *   javac VirtualThreads.java
 *   java VirtualThreads
 */
public class VirtualThreads {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) throws Exception {
        check(!Thread.currentThread().isVirtual(), "main is an ordinary platform thread");
        System.out.println("main        : " + Thread.currentThread());

        // ---- starting one directly ------------------------------------------
        AtomicInteger onVirtual = new AtomicInteger();
        Thread single = Thread.ofVirtual().name("qa-vthread").start(() -> {
            if (Thread.currentThread().isVirtual()) {
                onVirtual.incrementAndGet();
            }
            System.out.println("  task ran on: " + Thread.currentThread());
        });
        single.join();

        check(onVirtual.get() == 1, "the task really ran on a virtual thread");
        check(single.isVirtual(), "the handle says virtual");
        check(single.getName().equals("qa-vthread"), "the name was applied");

        // ---- the factory numbers the threads ---------------------------------
        ThreadFactory factory = Thread.ofVirtual().name("worker-", 0).factory();
        List<Thread> workers = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            workers.add(factory.newThread(() -> {
            }));
        }
        check(workers.get(0).getName().equals("worker-0"), "the factory starts numbering at the given value");
        check(workers.get(1).getName().equals("worker-1"), "and counts up");
        check(workers.stream().allMatch(Thread::isVirtual), "every thread from the factory is virtual");
        for (Thread worker : workers) {
            worker.start();
        }
        for (Thread worker : workers) {
            worker.join();
        }
        System.out.println("factory     : " + workers.get(0).getName() + " .. "
                + workers.get(workers.size() - 1).getName());

        // ---- many blocking tasks at once -------------------------------------
        // 10,000 threads each sleeping 10ms. A platform-thread pool of this size
        // would need 10,000 OS threads; virtual threads need only a few carriers.
        int tasks = 10_000;
        CountDownLatch finished = new CountDownLatch(tasks);
        AtomicInteger completed = new AtomicInteger();
        long start = System.nanoTime();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < tasks; i++) {
                executor.submit(() -> {
                    try {
                        Thread.sleep(10);
                        completed.incrementAndGet();
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                    } finally {
                        finished.countDown();
                    }
                });
            }
        }   // close() waits for every task, like awaitTermination

        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;
        check(finished.getCount() == 0, "every task finished");
        check(completed.get() == tasks, "every task completed its work");
        check(elapsedMillis < 30_000, "the whole batch finished promptly");
        System.out.printf("%d virtual threads, 10ms sleep each, finished in %d ms%n",
                tasks, elapsedMillis);

        // If they ran 10,000-at-a-time-per-carrier this would still be ~10ms of
        // sleeping; the point is that it did not need 10,000 OS threads to do it.
        System.out.println("carriers    : " + Runtime.getRuntime().availableProcessors()
                + " CPUs are enough to drive all of them");
        System.out.println("All checks passed.");
    }
}
