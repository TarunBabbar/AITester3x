import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 46 - Observer pattern: a custom listener interface on a test results board,
 * plus the standard library's PropertyChangeSupport for bean style events.
 *
 * Compile and run:
 *   javac ObserverPattern.java
 *   java ObserverPattern
 */
public class ObserverPattern {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    record TestEvent(String id, String status) {
    }

    @FunctionalInterface
    interface TestListener {
        void onResult(TestEvent event);
    }

    /** Publishes each result to every subscriber. */
    static final class ResultsBoard {
        private final List<TestListener> listeners = new ArrayList<>();
        private int passed;
        private int failed;

        void subscribe(TestListener listener) {
            listeners.add(listener);
        }

        void unsubscribe(TestListener listener) {
            listeners.remove(listener);
        }

        int subscriberCount() {
            return listeners.size();
        }

        void publish(String id, String status) {
            if (status.equals("pass")) {
                passed++;
            } else {
                failed++;
            }
            TestEvent event = new TestEvent(id, status);
            // Iterate a copy: a listener is allowed to unsubscribe while being notified.
            for (TestListener listener : List.copyOf(listeners)) {
                listener.onResult(event);
            }
        }

        int passed() {
            return passed;
        }

        int failed() {
            return failed;
        }
    }

    /** A bean that broadcasts old -> new values through PropertyChangeSupport. */
    static final class BuildStatus {
        private final PropertyChangeSupport support = new PropertyChangeSupport(this);
        private String status = "queued";

        void addListener(PropertyChangeListener listener) {
            support.addPropertyChangeListener(listener);
        }

        void removeListener(PropertyChangeListener listener) {
            support.removePropertyChangeListener(listener);
        }

        void setStatus(String next) {
            String previous = status;
            status = next;
            support.firePropertyChange("status", previous, next);
        }

        String status() {
            return status;
        }
    }

    public static void main(String[] args) {
        // ---- a custom listener interface -------------------------------------
        ResultsBoard board = new ResultsBoard();
        List<String> seen = new ArrayList<>();
        TestListener recorder = event -> seen.add(event.id() + ":" + event.status());
        Consumer<TestEvent> failureAlert = event -> {
            if (event.status().equals("fail")) {
                System.out.println("  alert: " + event.id() + " failed");
            }
        };

        board.subscribe(recorder);
        board.subscribe(failureAlert::accept);
        check(board.subscriberCount() == 2, "two subscribers");

        board.publish("TC-01", "pass");
        board.publish("TC-02", "fail");
        board.publish("TC-03", "pass");

        check(seen.equals(List.of("TC-01:pass", "TC-02:fail", "TC-03:pass")), "the recorder saw everything");
        check(board.passed() == 2 && board.failed() == 1, "the board keeps counts");
        System.out.println("recorded    : " + seen);

        // ---- unsubscribing stops the notifications ---------------------------
        board.unsubscribe(failureAlert::accept);   // a different lambda: nothing is removed
        check(board.subscriberCount() == 2, "a different lambda is not the same listener");
        board.unsubscribe(recorder);
        check(board.subscriberCount() == 1, "the recorder really was removed");

        int before = seen.size();
        board.publish("TC-04", "pass");
        check(seen.size() == before, "the removed listener is no longer called");
        check(board.passed() == 3, "the board still counted the result");
        System.out.println("after unsubscribe: " + seen.size() + " recorded events");

        // ---- the standard library's bean events ------------------------------
        BuildStatus build = new BuildStatus();
        List<String> transitions = new ArrayList<>();
        PropertyChangeListener watcher = (PropertyChangeEvent event) ->
                transitions.add(event.getOldValue() + " -> " + event.getNewValue());
        build.addListener(watcher);

        build.setStatus("running");
        build.setStatus("passed");
        check(build.status().equals("passed"), "the bean holds the new value");
        check(transitions.equals(List.of("queued -> running", "running -> passed")),
                "each change carries the old and new value");
        System.out.println("transitions : " + transitions);

        build.removeListener(watcher);
        build.setStatus("archived");
        check(transitions.size() == 2, "a removed PropertyChangeListener hears nothing");
        check(build.status().equals("archived"), "but the value still changed");
        System.out.println("All checks passed.");
    }
}
