import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * 124 - Word ladder.
 *
 * Words become a graph by replacing one letter with a wildcard: every word
 * joins one pattern per position, and sharing a pattern means a one-letter
 * change. Breadth-first search finds the shortest transformation from the begin
 * word to the end word, a parent map rebuilds one shortest ladder, and a
 * bidirectional search expands the smaller frontier from both ends.
 *
 * Compile and run:
 *   javac WordLadder.java
 *   java WordLadder
 */
public class WordLadder {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static void validate(String begin, String end, List<String> words) {
        if (begin == null || begin.isEmpty()) {
            throw new IllegalArgumentException("begin must be a non-empty word");
        }
        if (end == null || end.isEmpty()) {
            throw new IllegalArgumentException("end must be a non-empty word");
        }
        if (words == null) {
            throw new IllegalArgumentException("words must not be null");
        }
        for (String word : words) {
            if (word == null || word.isEmpty()) {
                throw new IllegalArgumentException("every dictionary word must be non-empty");
            }
        }
    }

    static Map<String, List<String>> prepare(String begin, String end, List<String> words) {
        validate(begin, end, words);
        if (begin.length() != end.length()) {
            return null;
        }

        Set<String> dictionary = new HashSet<>();
        for (String word : words) {
            if (word.length() == begin.length()) {
                dictionary.add(word);
            }
        }
        if (!dictionary.contains(end)) {
            return null;
        }

        dictionary.add(begin);
        return buildPatterns(dictionary);
    }

    static Map<String, List<String>> buildPatterns(Set<String> dictionary) {
        Map<String, List<String>> patterns = new HashMap<>();
        for (String word : dictionary) {
            char[] letters = word.toCharArray();
            for (int index = 0; index < letters.length; index++) {
                char original = letters[index];
                letters[index] = '*';
                patterns.computeIfAbsent(new String(letters), unused -> new ArrayList<>()).add(word);
                letters[index] = original;
            }
        }
        return patterns;
    }

    static List<String> neighbours(String word, Map<String, List<String>> patterns) {
        List<String> result = new ArrayList<>();
        char[] letters = word.toCharArray();
        for (int index = 0; index < letters.length; index++) {
            char original = letters[index];
            letters[index] = '*';
            List<String> bucket = patterns.get(new String(letters));
            if (bucket != null) {
                for (String candidate : bucket) {
                    if (!candidate.equals(word)) {
                        result.add(candidate);
                    }
                }
            }
            letters[index] = original;
        }
        return result;
    }

    static int ladderLength(String begin, String end, List<String> words) {
        if (begin != null && begin.equals(end)) {
            validate(begin, end, words);
            return 1;
        }
        Map<String, List<String>> patterns = prepare(begin, end, words);
        if (patterns == null) {
            return 0;
        }

        Deque<String> queue = new ArrayDeque<>();
        Map<String, Integer> distances = new HashMap<>();
        queue.addLast(begin);
        distances.put(begin, 1);

        while (!queue.isEmpty()) {
            String word = queue.removeFirst();
            int steps = distances.get(word);
            for (String next : neighbours(word, patterns)) {
                if (next.equals(end)) {
                    return steps + 1;
                }
                if (distances.containsKey(next)) {
                    continue;
                }
                distances.put(next, steps + 1);
                queue.addLast(next);
            }
        }

        return 0;
    }

    static List<String> findLadder(String begin, String end, List<String> words) {
        List<String> path = new ArrayList<>();
        if (begin != null && begin.equals(end)) {
            validate(begin, end, words);
            path.add(begin);
            return path;
        }
        Map<String, List<String>> patterns = prepare(begin, end, words);
        if (patterns == null) {
            return path;
        }

        Map<String, String> parents = new HashMap<>();
        parents.put(begin, null);
        Deque<String> queue = new ArrayDeque<>();
        queue.addLast(begin);

        while (!queue.isEmpty()) {
            String word = queue.removeFirst();
            if (word.equals(end)) {
                break;
            }
            for (String next : neighbours(word, patterns)) {
                if (parents.containsKey(next)) {
                    continue;
                }
                parents.put(next, word);
                queue.addLast(next);
            }
        }

        if (!parents.containsKey(end)) {
            return path;
        }

        String current = end;
        while (current != null) {
            path.add(current);
            current = parents.get(current);
        }
        Collections.reverse(path);
        return path;
    }

