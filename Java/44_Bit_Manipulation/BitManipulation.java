import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * 44 - Bit manipulation: shifts, masks and flags, then a BitSet used as a prime
 * sieve. Also the difference between >> and >>> and how shift counts wrap.
 *
 * Compile and run:
 *   javac BitManipulation.java
 *   java BitManipulation
 */
public class BitManipulation {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    // permission flags: one bit each
    static final int READ = 1;          // 001
    static final int WRITE = 1 << 1;    // 010
    static final int EXECUTE = 1 << 2;  // 100

    static int setBit(int value, int index) {
        return value | (1 << index);
    }

    static int clearBit(int value, int index) {
        return value & ~(1 << index);
    }

    static int toggleBit(int value, int index) {
        return value ^ (1 << index);
    }

    static boolean isSet(int value, int index) {
        return ((value >>> index) & 1) == 1;
    }

    public static void main(String[] args) {
        // ---- shifts ----------------------------------------------------------
        check(-8 >> 1 == -4, ">> keeps the sign (arithmetic shift)");
        check(-8 >>> 1 == 2_147_483_644, ">>> fills with zeros (logical shift)");
        check(1 << 31 == Integer.MIN_VALUE, "shifting into the sign bit");
        check(1 << 32 == 1, "an int shift uses the count modulo 32");
        check(1L << 64 == 1L, "a long shift uses the count modulo 64");
        System.out.printf("-8 >> 1  = %d, -8 >>> 1 = %d%n", -8 >> 1, -8 >>> 1);

        // ---- unpacking a packed colour ---------------------------------------
        int colour = 0xFF3366CC;      // alpha, red, green, blue
        int alpha = (colour >>> 24) & 0xFF;
        int red = (colour >>> 16) & 0xFF;
        int green = (colour >>> 8) & 0xFF;
        int blue = colour & 0xFF;
        check(alpha == 255 && red == 51 && green == 102 && blue == 204, "unpacking RGBA");
        int repacked = (alpha << 24) | (red << 16) | (green << 8) | blue;
        check(repacked == colour, "repacking gives the same int");
        System.out.printf("colour   : A=%d R=%d G=%d B=%d (%s)%n",
                alpha, red, green, blue, Integer.toHexString(colour));

        // ---- flags: one int, several booleans --------------------------------
        int permissions = READ | WRITE;
        check((permissions & READ) != 0, "READ is on");
        check((permissions & EXECUTE) == 0, "EXECUTE is off");
        permissions |= EXECUTE;              // grant
        permissions &= ~WRITE;               // revoke
        check(permissions == (READ | EXECUTE), "grant then revoke");
        check(Integer.toBinaryString(permissions).equals("101"), "flags as binary");
        System.out.println("perms    : " + Integer.toBinaryString(permissions) + " (read + execute)");

        // ---- single-bit helpers ---------------------------------------------
        int bits = 0b1010;
        check(setBit(bits, 0) == 0b1011, "set a clear bit");
        check(setBit(bits, 1) == 0b1010, "setting an already-set bit changes nothing");
        check(clearBit(bits, 1) == 0b1000, "clear a set bit");
        check(toggleBit(bits, 0) == 0b1011, "toggle flips it");
        check(isSet(bits, 1) && !isSet(bits, 0), "testing bits");

        // ---- counting bits ---------------------------------------------------
        check(Integer.bitCount(0b1011) == 3, "bitCount");
        check(Integer.numberOfTrailingZeros(0b1000) == 3, "trailing zeros");
        check(Integer.highestOneBit(0b1011) == 0b1000, "highestOneBit");
        check(Integer.lowestOneBit(0b1100) == 0b0100, "lowestOneBit");
        check(Long.bitCount(-1L) == 64, "all 64 bits set");

        // ---- signed versus unsigned ------------------------------------------
        check(Integer.compareUnsigned(-1, 1) > 0, "-1 is the largest unsigned int");
        check(Integer.toUnsignedString(-1).equals("4294967295"), "unsigned string form");
        check(Integer.parseUnsignedInt("4294967295") == -1, "parsing an unsigned value");
        check(Integer.divideUnsigned(-1, 2) == 2_147_483_647, "unsigned division");
        System.out.println("unsigned : " + Integer.toUnsignedString(-1));

        // ---- BitSet: a sieve of Eratosthenes ---------------------------------
        int limit = 50;
        BitSet composite = new BitSet(limit + 1);
        for (int candidate = 2; candidate * candidate <= limit; candidate++) {
            if (!composite.get(candidate)) {
                for (int multiple = candidate * candidate; multiple <= limit; multiple += candidate) {
                    composite.set(multiple);
                }
            }
        }
        List<Integer> primes = new ArrayList<>();
        for (int value = 2; value <= limit; value++) {
            if (!composite.get(value)) {
                primes.add(value);
            }
        }
        check(primes.equals(List.of(2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47)),
                "the 15 primes up to 50");
        check(composite.cardinality() == 49 - 15, "34 composites among 2..50");
        check(composite.nextSetBit(10) == 10, "nextSetBit finds 10, which is composite");
        check(composite.nextClearBit(10) == 11, "11 is the next prime");
        System.out.println("primes   : " + primes);

        BitSet left = new BitSet();
        left.set(1);
        left.set(3);
        BitSet right = new BitSet();
        right.set(3);
        right.set(4);
        BitSet union = (BitSet) left.clone();
        union.or(right);
        BitSet intersection = (BitSet) left.clone();
        intersection.and(right);
        check(union.cardinality() == 3, "union of {1,3} and {3,4}");
        check(intersection.cardinality() == 1 && intersection.get(3), "intersection is {3}");
        System.out.println("All checks passed.");
    }
}
