import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/**
 * 81 - Resilience patterns: a circuit breaker that fails fast while a
 * dependency is down, and retries with exponential backoff plus jitter.
 *
 * Compile and run:
 *   javac CircuitBreaker.java
 *   java CircuitBreaker
 */
public class CircuitBreaker {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    enum State {
        CLOSED, OPEN, HALF_OPEN
    }

    /** Thrown instead of calling the dependency at all. */
    static class CircuitOpenException extends RuntimeException {
        CircuitOpenException(String message) {
            super(message);
        }
    }

    @FunctionalInterface
    interface Operation<T> {
        T run() throws Exception;
    }

    static final class Breaker {
        private final int failureThreshold;
        private final int successThreshold;
        private final long cooldownMillis;
        private final LongSupplier clock;

        private State state = State.CLOSED;
        private int failures;
        private int successesInHalfOpen;
        private long openedAt;

        Breaker(int failureThreshold, int successThreshold, long cooldownMillis, LongSupplier clock) {
            this.failureThreshold = failureThreshold;
            this.successThreshold = successThreshold;
            this.cooldownMillis = cooldownMillis;
            this.clock = clock;
        }

        <T> T call(Operation<T> operation) throws Exception {
            if (state == State.OPEN) {
                if (clock.getAsLong() - openedAt < cooldownMillis) {
                    throw new CircuitOpenException("circuit is open, not calling the dependency");
                }
                state = State.HALF_OPEN;      // one trial call is allowed through
                successesInHalfOpen = 0;
            }
            try {
                T result = operation.run();
                onSuccess();
                return result;
            } catch (Exception failure) {
                onFailure();
                throw failure;
            }
        }

        private void onSuccess() {
            if (state == State.HALF_OPEN) {
                successesInHalfOpen++;
                if (successesInHalfOpen >= successThreshold) {
                    state = State.CLOSED;
                    failures = 0;
                }
            } else {
                failures = 0;                 // a healthy call wipes the slate
            }
        }

        private void onFailure() {
            failures++;
            if (state == State.HALF_OPEN || failures >= failureThreshold) {
                state = State.OPEN;           // one bad trial is enough to give up again
                openedAt = clock.getAsLong();
            }
        }

        State state() {
            return state;
        }

        int failures() {
            return failures;
        }
    }

    // --------------------------------------------------- retry with backoff
    record RetryOutcome<T>(T value, int attempts, List<Long> delays) {
    }

