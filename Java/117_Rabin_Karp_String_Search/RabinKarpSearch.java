import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 117 - Rabin-Karp rolling-hash string search.
 *
 * Hashes the pattern and the first window, then slides the window one character
 * at a time by removing the outgoing character and adding the incoming one. A
 * hash match is only a candidate, so the characters are compared to rule out
 * collisions. Expected O(n + m) for a good base and modulus.
 *
 * Brute force checks the random cases, and the rolling hash is compared against
 * recomputing hashes from scratch.
 *
 * Compile and run:
 *   javac RabinKarpSearch.java
 *   java RabinKarpSearch
 */
public class RabinKarpSearch {

    static final long BASE = 257;
    static final long MODULUS = 1_000_000_007;

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static long hash(String value) {
        long result = 0;
        for (int index = 0; index < value.length(); index++) {
            result = (result * BASE + value.charAt(index)) % MODULUS;
        }
        return result;
    }

    static List<Integer> search(String text, String pattern) {
        List<Integer> positions = new ArrayList<>();
        int textLength = text.length();
        int patternLength = pattern.length();

        if (patternLength == 0) {
            for (int index = 0; index <= textLength; index++) {
                positions.add(index);
            }
            return positions;
        }

        if (patternLength > textLength) {
            return positions;
        }

        long highPower = 1;
        for (int index = 0; index < patternLength - 1; index++) {
            highPower = (highPower * BASE) % MODULUS;
        }

        long patternHash = 0;
        long windowHash = 0;
        for (int index = 0; index < patternLength; index++) {
            patternHash = (patternHash * BASE + pattern.charAt(index)) % MODULUS;
            windowHash = (windowHash * BASE + text.charAt(index)) % MODULUS;
        }

        for (int start = 0; start + patternLength <= textLength; start++) {
            if (windowHash == patternHash && text.regionMatches(start, pattern, 0, patternLength)) {
                positions.add(start);
            }

            if (start + patternLength < textLength) {
                windowHash = (windowHash - text.charAt(start) * highPower % MODULUS + MODULUS) % MODULUS;
                windowHash = (windowHash * BASE + text.charAt(start + patternLength)) % MODULUS;
            }
        }

        return positions;
    }

    static List<Integer> bruteForce(String text, String pattern) {
        List<Integer> positions = new ArrayList<>();
        if (pattern.isEmpty()) {
            for (int index = 0; index <= text.length(); index++) {
                positions.add(index);
            }
            return positions;
        }
        for (int start = 0; start + pattern.length() <= text.length(); start++) {
            if (text.regionMatches(start, pattern, 0, pattern.length())) {
                positions.add(start);
            }
        }
        return positions;
    }

