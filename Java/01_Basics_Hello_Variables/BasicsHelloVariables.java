/**
 * 01 - Basics: hello world, variables and primitive types.
 *
 * Compile and run:
 *   javac BasicsHelloVariables.java
 *   java BasicsHelloVariables
 */
public class BasicsHelloVariables {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) {
        String name = "Java learner";
        int copies = 3;
        double price = 12.5;
        boolean inStock = true;
        char grade = 'A';
        var total = copies * price; // var infers double

        System.out.println("Hello, " + name + "!");
        System.out.printf("copies=%d price=%.2f total=%.2f%n", copies, price, total);
        System.out.printf("inStock=%b grade=%c%n", inStock, grade);

        check(total == 37.5, "total should be 37.5");
        check(inStock, "inStock should be true");
        check(name.length() == 12, "name should have 12 characters");

        System.out.printf("integer division 7 / 2 = %d, remainder = %d%n", 7 / 2, 7 % 2);
        check(7 / 2 == 3 && 7 % 2 == 1, "integer division truncates");
        System.out.println("All checks passed.");
    }
}
