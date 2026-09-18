/**
 * 07 - Methods: overloading, varargs, recursion and binary search.
 *
 * Compile and run:
 *   javac MethodsOverloadingRecursion.java
 *   java MethodsOverloadingRecursion
 */
public class MethodsOverloadingRecursion {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static int add(int a, int b) {
        return a + b;
    }

    static double add(double a, double b) {
        return a + b;
    }

    static String add(String a, String b) {
        return a + b;
    }

    static int sum(int... numbers) {
        int total = 0;
        for (int number : numbers) {
            total += number;
        }
        return total;
    }

    static long factorial(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("n must be >= 0, got " + n);
        }
        return n <= 1 ? 1 : n * factorial(n - 1);
    }

    static int fibonacci(int n) {
        return n < 2 ? n : fibonacci(n - 1) + fibonacci(n - 2);
    }

    static int binarySearch(int[] sorted, int target, int low, int high) {
        if (low > high) {
            return -1;
        }
        int middle = (low + high) >>> 1;
        if (sorted[middle] == target) {
            return middle;
        }
        return sorted[middle] < target
                ? binarySearch(sorted, target, middle + 1, high)
                : binarySearch(sorted, target, low, middle - 1);
    }

    public static void main(String[] args) {
        check(add(2, 3) == 5, "int overload");
        check(add(2.5, 3.5) == 6.0, "double overload");
        check(add("Ja", "va").equals("Java"), "String overload");
        check(sum() == 0 && sum(1, 2, 3, 4) == 10, "varargs");

        check(factorial(5) == 120, "5! = 120");
        check(factorial(0) == 1, "0! = 1");
        try {
            factorial(-1);
            throw new AssertionError("a negative factorial should fail");
        } catch (IllegalArgumentException expected) {
            System.out.println("Caught: " + expected.getMessage());
        }

        check(fibonacci(10) == 55, "fib(10) = 55");

        int[] sorted = {1, 3, 5, 7, 9, 11};
        check(binarySearch(sorted, 7, 0, sorted.length - 1) == 3, "index of 7 is 3");
        check(binarySearch(sorted, 4, 0, sorted.length - 1) == -1, "4 is not present");

        System.out.printf("overloads   : %d, %.1f, %s%n", add(2, 3), add(2.5, 3.5), add("Ja", "va"));
        System.out.printf("varargs sum : %d%n", sum(1, 2, 3, 4));
        System.out.printf("recursion   : 5! = %d, fib(10) = %d%n", factorial(5), fibonacci(10));
        System.out.println("All checks passed.");
    }
}
