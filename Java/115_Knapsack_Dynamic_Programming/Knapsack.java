import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 115 - 0/1 and unbounded knapsack.
 *
 * The 0/1 version fills a one-dimensional table right-to-left so each item is
 * used at most once; the unbounded version fills it left-to-right so items can
 * repeat. A second pass keeps the full table to reconstruct which items were
 * taken, and brute force over subsets checks the small cases.
 *
 * Compile and run:
 *   javac Knapsack.java
 *   java Knapsack
 */
public class Knapsack {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static void validate(int[] weights, int[] values, int capacity) {
        check(weights.length == values.length, "weights and values must have the same length");
        check(capacity >= 0, "capacity must not be negative");
        for (int weight : weights) {
            check(weight > 0, "weights must be positive");
        }
        for (int value : values) {
            check(value >= 0, "values must not be negative");
        }
    }

    static int zeroOne(int[] weights, int[] values, int capacity) {
        validate(weights, values, capacity);
        int[] best = new int[capacity + 1];

        for (int item = 0; item < weights.length; item++) {
            for (int current = capacity; current >= weights[item]; current--) {
                best[current] = Math.max(best[current], best[current - weights[item]] + values[item]);
            }
        }

        return best[capacity];
    }

    /** Indices of one optimal set, in ascending order. */
    static int[] chosenItems(int[] weights, int[] values, int capacity) {
        validate(weights, values, capacity);
        int count = weights.length;
        int[][] table = new int[count + 1][capacity + 1];

        for (int item = 1; item <= count; item++) {
            for (int current = 0; current <= capacity; current++) {
                table[item][current] = table[item - 1][current];
                if (weights[item - 1] <= current) {
                    table[item][current] = Math.max(
                            table[item][current],
                            table[item - 1][current - weights[item - 1]] + values[item - 1]);
                }
            }
        }

        List<Integer> picked = new ArrayList<>();
        int current = capacity;
        for (int item = count; item >= 1; item--) {
            if (table[item][current] != table[item - 1][current]) {
                picked.add(item - 1);
                current -= weights[item - 1];
            }
        }

        int[] result = new int[picked.size()];
        for (int index = 0; index < result.length; index++) {
            result[index] = picked.get(picked.size() - 1 - index);
        }
        return result;
    }

    static int unbounded(int[] weights, int[] values, int capacity) {
        validate(weights, values, capacity);
        int[] best = new int[capacity + 1];

        for (int current = 1; current <= capacity; current++) {
            for (int item = 0; item < weights.length; item++) {
                if (weights[item] <= current) {
                    best[current] = Math.max(best[current], best[current - weights[item]] + values[item]);
                }
            }
        }

        return best[capacity];
    }

    static int bruteForce(int[] weights, int[] values, int capacity) {
        int best = 0;
        int count = weights.length;
        for (int mask = 0; mask < (1 << count); mask++) {
            int weight = 0;
            int value = 0;
            for (int item = 0; item < count; item++) {
                if ((mask & (1 << item)) != 0) {
                    weight += weights[item];
                    value += values[item];
                }
            }
            if (weight <= capacity) {
                best = Math.max(best, value);
            }
        }
        return best;
    }

    static int unboundedBrute(int[] weights, int[] values, int capacity, int item) {
        if (item == weights.length) {
            return 0;
        }
        int best = unboundedBrute(weights, values, capacity, item + 1);
        for (int count = 1; count * weights[item] <= capacity; count++) {
            best = Math.max(best, count * values[item]
                    + unboundedBrute(weights, values, capacity - count * weights[item], item + 1));
        }
        return best;
    }

    static int totalWeight(int[] weights, int[] chosen) {
        int total = 0;
        for (int index : chosen) {
            total += weights[index];
        }
        return total;
    }

    static int totalValue(int[] values, int[] chosen) {
        int total = 0;
        for (int index : chosen) {
            total += values[index];
        }
        return total;
    }

