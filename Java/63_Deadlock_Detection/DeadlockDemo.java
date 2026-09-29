import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 63 - Deadlock: reproduce it with two locks taken in opposite orders, find it
 * with ThreadMXBean, then fix it by agreeing on a lock order (or by using tryLock).
 *
 * Compile and run:
 *   javac DeadlockDemo.java
 *   java DeadlockDemo
 */
public class DeadlockDemo {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /**
     * Two daemon threads each grab one lock, then reach for the other. Neither
     * ever gets it, so they stay deadlocked - safely, because they are daemons
     * and the JVM will exit without them.
     */
    static Thread[] startDeadlockedPair(ReentrantLock first, ReentrantLock second) {
        CountDownLatch bothHoldOne = new CountDownLatch(2);

        Thread left = new Thread(() -> {
            first.lock();
            try {
                bothHoldOne.countDown();
                bothHoldOne.await(5, TimeUnit.SECONDS);
                second.lock();                 // blocks forever
                try {
                    // never reached
                } finally {
                    second.unlock();
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } finally {
                first.unlock();
            }
        }, "left");

        Thread right = new Thread(() -> {
            second.lock();
            try {
                bothHoldOne.countDown();
                bothHoldOne.await(5, TimeUnit.SECONDS);
                first.lock();                  // blocks forever
                try {
                    // never reached
                } finally {
                    first.unlock();
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } finally {
                second.unlock();
            }
        }, "right");

        left.setDaemon(true);
        right.setDaemon(true);
        left.start();
        right.start();
        return new Thread[] {left, right};
    }

    /** Both threads take the locks in the SAME order, so no cycle is possible. */
    static void orderedWork(ReentrantLock a, ReentrantLock b, AtomicInteger completed, int iterations) {
        // Compare identity so both threads agree on which lock is "first".
        ReentrantLock lower = System.identityHashCode(a) <= System.identityHashCode(b) ? a : b;
        ReentrantLock higher = lower == a ? b : a;
        for (int i = 0; i < iterations; i++) {
            lower.lock();
            try {
                higher.lock();
                try {
                    completed.incrementAndGet();
                } finally {
                    higher.unlock();
                }
            } finally {
                lower.unlock();
            }
        }
    }

    /** The alternative: never wait indefinitely - try, and back off. */
    static void tryLockWork(ReentrantLock first, ReentrantLock second, AtomicInteger completed, int iterations) {
        for (int i = 0; i < iterations; i++) {
            boolean haveFirst = false;
            boolean haveSecond = false;
            try {
                haveFirst = first.tryLock(20, TimeUnit.MILLISECONDS);
                if (!haveFirst) {
                    continue;
                }
                haveSecond = second.tryLock(20, TimeUnit.MILLISECONDS);
                if (haveSecond) {
                    completed.incrementAndGet();
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            } finally {
                if (haveSecond) {
                    second.unlock();
                }
                if (haveFirst) {
                    first.unlock();
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        // ---- reproduce the deadlock -----------------------------------------
        ReentrantLock first = new ReentrantLock();
        ReentrantLock second = new ReentrantLock();
        Thread[] pair = startDeadlockedPair(first, second);

        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        long[] deadlocked = new long[0];
        for (int attempt = 0; attempt < 50 && deadlocked.length == 0; attempt++) {
            Thread.sleep(100);
            deadlocked = threads.findDeadlockedThreads();
        }

        check(deadlocked != null && deadlocked.length >= 2, "the JVM detected the deadlock");
        String[] names = new String[deadlocked.length];
        for (int i = 0; i < deadlocked.length; i++) {
            ThreadInfo info = threads.getThreadInfo(deadlocked[i]);
            names[i] = info.getThreadName();
        }
        check(Arrays.asList(names).contains("left") && Arrays.asList(names).contains("right"),
                "both threads are named in the report");
        check(pair[0].isAlive() && pair[1].isAlive(), "and both are still stuck");
        System.out.println("deadlock    : " + Arrays.toString(names) + " are blocked on each other");
        System.out.println("             findDeadlockedThreads() reported " + deadlocked.length + " threads");
        // These two daemons stay stuck for the rest of the run; the JVM still
        // exits once main finishes, which is exactly why daemon threads are handy.

        // ---- fix one: a consistent lock order -------------------------------
        ReentrantLock a = new ReentrantLock();
        ReentrantLock b = new ReentrantLock();
        int iterations = 2_000;
        AtomicInteger completed = new AtomicInteger();

        Thread one = new Thread(() -> orderedWork(a, b, completed, iterations), "one");
        Thread two = new Thread(() -> orderedWork(a, b, completed, iterations), "two");
        one.start();
        two.start();
        one.join(15_000);
        two.join(15_000);

        check(!one.isAlive() && !two.isAlive(), "both threads finished instead of deadlocking");
        check(completed.get() == 2 * iterations, "every critical section ran: " + completed.get());
        System.out.println("lock order  : " + completed.get() + " critical sections, no deadlock");

        // ---- fix two: tryLock with a timeout, then give up and retry --------
        ReentrantLock x = new ReentrantLock();
        ReentrantLock y = new ReentrantLock();
        AtomicInteger acquisitions = new AtomicInteger();
        int attempts = 500;

        // Note the opposite orders - this would deadlock with plain lock().
        Thread p = new Thread(() -> tryLockWork(x, y, acquisitions, attempts), "p");
        Thread q = new Thread(() -> tryLockWork(y, x, acquisitions, attempts), "q");
        p.start();
        q.start();
        p.join(15_000);
        q.join(15_000);

        check(!p.isAlive() && !q.isAlive(), "tryLock let both threads give up rather than hang");
        check(acquisitions.get() > 0, "and work still got done: " + acquisitions.get());
        System.out.println("tryLock     : " + acquisitions.get()
                + " acquisitions out of " + (2 * attempts) + " attempts, no deadlock");
        System.out.println("All checks passed.");
    }
}
