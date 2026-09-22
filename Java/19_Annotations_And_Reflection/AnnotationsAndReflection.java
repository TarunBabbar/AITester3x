import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 19 - Annotations and reflection: define a runtime annotation, then read it and
 * call the annotated methods without knowing their names at compile time.
 *
 * Compile and run:
 *   javac AnnotationsAndReflection.java
 *   java AnnotationsAndReflection
 */
public class AnnotationsAndReflection {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    @Retention(RetentionPolicy.RUNTIME)   // keep it in the class file
    @Target(ElementType.METHOD)           // only methods may carry it
    @interface TestCase {
        String id();

        String expected() default "PASS";

        int priority() default 1;
    }

    static class Calculator {
        int add(int a, int b) {
            return a + b;
        }

        @TestCase(id = "TC-01")
        int doubleValue(int n) {
            return n * 2;
        }

        @TestCase(id = "TC-02", expected = "Hello, Ada", priority = 2)
        String greet(String name) {
            return "Hello, " + name;
        }
    }

    /** Run every @TestCase method by feeding each parameter type a sample value. */
    static int runAnnotated(Calculator calculator) throws Exception {
        int run = 0;
        for (Method method : Calculator.class.getDeclaredMethods()) {
            TestCase testCase = method.getAnnotation(TestCase.class);
            if (testCase == null) {
                continue;
            }
            Object[] arguments = Arrays.stream(method.getParameterTypes())
                    .map(type -> type == int.class ? (Object) 3 : (Object) "Ada")
                    .toArray();
            Object result = method.invoke(calculator, arguments);
            System.out.printf("%-6s %-12s %-14s -> %s%n",
                    testCase.id(), method.getName(), Arrays.toString(arguments), result);
            run++;
        }
        return run;
    }

    public static void main(String[] args) throws Exception {
        Class<Calculator> type = Calculator.class;
        System.out.println("Class      : " + type.getName());
        System.out.println("Simple name: " + type.getSimpleName());
        check(type.getSimpleName().equals("Calculator"), "simple name");

        Calculator calculator = new Calculator();
        int run = runAnnotated(calculator);
        check(run == 2, "two methods carry @TestCase");

        Method add = type.getDeclaredMethod("add", int.class, int.class);
        check((int) add.invoke(calculator, 2, 3) == 5, "reflective call to add");
        check(add.getAnnotation(TestCase.class) == null, "add is not annotated");

        Method greet = type.getDeclaredMethod("greet", String.class);
        TestCase meta = greet.getAnnotation(TestCase.class);
        check(meta.id().equals("TC-02"), "annotation id read back");
        check(meta.expected().equals("Hello, Ada"), "explicit expected value");
        check(meta.priority() == 2, "explicit priority");
        check(add.getParameterCount() == 2, "add takes two parameters");

        Method doubleValue = type.getDeclaredMethod("doubleValue", int.class);
        check(doubleValue.getAnnotation(TestCase.class).priority() == 1, "default priority");

        System.out.println("Declared methods : " + type.getDeclaredMethods().length);
        System.out.println("All checks passed.");
    }
}
