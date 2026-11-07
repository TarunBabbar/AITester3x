import java.util.Random;

/**
 * 112 - Longest palindromic substring, three ways.
 *
 * manacher() expands around every centre in O(n) by reusing the palindrome
 * already known to the right of the current centre. expandAroundCenter() is the
 * simple O(n^2) version, and bruteForce() checks every substring in O(n^3) to
 * keep the fast ones honest on random input.
 *
 * On ties the earliest longest palindrome wins, so the three implementations
 * agree on both length and content.
 *
 * Compile and run:
 *   javac LongestPalindromicSubstring.java
 *   java LongestPalindromicSubstring
 */
public class LongestPalindromicSubstring {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static String manacher(String text) {
        if (text.isEmpty()) {
            return "";
        }

        StringBuilder expanded = new StringBuilder("#");
        for (int index = 0; index < text.length(); index++) {
            expanded.append(text.charAt(index)).append('#');
        }

        int length = expanded.length();
        int[] radius = new int[length];
        int centre = 0;
        int right = 0;
        int bestCentre = 0;
        int bestRadius = 0;

        for (int index = 0; index < length; index++) {
            int mirror = 2 * centre - index;
            if (index < right) {
                radius[index] = Math.min(right - index, radius[mirror]);
            }

            while (index - radius[index] - 1 >= 0
                    && index + radius[index] + 1 < length
                    && expanded.charAt(index - radius[index] - 1) == expanded.charAt(index + radius[index] + 1)) {
                radius[index]++;
            }

            if (index + radius[index] > right) {
                centre = index;
                right = index + radius[index];
            }

            if (radius[index] > bestRadius) {
                bestRadius = radius[index];
                bestCentre = index;
            }
        }

        int start = (bestCentre - bestRadius) / 2;
        return text.substring(start, start + bestRadius);
    }

    static String expandAroundCenter(String text) {
        if (text.isEmpty()) {
            return "";
        }

        int bestStart = 0;
        int bestEnd = 1;

        for (int centre = 0; centre < text.length(); centre++) {
            int[] odd = expand(text, centre, centre);
            if (odd[1] - odd[0] > bestEnd - bestStart) {
                bestStart = odd[0];
                bestEnd = odd[1];
            }

            int[] even = expand(text, centre, centre + 1);
            if (even[1] - even[0] > bestEnd - bestStart) {
                bestStart = even[0];
                bestEnd = even[1];
            }
        }

        return text.substring(bestStart, bestEnd);
    }

    private static int[] expand(String text, int left, int right) {
        while (left >= 0 && right < text.length() && text.charAt(left) == text.charAt(right)) {
            left--;
            right++;
        }
        return new int[] {left + 1, right};
    }

    static String bruteForce(String text) {
        String best = "";
        for (int start = 0; start < text.length(); start++) {
            for (int end = text.length(); end > start; end--) {
                if (end - start <= best.length()) {
                    break;
                }
                if (isPalindrome(text, start, end)) {
                    best = text.substring(start, end);
                    break;
                }
            }
        }
        return best;
    }

    private static boolean isPalindrome(String text, int start, int end) {
        int left = start;
        int right = end - 1;
        while (left < right) {
            if (text.charAt(left) != text.charAt(right)) {
                return false;
            }
            left++;
            right--;
        }
        return true;
    }

    public static void main(String[] args) {
        // ---- known answers ---------------------------------------------------
        String[][] known = {
            {"babad", "bab"},
            {"cbbd", "bb"},
            {"a", "a"},
            {"", ""},
            {"aaaa", "aaaa"},
            {"abcda", "a"},
            {"forgeeksskeegfor", "geeksskeeg"},
            {"abacdfgdcaba", "aba"},
            {"racecar", "racecar"},
            {"abacabadabacaba", "abacabadabacaba"},
        };

        for (String[] pair : known) {
            String expected = pair[1];
            check(manacher(pair[0]).equals(expected), "manacher failed on " + pair[0]);
            check(expandAroundCenter(pair[0]).equals(expected), "expand failed on " + pair[0]);
            check(bruteForce(pair[0]).equals(expected), "brute force failed on " + pair[0]);
        }
        System.out.println("known cases  : 10 inputs agree across all three implementations");

        // ---- properties on fixed input ---------------------------------------
        String sample = "forgeeksskeegfor";
        String longest = manacher(sample);
        check(isPalindrome(longest, 0, longest.length()), "result must be a palindrome");
        check(sample.contains(longest), "result must be a substring");
        check(longest.length() == 10, "expected length 10, got " + longest.length());
        System.out.println("sample       : " + sample + " -> " + longest);

        // ---- ties are resolved the same way ---------------------------------
        check(manacher("abacdfgdcaba").equals("aba"), "earliest tie");
        check(expandAroundCenter("abacdfgdcaba").equals("aba"), "earliest tie, expanding");
        System.out.println("ties         : earliest longest palindrome wins");

        // ---- randomised cross-check ------------------------------------------
        Random random = new Random(20261106);
        String alphabet = "abc";
        int checked = 0;

        for (int trial = 0; trial < 1_000; trial++) {
            int length = random.nextInt(30);
            StringBuilder builder = new StringBuilder(length);
            for (int index = 0; index < length; index++) {
                builder.append(alphabet.charAt(random.nextInt(alphabet.length())));
            }
            String text = builder.toString();

            String fast = manacher(text);
            String middle = expandAroundCenter(text);
            String slow = bruteForce(text);

            check(fast.length() == slow.length(),
                    "manacher length mismatch on " + text + ": " + fast.length() + " vs " + slow.length());
            check(middle.length() == slow.length(),
                    "expand length mismatch on " + text + ": " + middle.length() + " vs " + slow.length());
            check(isPalindrome(fast, 0, fast.length()), "manacher returned a non-palindrome for " + text);
            check(text.contains(fast), "manacher returned a non-substring for " + text);
            checked++;
        }
        System.out.println("random check : " + checked + " strings match brute force in length");

        // ---- one repeated character ------------------------------------------
        String repeated = "a".repeat(5_000);
        check(manacher(repeated).length() == 5_000, "the whole run is a palindrome");
        check(expandAroundCenter(repeated).length() == 5_000, "expanding finds the same run");
        System.out.println("long run     : 5000 identical characters -> " + manacher(repeated).length());

        // ---- a large input, to show the linear behaviour ----------------------
        StringBuilder largeBuilder = new StringBuilder(200_000);
        for (int index = 0; index < 200_000; index++) {
            largeBuilder.append((char) ('a' + random.nextInt(2)));
        }
        String large = largeBuilder.toString();

        long started = System.nanoTime();
        String largeResult = manacher(large);
        long elapsed = System.nanoTime() - started;

        check(isPalindrome(largeResult, 0, largeResult.length()), "large result is a palindrome");
        check(largeResult.length() == expandAroundCenter(large).length(), "large result matches expanding");
        System.out.printf("large input  : 200,000 characters, longest %d, manacher %.1f ms%n",
                largeResult.length(), elapsed / 1_000_000.0);

        System.out.println("All checks passed.");
    }
}
