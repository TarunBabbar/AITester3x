import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 121 - Edit distance.
 *
 * The distance is a weighted Levenshtein: insertions, deletions, and
 * substitutions each carry their own positive integer cost. Symbols are
 * Unicode code points, so accents and emoji count as single characters. The
 * reconstruction walks the full table backwards to produce a match, insert,
 * delete, or substitute script, which is replayed against the source. Small
 * random inputs are cross-checked against an unmemoised recursive search.
 *
 * Compile and run:
 *   javac EditDistance.java
 *   java EditDistance
 */
public class EditDistance {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    record Costs(int insert, int delete, int substitute) {
        Costs {
            if (insert < 1 || delete < 1 || substitute < 1) {
                throw new IllegalArgumentException("costs must be positive integers");
            }
        }

        static final Costs UNIT = new Costs(1, 1, 1);
    }

    record Edit(String operation, String from, String to) {
        static Edit match(String symbol) {
            return new Edit("match", symbol, symbol);
        }

        static Edit substitute(String from, String to) {
            return new Edit("substitute", from, to);
        }

        static Edit insert(String symbol) {
            return new Edit("insert", null, symbol);
        }

        static Edit delete(String symbol) {
            return new Edit("delete", symbol, null);
        }
    }

    static String[] symbols(String text) {
        return text.codePoints().mapToObj(Character::toString).toArray(String[]::new);
    }

    static int[][] table(String left, String right, Costs costs) {
        String[] a = symbols(left);
        String[] b = symbols(right);
        int[][] values = new int[a.length + 1][b.length + 1];

        for (int i = 0; i <= a.length; i++) {
            values[i][0] = i * costs.delete();
        }
        for (int j = 0; j <= b.length; j++) {
            values[0][j] = j * costs.insert();
        }

        for (int i = 1; i <= a.length; i++) {
            for (int j = 1; j <= b.length; j++) {
                int substitution = values[i - 1][j - 1]
                        + (a[i - 1].equals(b[j - 1]) ? 0 : costs.substitute());
                values[i][j] = Math.min(
                        Math.min(values[i - 1][j] + costs.delete(), values[i][j - 1] + costs.insert()),
                        substitution);
            }
        }

        return values;
    }

    static int distance(String left, String right) {
        return distance(left, right, Costs.UNIT);
    }

    static int distance(String left, String right, Costs costs) {
        String[] a = symbols(left);
        String[] b = symbols(right);
        int[] previous = new int[b.length + 1];

        for (int j = 0; j <= b.length; j++) {
            previous[j] = j * costs.insert();
        }

        for (int i = 1; i <= a.length; i++) {
            int[] current = new int[b.length + 1];
            current[0] = i * costs.delete();
            for (int j = 1; j <= b.length; j++) {
                int substitution = previous[j - 1]
                        + (a[i - 1].equals(b[j - 1]) ? 0 : costs.substitute());
                current[j] = Math.min(
                        Math.min(previous[j] + costs.delete(), current[j - 1] + costs.insert()),
                        substitution);
            }
            previous = current;
        }

        return previous[b.length];
    }

