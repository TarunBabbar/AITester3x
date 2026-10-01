import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 79 - A trie (prefix tree): insert, exact lookup, prefix counting and
 * autocomplete, plus a wildcard pattern match where '.' stands for any letter.
 *
 * Compile and run:
 *   javac Trie.java
 *   java Trie
 */
public class Trie {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static final class Node {
        /** A sorted map keeps every traversal alphabetical and deterministic. */
        final Map<Character, Node> children = new TreeMap<>();
        boolean endOfWord;
        /** How many inserted words pass through this node, so prefix counts are free. */
        int wordsBelow;
    }

    static final class PrefixTree {
        private final Node root = new Node();
        private int size;

        void insert(String word) {
            if (word == null || word.isEmpty()) {
                throw new IllegalArgumentException("a word must have at least one character");
            }
            Node existing = find(word);
            if (existing != null && existing.endOfWord) {
                return;      // already stored, so the prefix counts must not move
            }
            Node node = root;
            for (char character : word.toCharArray()) {
                node = node.children.computeIfAbsent(character, key -> new Node());
                node.wordsBelow++;
            }
            node.endOfWord = true;
            size++;
        }

        private Node find(String text) {
            Node node = root;
            for (char character : text.toCharArray()) {
                node = node.children.get(character);
                if (node == null) {
                    return null;
                }
            }
            return node;
        }

        boolean contains(String word) {
            Node node = find(word);
            return node != null && node.endOfWord;
        }

        boolean startsWith(String prefix) {
            return find(prefix) != null;
        }

        int size() {
            return size;
        }

        /** How many stored words begin with this prefix. */
        int countWithPrefix(String prefix) {
            if (prefix.isEmpty()) {
                return size;
            }
            Node node = find(prefix);
            return node == null ? 0 : node.wordsBelow;
        }

        /** Autocomplete: every word under the prefix, alphabetically. */
        List<String> wordsWithPrefix(String prefix) {
            List<String> words = new ArrayList<>();
            Node node = find(prefix);
            if (node != null) {
                collect(node, new StringBuilder(prefix), words);
            }
            return words;
        }

        private void collect(Node node, StringBuilder prefix, List<String> words) {
            if (node.endOfWord) {
                words.add(prefix.toString());
            }
            node.children.forEach((character, child) -> {
                prefix.append(character);
                collect(child, prefix, words);
                prefix.deleteCharAt(prefix.length() - 1);
            });
        }

        /** The shared opening of every word in the tree. */
        String longestCommonPrefix() {
            StringBuilder prefix = new StringBuilder();
            Node node = root;
            while (node.children.size() == 1 && !node.endOfWord) {
                Map.Entry<Character, Node> only = node.children.entrySet().iterator().next();
                prefix.append(only.getKey());
                node = only.getValue();
            }
            return prefix.toString();
        }

        /** Words matching a pattern where '.' matches exactly one character. */
        List<String> matching(String pattern) {
            List<String> matches = new ArrayList<>();
            match(root, pattern, 0, new StringBuilder(), matches);
            return matches;
        }

        private void match(Node node, String pattern, int index, StringBuilder built, List<String> matches) {
            if (index == pattern.length()) {
                if (node.endOfWord) {
                    matches.add(built.toString());
                }
                return;
            }
            char wanted = pattern.charAt(index);
            if (wanted == '.') {
                node.children.forEach((character, child) -> {
                    built.append(character);
                    match(child, pattern, index + 1, built, matches);
                    built.deleteCharAt(built.length() - 1);
                });
            } else {
                Node child = node.children.get(wanted);
                if (child != null) {
                    built.append(wanted);
                    match(child, pattern, index + 1, built, matches);
                    built.deleteCharAt(built.length() - 1);
                }
            }
        }
    }

