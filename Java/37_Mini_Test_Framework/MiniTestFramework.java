import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 37 - A mini test framework: @Test and @BeforeEach annotations, a small set of
 * assertions, and a reflection-based runner that reports PASS or FAIL per test.
 *
 * Compile and run:
 *   javac MiniTestFramework.java
 *   java MiniTestFramework
 */
public class MiniTestFramework {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Test {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface BeforeEach {
    }

    record Result(String name, boolean passed, String detail) {
    }

    /** The assertions a test may use. Each failure throws an AssertionError. */
    static final class Assert {
        private Assert() {
        }

        static void assertEquals(Object expected, Object actual) {
            if (!Objects.equals(expected, actual)) {
                throw new AssertionError("expected <" + expected + "> but was <" + actual + ">");
            }
        }

        static void assertTrue(boolean condition) {
            if (!condition) {
                throw new AssertionError("expected true but was false");
            }
        }

        static void assertThrows(Class<? extends Throwable> expected, Runnable body) {
            try {
                body.run();
            } catch (Throwable thrown) {
                if (expected.isInstance(thrown)) {
                    return;
                }
                throw new AssertionError("expected " + expected.getSimpleName()
                        + " but got " + thrown.getClass().getSimpleName());
            }
            throw new AssertionError("expected " + expected.getSimpleName()
                    + " but nothing was thrown");
        }
    }

    /** Run every @Test method, calling each @BeforeEach first. */
    static List<Result> run(Class<?> testClass) throws Exception {
        List<Method> before = new ArrayList<>();
        List<Method> tests = new ArrayList<>();
        for (Method method : testClass.getDeclaredMethods()) {
            if (method.getAnnotation(BeforeEach.class) != null) {
                before.add(method);
            }
            if (method.getAnnotation(Test.class) != null) {
                tests.add(method);
            }
        }

        List<Result> results = new ArrayList<>();
        for (Method test : tests) {
            Object instance = testClass.getDeclaredConstructor().newInstance();
            try {
                for (Method setup : before) {
                    setup.invoke(instance);
                }
                test.invoke(instance);
                results.add(new Result(test.getName(), true, ""));
            } catch (InvocationTargetException wrapped) {
                results.add(new Result(test.getName(), false, String.valueOf(wrapped.getCause())));
            }
        }
        return results;
    }

    // ------------------------------------------------------- the code on test
    static final class Calculator {
        private int value;

        void add(int amount) {
            value += amount;
        }

        int value() {
            return value;
        }
    }

    static final class CalculatorTest {
        private Calculator calculator;

        @BeforeEach
        void setUp() {
            calculator = new Calculator();
        }

        @Test
        void startsAtZero() {
            Assert.assertEquals(0, calculator.value());
        }

        @Test
        void addsUp() {
            calculator.add(3);
            calculator.add(4);
            Assert.assertEquals(7, calculator.value());
        }

        @Test
        void beforeEachRunsAgainForThisTest() {
            // If setUp did not re-run, this would still be 7.
            Assert.assertEquals(0, calculator.value());
        }

        @Test
        void assertsAnException() {
            int zero = Integer.parseInt("0");       // hides the zero from the compiler
            Assert.assertThrows(ArithmeticException.class, () -> {
                int ignored = 1 / zero;
            });
        }

        @Test
        void failsOnPurpose() {
            Assert.assertEquals(99, calculator.value());
        }

        void notAnnotatedSoItMustNotRun() {
            throw new IllegalStateException("the runner should have skipped this");
        }
    }

    public static void main(String[] args) throws Exception {
        List<Result> results = run(CalculatorTest.class);

        int passed = 0;
        for (Result result : results) {
            System.out.printf("%-6s %s%s%n", result.passed() ? "PASS" : "FAIL",
                    result.name(), result.passed() ? "" : "  <- " + result.detail());
            if (result.passed()) {
                passed++;
            }
        }

        check(results.size() == 5, "five @Test methods were collected");
        check(passed == 4, "four pass");
        check(results.stream().filter(r -> !r.passed()).count() == 1, "exactly one fails");
        check(results.stream().noneMatch(r -> r.name().equals("notAnnotatedSoItMustNotRun")),
                "an unannotated method is never run");
        check(results.stream().anyMatch(r -> r.name().equals("failsOnPurpose")
                && r.detail().contains("expected <99> but was <0>")), "the failure message is useful");

        System.out.printf("%n%d/%d passed, %d failed%n", passed, results.size(),
                results.size() - passed);
        System.out.println("All checks passed.");
    }
}
