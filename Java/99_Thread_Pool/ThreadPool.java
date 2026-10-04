import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 99 - A thread pool written from scratch: a fixed set of workers pulling from a
 * bounded queue, with a rejection policy when it is saturated, a graceful
 * shutdown that drains the queue, and metrics to prove what happened.
 *
 * Compile and run:
 *   javac ThreadPool.java
 *   java ThreadPool
 */
public class ThreadPool {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static class RejectedExecutionException extends RuntimeException {
        RejectedExecutionException(String message) {
            super(message);
        }
    }

    enum RejectionPolicy {
        /** Refuse the task and tell the caller. */
        ABORT,
        /** Run it on the caller's thread, which slows the producer down. */
        CALLER_RUNS
    }

    static final class Pool implements AutoCloseable {
        private final BlockingQueue<Runnable> queue;
        private final List<Thread> workers = new ArrayList<>();
        private final RejectionPolicy policy;
        private final AtomicInteger submitted = new AtomicInteger();
        private final AtomicInteger completed = new AtomicInteger();
        private final AtomicInteger failed = new AtomicInteger();
        private final AtomicInteger rejected = new AtomicInteger();
        private final AtomicInteger running = new AtomicInteger();
        private volatile boolean accepting = true;

        Pool(int workerCount, int queueCapacity, RejectionPolicy policy) {
            if (workerCount < 1 || queueCapacity < 1) {
                throw new IllegalArgumentException("workerCount and queueCapacity must be at least 1");
            }
            this.policy = policy;
            this.queue = new LinkedBlockingQueue<>(queueCapacity);
            for (int i = 0; i < workerCount; i++) {
                Thread worker = new Thread(this::work, "pool-worker-" + i);
                worker.setDaemon(true);
                workers.add(worker);
                worker.start();
            }
        }

