import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 82 - A Bloom filter: a bitmap with several hashes that can answer "definitely
 * not present" or "possibly present", in a fixed amount of memory.
 *
 * There are never false negatives. False positives are possible, and the size
 * and hash count are chosen from the expected item count and target rate.
 *
 * Compile and run:
 *   javac BloomFilter.java
 *   java BloomFilter
 */
public class BloomFilter {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /** FNV-1a, 64 bit: short, dependency free and well spread for short strings. */
    static long fnv1a(String text) {
        long hash = 0xcbf29ce484222325L;
        for (int i = 0; i < text.length(); i++) {
            hash ^= text.charAt(i);
            hash *= 0x100000001b3L;
        }
        return hash;
    }

    static final class Bloom {
        private final boolean[] bits;
        private final int hashCount;
        private int inserted;

        Bloom(int bitCount, int hashCount) {
            if (bitCount < 1 || hashCount < 1) {
                throw new IllegalArgumentException("bitCount and hashCount must be at least 1");
            }
            this.bits = new boolean[bitCount];
            this.hashCount = hashCount;
        }

        /**
         * The standard sizing formulas: m = -n ln p / (ln 2)^2 for the bit count,
         * k = (m/n) ln 2 for the number of hashes.
         */
        static Bloom forExpected(int expectedItems, double falsePositiveRate) {
            if (expectedItems < 1) {
                throw new IllegalArgumentException("expectedItems must be at least 1");
            }
            if (!(falsePositiveRate > 0 && falsePositiveRate < 1)) {
                throw new IllegalArgumentException("falsePositiveRate must be between 0 and 1");
            }
            double ln2 = Math.log(2);
            int bitCount = (int) Math.ceil(-expectedItems * Math.log(falsePositiveRate) / (ln2 * ln2));
            int hashCount = Math.max(1, (int) Math.round((double) bitCount / expectedItems * ln2));
            return new Bloom(bitCount, hashCount);
        }

        /**
         * Double hashing: derive every index from two base hashes instead of
         * computing k independent hashes.
         */
        private int index(long first, long second, int round) {
            return (int) Math.floorMod(first + (long) round * second, bits.length);
        }

        private long[] hashes(String value) {
            long first = fnv1a(value);
            long second = fnv1a(value + "|salt") | 1L;   // odd, so it walks the whole ring
            return new long[] {first, second};
        }

        void add(String value) {
            long[] pair = hashes(value);
            for (int round = 0; round < hashCount; round++) {
                bits[index(pair[0], pair[1], round)] = true;
            }
            inserted++;
        }

        boolean mightContain(String value) {
            long[] pair = hashes(value);
            for (int round = 0; round < hashCount; round++) {
                if (!bits[index(pair[0], pair[1], round)]) {
                    return false;      // one clear bit proves absence
                }
            }
            return true;
        }

        int bitCount() {
            return bits.length;
        }

        int hashCount() {
            return hashCount;
        }

        int inserted() {
            return inserted;
        }

        int setBits() {
            int count = 0;
            for (boolean bit : bits) {
                if (bit) {
                    count++;
                }
            }
            return count;
        }

        double fillRatio() {
            return (double) setBits() / bits.length;
        }

        /** The expected false positive rate for what has actually been stored. */
        double theoreticalFalsePositiveRate() {
            return Math.pow(1 - Math.exp(-(double) hashCount * inserted / bits.length), hashCount);
        }
    }

    /** Count how many of these are wrongly reported as present. */
    static int falsePositives(Bloom filter, List<String> absent) {
        int wrong = 0;
        for (String value : absent) {
            if (filter.mightContain(value)) {
                wrong++;
            }
        }
        return wrong;
    }

