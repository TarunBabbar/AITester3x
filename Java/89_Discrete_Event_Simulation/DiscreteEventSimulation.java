import java.util.ArrayDeque;
import java.util.Deque;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * 89 - A discrete event simulation: a single server with a queue. Arrivals and
 * service times are drawn from a seeded generator, the clock jumps from event to
 * event rather than ticking, and the run reports waiting times and utilisation.
 *
 * Compile and run:
 *   javac DiscreteEventSimulation.java
 *   java DiscreteEventSimulation
 */
public class DiscreteEventSimulation {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /**
     * At the same instant a service completion is handled before an arrival, so
     * the server is already free when the newcomer looks.
     */
    record Event(double time, boolean arrival, int customer) implements Comparable<Event> {
        @Override
        public int compareTo(Event other) {
            int byTime = Double.compare(time, other.time);
            if (byTime != 0) {
                return byTime;
            }
            if (arrival != other.arrival) {
                return arrival ? 1 : -1;
            }
            return Integer.compare(customer, other.customer);
        }

        @Override
        public String toString() {
            return (arrival ? "arrival" : "departure") + " of " + customer + " at " + time;
        }
    }

    record Outcome(int arrivals, int served, int leftWaiting, int maxQueue,
                   double averageWait, double maxWait, double utilisation, double clock) {
    }

    /** A draw from the exponential distribution with the given mean. */
    static double exponential(Random random, double mean) {
        return -mean * Math.log(1 - random.nextDouble());
    }

    static Outcome simulate(int customers, double meanGap, double meanService, long seed) {
        Random random = new Random(seed);
        double[] arrivalAt = new double[customers];
        double[] service = new double[customers];

        double moment = 0;
        for (int customer = 0; customer < customers; customer++) {
            moment += exponential(random, meanGap);
            arrivalAt[customer] = moment;
            service[customer] = exponential(random, meanService);
        }

        PriorityQueue<Event> events = new PriorityQueue<>();
        for (int customer = 0; customer < customers; customer++) {
            events.add(new Event(arrivalAt[customer], true, customer));
        }

        Deque<Integer> queue = new ArrayDeque<>();
        double clock = 0;
        double busyTime = 0;
        double totalWait = 0;
        double maxWait = 0;
        int served = 0;
        int maxQueue = 0;
        boolean busy = false;

        while (!events.isEmpty()) {
            Event event = events.poll();
            clock = event.time();

            if (event.arrival()) {
                queue.addLast(event.customer());
                maxQueue = Math.max(maxQueue, queue.size());
            } else {
                served++;                 // the customer's service just finished
                busy = false;
            }

            // Whoever is at the head of the queue starts now, if the server is free.
            if (!busy && !queue.isEmpty()) {
                int customer = queue.pollFirst();
                double wait = clock - arrivalAt[customer];
                totalWait += wait;
                maxWait = Math.max(maxWait, wait);
                busyTime += service[customer];
                busy = true;
                events.add(new Event(clock + service[customer], false, customer));
            }
        }

        return new Outcome(
                customers,
                served,
                queue.size(),
                maxQueue,
                served == 0 ? 0 : totalWait / served,
                maxWait,
                clock == 0 ? 0 : busyTime / clock,
                clock);
    }

