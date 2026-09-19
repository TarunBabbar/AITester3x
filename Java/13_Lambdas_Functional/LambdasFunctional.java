import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 13 - Lambdas and functional interfaces: custom @FunctionalInterface,
 * the java.util.function types, composition and method references.
 *
 * Compile and run:
 *   javac LambdasFunctional.java
 *   java LambdasFunctional
 */
public class LambdasFunctional {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    @FunctionalInterface
    interface Calculator {
        int apply(int a, int b);

        default Calculator andThen(Calculator next) {
            return (a, b) -> next.apply(this.apply(a, b), b);
        }
    }

    record Person(String name, int age) {
    }

    public static void main(String[] args) {
        Calculator add = (a, b) -> a + b;
        Calculator multiply = (a, b) -> a * b;
        check(add.apply(2, 3) == 5, "lambda addition");
        check(add.andThen(multiply).apply(2, 3) == 15, "(2 + 3) * 3 = 15");

        Function<Integer, Integer> square = n -> n * n;
        Function<Integer, String> label = n -> "n=" + n;
        check(square.andThen(label).apply(4).equals("n=16"), "function composition");

        Predicate<String> notEmpty = text -> !text.isEmpty();
        Predicate<String> shortWord = text -> text.length() <= 5;
        check(notEmpty.and(shortWord).test("java"), "predicate and");
        check(notEmpty.and(shortWord).negate().test("javascript"), "predicate negate");
        check(notEmpty.or(text -> text.equals("")).test(""), "predicate or");

        BiFunction<Integer, Integer, Integer> larger = Math::max;
        check(larger.apply(3, 9) == 9, "method reference to a static method");

        Supplier<List<String>> newList = ArrayList::new;
        check(newList.get().isEmpty(), "constructor reference makes a new list");

        StringBuilder seen = new StringBuilder();
        Consumer<String> collector = text -> seen.append(text).append(';');
        List.of("a", "b", "c").forEach(collector);
        check(seen.toString().equals("a;b;c;"), "consumer side effects run forEach");

        int bonus = 10;
        Function<Integer, Integer> addBonus = value -> value + bonus; // captures a local
        check(addBonus.apply(5) == 15, "lambda closes over an effectively final local");

        List<Person> people = List.of(
                new Person("Alice", 34),
                new Person("Bob", 17),
                new Person("Carol", 29));
        List<String> adults = people.stream()
                .filter(person -> person.age() >= 18)
                .map(Person::name)
                .sorted()
                .toList();
        check(adults.equals(List.of("Alice", "Carol")), "filter and map over records");

        System.out.println("add.andThen(multiply)(2, 3) : " + add.andThen(multiply).apply(2, 3));
        System.out.println("square.andThen(label)(4)    : " + square.andThen(label).apply(4));
        System.out.println("adults                      : " + adults);
        System.out.println("All checks passed.");
    }
}
