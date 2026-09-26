import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 47 - Divide and conquer: fast exponentiation, modular exponentiation, peasant
 * multiplication, Euclid's GCD and the Tower of Hanoi.
 *
 * Compile and run:
 *   javac DivideAndConquer.java
 *   java DivideAndConquer
 */
public class DivideAndConquer {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    // ------------------------------------------------- exponentiation by squaring
    static long power(long base, int exponent) {
        if (exponent < 0) {
            throw new IllegalArgumentException("exponent must not be negative");
        }
        long result = 1;
        long factor = base;
        int remaining = exponent;
        while (remaining > 0) {
            if ((remaining & 1) == 1) {
                result *= factor;
            }
            factor *= factor;
            remaining >>= 1;
        }
        return result;
    }

    static long powerMod(long base, long exponent, long modulus) {
        long result = 1;
        long factor = base % modulus;
        long remaining = exponent;
        while (remaining > 0) {
            if ((remaining & 1) == 1) {
                result = result * factor % modulus;
            }
            factor = factor * factor % modulus;
            remaining >>= 1;
        }
        return result;
    }

    // ------------------------------------------------- Russian peasant multiply
    static long peasantMultiply(long left, long right) {
        long total = 0;
        long a = left;
        long b = right;
        while (b > 0) {
            if ((b & 1) == 1) {
                total += a;
            }
            a <<= 1;      // double
            b >>= 1;      // halve
        }
        return total;
    }

    // ------------------------------------------------------------ Euclid's GCD
    static long gcdRecursive(long a, long b) {
        return b == 0 ? Math.abs(a) : gcdRecursive(b, a % b);
    }

    static long gcdIterative(long a, long b) {
        long left = Math.abs(a);
        long right = Math.abs(b);
        while (right != 0) {
            long remainder = left % right;
            left = right;
            right = remainder;
        }
        return left;
    }

    static long lcm(long a, long b) {
        if (a == 0 || b == 0) {
            return 0;
        }
        return Math.abs(a / gcdIterative(a, b) * b);
    }

    // -------------------------------------------------------------- Tower of Hanoi
    static void hanoi(int discs, char from, char to, char via, List<String> moves) {
        if (discs == 0) {
            return;
        }
        hanoi(discs - 1, from, via, to, moves);
        moves.add(from + "->" + to);
        hanoi(discs - 1, via, to, from, moves);
    }

    /** Replay the moves on three stacks, proving no larger disc lands on a smaller one. */
    static void verifyHanoi(List<String> moves, int discs) {
        Map<Character, Deque<Integer>> pegs = new LinkedHashMap<>();
        for (char peg : new char[] {'A', 'B', 'C'}) {
            pegs.put(peg, new ArrayDeque<>());
        }
        for (int disc = discs; disc >= 1; disc--) {
            pegs.get('A').push(disc);          // 1 is the smallest, and on top
        }
        for (String move : moves) {
            char from = move.charAt(0);
            char to = move.charAt(3);
            if (pegs.get(from).isEmpty()) {
                throw new AssertionError("move from an empty peg: " + move);
            }
            int disc = pegs.get(from).pop();
            if (!pegs.get(to).isEmpty() && pegs.get(to).peek() < disc) {
                throw new AssertionError("a larger disc landed on a smaller one: " + move);
            }
            pegs.get(to).push(disc);
        }
        check(pegs.get('C').size() == discs, "every disc ended on the target peg");
        check(pegs.get('A').isEmpty() && pegs.get('B').isEmpty(), "the other pegs are empty");
    }

    public static void main(String[] args) {
        // ---- fast exponentiation, checked against BigInteger -----------------
        check(power(2, 10) == 1024, "2^10");
        check(power(5, 0) == 1, "anything to the power 0 is 1");
        check(power(3, 20) == BigInteger.valueOf(3).pow(20).longValue(),
                "3^20 matches BigInteger.pow exactly");
        check(power(2, 62) == BigInteger.TWO.pow(62).longValue(), "the largest power that fits a long");
        try {
            power(2, -1);
            throw new AssertionError("a negative exponent should fail");
        } catch (IllegalArgumentException expected) {
            System.out.println("negative exponent rejected: " + expected.getMessage());
        }
        System.out.println("3^20        : " + power(3, 20));

        // ---- modular exponentiation, checked against BigInteger.modPow --------
        long modulus = 1_000_000_007L;
        BigInteger expected = BigInteger.TWO
                .modPow(BigInteger.valueOf(1000), BigInteger.valueOf(modulus));
        check(powerMod(2, 1000, modulus) == expected.longValue(),
                "2^1000 mod 1e9+7 matches BigInteger.modPow");
        check(powerMod(7, 0, modulus) == 1, "exponent 0");
        System.out.println("2^1000 mod p: " + powerMod(2, 1000, modulus));

        // ---- peasant multiplication -----------------------------------------
        check(peasantMultiply(13, 11) == 143, "13 x 11");
        check(peasantMultiply(0, 99) == 0, "zero");
        check(peasantMultiply(1234, 5678) == 1234L * 5678L, "it agrees with the * operator");
        System.out.println("13 x 11     : " + peasantMultiply(13, 11));

        // ---- GCD and LCM -----------------------------------------------------
        check(gcdRecursive(48, 18) == 6, "gcd(48, 18)");
        check(gcdIterative(48, 18) == 6, "the iterative version agrees");
        check(gcdRecursive(17, 5) == 1, "coprime numbers");
        check(gcdIterative(0, 9) == 9, "gcd with zero");
        check(lcm(4, 6) == 12 && lcm(21, 6) == 42, "lcm");
        check(gcdRecursive(1_000_000_007L, 998_244_353L) == 1, "two large primes are coprime");
        System.out.println("gcd/lcm     : gcd(48,18)=" + gcdIterative(48, 18) + " lcm(4,6)=" + lcm(4, 6));

        // ---- Tower of Hanoi --------------------------------------------------
        for (int discs = 1; discs <= 10; discs++) {
            List<String> moves = new ArrayList<>();
            hanoi(discs, 'A', 'C', 'B', moves);
            check(moves.size() == (1 << discs) - 1, discs + " discs take 2^n - 1 moves");
            verifyHanoi(moves, discs);
        }
        List<String> small = new ArrayList<>();
        hanoi(3, 'A', 'C', 'B', small);
        check(small.get(0).equals("A->C"), "the first move takes the smallest disc to C");
        check(small.get(small.size() - 1).equals("A->C"), "the last move puts the biggest disc on C");
        System.out.println("hanoi(3)    : " + String.join(" ", small));
        System.out.println("hanoi(10)   : " + ((1 << 10) - 1) + " moves, all legal");
        System.out.println("All checks passed.");
    }
}