    public static void main(String[] args) {
        PrefixTree tree = new PrefixTree();
        List<String> words = List.of("car", "cart", "cat", "dog", "do", "door");
        words.forEach(tree::insert);

        check(tree.size() == 6, "six distinct words");
        tree.insert("car");
        check(tree.size() == 6, "inserting a duplicate does not change the size");
        System.out.println("size        : " + tree.size());

        // ---- exact lookup and prefixes ----------------------------------------
        check(tree.contains("car"), "car is a word");
        check(tree.contains("cart"), "cart is a word");
        check(!tree.contains("ca"), "ca is only a prefix, not a word");
        check(!tree.contains("carrot"), "carrot is not present");
        check(tree.startsWith("ca"), "ca is a prefix of something");
        check(!tree.startsWith("z"), "nothing starts with z");
        check(tree.contains("do"), "do is a word even though it is also a prefix");

        // ---- prefix counting --------------------------------------------------
        check(tree.countWithPrefix("ca") == 3, "three words start with ca");
        check(tree.countWithPrefix("car") == 2, "two start with car");
        check(tree.countWithPrefix("do") == 3, "do, dog and door all start with do");
        check(tree.countWithPrefix("d") == 3, "the bare d counts the same three");
        check(tree.countWithPrefix("z") == 0, "nothing starts with z");
        check(tree.countWithPrefix("") == 6, "the empty prefix counts everything");
        System.out.println("prefix ca   : " + tree.countWithPrefix("ca"));

        // ---- autocomplete -----------------------------------------------------
        check(tree.wordsWithPrefix("car").equals(List.of("car", "cart")), "car completes to two words");
        check(tree.wordsWithPrefix("do").equals(List.of("do", "dog", "door")),
                "do completes to three, in alphabetical order");
        check(tree.wordsWithPrefix("ca").equals(List.of("car", "cart", "cat")), "ca completes to three");
        check(tree.wordsWithPrefix("z").isEmpty(), "an unknown prefix completes to nothing");
        check(tree.wordsWithPrefix("car").size() == tree.countWithPrefix("car"),
                "the count and the listing agree");
        System.out.println("autocomplete: " + tree.wordsWithPrefix("ca"));

        // ---- longest common prefix --------------------------------------------
        check(tree.longestCommonPrefix().isEmpty(),
                "with car... and dog... the tree has two branches, so there is no common prefix");

        PrefixTree onlyCats = new PrefixTree();
        List.of("car", "cart", "cat").forEach(onlyCats::insert);
        check(onlyCats.longestCommonPrefix().equals("ca"), "these three share ca");
        PrefixTree one = new PrefixTree();
        one.insert("solo");
        check(one.longestCommonPrefix().equals("solo"), "a single word is its own common prefix");

        // ---- wildcard matching ------------------------------------------------
        check(tree.matching("ca.").equals(List.of("car", "cat")), "ca. matches the three letter words");
        check(tree.matching("c..").equals(List.of("car", "cat")), "c.. matches the same pair");
        check(tree.matching("...").equals(List.of("car", "cat", "dog")), "three letters only");
        check(tree.matching("do.").equals(List.of("dog")), "do. matches dog, not door");
        check(tree.matching("do..").equals(List.of("door")), "do.. matches door");
        check(tree.matching("....").equals(List.of("cart", "door")), "four letter words");
        check(tree.matching("z..").isEmpty(), "an unknown start matches nothing");
        System.out.println("pattern ca. : " + tree.matching("ca."));

        // ---- thousands of words, to show the shape holds ----------------------
        PrefixTree big = new PrefixTree();
        for (int i = 0; i < 5_000; i++) {
            big.insert(String.format("word%04d", i));
        }
        check(big.size() == 5_000, "five thousand distinct words");
        check(big.countWithPrefix("word") == 5_000, "all of them share the word prefix");
        check(big.countWithPrefix("word00") == 100, "word00 narrows it to a hundred");
        check(big.wordsWithPrefix("word000").size() == 10, "and word000 to ten");
        check(big.contains("word4999") && !big.contains("word5000"), "the boundaries are right");
        check(big.longestCommonPrefix().equals("word"), "the shared prefix is word");
        System.out.println("big trie    : " + big.size() + " words, common prefix \""
                + big.longestCommonPrefix() + "\"");

        // ---- bad input --------------------------------------------------------
        for (String bad : new String[] {"", null}) {
            try {
                tree.insert(bad);
                throw new AssertionError("should have been rejected: " + bad);
            } catch (IllegalArgumentException expected) {
                System.out.println("rejected    : " + expected.getMessage());
            }
        }
        System.out.println("All checks passed.");
    }
}
