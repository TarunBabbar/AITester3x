import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

/**
 * 108 - A skip list: an ordered set built from linked lists stacked in levels.
 * Each node is promoted to the next level with probability one half, so a search
 * skips along the sparse upper levels and drops down, which is where the
 * logarithmic behaviour comes from.
 *
 * It is the probabilistic counterpart to program 97's AVL tree: no rotations,
 * no rebalancing on delete, and sorted input is no worse than random input. The
 * price is that the bounds are expected rather than worst case, which is why the
 * level structure is verified here and the probe counts are measured.
 *
 * Compile and run:
 *   javac SkipList.java
 *   java SkipList
 */
public class SkipList {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static final class Node {
        final int key;
        final Node[] next;

        Node(int key, int height) {
            this.key = key;
            this.next = new Node[height];
        }
    }

    static final class SortedSkipList {
        static final int MAX_LEVEL = 24;

        private final Node head = new Node(Integer.MIN_VALUE, MAX_LEVEL);
        private final Random random;
        private int level = 1;                 // the highest level currently in use
        private int size;

        SortedSkipList(long seed) {
            this.random = new Random(seed);
        }

        int size() {
            return size;
        }

        int level() {
            return level;
        }

        /** How many levels this node gets: one, plus a coin toss per extra level. */
        private int randomLevel() {
            int height = 1;
            while (height < MAX_LEVEL && random.nextBoolean()) {
                height++;
            }
            return height;
        }

        /** The last node before `key` at each level, which is what an insert needs. */
        private Node[] predecessors(int key) {
            Node[] update = new Node[MAX_LEVEL];
            Node current = head;
            for (int index = level - 1; index >= 0; index--) {
                while (current.next[index] != null && current.next[index].key < key) {
                    current = current.next[index];
                }
                update[index] = current;
            }
            return update;
        }

        boolean add(int key) {
            Node[] update = predecessors(key);
            Node after = update[0].next[0];
            if (after != null && after.key == key) {
                return false;                              // already here
            }

            int height = randomLevel();
            if (height > level) {
                for (int index = level; index < height; index++) {
                    update[index] = head;                  // the new level starts at the head
                }
                level = height;
            }

            Node node = new Node(key, height);
            for (int index = 0; index < height; index++) {
                node.next[index] = update[index].next[index];
                update[index].next[index] = node;
            }
            size++;
            return true;
        }

        boolean contains(int key) {
            return find(key) != null;
        }

        private Node find(int key) {
            Node current = head;
            for (int index = level - 1; index >= 0; index--) {
                while (current.next[index] != null && current.next[index].key < key) {
                    current = current.next[index];
                }
            }
            Node found = current.next[0];
            return found != null && found.key == key ? found : null;
        }

        boolean remove(int key) {
            Node[] update = predecessors(key);
            Node target = update[0].next[0];
            if (target == null || target.key != key) {
                return false;
            }
            for (int index = 0; index < target.next.length; index++) {
                if (update[index].next[index] == target) {
                    update[index].next[index] = target.next[index];
                }
            }
            // Give back any level that is now empty.
            while (level > 1 && head.next[level - 1] == null) {
                level--;
            }
            size--;
            return true;
        }

        int first() {
            Node node = head.next[0];
            return node == null ? Integer.MIN_VALUE : node.key;
        }

        int last() {
            int key = Integer.MIN_VALUE;
            for (Node node = head.next[0]; node != null; node = node.next[0]) {
                key = node.key;
            }
            return key;
        }

        List<Integer> sorted() {
            List<Integer> keys = new ArrayList<>();
            for (Node node = head.next[0]; node != null; node = node.next[0]) {
                keys.add(node.key);
            }
            return keys;
        }

        /** The keys present at each level, for inspecting the structure. */
        List<List<Integer>> levels() {
            List<List<Integer>> all = new ArrayList<>();
            for (int index = 0; index < level; index++) {
                List<Integer> keys = new ArrayList<>();
                for (Node node = head.next[index]; node != null; node = node.next[index]) {
                    keys.add(node.key);
                }
                all.add(keys);
            }
            return all;
        }

        /**
         * How many nodes a search touches. Level 0 alone would be a linear walk;
         * the point of the upper levels is that this stays small.
         */
        int probes(int key) {
            int visited = 0;
            Node current = head;
            for (int index = level - 1; index >= 0; index--) {
                while (current.next[index] != null && current.next[index].key < key) {
                    current = current.next[index];
                    visited++;
                }
                visited++;                                 // the comparison that stopped the walk
            }
            return visited;
        }

        double averageProbes() {
            if (size == 0) {
                return 0;
            }
            long total = 0;
            for (int key : sorted()) {
                total += probes(key);
            }
            return (double) total / size;
        }

