import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 104 - A lock-free queue: the Michael-Scott algorithm, where offer and poll are
 * compare-and-set loops rather than a lock. A dummy node at the head keeps the
 * two ends from tripping over each other, and a lagging tail is repaired by
 * whichever thread notices.
 *
 * Compile and run:
 *   javac LockFreeQueue.java
 *   java LockFreeQueue
 */
public class LockFreeQueue {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static final class Node<T> {
        final T value;                                 // null only for the dummy
        final AtomicReference<Node<T>> next = new AtomicReference<>();

        Node(T value) {
            this.value = value;
        }
    }

    static final class Queue<T> {
        private final AtomicReference<Node<T>> head;
        private final AtomicReference<Node<T>> tail;
        private final AtomicInteger retries = new AtomicInteger();

        Queue() {
            Node<T> dummy = new Node<>(null);
            head = new AtomicReference<>(dummy);
            tail = new AtomicReference<>(dummy);
        }

        void offer(T value) {
            Node<T> node = new Node<>(value);
            while (true) {
                Node<T> last = tail.get();
                Node<T> next = last.next.get();
                if (last != tail.get()) {
                    retries.incrementAndGet();         // the tail moved under us
                    continue;
                }
                if (next == null) {
                    if (last.next.compareAndSet(null, node)) {
                        // Best effort: if this fails another thread got there first,
                        // and it will fix the tail for us.
                        tail.compareAndSet(last, node);
                        return;
                    }
                } else {
                    tail.compareAndSet(last, next);     // help the lagging tail along
                }
                retries.incrementAndGet();
            }
        }

        T poll() {
            while (true) {
                Node<T> first = head.get();
                Node<T> last = tail.get();
                Node<T> next = first.next.get();
                if (first != head.get()) {
                    retries.incrementAndGet();
                    continue;
                }
                if (first == last) {
                    if (next == null) {
                        return null;                    // genuinely empty
                    }
                    tail.compareAndSet(last, next);      // the tail is behind again
                } else {
                    T value = next.value;
                    if (head.compareAndSet(first, next)) {
                        return value;
                    }
                }
                retries.incrementAndGet();
            }
        }

        boolean isEmpty() {
            Node<T> first = head.get();
            return first == tail.get() && first.next.get() == null;
        }

        int retries() {
            return retries.get();
        }
    }