    static List<String> words(String prefix, int count) {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            values.add(prefix + i);
        }
        return values;
    }

    public static void main(String[] args) {
        // ---- sizing -----------------------------------------------------------
        Bloom tuned = Bloom.forExpected(1_000, 0.01);
        check(tuned.hashCount() == 7, "about 7 hashes per item for a 1% target");
        check(tuned.bitCount() == 9_586, "and 9,586 bits, which is under 1.2KB");
        check(tuned.bitCount() < 1_000 * 12, "far smaller than storing the strings themselves");
        System.out.printf("sizing      : %d bits (%.1f per item), %d hashes for a 1%% target%n",
                tuned.bitCount(), (double) tuned.bitCount() / 1_000, tuned.hashCount());

        check(Bloom.forExpected(1_000_000, 0.01).bitCount() > tuned.bitCount(),
                "more items need more bits");
        check(Bloom.forExpected(1_000, 0.001).bitCount() > tuned.bitCount(),
                "a stricter target needs more bits");
        check(Bloom.forExpected(1_000, 0.05).hashCount() < tuned.hashCount(),
                "a looser target needs fewer hashes");

        for (Object[] bad : new Object[][] {{0, 0.01}, {1_000, 0.0}, {1_000, 1.0}, {1_000, -0.5}}) {
            try {
                Bloom.forExpected((Integer) bad[0], (Double) bad[1]);
                throw new AssertionError("should have been rejected: " + bad[0] + ", " + bad[1]);
            } catch (IllegalArgumentException expected) {
                // expected
            }
        }
        System.out.println("rejected    : zero items and out-of-range rates");

        // ---- no false negatives ----------------------------------------------
        List<String> members = new ArrayList<>();
        for (int i = 0; i < 1_000; i++) {
            members.add(String.format("word-%05d", i));
        }
        members.forEach(tuned::add);

        int missed = 0;
        for (String member : members) {
            if (!tuned.mightContain(member)) {
                missed++;
            }
        }
        check(missed == 0, "every inserted value is still reported as possibly present");
        check(tuned.inserted() == 1_000, "a thousand values were added");
        System.out.printf("fill        : %.1f%% of the bits are set%n", tuned.fillRatio() * 100);

        // ---- measured false positives, against the theory ---------------------
        Random seeded = new Random(42);
        List<String> absent = new ArrayList<>();
        for (int i = 0; i < 20_000; i++) {
            absent.add("absent-" + seeded.nextInt(10_000_000));
        }
        int wrong = falsePositives(tuned, absent);
        double measured = (double) wrong / absent.size();
        double theory = tuned.theoreticalFalsePositiveRate();

        check(measured > 0, "with 10,000 bits for 1,000 items, collisions do happen");
        check(measured < 0.05, "but the rate stays well under 5%: " + measured);
        check(Math.abs(measured - theory) < 0.02,
                "and it lands close to the predicted " + theory);
        System.out.printf("false pos   : %.2f%% measured, %.2f%% predicted over %d absent values%n",
                measured * 100, theory * 100, absent.size());

        // ---- a filter that is too small saturates ----------------------------
        Bloom tiny = new Bloom(8, 3);
        for (int i = 0; i < 20; i++) {
            tiny.add("filler-" + i);
        }
        check(tiny.setBits() == 8, "all eight bits get set");
        check(tiny.fillRatio() == 1.0, "so it is completely full");
        check(tiny.mightContain("anything-at-all"), "and now it claims to contain everything");
        check(tiny.mightContain("even-this"), "including values it has never seen");
        // The members are still found, so even a saturated filter has no false negatives.
        check(tiny.mightContain("filler-0") && tiny.mightContain("filler-19"),
                "but there are still no false negatives");
        System.out.println("saturated   : a full filter says yes to everything, with no false negatives");

        // ---- a generous filter is much sharper -------------------------------
        Bloom roomy = Bloom.forExpected(1_000, 0.001);
        members.forEach(roomy::add);
        int roomyWrong = falsePositives(roomy, absent);
        check(roomyWrong < wrong, "a larger filter makes fewer mistakes: "
                + roomyWrong + " versus " + wrong);
        check(members.stream().allMatch(roomy::mightContain), "and still finds every member");
        System.out.printf("roomier     : %d bits, %d false positives versus %d%n",
                roomy.bitCount(), roomyWrong, wrong);

        // ---- hashing and the smallest possible filter -------------------------
        check(fnv1a("hello") != fnv1a("world"), "two different words hash to different values");
        check(fnv1a("") != fnv1a("a"), "the empty string hashes too");
        check(fnv1a("same") == fnv1a("same"), "and hashing is deterministic");

        Bloom empty = new Bloom(64, 4);
        check(!empty.mightContain("anything"), "an empty filter contains nothing at all");
        check(empty.setBits() == 0, "and has no bits set");
        empty.add("hello");
        check(empty.mightContain("hello"), "the added word is then found");
        check(empty.mightContain("hello"), "repeatedly, since nothing is consumed");
        check(empty.setBits() > 0 && empty.setBits() <= 4, "one word sets at most four bits");
        System.out.println("All checks passed.");
    }
}
