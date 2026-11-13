import java.util.Random;

/**
 * 120 - Longest common subsequence.
 *
 * The length uses a rolling row, the reconstruction walks a full table
 * backwards, and brute force enumerates subsequences of the shorter string for
 * small inputs. Characters are compared as-is, so this works for any text.
 *
 * Compile and run:
 *   javac LongestCommonSubsequence.java
 *   java LongestCommonSubsequence
 */
public class LongestCommonSubsequence {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static int[][] table(String left, String right) {
        int[][] values = new int[left.length() + 1][right.length() + 1];
        for (int i = 1; i <= left.length(); i++) {
            for (int j = 1; j <= right.length(); j++) {
                if (left.charAt(i - 1) == right.charAt(j - 1)) {
                    values[i][j] = values[i - 1][j - 1] + 1;
                } else {
                    values[i][j] = Math.max(values[i - 1][j], values[i][j - 1]);
                }
            }
        }
        return values;
    }

    static int length(String left, String right) {
        int[] previous = new int[right.length() + 1];
        for (int i = 1; i <= left.length(); i++) {
            int[] current = new int[right.length() + 1];
            for (int j = 1; j <= right.length(); j++) {
                if (left.charAt(i - 1) == right.charAt(j - 1)) {
                    current[j] = previous[j - 1] + 1;
                } else {
                    current[j] = Math.max(previous[j], current[j - 1]);
                }
            }
            previous = current;
        }
        return previous[right.length()];
    }

    static String sequence(String left, String right) {
        int[][] values = table(left, right);
        StringBuilder builder = new StringBuilder();
        int i = left.length();
        int j = right.length();

        while (i > 0 && j > 0) {
            if (left.charAt(i - 1) == right.charAt(j - 1)) {
                builder.append(left.charAt(i - 1));
                i--;
                j--;
            } else if (values[i - 1][j] >= values[i][j - 1]) {
                i--;
            } else {
                j--;
            }
        }

        return builder.reverse().toString();
    }

    static boolean isSubsequence(String needle, String haystack) {
        int position = 0;
        for (int index = 0; index < haystack.length() && position < needle.length(); index++) {
            if (haystack.charAt(index) == needle.charAt(position)) {
                position++;
            }
        }
        return position == needle.length();
    }

    static int bruteForce(String left, String right) {
        String smaller = left.length() <= right.length() ? left : right;
        String larger = left.length() <= right.length() ? right : left;
        int best = 0;

        for (int mask = 0; mask < (1 << smaller.length()); mask++) {
            StringBuilder builder = new StringBuilder();
            for (int index = 0; index < smaller.length(); index++) {
                if ((mask & (1 << index)) != 0) {
                    builder.append(smaller.charAt(index));
                }
            }
            String candidate = builder.toString();
            if (candidate.length() > best && isSubsequence(candidate, larger)) {
                best = candidate.length();
            }
        }

        return best;
    }

    public static void main(String[] args) {
        // ---- known cases -----------------------------------------------------
        check(length("AGGTAB", "GXTXAYB") == 4, "classic length");
        check(sequence("AGGTAB", "GXTXAYB").equals("GTAB"), "classic sequence");
        check(length("ABCBDAB", "BDCABA") == 4, "second classic length");
        check(length("kitten", "sitting") == 4, "kitten/sitting");
        check(length("abcdef", "abcdef") == 6, "identical strings");
        check(sequence("abcdef", "abcdef").equals("abcdef"), "identical sequence");
        check(length("abc", "def") == 0, "no common characters");
        check(sequence("abc", "def").isEmpty(), "empty sequence");
        System.out.println("known cases  : lengths and reconstructions match");

        // ---- empty inputs ----------------------------------------------------
        check(length("", "") == 0, "both empty");
        check(length("", "abc") == 0, "left empty");
        check(length("abc", "") == 0, "right empty");
        check(sequence("", "abc").isEmpty(), "empty reconstruction");
        check(sequence("abc", "").isEmpty(), "empty reconstruction");
        System.out.println("empty inputs : zero length and empty reconstruction");

        // ---- reconstruction properties --------------------------------------
        String[][] pairs = {
            {"AGGTAB", "GXTXAYB"},
            {"ABCBDAB", "BDCABA"},
            {"kitten", "sitting"},
            {"nave caf", "naive cafe"},
            {"the quick brown fox", "the quick brown cat"},
            {"aaaa", "aa"},
        };
        for (String[] pair : pairs) {
            String found = sequence(pair[0], pair[1]);
            check(found.length() == length(pair[0], pair[1]), "length of reconstruction");
            check(isSubsequence(found, pair[0]), "subsequence of the left input");
            check(isSubsequence(found, pair[1]), "subsequence of the right input");
            check(table(pair[0], pair[1])[pair[0].length()][pair[1].length()] == found.length(),
                    "table corner matches the length");
            System.out.println("  " + pair[0] + " / " + pair[1] + " -> " + found);
        }

        // ---- unicode ---------------------------------------------------------
        String accented = sequence("naïve café", "naive cafe");
        check(isSubsequence(accented, "naïve café"), "unicode subsequence");
        check(accented.length() == length("naïve café", "naive cafe"), "unicode length");
        System.out.println("unicode      : " + accented);

        // ---- randomised cross-check -----------------------------------------
        Random random = new Random(20261106);
        int checked = 0;

        for (int trial = 0; trial < 300; trial++) {
            int leftLength = random.nextInt(11);
            int rightLength = random.nextInt(11);
            StringBuilder leftBuilder = new StringBuilder();
            StringBuilder rightBuilder = new StringBuilder();
            for (int index = 0; index < leftLength; index++) {
                leftBuilder.append((char) ('a' + random.nextInt(3)));
            }
            for (int index = 0; index < rightLength; index++) {
                rightBuilder.append((char) ('a' + random.nextInt(3)));
            }
            String left = leftBuilder.toString();
            String right = rightBuilder.toString();

            int fast = length(left, right);
            int slow = bruteForce(left, right);
            check(fast == slow, "length mismatch for " + left + " / " + right);

            String found = sequence(left, right);
            check(found.length() == fast, "reconstruction length mismatch");
            check(isSubsequence(found, left), "reconstruction not in the left input");
            check(isSubsequence(found, right), "reconstruction not in the right input");
            checked++;
        }
        System.out.println("random check : " + checked + " pairs match brute force");

        // ---- a larger input --------------------------------------------------
        StringBuilder firstBuilder = new StringBuilder();
        StringBuilder secondBuilder = new StringBuilder();
        for (int index = 0; index < 2_000; index++) {
            firstBuilder.append((char) ('a' + random.nextInt(4)));
            secondBuilder.append((char) ('a' + random.nextInt(4)));
        }
        String first = firstBuilder.toString();
        String second = secondBuilder.toString();

        long started = System.nanoTime();
        int largeLength = length(first, second);
        long elapsed = System.nanoTime() - started;

        String largeSequence = sequence(first, second);
        check(largeSequence.length() == largeLength, "large reconstruction length");
        check(isSubsequence(largeSequence, first) && isSubsequence(largeSequence, second),
                "large reconstruction is a common subsequence");
        System.out.printf("large input  : 2,000 x 2,000 characters, LCS %d in %.1f ms%n",
                largeLength, elapsed / 1_000_000.0);

        // ---- symmetry --------------------------------------------------------
        check(length("abcdef", "ace") == length("ace", "abcdef"), "length is symmetric");
        check(sequence("abcdef", "ace").equals(sequence("ace", "abcdef")), "reconstruction is symmetric here");

        System.out.println("All checks passed.");
    }
}