    static int ladderLengthBidirectional(String begin, String end, List<String> words) {
        if (begin != null && begin.equals(end)) {
            validate(begin, end, words);
            return 1;
        }
        Map<String, List<String>> patterns = prepare(begin, end, words);
        if (patterns == null) {
            return 0;
        }

        Map<String, Integer> forwardVisited = new HashMap<>();
        Map<String, Integer> backwardVisited = new HashMap<>();
        List<String> forwardFrontier = new ArrayList<>();
        List<String> backwardFrontier = new ArrayList<>();

        forwardVisited.put(begin, 1);
        forwardFrontier.add(begin);
        backwardVisited.put(end, 1);
        backwardFrontier.add(end);

        while (!forwardFrontier.isEmpty() && !backwardFrontier.isEmpty()) {
            if (forwardFrontier.size() > backwardFrontier.size()) {
                Map<String, Integer> visitedTemporary = forwardVisited;
                forwardVisited = backwardVisited;
                backwardVisited = visitedTemporary;

                List<String> frontierTemporary = forwardFrontier;
                forwardFrontier = backwardFrontier;
                backwardFrontier = frontierTemporary;
            }

            List<String> nextFrontier = new ArrayList<>();
            for (String word : forwardFrontier) {
                int steps = forwardVisited.get(word);
                for (String next : neighbours(word, patterns)) {
                    if (forwardVisited.containsKey(next)) {
                        continue;
                    }
                    Integer other = backwardVisited.get(next);
                    if (other != null) {
                        return steps + other;
                    }
                    forwardVisited.put(next, steps + 1);
                    nextFrontier.add(next);
                }
            }
            forwardFrontier = nextFrontier;
        }

        return 0;
    }

    static boolean differsByOne(String left, String right) {
        if (left.length() != right.length()) {
            return false;
        }
        int differences = 0;
        for (int index = 0; index < left.length(); index++) {
            if (left.charAt(index) != right.charAt(index)) {
                differences++;
            }
        }
        return differences == 1;
    }

    static void validatePath(List<String> path, String begin, String end, List<String> words) {
        check(!path.isEmpty(), "a path exists");
        check(path.get(0).equals(begin), "the path starts at begin");
        check(path.get(path.size() - 1).equals(end), "the path ends at end");

        Set<String> dictionary = new HashSet<>(words);
        for (int index = 1; index < path.size(); index++) {
            check(differsByOne(path.get(index - 1), path.get(index)),
                    "consecutive words differ by one letter");
            check(dictionary.contains(path.get(index)) || path.get(index).equals(end),
                    "every intermediate word comes from the dictionary");
        }
    }

    static boolean rejects(Runnable action) {
        try {
            action.run();
            return false;
        } catch (IllegalArgumentException error) {
            return true;
        }
    }