    public static void main(String[] args) {
        // ---- the classic example --------------------------------------------
        int[] weights = {1, 3, 4, 5};
        int[] values = {1, 4, 5, 7};
        int capacity = 7;

        int best = zeroOne(weights, values, capacity);
        int[] chosen = chosenItems(weights, values, capacity);
        System.out.println("classic      : value " + best + ", items " + java.util.Arrays.toString(chosen));
        check(best == 9, "expected 9, got " + best);
        check(totalValue(values, chosen) == best, "reconstruction value");
        check(totalWeight(weights, chosen) <= capacity, "reconstruction fits");
        check(chosen.length == 2, "two items are taken");

        // ---- edge cases ------------------------------------------------------
        check(zeroOne(new int[0], new int[0], 10) == 0, "no items");
        check(zeroOne(weights, values, 0) == 0, "no capacity");
        check(zeroOne(new int[] {5}, new int[] {10}, 4) == 0, "item too heavy");
        check(zeroOne(new int[] {5}, new int[] {10}, 5) == 10, "item exactly fits");
        check(chosenItems(new int[0], new int[0], 5).length == 0, "nothing to choose");
        System.out.println("edge cases   : empty, zero capacity, too heavy, exact fit");

        // ---- zero-value items ------------------------------------------------
        check(zeroOne(new int[] {1, 2}, new int[] {0, 0}, 3) == 0, "zero values");
        check(zeroOne(new int[] {2, 2}, new int[] {3, 3}, 2) == 3, "pick one of two identical items");
        System.out.println("zero values  : best value stays 0");

        // ---- unbounded -------------------------------------------------------
        int[] coinWeights = {2, 3, 4};
        int[] coinValues = {3, 5, 7};
        check(unbounded(coinWeights, coinValues, 10) == 17, "unbounded best");
        check(unbounded(weights, values, 7) >= zeroOne(weights, values, 7), "unbounded is never worse");
        check(unbounded(new int[0], new int[0], 5) == 0, "unbounded with no items");
        System.out.println("unbounded    : capacity 10 -> " + unbounded(coinWeights, coinValues, 10));

        // ---- validation ------------------------------------------------------
        int[][] mismatched = {{1, 2}, {3}};
        try {
            zeroOne(mismatched[0], mismatched[1], 5);
            throw new AssertionError("mismatched arrays should fail");
        } catch (AssertionError expected) {
            check(expected.getMessage().contains("same length"), expected.getMessage());
        }
        try {
            zeroOne(weights, values, -1);
            throw new AssertionError("negative capacity should fail");
        } catch (AssertionError expected) {
            check(expected.getMessage().contains("capacity"), expected.getMessage());
        }
        try {
            zeroOne(new int[] {0}, new int[] {1}, 5);
            throw new AssertionError("zero weight should fail");
        } catch (AssertionError expected) {
            check(expected.getMessage().contains("positive"), expected.getMessage());
        }
        System.out.println("validation   : mismatched lengths, negative capacity, zero weight rejected");

        // ---- randomised cross-check -----------------------------------------
        Random random = new Random(20261106);
        int checked = 0;

        for (int trial = 0; trial < 400; trial++) {
            int count = random.nextInt(13);
            int[] randomWeights = new int[count];
            int[] randomValues = new int[count];
            for (int item = 0; item < count; item++) {
                randomWeights[item] = 1 + random.nextInt(10);
                randomValues[item] = random.nextInt(21);
            }
            int randomCapacity = random.nextInt(41);

            int fast = zeroOne(randomWeights, randomValues, randomCapacity);
            int exhaustive = bruteForce(randomWeights, randomValues, randomCapacity);
            check(fast == exhaustive, "0/1 mismatch at trial " + trial);

            int[] picked = chosenItems(randomWeights, randomValues, randomCapacity);
            check(totalWeight(randomWeights, picked) <= randomCapacity, "chosen set exceeds capacity");
            check(totalValue(randomValues, picked) == fast, "chosen set value differs from optimum");
            for (int index = 1; index < picked.length; index++) {
                check(picked[index - 1] < picked[index], "chosen indices must be ascending");
            }
            checked++;
        }
        System.out.println("0/1 check    : " + checked + " random instances match brute force");

        checked = 0;
        for (int trial = 0; trial < 200; trial++) {
            int count = 1 + random.nextInt(4);
            int[] randomWeights = new int[count];
            int[] randomValues = new int[count];
            for (int item = 0; item < count; item++) {
                randomWeights[item] = 1 + random.nextInt(5);
                randomValues[item] = random.nextInt(10);
            }
            int randomCapacity = random.nextInt(21);

            check(unbounded(randomWeights, randomValues, randomCapacity)
                    == unboundedBrute(randomWeights, randomValues, randomCapacity, 0),
                    "unbounded mismatch at trial " + trial);
            checked++;
        }
        System.out.println("unbounded    : " + checked + " random instances match exhaustive counts");

        // ---- a larger instance ----------------------------------------------
        int count = 500;
        int[] largeWeights = new int[count];
        int[] largeValues = new int[count];
        for (int item = 0; item < count; item++) {
            largeWeights[item] = 1 + random.nextInt(50);
            largeValues[item] = 1 + random.nextInt(100);
        }

        long started = System.nanoTime();
        int largeBest = zeroOne(largeWeights, largeValues, 5_000);
        long elapsed = System.nanoTime() - started;

        check(largeBest > 0, "a large instance should use some items");
        System.out.printf("large input  : 500 items, capacity 5000, value %d in %.1f ms%n",
                largeBest, elapsed / 1_000_000.0);

        int[] largePicked = chosenItems(largeWeights, largeValues, 5_000);
        check(totalValue(largeValues, largePicked) == largeBest, "large reconstruction value");
        check(totalWeight(largeWeights, largePicked) <= 5_000, "large reconstruction weight");
        System.out.println("large picked : " + largePicked.length + " items");

        System.out.println("All checks passed.");
    }
}
