import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 58 - Design patterns part two: Command with undo, Decorator, Template Method
 * and Chain of Responsibility.
 *
 * Compile and run:
 *   javac DesignPatterns2.java
 *   java DesignPatterns2
 */
public class DesignPatterns2 {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    // ------------------------------------------------------------- 1. Command
    interface Command {
        String execute();
    }

    static final class AddLine implements Command {
        private final List<String> document;
        private final String line;

        AddLine(List<String> document, String line) {
            this.document = document;
            this.line = line;
        }

        @Override
        public String execute() {
            document.add(line);
            return "added \"" + line + "\"";
        }
    }

    static final class ClearDocument implements Command {
        private final List<String> document;
        private final List<String> removed = new ArrayList<>();

        ClearDocument(List<String> document) {
            this.document = document;
        }

        @Override
        public String execute() {
            removed.addAll(document);
            document.clear();
            return "cleared " + removed.size() + " line(s)";
        }

        String undo() {
            document.addAll(removed);
            return "restored " + removed.size() + " line(s)";
        }
    }

    /** A command made of other commands. */
    record Macro(List<Command> steps) implements Command {
        Macro(Command... steps) {
            this(List.of(steps));
        }

        @Override
        public String execute() {
            return steps.stream().map(Command::execute).collect(Collectors.joining(", "));
        }
    }

    // ----------------------------------------------------------- 2. Decorator
    interface Coffee {
        String description();

        double cost();
    }

    record Espresso() implements Coffee {
        @Override
        public String description() {
            return "espresso";
        }

        @Override
        public double cost() {
            return 2.0;
        }
    }

    record WithMilk(Coffee base) implements Coffee {
        @Override
        public String description() {
            return base.description() + " + milk";
        }

        @Override
        public double cost() {
            return base.cost() + 0.5;
        }
    }

    record WithSugar(Coffee base) implements Coffee {
        @Override
        public String description() {
            return base.description() + " + sugar";
        }

        @Override
        public double cost() {
            return base.cost() + 0.2;
        }
    }

    // ------------------------------------------------------- 3. Template Method
    abstract static class TestRun {
        /** The template: the order is fixed, the steps are not. */
        final String run() {
            return setup() + " -> " + execute() + " -> " + verify();
        }

        abstract String setup();

        abstract String execute();

        /** A hook with a default the subclass may ignore. */
        String verify() {
            return "verified by the default hook";
        }
    }

    static final class SmokeRun extends TestRun {
        @Override
        String setup() {
            return "browser up";
        }

        @Override
        String execute() {
            return "run 3 smoke cases";
        }
    }

    static final class ApiRun extends TestRun {
        @Override
        String setup() {
            return "token fetched";
        }

        @Override
        String execute() {
            return "call 5 endpoints";
        }

        @Override
        String verify() {
            return "asserted 5 status codes";
        }
    }

    // --------------------------------------------- 4. Chain of Responsibility
    abstract static class Handler {
        private Handler next;

        Handler then(Handler follower) {
            this.next = follower;
            return follower;
        }

        final String handle(int level, String message) {
            if (canHandle(level)) {
                return name() + " handled: " + message;
            }
            return next == null
                    ? "unhandled at level " + level + ": " + message
                    : next.handle(level, message);
        }

        abstract boolean canHandle(int level);

        abstract String name();
    }

    static final class DebugHandler extends Handler {
        @Override
        boolean canHandle(int level) {
            return level <= 10;
        }

        @Override
        String name() {
            return "debug";
        }
    }

    static final class InfoHandler extends Handler {
        @Override
        boolean canHandle(int level) {
            return level <= 20;
        }

        @Override
        String name() {
            return "info";
        }
    }

    static final class ErrorHandler extends Handler {
        @Override
        boolean canHandle(int level) {
            return level <= 40;
        }

        @Override
        String name() {
            return "error";
        }
    }

    public static void main(String[] args) {
        // ---- command ---------------------------------------------------------
        List<String> document = new ArrayList<>();
        AddLine first = new AddLine(document, "first line");
        AddLine second = new AddLine(document, "second line");
        ClearDocument clear = new ClearDocument(document);

        check(first.execute().equals("added \"first line\""), "the command reports what it did");
        check(document.equals(List.of("first line")), "and it really did it");

        Macro macro = new Macro(second, clear);
        check(macro.execute().equals("added \"second line\", cleared 2 line(s)"),
                "a macro runs its steps in order");
        check(document.isEmpty(), "the document is empty afterwards");
        check(clear.undo().equals("restored 2 line(s)"), "the command can undo itself");
        check(document.equals(List.of("first line", "second line")), "the undo restored both lines");
        System.out.println("command     : " + document);

        // ---- decorator --------------------------------------------------------
        Coffee plain = new Espresso();
        Coffee milky = new WithMilk(plain);
        Coffee fancy = new WithSugar(new WithMilk(plain));

        check(plain.description().equals("espresso") && plain.cost() == 2.0, "plain coffee");
        check(milky.description().equals("espresso + milk")
                && Math.abs(milky.cost() - 2.5) < 0.001, "one decorator");
        check(fancy.description().equals("espresso + milk + sugar"), "decorators stack");
        check(Math.abs(fancy.cost() - 2.7) < 0.001, "and their costs add up");
        check(new WithSugar(new Espresso()).description().equals("espresso + sugar"),
                "the order of decoration is visible in the description");
        System.out.println("decorator   : " + fancy.description() + " = " + fancy.cost());

        // ---- template method --------------------------------------------------
        check(new SmokeRun().run().equals(
                "browser up -> run 3 smoke cases -> verified by the default hook"),
                "the default hook is used when the subclass says nothing");
        check(new ApiRun().run().equals(
                "token fetched -> call 5 endpoints -> asserted 5 status codes"),
                "the subclass may replace the hook");
        System.out.println("template    : " + new ApiRun().run());

        // ---- chain of responsibility ------------------------------------------
        DebugHandler chain = new DebugHandler();
        chain.then(new InfoHandler()).then(new ErrorHandler());

        check(chain.handle(5, "trace").equals("debug handled: trace"),
                "the first handler that can, does");
        check(chain.handle(15, "deploy finished").equals("info handled: deploy finished"),
                "otherwise the chain passes it along");
        check(chain.handle(35, "disk full").equals("error handled: disk full"),
                "further down the chain");
        check(chain.handle(99, "cosmic ray").startsWith("unhandled at level 99"),
                "and a short chain reports what nobody handled");
        System.out.println("chain       : " + chain.handle(35, "disk full"));
        System.out.println("All checks passed.");
    }
}
