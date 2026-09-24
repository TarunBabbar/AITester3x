import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 33 - Dynamic programming: memoized and tabulated Fibonacci, coin change with
 * reconstruction, 0/1 knapsack and longest common subsequence.
 *
 * Compile and run:
 *   javac DynamicProgramming.java
 *   java DynamicProgramming
 */
public class DynamicProgramming {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    // ------------------------------------------------------------ Fibonacci
    static long fibMemo(int n, long[] memo) {
        if (n < 2) {
            return n;
        }
        if (memo[n] != 0) {
            return memo[n];
        }
        memo[n] = fibMemo(n - 1, memo) + fibMemo(n - 2, memo);
        return memo[n];
    }

    static long fibTable(int n) {
        if (n < 2) {
            return n;
        }
        long previous = 0;
        long current = 1;
        for (int i = 2; i <= n; i++) {
            long next = previous + current;
            previous = current;
            current = next;
        }
        return current;
    }

    // ---------------------------------------------------------- coin change
    static int minCoins(int amount, int[] coins) {
        int[] best = new int[amount + 1];
        Arrays.fill(best, Integer.MAX_VALUE);
        best[0] = 0;
        for (int target = 1; target <= amount; target++) {
            for (int coin : coins) {
                if (coin <= target && best[target - coin] != Integer.MAX_VALUE) {
                    best[target] = Math.min(best[target], best[target - coin] + 1);
                }
            }
        }
        return best[amount] == Integer.MAX_VALUE ? -1 : best[amount];
    }

    static List<Integer> coinCombination(int amount, int[] coins) {
        int[] best = new int[amount + 1];
        Arrays.fill(best, Integer.MAX_VALUE);
        best[0] = 0;
        for (int target = 1; target <= amount; target++) {
            for (int coin : coins) {
                if (coin <= target && best[target - coin] != Integer.MAX_VALUE) {
                    best[target] = Math.min(best[target], best[target - coin] + 1);
                }
            }
        }
        List<Integer> used = new ArrayList<>();
        if (best[amount] == Integer.MAX_VALUE) {
            return used;
        }
        int remaining = amount;
        while (remaining > 0) {
            for (int coin : coins) {
                if (coin <= remaining && best[remaining - coin] == best[remaining] - 1) {
                    used.add(coin);
                    remaining -= coin;
                    break;
                }
            }
        }
        return used;
    }

    // -------------------------------------------------------------- knapsack
    record Item(String name, int weight, int value) {
    }

    static int knapsack(List<Item> items, int capacity) {
        int[][] table = new int[items.size() + 1][capacity + 1];
        for (int i = 1; i <= items.size(); i++) {
            Item item = items.get(i - 1);
            for (int room = 0; room <= capacity; room++) {
                table[i][room] = table[i - 1][room];
                if (item.weight() <= room) {
                    table[i][room] = Math.max(table[i][room],
                            table[i - 1][room - item.weight()] + item.value());
                }
            }
        }
        return table[items.size()][capacity];
    }

    static List<String> knapsackChoices(List<Item> items, int capacity) {
        int[][] table = new int[items.size() + 1][capacity + 1];
        for (int i = 1; i <= items.size(); i++) {
            Item item = items.get(i - 1);
            for (int room = 0; room <= capacity; room++) {
                table[i][room] = table[i - 1][room];
                if (item.weight() <= room) {
                    table[i][room] = Math.max(table[i][room],
                            table[i - 1][room - item.weight()] + item.value());
                }
            }
        }
        List<String> chosen = new ArrayList<>();
        int room = capacity;
        for (int i = items.size(); i > 0; i--) {
            if (table[i][room] != table[i - 1][room]) {
                chosen.add(0, items.get(i - 1).name());
                room -= items.get(i - 1).weight();
            }
        }
        return chosen;
    }

    // ------------------------------------------------------------------- LCS
    static int lcsLength(String a, String b) {
        int[][] table = new int[a.length() + 1][b.length() + 1];
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                table[i][j] = a.charAt(i - 1) == b.charAt(j - 1)
                        ? table[i - 1][j - 1] + 1
                        : Math.max(table[i - 1][j], table[i][j - 1]);
            }
        }
        return table[a.length()][b.length()];
    }

    static String lcs(String a, String b) {
        int[][] table = new int[a.length() + 1][b.length() + 1];
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                table[i][j] = a.charAt(i - 1) == b.charAt(j - 1)
                        ? table[i - 1][j - 1] + 1
                        : Math.max(table[i - 1][j], table[i][j - 1]);
            }
        }
        StringBuilder result = new StringBuilder();
        int i = a.length();
        int j = b.length();
        while (i > 0 && j > 0) {
            if (a.charAt(i - 1) == b.charAt(j - 1)) {
                result.append(a.charAt(i - 1));
                i--;
                j--;
            } else if (table[i - 1][j] >= table[i][j - 1]) {
                i--;
            } else {
                j--;
            }
        }
        return result.reverse().toString();
    }

    public static void main(String[] args) {
        check(fibMemo(30, new long[31]) == 832_040L, "fib(30) with memoization");
        check(fibTable(30) == 832_040L, "fib(30) by tabulation");
        check(fibTable(50) == 12_586_269_025L, "fib(50) still fits in a long");
        System.out.println("fib(30)      : memo=" + fibMemo(30, new long[31]) + " table=" + fibTable(30));

        int[] coins = {1, 5, 10, 25};
        check(minCoins(63, coins) == 6, "63 needs six coins");
        check(minCoins(30, coins) == 2, "30 needs two coins");
        check(minCoins(7, new int[] {2, 4}) == -1, "an odd amount cannot be made from even coins");
        List<Integer> combination = coinCombination(63, coins);
        check(combination.size() == 6, "the reconstructed combination has six coins");
        check(combination.stream().mapToInt(Integer::intValue).sum() == 63, "it sums to 63");
        System.out.println("coins 63     : " + combination + " (" + minCoins(63, coins) + " coins)");

        List<Item> items = List.of(
                new Item("gold", 10, 60),
                new Item("silver", 20, 100),
                new Item("bronze", 30, 120));
        check(knapsack(items, 50) == 220, "best value for capacity 50");
        check(knapsackChoices(items, 50).equals(List.of("silver", "bronze")), "chosen items");
        System.out.println("knapsack     : value=" + knapsack(items, 50)
                + " taking " + knapsackChoices(items, 50));

        check(lcsLength("AGGTAB", "GXTXAYB") == 4, "LCS length is 4");
        check(lcs("AGGTAB", "GXTXAYB").equals("GTAB"), "LCS is GTAB");
        System.out.println("LCS          : " + lcs("AGGTAB", "GXTXAYB"));
        System.out.println("All checks passed.");
    }
}