    public static void main(String[] args) {
        // ---- known cases -----------------------------------------------------
        check(search("AABAACAADAABAABA", "AABA").equals(List.of(0, 9, 12)), "classic example");
        check(search("aaaaa", "aa").equals(List.of(0, 1, 2, 3)), "overlapping matches");
        check(search("abc", "abcdef").isEmpty(), "pattern longer than text");
        check(search("abc", "d").isEmpty(), "no match");
        check(search("abc", "abc").equals(List.of(0)), "whole text");
        check(search("", "a").isEmpty(), "empty text");
        check(search("abc", "").equals(List.of(0, 1, 2, 3)), "empty pattern matches every position");
        check(search("", "").equals(List.of(0)), "both empty");
        System.out.println("known cases  : classic, overlapping, empty, and oversized patterns");

        // ---- one-character and repeated patterns -----------------------------
        check(search("abababab", "abab").equals(List.of(0, 2, 4)), "repeated pattern");
        check(search("aaaa", "a").equals(List.of(0, 1, 2, 3)), "single character");
        check(search("zzzzzz", "zz").equals(List.of(0, 1, 2, 3, 4)), "all same character");
        System.out.println("simple cases : repeated and single-character patterns");

        // ---- unicode ---------------------------------------------------------
        check(search("héllo héllo", "héllo").equals(List.of(0, 6)), "accented characters");
        check(search("αβγαβγ", "βγ").equals(List.of(1, 4)), "greek characters");
        System.out.println("unicode      : accented and greek text searched");

        // ---- hash properties -------------------------------------------------
        check(hash("") == 0, "empty hash is zero");
        check(hash("a") != hash("b"), "different strings usually hash differently");
        check(hash("abc") == hash("abc"), "hashing is deterministic");
        check(hash("abc") != hash("abd"), "a final character changes the hash");
        System.out.println("hash         : deterministic, empty-aware, and sensitive");

        // ---- rolling hash equals a fresh hash --------------------------------
        Random random = new Random(20261106);
        StringBuilder sampleBuilder = new StringBuilder();
        for (int index = 0; index < 200; index++) {
            sampleBuilder.append((char) ('a' + random.nextInt(4)));
        }
        String sample = sampleBuilder.toString();

        long window = 0;
        long highPower = 1;
        for (int index = 0; index < 5; index++) {
            window = (window * BASE + sample.charAt(index)) % MODULUS;
            if (index < 4) {
                highPower = (highPower * BASE) % MODULUS;
            }
        }

        boolean rollingMatches = true;
        for (int start = 0; start + 5 <= sample.length(); start++) {
            if (window != hash(sample.substring(start, start + 5))) {
                rollingMatches = false;
                break;
            }
            if (start + 5 < sample.length()) {
                window = (window - sample.charAt(start) * highPower % MODULUS + MODULUS) % MODULUS;
                window = (window * BASE + sample.charAt(start + 5)) % MODULUS;
            }
        }
        check(rollingMatches, "rolling hash must equal a fresh hash at every window");
        System.out.println("rolling hash : 196 windows match freshly computed hashes");

        // ---- randomised cross-check -----------------------------------------
        int checked = 0;
        for (int trial = 0; trial < 500; trial++) {
            int textLength = random.nextInt(50);
            StringBuilder textBuilder = new StringBuilder(textLength);
            for (int index = 0; index < textLength; index++) {
                textBuilder.append((char) ('a' + random.nextInt(3)));
            }
            String text = textBuilder.toString();

            int patternLength = random.nextInt(6);
            StringBuilder patternBuilder = new StringBuilder(patternLength);
            for (int index = 0; index < patternLength; index++) {
                patternBuilder.append((char) ('a' + random.nextInt(3)));
            }
            String pattern = patternBuilder.toString();

            List<Integer> fast = search(text, pattern);
            List<Integer> slow = bruteForce(text, pattern);
            check(fast.equals(slow), "mismatch for pattern " + pattern + " in " + text);
            for (int position : fast) {
                check(text.startsWith(pattern, position), "reported match must be real");
            }
            checked++;
        }
        System.out.println("random check : " + checked + " text/pattern pairs match brute force");

        // ---- a large input ---------------------------------------------------
        StringBuilder largeBuilder = new StringBuilder(200_000);
        for (int index = 0; index < 200_000; index++) {
            largeBuilder.append(index % 2 == 0 ? 'a' : 'b');
        }
        String large = largeBuilder.toString();

        long started = System.nanoTime();
        List<Integer> largeMatches = search(large, "aba");
        long elapsed = System.nanoTime() - started;

        check(largeMatches.size() == 99_999, "expected 99,999 overlapping matches, got " + largeMatches.size());
        check(largeMatches.get(0) == 0, "first match at the start");
        check(largeMatches.get(largeMatches.size() - 1) == 199_996, "last match near the end");
        System.out.printf("large input  : 200,000 characters, %d matches in %.1f ms%n",
                largeMatches.size(), elapsed / 1_000_000.0);

        // ---- pattern at both ends --------------------------------------------
        check(search("abcXabc", "abc").equals(List.of(0, 4)), "matches at both ends");
        check(search("abcabcabc", "abcabc").equals(List.of(0, 3)), "overlapping long pattern");

        System.out.println("All checks passed.");
    }
}
