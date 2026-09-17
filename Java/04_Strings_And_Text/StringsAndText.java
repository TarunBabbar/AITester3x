import java.util.Arrays;

/**
 * 04 - Strings and text: methods, StringBuilder, text blocks and formatting.
 *
 * Compile and run:
 *   javac StringsAndText.java
 *   java StringsAndText
 */
public class StringsAndText {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) {
        String sentence = "  the quick brown fox jumps over the lazy dog  ";
        String trimmed = sentence.strip();
        check(sentence.length() > trimmed.length(), "strip removes the padding");
        check(trimmed.startsWith("the") && trimmed.endsWith("dog"), "prefix and suffix");

        String[] words = trimmed.split(" ");
        check(words.length == 9, "there are nine words");
        check(String.join("-", Arrays.copyOf(words, 3)).equals("the-quick-brown"),
                "join of the first three words");

        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            builder.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1))
                    .append(' ');
        }
        String titleCase = builder.toString().trim();
        check(titleCase.startsWith("The Quick Brown"), "title case is applied");
        check(titleCase.split(" ").length == 9, "title case keeps nine words");

        String block = """
                {
                  "name": "demo",
                  "count": 3
                }
                """;
        check(block.contains("\"count\": 3"), "text block keeps its content");
        check(block.lines().count() == 4, "text block has four lines");

        String formatted = "%s scored %d%% in %s".formatted("Alice", 92, "testing");
        check(formatted.equals("Alice scored 92% in testing"), "formatted() builds the text");
        check("JAVA".equalsIgnoreCase("java"), "equalsIgnoreCase ignores case");

        System.out.println("trimmed : " + trimmed);
        System.out.println("words   : " + words.length);
        System.out.println("title   : " + titleCase);
        System.out.println("block   :\n" + block);
        System.out.println("format  : " + formatted);
        System.out.println("All checks passed.");
    }
}
