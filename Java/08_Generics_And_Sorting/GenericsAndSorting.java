import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * 08 - Generics: generic classes, generic methods, bounds, Comparable and Comparator.
 *
 * Compile and run:
 *   javac GenericsAndSorting.java
 *   java GenericsAndSorting
 */
public class GenericsAndSorting {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static class Box<T> {
        private final T value;

        Box(T value) {
            this.value = value;
        }

        T value() {
            return value;
        }

        <R> Box<R> map(Function<T, R> mapper) {
            return new Box<>(mapper.apply(value));
        }

        @Override
        public String toString() {
            return "Box[" + value + "]";
        }
    }

    static <T extends Comparable<T>> T max(List<T> items) {
        if (items.isEmpty()) {
            throw new IllegalArgumentException("cannot take the max of an empty list");
        }
        T best = items.get(0);
        for (T item : items) {
            if (item.compareTo(best) > 0) {
                best = item;
            }
        }
        return best;
    }

    record Employee(String name, int salary) implements Comparable<Employee> {
        @Override
        public int compareTo(Employee other) {
            return Integer.compare(salary, other.salary);
        }
    }

    public static void main(String[] args) {
        Box<String> text = new Box<>("hello");
        check(text.value().equals("hello"), "box holds the string");
        check(text.map(String::length).value() == 5, "map transforms the contents");
        System.out.println(text + " -> " + text.map(String::toUpperCase));

        check(max(List.of(3, 9, 4)).equals(9), "max of integers");
        check(max(List.of("pear", "apple", "fig")).equals("pear"), "max of strings");

        List<Employee> staff = new ArrayList<>(List.of(
                new Employee("Alice", 90000),
                new Employee("Bob", 70000),
                new Employee("Carol", 105000)));
        check(max(staff).name().equals("Carol"), "highest paid is Carol");

        staff.sort(Comparator.naturalOrder());
        check(staff.get(0).name().equals("Bob"), "natural order sorts by salary");

        staff.sort(Comparator.comparing(Employee::name));
        check(staff.get(0).name().equals("Alice"), "sorted by name");

        staff.sort(Comparator.comparingInt(Employee::salary).reversed());
        check(staff.get(0).name().equals("Carol"), "reversed salary order");

        System.out.println("staff by salary desc : " + staff);
        System.out.println("All checks passed.");
    }
}
