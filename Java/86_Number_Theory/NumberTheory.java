import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 86 - Number theory: a prime sieve, factorisation, the extended Euclidean
 * algorithm, modular inverses, the Chinese remainder theorem, Euler's totient
 * and a deterministic Miller-Rabin primality test.
 *
 * Compile and run:
 *   javac NumberTheory.java
 *   java NumberTheory
 */
public class NumberTheory {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    // ---------------------------------------------------------------- primes
    static boolean[] sieve(int limit) {
        boolean[] composite = new boolean[limit + 1];
        for (int candidate = 2; (long) candidate * candidate <= limit; candidate++) {
            if (!composite[candidate]) {
                for (int multiple = candidate * candidate; multiple <= limit; multiple += candidate) {
                    composite[multiple] = true;
                }
            }
        }
        return composite;
    }

    static List<Integer> primesUpTo(int limit) {
        boolean[] composite = sieve(limit);
        List<Integer> primes = new ArrayList<>();
        for (int value = 2; value <= limit; value++) {
            if (!composite[value]) {
                primes.add(value);
            }
        }
        return primes;
    }

    /** Prime factors with their multiplicity, in ascending order. */
    static Map<Long, Integer> factorize(long value) {
        Map<Long, Integer> factors = new TreeMap<>();
        if (value <= 1) {
            return factors;
        }
        long remaining = value;
        for (long divisor = 2; divisor * divisor <= remaining; divisor += divisor == 2 ? 1 : 2) {
            while (remaining % divisor == 0) {
                factors.merge(divisor, 1, Integer::sum);
                remaining /= divisor;
            }
        }
        if (remaining > 1) {
            factors.merge(remaining, 1, Integer::sum);
        }
        return factors;
    }

    static String render(Map<Long, Integer> factors) {
        if (factors.isEmpty()) {
            return "1";
        }
        StringBuilder out = new StringBuilder();
        factors.forEach((prime, power) -> {
            if (out.length() > 0) {
                out.append(" x ");
            }
            out.append(prime);
            if (power > 1) {
                out.append('^').append(power);
            }
        });
        return out.toString();
    }

    // ------------------------------------------------- modular arithmetic
    // Multiplying two longs near 2^63 overflows, so the product is widened for
    // the modular step. The squaring loop itself is the schoolbook version.
    static long multiplyModulo(long left, long right, long modulus) {
        return BigInteger.valueOf(left).multiply(BigInteger.valueOf(right))
                .mod(BigInteger.valueOf(modulus)).longValue();
    }

    static long powerModulo(long base, long exponent, long modulus) {
        if (modulus == 1) {
            return 0;
        }
        long result = 1;
        long factor = Math.floorMod(base, modulus);
        long remaining = exponent;
        while (remaining > 0) {
            if ((remaining & 1L) == 1L) {
                result = multiplyModulo(result, factor, modulus);
            }
            factor = multiplyModulo(factor, factor, modulus);
            remaining >>= 1;
        }
        return result;
    }

    /** The modular inverse by the extended Euclidean algorithm. */
    static long modularInverse(long value, long modulus) {
        long oldR = value;
        long r = modulus;
        long oldS = 1;
        long s = 0;

        while (r != 0) {
            long quotient = oldR / r;
            long nextR = oldR - quotient * r;
            oldR = r;
            r = nextR;
            long nextS = oldS - quotient * s;
            oldS = s;
            s = nextS;
        }
        if (oldR != 1) {
            throw new IllegalArgumentException(value + " has no inverse modulo " + modulus
                    + " because their gcd is " + oldR);
        }
        return Math.floorMod(oldS, modulus);
    }

    record Congruence(long remainder, long modulus) {
    }

    /** The unique solution modulo the product, assuming the moduli are coprime. */
    static long chineseRemainder(List<Congruence> system) {
        long product = 1;
        for (Congruence congruence : system) {
            product *= congruence.modulus();
        }
        long total = 0;
        for (Congruence congruence : system) {
            long partial = product / congruence.modulus();
            total += congruence.remainder() * partial * modularInverse(partial, congruence.modulus());
        }
        return Math.floorMod(total, product);
    }

    /** Euler's totient, from the factorisation: multiply p-1/p for each prime. */
    static long totient(long value) {
        long result = value;
        for (Long prime : factorize(value).keySet()) {
            result = result / prime * (prime - 1);
        }
        return result;
    }

