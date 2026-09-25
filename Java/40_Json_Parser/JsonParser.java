import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 40 - A small JSON reader and writer: recursive-descent parsing into Map,
 * List, String, Long, Double, Boolean and null, plus serialisation back out.
 *
 * Compile and run:
 *   javac JsonParser.java
 *   java JsonParser
 */
public class JsonParser {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static String classOf(Object value) {
        return value == null ? "null" : value.getClass().getSimpleName();
    }

    static final class Json {
        private final String text;
        private int position;

        private Json(String text) {
            this.text = text;
        }

        static Object parse(String text) {
            Json reader = new Json(text);
            reader.skipWhitespace();
            Object value = reader.readValue();
            reader.skipWhitespace();
            if (reader.position != text.length()) {
                throw new IllegalArgumentException(
                        "unexpected trailing content at " + reader.position);
            }
            return value;
        }

        private Object readValue() {
            if (position >= text.length()) {
                throw new IllegalArgumentException("unexpected end of input");
            }
            return switch (text.charAt(position)) {
                case '{' -> readObject();
                case '[' -> readArray();
                case '"' -> readString();
                case 't' -> readLiteral("true", Boolean.TRUE);
                case 'f' -> readLiteral("false", Boolean.FALSE);
                case 'n' -> readLiteral("null", null);
                default -> readNumber();
            };
        }

        private Map<String, Object> readObject() {
            Map<String, Object> object = new LinkedHashMap<>();
            expect('{');
            skipWhitespace();
            if (peekIs('}')) {
                position++;
                return object;
            }
            while (true) {
                skipWhitespace();
                String key = readString();
                skipWhitespace();
                expect(':');
                skipWhitespace();
                object.put(key, readValue());
                skipWhitespace();
                if (peekIs(',')) {
                    position++;
                    continue;
                }
                expect('}');
                return object;
            }
        }

        private List<Object> readArray() {
            List<Object> array = new ArrayList<>();
            expect('[');
            skipWhitespace();
            if (peekIs(']')) {
                position++;
                return array;
            }
            while (true) {
                skipWhitespace();
                array.add(readValue());
                skipWhitespace();
                if (peekIs(',')) {
                    position++;
                    continue;
                }
                expect(']');
                return array;
            }
        }

        private String readString() {
            expect('"');
            StringBuilder out = new StringBuilder();
            while (true) {
                if (position >= text.length()) {
                    throw new IllegalArgumentException("unterminated string");
                }
                char character = text.charAt(position++);
                if (character == '"') {
                    return out.toString();
                }
                if (character != '\\') {
                    out.append(character);
                    continue;
                }
                if (position >= text.length()) {
                    throw new IllegalArgumentException("unterminated escape");
                }
                char escape = text.charAt(position++);
                switch (escape) {
                    case '"' -> out.append('"');
                    case '\\' -> out.append('\\');
                    case '/' -> out.append('/');
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'u' -> {
                        if (position + 4 > text.length()) {
                            throw new IllegalArgumentException("truncated unicode escape");
                        }
                        out.append((char) Integer.parseInt(
                                text.substring(position, position + 4), 16));
                        position += 4;
                    }
                    default -> throw new IllegalArgumentException("unknown escape \\" + escape);
                }
            }
        }

        private Object readNumber() {
            int start = position;
            while (position < text.length()
                    && "-+.eE0123456789".indexOf(text.charAt(position)) >= 0) {
                position++;
            }
            String literal = text.substring(start, position);
            if (literal.isEmpty()) {
                throw new IllegalArgumentException("expected a value at " + start);
            }
            // Separate returns on purpose: in a ternary mixing double and long,
            // Java promotes BOTH branches to double, so integers would box as Double.
            if (literal.contains(".") || literal.contains("e") || literal.contains("E")) {
                return Double.valueOf(literal);
            }
            return Long.valueOf(literal);
        }

        private Object readLiteral(String literal, Object value) {
            if (!text.startsWith(literal, position)) {
                throw new IllegalArgumentException("expected " + literal + " at " + position);
            }
            position += literal.length();
            return value;
        }

        private void expect(char wanted) {
            if (position >= text.length() || text.charAt(position) != wanted) {
                throw new IllegalArgumentException("expected '" + wanted + "' at " + position);
            }
            position++;
        }

