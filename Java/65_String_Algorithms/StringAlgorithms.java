import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 65 - String algorithms: Knuth-Morris-Pratt search, Levenshtein edit distance,
 * longest palindromic substring and anagram grouping.
 *
 * Compile and run:
 *   javac StringAlgorithms.java
 *   java StringAlgorithms
 */
public class StringAlgorithms {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    // ------------------------------------------------------- KMP (search)
    /** The failure table: how far to fall back when a mismatch happens. */
    static int[] failureTable(String pattern) {
        int[] table = new int[pattern.length()];
        int matched = 0;
        for (int i = 1; i < pattern.length(); ) {
            if (pattern.charAt(i) == pattern.charAt(matched)) {
                table[i++] = ++matched;
            } else if (matched > 0) {
                matched = table[matched - 1];
            } else {
                table[i++] = 0;
            }
        }
        return table;
    }

    /** Every start index where the pattern occurs. Never re-reads a character. */
    static List<Integer> kmpSearch(String text, String pattern) {
        List<Integer> found = new ArrayList<>();
        if (pattern.isEmpty() || pattern.length() > text.length()) {
            return found;
        }
        int[] table = failureTable(pattern);
        int matched = 0;
        for (int i = 0; i < text.length(); i++) {
            while (matched > 0 && text.charAt(i) != pattern.charAt(matched)) {
                matched = table[matched - 1];
            }
            if (text.charAt(i) == pattern.charAt(matched)) {
                matched++;
            }
            if (matched == pattern.length()) {
                found.add(i - matched + 1);
                matched = table[matched - 1];
            }
        }
        return found;
    }

    static List<Integer> naiveSearch(String text, String pattern) {
        List<Integer> found = new ArrayList<>();
        for (int i = 0; i + pattern.length() <= text.length(); i++) {
            if (text.startsWith(pattern, i)) {
                found.add(i);
            }
        }
        return found;
    }

    // ------------------------------------------- Levenshtein (edit distance)
    static int editDistance(String left, String right) {
        int[][] table = new int[left.length() + 1][right.length() + 1];
        for (int i = 0; i <= left.length(); i++) {
            table[i][0] = i;
        }
        for (int j = 0; j <= right.length(); j++) {
            table[0][j] = j;
        }
        for (int i = 1; i <= left.length(); i++) {
            for (int j = 1; j <= right.length(); j++) {
                int substitution = table[i - 1][j - 1] + (left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1);
                int deletion = table[i - 1][j] + 1;
                int insertion = table[i][j - 1] + 1;
                table[i][j] = Math.min(substitution, Math.min(deletion, insertion));
            }
        }
        return table[left.length()][right.length()];
    }

    // ------------------------------------ longest palindromic substring
    static String longestPalindrome(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        int bestStart = 0;
        int bestLength = 1;
        for (int centre = 0; centre < text.length(); centre++) {
            int odd = expandAround(text, centre, centre);
            int even = expandAround(text, centre, centre + 1);
            int length = Math.max(odd, even);
            if (length > bestLength) {
                bestLength = length;
                bestStart = centre - (length - 1) / 2;
            }
        }
        return text.substring(bestStart, bestStart + bestLength);
    }

    private static int expandAround(String text, int left, int right) {
        while (left >= 0 && right < text.length() && text.charAt(left) == text.charAt(right)) {
            left--;
            right++;
        }
        return right - left - 1;
    }

    // ------------------------------------------------- anagram grouping
    static Map<String, List<String>> groupAnagrams(List<String> words) {
        Map<String, List<String>> groups = new TreeMap<>();
        for (String word : words) {
            char[] letters = word.toCharArray();
            Arrays.sort(letters);
            groups.computeIfAbsent(new String(letters), key -> new ArrayList<>()).add(word);
        }
        return groups;
    }

