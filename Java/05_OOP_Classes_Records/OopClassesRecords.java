import java.util.ArrayList;
import java.util.List;

/**
 * 05 - Object oriented Java: interfaces, abstract classes, records and polymorphism.
 *
 * Compile and run:
 *   javac OopClassesRecords.java
 *   java OopClassesRecords
 */
public class OopClassesRecords {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    interface Shape {
        double area();

        default String describe() {
            return "%s with area %.2f".formatted(getClass().getSimpleName(), area());
        }
    }

    record Point(double x, double y) {
    }

    static class Rectangle implements Shape {
        private final double width;
        private final double height;

        Rectangle(double width, double height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public double area() {
            return width * height;
        }
    }

    static class Circle implements Shape {
        private final double radius;

        Circle(double radius) {
            this.radius = radius;
        }

        @Override
        public double area() {
            return Math.PI * radius * radius;
        }
    }

    abstract static class Animal {
        private final String name;

        Animal(String name) {
            this.name = name;
        }

        String name() {
            return name;
        }

        abstract String sound();
    }

    static class Dog extends Animal {
        Dog(String name) {
            super(name);
        }

        @Override
        String sound() {
            return "woof";
        }
    }

    public static void main(String[] args) {
        List<Shape> shapes = new ArrayList<>(List.of(new Rectangle(3, 4), new Circle(1)));
        double totalArea = 0;
        for (Shape shape : shapes) {
            totalArea += shape.area();
        }
        check(Math.abs(shapes.get(0).area() - 12.0) < 0.001, "rectangle area is 12");
        check(Math.abs(shapes.get(1).area() - Math.PI) < 0.001, "circle area is pi");
        check(Math.abs(totalArea - (12 + Math.PI)) < 0.001, "total area adds up");

        Point point = new Point(2, 5);
        check(point.x() == 2 && point.y() == 5, "record accessors work");
        check(point.equals(new Point(2, 5)), "records compare by value");
        check(point.toString().equals("Point[x=2.0, y=5.0]"), "record toString is generated");

        Animal dog = new Dog("Rex");
        check(dog.sound().equals("woof"), "polymorphic sound");
        check(dog.name().equals("Rex"), "name is inherited from the abstract class");

        System.out.println(shapes.get(0).describe());
        System.out.println(shapes.get(1).describe());
        System.out.println(point + " and " + dog.name() + " says " + dog.sound());
        System.out.println("All checks passed.");
    }
}
