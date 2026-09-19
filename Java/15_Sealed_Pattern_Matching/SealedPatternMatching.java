/**
 * 15 - Sealed types and pattern matching: sealed interfaces, record patterns,
 * switch pattern matching with guards and instanceof patterns.
 *
 * Compile and run:
 *   javac SealedPatternMatching.java
 *   java SealedPatternMatching
 */
public class SealedPatternMatching {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    sealed interface Shape permits Circle, Rectangle, Square {
    }

    record Circle(double radius) implements Shape {
    }

    record Rectangle(double width, double height) implements Shape {
    }

    record Square(double side) implements Shape {
    }

    record Point(int x, int y) {
    }

    static double area(Shape shape) {
        // Exhaustive: no default needed because Shape is sealed.
        return switch (shape) {
            case Circle circle -> Math.PI * circle.radius() * circle.radius();
            case Rectangle rectangle -> rectangle.width() * rectangle.height();
            case Square square -> square.side() * square.side();
        };
    }

    static String classify(Object value) {
        return switch (value) {
            case Point(int x, int y) when x == y -> "diagonal point at " + x;   // record pattern + guard
            case Point(int x, int y) -> "point " + x + "," + y;
            case String text when text.isEmpty() -> "empty text";
            case String text -> "text of length " + text.length();
            case Integer number -> "number " + number;
            default -> "unknown";
        };
    }

    static String legacyInstanceOf(Object value) {
        if (value instanceof String text && text.length() > 3) {
            return "long text: " + text;
        }
        return "not a long text";
    }

    public static void main(String[] args) {
        Shape shape = new Rectangle(3, 4);
        check(Math.abs(area(shape) - 12.0) < 0.001, "rectangle area");
        check(Math.abs(area(new Circle(1)) - Math.PI) < 0.001, "circle area");
        check(area(new Square(5)) == 25.0, "square area");

        check(classify(new Point(4, 4)).equals("diagonal point at 4"), "record pattern with guard");
        check(classify(new Point(2, 7)).equals("point 2,7"), "record pattern");
        check(classify("").equals("empty text"), "empty string guard");
        check(classify("hello").equals("text of length 5"), "string case");
        check(classify(42).equals("number 42"), "integer case");
        check(classify(3.14).equals("unknown"), "default case");

        check(legacyInstanceOf("abcdef").equals("long text: abcdef"), "instanceof pattern");
        check(legacyInstanceOf("ab").equals("not a long text"), "instanceof pattern fails the test");

        for (Object value : new Object[] {new Point(1, 1), new Point(1, 2), "hi", 7}) {
            System.out.println(value + " -> " + classify(value));
        }
        System.out.printf("rectangle area : %.2f%n", area(new Rectangle(3, 4)));
        System.out.println("All checks passed.");
    }
}
