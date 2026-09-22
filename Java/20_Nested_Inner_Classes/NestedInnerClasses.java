/**
 * 20 - Nested classes: static nested, inner (non-static), local, anonymous, and
 * how a lambda compares to an anonymous class.
 *
 * Compile and run:
 *   javac NestedInnerClasses.java
 *   java NestedInnerClasses
 */
public class NestedInnerClasses {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    // 1. static nested class: no link to an outer instance
    static class Config {
        private final String name;

        Config(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "Config[" + name + "]";
        }
    }

    // 2. inner class: every Audit knows which NestedInnerClasses made it
    private final String owner;
    private int audits = 0;

    NestedInnerClasses(String owner) {
        this.owner = owner;
    }

    class Audit {
        String stamp() {
            audits++;                       // mutates the OUTER instance
            return owner + " audit #" + audits;
        }
    }

    interface Greeter {
        String greet(String name);
    }

    // 3. local class: declared inside a method, visible only there
    static String viaLocalClass() {
        class Spanish implements Greeter {
            @Override
            public String greet(String name) {
                return "Hola, " + name;
            }
        }
        return new Spanish().greet("Ana");
    }

    // 4. anonymous class: one instance, no name
    static String viaAnonymousClass() {
        Greeter formal = new Greeter() {
            @Override
            public String greet(String name) {
                return "Good day, " + name;
            }
        };
        return formal.greet("Bond");
    }

    // 5. lambda: same contract, far less ceremony
    static String viaLambda() {
        Greeter casual = name -> "Hi " + name;
        return casual.greet("Sam");
    }

    public static void main(String[] args) {
        Config config = new Config("prod");
        check(config.toString().equals("Config[prod]"), "static nested class");
        System.out.println("static nested : " + config);

        NestedInnerClasses alice = new NestedInnerClasses("Alice");
        NestedInnerClasses.Audit first = alice.new Audit();   // note the syntax
        check(first.stamp().equals("Alice audit #1"), "inner class first stamp");
        check(first.stamp().equals("Alice audit #2"), "inner class second stamp");
        check(alice.audits == 2, "the inner class mutated the outer counter");

        NestedInnerClasses bob = new NestedInnerClasses("Bob");
        check(bob.new Audit().stamp().equals("Bob audit #1"), "a second outer instance is separate");
        check(alice.audits == 2, "Bob's audit did not touch Alice's counter");
        System.out.println("inner class   : " + bob.audits + " audit on Bob, " + alice.audits + " on Alice");

        check(viaLocalClass().equals("Hola, Ana"), "local class");
        check(viaAnonymousClass().equals("Good day, Bond"), "anonymous class");
        check(viaLambda().equals("Hi Sam"), "lambda");
        System.out.println("local         : " + viaLocalClass());
        System.out.println("anonymous     : " + viaAnonymousClass());
        System.out.println("lambda        : " + viaLambda());

        Greeter anonymous = new Greeter() {
            @Override
            public String greet(String name) {
                return "Dear " + name;
            }

            @Override
            public String toString() {
                return "an unnamed Greeter";
            }
        };
        check(anonymous.toString().equals("an unnamed Greeter"), "anonymous class can override Object methods");
        System.out.println("All checks passed.");
    }
}
