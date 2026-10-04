import java.util.ArrayList;
import java.util.List;

/**
 * 100 - HyperLogLog: estimate how many distinct values you have seen using a
 * fixed array of small counters. Each value sets one register to the length of a
 * run of leading zeros in its hash, and the harmonic mean of the registers gives
 * the estimate.
 *
 * Compile and run:
 *   javac HyperLogLog.java
 *   java HyperLogLog
 */
public class HyperLogLog {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /** A 64 bit hash: FNV-1a for the mixing, then a finaliser to spread the high bits. */
    static long hash64(String value) {
        long hash = 0xcbf29ce484222325L;
        for (int i = 0; i < value.length(); i++) {
            hash ^= value.charAt(i);
            hash *= 0x100000001b3L;
        }
        hash += 0x9e3779b97f4a7c15L;
        hash = (hash ^ (hash >>> 30)) * 0xbf58476d1ce4e5b9L;
        hash = (hash ^ (hash >>> 27)) * 0x94d049bb133111ebL;
        return hash ^ (hash >>> 31);
    }

    static final class Sketch {
        private final int precision;
        private final int[] registers;
        private final int bucketCount;

        Sketch(int precision) {
            if (precision < 4 || precision > 20) {
                throw new IllegalArgumentException("precision must be between 4 and 20, got " + precision);
            }
            this.precision = precision;
            this.bucketCount = 1 << precision;
            this.registers = new int[bucketCount];
        }

        void add(String value) {
            long hash = hash64(value);
            int bucket = (int) (hash >>> (64 - precision));          // the top p bits
            long rest = hash << precision;                           // everything else
            int rank = Long.numberOfLeadingZeros(rest) + 1;
            if (rank > registers[bucket]) {
                registers[bucket] = rank;                            // keep only the maximum
            }
        }

        /** Two sketches of disjoint sets merge by taking the larger register. */
        void merge(Sketch other) {
            if (other.precision != precision) {
                throw new IllegalArgumentException("cannot merge sketches of different precision");
            }
            for (int i = 0; i < bucketCount; i++) {
                registers[i] = Math.max(registers[i], other.registers[i]);
            }
        }

        double estimate() {
            double sum = 0;
            int emptyRegisters = 0;
            for (int register : registers) {
                sum += Math.pow(2, -register);
                if (register == 0) {
                    emptyRegisters++;
                }
            }

            double alpha = switch (bucketCount) {
                case 16 -> 0.673;
                case 32 -> 0.697;
                case 64 -> 0.709;
                default -> 0.7213 / (1 + 1.079 / bucketCount);
            };
            double raw = alpha * bucketCount * (double) bucketCount / sum;

            // Linear counting is far better while many registers are still empty.
            if (raw <= 2.5 * bucketCount && emptyRegisters > 0) {
                return bucketCount * Math.log((double) bucketCount / emptyRegisters);
            }
            return raw;
        }

        long roundedEstimate() {
            return Math.round(estimate());
        }

        int registerCount() {
            return bucketCount;
        }

        int precision() {
            return precision;
        }

        int highestRegister() {
            int highest = 0;
            for (int register : registers) {
                highest = Math.max(highest, register);
            }
            return highest;
        }

        int emptyRegisters() {
            int count = 0;
            for (int register : registers) {
                if (register == 0) {
                    count++;
                }
            }
            return count;
        }

        int bytesUsed() {
            return registers.length * Integer.BYTES;
        }
    }

    static double errorPercent(long estimate, long actual) {
        return actual == 0 ? 0 : Math.abs(estimate - actual) * 100.0 / actual;
    }