    public static void main(String[] args) throws Exception {
        // ---- single threaded, in order ---------------------------------------
        Queue<Integer> queue = new Queue<>();
        check(queue.isEmpty(), "a fresh queue is empty");
        check(queue.poll() == null, "and poll gives null rather than throwing");

        for (int value = 1; value <= 5; value++) {
            queue.offer(value);
        }
        check(!queue.isEmpty(), "it is not empty once something is offered");
        List<Integer> drained = new ArrayList<>();
        for (Integer value = queue.poll(); value != null; value = queue.poll()) {
            drained.add(value);
        }
        check(drained.equals(List.of(1, 2, 3, 4, 5)), "first in, first out");
        check(queue.poll() == null, "and it is empty again");
        System.out.println("single thread: " + drained);

        // ---- interleaving offers and polls -----------------------------------
        Queue<String> interleaved = new Queue<>();
        interleaved.offer("a");
        interleaved.offer("b");
        check(interleaved.poll().equals("a"), "the first out is the first in");
        interleaved.offer("c");
        check(interleaved.poll().equals("b"), "the queue keeps its place");
        check(interleaved.poll().equals("c"), "and the late arrival is last");
        check(interleaved.poll() == null, "then it is empty");
        System.out.println("interleaved  : a, b, c in order");

        // ---- many producers and consumers at once ----------------------------
        int producers = 4;
        int consumers = 4;
        int perProducer = 2_500;
        int total = producers * perProducer;

        Queue<Integer> shared = new Queue<>();
        Set<Integer> received = ConcurrentHashMap.newKeySet();
        AtomicInteger duplicates = new AtomicInteger();
        AtomicInteger missing = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(producers + consumers);
        CountDownLatch startTogether = new CountDownLatch(1);

        List<Future<?>> futures = new ArrayList<>();
        for (int producer = 0; producer < producers; producer++) {
            int base = producer * perProducer;
            futures.add(pool.submit(() -> {
                await(startTogether);
                for (int i = 0; i < perProducer; i++) {
                    shared.offer(base + i);
                }
            }));
        }

        CountDownLatch consumersDone = new CountDownLatch(consumers);
        for (int consumer = 0; consumer < consumers; consumer++) {
            futures.add(pool.submit(() -> {
                await(startTogether);
                try {
                    int idleSpins = 0;
                    while (idleSpins < 200) {
                        Integer value = shared.poll();
                        if (value == null) {
                            idleSpins++;
                            Thread.onSpinWait();
                        } else {
                            idleSpins = 0;
                            if (!received.add(value)) {
                                duplicates.incrementAndGet();
                            }
                        }
                    }
                } finally {
                    consumersDone.countDown();
                }
            }));
        }

        startTogether.countDown();
        try {
            check(consumersDone.await(30, TimeUnit.SECONDS), "the consumers finished");
        } finally {
            pool.shutdownNow();
        }

        for (int value = 0; value < total; value++) {
            if (!received.contains(value)) {
                missing.incrementAndGet();
            }
        }
        check(duplicates.get() == 0, "no value was handed out twice: " + duplicates.get());
        check(missing.get() == 0, "and none was lost: " + missing.get() + " missing");
        check(received.size() == total, "all " + total + " values came through: " + received.size());
        System.out.println("concurrent   : " + received.size() + " of " + total
                + " values, no duplicates, no losses");
        System.out.println("contention   : " + shared.retries() + " compare-and-set retries");

        // ---- the queue survives a hammering of mixed operations --------------
        Queue<Integer> mixed = new Queue<>();
        AtomicInteger offered = new AtomicInteger();
        AtomicInteger polled = new AtomicInteger();
        Set<Integer> seen = ConcurrentHashMap.newKeySet();
        AtomicInteger duplicatesAgain = new AtomicInteger();

        ExecutorService mixedPool = Executors.newFixedThreadPool(6);
        CountDownLatch mixedDone = new CountDownLatch(6);
        for (int worker = 0; worker < 3; worker++) {
            int base = worker * 1_000;
            mixedPool.submit(() -> {
                try {
                    for (int i = 0; i < 1_000; i++) {
                        mixed.offer(base + i);
                        offered.incrementAndGet();
                    }
                } finally {
                    mixedDone.countDown();
                }
            });
        }
        for (int worker = 0; worker < 3; worker++) {
            mixedPool.submit(() -> {
                try {
                    int empty = 0;
                    while (empty < 500) {
                        Integer value = mixed.poll();
                        if (value == null) {
                            empty++;
                            Thread.onSpinWait();
                        } else {
                            empty = 0;
                            polled.incrementAndGet();
                            if (!seen.add(value)) {
                                duplicatesAgain.incrementAndGet();
                            }
                        }
                    }
                } finally {
                    mixedDone.countDown();
                }
            });
        }
        check(mixedDone.await(30, TimeUnit.SECONDS), "the mixed workload finished");
        mixedPool.shutdownNow();
        check(duplicatesAgain.get() == 0, "still no duplicates under a mixed load");
        check(polled.get() <= offered.get(), "never more out than in: "
                + polled.get() + " of " + offered.get());
        System.out.println("mixed load   : " + polled.get() + " polled of "
                + offered.get() + " offered, " + duplicatesAgain.get() + " duplicates");

        // ---- empty checks under contention -----------------------------------
        Queue<String> pristine = new Queue<>();
        AtomicInteger sawNonEmpty = new AtomicInteger();
        ExecutorService emptyPool = Executors.newFixedThreadPool(4);
        CountDownLatch emptyDone = new CountDownLatch(4);
        for (int worker = 0; worker < 4; worker++) {
            emptyPool.submit(() -> {
                try {
                    for (int i = 0; i < 2_000; i++) {
                        if (!pristine.isEmpty()) {
                            sawNonEmpty.incrementAndGet();
                        }
                    }
                } finally {
                    emptyDone.countDown();
                }
            });
        }
        check(emptyDone.await(20, TimeUnit.SECONDS), "the emptiness checks finished");
        emptyPool.shutdownNow();
        check(sawNonEmpty.get() == 0, "an untouched queue never claims to hold anything");
        System.out.println("empty queue  : " + sawNonEmpty.get() + " false non-empty readings");

        // ---- a single consumer drains everything one producer added ----------
        Queue<Integer> pipe = new Queue<>();
        for (int i = 0; i < 1_000; i++) {
            pipe.offer(i);
        }
        int count = 0;
        int expected = 0;
        for (Integer value = pipe.poll(); value != null; value = pipe.poll()) {
            check(value == expected++, "values come out in order after the concurrent phase");
            count++;
        }
        check(count == 1_000, "all thousand come back out");
        System.out.println("drain        : " + count + " values in strict order");
        System.out.println("All checks passed.");
    }

    /** Wait for the starting gun, treating an interrupt as a reason to give up. */
    static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
