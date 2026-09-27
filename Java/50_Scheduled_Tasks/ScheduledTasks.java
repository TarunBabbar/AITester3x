import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 50 - Scheduled tasks: one-shot delays, fixed-rate and fixed-delay repetition,
 * cancelling, exceptions from a task, and an orderly shutdown.
 *
 * Compile and run:
 *   javac ScheduledTasks.java
 *   java ScheduledTasks
 */
public class ScheduledTasks {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) throws Exception {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
        try {
            // ---- a single delayed task -----------------------------------------
            ScheduledFuture<String> delayed = scheduler.schedule(
                    () -> "done", 150, TimeUnit.MILLISECONDS);
            check(!delayed.isDone(), "it has not run yet");
            check(delayed.get().equals("done"), "the task's return value comes back");
            check(delayed.isDone(), "and it is finished");

            // ---- scheduleAtFixedRate -------------------------------------------
            AtomicInteger ticks = new AtomicInteger();
            CountDownLatch threeTicks = new CountDownLatch(3);
            ScheduledFuture<?> periodic = scheduler.scheduleAtFixedRate(() -> {
                if (ticks.incrementAndGet() >= 3) {
                    threeTicks.countDown();
                }
            }, 0, 60, TimeUnit.MILLISECONDS);

            check(threeTicks.await(5, TimeUnit.SECONDS), "the fixed-rate task ran three times");
            periodic.cancel(false);
            check(periodic.isCancelled(), "the future reports itself cancelled");
            int afterCancel = ticks.get();
            Thread.sleep(200);
            check(ticks.get() <= afterCancel + 1,
                    "a cancelled task stops, allowing at most the run already in flight");
            System.out.println("fixed rate  : ran " + afterCancel + " times, then cancelled");

            // ---- scheduleWithFixedDelay ----------------------------------------
            List<Long> gaps = new CopyOnWriteArrayList<>();
            AtomicLong last = new AtomicLong(System.nanoTime());
            CountDownLatch delayedTicks = new CountDownLatch(3);
            ScheduledFuture<?> withDelay = scheduler.scheduleWithFixedDelay(() -> {
                long now = System.nanoTime();
                gaps.add(TimeUnit.NANOSECONDS.toMillis(now - last.getAndSet(now)));
                delayedTicks.countDown();
            }, 0, 50, TimeUnit.MILLISECONDS);

            check(delayedTicks.await(5, TimeUnit.SECONDS), "the fixed-delay task ran three times");
            withDelay.cancel(false);
            check(gaps.size() >= 3, "at least three gaps were measured");
            long laterGaps = gaps.stream().skip(1).mapToLong(Long::longValue).min().orElse(0);
            check(laterGaps >= 30, "the delay really is applied between runs, not between starts");
            System.out.println("fixed delay : gaps " + gaps + " ms");

            // ---- a task that throws ---------------------------------------------
            ScheduledFuture<?> failing = scheduler.schedule(() -> {
                throw new IllegalStateException("boom");
            }, 0, TimeUnit.MILLISECONDS);
            try {
                failing.get();
                throw new AssertionError("the exception should surface at get()");
            } catch (ExecutionException expected) {
                check(expected.getCause().getMessage().equals("boom"), "the cause is preserved");
                System.out.println("throwing    : " + expected.getCause());
            }

            // ---- an immediate task still goes through the scheduler -------------
            check(scheduler.submit(() -> 21 * 2).get() == 42, "submit still works on this executor");
        } finally {
            scheduler.shutdown();
            check(scheduler.awaitTermination(5, TimeUnit.SECONDS), "the scheduler stopped promptly");
            check(scheduler.isShutdown(), "and reports itself shut down");
            System.out.println("shutdown    : clean");
        }
        System.out.println("All checks passed.");
    }
}
