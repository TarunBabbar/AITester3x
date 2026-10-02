import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 90 - Weak and soft references: WeakHashMap entries that vanish when nobody
 * else holds the key, ReferenceQueue notifications, and the classic leak where a
 * value keeps its own key alive.
 *
 * Collection is at the collector's discretion, so anything that expects an object
 * to disappear polls and nudges the collector with a deadline. Anything that
 * expects an object to SURVIVE is safe to assert directly.
 *
 * Compile and run:
 *   javac WeakReferences.java
 *   java WeakReferences
 */
public class WeakReferences {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /** Nudge the collector until the map reports the size we are waiting for. */
    static boolean awaitSize(Map<?, ?> map, int expected, long timeoutMillis)
            throws InterruptedException {
        long deadline = System.nanoTime() + timeoutMillis * 1_000_000L;
        while (System.nanoTime() < deadline) {
            System.gc();
            if (map.size() == expected) {     // size() expunges cleared entries
                return true;
            }
            Thread.sleep(25);
        }
        return false;
    }

    /** Nudge the collector until the reference clears and reaches the queue. */
    static boolean awaitEnqueued(ReferenceQueue<?> queue, long timeoutMillis)
            throws InterruptedException {
        long deadline = System.nanoTime() + timeoutMillis * 1_000_000L;
        while (System.nanoTime() < deadline) {
            System.gc();
            if (queue.poll() != null) {
                return true;
            }
            Thread.sleep(25);
        }
        return false;
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> cache = new WeakHashMap<>();

        // ---- a strongly reachable key is never dropped ------------------------
        String held = new String("held");            // not an interned literal
        cache.put(held, "value");
        System.gc();
        check(cache.size() == 1, "a key that is still reachable keeps its entry");
        check(cache.get(held).equals("value"), "and the value is still readable");
        System.out.println("strong key   : the entry survived a collection");

        // ---- a key that becomes unreachable is dropped ------------------------
        String temporary = new String("temporary");
        cache.put(temporary, "value");
        check(cache.size() == 2, "two entries while both keys are reachable");
        temporary = null;                            // nothing strong refers to it now
        check(awaitSize(cache, 1, 5_000),
                "once the key is unreachable the entry disappears by itself");
        check(held != null && cache.containsKey(held), "the other key was never touched");

        // ---- a hundred keys, released in one go -------------------------------
        Map<String, Integer> many = new WeakHashMap<>();
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            String key = new String("key" + i);
            keys.add(key);
            many.put(key, i);
        }
        check(many.size() == 100, "a hundred entries while the keys are held");
        keys.clear();
        check(awaitSize(many, 0, 5_000), "dropping the only strong references empties the cache");
        System.out.println("released     : a hundred entries cleared themselves");

        // ---- ReferenceQueue tells you what was collected ---------------------
        ReferenceQueue<Object> queue = new ReferenceQueue<>();
        Object payload = new Object();
        WeakReference<Object> reference = new WeakReference<>(payload, queue);
        check(reference.get() == payload, "a weak reference still reaches its object");
        check(reference.refersTo(payload), "refersTo agrees");
        payload = null;
        check(awaitEnqueued(queue, 5_000), "the collector reported the reference on the queue");
        check(reference.get() == null, "and the reference no longer reaches anything");
        check(reference.refersTo(null), "so refersTo(null) is true");
        System.out.println("queue        : the reference arrived after collection");

        // ---- a soft reference holds on while memory is free -------------------
        SoftReference<byte[]> soft = new SoftReference<>(new byte[4096]);
        check(soft.get() != null, "a fresh soft reference holds its value");
        System.gc();
        // Deliberately not asserting it was cleared: a soft reference is only
        // released under memory pressure, so the collector may well keep it.
        System.out.println("soft ref     : still present after a collection = " + (soft.get() != null));

        // ---- the leak: a value that strongly refers to its own key -----------
        Map<Object, Object> leaky = new WeakHashMap<>();
        Object leakingKey = new Object();
        leaky.put(leakingKey, leakingKey);       // the value keeps the key alive
        leakingKey = null;
        System.gc();
        Thread.sleep(200);
        check(leaky.size() == 1,
                "the entry survives even after a collection: the map itself holds the only reference");
        check(leaky.keySet().iterator().next() != null, "and the key is still there");
        System.out.println("self leak    : a value pointing at its key defeats the weak key");

        // ---- the same shape, done correctly ----------------------------------
        Map<Object, String> fine = new WeakHashMap<>();
        Object cleanKey = new Object();
        fine.put(cleanKey, cleanKey.toString());  // the value is a copy, not the key
        cleanKey = null;
        check(awaitSize(fine, 0, 5_000), "a value that does not refer back releases normally");
        System.out.println("done right   : copying the value out lets the key go");

        // ---- WeakHashMap is not synchronized --------------------------------
        Map<String, Integer> notSafe = new WeakHashMap<>();
        notSafe.put("a", 1);
        notSafe.put("b", 2);
        check(notSafe.size() == 2, "plain Map semantics while nothing is collected");
        check(notSafe.getOrDefault("missing", -1) == -1, "and the usual defaults work");
        System.out.println("All checks passed.");
    }
}
