import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

/**
 * 70 - Property-based testing: instead of picking examples, state a rule that
 * must hold for every input and hunt for a counterexample with a seeded
 * generator. Includes reproducibility and a simple shrinker.
 *
 * Compile and run:
 *   javac PropertyTesting.java
 *   java PropertyTesting
 */
public class PropertyTesting {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /** Returns null when the input is fine, or a description of the failure. */
    @FunctionalInterface
    interface Property<T> {
        String check(T input);
    }

    @FunctionalInterface
    interface Generator<T> {
        T generate(Random random);
    }

    record Counterexample(String property, long seed, int caseNumber, String input, String detail) {
        @Override
        public String toString() {
            return property + " failed on case " + caseNumber
                    + " with \"" + input + "\" (" + detail + ", seed " + seed + ")";
        }
    }

    /** Try random inputs until one breaks the property, or the budget runs out. */
    static <T> Counterexample search(String name, long seed, int cases,
                                     Generator<T> generator, Property<T> property) {
        Random random = new Random(seed);
        for (int number = 1; number <= cases; number++) {
            T input = generator.generate(random);
            String failure = property.check(input);
            if (failure != null) {
                return new Counterexample(name, seed, number, String.valueOf(input), failure);
            }
        }
        return null;
    }

    // ------------------------------------------------------------- generators
    static String randomText(Random random) {
        int length = random.nextInt(13);
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < length; i++) {
            text.append((char) ('a' + random.nextInt(26)));
        }
        return text.toString();
    }

    static List<Integer> randomNumbers(Random random) {
        int size = random.nextInt(9);
        List<Integer> numbers = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            numbers.add(random.nextInt(101) - 50);
        }
        return numbers;
    }

    /** Runs of the same character, which is what makes run-length coding interesting. */
    static String randomRuns(Random random) {
        int runs = random.nextInt(5);
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < runs; i++) {
            char value = (char) ('a' + random.nextInt(4));
            int repeat = 1 + random.nextInt(4);
            text.append(String.valueOf(value).repeat(repeat));
        }
        return text.toString();
    }

    // ------------------------------------------------- run-length coding
    static String encode(String text) {
        StringBuilder out = new StringBuilder();
        int index = 0;
        while (index < text.length()) {
            char current = text.charAt(index);
            int count = 0;
            while (index < text.length() && text.charAt(index) == current && count < 9) {
                count++;
                index++;
            }
            out.append(count).append(current);
        }
        return out.toString();
    }

    static String decode(String encoded) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i + 1 < encoded.length(); i += 2) {
            int count = encoded.charAt(i) - '0';
            char value = encoded.charAt(i + 1);
            out.append(String.valueOf(value).repeat(count));
        }
        return out.toString();
    }

    static String reverse(String text) {
        return new StringBuilder(text).reverse().toString();
    }

    /** Remove characters while the failure survives, to get the smallest example. */
    static String shrink(String failing, Predicate<String> stillFails) {
        String current = failing;
        boolean changed = true;
        while (changed) {
            changed = false;
            if (current.length() > 1) {
                String withoutFirst = current.substring(1);
                if (stillFails.test(withoutFirst)) {
                    current = withoutFirst;
                    changed = true;
                    continue;
                }
                String withoutLast = current.substring(0, current.length() - 1);
                if (stillFails.test(withoutLast)) {
                    current = withoutLast;
                    changed = true;
                }
            }
        }
        return current;
    }

    public static void main(String[] args) {
        // ---- properties that should hold -------------------------------------
        check(search("reverse twice", 42, 500, PropertyTesting::randomText,
                text -> reverse(reverse(text)).equals(text) ? null : "not the original")
                == null, "reversing twice returns the original");

        check(search("concatenation length", 7, 500, PropertyTesting::randomText,
                text -> (text + text).length() == 2 * text.length() ? null : "lengths differ")
                == null, "concatenation adds lengths");

        check(search("sort is ordered", 11, 500, PropertyTesting::randomNumbers,
                numbers -> {
                    List<Integer> sorted = new ArrayList<>(numbers);
                    Collections.sort(sorted);
                    for (int i = 1; i < sorted.size(); i++) {
                        if (sorted.get(i - 1) > sorted.get(i)) {
                            return "out of order at " + i;
                        }
                    }
                    List<Integer> twice = new ArrayList<>(sorted);
                    Collections.sort(twice);
                    return twice.equals(sorted) ? null : "not idempotent";
                }) == null, "sorting is ordered and idempotent");

        check(search("absolute value", 3, 300,
                random -> random.nextInt(1001) - 500,
                value -> Math.abs(value) >= 0 ? null : "negative result")
                == null, "abs is never negative");

        check(search("run-length round trip", 5, 500, PropertyTesting::randomRuns,
                text -> decode(encode(text)).equals(text) ? null : "round trip lost data")
                == null, "the encoder and decoder agree on every generated string");

        check(search("run-length shrinks", 5, 500, PropertyTesting::randomRuns,
                text -> text.isEmpty() || encode(text).length() <= 2 * text.length()
                        ? null : "encoding grew out of proportion")
                == null, "encoding never doubles the text");
        System.out.println("held        : 6 properties, thousands of random cases");

        // ---- a property that does not hold -----------------------------------
        Property<String> atMostFive = text -> text.length() <= 5
                ? null
                : "length " + text.length() + " exceeds five";
        Counterexample failure = search("at most five characters", 99, 200,
                PropertyTesting::randomText, atMostFive);

        check(failure != null, "the property is falsified");
        check(failure.seed() == 99, "the seed is recorded so it can be replayed");
        check(failure.input().length() > 5, "the counterexample really is longer than five");
        System.out.println("falsified   : " + failure);

        // ---- the same seed finds the same counterexample ---------------------
        Counterexample replay = search("at most five characters", 99, 200,
                PropertyTesting::randomText, atMostFive);
        check(replay.equals(failure), "the same seed reproduces the failure exactly");
        Counterexample different = search("at most five characters", 100, 200,
                PropertyTesting::randomText, atMostFive);
        check(!different.input().equals(failure.input()), "a different seed finds a different input");
        System.out.println("replayed    : case " + replay.caseNumber() + " with \"" + replay.input() + "\"");

        // ---- shrinking --------------------------------------------------------
        String shrunk = shrink(failure.input(), candidate -> candidate.length() > 5);
        check(shrunk.length() == 6, "shrinking reaches the smallest failing length");
        check(failure.input().contains(shrunk), "the shrunk case is a slice of the original");
        System.out.println("shrunk      : \"" + failure.input() + "\" -> \"" + shrunk + "\"");
        System.out.println("All checks passed.");
    }
}
