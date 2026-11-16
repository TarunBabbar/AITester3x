import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;

/**
 * 122 - Reservoir sampling.
 *
 * Algorithm R keeps a uniform sample of k items from a stream of unknown
 * length: the first k items fill the reservoir and every later item replaces a
 * random slot with probability k / n. The random source is injected so runs are
 * reproducible, and the sample is handed out as a copy.
 *
 * Compile and run:
 *   javac ReservoirSampling.java
 *   java ReservoirSampling
 */
public class ReservoirSampling {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static final class Reservoir<T> {
        private final int capacity;
        private final DoubleSupplier random;
        private final List<T> items = new ArrayList<>();
        private long seen;

        Reservoir(int capacity) {
            this(capacity, Math::random);
        }

        Reservoir(int capacity, DoubleSupplier random) {
            if (capacity < 0) {
                throw new IllegalArgumentException("capacity must not be negative");
            }
            if (random == null) {
                throw new IllegalArgumentException("random source must not be null");
            }
            this.capacity = capacity;
            this.random = random;
        }

        int capacity() {
            return capacity;
        }

        int size() {
            return items.size();
        }

        long seenCount() {
            return seen;
        }

        boolean isEmpty() {
            return items.isEmpty();
        }

        boolean isFull() {
            return items.size() == capacity;
        }

        void add(T item) {
            seen++;
            if (items.size() < capacity) {
                items.add(item);
                return;
            }
            if (capacity == 0) {
                return;
            }
            int slot = draw(seen);
            if (slot < capacity) {
                items.set(slot, item);
            }
        }

        void addAll(Iterable<? extends T> values) {
            for (T value : values) {
                add(value);
            }
        }

        List<T> sample() {
            return new ArrayList<>(items);
        }

        private int draw(long limit) {
            double value = random.getAsDouble();
            if (!(value >= 0.0 && value < 1.0)) {
                throw new IllegalArgumentException("random source returned " + value);
            }
            return (int) Math.floor(value * limit);
        }
    }

    static final class Seeded implements DoubleSupplier {
        private long state;

        Seeded(long seed) {
            this.state = seed;
        }

        @Override
        public double getAsDouble() {
            state = (state * 1103515245L + 12345L) % 2147483648L;
            return state / 2147483648.0;
        }
    }