        private void work() {
            while (true) {
                Runnable task;
                try {
                    task = queue.poll(100, TimeUnit.MILLISECONDS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return;                             // interrupted: give up the rest of the queue
                }
                if (task == null) {
                    // Exit only once the pool is closed AND nothing is left to run.
                    if (!accepting && queue.isEmpty()) {
                        return;
                    }
                    continue;
                }
                running.incrementAndGet();
                try {
                    task.run();
                    completed.incrementAndGet();
                } catch (RuntimeException failure) {
                    failed.incrementAndGet();            // one bad task must not kill the worker
                } finally {
                    running.decrementAndGet();
                }
            }
        }

        void submit(Runnable task) {
            if (!accepting) {
                rejected.incrementAndGet();
                throw new RejectedExecutionException("the pool is shut down");
            }
            submitted.incrementAndGet();
            if (queue.offer(task)) {
                return;
            }
            if (policy == RejectionPolicy.CALLER_RUNS) {
                task.run();                             // apply back pressure to the caller
                completed.incrementAndGet();
                return;
            }
            rejected.incrementAndGet();
            throw new RejectedExecutionException("the queue is full");
        }

        /** Stop accepting, let the queue drain, and wait for the workers. */
        boolean shutdown(long timeoutMillis) {
            accepting = false;
            long deadline = System.nanoTime() + timeoutMillis * 1_000_000L;
            for (Thread worker : workers) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) {
                    return false;
                }
                try {
                    worker.join(Math.max(1, remaining / 1_000_000L));
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return workers.stream().noneMatch(Thread::isAlive);
        }

        /** Abandon whatever is still queued. */
        int shutdownNow() {
            accepting = false;
            int abandoned = queue.size();
            queue.clear();
            for (Thread worker : workers) {
                worker.interrupt();
            }
            for (Thread worker : workers) {
                try {
                    worker.join(2_000);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
            return abandoned;
        }

        @Override
        public void close() {
            shutdown(5_000);
        }

        int queueDepth() {
            return queue.size();
        }

        int workerCount() {
            return workers.size();
        }

        int submitted() {
            return submitted.get();
        }

        int completed() {
            return completed.get();
        }

        int failed() {
            return failed.get();
        }

        int rejected() {
            return rejected.get();
        }

        int running() {
            return running.get();
        }

        boolean isAccepting() {
            return accepting;
        }
    }

    public static void main(String[] args) throws Exception {
        // ---- every task runs exactly once ------------------------------------
        // The queue has to be big enough to hold the burst: a bounded queue will
        // genuinely refuse work when the producers outrun the workers, as the
        // saturation test below shows on purpose.
        AtomicInteger counter = new AtomicInteger();
        try (Pool pool = new Pool(4, 2_000, RejectionPolicy.ABORT)) {
            for (int i = 0; i < 1_000; i++) {
                pool.submit(counter::incrementAndGet);
            }
        }
        check(counter.get() == 1_000, "all thousand tasks ran: " + counter.get());
        System.out.println("throughput   : 1000 tasks across 4 workers");

        Pool measured = new Pool(4, 100, RejectionPolicy.ABORT);
        try {
            for (int i = 0; i < 100; i++) {
                measured.submit(() -> {
                });
            }
            measured.shutdown(5_000);
            check(measured.submitted() == 100, "the submissions were counted");
            check(measured.completed() == 100, "and so were the completions");
            check(measured.rejected() == 0, "nothing was refused");
            check(measured.failed() == 0, "and nothing threw");
            check(measured.queueDepth() == 0, "the queue drained");
            System.out.println("metrics      : submitted " + measured.submitted()
                    + ", completed " + measured.completed() + ", rejected " + measured.rejected());
        } finally {
            measured.shutdownNow();
        }

        // ---- the workers really are concurrent --------------------------------
        CountDownLatch together = new CountDownLatch(8);
        Pool parallel = new Pool(8, 8, RejectionPolicy.ABORT);
        try {
            for (int i = 0; i < 8; i++) {
                parallel.submit(together::countDown);
            }
            check(together.await(5, TimeUnit.SECONDS),
                    "eight tasks started before any finished, so eight workers are alive");
            check(parallel.running() <= 8, "and no more than eight run at once: " + parallel.running());
        } finally {
            parallel.shutdownNow();
        }
        System.out.println("concurrency  : eight tasks held the latch down at the same time");

        // ---- a saturated pool refuses work -----------------------------------
        CountDownLatch blocked = new CountDownLatch(1);
        Pool saturated = new Pool(1, 1, RejectionPolicy.ABORT);
        try {
            saturated.submit(() -> {
                try {
                    blocked.await(5, TimeUnit.SECONDS);    // occupies the only worker
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            });
            saturated.submit(() -> {
            });                                            // waits in the single queue slot
            Thread.sleep(100);                             // let the worker pick up the first task
            try {
                saturated.submit(() -> {
                });
                throw new AssertionError("the third task should have been refused");
            } catch (RejectedExecutionException expected) {
                check(saturated.rejected() == 1, "the refusal was counted");
                System.out.println("abort        : " + expected.getMessage());
            }
        } finally {
            blocked.countDown();
            saturated.shutdownNow();
        }

        // ---- the caller runs policy instead ----------------------------------
        CountDownLatch blockedAgain = new CountDownLatch(1);
        Pool callerRuns = new Pool(1, 1, RejectionPolicy.CALLER_RUNS);
        AtomicInteger ranOnCaller = new AtomicInteger();
        String callerName = Thread.currentThread().getName();
        try {
            callerRuns.submit(() -> {
                try {
                    blockedAgain.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            });
            callerRuns.submit(() -> {
            });
            Thread.sleep(100);
            callerRuns.submit(() -> {
                if (Thread.currentThread().getName().equals(callerName)) {
                    ranOnCaller.incrementAndGet();
                }
            });
            check(ranOnCaller.get() == 1, "the overflowing task ran on the caller's thread");
            check(callerRuns.rejected() == 0, "so nothing was refused");
            System.out.println("caller runs  : the task ran on " + callerName + " instead of being refused");
        } finally {
            blockedAgain.countDown();
            callerRuns.shutdownNow();
        }

        // ---- a failing task does not take the worker with it -----------------
        Pool tolerant = new Pool(2, 10, RejectionPolicy.ABORT);
        try {
            tolerant.submit(() -> {
                throw new IllegalStateException("this task is broken");
            });
            tolerant.submit(() -> {
            });
            tolerant.shutdown(5_000);
            check(tolerant.failed() == 1, "the failure was counted");
            check(tolerant.completed() == 1, "and the good task still ran");
        } finally {
            tolerant.shutdownNow();
        }
        System.out.println("failures     : a throwing task was counted and the pool carried on");

        // ---- shutdown drains, shutdownNow does not ---------------------------
        AtomicInteger finished = new AtomicInteger();
        Pool drainer = new Pool(2, 50, RejectionPolicy.ABORT);
        for (int i = 0; i < 50; i++) {
            drainer.submit(() -> {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
                finished.incrementAndGet();
            });
        }
        check(drainer.shutdown(10_000), "a graceful shutdown waited for the queue to drain");
        check(finished.get() == 50, "so all fifty tasks finished: " + finished.get());
        check(!drainer.isAccepting(), "and the pool is closed to new work");
        System.out.println("graceful     : " + finished.get() + " queued tasks completed before stopping");

        Pool impatient = new Pool(1, 20, RejectionPolicy.ABORT);
        AtomicInteger barelyStarted = new AtomicInteger();
        for (int i = 0; i < 20; i++) {
            impatient.submit(() -> {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
                barelyStarted.incrementAndGet();
            });
        }
        Thread.sleep(50);
        int abandoned = impatient.shutdownNow();
        check(abandoned > 0, "shutdownNow reported what it threw away: " + abandoned);
        check(barelyStarted.get() < 20, "and most tasks never finished");
        check(impatient.completed() < 20, "which the metrics agree with");
        System.out.println("immediate    : abandoned " + abandoned + " queued tasks, "
                + impatient.completed() + " had completed");

        // ---- submitting after shutdown ---------------------------------------
        Pool closed = new Pool(1, 1, RejectionPolicy.ABORT);
        closed.shutdown(1_000);
        try {
            closed.submit(() -> {
            });
            throw new AssertionError("a closed pool should refuse work");
        } catch (RejectedExecutionException expected) {
            System.out.println("closed       : " + expected.getMessage());
        }

        // ---- bad construction ------------------------------------------------
        for (int[] bad : new int[][] {{0, 1}, {1, 0}, {-1, 5}}) {
            try {
                new Pool(bad[0], bad[1], RejectionPolicy.ABORT);
                throw new AssertionError("should have been rejected: " + bad[0] + ", " + bad[1]);
            } catch (IllegalArgumentException expected) {
                // expected
            }
        }
        System.out.println("bad sizes    : zero and negative sizes are rejected");
        System.out.println("All checks passed.");
    }
}