    public static void main(String[] args) {
        // ---- KMP -------------------------------------------------------------
        check(failureTable("abab").length == 4, "the failure table is as long as the pattern");
        check(Arrays.equals(failureTable("abab"), new int[] {0, 0, 1, 2}), "the failure table values");
        check(Arrays.equals(failureTable("aabaa"), new int[] {0, 1, 0, 1, 2}), "overlapping prefixes are found");

        check(kmpSearch("abababab", "abab").equals(List.of(0, 2, 4)), "overlapping matches are found");
        check(kmpSearch("aaaaa", "aa").equals(List.of(0, 1, 2, 3)), "every overlapping pair");
        check(kmpSearch("hello world", "world").equals(List.of(6)), "a single match");
        check(kmpSearch("hello", "zzz").isEmpty(), "no match");
        check(kmpSearch("abc", "abcd").isEmpty(), "a pattern longer than the text");
        check(kmpSearch("anything", "").isEmpty(), "an empty pattern finds nothing");

        String[][] pairs = {
                {"aabaabaaaabaabaaab", "aabaa"},
                {"mississippi", "issi"},
                {"aaaaaaaaab", "aaab"},
                {"the quick brown fox", "own"},
                {"abcabcabcabc", "abcabc"},
        };
        for (String[] pair : pairs) {
            check(kmpSearch(pair[0], pair[1]).equals(naiveSearch(pair[0], pair[1])),
                    "KMP agrees with a naive search for " + pair[1]);
        }
        System.out.println("kmp         : abab in abababab at " + kmpSearch("abababab", "abab"));

        // ---- edit distance ---------------------------------------------------
        check(editDistance("kitten", "sitting") == 3, "kitten -> sitting is 3 edits");
        check(editDistance("flaw", "lawn") == 2, "flaw -> lawn is 2");
        check(editDistance("same", "same") == 0, "identical strings need no edits");
        check(editDistance("", "abc") == 3, "an empty string needs three insertions");
        check(editDistance("abc", "") == 3, "and the other way round");
        check(editDistance("abc", "abd") == 1, "one substitution");
        check(editDistance("saturday", "sunday") == 3, "the classic weekend example");
        System.out.println("distance    : kitten -> sitting = " + editDistance("kitten", "sitting"));

        // ---- longest palindrome ----------------------------------------------
        check(longestPalindrome("babad").equals("bab"), "babad has bab and aba; the first wins");
        check(longestPalindrome("cbbd").equals("bb"), "cbbd has bb");
        check(longestPalindrome("a").equals("a"), "a single character is a palindrome");
        check(longestPalindrome("").isEmpty(), "empty in, empty out");
        check(longestPalindrome("ac").equals("a"), "with no repeat, one character is the best there is");

        String longCase = longestPalindrome("forgeeksskeegfor");
        check(longCase.length() == 10, "the longest palindrome here is ten characters");
        check(longCase.contentEquals(new StringBuilder(longCase).reverse()),
                "and it really is a palindrome");
        System.out.println("palindrome  : " + longCase);

        // every result must actually be a palindrome
        for (String sample : new String[] {"babad", "cbbd", "abacdfgdcaba", "racecar", "abb"}) {
            String found = longestPalindrome(sample);
            check(found.contentEquals(new StringBuilder(found).reverse()),
                    "the answer for " + sample + " is a palindrome");
            check(sample.contains(found), "and it comes from the input");
        }

        // ---- anagram grouping ------------------------------------------------
        Map<String, List<String>> groups = groupAnagrams(
                List.of("eat", "tea", "tan", "ate", "nat", "bat"));
        check(groups.size() == 3, "three anagram groups");
        check(groups.get("aet").equals(List.of("eat", "tea", "ate")), "the a/e/t family");
        check(groups.get("ant").equals(List.of("tan", "nat")), "the a/n/t family");
        check(groups.get("abt").equals(List.of("bat")), "a family of one");
        System.out.println("anagrams    : " + groups);

        check(groupAnagrams(List.of("abc", "bca", "cab", "xyz")).size() == 2, "two groups here");
        check(groupAnagrams(List.of()).isEmpty(), "no words, no groups");
        System.out.println("All checks passed.");
    }
}