    public static void main(String[] args) {
        // ---- a server that keeps up ------------------------------------------
        Outcome quick = simulate(200, 10.0, 1.0, 42);
        System.out.printf("fast server  : %d invited, %d served, %d still queued, max queue %d%n",
                quick.arrivals(), quick.served(), quick.leftWaiting(), quick.maxQueue());
        check(quick.served() + quick.leftWaiting() == quick.arrivals(),
                "every customer is either served or still waiting");
        check(quick.served() == quick.arrivals(), "a server ten times faster than the arrivals clears the queue");
        check(quick.maxQueue() <= 5, "so the queue barely forms: " + quick.maxQueue());
        check(quick.utilisation() > 0 && quick.utilisation() < 0.5,
                "and it is mostly idle: " + quick.utilisation());
        check(quick.averageWait() < 1.0, "waiting is negligible when the server is idle");
        check(quick.clock() > 0, "the clock advanced");

        // ---- a server that cannot keep up ------------------------------------
        Outcome slow = simulate(200, 10.0, 20.0, 42);
        System.out.printf("slow server  : %d invited, %d served, %d still queued, max queue %d%n",
                slow.arrivals(), slow.served(), slow.leftWaiting(), slow.maxQueue());
        check(slow.served() + slow.leftWaiting() == slow.arrivals(), "the books still balance");
        // Arrivals stop after the last customer, so the server does eventually work
        // through the backlog. An overload shows up as queue length and waiting time,
        // not as customers left unserved.
        check(slow.maxQueue() > 50, "but the queue grew long first: " + slow.maxQueue());
        check(slow.maxQueue() > quick.maxQueue(), "far longer than the fast server's");
        check(slow.averageWait() > 5.0, "and the average wait is far worse: " + slow.averageWait());
        check(slow.averageWait() > quick.averageWait(), "compared with the fast server's");
        check(slow.clock() > quick.clock(), "the run lasts longer, because the work outlasts the arrivals");

        // ---- utilisation is a ratio of busy time to elapsed time -------------
        check(quick.utilisation() > 0 && quick.utilisation() <= 1, "utilisation is a fraction");
        check(slow.utilisation() > 0 && slow.utilisation() <= 1, "for the slow server too");
        check(slow.utilisation() > quick.utilisation(), "the slow server is the busier of the two");
        // roughly 1/10 for the fast server: 200 arrivals over about 2000 time units
        check(quick.utilisation() < 0.15, "the fast server's utilisation tracks the load: "
                + quick.utilisation());

        // ---- the average is the total over the served ------------------------
        Outcome steady = simulate(500, 5.0, 4.0, 7);
        check(steady.averageWait() >= 0, "waiting times are never negative");
        check(steady.maxWait() >= steady.averageWait(), "the worst wait is never below the average: "
                + steady.maxWait() + " against " + steady.averageWait());
        check(steady.arrivals() == 500, "all arrivals are accounted for");
        System.out.printf("busy server  : %.2f average wait, %.2f worst, %.1f%% utilisation%n",
                steady.averageWait(), steady.maxWait(), steady.utilisation() * 100);

        // ---- the queue never exceeds the number of arrivals -------------------
        check(steady.maxQueue() <= steady.arrivals(), "the queue cannot hold more than arrived");
        check(steady.maxQueue() >= 1, "and at least one customer was in it");

        // ---- determinism ------------------------------------------------------
        check(simulate(200, 10.0, 20.0, 42).equals(slow), "the same seed gives the same run");
        check(!simulate(200, 10.0, 20.0, 43).equals(slow), "a different seed gives a different run");
        System.out.println("determinism  : seed 42 reproduces exactly, seed 43 does not");

        // ---- the event queue really is ordered -------------------------------
        PriorityQueue<Event> ordering = new PriorityQueue<>();
        ordering.add(new Event(2.0, true, 1));
        ordering.add(new Event(1.0, false, 2));
        ordering.add(new Event(1.0, true, 3));
        Event first = ordering.poll();
        Event second = ordering.poll();
        Event third = ordering.poll();
        check(first.time() == 1.0, "the earliest event comes out first");
        check(!first.arrival(), "and at time 1.0 the departure is picked before the arrival");
        check(second.arrival() && second.time() == 1.0, "so the arrival at the same instant comes second");
        check(third.time() == 2.0, "then the later one");
        System.out.println("ordering     : departures before arrivals at the same instant");

        // ---- the generator behaves -------------------------------------------
        Random random = new Random(1);
        double total = 0;
        int draws = 100_000;
        for (int i = 0; i < draws; i++) {
            double sample = exponential(random, 4.0);
            check(sample > 0, "an exponential draw is always positive");
            total += sample;
        }
        double observed = total / draws;
        check(Math.abs(observed - 4.0) < 0.2,
                "a hundred thousand draws average close to the mean: " + observed);
        System.out.printf("generator    : 100,000 draws averaged %.3f against a mean of 4%n", observed);
        System.out.println("All checks passed.");
    }
}
