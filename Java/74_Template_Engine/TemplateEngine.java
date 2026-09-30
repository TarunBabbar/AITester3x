import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 74 - Templating and formatting: a tiny template engine with placeholders,
 * loops and conditionals, plus the printf style format specifiers.
 *
 * Supports {{name}}, {{#if flag}}...{{/if}} and {{#each list}}...{{/each}},
 * where the current item is {{this}}. Blocks nest.
 *
 * Compile and run:
 *   javac TemplateEngine.java
 *   java TemplateEngine
 */
public class TemplateEngine {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static String render(String template, Map<String, Object> model) {
        StringBuilder out = new StringBuilder();
        renderInto(template, model, out);
        return out.toString();
    }

    private static void renderInto(String template, Map<String, Object> model, StringBuilder out) {
        int index = 0;
        while (index < template.length()) {
            int open = template.indexOf("{{", index);
            if (open < 0) {
                out.append(template, index, template.length());
                return;
            }
            out.append(template, index, open);

            int close = template.indexOf("}}", open);
            if (close < 0) {
                throw new IllegalArgumentException("unclosed tag at position " + open);
            }
            String tag = template.substring(open + 2, close).trim();
            index = close + 2;

            if (tag.startsWith("#each ")) {
                String name = tag.substring("#each ".length()).trim();
                int end = findBlockEnd(template, index, "each");
                String body = template.substring(index, end);
                index = end + "{{/each}}".length();

                Object value = model.get(name);
                if (value instanceof Iterable<?> items) {
                    for (Object item : items) {
                        Map<String, Object> scope = new HashMap<>(model);
                        scope.put("this", item);
                        renderInto(body, scope, out);
                    }
                }
            } else if (tag.startsWith("#if ")) {
                String name = tag.substring("#if ".length()).trim();
                int end = findBlockEnd(template, index, "if");
                String body = template.substring(index, end);
                index = end + "{{/if}}".length();

                if (isTruthy(model.get(name))) {
                    renderInto(body, model, out);
                }
            } else if (tag.startsWith("#") || tag.startsWith("/")) {
                throw new IllegalArgumentException("unexpected tag {{" + tag + "}}");
            } else {
                out.append(valueOf(model.get(tag)));
            }
        }
    }

    /** Find the {{/kind}} that closes the block starting at {@code from}, counting nests. */
    private static int findBlockEnd(String template, int from, String kind) {
        String opener = "{{#" + kind + " ";
        String closer = "{{/" + kind + "}}";
        int depth = 1;
        int index = from;
        while (index < template.length()) {
            int nextOpen = template.indexOf(opener, index);
            int nextClose = template.indexOf(closer, index);
            if (nextClose < 0) {
                throw new IllegalArgumentException("missing {{/" + kind + "}}");
            }
            if (nextOpen >= 0 && nextOpen < nextClose) {
                depth++;
                index = nextOpen + opener.length();
            } else {
                depth--;
                if (depth == 0) {
                    return nextClose;
                }
                index = nextClose + closer.length();
            }
        }
        throw new IllegalArgumentException("missing {{/" + kind + "}}");
    }

    static boolean isTruthy(Object value) {
        return switch (value) {
            case null -> false;
            case Boolean flag -> flag;
            case Number number -> number.doubleValue() != 0;
            case String text -> !text.isEmpty();
            case Iterable<?> items -> items.iterator().hasNext();
            default -> true;
        };
    }

    static String valueOf(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    public static void main(String[] args) {
        Map<String, Object> model = new HashMap<>();
        model.put("name", "Ada");
        model.put("count", 3);
        model.put("passed", true);
        model.put("failed", false);
        model.put("empty", "");
        model.put("tests", List.of("login", "checkout", "search"));

        // ---- placeholders ----------------------------------------------------
        check(render("Hello {{name}}!", model).equals("Hello Ada!"), "a simple placeholder");
        check(render("{{ name }}", model).equals("Ada"), "spaces inside the tag are trimmed");
        check(render("no tags here", model).equals("no tags here"), "plain text passes through");
        check(render("{{missing}}", model).isEmpty(), "an unknown key renders as nothing");
        check(render("", model).isEmpty(), "an empty template renders empty");
        System.out.println("render      : " + render("Hello {{name}}, {{count}} tests", model));

        // ---- conditionals ----------------------------------------------------
        check(render("{{#if passed}}green{{/if}}", model).equals("green"), "if true renders the body");
        check(render("{{#if failed}}red{{/if}}", model).isEmpty(), "if false renders nothing");
        check(render("{{#if empty}}x{{/if}}", model).isEmpty(), "an empty string is falsy");
        check(render("{{#if count}}counted{{/if}}", model).equals("counted"), "a non-zero number is truthy");
        check(render("{{#if missing}}x{{/if}}", model).isEmpty(), "a missing key is falsy");

        // ---- loops -----------------------------------------------------------
        check(render("{{#each tests}}[{{this}}]{{/each}}", model).equals("[login][checkout][search]"),
                "each repeats the body per item");
        check(render("{{#each tests}}{{this}}, {{/each}}", model).equals("login, checkout, search, "),
                "each adds no separators of its own");
        check(render("{{#each missing}}x{{/each}}", model).isEmpty(), "each over nothing renders nothing");
        check(render("{{#each name}}x{{/each}}", model).isEmpty(), "each over a non-list renders nothing");
        check(render("{{#each tests}}{{name}}:{{this}} {{/each}}", model)
                .equals("Ada:login Ada:checkout Ada:search "), "the outer model is visible inside each");

        // ---- nesting ---------------------------------------------------------
        model.put("groups", List.of(List.of("a", "b"), List.of("c")));
        check(render("{{#each groups}}<{{#each this}}{{this}}{{/each}}>{{/each}}", model)
                .equals("<ab><c>"), "blocks nest");
        check(render("{{#each tests}}{{#if passed}}!{{/if}}{{this}} {{/each}}", model)
                .equals("!login !checkout !search "), "a conditional nests inside a loop");

        // ---- a realistic template --------------------------------------------
        String template = """
                Report for {{name}}
                {{#each tests}}- {{this}}
                {{/each}}{{#if failed}}FAILURES PRESENT{{/if}}{{#if passed}}ALL GREEN{{/if}}""";
        String report = render(template, model);
        check(report.contains("Report for Ada"), "the heading rendered");
        check(report.contains("- search"), "the list rendered");
        check(report.endsWith("ALL GREEN"), "the trailing conditional rendered");
        System.out.println("report      :\n" + report);

        // ---- broken templates fail loudly ------------------------------------
        for (String broken : List.of("{{name", "{{#each tests}}x", "{{#if passed}}x", "{{/if}}", "{{#nope}}")) {
            try {
                render(broken, model);
                throw new AssertionError("should have been rejected: " + broken);
            } catch (IllegalArgumentException expected) {
                System.out.printf("rejected    : %-24s -> %s%n", broken, expected.getMessage());
            }
        }

        // ---- format specifiers -----------------------------------------------
        check(String.format("%s scored %d", "Alice", 92).equals("Alice scored 92"), "%s and %d");
        check(String.format("%05.2f", 3.14159).equals("03.14"), "zero padded, rounded");
        check(String.format(Locale.US, "%,d", 1_234_567).equals("1,234,567"), "grouping separator");
        check(String.format("%x %X %o", 255, 255, 8).equals("ff FF 10"), "hex and octal");
        check(String.format("%.3e", 12345.678).equals("1.235e+04"), "scientific notation");
        check(String.format("%-10s|", "left").equals("left      |"), "left aligned");
        check(String.format("%10s|", "right").equals("     right|"), "right aligned");
        check(String.format("%b %b", true, null).equals("true false"), "%b is false for null");
        check(String.format("%c", 65).equals("A"), "%c from a code point");
        check(String.format("%d%%", 50).equals("50%"), "a literal percent sign");
        check(String.format("%s", (Object) null).equals("null"), "%s prints null, unlike our template");
        check("%s and %s".formatted("a", "b").equals("a and b"), "formatted() is the inline form");
        // %d with a locale grouping is locale dependent, which is why Locale.US is
        // passed explicitly above - the default here would use Indian grouping.
        System.out.println("format      : " + String.format(Locale.US, "%,d tests in %.2f s", 1_234_567, 12.3456));

        StringBuilder sink = new StringBuilder();
        try (Formatter formatter = new Formatter(sink, Locale.US)) {
            formatter.format("%s: %d", "tests", 42);
        }
        check(sink.toString().equals("tests: 42"), "Formatter can write into a StringBuilder");
        System.out.println("All checks passed.");
    }
}
