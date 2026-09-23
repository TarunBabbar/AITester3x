import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * 29 - Big numbers: BigInteger for values beyond long, and BigDecimal for exact
 * decimal arithmetic where doubles quietly lose precision.
 *
 * Compile and run:
 *   javac BigNumbers.java
 *   java BigNumbers
 */
public class BigNumbers {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static BigInteger factorial(int n) {
        BigInteger result = BigInteger.ONE;
        for (int i = 2; i <= n; i++) {
            result = result.multiply(BigInteger.valueOf(i));
        }
        return result;
    }

    static int trailingZeros(BigInteger value) {
        int zeros = 0;
        while (value.signum() != 0 && value.mod(BigInteger.TEN).signum() == 0) {
            zeros++;
            value = value.divide(BigInteger.TEN);
        }
        return zeros;
    }

    public static void main(String[] args) {
        // ---- BigInteger -----------------------------------------------------
        BigInteger hundredFactorial = factorial(100);
        check(hundredFactorial.toString().length() == 158, "100! has 158 digits");
        check(hundredFactorial.toString().startsWith("93326215443944152681699238856266700490715968264381621468592963895217"),
                "100! starts with the known digits");
        check(trailingZeros(hundredFactorial) == 24, "100! ends in 24 zeros");
        System.out.println("100!            : " + hundredFactorial.toString().length()
                + " digits, " + trailingZeros(hundredFactorial) + " trailing zeros");

        BigInteger longMax = BigInteger.valueOf(Long.MAX_VALUE);
        check(longMax.add(BigInteger.ONE).toString().equals("9223372036854775808"),
                "BigInteger sails past Long.MAX_VALUE");
        check(BigInteger.TWO.pow(100).mod(BigInteger.valueOf(1000))
                .equals(BigInteger.valueOf(376)), "2^100 mod 1000");
        check(BigInteger.valueOf(48).gcd(BigInteger.valueOf(18))
                .equals(BigInteger.valueOf(6)), "gcd(48, 18)");
        check(BigInteger.valueOf(2).modPow(BigInteger.valueOf(10), BigInteger.valueOf(1000))
                .equals(BigInteger.valueOf(24)), "modPow 2^10 mod 1000");

        BigInteger mersenne = BigInteger.TWO.pow(127).subtract(BigInteger.ONE);
        check(mersenne.isProbablePrime(64), "2^127 - 1 is prime");
        check(!BigInteger.valueOf(91).isProbablePrime(64), "91 = 7 x 13 is not prime");
        check(BigInteger.valueOf(2).nextProbablePrime().equals(BigInteger.valueOf(3)),
                "the next prime after 2 is 3");
        System.out.println("2^127 - 1       : prime=" + mersenne.isProbablePrime(64));

        // ---- BigDecimal ------------------------------------------------------
        double naive = 0.1 + 0.2;
        check(naive != 0.3, "doubles cannot hold 0.1 + 0.2 exactly");
        System.out.println("0.1 + 0.2       : " + naive + "  (double)");

        BigDecimal exact = new BigDecimal("0.1").add(new BigDecimal("0.2"));
        check(exact.compareTo(new BigDecimal("0.3")) == 0, "BigDecimal adds exactly");
        check(exact.equals(new BigDecimal("0.3")), "sum keeps scale 1");
        check(!new BigDecimal("0.30").equals(new BigDecimal("0.3")),
                "equals() also compares scale");
        check(new BigDecimal("0.30").compareTo(new BigDecimal("0.3")) == 0,
                "compareTo() ignores scale");
        System.out.println("0.1 + 0.2       : " + exact + "  (BigDecimal)");

        BigDecimal ten = new BigDecimal("10");
        BigDecimal three = new BigDecimal("3");
        check(ten.divide(three, 2, RoundingMode.HALF_UP).equals(new BigDecimal("3.33")),
                "10/3 at scale 2");
        check(ten.divide(three, 4, RoundingMode.HALF_UP).toString().equals("3.3333"),
                "10/3 at scale 4");
        check(new BigDecimal("2.5").setScale(0, RoundingMode.HALF_UP).equals(new BigDecimal("3")),
                "HALF_UP rounds 2.5 to 3");
        check(new BigDecimal("2.5").setScale(0, RoundingMode.HALF_EVEN).equals(new BigDecimal("2")),
                "HALF_EVEN rounds 2.5 to 2 (banker's rounding)");
        try {
            ten.divide(three);
            throw new AssertionError("10/3 has no exact decimal form");
        } catch (ArithmeticException expected) {
            System.out.println("divide          : " + expected.getMessage());
        }

        // ---- money ----------------------------------------------------------
        BigDecimal price = new BigDecimal("15.99");
        BigDecimal tax = price.multiply(new BigDecimal("0.08")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = price.add(tax);
        check(tax.equals(new BigDecimal("1.28")), "8% tax on 15.99 rounds to 1.28");
        check(total.equals(new BigDecimal("17.27")), "total is 17.27");
        System.out.println("money           : " + price + " + " + tax + " = " + total);

        double doubleTax = 15.99 * 0.08;
        check(doubleTax != 1.28, "the same maths in double is not exactly 1.28");
        System.out.println("same in double  : " + doubleTax);
        System.out.println("All checks passed.");
    }
}
