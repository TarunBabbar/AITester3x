import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * 59 - Functional composition: andThen/compose, predicate combinators, currying,
 * partial application, pipelines, memoization and validators.
 *
 * Compile and run:
 *   javac FunctionalComposition.java
 *   java FunctionalComposition
 */
public class FunctionalComposition {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /** A validator returns the problems it found; empty means the value is fine. */
    @FunctionalInterface
    interface Validator<T> {
        List<String> validate(T value);

        default Validator<T> and(Validator<T> other) {
            return value -> {
                List<String> problems = new ArrayList<>(this.validate(value));
                problems.addAll(other.validate(value));
                return problems;
            };
        }

        static <T> Validator<T> of(Predicate<T> rule, String message) {
            return value -> rule.test(value) ? List.of() : List.of(message);
        }
    }

    static final Function<Integer, Function<Integer, Integer>> CURRIED_ADD = a -> b -> a + b;

    /** Turn a two-argument function into one that takes the first argument now. */
    static <A, B, C> Function<B, C> partial(BiFunction<A, B, C> function, A first) {
        return second -> function.apply(first, second);
    }

    static <T> T applySteps(T start, List<Function<T, T>> steps) {
        T value = start;
        for (Function<T, T> step : steps) {
            value = step.apply(value);
        }
        return value;
    }

    static <A, B> Function<A, B> memoize(Function<A, B> function) {
        Map<A, B> cache = new HashMap<>();
        return input -> cache.computeIfAbsent(input, function);
    }

    static <T> Predicate<T> allOf(List<Predicate<T>> predicates) {
        return value -> predicates.stream().allMatch(predicate -> predicate.test(value));
    }

    static <T> Predicate<T> anyOf(List<Predicate<T>> predicates) {
        return value -> predicates.stream().anyMatch(predicate -> predicate.test(value));
    }

    @SafeVarargs
    static <T> UnaryOperator<T> pipeline(UnaryOperator<T>... steps) {
        return value -> applySteps(value, List.of(steps));
    }

    public static void main(String[] args) {
        // ---- composition -----------------------------------------------------
        Function<Integer, Integer> timesTwo = number -> number * 2;
        Function<Integer, Integer> plusOne = number -> number + 1;
        check(timesTwo.andThen(plusOne).apply(5) == 11, "andThen applies this function first");
        check(timesTwo.compose(plusOne).apply(5) == 12, "compose applies its argument first");
        check(Function.identity().apply("same").equals("same"), "identity changes nothing");
        System.out.println("andThen     : (5 * 2) + 1 = " + timesTwo.andThen(plusOne).apply(5));
        System.out.println("compose     : (5 + 1) * 2 = " + timesTwo.compose(plusOne).apply(5));

        // ---- predicate combinators -------------------------------------------
        Predicate<String> notEmpty = text -> !text.isEmpty();
        Predicate<String> shortWord = text -> text.length() <= 5;
        check(notEmpty.and(shortWord).test("java"), "and");
        check(notEmpty.or(shortWord).test(""), "or");
        check(notEmpty.negate().test(""), "negate");
        check(Predicate.not(notEmpty).test(""), "the static not() reads better than a lambda");
        check(allOf(List.of(notEmpty, shortWord)).test("ok"), "allOf");
        check(anyOf(List.of(notEmpty, shortWord)).test(""), "anyOf");

        // ---- currying and partial application --------------------------------
        check(CURRIED_ADD.apply(3).apply(4) == 7, "a curried function takes one argument at a time");
        Function<Integer, Integer> addTen = CURRIED_ADD.apply(10);
        check(addTen.apply(5) == 15, "the partially applied function is reusable");
        check(addTen.apply(-10) == 0, "and keeps working for other inputs");

        BiFunction<String, String, String> join = (left, right) -> left + "-" + right;
        Function<String, String> prefixed = partial(join, "pre");
        check(prefixed.apply("fix").equals("pre-fix"), "partial() fixes the first argument");
        System.out.println("partial     : " + prefixed.apply("fix"));

        // ---- pipelines -------------------------------------------------------
        List<Function<String, String>> steps = List.of(
                String::trim,
                String::toLowerCase,
                text -> text.replace(" ", "-"));
        check(applySteps("  Hello World  ", steps).equals("hello-world"), "steps run in order");
        System.out.println("pipeline    : " + applySteps("  Hello World  ", steps));

        UnaryOperator<Integer> scaled = pipeline(number -> number + 1, number -> number * 3);
        check(scaled.apply(4) == 15, "the varargs pipeline composes left to right");

        // ---- memoization -----------------------------------------------------
        AtomicInteger computations = new AtomicInteger();
        Function<Integer, Integer> square = memoize(number -> {
            computations.incrementAndGet();
            return number * number;
        });
        check(square.apply(4) == 16, "the first call computes");
        check(square.apply(4) == 16, "the second call is served from the cache");
        check(computations.get() == 1, "so the body ran exactly once");
        check(square.apply(5) == 25 && computations.get() == 2, "a new input computes again");
        System.out.println("memoize     : 3 calls, " + computations.get() + " computations");
        // Caveat: a cached null looks absent to computeIfAbsent, so it recomputes.

        // ---- a validator built from combinators ------------------------------
        Validator<String> usernameRules = Validator.<String>of(text -> !text.isBlank(), "must not be blank")
                .and(Validator.of(text -> text.length() >= 3, "needs 3 characters"))
                .and(Validator.of(text -> !text.contains(" "), "must not contain spaces"));

        check(usernameRules.validate("ada").isEmpty(), "a good username has no problems");
        check(usernameRules.validate("").equals(List.of("must not be blank", "needs 3 characters")),
                "two rules failed, reported in the order they were combined");
        check(usernameRules.validate("ada lovelace").equals(List.of("must not contain spaces")),
                "one rule failed");

        Validator<String> singleRule = Validator.of(text -> !text.isBlank(), "must not be blank");
        check(singleRule.validate("x").isEmpty(), "a single-rule validator passes a good value");
        check(singleRule.validate("   ").equals(List.of("must not be blank")),
                "and reports a blank one");
        System.out.println("validator   : " + usernameRules.validate("ab c"));

        // ---- reduce and filter with the combinators --------------------------
        List<Integer> numbers = List.of(1, 2, 3, 4, 5);
        check(numbers.stream().reduce(0, Integer::sum) == 15, "reduce with an identity");
        Optional<Integer> product = numbers.stream().reduce((left, right) -> left * right);
        check(product.orElse(0) == 120, "reduce without an identity returns an Optional");
        check(numbers.stream().filter(allOf(List.of(number -> number % 2 == 1))).count() == 3,
                "a composed predicate works as a filter");
        System.out.println("All checks passed.");
    }
}