    static String join(List<Integer> values) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                builder.append(", ");
            }
            builder.append(values.get(index));
        }
        return builder.toString();
    }

    public static void main(String[] args) {
        // ---- empty and zero capacity ----------------------------------------
        Reservoir<String> empty = new Reservoir<>(3, new Seeded(2));
        check(empty.size() == 0 && empty.seenCount() == 0, "starts empty");
        check(empty.sample().isEmpty() && empty.isEmpty(), "no sample yet");
        check(!empty.isFull() && empty.capacity() == 3, "three slots are waiting");

        Reservoir<Integer> zero = new Reservoir<>(0, new Seeded(1));
        zero.addAll(List.of(1, 2, 3, 4, 5));
        check(zero.size() == 0 && zero.sample().isEmpty(), "keeps nothing");
        check(zero.seenCount() == 5, "still counts the stream");
        check(zero.isFull(), "a zero-capacity reservoir is full");
        System.out.println("zero capacity: " + zero.size() + " items after " + zero.seenCount() + " additions");

        // ---- a reservoir larger than the stream -----------------------------
        List<Integer> stream = List.of(0, 1, 2, 3, 4, 5, 6, 7);
        Reservoir<Integer> all = new Reservoir<>(10, new Seeded(3));
        all.addAll(stream);
        check(all.sample().equals(stream), "keeps the whole stream in order");
        check(all.size() == stream.size() && all.seenCount() == stream.size(), "counts match");
        check(!all.isFull(), "a larger reservoir has room left");

        Reservoir<Integer> exact = new Reservoir<>(stream.size(), new Seeded(4));
        exact.addAll(stream);
        check(exact.sample().equals(stream), "an exactly-sized reservoir keeps everything");
        check(exact.isFull(), "an exactly-sized reservoir is full");
        System.out.println("sample       : " + join(all.sample()));

        // ---- the sample is a copy -------------------------------------------
        Reservoir<Integer> copied = new Reservoir<>(3, new Seeded(5));
        copied.addAll(List.of(1, 2, 3));
        List<Integer> returned = copied.sample();
        returned.add(99);
        returned.set(0, 42);
        check(copied.size() == 3 && copied.sample().equals(List.of(1, 2, 3)),
                "mutating the sample leaves the reservoir alone");
        System.out.println("copy         : internal sample " + join(copied.sample()));

        // ---- replacements keep the size -------------------------------------
        Reservoir<Integer> fixed = new Reservoir<>(3, new Seeded(6));
        for (int item = 0; item < 1_000; item++) {
            fixed.add(item);
            check(fixed.size() <= 3, "never grows past capacity");
            check(fixed.size() == Math.min(item + 1, 3), "fills up and then stays full");
        }
        check(fixed.seenCount() == 1_000 && fixed.isFull(), "all items counted");
        check(new java.util.HashSet<>(fixed.sample()).size() == 3, "no duplicates from distinct items");
        check(fixed.sample().stream().allMatch(item -> item >= 0 && item < 1_000), "values come from the stream");
        System.out.println("after 1,000  : " + join(fixed.sample()));

        // ---- determinism ----------------------------------------------------
        Reservoir<Integer> first = new Reservoir<>(4, new Seeded(2026));
        Reservoir<Integer> second = new Reservoir<>(4, new Seeded(2026));
        check(first.sample().equals(second.sample()), "two empty reservoirs agree");
        for (int item = 0; item < 100; item++) {
            first.add(item);
            second.add(item);
        }
        check(first.sample().equals(second.sample()), "same seed, same sample");
        check(first.seenCount() == second.seenCount(), "same seed, same count");
        System.out.println("seeded       : " + join(first.sample()));

        // ---- inclusion frequencies -----------------------------------------
        int trials = 5_000;
        int universe = 10;
        int[] counts = new int[universe];
        Seeded random = new Seeded(87);

        for (int trial = 0; trial < trials; trial++) {
            Reservoir<Integer> sampler = new Reservoir<>(3, random);
            for (int item = 0; item < universe; item++) {
                sampler.add(item);
            }
            for (int item : sampler.sample()) {
                counts[item]++;
            }
        }

        double expected = (double) (trials * 3) / universe;
        int total = 0;
        StringBuilder observed = new StringBuilder();
        for (int item = 0; item < universe; item++) {
            check(counts[item] > expected * 0.85 && counts[item] < expected * 1.15,
                    "item " + item + " was included " + counts[item] + " times, expected about " + expected);
            total += counts[item];
            if (item > 0) {
                observed.append(", ");
            }
            observed.append(counts[item]);
        }
        check(total == trials * 3, "each trial returns exactly three items");
        System.out.println("frequencies  : 5,000 trials, expected " + expected + " each, observed " + observed);

        // ---- small streams ---------------------------------------------------
        Reservoir<String> single = new Reservoir<>(1, new Seeded(9));
        single.add("only");
        check(single.sample().equals(List.of("only")) && single.isFull(), "one item fills a one-slot reservoir");

        Reservoir<Integer> singleton = new Reservoir<>(1, new Seeded(10));
        for (int item = 0; item < 100; item++) {
            singleton.add(item);
        }
        check(singleton.size() == 1 && singleton.seenCount() == 100, "a one-slot reservoir keeps one item");
        System.out.println("one slot     : " + join(singleton.sample()) + " after 100 items");

        // ---- validation ------------------------------------------------------
        boolean negativeRejected = false;
        try {
            new Reservoir<Integer>(-1);
        } catch (IllegalArgumentException error) {
            negativeRejected = true;
        }
        check(negativeRejected, "a negative capacity rejected");

        boolean nullRejected = false;
        try {
            new Reservoir<Integer>(1, null);
        } catch (IllegalArgumentException error) {
            nullRejected = true;
        }
        check(nullRejected, "a null random source rejected");

        List<DoubleSupplier> badSources = List.of(
                () -> 1.0,
                () -> -0.1,
                () -> Double.NaN,
                () -> Double.POSITIVE_INFINITY);
        for (DoubleSupplier source : badSources) {
            Reservoir<Integer> sampler = new Reservoir<>(1, source);
            sampler.add(1);
            boolean threw = false;
            try {
                sampler.add(2);
            } catch (IllegalArgumentException error) {
                threw = true;
            }
            check(threw, "an out-of-range draw rejected");
        }
        System.out.println("validation   : negative capacity, null source, and out-of-range draws rejected");

        System.out.println("All checks passed.");
    }
}
