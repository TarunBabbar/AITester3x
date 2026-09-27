import java.io.Closeable;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * 52 - Exception diagnostics: chained causes, suppressed exceptions from
 * try-with-resources, custom hierarchies, stack frames and StackWalker.
 *
 * Compile and run:
 *   javac ExceptionDiagnostics.java
 *   java ExceptionDiagnostics
 */
public class ExceptionDiagnostics {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static class TestDataException extends Exception {
        TestDataException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /** A resource that records what it did, and can fail on close. */
    static final class Audited implements Closeable {
        private final String name;
        private final boolean failOnClose;
        private final List<String> log;

        Audited(String name, boolean failOnClose, List<String> log) {
            this.name = name;
            this.failOnClose = failOnClose;
            this.log = log;
            log.add("open " + name);
        }

        void work(boolean fail) throws IOException {
            log.add("work " + name);
            if (fail) {
                throw new IOException("work failed in " + name);
            }
        }

        @Override
        public void close() throws IOException {
            log.add("close " + name);
            if (failOnClose) {
                throw new IOException("close failed in " + name);
            }
        }
    }

    static List<String> describeChain(Throwable throwable) {
        List<String> chain = new ArrayList<>();
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            chain.add(current.getClass().getSimpleName() + ": " + current.getMessage());
        }
        return chain;
    }

    /** Walk the current stack. Called from main, it sees its own frame first. */
    static List<String> captureStack() {
        return StackWalker.getInstance().walk(frames -> frames
                .map(StackWalker.StackFrame::getMethodName)
                .toList());
    }

    public static void main(String[] args) {
        // ---- chained causes --------------------------------------------------
        Exception chained = new TestDataException("could not load issue VWO-49",
                new IOException("connection reset",
                        new UnknownHostException("jira.example")));
        check(chained.getCause() != null, "the direct cause is present");
        check(chained.getCause().getCause() instanceof UnknownHostException,
                "the root cause is two levels down");
        check(describeChain(chained).equals(List.of(
                "TestDataException: could not load issue VWO-49",
                "IOException: connection reset",
                "UnknownHostException: jira.example")), "the whole chain reads cleanly");
        System.out.println("chain       : " + String.join(" -> ", describeChain(chained)));

        // ---- suppressed exceptions from try-with-resources -------------------
        List<String> log = new ArrayList<>();
        IOException primary = null;
        try {
            try (Audited first = new Audited("first", true, log);
                 Audited second = new Audited("second", true, log)) {
                first.work(true);                     // this is the primary failure
            }
        } catch (IOException thrown) {
            primary = thrown;
        }

        check(primary != null && primary.getMessage().equals("work failed in first"),
                "the body's exception is the primary one");
        check(primary.getSuppressed().length == 2, "both close failures are attached as suppressed");
        check(primary.getSuppressed()[0].getMessage().equals("close failed in second"),
                "resources close in reverse order, so second closes first");
        check(primary.getSuppressed()[1].getMessage().equals("close failed in first"),
                "and first closes last");
        check(log.equals(List.of("open first", "open second", "work first",
                "close second", "close first")), "the log shows the real order");
        System.out.println("primary     : " + primary);
        for (Throwable suppressed : primary.getSuppressed()) {
            System.out.println("  suppressed: " + suppressed.getMessage());
        }

        // ---- a close failure alone is just an exception ----------------------
        try (Audited quiet = new Audited("quiet", true, new ArrayList<>())) {
            quiet.work(false);
        } catch (IOException expected) {
            check(expected.getSuppressed().length == 0, "with no primary failure there is nothing to suppress");
            System.out.println("close only  : " + expected.getMessage());
        }

        // ---- custom hierarchy -------------------------------------------------
        try {
            throw new TestDataException("missing fixture", null);
        } catch (TestDataException expected) {
            check(expected.getCause() == null, "a null cause is allowed");
            check(expected instanceof Exception, "it is a checked exception");
        }

        // ---- stack frames -----------------------------------------------------
        try {
            new Audited("probe", false, new ArrayList<>()).work(true);
        } catch (IOException error) {
            StackTraceElement top = error.getStackTrace()[0];
            check(top.getMethodName().equals("work"), "the top frame is where it was thrown");
            check(top.getClassName().endsWith("ExceptionDiagnostics$Audited"), "the declaring class");
            check(top.getLineNumber() > 0, "a real line number is recorded");
            System.out.println("top frame   : " + top);
        }

        // ---- StackWalker ------------------------------------------------------
        List<String> methods = captureStack();
        check(methods.get(0).equals("captureStack"),
                "the innermost frame is the method that did the walking");
        check(methods.contains("main"), "the walker reaches main");
        check(methods.size() >= 2, "there is more than one frame in the stack");
        check(methods.stream().noneMatch(name -> name.contains("StackWalker")),
                "it hides its own implementation frames");
        System.out.println("walker      : " + methods);
        System.out.println("All checks passed.");
    }
}
