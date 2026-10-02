import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 88 - A unified diff: the longest common subsequence of two files decides which
 * lines are context, which were removed and which were added.
 *
 * The invariant worth trusting: taking every line that is not an addition
 * rebuilds the old file, and taking every line that is not a removal rebuilds
 * the new one.
 *
 * Compile and run:
 *   javac UnifiedDiff.java
 *   java UnifiedDiff
 */
public class UnifiedDiff {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    record DiffLine(char kind, String text) {
        @Override
        public String toString() {
            return kind + text;
        }
    }

    static List<DiffLine> diff(List<String> before, List<String> after) {
        // lcs[i][j] = length of the common subsequence of before[i..] and after[j..]
        int[][] lcs = new int[before.size() + 1][after.size() + 1];
        for (int i = before.size() - 1; i >= 0; i--) {
            for (int j = after.size() - 1; j >= 0; j--) {
                lcs[i][j] = before.get(i).equals(after.get(j))
                        ? lcs[i + 1][j + 1] + 1
                        : Math.max(lcs[i + 1][j], lcs[i][j + 1]);
            }
        }

        List<DiffLine> lines = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < before.size() && j < after.size()) {
            if (before.get(i).equals(after.get(j))) {
                lines.add(new DiffLine(' ', before.get(i)));
                i++;
                j++;
            } else if (lcs[i + 1][j] >= lcs[i][j + 1]) {
                lines.add(new DiffLine('-', before.get(i)));      // dropping this line keeps the LCS longer
                i++;
            } else {
                lines.add(new DiffLine('+', after.get(j)));
                j++;
            }
        }
        while (i < before.size()) {
            lines.add(new DiffLine('-', before.get(i)));
            i++;
        }
        while (j < after.size()) {
            lines.add(new DiffLine('+', after.get(j)));
            j++;
        }
        return lines;
    }

    /** Everything that is not an addition is the old file. */
    static List<String> oldSide(List<DiffLine> lines) {
        return lines.stream().filter(line -> line.kind() != '+').map(DiffLine::text).toList();
    }

    /** Everything that is not a removal is the new file. */
    static List<String> newSide(List<DiffLine> lines) {
        return lines.stream().filter(line -> line.kind() != '-').map(DiffLine::text).toList();
    }

    static int count(List<DiffLine> lines, char kind) {
        return (int) lines.stream().filter(line -> line.kind() == kind).count();
    }

    static String render(List<DiffLine> lines) {
        StringBuilder out = new StringBuilder();
        for (DiffLine line : lines) {
            out.append(line.kind()).append(line.text()).append('\n');
        }
        return out.toString();
    }

    static String unified(List<String> before, List<String> after,
                          String beforeName, String afterName) {
        List<DiffLine> lines = diff(before, after);
        return "--- " + beforeName + "\n"
                + "+++ " + afterName + "\n"
                + "@@ -1," + before.size() + " +1," + after.size() + " @@\n"
                + render(lines);
    }

    static List<String> lines(String text) {
        return List.of(text.split("\n"));
    }

    public static void main(String[] args) {
        // ---- the smallest interesting change --------------------------------
        List<String> before = List.of("alpha", "beta", "gamma");
        List<String> after = List.of("alpha", "BETA", "gamma");
        List<DiffLine> lines = diff(before, after);

        check(lines.size() == 4, "four lines: two context, one out, one in");
        check(lines.get(0).equals(new DiffLine(' ', "alpha")), "the first line is untouched");
        check(lines.get(1).equals(new DiffLine('-', "beta")), "beta was removed");
        check(lines.get(2).equals(new DiffLine('+', "BETA")), "BETA was added");
        check(lines.get(3).equals(new DiffLine(' ', "gamma")), "and gamma is untouched");
        System.out.println("render       :\n" + render(lines));

        // ---- the invariant ---------------------------------------------------
        check(oldSide(lines).equals(before), "the removals and context rebuild the old file");
        check(newSide(lines).equals(after), "the additions and context rebuild the new file");

        List<List<String>> samples = List.of(
                List.of("a", "b", "c"),
                List.of("a", "x", "c"),
                List.of("one", "two"),
                List.of("one", "two", "three"),
                List.of(),
                List.of("only"));
        for (List<String> left : samples) {
            for (List<String> right : samples) {
                List<DiffLine> result = diff(left, right);
                check(oldSide(result).equals(left), "rebuilding the old side works for every pair");
                check(newSide(result).equals(right), "and the new side too");

                // minimality: the context lines are exactly the LCS, so the changes
                // are everything else
                int context = count(result, ' ');
                check(count(result, '-') + count(result, '+')
                                == left.size() + right.size() - 2 * context,
                        "every non-context line is one change");
            }
        }
        System.out.println("invariant    : verified over " + (samples.size() * samples.size())
                + " pairs of files");

        // ---- no change at all ------------------------------------------------
        List<DiffLine> same = diff(before, before);
        check(count(same, ' ') == 3 && count(same, '+') == 0 && count(same, '-') == 0,
                "identical files produce only context");
        check(oldSide(same).equals(before) && newSide(same).equals(before), "and both sides match");

        // ---- pure insertion and pure deletion --------------------------------
        List<DiffLine> inserted = diff(List.of("a", "c"), List.of("a", "b", "c"));
        check(count(inserted, '+') == 1 && count(inserted, '-') == 0, "a pure insertion has no removals");
        check(oldSide(inserted).equals(List.of("a", "c")), "and rebuilds the old file");

        List<DiffLine> deleted = diff(List.of("a", "b", "c"), List.of("a", "c"));
        check(count(deleted, '-') == 1 && count(deleted, '+') == 0, "a pure deletion has no additions");
        check(newSide(deleted).equals(List.of("a", "c")), "and rebuilds the new file");

        // ---- empty files -----------------------------------------------------
        List<DiffLine> fromNothing = diff(List.of(), List.of("a", "b"));
        check(count(fromNothing, '+') == 2 && count(fromNothing, ' ') == 0, "everything is an addition");

        List<DiffLine> toNothing = diff(List.of("a", "b"), List.of());
        check(count(toNothing, '-') == 2 && count(toNothing, ' ') == 0, "everything is a removal");

        List<DiffLine> bothEmpty = diff(List.of(), List.of());
        check(bothEmpty.isEmpty(), "two empty files differ in nothing");

        // ---- a realistic edit ------------------------------------------------
        List<String> oldText = lines("""
                public class Greeter {
                    String greet(String name) {
                        return "Hello, " + name;
                    }
                }""");
        List<String> newText = lines("""
                public class Greeter {
                    String greet(String name) {
                        String trimmed = name.trim();
                        return "Hello, " + trimmed + "!";
                    }
                }""");

        List<DiffLine> edit = diff(oldText, newText);
        check(oldSide(edit).equals(oldText) && newSide(edit).equals(newText),
                "the source edit round trips as well");
        check(count(edit, '-') == 1 && count(edit, '+') == 2,
                "one line replaced by two: " + count(edit, '-') + " out, " + count(edit, '+') + " in");
        check(count(edit, ' ') == 4, "with four lines of context");

        String patch = unified(oldText, newText, "Greeter.java", "Greeter.java");
        check(patch.startsWith("--- Greeter.java\n+++ Greeter.java\n"), "the patch header names the file");
        check(patch.contains("@@ -1,5 +1,6 @@\n"), "and the hunk header counts both sides");
        check(patch.contains("-        return \"Hello, \" + name;"), "the removed line is prefixed with a minus");
        check(patch.contains("+        String trimmed = name.trim();"), "and the additions with a plus");
        System.out.println("patch        :\n" + patch);

        // ---- whitespace counts as a change -----------------------------------
        List<DiffLine> trailing = diff(List.of("value"), List.of("value "));
        check(count(trailing, '-') == 1 && count(trailing, '+') == 1,
                "a trailing space is a real difference");
        System.out.println("All checks passed.");
    }
}
