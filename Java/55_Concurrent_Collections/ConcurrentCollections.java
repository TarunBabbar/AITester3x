import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 55 - Concurrent collections: atomic compound updates on ConcurrentHashMap,
 * LongAdder versus AtomicLong under contention, and CopyOnWriteArrayList.
 *
 * Compile and run:
 *   javac ConcurrentCollections.java
 *   java ConcurrentCollections
 */
public class ConcurrentCollections {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static final int THREADS = 8;
    static final int TOTAL = 200_000;

    public static void main(String[] args) throws Exception {
        // ---- merge is one atomic step, so nothing is lost --------------------
        ConcurrentHashMap<String, Long> counts = new ConcurrentHashMap<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(THREADS)) {
            CountDownLatch finished = new CountDownLatch(THREADS);
            for (int thread = 0; thread < THREADS; thread++) {
                pool.submit(() -> {
                    try {
                        for (int i = 0; i < TOTAL / THREADS; i++) {
                            counts.merge("bucket" + (i % 5), 1L, Long::sum);
                        }
                    } finally {
                        finished.countDown();
                    }
                });
            }
            check(finished.await(20, TimeUnit.SECONDS), "every worker finished");
        }
        long recorded = counts.values().stream().mapToLong(Long::longValue).sum();
        check(counts.size() == 5, "five buckets");
        check(recorded == TOTAL, "merge lost no updates: " + recorded + " of " + TOTAL);
        System.out.println("merge       : " + recorded + " updates across " + counts.size() + " keys");
        // Note: byLength.computeIfAbsent(key, k -> new ArrayList<>()).add(v) would
        // LOSE updates - the map step is atomic but a plain ArrayList is not.

        // ---- putIfAbsent and replace are single steps too --------------------
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
        check(map.putIfAbsent("a", 1) == null, "the first putIfAbsent wins");
        check(map.putIfAbsent("a", 99) == 1, "the second returns the existing value");
        check(map.get("a") == 1, "and does not overwrite it");
        check(map.replace("a", 1, 2), "compare-and-set succeeds when the value matches");
        check(!map.replace("a", 1, 3), "and fails when it does not");
        check(map.get("a") == 2, "the value is 2");
        try {
            map.put("null-value", null);
            throw new AssertionError("ConcurrentHashMap rejects null values");
        } catch (NullPointerException expected) {
            System.out.println("null value  : rejected with NullPointerException");
        }

        // ---- a concurrent set built from the same map -----------------------
        Set<String> tags = ConcurrentHashMap.newKeySet();
        try (ExecutorService pool = Executors.newFixedThreadPool(4)) {
            for (int thread = 0; thread < 4; thread++) {
                pool.submit(() -> {
                    for (int i = 0; i < 500; i++) {
                        tags.add("tag" + (i % 250));     // duplicates on purpose
                    }
                });
            }
        }
        check(tags.size() == 250, "duplicates collapsed: " + tags.size());

        // ---- AtomicLong versus LongAdder under contention --------------------
        AtomicLong atomic = new AtomicLong();
        LongAdder adder = new LongAdder();

        long atomicMillis = timeContended(() -> {
            for (int i = 0; i < TOTAL / THREADS; i++) {
                atomic.incrementAndGet();
            }
        });
        long adderMillis = timeContended(() -> {
            for (int i = 0; i < TOTAL / THREADS; i++) {
                adder.increment();
            }
        });

        check(atomic.get() == TOTAL, "AtomicLong counted every increment");
        check(adder.sum() == TOTAL, "LongAdder counted every increment");
        System.out.printf("contention  : AtomicLong %d ms, LongAdder %d ms (%d increments on %d threads)%n",
                atomicMillis, adderMillis, TOTAL, THREADS);
        // The timing gap is informational; LongAdder wins by spreading writes over
        // cells, which is why it is preferred for pure counters.

        // ---- CopyOnWriteArrayList: iterate while someone else writes ---------
        CopyOnWriteArrayList<String> events = new CopyOnWriteArrayList<>();
        events.add("start");
        for (String event : events) {              // iterates a snapshot
            if (event.equals("start")) {
                events.add("added-during-iteration");
            }
        }
        check(events.size() == 2, "the write succeeded");
        check(!events.get(0).equals("added-during-iteration"),
                "but this pass still saw only the original snapshot");

        Iterator<String> iterator = events.iterator();
        iterator.next();
        try {
            iterator.remove();
            throw new AssertionError("CopyOnWriteArrayList iterators refuse remove");
        } catch (UnsupportedOperationException expected) {
            System.out.println("COW iterator: remove() is unsupported");
        }
        events.addIfAbsent("start");               // already there
        check(events.size() == 2, "addIfAbsent did nothing");
        check(!events.remove("missing") && events.remove("start"), "remove reports whether it changed anything");
        System.out.println("cow list    : " + events);
        System.out.println("All checks passed.");
    }

    /** Run the body on THREADS threads and return how long the batch took. */
    static long timeContended(Runnable body) throws Exception {
        long start = System.nanoTime();
        try (ExecutorService pool = Executors.newFixedThreadPool(THREADS)) {
            List<Future<?>> futures = new ArrayList<>();
            for (int thread = 0; thread < THREADS; thread++) {
                futures.add(pool.submit(body));
            }
            for (Future<?> future : futures) {
                future.get();
            }
        }
        return (System.nanoTime() - start) / 1_000_000;
    }
}
