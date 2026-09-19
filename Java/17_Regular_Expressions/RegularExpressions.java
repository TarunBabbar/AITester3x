import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 17 - Regular expressions: Pattern, Matcher, named groups, find, replaceAll and split.
 *
 * Compile and run:
 *   javac RegularExpressions.java
 *   java RegularExpressions
 */
public class RegularExpressions {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) {
        Pattern email = Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[a-z]{2,}$");
        check(email.matcher("alice@example.com").matches(), "a valid email matches");
        check(!email.matcher("alice@example").matches(), "a missing TLD does not match");
        check(!email.matcher("not-an-email").matches(), "plain text does not match");

        Pattern date = Pattern.compile("(?<year>\\d{4})-(?<month>\\d{2})-(?<day>\\d{2})");
        Matcher dates = date.matcher("released 2026-01-05 and updated 2026-03-16");
        int found = 0;
        while (dates.find()) {
            found++;
            if (found == 1) {
                check(dates.group().equals("2026-01-05"), "group() is the whole match");
                check(dates.group("year").equals("2026"), "named group year");
                check(dates.group("month").equals("01"), "named group month");
                check(dates.group(3).equals("05"), "numbered group day");
            }
        }
        check(found == 2, "two dates were found");

        check("a1b22c333".replaceAll("\\d+", "#").equals("a#b#c#"), "replaceAll collapses digits");
        check("one,two;;three".split("[,;]+").length == 3, "split handles several delimiters");
        check(Pattern.matches("\\d{3}-\\d{4}", "555-1234"), "Pattern.matches is a shortcut");

        String log = "ERROR 503; INFO 200; ERROR 404";
        Matcher codes = Pattern.compile("ERROR (\\d+)").matcher(log);
        StringBuilder collected = new StringBuilder();
        while (codes.find()) {
            collected.append(codes.group(1)).append(' ');
        }
        check(collected.toString().trim().equals("503 404"), "extracting captures in a loop");

        check("Hello World".replaceAll("(?i)world", "Java").equals("Hello Java"),
                "case-insensitive replace");
        check("  padded  ".matches("\\s+.*\\s+"), "matches looks at the whole input");
        check("abc123".matches("[a-z]+\\d+"), "character classes with quantifiers");

        System.out.println("dates found    : " + found);
        System.out.println("error codes    : " + collected.toString().trim());
        System.out.println("tokenised      : " + java.util.Arrays.toString("one,two;;three".split("[,;]+")));
        System.out.println("All checks passed.");
    }
}
