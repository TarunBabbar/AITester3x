import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 42 - Microbenchmarking: warm-up, System.nanoTime, and why a benchmark has to
 * consume its result. The printed timings are informational; the assertions
 * only check that the work was correct.
 *
 * Compile and run:
 *   javac Microbenchmarking.java
 *   java Microbenchmarking
 */
public class Microbenchmarking {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /**
     * Volatile sink. If the result were discarded the JIT could delete the whole
     * computation and the measurement would be meaningless.
     */
    static volatile Object blackhole;

    static String concatWithPlus(int count) {
        String text = "";
        for (int i = 0; i < count; i++) {
            text += i % 10;
        }
        return text;
    }

    static String concatWithBuilder(int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(i % 10);
        }
        return builder.toString();
    }

    static List<Integer> fillDefault(int count) {
        List<Integer> list = new ArrayList<>();          // grows and copies repeatedly
        for (int i = 0; i < count; i++) {
            list.add(i);
        }
        return list;
    }

    static List<Integer> fillPresized(int count) {
        List<Integer> list = new ArrayList<>(count);     // one allocation up front
        for (int i = 0; i < count; i++) {
            list.add(i);
        }
        return list;
    }

    /** Time one call, keeping the result so it cannot be optimised away. */
    static long nanos(Supplier<Object> work) {
        long start = System.nanoTime();
        blackhole = work.get();
        return System.nanoTime() - start;
    }

    /** Warm up first, then report the fastest of several runs. */
    static long bestOf(int warmups, int runs, Supplier<Object> work) {
        for (int i = 0; i < warmups; i++) {
            blackhole = work.get();
        }
        long best = Long.MAX_VALUE;
        for (int i = 0; i < runs; i++) {
            best = Math.min(best, nanos(work));
        }
        return best;
    }

    static String millis(long nanos) {
        return String.format("%.3f ms", nanos / 1_000_000.0);
    }

    public static void main(String[] args) {
        // ---- the clock is not infinitely fine ---------------------------------
        long empty = nanos(() -> null);
        check(empty >= 0, "nanoTime never goes backwards");
        System.out.println("timing an empty block : " + empty + " ns (nanoTime resolution, not zero work)");

        // ---- string building ---------------------------------------------------
        int size = 20_000;
        String withPlus = concatWithPlus(size);
        String withBuilder = concatWithBuilder(size);
        check(withPlus.length() == size, "the + operator produced the right length");
        check(withPlus.equals(withBuilder), "both approaches build the same text");

        long plusNanos = bestOf(3, 5, () -> concatWithPlus(size));
        long builderNanos = bestOf(3, 5, () -> concatWithBuilder(size));
        System.out.printf("concat %6d chars : + = %s, StringBuilder = %s%n",
                size, millis(plusNanos), millis(builderNanos));

        // ---- collection growth -------------------------------------------------
        int elements = 1_000_000;
        List<Integer> grown = fillDefault(elements);
        List<Integer> presized = fillPresized(elements);
        check(grown.size() == elements && presized.size() == elements, "both lists are full");
        check(grown.equals(presized), "both lists hold the same values");

        long grownNanos = bestOf(1, 3, () -> fillDefault(elements));
        long presizedNanos = bestOf(1, 3, () -> fillPresized(elements));
        System.out.printf("fill %7d ints  : default = %s, presized = %s%n",
                elements, millis(grownNanos), millis(presizedNanos));

        // ---- the same measurement, done badly ---------------------------------
        // No warm-up, a single run, and the result thrown away: whatever this
        // prints should not be trusted.
        long coldNanos = nanos(() -> {
            concatWithBuilder(size);
            return null;
        });
        long warmNanos = bestOf(5, 10, () -> concatWithBuilder(size));
        System.out.printf("cold vs warm (builder) : cold=%s warm=%s%n",
                millis(coldNanos), millis(warmNanos));

        System.out.println("""

                Note: timings vary by machine, JVM and load. Treat them as a way to spot
                an order-of-magnitude difference, not as precise numbers.""");
        System.out.println("All correctness checks passed.");
    }
}
