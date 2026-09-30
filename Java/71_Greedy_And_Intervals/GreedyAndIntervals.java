import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * 71 - Greedy algorithms and intervals: activity selection, interval merging,
 * meeting rooms, Huffman coding, and a case where greedy is provably wrong.
 *
 * Compile and run:
 *   javac GreedyAndIntervals.java
 *   java GreedyAndIntervals
 */
public class GreedyAndIntervals {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    record Interval(int start, int end) {
        @Override
        public String toString() {
            return "[" + start + "," + end + "]";
        }
    }

    // ------------------------------------------------- activity selection
    /** Greedy on the earliest finishing activity fits the most in. */
    static List<Interval> selectActivities(List<Interval> intervals) {
        List<Interval> ordered = new ArrayList<>(intervals);
        ordered.sort(Comparator.comparingInt(Interval::end).thenComparingInt(Interval::start));
        List<Interval> chosen = new ArrayList<>();
        int lastEnd = Integer.MIN_VALUE;
        for (Interval interval : ordered) {
            if (interval.start() >= lastEnd) {
                chosen.add(interval);
                lastEnd = interval.end();
            }
        }
        return chosen;
    }

    // --------------------------------------------------- merging intervals
    /** Touching intervals merge too: these are closed ranges. */
    static List<Interval> merge(List<Interval> intervals) {
        if (intervals.isEmpty()) {
            return List.of();
        }
        List<Interval> ordered = new ArrayList<>(intervals);
        ordered.sort(Comparator.comparingInt(Interval::start).thenComparingInt(Interval::end));
        List<Interval> merged = new ArrayList<>();
        Interval current = ordered.get(0);
        for (int i = 1; i < ordered.size(); i++) {
            Interval next = ordered.get(i);
            if (next.start() <= current.end()) {
                current = new Interval(current.start(), Math.max(current.end(), next.end()));
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        return merged;
    }

    // ------------------------------------------------------- meeting rooms
    /** The most meetings happening at once is the number of rooms needed. */
    static int minRooms(List<Interval> intervals) {
        List<int[]> events = new ArrayList<>();
        for (Interval interval : intervals) {
            events.add(new int[] {interval.start(), 1});
            events.add(new int[] {interval.end(), -1});
        }
        // At the same minute, a finish is processed before a start, so back-to-back
        // meetings can reuse one room.
        events.sort((left, right) -> left[0] != right[0]
                ? Integer.compare(left[0], right[0])
                : Integer.compare(left[1], right[1]));
        int current = 0;
        int peak = 0;
        for (int[] event : events) {
            current += event[1];
            peak = Math.max(peak, current);
        }
        return peak;
    }

    // ------------------------------------------------------- Huffman coding
    record HuffmanNode(char symbol, int frequency, HuffmanNode left, HuffmanNode right) {
        boolean isLeaf() {
            return left == null && right == null;
        }
    }

    static Map<Character, Integer> frequencies(String text) {
        Map<Character, Integer> counts = new java.util.TreeMap<>();
        for (char character : text.toCharArray()) {
            counts.merge(character, 1, Integer::sum);
        }
        return counts;
    }

    static HuffmanNode buildTree(Map<Character, Integer> frequencies) {
        PriorityQueue<HuffmanNode> queue = new PriorityQueue<>(
                Comparator.comparingInt(HuffmanNode::frequency)
                        .thenComparingInt(node -> node.symbol()));
        frequencies.forEach((symbol, count) -> queue.add(new HuffmanNode(symbol, count, null, null)));
        while (queue.size() > 1) {
            HuffmanNode left = queue.poll();
            HuffmanNode right = queue.poll();
            queue.add(new HuffmanNode('\0', left.frequency() + right.frequency(), left, right));
        }
        return queue.poll();
    }

    static void collectCodes(HuffmanNode node, String prefix, Map<Character, String> codes) {
        if (node == null) {
            return;
        }
        if (node.isLeaf()) {
            codes.put(node.symbol(), prefix.isEmpty() ? "0" : prefix);
            return;
        }
        collectCodes(node.left(), prefix + "0", codes);
        collectCodes(node.right(), prefix + "1", codes);
    }

    static String encodeWith(String text, Map<Character, String> codes) {
        StringBuilder out = new StringBuilder();
        for (char character : text.toCharArray()) {
            out.append(codes.get(character));
        }
        return out.toString();
    }

    static String decodeWith(String bits, HuffmanNode root) {
        StringBuilder out = new StringBuilder();
        if (root.isLeaf()) {                 // only one distinct symbol
            return String.valueOf(root.symbol()).repeat(bits.length());
        }
        HuffmanNode node = root;
        for (int i = 0; i < bits.length(); i++) {
            node = bits.charAt(i) == '0' ? node.left() : node.right();
            if (node.isLeaf()) {
                out.append(node.symbol());
                node = root;
            }
        }
        return out.toString();
    }

    /** No code may be the beginning of another, or decoding is ambiguous. */
    static boolean prefixFree(Map<Character, String> codes) {
        List<String> all = new ArrayList<>(codes.values());
        for (String first : all) {
            for (String second : all) {
                if (!first.equals(second) && second.startsWith(first)) {
                    return false;
                }
            }
        }
        return true;
    }

    // ------------------------------------------------- greedy versus optimal
    static int greedyCoins(int amount, int[] coins) {
        int[] descending = coins.clone();
        Arrays.sort(descending);
        int remaining = amount;
        int used = 0;
        for (int i = descending.length - 1; i >= 0; i--) {
            while (remaining >= descending[i]) {
                remaining -= descending[i];
                used++;
            }
        }
        return remaining == 0 ? used : -1;
    }

    static int optimalCoins(int amount, int[] coins) {
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

    public static void main(String[] args) {
        // ---- activity selection ----------------------------------------------
        List<Interval> meetings = List.of(
                new Interval(1, 4), new Interval(3, 5), new Interval(0, 6),
                new Interval(5, 7), new Interval(3, 9), new Interval(5, 9),
                new Interval(6, 10), new Interval(8, 11), new Interval(8, 12),
                new Interval(2, 14), new Interval(12, 16));
        List<Interval> selected = selectActivities(meetings);
        check(selected.size() == 4, "the classic example fits four activities");
        check(selected.equals(List.of(new Interval(1, 4), new Interval(5, 7),
                new Interval(8, 11), new Interval(12, 16))), "and they are the earliest-finishing ones");
        System.out.println("selection   : " + selected);

        // ---- merging ----------------------------------------------------------
        check(merge(List.of(new Interval(1, 3), new Interval(2, 6),
                new Interval(8, 10), new Interval(15, 18)))
                .equals(List.of(new Interval(1, 6), new Interval(8, 10), new Interval(15, 18))),
                "overlapping intervals merge");
        check(merge(List.of(new Interval(1, 4), new Interval(2, 3)))
                .equals(List.of(new Interval(1, 4))), "a contained interval disappears");
        check(merge(List.of(new Interval(1, 3), new Interval(3, 5)))
                .equals(List.of(new Interval(1, 5))), "touching intervals merge, since these are closed");
        check(merge(List.of()).isEmpty(), "nothing to merge");
        check(merge(List.of(new Interval(5, 6))).equals(List.of(new Interval(5, 6))), "a single interval");
        System.out.println("merged      : " + merge(List.of(new Interval(1, 3), new Interval(2, 6),
                new Interval(8, 10), new Interval(15, 18))));

        // ---- meeting rooms ----------------------------------------------------
        check(minRooms(List.of(new Interval(0, 30), new Interval(5, 10),
                new Interval(15, 20))) == 2, "the classic two-room case");
        check(minRooms(List.of(new Interval(7, 10), new Interval(2, 4))) == 1, "no overlap, one room");
        check(minRooms(List.of(new Interval(1, 5), new Interval(2, 6),
                new Interval(3, 7))) == 3, "three meetings all overlap");
        check(minRooms(List.of(new Interval(1, 3), new Interval(3, 5))) == 1,
                "back-to-back meetings reuse the room");
        System.out.println("rooms       : " + minRooms(List.of(new Interval(0, 30),
                new Interval(5, 10), new Interval(15, 20))) + " for the classic case");

        // ---- Huffman coding ---------------------------------------------------
        String text = "abracadabra";
        Map<Character, Integer> counts = frequencies(text);
        check(counts.get('a') == 5 && counts.get('b') == 2 && counts.get('c') == 1, "the letter counts");
        check(counts.values().stream().mapToInt(Integer::intValue).sum() == text.length(),
                "the counts add up to the length");

        HuffmanNode root = buildTree(counts);
        Map<Character, String> codes = new java.util.TreeMap<>();
        collectCodes(root, "", codes);
        check(codes.size() == counts.size(), "one code per distinct symbol");
        check(prefixFree(codes), "the codes are prefix free, so decoding is unambiguous");

        String bits = encodeWith(text, codes);
        check(decodeWith(bits, root).equals(text), "Huffman decoding returns the original");
        check(bits.length() < text.length() * 8, "and uses fewer than eight bits per character");

        int weighted = counts.entrySet().stream()
                .mapToInt(entry -> entry.getValue() * codes.get(entry.getKey()).length())
                .sum();
        check(weighted == bits.length(), "the encoded length equals the weighted code lengths");
        System.out.println("huffman     : " + codes);
        System.out.printf("huffman     : %d bits instead of %d%n", bits.length(), text.length() * 8);

        // a single distinct symbol is the edge case
        Map<Character, String> single = new java.util.TreeMap<>();
        collectCodes(buildTree(frequencies("aaaa")), "", single);
        check(single.get('a').equals("0"), "one symbol gets the code 0");
        check(decodeWith(encodeWith("aaaa", single), buildTree(frequencies("aaaa"))).equals("aaaa"),
                "and it still round trips");

        // ---- where greedy fails ----------------------------------------------
        int[] canonical = {1, 5, 10, 25};
        check(greedyCoins(63, canonical) == 6, "greedy is optimal for ordinary coins");
        check(greedyCoins(63, canonical) == optimalCoins(63, canonical), "and matches the DP answer");

        int[] awkward = {1, 3, 4};
        check(greedyCoins(6, awkward) == 3, "greedy takes 4 + 1 + 1 for amount 6");
        check(optimalCoins(6, awkward) == 2, "but 3 + 3 is better");
        check(greedyCoins(6, awkward) > optimalCoins(6, awkward),
                "so greedy is not optimal for every coin system");
        System.out.println("greedy fail : coins " + Arrays.toString(awkward)
                + " for 6 -> greedy " + greedyCoins(6, awkward)
                + ", optimal " + optimalCoins(6, awkward));
        System.out.println("All checks passed.");
    }
}