        /**
         * Every level is sorted, every key on a high level also appears on the
         * level below it (so each level is a subsequence of level 0), level 0 holds
         * exactly the keys, and nothing lives above the recorded height.
         */
        boolean structureIsSound() {
            List<Integer> bottom = sorted();
            if (bottom.size() != size) {
                return false;
            }
            for (int i = 1; i < bottom.size(); i++) {
                if (bottom.get(i - 1) >= bottom.get(i)) {
                    return false;
                }
            }
            for (int index = 1; index < MAX_LEVEL; index++) {
                int position = 0;
                for (Node node = head.next[index]; node != null; node = node.next[index]) {
                    if (node.next.length <= index) {
                        return false;                      // a node too short for this level
                    }
                    while (position < bottom.size() && bottom.get(position) != node.key) {
                        position++;
                    }
                    if (position == bottom.size()) {
                        return false;                      // not on level 0, or out of order
                    }
                    position++;
                }
                if (index >= level && head.next[index] != null) {
                    return false;                          // a level above the recorded height
                }
            }
            return true;
        }
    }

    public static void main(String[] args) {
        // ---- an empty list ---------------------------------------------------
        SortedSkipList empty = new SortedSkipList(1);
        check(empty.size() == 0, "nothing in it");
        check(empty.level() == 1, "one level, which is the minimum");
        check(!empty.contains(5), "nothing to find");
        check(empty.sorted().isEmpty(), "and nothing to list");
        check(empty.structureIsSound(), "an empty list is still a valid one");
        check(!empty.remove(5), "removing from it reports nothing to remove");
        check(empty.averageProbes() == 0, "no probes when there is nothing");
        System.out.println("empty        : size 0, level 1");

        // ---- the basics ------------------------------------------------------
        SortedSkipList small = new SortedSkipList(7);
        for (int key : new int[] {5, 1, 9, 3, 7}) {
            check(small.add(key), "adding " + key + " should be new");
        }
        check(small.size() == 5, "five keys in");
        check(!small.add(5), "adding a duplicate reports it was already there");
        check(small.size() == 5, "and does not change the size");
        for (int key : new int[] {1, 3, 5, 7, 9}) {
            check(small.contains(key), "should contain " + key);
        }
        check(!small.contains(4), "but not 4");
        check(small.sorted().equals(List.of(1, 3, 5, 7, 9)), small.sorted().toString());
        check(small.first() == 1 && small.last() == 9, "the ends are right");
        check(small.structureIsSound(), "and the levels are consistent");
        System.out.println("basics       : " + small.sorted() + " at level " + small.level());

        // ---- removing --------------------------------------------------------
        check(small.remove(5), "5 was there");
        check(!small.remove(5), "and now it is not");
        check(small.sorted().equals(List.of(1, 3, 7, 9)), small.sorted().toString());
        check(small.structureIsSound(), "still sound after a removal");
        check(small.remove(1) && small.remove(9), "the ends come off");
        check(small.first() == 3 && small.last() == 7, "and the ends move in");
        check(small.remove(3) && small.remove(7), "then the rest");
        check(small.size() == 0, "it is empty again");
        check(small.level() == 1, "and the height has come back down to one: " + small.level());
        check(small.structureIsSound(), "with a sound structure");
        System.out.println("removing     : emptied, and the level fell back to " + small.level());

        // ---- against a reference set ----------------------------------------
        SortedSkipList list = new SortedSkipList(20260913);
        TreeSet<Integer> reference = new TreeSet<>();
        Random random = new Random(42);
        for (int i = 0; i < 2_000; i++) {
            int key = random.nextInt(1_000);
            boolean added = list.add(key);
            boolean expected = reference.add(key);
            check(added == expected, "add(" + key + ") disagreed with the reference");
        }
        check(list.size() == reference.size(), "sizes match: " + list.size());
        check(list.sorted().equals(new ArrayList<>(reference)), "and so do the contents");
        check(list.structureIsSound(), "after 2000 inserts the structure is sound");
        check(list.level() <= 20, "and the height is modest: " + list.level());
        System.out.println("reference    : 2000 inserts into " + list.size()
                + " keys, level " + list.level());

        for (int key : reference) {
            if (!list.contains(key)) {
                throw new AssertionError("lost key " + key);
            }
        }

        // ---- deleting half of them ------------------------------------------
        int removed = 0;
        for (int key : new ArrayList<>(reference)) {
            if (key % 2 == 0) {
                check(list.remove(key), "removing " + key);
                reference.remove(key);
                removed++;
            }
        }
        check(list.size() == reference.size(), "the sizes still match");
        check(list.sorted().equals(new ArrayList<>(reference)), "and the contents");
        check(list.structureIsSound(), "with the structure intact after " + removed + " removals");
        System.out.println("deleting     : removed " + removed + " keys, "
                + list.size() + " left, level " + list.level());

        // removing what is not there
        check(!list.remove(5_000), "a key outside the range is not there");
        check(!list.remove(-1), "nor is one below it");
        check(list.size() == reference.size(), "and nothing changed");

        // ---- the promised logarithms ----------------------------------------
        SortedSkipList large = new SortedSkipList(99);
        for (int key = 0; key < 20_000; key++) {
            large.add(key * 3);
        }
        check(large.size() == 20_000, "twenty thousand keys");
        check(large.structureIsSound(), "sound at that size");
        double average = large.averageProbes();
        double binarySearch = Math.log(large.size()) / Math.log(2);
        check(average < 6 * binarySearch, "probes should stay near the logarithm: " + average
                + " against " + binarySearch);
        check(average < large.size() / 100.0, "and far below a linear scan");
        System.out.printf("probes       : %.1f on average for 20,000 keys, against %.1f for binary search%n",
                average, binarySearch);

        // ---- sorted input is not a problem -----------------------------------
        // A plain binary search tree would become a linked list here. The coin
        // tosses do not care what order the keys arrived in.
        SortedSkipList ascending = new SortedSkipList(5);
        for (int key = 1; key <= 10_000; key++) {
            ascending.add(key);
        }
        check(ascending.size() == 10_000, "all of them went in");
        check(ascending.structureIsSound(), "in ascending order too");
        check(ascending.level() <= 24, "and the height stayed bounded: " + ascending.level());
        check(ascending.averageProbes() < 6 * (Math.log(10_000) / Math.log(2)),
                "still logarithmic on sorted input: " + ascending.averageProbes());
        System.out.println("sorted input : 10000 ascending keys, level " + ascending.level()
                + ", average probes " + String.format("%.1f", ascending.averageProbes()));

        // ---- the level distribution ------------------------------------------
        List<List<Integer>> levels = large.levels();
        check(levels.get(0).size() == large.size(), "every key is on level 0");
        double second = levels.get(1).size() / (double) large.size();
        check(second > 0.35 && second < 0.65, "about half are promoted to level 1: " + second);
        for (int index = 1; index < levels.size(); index++) {
            check(levels.get(index).size() <= levels.get(index - 1).size(),
                    "each level is no larger than the one below it");
        }
        int total = levels.stream().mapToInt(List::size).sum();
        System.out.printf("distribution : %d keys across %d levels, level 1 holds %.0f%% of them%n",
                total, levels.size(), second * 100);
        System.out.println("distribution : " + levels.stream().map(List::size).toList());

        // ---- determinism -----------------------------------------------------
        SortedSkipList again = new SortedSkipList(20260913);
        Random sequence = new Random(42);
        for (int i = 0; i < 2_000; i++) {
            again.add(sequence.nextInt(1_000));
        }
        SortedSkipList same = new SortedSkipList(20260913);
        Random replay = new Random(42);
        for (int i = 0; i < 2_000; i++) {
            same.add(replay.nextInt(1_000));
        }
        check(again.level() == same.level(), "the same seed builds the same height");
        check(again.levels().equals(same.levels()), "and the same levels");
        check(again.sorted().equals(same.sorted()), "and the same contents");
        System.out.println("determinism  : one seed, one structure");

        // A different seed still holds the same keys; only the shape changes.
        SortedSkipList otherSeed = new SortedSkipList(1234);
        Random sameKeys = new Random(42);
        for (int i = 0; i < 2_000; i++) {
            otherSeed.add(sameKeys.nextInt(1_000));
        }
        check(otherSeed.size() == same.size(), "a different seed holds the same keys");
        check(otherSeed.sorted().equals(same.sorted()), "with the same contents");
        check(otherSeed.structureIsSound(), "and a sound structure of its own");
        System.out.println("determinism  : seed 20260913 built level " + same.level()
                + ", seed 1234 built level " + otherSeed.level());

        // ---- the extremes of int ---------------------------------------------
        SortedSkipList extremes = new SortedSkipList(3);
        check(extremes.add(Integer.MIN_VALUE), "the smallest int is a valid key");
        check(extremes.add(Integer.MAX_VALUE), "and so is the largest");
        check(extremes.add(0), "and zero");
        check(extremes.contains(Integer.MIN_VALUE), "the smallest is findable");
        check(extremes.contains(Integer.MAX_VALUE), "and the largest");
        check(extremes.sorted().equals(
                List.of(Integer.MIN_VALUE, 0, Integer.MAX_VALUE)), extremes.sorted().toString());
        check(extremes.structureIsSound(), "and the structure copes");
        check(extremes.remove(Integer.MIN_VALUE), "the smallest can be removed");
        check(!extremes.contains(Integer.MIN_VALUE), "and is then gone");
        System.out.println("extremes     : " + extremes.sorted());

        // ---- adding after removing -------------------------------------------
        check(extremes.add(Integer.MIN_VALUE), "putting it back works");
        check(extremes.size() == 3, "with the size back up");
        check(extremes.contains(Integer.MIN_VALUE), "and it is findable again");
        check(extremes.structureIsSound(), "structure still sound");
        System.out.println("All checks passed.");
    }
}