    // -------------------------------------------------------- primality
    /**
     * Deterministic for everything below 3.3 x 10^24 with this witness set, so no
     * random witnesses and no probabilistic answer in this range.
     */
    static boolean isPrime(long candidate) {
        if (candidate < 2) {
            return false;
        }
        for (long small : new long[] {2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37}) {
            if (candidate == small) {
                return true;
            }
            if (candidate % small == 0) {
                return false;
            }
        }

        long d = candidate - 1;
        int rounds = 0;
        while (d % 2 == 0) {
            d /= 2;
            rounds++;
        }

        for (long witness : new long[] {2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37}) {
            long x = powerModulo(witness, d, candidate);
            if (x == 1 || x == candidate - 1) {
                continue;
            }
            boolean composite = true;
            for (int round = 1; round < rounds; round++) {
                x = multiplyModulo(x, x, candidate);
                if (x == candidate - 1) {
                    composite = false;
                    break;
                }
            }
            if (composite) {
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) {
        // ---- the sieve --------------------------------------------------------
        List<Integer> underHundred = primesUpTo(100);
        check(underHundred.size() == 25, "there are 25 primes below 100");
        check(underHundred.get(0) == 2 && underHundred.get(24) == 97, "from 2 to 97");
        check(primesUpTo(1).isEmpty() && primesUpTo(2).equals(List.of(2)), "the small boundaries");
        check(primesUpTo(1_000).size() == 168, "168 primes below 1000");
        System.out.println("primes < 100 : " + underHundred);

        // every sieved prime must satisfy the independent primality test
        for (int prime : primesUpTo(2_000)) {
            check(isPrime(prime), "the sieve and Miller-Rabin agree that " + prime + " is prime");
        }
        System.out.println("cross-check  : the sieve and Miller-Rabin agree below 2000");

        // ---- factorisation ----------------------------------------------------
        check(render(factorize(360)).equals("2^3 x 3^2 x 5"), "360 factorises as expected");
        check(render(factorize(97)).equals("97"), "a prime is its own factor");
        check(factorize(1).isEmpty(), "1 has no prime factors");
        check(factorize(0).isEmpty(), "and neither does 0, by convention");
        check(render(factorize(1_000_000)).equals("2^6 x 5^6"), "a million");
        check(render(factorize(1_234_567)).equals("127 x 9721"), "and a less obvious one");
        System.out.println("factorise    : 360 = " + render(factorize(360))
                + ", 1234567 = " + render(factorize(1_234_567)));

        // ---- modular arithmetic ----------------------------------------------
        check(powerModulo(2, 10, 1_000) == 24, "2^10 mod 1000 is 24");
        check(powerModulo(3, 0, 7) == 1, "anything to the power zero is one");
        check(powerModulo(5, 3, 1) == 0, "modulo one is always zero");
        // the squaring loop must agree with BigInteger on cases that cannot overflow
        for (long base = 2; base < 10; base++) {
            for (long exponent = 0; exponent < 20; exponent++) {
                long mine = powerModulo(base, exponent, 1_000_000_007);
                long theirs = BigInteger.valueOf(base)
                        .modPow(BigInteger.valueOf(exponent), BigInteger.valueOf(1_000_000_007))
                        .longValue();
                check(mine == theirs, "powerModulo agrees with BigInteger for " + base + "^" + exponent);
            }
        }
        System.out.println("powerModulo  : agrees with BigInteger.modPow on every case");

        check(modularInverse(3, 11) == 4, "3 * 4 = 12 is 1 modulo 11");
        check(modularInverse(7, 40) == 23, "7 * 23 = 161 is 1 modulo 40");
        check(multiplyModulo(3, modularInverse(3, 11), 11) == 1, "the inverse really undoes the multiply");
        check(modularInverse(1, 7) == 1, "one is its own inverse");
        try {
            modularInverse(6, 9);
            throw new AssertionError("6 and 9 are not coprime, so there is no inverse");
        } catch (IllegalArgumentException expected) {
            System.out.println("no inverse   : " + expected.getMessage());
        }

        // ---- Chinese remainder theorem ---------------------------------------
        long answer = chineseRemainder(List.of(
                new Congruence(2, 3), new Congruence(3, 5), new Congruence(2, 7)));
        check(answer == 23, "the classic puzzle has answer 23");
        for (Congruence congruence : List.of(
                new Congruence(2, 3), new Congruence(3, 5), new Congruence(2, 7))) {
            check(answer % congruence.modulus() == congruence.remainder(),
                    "23 satisfies the congruence modulo " + congruence.modulus());
        }
        check(answer + 105 == 128, "and the next solution is 105 later");
        check(chineseRemainder(List.of(new Congruence(1, 2))) == 1, "a single congruence is trivial");
        System.out.println("CRT          : x = " + answer + " modulo 105");

        // ---- totient ----------------------------------------------------------
        check(totient(36) == 12, "phi(36) is 12");
        check(totient(1) == 1, "phi(1) is 1");
        check(totient(97) == 96, "for a prime, phi(p) is p-1");
        check(totient(1_000_000) == 400_000, "phi(10^6) is 400,000");
        check(totient(2 * 97) == 96, "phi of two primes is (p-1)(q-1): 1 * 96");
        System.out.println("totient      : phi(360) = " + totient(360));

        // Euler's theorem: a^phi(n) = 1 mod n when a and n are coprime
        for (long base : new long[] {7, 11, 13}) {
            check(powerModulo(base, totient(360), 360) == 1,
                    "Euler's theorem holds for " + base + " modulo 360");
        }
        System.out.println("Euler        : a^phi(n) = 1 mod n verified for 360");

        // ---- primality --------------------------------------------------------
        check(isPrime(2) && isPrime(3) && isPrime(97), "small primes");
        check(!isPrime(1) && !isPrime(0) && !isPrime(-7), "nothing below two is prime");
        check(!isPrime(91), "91 is 7 x 13");
        check(!isPrime(1_000_003L * 1_000_033L), "a product of two large primes is composite");
        check(isPrime(1_000_003L), "but the factor itself is prime");
        check(isPrime(2_305_843_009_213_693_951L), "2^61 - 1 is a Mersenne prime");
        // Carmichael numbers fool Fermat's test but not Miller-Rabin
        for (long carmichael : new long[] {561, 1105, 1729, 6601}) {
            check(!isPrime(carmichael), carmichael + " is a Carmichael number, so composite");
        }
        check(isPrime(2_147_483_647L), "2^31 - 1 is prime");
        System.out.println("primality    : 2^61-1 is prime, 561 and friends are not");

        int count = 0;
        for (int value = 2; value <= 10_000; value++) {
            if (isPrime(value)) {
                count++;
            }
        }
        check(count == 1_229, "there are 1,229 primes below 10,000");
        System.out.println("below 10000  : " + count + " primes");
        System.out.println("All checks passed.");
    }
}
