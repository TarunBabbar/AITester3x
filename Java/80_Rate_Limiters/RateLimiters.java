import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 80 - Rate limiters: fixed window, sliding window log and token bucket, all
 * driven by a clock the test moves by hand so nothing depends on wall time.
 *
 * Compile and run:
 *   javac RateLimiters.java
 *   java RateLimiters
 */
public class RateLimiters {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /** A clock the tests control, so every result is reproducible. */
    static final class FakeClock {
        private long millis;

        FakeClock(long start) {
            this.millis = start;
        }

        long millis() {
            return millis;
        }

        void advance(long by) {
            millis += by;
        }
    }

    /**
     * Fixed window: a counter that resets on a boundary. Simple, but it lets a
     * caller spend the whole allowance at the end of one window and again at the
     * start of the next.
     */
    static final class FixedWindow {
        private final int limit;
        private final long windowMillis;
        private final FakeClock clock;
        private long windowStart;
        private int used;

        FixedWindow(int limit, long windowMillis, FakeClock clock) {
            this.limit = limit;
            this.windowMillis = windowMillis;
            this.clock = clock;
            this.windowStart = clock.millis();
        }

        boolean allow() {
            long now = clock.millis();
            if (now - windowStart >= windowMillis) {
                windowStart = now;
                used = 0;
            }
            if (used < limit) {
                used++;
                return true;
            }
            return false;
        }
    }

    /** Sliding window log: remembers when each accepted call happened. */
    static final class SlidingWindow {
        private final int limit;
        private final long windowMillis;
        private final FakeClock clock;
        private final Deque<Long> accepted = new ArrayDeque<>();

        SlidingWindow(int limit, long windowMillis, FakeClock clock) {
            this.limit = limit;
            this.windowMillis = windowMillis;
            this.clock = clock;
        }

        /** Drop everything that has fallen out of the window. */
        private void prune() {
            long now = clock.millis();
            while (!accepted.isEmpty() && now - accepted.peekFirst() >= windowMillis) {
                accepted.pollFirst();
            }
        }

        boolean allow() {
            long now = clock.millis();
            prune();
            if (accepted.size() < limit) {
                accepted.addLast(now);
                return true;
            }
            return false;
        }

        int logged() {
            prune();
            return accepted.size();
        }
    }

    /** Token bucket: tokens refill continuously, so an idle caller may burst. */
    static final class TokenBucket {
        private final int capacity;
        private final double tokensPerMilli;
        private final FakeClock clock;
        private double tokens;
        private long lastRefill;

        TokenBucket(int capacity, double tokensPerSecond, FakeClock clock) {
            this.capacity = capacity;
            this.tokensPerMilli = tokensPerSecond / 1000.0;
            this.clock = clock;
            this.tokens = capacity;
            this.lastRefill = clock.millis();
        }

        /** A pure read: it must not consume anything. */
        double tokens() {
            return Math.min(capacity, tokens + (clock.millis() - lastRefill) * tokensPerMilli);
        }

        boolean allow() {
            long now = clock.millis();
            tokens = tokens();
            lastRefill = now;
            if (tokens >= 1) {
                tokens -= 1;
                return true;
            }
            return false;
        }
    }

