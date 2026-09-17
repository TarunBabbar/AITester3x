/**
 * 02 - Control flow: if/else, switch expressions and loops.
 *
 * Compile and run:
 *   javac ControlFlowLoops.java
 *   java ControlFlowLoops
 */
public class ControlFlowLoops {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static String classify(int number) {
        return switch (number) {
            case 0 -> "zero";
            case 1, 2, 3 -> "small";
            default -> number < 0 ? "negative" : "large";
        };
    }

    static String fizzBuzz(int upTo) {
        StringBuilder builder = new StringBuilder();
        for (int i = 1; i <= upTo; i++) {
            if (i % 15 == 0) {
                builder.append("FizzBuzz ");
            } else if (i % 3 == 0) {
                builder.append("Fizz ");
            } else if (i % 5 == 0) {
                builder.append("Buzz ");
            } else {
                builder.append(i).append(' ');
            }
        }
        return builder.toString().trim();
    }

    public static void main(String[] args) {
        check(classify(0).equals("zero"), "0 is zero");
        check(classify(2).equals("small"), "2 is small");
        check(classify(9).equals("large"), "9 is large");
        check(classify(-4).equals("negative"), "-4 is negative");

        int sum = 0;
        for (int i = 1; i <= 10; i++) {
            sum += i;
        }
        check(sum == 55, "sum 1..10 is 55");

        int[] numbers = {4, 7, 2, 9, 6};
        int oddSum = 0;
        for (int number : numbers) {
            if (number % 2 == 0) {
                continue; // skip even numbers
            }
            oddSum += number;
        }
        check(oddSum == 16, "odd numbers 7 + 9 = 16");

        int countdown = 3;
        StringBuilder ticks = new StringBuilder();
        while (countdown > 0) {
            ticks.append(countdown).append(' ');
            countdown--;
        }
        check(ticks.toString().trim().equals("3 2 1"), "countdown is 3 2 1");

        String fizz = fizzBuzz(15);
        System.out.println("FizzBuzz 1..15 : " + fizz);
        check(fizz.startsWith("1 2 Fizz 4 Buzz Fizz 7"), "FizzBuzz starts correctly");
        check(fizz.endsWith("14 FizzBuzz"), "FizzBuzz ends correctly");

        System.out.printf("classify(9)=%s sum=%d oddSum=%d%n", classify(9), sum, oddSum);
        System.out.println("All checks passed.");
    }
}