        private boolean peekIs(char character) {
            return position < text.length() && text.charAt(position) == character;
        }

        private void skipWhitespace() {
            while (position < text.length() && Character.isWhitespace(text.charAt(position))) {
                position++;
            }
        }
    }

    static String write(Object value) {
        StringBuilder out = new StringBuilder();
        writeValue(value, out);
        return out.toString();
    }

    private static void writeValue(Object value, StringBuilder out) {
        switch (value) {
            case null -> out.append("null");
            case String text -> writeString(text, out);
            case Boolean flag -> out.append(flag);
            case Number number -> out.append(number);
            case Map<?, ?> map -> {
                out.append('{');
                boolean first = true;
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (!first) {
                        out.append(',');
                    }
                    first = false;
                    writeString(String.valueOf(entry.getKey()), out);
                    out.append(':');
                    writeValue(entry.getValue(), out);
                }
                out.append('}');
            }
            case List<?> list -> {
                out.append('[');
                boolean first = true;
                for (Object item : list) {
                    if (!first) {
                        out.append(',');
                    }
                    first = false;
                    writeValue(item, out);
                }
                out.append(']');
            }
            default -> throw new IllegalArgumentException("cannot write " + value.getClass());
        }
    }

    private static void writeString(String text, StringBuilder out) {
        out.append('"');
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            switch (character) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> {
                    if (character < 0x20) {
                        out.append(String.format("\\u%04x", (int) character));
                    } else {
                        out.append(character);
                    }
                }
            }
        }
        out.append('"');
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        String json = """
                {
                  "name": "run-1",
                  "passed": true,
                  "count": 42,
                  "ratio": 0.75,
                  "tags": ["smoke", "api"],
                  "nested": {"depth": 2, "missing": null},
                  "escaped": "a \\"quote\\" and a backslash \\\\ and a newline \\n",
                  "unicode": "\\u0041\\u0042"
                }
                """;

        Map<String, Object> parsed = (Map<String, Object>) Json.parse(json);
        check(parsed.get("name").equals("run-1"), "string value");
        check(parsed.get("passed").equals(Boolean.TRUE), "boolean value");
        check(parsed.get("count").equals(42L), "an integer becomes a Long, got "
                + parsed.get("count") + " of type " + classOf(parsed.get("count")));
        check(parsed.get("ratio").equals(0.75), "a decimal becomes a Double");

        check(parsed.get("tags").equals(List.of("smoke", "api")), "array of strings");
        Map<String, Object> nested = (Map<String, Object>) parsed.get("nested");
        check(nested.get("depth").equals(2L), "nested object");
        check(nested.containsKey("missing") && nested.get("missing") == null, "explicit null");

        check(parsed.get("escaped").equals("a \"quote\" and a backslash \\ and a newline \n"),
                "escape sequences decoded");
        check(parsed.get("unicode").equals("AB"), "unicode escapes decoded");
        check(parsed.keySet().equals(java.util.Set.of("name", "passed", "count", "ratio",
                "tags", "nested", "escaped", "unicode")), "key order is preserved");
        System.out.println("parsed keys : " + parsed.keySet());

        String written = write(parsed);
        check(written.startsWith("{\"name\":\"run-1\""), "the writer emits compact JSON");
        Map<String, Object> reparsed = (Map<String, Object>) Json.parse(written);
        check(reparsed.equals(parsed), "write then read returns an equal structure");
        check(write(List.of(1L, 2L, 3L)).equals("[1,2,3]"), "arrays serialise");
        check(write(null).equals("null"), "null serialises");
        check(write("a\"b").equals("\"a\\\"b\""), "quotes are escaped");
        System.out.println("written     : " + written.substring(0, Math.min(60, written.length())) + "...");

        for (String bad : List.of("{\"a\": }", "{\"a\": 1,}", "[1,2", "\"unterminated", "{} extra", "tru")) {
            try {
                Json.parse(bad);
                throw new AssertionError("should have been rejected: " + bad);
            } catch (IllegalArgumentException expected) {
                System.out.printf("rejected    : %-16s -> %s%n", bad, expected.getMessage());
            }
        }
        System.out.println("All checks passed.");
    }
}
