import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 39 - A command line tool: parse flags, read a file, count words and report the
 * most frequent ones. The parsing and counting are plain methods so they can be
 * tested directly, exactly as a real CLI would be.
 *
 * Compile and run:
 *   javac CommandLineTool.java
 *   java CommandLineTool --file words.txt --top 3 --min-length 4 -i
 */
public class CommandLineTool {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    record Options(String path, int top, int minLength, boolean ignoreCase) {
    }

    static Options parse(String[] args) {
        String path = null;
        int top = 5;
        int minLength = 1;
        boolean ignoreCase = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--file" -> path = value(args, ++i, "--file");
                case "--top" -> top = Integer.parseInt(value(args, ++i, "--top"));
                case "--min-length" -> minLength = Integer.parseInt(value(args, ++i, "--min-length"));
                case "-i", "--ignore-case" -> ignoreCase = true;
                case "-h", "--help" -> {
                    System.out.println("usage: --file <path> [--top N] [--min-length N] [-i]");
                    System.exit(0);
                }
                default -> throw new IllegalArgumentException("unknown option: " + args[i]);
            }
        }
        if (top < 1) {
            throw new IllegalArgumentException("--top must be at least 1");
        }
        return new Options(path, top, minLength, ignoreCase);
    }

    private static String value(String[] args, int index, String option) {
        if (index >= args.length) {
            throw new IllegalArgumentException(option + " needs a value");
        }
        return args[index];
    }

    static Map<String, Integer> countWords(List<String> lines, Options options) {
        Map<String, Integer> counts = new TreeMap<>();
        for (String line : lines) {
            for (String raw : line.split("[^A-Za-z0-9']+")) {
                if (raw.isEmpty()) {
                    continue;
                }
                String word = options.ignoreCase() ? raw.toLowerCase() : raw;
                if (word.length() < options.minLength()) {
                    continue;
                }
                counts.merge(word, 1, Integer::sum);
            }
        }
        return counts;
    }

    /** Most frequent first; ties broken alphabetically so the output is stable. */
    static List<Map.Entry<String, Integer>> top(Map<String, Integer> counts, int limit) {
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(limit)
                .toList();
    }

    public static void main(String[] args) throws IOException {
        Path working = Files.createTempDirectory("java-cli-");
        Path file = working.resolve("words.txt");
        try {
            Files.writeString(file, """
                    the quick brown fox
                    the lazy dog and the Fox
                    quick quick brown
                    """);

            String[] commandLine = {
                    "--file", file.toString(), "--top", "3", "--min-length", "4", "-i"
            };
            Options options = parse(commandLine);
            check(options.path().equals(file.toString()), "path parsed");
            check(options.top() == 3, "top parsed");
            check(options.minLength() == 4, "min length parsed");
            check(options.ignoreCase(), "the -i flag sets ignore case");

            Map<String, Integer> counts = countWords(Files.readAllLines(file), options);
            check(!counts.containsKey("the"), "three-letter words were filtered out");
            check(!counts.containsKey("fox"), "fox is only three letters, so --min-length 4 drops it");
            check(counts.get("quick") == 3, "-i merged Quick with quick");
            check(counts.get("brown") == 2, "brown appears twice");
            check(counts.get("lazy") == 1, "lazy is the only word that appears once");

            List<Map.Entry<String, Integer>> leaders = top(counts, 3);
            check(leaders.size() == 3, "three leaders requested");
            check(leaders.get(0).getKey().equals("quick"), "quick is the most frequent");
            check(leaders.get(1).getKey().equals("brown"), "brown is next");
            check(leaders.get(2).getKey().equals("lazy"), "lazy is third");

            // equal counts fall back to alphabetical order so the output is stable
            Map<String, Integer> tied = new TreeMap<>(Map.of("zebra", 2, "apple", 2, "mango", 1));
            List<Map.Entry<String, Integer>> ordered = top(tied, 2);
            check(ordered.get(0).getKey().equals("apple") && ordered.get(1).getKey().equals("zebra"),
                    "a tie on count is broken alphabetically");

            System.out.println("file        : " + file.getFileName());
            for (Map.Entry<String, Integer> entry : leaders) {
                System.out.printf("  %-8s %d%n", entry.getKey(), entry.getValue());
            }

            // case-sensitive counting keeps the variants apart
            Options sensitive = parse(new String[] {"--top", "1"});
            Map<String, Integer> exact = countWords(Files.readAllLines(file), sensitive);
            check(exact.containsKey("Fox") && exact.containsKey("fox"), "without -i the cases stay apart");

            // bad input fails loudly rather than guessing
            try {
                parse(new String[] {"--nope"});
                throw new AssertionError("an unknown option should fail");
            } catch (IllegalArgumentException expected) {
                System.out.println("bad option  : " + expected.getMessage());
            }
            try {
                parse(new String[] {"--file"});
                throw new AssertionError("a missing value should fail");
            } catch (IllegalArgumentException expected) {
                System.out.println("missing arg : " + expected.getMessage());
            }
            try {
                parse(new String[] {"--top", "0"});
                throw new AssertionError("top must be positive");
            } catch (IllegalArgumentException expected) {
                System.out.println("bad value   : " + expected.getMessage());
            }
        } finally {
            try (var paths = Files.walk(working)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
        System.out.println("All checks passed (temp tree cleaned up).");
    }
}
