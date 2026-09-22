import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 21 - Design patterns: Builder, Strategy, Factory and Singleton, each kept
 * small enough to read in one go.
 *
 * Compile and run:
 *   javac DesignPatterns.java
 *   java DesignPatterns
 */
public class DesignPatterns {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    // ---------------------------------------------------------- 1. Builder
    static final class Pizza {
        private final String size;
        private final boolean cheese;
        private final List<String> toppings;

        private Pizza(Builder builder) {
            this.size = builder.size;
            this.cheese = builder.cheese;
            this.toppings = List.copyOf(builder.toppings);
        }

        @Override
        public String toString() {
            return size + " pizza" + (cheese ? " + cheese" : "") + " + " + toppings;
        }

        static final class Builder {
            private final String size;
            private boolean cheese;
            private final List<String> toppings = new ArrayList<>();

            Builder(String size) {
                this.size = size;
            }

            Builder cheese() {
                this.cheese = true;
                return this;
            }

            Builder topping(String topping) {
                this.toppings.add(topping);
                return this;
            }

            Pizza build() {
                return new Pizza(this);
            }
        }
    }

    // --------------------------------------------------------- 2. Strategy
    interface DiscountStrategy {
        double apply(double price);
    }

    enum Discount implements DiscountStrategy {
        NONE {
            @Override
            public double apply(double price) {
                return price;
            }
        },
        TEN_PERCENT {
            @Override
            public double apply(double price) {
                return price * 0.9;
            }
        },
        HALF {
            @Override
            public double apply(double price) {
                return price * 0.5;
            }
        }
    }

    static double checkout(double price, DiscountStrategy strategy) {
        return strategy.apply(price);
    }

    // ---------------------------------------------------------- 3. Factory
    interface Shape {
        double area();
    }

    record Circle(double radius) implements Shape {
        @Override
        public double area() {
            return Math.PI * radius * radius;
        }
    }

    record Square(double side) implements Shape {
        @Override
        public double area() {
            return side * side;
        }
    }

    static Shape shapeOf(String kind, double size) {
        return switch (kind.toLowerCase()) {
            case "circle" -> new Circle(size);
            case "square" -> new Square(size);
            default -> throw new IllegalArgumentException("unknown shape: " + kind);
        };
    }

    // -------------------------------------------------------- 4. Singleton
    static final class Registry {
        private final Map<String, Integer> counts = new HashMap<>();

        private Registry() {
            // private: only the holder below may construct it
        }

        private static final class Holder {
            private static final Registry INSTANCE = new Registry();
        }

        static Registry instance() {
            return Holder.INSTANCE;
        }

        Registry add(String key) {
            counts.merge(key, 1, Integer::sum);
            return this;
        }

        int count(String key) {
            return counts.getOrDefault(key, 0);
        }
    }

    public static void main(String[] args) {
        Pizza pizza = new Pizza.Builder("large").cheese().topping("olives").topping("ham").build();
        check(pizza.toString().equals("large pizza + cheese + [olives, ham]"), "builder assembles the object");
        System.out.println("builder  : " + pizza);

        check(checkout(200, Discount.NONE) == 200.0, "no discount");
        check(checkout(200, Discount.TEN_PERCENT) == 180.0, "ten percent off");
        check(checkout(200, Discount.HALF) == 100.0, "half price");
        System.out.printf("strategy : 200 -> %.2f (NONE), %.2f (TEN_PERCENT), %.2f (HALF)%n",
                checkout(200, Discount.NONE), checkout(200, Discount.TEN_PERCENT),
                checkout(200, Discount.HALF));

        check(Math.abs(shapeOf("circle", 1).area() - Math.PI) < 0.001, "factory builds a circle");
        check(shapeOf("square", 3).area() == 9.0, "factory builds a square");
        try {
            shapeOf("triangle", 1);
            throw new AssertionError("an unknown shape should fail");
        } catch (IllegalArgumentException expected) {
            System.out.println("factory  : caught " + expected.getMessage());
        }

        Registry first = Registry.instance().add("qa").add("qa").add("dev");
        Registry second = Registry.instance();
        check(first == second, "both lookups return the same instance");
        check(second.count("qa") == 2 && second.count("dev") == 1, "shared state is visible");
        System.out.println("singleton: qa=" + second.count("qa") + " dev=" + second.count("dev"));
        System.out.println("All checks passed.");
    }
}
