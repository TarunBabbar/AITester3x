import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 57 - Regular expressions part two: flags, lookarounds, backreferences,
 * replacement with a function, and quantifier behaviour.
 *
 * Compile and run:
 *   javac AdvancedRegex.java
 *   java AdvancedRegex
 */
public class AdvancedRegex {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static List<String> findAll(Pattern pattern, String text, int group) {
        List<String> found = new ArrayList<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            found.add(matcher.group(group));
        }
        return found;
    }

    public static void main(String[] args) {
        // ---- flags -----------------------------------------------------------
        check(Pattern.compile("^abc$", Pattern.CASE_INSENSITIVE).matcher("ABC").matches(),
                "CASE_INSENSITIVE");
        check(Pattern.compile("^b.*$", Pattern.MULTILINE).matcher("a\nbc").find(),
                "MULTILINE makes ^ match at a line start");
        check(Pattern.compile("a.b", Pattern.DOTALL).matcher("a\nb").matches(),
                "DOTALL lets the dot match a newline");
        check(!Pattern.compile("a.b").matcher("a\nb").matches(),
                "without DOTALL the dot stops at the newline");
        check(Pattern.compile("a  # the a\n b  # then b", Pattern.COMMENTS).matcher("ab").matches(),
                "COMMENTS ignores whitespace and everything after #");

        // flags can also be turned on inline
        check(Pattern.compile("(?i)hello").matcher("HELLO").matches(), "inline (?i)");
        check(Pattern.compile("(?s)a.b").matcher("a\nb").matches(), "inline (?s) is DOTALL");

        // ---- lookahead and lookbehind ---------------------------------------
        check(findAll(Pattern.compile("(?<=\\$)\\d+"), "costs $42 and $7", 0).equals(List.of("42", "7")),
                "lookbehind matches the amount without the dollar sign");

        Pattern notFollowedByBar = Pattern.compile("foo(?!bar)");
        check(notFollowedByBar.matcher("foobaz").find(), "negative lookahead matches foobaz");
        check(!notFollowedByBar.matcher("foobar").find(), "and rejects foobar");

        check(findAll(Pattern.compile("\\w+(?=;)"), "one; two; three", 0).equals(List.of("one", "two")),
                "lookahead finds words before a semicolon");
        System.out.println("lookarounds : " + findAll(Pattern.compile("(?<=\\$)\\d+"), "costs $42 and $7", 0));

        // ---- backreferences --------------------------------------------------
        List<String> repeated = findAll(Pattern.compile("\\b(\\w+)\\s+\\1\\b"), "hello hello world world", 1);
        check(repeated.equals(List.of("hello", "world")), "\\1 refers back to group 1");
        check("bookkeeper".replaceAll("(.)\\1", "$1").equals("bokeper"),
                "a backreference works in the replacement as $1");

        // ---- named groups ----------------------------------------------------
        Pattern date = Pattern.compile("(?<year>\\d{4})-(?<month>\\d{2})");
        Matcher match = date.matcher("2026-09");
        check(match.matches(), "the whole input matches");
        check(match.group("year").equals("2026") && match.group("month").equals("09"), "named groups read back");
        check(date.matcher("2026-09").replaceAll("${month}/${year}").equals("09/2026"),
                "named groups work in the replacement");
        System.out.println("named groups: year=" + match.group("year") + " month=" + match.group("month"));

        // ---- replacing with a function ---------------------------------------
        Pattern word = Pattern.compile("\\b\\w+\\b");
        check(word.matcher("hello world").replaceAll(result -> result.group().toUpperCase())
                .equals("HELLO WORLD"), "replaceAll can transform each match");
        check(Pattern.compile("[aeiou]").matcher("banana")
                .replaceAll(result -> "[" + result.group().toUpperCase() + "]").equals("b[A]n[A]n[A]"),
                "and wrap each vowel");
        System.out.println("shouted     : " + word.matcher("hello world")
                .replaceAll(result -> result.group().toUpperCase()));

        // ---- appendReplacement / appendTail ----------------------------------
        Matcher card = Pattern.compile("\\d{4}-\\d{4}-\\d{4}-(\\d{4})").matcher("card 4111-1111-1111-1234");
        StringBuilder masked = new StringBuilder();
        while (card.find()) {
            card.appendReplacement(masked, "****-****-****-" + card.group(1));
        }
        card.appendTail(masked);
        check(masked.toString().equals("card ****-****-****-1234"), "manual replacement keeps the rest");
        System.out.println("masked      : " + masked);

        // ---- Pattern.quote ---------------------------------------------------
        check(Pattern.quote("a.b").equals("\\Qa.b\\E"), "quote wraps the literal");
        check(Pattern.compile(Pattern.quote("a.b")).matcher("a.b").matches(), "a quoted literal matches itself");
        check(Pattern.compile("a.b").matcher("axb").find(), "an unquoted dot matches any character");

        // ---- split, with and without lookahead -------------------------------
        check(Arrays.toString("a1b2c".split("\\d")).equals("[a, b, c]"), "split on a digit");
        check("a1b2c".split("\\d", 2).length == 2, "a positive limit caps the pieces");
        check("a1b2c".split("\\d", -1)[0].equals("a"), "a negative limit keeps trailing empties");
        check("a1b2c".split("(?=\\d)").length == 3, "a lookahead splits without consuming the digit");
        System.out.println("lookahead split: " + Arrays.toString("a1b2c".split("(?=\\d)")));

        // ---- greedy, reluctant and possessive --------------------------------
        check(Pattern.compile("<(.+)>").matcher("<a><b>").groupCount() == 1, "the greedy pattern compiles");
        check(Pattern.compile("<(.+)>").matcher("<a><b>").matches(), "greedy takes as much as it can");
        Matcher greedy = Pattern.compile("<(.+)>").matcher("<a><b>");
        greedy.find();
        check(greedy.group(1).equals("a><b"), "so the capture spans both tags");
        Matcher reluctant = Pattern.compile("<(.+?)>").matcher("<a><b>");
        reluctant.find();
        check(reluctant.group(1).equals("a"), "the reluctant version stops at the first >");

        check(Pattern.compile("a*+b").matcher("aaab").matches(), "a possessive quantifier still matches here");
        check(!Pattern.compile("a*+ab").matcher("aaab").matches(),
                "but it never gives a character back, so this fails");
        System.out.println("All checks passed.");
    }
}