    /**
     * Retries a failing call with exponentially growing waits, each with a bit of
     * jitter so that many callers do not retry in lockstep. The waits are recorded
     * rather than slept through, which keeps the test instant.
     */
    static <T> RetryOutcome<T> retry(Operation<T> operation, int maxAttempts,
                                     long baseMillis, Random jitter) throws Exception {
        List<Long> delays = new ArrayList<>();
        Exception last = null;
        long delay = baseMillis;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return new RetryOutcome<>(operation.run(), attempt, delays);
            } catch (Exception failure) {
                last = failure;
                if (attempt < maxAttempts) {
                    long wait = delay + jitter.nextInt((int) Math.max(1, delay / 2) + 1);
                    delays.add(wait);
                    delay *= 2;
                }
            }
        }
        throw last;
    }

    public static void main(String[] args) throws Exception {
        // ---- the breaker opens after repeated failures ------------------------
        AtomicLong clock = new AtomicLong(1_000);
        Breaker breaker = new Breaker(3, 2, 1_000, clock::get);
        AtomicLong calls = new AtomicLong();

        Operation<String> alwaysFails = () -> {
            calls.incrementAndGet();
            throw new IllegalStateException("dependency is down");
        };

        check(breaker.state() == State.CLOSED, "a new breaker is closed");

        for (int i = 1; i <= 3; i++) {
            try {
                breaker.call(alwaysFails);
                throw new AssertionError("the call should have failed");
            } catch (IllegalStateException expected) {
                // the caller still sees the real failure while the breaker is closed
            }
        }
        check(calls.get() == 3, "three calls reached the dependency");
        check(breaker.state() == State.OPEN, "and the breaker opened");
        System.out.println("opened      : after 3 failures");

        try {
            breaker.call(alwaysFails);
            throw new AssertionError("an open breaker should refuse");
        } catch (CircuitOpenException expected) {
            check(calls.get() == 3, "the dependency was NOT called again");
            System.out.println("fail fast   : " + expected.getMessage());
        }

        // ---- the cooldown must elapse before a trial -------------------------
        clock.addAndGet(500);
        try {
            breaker.call(alwaysFails);
            throw new AssertionError("still inside the cooldown");
        } catch (CircuitOpenException expected) {
            check(calls.get() == 3, "no call during the cooldown");
        }

        clock.addAndGet(500);                 // cooldown complete
        try {
            breaker.call(alwaysFails);
            throw new AssertionError("the trial call should have failed");
        } catch (IllegalStateException expected) {
            check(calls.get() == 4, "the half-open trial did reach the dependency");
            check(breaker.state() == State.OPEN, "and one bad trial reopens it");
        }
        System.out.println("half open   : a failed trial reopens immediately");

        // ---- recovery needs consecutive successes ----------------------------
        clock.addAndGet(1_000);
        Operation<String> succeeds = () -> {
            calls.incrementAndGet();
            return "ok";
        };

        check(breaker.call(succeeds).equals("ok"), "the first trial succeeds");
        check(breaker.state() == State.HALF_OPEN,
                "one success is not enough with a threshold of two");
        check(breaker.call(succeeds).equals("ok"), "the second trial succeeds");
        check(breaker.state() == State.CLOSED, "and now the breaker closes");
        check(calls.get() == 6, "four failures and two successes reached the dependency");
        System.out.println("recovered   : closed again after 2 consecutive successes");

        // ---- a healthy call resets the failure count -------------------------
        Operation<String> failOnce = () -> {
            throw new IllegalStateException("blip");
        };
        try {
            breaker.call(failOnce);
        } catch (IllegalStateException expected) {
            check(breaker.failures() == 1, "one failure recorded");
        }
        breaker.call(succeeds);
        check(breaker.failures() == 0, "a success clears the count, so two blips never trip it");
        try {
            breaker.call(failOnce);
        } catch (IllegalStateException expected) {
            check(breaker.failures() == 1, "back to one, not two");
        }
        check(breaker.state() == State.CLOSED, "so the breaker stays closed");
        System.out.println("reset       : a success between failures keeps it closed");

        // ---- retry with exponential backoff ----------------------------------
        AtomicLong attempts = new AtomicLong();
        Operation<String> flaky = () -> {
            if (attempts.incrementAndGet() < 3) {
                throw new IllegalStateException("temporary failure " + attempts.get());
            }
            return "recovered";
        };

        RetryOutcome<String> outcome = retry(flaky, 5, 100, new Random(7));
        check(outcome.value().equals("recovered"), "the retry eventually succeeded");
        check(outcome.attempts() == 3, "it took three attempts");
        check(outcome.delays().size() == 2, "with a wait before each of the last two");
        check(outcome.delays().get(0) >= 100 && outcome.delays().get(0) < 150,
                "the first wait is the base plus jitter: " + outcome.delays().get(0));
        check(outcome.delays().get(1) >= 200 && outcome.delays().get(1) < 300,
                "the second doubles the base: " + outcome.delays().get(1));
        check(outcome.delays().get(1) > outcome.delays().get(0), "so the waits grow");
        System.out.println("retry       : " + outcome.attempts() + " attempts, waits " + outcome.delays());

        // jitter differs between runs with the same wait schedule, so callers do
        // not all retry in lockstep. Both operations fail twice, then succeed, so
        // retry returns a value with the waits it would have taken.
        AtomicLong counterA = new AtomicLong();
        RetryOutcome<String> withSeed99 = retry(() -> {
            if (counterA.incrementAndGet() <= 2) {
                throw new IllegalStateException("temporary");
            }
            return "ok";
        }, 4, 100, new Random(99));

        AtomicLong counterB = new AtomicLong();
        RetryOutcome<String> withSeed1234 = retry(() -> {
            if (counterB.incrementAndGet() <= 2) {
                throw new IllegalStateException("temporary");
            }
            return "ok";
        }, 4, 100, new Random(1234));

        check(withSeed99.delays().size() == 2, "two waits before the third attempt succeeds");
        check(!withSeed99.delays().equals(withSeed1234.delays()),
                "a different jitter seed spreads the callers out: "
                        + withSeed99.delays() + " versus " + withSeed1234.delays());

        try {
            retry(() -> {
                throw new IllegalStateException("permanent");
            }, 4, 50, new Random(1));
            throw new AssertionError("a permanent failure should surface");
        } catch (IllegalStateException expected) {
            check(expected.getMessage().equals("permanent"), "the last failure is rethrown");
            System.out.println("gave up     : after the attempt budget, the real error surfaces");
        }
        System.out.println("All checks passed.");
    }
}