    public static void main(String[] args) {
        // ---- fixed window, and its boundary flaw ------------------------------
        FakeClock fixedClock = new FakeClock(0);
        FixedWindow fixed = new FixedWindow(3, 1_000, fixedClock);

        check(fixed.allow() && fixed.allow() && fixed.allow(), "the first three are admitted");
        check(!fixed.allow(), "the fourth is refused");
        fixedClock.advance(998);
        check(!fixed.allow(), "still refused, 998ms into the window");
        fixedClock.advance(2);
        check(fixed.allow(), "the window rolls over at 1000ms");
        check(fixed.allow() && fixed.allow(), "and admits its own three");
        check(!fixed.allow(), "before refusing again");
        System.out.println("fixed window: 6 admitted within 1000ms across the boundary");
        // Calls one and four are a millisecond apart, so six got through where the
        // limit of three suggested otherwise.

        // ---- sliding window log, on the same schedule -------------------------
        FakeClock slidingClock = new FakeClock(0);
        SlidingWindow sliding = new SlidingWindow(3, 1_000, slidingClock);

        check(sliding.allow(), "call at t=0");
        slidingClock.advance(1);
        check(sliding.allow(), "call at t=1");
        slidingClock.advance(1);
        check(sliding.allow(), "call at t=2");
        check(!sliding.allow(), "a fourth at t=2 is refused");
        check(sliding.logged() == 3, "three calls are on record");

        slidingClock.advance(998);          // t = 1000
        check(sliding.allow(), "the call from t=0 has now aged out, so exactly one is allowed");
        check(!sliding.allow(), "and only one, because two are still inside the window");

        slidingClock.advance(1);            // t = 1001
        check(sliding.allow(), "the call from t=1 ages out one millisecond later");
        System.out.println("sliding log : smoothed across the boundary, never more than 3 in 1000ms");

        // a quiet period empties the log entirely
        slidingClock.advance(5_000);
        check(sliding.logged() == 0, "after a long idle the log is empty");
        check(sliding.allow() && sliding.allow() && sliding.allow(), "the full allowance is back");
        check(!sliding.allow(), "and is once again capped at three");

        // ---- token bucket -----------------------------------------------------
        FakeClock bucketClock = new FakeClock(0);
        TokenBucket bucket = new TokenBucket(5, 2, bucketClock);

        check(Math.abs(bucket.tokens() - 5.0) < 1e-9, "a new bucket is full");
        int admitted = 0;
        for (int i = 0; i < 5; i++) {
            if (bucket.allow()) {
                admitted++;
            }
        }
        check(admitted == 5, "the whole burst is admitted");
        check(!bucket.allow(), "and then the bucket is empty");
        check(Math.abs(bucket.tokens()) < 1e-9, "the read agrees that nothing is left");

        bucketClock.advance(500);
        check(Math.abs(bucket.tokens() - 1.0) < 1e-9, "half a second at 2/s refills one token");
        check(bucket.allow(), "which buys exactly one call");
        check(!bucket.allow(), "but not a second");
        check(Math.abs(bucket.tokens()) < 1e-9, "and leaves none behind");

        bucketClock.advance(5_000);
        check(Math.abs(bucket.tokens() - 5.0) < 1e-9, "idling refills, but only up to the capacity");
        int burst = 0;
        for (int i = 0; i < 6; i++) {
            if (bucket.allow()) {
                burst++;
            }
        }
        check(burst == 5, "so five are allowed after the idle period, not six");
        System.out.printf("token bucket: refills %.1f/s, capacity %d, burst of %d admitted%n",
                2.0, 5, burst);

        // a slow trickle never exhausts the bucket
        FakeClock trickleClock = new FakeClock(0);
        TokenBucket trickle = new TokenBucket(2, 10, trickleClock);
        int trickleAdmitted = 0;
        for (int i = 0; i < 20; i++) {
            if (trickle.allow()) {
                trickleAdmitted++;
            }
            trickleClock.advance(100);      // one call per 100ms = 10/s, exactly the refill rate
        }
        check(trickleAdmitted == 20, "a caller at the refill rate is never refused");
        System.out.println("trickle     : " + trickleAdmitted + " of 20 admitted at exactly the refill rate");

        // faster than the refill rate, and calls start getting refused
        FakeClock fastClock = new FakeClock(0);
        TokenBucket fast = new TokenBucket(2, 10, fastClock);
        int fastAdmitted = 0;
        for (int i = 0; i < 20; i++) {
            if (fast.allow()) {
                fastAdmitted++;
            }
            fastClock.advance(10);          // 100/s, ten times the refill rate
        }
        check(fastAdmitted < 20, "a caller over the refill rate does get refused: " + fastAdmitted);
        System.out.println("too fast    : " + fastAdmitted + " of 20 admitted when over the limit");
        System.out.println("All checks passed.");
    }
}
