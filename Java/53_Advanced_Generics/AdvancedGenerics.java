import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 53 - Advanced generics: wildcards and PECS, multiple bounds, recursive type
 * bounds, generic methods and what type erasure actually takes away.
 *
 * Compile and run:
 *   javac AdvancedGenerics.java
 *   java AdvancedGenerics
 */
public class AdvancedGenerics {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /**
     * PECS - Producer Extends, Consumer Super. The source produces values, the
     * target consumes them, and this signature lets List<Integer> fill a List<Number>.
     */
    static <T> void copy(List<? extends T> source, List<? super T> target) {
        for (T item : source) {
            target.add(item);
        }
    }

    /** Reading numbers: any list of a Number subtype works. */
    static double sum(List<? extends Number> numbers) {
        double total = 0;
        for (Number number : numbers) {
            total += number.doubleValue();
        }
        return total;
    }

    /** Recursive bound: T must be comparable to itself. */
    static <T extends Comparable<T>> T max(List<? extends T> items) {
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

    /** Multiple bounds: must be a Number AND comparable, so compareTo is callable. */
    static <T extends Number & Comparable<T>> T clamp(T value, T low, T high) {
        if (value.compareTo(low) < 0) {
            return low;
        }
        if (value.compareTo(high) > 0) {
            return high;
        }
        return value;
    }

    record Pair<A, B>(A first, B second) {
        <R> Pair<R, B> withFirst(R replacement) {
            return new Pair<>(replacement, second);
        }
    }

    /** A stack that only accepts one family of types. */
    static final class TypedStack<T> {
        private final List<T> items = new ArrayList<>();

        void push(T item) {
            items.add(item);
        }

        T pop() {
            if (items.isEmpty()) {
                throw new IllegalStateException("the stack is empty");
            }
            return items.remove(items.size() - 1);
        }

        int size() {
            return items.size();
        }
    }

    public static void main(String[] args) {
        // ---- PECS ------------------------------------------------------------
        List<Integer> integers = List.of(1, 2, 3);
        List<Number> numbers = new ArrayList<>();
        copy(integers, numbers);
        check(numbers.equals(List.of(1, 2, 3)), "List<Integer> fills a List<Number>");

        List<Object> objects = new ArrayList<>();
        copy(integers, objects);
        check(objects.size() == 3, "and a List<Object> too");
        System.out.println("pecs        : " + numbers);

        // The compiler forbids the reverse: you cannot ADD to a ? extends list,
        // and you cannot READ a typed value out of a ? super list. Those lines
        // would not compile, which is exactly the point of the rule.

        // ---- ? extends for reading -------------------------------------------
        check(sum(List.of(1, 2, 3)) == 6.0, "sum of integers");
        check(Math.abs(sum(List.of(1.5, 2.5)) - 4.0) < 0.001, "sum of doubles");
        check(sum(List.of(1L, 2L, 3L)) == 6.0, "sum of longs");
        System.out.println("sum         : " + sum(List.of(1, 2.5, 3L)));

        // ---- recursive bound --------------------------------------------------
        check(max(List.of(3, 9, 4)) == 9, "max of integers");
        check(max(List.of("pear", "apple", "fig")).equals("pear"), "max of strings");
        check(Collections.max(List.of(5, 1, 7)) == 7, "the standard library agrees");

        // ---- multiple bounds --------------------------------------------------
        check(clamp(5, 0, 3) == 3, "clamped above");
        check(clamp(-1, 0, 3) == 0, "clamped below");
        check(clamp(2, 0, 3) == 2, "left alone in range");
        check(clamp(2.5, 0.0, 2.0) == 2.0, "works for doubles too");

        // ---- generics in records and methods ---------------------------------
        Pair<String, Integer> pair = new Pair<>("score", 42);
        check(pair.first().equals("score") && pair.second() == 42, "record type parameters");
        check(pair.withFirst(1.5).first() == 1.5, "the method's own type parameter replaced A");
        check(pair.withFirst(true).second() == 42, "and B was preserved");
        System.out.println("pair        : " + pair + " -> " + pair.withFirst(1.5));

        // ---- a generic container ---------------------------------------------
        TypedStack<String> stack = new TypedStack<>();
        stack.push("a");
        stack.push("b");
        check(stack.pop().equals("b") && stack.size() == 1, "generic stack pops in order");
        try {
            new TypedStack<Integer>().pop();
            throw new AssertionError("popping an empty stack should fail");
        } catch (IllegalStateException expected) {
            System.out.println("empty stack : " + expected.getMessage());
        }

        // ---- type erasure -----------------------------------------------------
        List<String> strings = new ArrayList<>();
        List<Integer> ints = new ArrayList<>();
        check(strings.getClass() == ints.getClass(), "at runtime both are just ArrayList");
        check(strings.getClass().getTypeParameters().length == 1
                && strings.getClass().getTypeParameters()[0].getName().equals("E"),
                "only ArrayList's own E parameter remains: the String is gone");
        check(strings.equals(ints), "and two differently-typed empty lists are equal");
        System.out.println("erasure     : List<String> and List<Integer> are both "
                + strings.getClass().getSimpleName());
        System.out.println("All checks passed.");
    }
}