    public static void main(String[] args) {
        // ---- the classic ladder ---------------------------------------------
        List<String> classic = List.of("hot", "dot", "dog", "lot", "log", "cog");
        check(ladderLength("hit", "cog", classic) == 5, "classic length");
        check(ladderLengthBidirectional("hit", "cog", classic) == 5, "classic bidirectional length");
        List<String> classicPath = findLadder("hit", "cog", classic);
        check(classicPath.size() == 5, "classic path length");
        validatePath(classicPath, "hit", "cog", classic);
        System.out.println("classic      : " + String.join(" -> ", classicPath));

        // ---- unreachable targets --------------------------------------------
        List<String> withoutEnd = List.of("hot", "dot", "dog", "lot", "log");
        check(ladderLength("hit", "cog", withoutEnd) == 0, "no path");
        check(ladderLengthBidirectional("hit", "cog", withoutEnd) == 0, "no path bidirectional");
        check(findLadder("hit", "cog", withoutEnd).isEmpty(), "empty path");
        check(ladderLength("hit", "cog", List.of("hot", "dot")) == 0, "target outside the dictionary");
        check(ladderLength("hit", "cogs", List.of("cogs")) == 0, "different word lengths");
        System.out.println("unreachable  : missing targets and mismatched lengths return zero");

        // ---- short ladders ---------------------------------------------------
        check(ladderLength("a", "b", List.of("b")) == 2, "one transformation");
        check(ladderLengthBidirectional("a", "b", List.of("b")) == 2, "one transformation bidirectional");
        check(String.join(",", findLadder("a", "b", List.of("b"))).equals("a,b"), "one-step path");
        check(ladderLength("abc", "abc", List.of()) == 1, "begin equals end");
        check(String.join(",", findLadder("abc", "abc", List.of())).equals("abc"), "a single-word path");
        System.out.println("short        : one-step and same-word ladders count correctly");

        // ---- a full hypercube -----------------------------------------------
        List<String> hypercube = new ArrayList<>();
        for (char first : new char[] {'a', 'b'}) {
            for (char second : new char[] {'a', 'b'}) {
                for (char third : new char[] {'a', 'b'}) {
                    for (char fourth : new char[] {'a', 'b'}) {
                        hypercube.add("" + first + second + third + fourth);
                    }
                }
            }
        }
        check(ladderLength("aaaa", "bbbb", hypercube) == 5, "four changes");
        check(ladderLengthBidirectional("aaaa", "bbbb", hypercube) == 5, "four changes bidirectional");
        List<String> hypercubePath = findLadder("aaaa", "bbbb", hypercube);
        check(hypercubePath.size() == 5, "hypercube path length");
        validatePath(hypercubePath, "aaaa", "bbbb", hypercube);
        System.out.println("hypercube    : " + String.join(" -> ", hypercubePath));

        // ---- random graphs against breadth-first search --------------------
        Random random = new Random(124);
        List<String> universe = new ArrayList<>();
        for (char first : new char[] {'a', 'b', 'c'}) {
            for (char second : new char[] {'a', 'b', 'c'}) {
                for (char third : new char[] {'a', 'b', 'c'}) {
                    universe.add("" + first + second + third);
                }
            }
        }

        for (int trial = 0; trial < 300; trial++) {
            List<String> words = new ArrayList<>();
            for (String word : universe) {
                if (random.nextBoolean()) {
                    words.add(word);
                }
            }
            String begin = universe.get(random.nextInt(universe.size()));
            String end = universe.get(random.nextInt(universe.size()));

            int expected = ladderLength(begin, end, words);
            check(ladderLengthBidirectional(begin, end, words) == expected,
                    "bidirectional mismatch at trial " + trial);

            List<String> path = findLadder(begin, end, words);
            if (expected == 0) {
                check(path.isEmpty(), "unexpected path at trial " + trial);
            } else {
                check(path.size() == expected, "path length mismatch at trial " + trial);
                validatePath(path, begin, end, words);
            }
        }
        System.out.println("random       : 300 begin/end pairs agree and every path is valid");

        // ---- a large dictionary ---------------------------------------------
        List<String> big = new ArrayList<>();
        for (int number = 0; number < 10_000; number++) {
            big.add(String.format("w%04d", number));
        }

        long started = System.nanoTime();
        int bigLength = ladderLengthBidirectional("w0000", "w9999", big);
        long elapsed = System.nanoTime() - started;

        check(bigLength == 5, "large ladder length");
        check(ladderLength("w0000", "w9999", big) == 5, "large plain breadth-first length");
        check(findLadder("w0000", "w9999", big).size() == 5, "large path");
        System.out.printf("large input  : 10,000 words, w0000 -> w9999 in %d words (%.1f ms bidirectional)%n",
                bigLength, elapsed / 1_000_000.0);

        // ---- validation ------------------------------------------------------
        check(rejects(() -> ladderLength("", "abc", List.of())), "empty begin rejected");
        check(rejects(() -> ladderLength("abc", "", List.of())), "empty end rejected");
        check(rejects(() -> ladderLength("abc", "abc", List.of(""))), "empty word rejected");
        check(rejects(() -> ladderLength(null, "abc", List.of())), "null begin rejected");
        check(rejects(() -> ladderLength("abc", null, List.of())), "null end rejected");
        check(rejects(() -> ladderLength("abc", "abc", null)), "null words rejected");
        check(rejects(() -> findLadder("", "abc", List.of())), "empty begin rejected in findLadder");
        check(rejects(() -> ladderLengthBidirectional("abc", "abc", List.of(""))),
                "empty word rejected in bidirectional");
        System.out.println("validation   : nulls, empty words, and empty endpoints rejected");

        System.out.println("All checks passed.");
    }
}