    static List<Edit> editScript(String left, String right, Costs costs) {
        String[] a = symbols(left);
        String[] b = symbols(right);
        int[][] values = table(left, right, costs);
        List<Edit> edits = new ArrayList<>();
        int i = a.length;
        int j = b.length;

        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && a[i - 1].equals(b[j - 1]) && values[i][j] == values[i - 1][j - 1]) {
                edits.add(Edit.match(a[i - 1]));
                i--;
                j--;
            } else if (i > 0 && j > 0 && values[i][j] == values[i - 1][j - 1] + costs.substitute()) {
                edits.add(Edit.substitute(a[i - 1], b[j - 1]));
                i--;
                j--;
            } else if (i > 0 && values[i][j] == values[i - 1][j] + costs.delete()) {
                edits.add(Edit.delete(a[i - 1]));
                i--;
            } else {
                check(j > 0, "an insertion needs a target symbol");
                edits.add(Edit.insert(b[j - 1]));
                j--;
            }
        }

        Collections.reverse(edits);
        return edits;
    }

    static String apply(String source, List<Edit> edits) {
        String[] input = symbols(source);
        StringBuilder output = new StringBuilder();
        int position = 0;

        for (Edit edit : edits) {
            if (!edit.operation().equals("insert")) {
                check(position < input.length, "the script reads past the source");
                check(input[position].equals(edit.from()), "the script reads the expected symbol");
                position++;
            }

            if (edit.operation().equals("match")) {
                check(edit.from().equals(edit.to()), "a match keeps the symbol");
                output.append(edit.to());
            } else if (edit.operation().equals("substitute") || edit.operation().equals("insert")) {
                output.append(edit.to());
            }
        }

        check(position == input.length, "the script consumes the whole source");
        return output.toString();
    }

    static int scriptCost(List<Edit> edits, Costs costs) {
        int total = 0;
        for (Edit edit : edits) {
            switch (edit.operation()) {
                case "insert" -> total += costs.insert();
                case "delete" -> total += costs.delete();
                case "substitute" -> total += costs.substitute();
                default -> { }
            }
        }
        return total;
    }

    static double similarity(String left, String right) {
        int longest = Math.max(
                left.codePointCount(0, left.length()),
                right.codePointCount(0, right.length()));
        if (longest == 0) {
            return 1.0;
        }
        return 1.0 - (double) distance(left, right) / longest;
    }

    static int bruteForce(String left, String right) {
        return bruteForce(symbols(left), symbols(right), 0, 0);
    }

    static int bruteForce(String[] left, String[] right, int i, int j) {
        if (i == left.length) {
            return right.length - j;
        }
        if (j == right.length) {
            return left.length - i;
        }
        if (left[i].equals(right[j])) {
            return bruteForce(left, right, i + 1, j + 1);
        }
        return 1 + Math.min(
                bruteForce(left, right, i + 1, j),
                Math.min(
                        bruteForce(left, right, i, j + 1),
                        bruteForce(left, right, i + 1, j + 1)));
    }

    static String randomText(Random random, int length) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < length; index++) {
            builder.append("abcd".charAt(random.nextInt(4)));
        }
        return builder.toString();
    }

    public static void main(String[] args) {
        // ---- known cases -----------------------------------------------------
        check(distance("kitten", "sitting") == 3, "kitten/sitting");
        check(distance("flaw", "lawn") == 2, "flaw/lawn");
        check(distance("intention", "execution") == 5, "intention/execution");
        check(distance("", "") == 0, "both empty");
        check(distance("", "abc") == 3, "left empty");
        check(distance("abc", "") == 3, "right empty");
        check(distance("abc", "abc") == 0, "identical");
        System.out.println("known cases  : kitten -> sitting = " + distance("kitten", "sitting")
                + ", intention -> execution = " + distance("intention", "execution"));

        // ---- edit scripts ----------------------------------------------------
        String[][] pairs = {
            {"kitten", "sitting"},
            {"flaw", "lawn"},
            {"", "abc"},
            {"abc", ""},
            {"naïve", "naive"},
        };
        for (String[] pair : pairs) {
            List<Edit> edits = editScript(pair[0], pair[1], Costs.UNIT);
            check(apply(pair[0], edits).equals(pair[1]), "script rebuilds " + pair[0] + " -> " + pair[1]);
            check(scriptCost(edits, Costs.UNIT) == distance(pair[0], pair[1]),
                    "script cost matches the distance for " + pair[0] + " -> " + pair[1]);
            long changes = edits.stream().filter(edit -> !edit.operation().equals("match")).count();
            System.out.printf("  \"%s\" -> \"%s\": %d steps, %d changes%n",
                    pair[0], pair[1], edits.size(), changes);
        }

        // ---- weighted costs --------------------------------------------------
        Costs dearSubstitution = new Costs(1, 1, 5);
        check(distance("abc", "adc", dearSubstitution) == 2, "delete and insert beat a dear substitution");
        check(distance("abc", "adc", Costs.UNIT) == 1, "a cheap substitution is used");
        Costs expensiveInsert = new Costs(10, 1, 4);
        check(distance("abc", "bc", expensiveInsert) == 1, "a cheap deletion");
        check(distance("bc", "abc", expensiveInsert) == 10, "an expensive insertion");
        check(distance("kitten", "sitting", new Costs(2, 3, 1))
                == distance("sitting", "kitten", new Costs(3, 2, 1)),
                "swapping the inputs swaps insertion and deletion costs");

        List<Edit> weightedEdits = editScript("kitten", "sitting", dearSubstitution);
        check(apply("kitten", weightedEdits).equals("sitting"), "weighted script rebuilds the target");
        check(scriptCost(weightedEdits, dearSubstitution) == distance("kitten", "sitting", dearSubstitution),
                "weighted script cost matches the distance");
        System.out.println("weighted     : kitten -> sitting costs "
                + scriptCost(weightedEdits, dearSubstitution) + " with substitution 5");

        // ---- unicode ---------------------------------------------------------
        check(distance("naïve", "naive") == 1, "accented letters count once");
        check(distance("a😀b", "a😞b") == 1, "emoji surrogate pairs count once");
        check(distance("😀😀", "😀") == 1, "repeated emoji");
        check(distance("héllo", "hello") == 1, "another accent");
        List<Edit> emojiEdits = editScript("a😀b", "a😞b", Costs.UNIT);
        check(apply("a😀b", emojiEdits).equals("a😞b"), "emoji script rebuilds the target");
        System.out.println("unicode      : naïve -> naive = " + distance("naïve", "naive")
                + ", a😀b -> a😞b = " + distance("a😀b", "a😞b"));

        // ---- similarity ------------------------------------------------------
        check(similarity("abc", "abc") == 1.0, "identical strings are fully similar");
        check(similarity("", "") == 1.0, "two empty strings are similar");
        check(Math.abs(similarity("abc", "abd") - 2.0 / 3.0) < 1e-9, "one change in three symbols");
        check(similarity("abc", "xyz") == 0.0, "nothing shared");
        check(similarity("kitten", "sitting") > 0.5 && similarity("kitten", "sitting") < 1.0,
                "partial similarity");
        System.out.printf("similarity   : kitten -> sitting = %.4f%n", similarity("kitten", "sitting"));

        // ---- metric properties ----------------------------------------------
        Random random = new Random(121);
        for (int trial = 0; trial < 200; trial++) {
            String a = randomText(random, random.nextInt(9));
            String b = randomText(random, random.nextInt(9));
            String c = randomText(random, random.nextInt(9));
            check(distance(a, b) == distance(b, a), "symmetry at trial " + trial);
            check(distance(a, a) == 0, "identity at trial " + trial);
            check(distance(a, c) <= distance(a, b) + distance(b, c), "triangle inequality at trial " + trial);
        }
        System.out.println("metrics      : 200 random triples are symmetric and obey the triangle inequality");

        // ---- against brute force --------------------------------------------
        for (int trial = 0; trial < 150; trial++) {
            String a = randomText(random, random.nextInt(6));
            String b = randomText(random, random.nextInt(6));
            check(distance(a, b) == bruteForce(a, b),
                    "brute force mismatch for \"" + a + "\" / \"" + b + "\"");
        }
        System.out.println("brute force  : 150 random pairs match the recursive search");

        // ---- a large input ---------------------------------------------------
        String first = randomText(random, 2_000);
        String second = randomText(random, 2_000);
        long started = System.nanoTime();
        int large = distance(first, second);
        long elapsed = System.nanoTime() - started;
        check(distance(second, first) == large, "large distance is symmetric");
        check(large <= 4_000, "distance never exceeds the total length");
        System.out.printf("large input  : 2,000 x 2,000 symbols, distance %d in %.1f ms%n",
                large, elapsed / 1_000_000.0);

        // ---- validation ------------------------------------------------------
        int[][] badCosts = {{0, 1, 1}, {-1, 1, 1}, {1, 0, 1}, {1, 1, 0}, {1, 1, -5}};
        for (int[] values : badCosts) {
            boolean threw = false;
            try {
                new Costs(values[0], values[1], values[2]);
            } catch (IllegalArgumentException error) {
                threw = true;
            }
            check(threw, "non-positive costs rejected");
        }
        System.out.println("validation   : non-positive costs rejected");

        System.out.println("All checks passed.");
    }
}