    static List<String> values(String prefix, int count) {
        List<String> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            items.add(prefix + i);
        }
        return items;
    }

    public static void main(String[] args) {
        // ---- nothing added ----------------------------------------------------
        Sketch empty = new Sketch(14);
        check(empty.estimate() == 0, "an empty sketch estimates zero");
        check(empty.emptyRegisters() == empty.registerCount(), "because every register is empty");
        System.out.println("empty        : estimate 0, " + empty.registerCount() + " registers");

        // ---- accuracy as the cardinality grows -------------------------------
        for (int trueCount : new int[] {10, 100, 1_000, 10_000, 100_000}) {
            Sketch sketch = new Sketch(14);
            for (String value : values("item-", trueCount)) {
                sketch.add(value);
            }
            long estimate = sketch.roundedEstimate();
            double error = errorPercent(estimate, trueCount);
            // The standard error is about 1.04/sqrt(16384), close to 0.8%, so 3% is
            // a generous bound; the exact figure is deterministic for this input.
            check(error < 3.0, trueCount + " estimated as " + estimate + ", off by "
                    + String.format("%.2f%%", error));
        }
        System.out.println("accuracy     : five sizes from 10 to 100,000 all within 3%");

        // a worked example, printed in full
        Sketch sample = new Sketch(14);
        values("user-", 50_000).forEach(sample::add);
        long sampleEstimate = sample.roundedEstimate();
        System.out.printf("50,000 users : estimated %d (off by %.2f%%), %d bytes%n",
                sampleEstimate, errorPercent(sampleEstimate, 50_000), sample.bytesUsed());
        check(errorPercent(sampleEstimate, 50_000) < 3.0, "the estimate is close");

        // ---- far smaller than storing the values -----------------------------
        // Even with one int per register; a production implementation packs them
        // into six bits each, which would be about 12KB here.
        long roughStorageForValues = 50_000L * 12;
        check(sample.bytesUsed() < roughStorageForValues / 8,
                "the sketch is far smaller than the data: " + sample.bytesUsed()
                        + " bytes against roughly " + roughStorageForValues);
        check(sample.bytesUsed() < 100 * 1024, "under 100KB for 50,000 distinct values");
        int packedBytes = (int) Math.ceil(sample.registerCount() * 6.0 / 8);
        check(packedBytes < 16 * 1024, "packed at six bits a register it would be "
                + packedBytes + " bytes");
        System.out.println("memory       : " + sample.bytesUsed() + " bytes for 50,000 values ("
                + packedBytes + " if packed)");

        // ---- precision trades accuracy for memory ----------------------------
        // The standard error is about 1.04/sqrt(m), so the bound used here is four
        // standard errors. Note the coarse sketches all overestimate: the raw
        // estimator carries a bias at small m, and this implementation has no
        // bias-correction table, which is exactly the trade-off being shown.
        for (int precision : new int[] {4, 6, 8, 10, 14}) {
            Sketch sketch = new Sketch(precision);
            values("item-", 20_000).forEach(sketch::add);
            long estimate = sketch.roundedEstimate();
            double error = errorPercent(estimate, 20_000);
            double allowed = 400.0 * 1.04 / Math.sqrt(sketch.registerCount());
            check(sketch.registerCount() == (1 << precision), "the register count follows the precision");
            check(error < allowed, "precision " + precision + " erred by "
                    + String.format("%.1f%%", error) + ", within four standard errors ("
                    + String.format("%.1f%%", allowed) + ")");
            System.out.printf("precision %-2d : %6d registers (%7d bytes), estimate %6d, error %5.1f%% "
                            + "(bound %4.1f%%)%n",
                    precision, sketch.registerCount(), sketch.bytesUsed(), estimate, error, allowed);
        }

        // a very coarse sketch is noticeably worse than a fine one
        Sketch coarse = new Sketch(4);
        Sketch fine = new Sketch(16);
        values("value-", 100_000).forEach(value -> {
            coarse.add(value);
            fine.add(value);
        });
        double coarseError = errorPercent(coarse.roundedEstimate(), 100_000);
        double fineError = errorPercent(fine.roundedEstimate(), 100_000);
        check(fineError < coarseError, "16 bits of precision beats 4: "
                + String.format("%.1f%% against %.1f%%", fineError, coarseError));
        System.out.printf("trade-off    : precision 4 errs by %.1f%%, precision 16 by %.1f%%%n",
                coarseError, fineError);

        // ---- duplicates do not move the estimate -----------------------------
        Sketch withDuplicates = new Sketch(14);
        List<String> repeated = new ArrayList<>();
        for (int i = 0; i < 10_000; i++) {
            repeated.add("same-" + (i % 1_000));
        }
        repeated.forEach(withDuplicates::add);
        check(errorPercent(withDuplicates.roundedEstimate(), 1_000) < 15.0,
                "10,000 insertions of 1,000 distinct values still estimates about 1,000: "
                        + withDuplicates.roundedEstimate());
        System.out.println("duplicates   : 10,000 adds of 1,000 distinct values estimates "
                + withDuplicates.roundedEstimate());

        // ---- merging ----------------------------------------------------------
        Sketch left = new Sketch(14);
        Sketch right = new Sketch(14);
        values("left-", 30_000).forEach(left::add);
        values("right-", 40_000).forEach(right::add);

        long leftOnly = left.roundedEstimate();
        long rightOnly = right.roundedEstimate();
        Sketch merged = new Sketch(14);
        values("left-", 30_000).forEach(merged::add);
        values("right-", 40_000).forEach(merged::add);

        left.merge(right);
        check(errorPercent(left.roundedEstimate(), 70_000) < 3.0,
                "the merged sketch estimates the union: " + left.roundedEstimate());
        check(errorPercent(left.roundedEstimate(), merged.roundedEstimate()) < 1.0,
                "and merging gives the same answer as adding everything to one sketch");
        System.out.println("merge        : " + leftOnly + " + " + rightOnly
                + " -> " + left.roundedEstimate() + " (expected about 70,000)");

        // ---- it really is only an estimate -----------------------------------
        check(hash64("alpha") == hash64("alpha"), "hashing is deterministic");
        check(hash64("alpha") != hash64("beta"), "and different values hash differently");
        check(sample.highestRegister() > 0, "registers hold run lengths, not booleans");
        check(sample.highestRegister() <= 64, "a rank cannot exceed the hash width");
        System.out.println("registers    : highest rank seen " + sample.highestRegister());

        // ---- bad precision ----------------------------------------------------
        for (int bad : new int[] {0, 3, 21, 64}) {
            try {
                new Sketch(bad);
                throw new AssertionError("precision " + bad + " should have been rejected");
            } catch (IllegalArgumentException expected) {
                // expected
            }
        }
        try {
            new Sketch(14).merge(new Sketch(10));
            throw new AssertionError("sketches of different precision cannot merge");
        } catch (IllegalArgumentException expected) {
            System.out.println("bad merge    : " + expected.getMessage());
        }
        System.out.println("All checks passed.");
    }
}
