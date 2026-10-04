import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 101 - A regular expression engine written from scratch: the pattern is parsed
 * into a small tree and matched by backtracking. Supports literals, '.', the
 * '*', '+' and '?' quantifiers, '{n,m}', character classes with ranges and
 * negation, alternation, groups, the ^ and $ anchors, and the usual escapes.
 *
 * Every supported pattern is cross-checked against java.util.regex below.
 *
 * Compile and run:
 *   javac RegexEngine.java
 *   java RegexEngine
 */
public class RegexEngine {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    // ------------------------------------------------------------------ tree
    sealed interface Node permits Literal, AnyChar, CharClass, Anchor, Concat, Alternate, Repeat {
    }

    record Literal(char value) implements Node {
    }

    record AnyChar() implements Node {
    }

    record CharClass(boolean negated, List<int[]> ranges) implements Node {
        boolean accepts(char character) {
            boolean inside = false;
            for (int[] range : ranges) {
                if (character >= range[0] && character <= range[1]) {
                    inside = true;
                    break;
                }
            }
            return negated != inside;
        }
    }

    record Anchor(boolean start) implements Node {
    }

    record Concat(List<Node> nodes) implements Node {
    }

    record Alternate(List<Node> options) implements Node {
    }

    /** maximum of -1 means unbounded. */
    record Repeat(Node inner, int minimum, int maximum) implements Node {
    }

    // ---------------------------------------------------------------- parser
    static final class Parser {
        private final String pattern;
        private int position;

        private Parser(String pattern) {
            this.pattern = pattern;
        }

        static Node parse(String pattern) {
            Parser parser = new Parser(pattern);
            Node node = parser.alternation();
            if (parser.position != pattern.length()) {
                throw new IllegalArgumentException("unexpected '" + pattern.charAt(parser.position)
                        + "' at " + parser.position + " in " + pattern);
            }
            return node;
        }

        /** alternation := concatenation ('|' concatenation)* */
        private Node alternation() {
            List<Node> options = new ArrayList<>();
            options.add(concatenation());
            while (peek('|')) {
                position++;
                options.add(concatenation());
            }
            return options.size() == 1 ? options.get(0) : new Alternate(List.copyOf(options));
        }

        /** concatenation := repeat* */
        private Node concatenation() {
            List<Node> nodes = new ArrayList<>();
            while (position < pattern.length()
                    && pattern.charAt(position) != '|' && pattern.charAt(position) != ')') {
                nodes.add(repeat());
            }
            if (nodes.isEmpty()) {
                return new Concat(List.of());
            }
            return nodes.size() == 1 ? nodes.get(0) : new Concat(List.copyOf(nodes));
        }

        /** repeat := atom quantifier? */
        private Node repeat() {
            Node atom = atom();
            if (position >= pattern.length()) {
                return atom;
            }
            char next = pattern.charAt(position);
            if (next == '*') {
                position++;
                return new Repeat(atom, 0, -1);
            }
            if (next == '+') {
                position++;
                return new Repeat(atom, 1, -1);
            }
            if (next == '?') {
                position++;
                return new Repeat(atom, 0, 1);
            }
            if (next == '{') {
                return counted(atom);
            }
            return atom;
        }

        private Node counted(Node atom) {
            int saved = position;
            position++;                       // {
            Integer low = number();
            if (low == null) {
                position = saved;             // a literal brace, not a quantifier
                return atom;
            }
            int high = low;
            if (peek(',')) {
                position++;
                Integer upper = number();
                high = upper == null ? -1 : upper;
            }
            if (!peek('}')) {
                position = saved;
                return atom;
            }
            position++;
            if (high >= 0 && high < low) {
                throw new IllegalArgumentException("repetition bounds are the wrong way round in " + pattern);
            }
            return new Repeat(atom, low, high);
        }

        private Integer number() {
            int start = position;
            while (position < pattern.length() && Character.isDigit(pattern.charAt(position))) {
                position++;
            }
            return position == start ? null : Integer.valueOf(pattern.substring(start, position));
        }

        private Node atom() {
            if (position >= pattern.length()) {
                throw new IllegalArgumentException("the pattern ends where an atom was expected");
            }
            char character = pattern.charAt(position);
            return switch (character) {
                case '(' -> {
                    position++;
                    Node inner = alternation();
                    if (!peek(')')) {
                        throw new IllegalArgumentException("missing ')' in " + pattern);
                    }
                    position++;
                    yield inner;
                }
                case '[' -> charClass();
                case '.' -> {
                    position++;
                    yield new AnyChar();
                }
                case '^' -> {
                    position++;
                    yield new Anchor(true);
                }
                case '$' -> {
                    position++;
                    yield new Anchor(false);
                }
                case '\\' -> escape();
                default -> {
                    if (character == '*' || character == '+' || character == '?') {
                        throw new IllegalArgumentException("nothing to repeat at " + position + " in " + pattern);
                    }
                    position++;
                    yield new Literal(character);
                }
            };
        }

        private Node escape() {
            position++;                       // backslash
            if (position >= pattern.length()) {
                throw new IllegalArgumentException("the pattern ends with a backslash");
            }
            char escaped = pattern.charAt(position++);
            List<int[]> digits = List.of(new int[] {'0', '9'});
            List<int[]> word = List.of(new int[] {'a', 'z'}, new int[] {'A', 'Z'},
                    new int[] {'0', '9'}, new int[] {'_', '_'});
            List<int[]> space = List.of(new int[] {' ', ' '}, new int[] {'\t', '\t'},
                    new int[] {'\n', '\n'}, new int[] {'\r', '\r'});
            return switch (escaped) {
                case 'd' -> new CharClass(false, digits);
                case 'D' -> new CharClass(true, digits);
                case 'w' -> new CharClass(false, word);
                case 'W' -> new CharClass(true, word);
                case 's' -> new CharClass(false, space);
                case 'S' -> new CharClass(true, space);
                default -> new Literal(escaped);
            };
        }

        private Node charClass() {
            position++;                       // [
            boolean negated = peek('^');
            if (negated) {
                position++;
            }
            List<int[]> ranges = new ArrayList<>();
            boolean first = true;
            while (position < pattern.length() && (pattern.charAt(position) != ']' || first)) {
                char low = pattern.charAt(position++);
                if (low == '\\') {
                    if (position >= pattern.length()) {
                        throw new IllegalArgumentException("the class ends with a backslash");
                    }
                    char escaped = pattern.charAt(position++);
                    if ("dDwWsS".indexOf(escaped) >= 0) {
                        throw new IllegalArgumentException(
                                "shorthand classes are not supported inside [ ] in " + pattern);
                    }
                    low = escaped;
                }
                char high = low;
                if (position + 1 < pattern.length() && pattern.charAt(position) == '-'
                        && pattern.charAt(position + 1) != ']') {
                    position++;
                    high = pattern.charAt(position++);
                }
                ranges.add(new int[] {low, high});
                first = false;
            }
            if (position >= pattern.length()) {
                throw new IllegalArgumentException("missing ']' in " + pattern);
            }
            position++;                       // ]
            if (ranges.isEmpty()) {
                throw new IllegalArgumentException("empty character class in " + pattern);
            }
            return new CharClass(negated, List.copyOf(ranges));
        }

        private boolean peek(char wanted) {
            return position < pattern.length() && pattern.charAt(position) == wanted;
        }
    }

    // --------------------------------------------------------------- matching
    /**
     * Every position the node can reach from `position`, so alternatives simply
     * union their results. That makes backtracking implicit in the set of ends.
     */
    static Set<Integer> match(Node node, String input, int position) {
        return switch (node) {
            case Literal literal -> position < input.length() && input.charAt(position) == literal.value()
                    ? Set.of(position + 1)
                    : Set.of();
            case AnyChar ignored -> position < input.length() && !isLineTerminator(input.charAt(position))
                    ? Set.of(position + 1)
                    : Set.of();
            case CharClass charClass -> position < input.length() && charClass.accepts(input.charAt(position))
                    ? Set.of(position + 1)
                    : Set.of();
            case Anchor anchor -> {
                if (anchor.start() && position == 0) {
                    yield Set.of(position);
                }
                if (!anchor.start() && position == input.length()) {
                    yield Set.of(position);
                }
                yield Set.of();
            }
            case Concat concat -> {
                Set<Integer> positions = Set.of(position);
                for (Node child : concat.nodes()) {
                    Set<Integer> next = new HashSet<>();
                    for (int at : positions) {
                        next.addAll(match(child, input, at));
                    }
                    if (next.isEmpty()) {
                        yield Set.of();
                    }
                    positions = next;
                }
                yield positions;
            }
            case Alternate alternate -> {
                Set<Integer> positions = new HashSet<>();
                for (Node option : alternate.options()) {
                    positions.addAll(match(option, input, position));
                }
                yield positions;
            }
            case Repeat repeat -> {
                Set<Integer> results = new HashSet<>();
                Set<Integer> current = Set.of(position);
                int limit = repeat.maximum() < 0 ? input.length() + 1 : repeat.maximum();
                for (int count = 0; count <= limit; count++) {
                    if (count >= repeat.minimum()) {
                        results.addAll(current);
                    }
                    if (count == limit) {
                        break;
                    }
                    Set<Integer> next = new HashSet<>();
                    for (int at : current) {
                        next.addAll(match(repeat.inner(), input, at));
                    }
                    if (next.isEmpty()) {
                        break;
                    }
                    current = next;
                }
                yield results;
            }
        };
    }

    /**
     * Java's dot excludes line terminators unless DOTALL is set, and this engine
     * does not support DOTALL, so the dot behaves the same way here.
     */
    static boolean isLineTerminator(char character) {
        return character == '\n' || character == '\r' || character == '\u0085'
                || character == '\u2028' || character == '\u2029';
    }

    // ------------------------------------------------------------------ api
    static boolean matches(String pattern, String input) {
        return match(Parser.parse(pattern), input, 0).contains(input.length());
    }

    static boolean find(String pattern, String input) {
        return findStart(pattern, input) >= 0;
    }

    /** Where the leftmost match begins, or -1. */
    static int findStart(String pattern, String input) {
        Node node = Parser.parse(pattern);
        for (int start = 0; start <= input.length(); start++) {
            if (!match(node, input, start).isEmpty()) {
                return start;
            }
        }
        return -1;
    }

    public static void main(String[] args) {
        // ---- the pattern list, and the same list through java.util.regex ------
        String[][] cases = {
                {"abc", "abc"},
                {"abc", "ab"},
                {"a.c", "abc"},
                {"a.c", "ac"},
                {"ab*c", "ac"},
                {"ab*c", "abbbc"},
                {"ab+c", "ac"},
                {"ab+c", "abc"},
                {"ab?c", "ac"},
                {"ab?c", "abc"},
                {"a|b", "a"},
                {"a|b", "c"},
                {"(ab)+", "ababab"},
                {"(ab)+", "aba"},
                {"[abc]+", "cab"},
                {"[a-c]{2}", "ab"},
                {"[a-c]{2}", "ad"},
                {"[^0-9]{3}", "abc"},
                {"[^0-9]{3}", "ab1"},
                {"^abc$", "abc"},
                {"^abc$", "xabc"},
                {"\\d{4}-\\d{2}-\\d{2}", "2026-01-05"},
                {"\\w+", "hello_42"},
                {"\\s", " "},
                {"a{2,4}", "aaa"},
                {"a{2,4}", "aaaaa"},
                {"a{2,}", "aaaaaa"},
                {"colou?r", "color"},
                {"colou?r", "colour"},
                {"(a|b)*c", "abababc"},
                {".*", ""},
                {"a*", ""},
                {"\\d+\\s\\w+", "42 bots"},
                {"[a-z]+@[a-z]+\\.[a-z]{2,3}", "ada@example.com"},
                {"[a-z]+@[a-z]+\\.[a-z]{2,3}", "ada@example"},
                {"(cat|dog)s?", "dogs"},
                {"a.b", "a\nb"},
        };

        int agreed = 0;
        for (String[] pair : cases) {
            boolean mine = matches(pair[0], pair[1]);
            boolean theirs = java.util.regex.Pattern.matches(pair[0], pair[1]);
            check(mine == theirs, "disagreement on /" + pair[0] + "/ against \"" + pair[1]
                    + "\": mine=" + mine + ", java.util.regex=" + theirs);
            agreed++;
        }
        System.out.println("cross-check  : " + agreed + " patterns agree with java.util.regex");

        // ---- a few of those, stated explicitly -------------------------------
        check(matches("abc", "abc"), "a literal matches itself");
        check(!matches("abc", "ab"), "but not a prefix");
        check(matches(".", "x"), "a dot matches one character");
        check(!matches(".", ""), "but not zero characters");
        check(matches("a*b", "b"), "a star allows zero");
        check(matches("a+b", "aab"), "a plus needs at least one");
        check(!matches("a+b", "b"), "and will not settle for zero");
        check(matches("colou?r", "color") && matches("colou?r", "colour"), "an optional letter");
        check(matches("a{3}", "aaa") && !matches("a{3}", "aa"), "an exact count");
        check(matches("a{2,}", "aaaa") && !matches("a{2,}", "a"), "an open ended range");
        check(matches("[a-c]+", "abcabc"), "a class with a range");
        check(matches("[^0-9]+", "abc") && !matches("[^0-9]+", "ab1"), "a negated class");
        check(matches("(ab|cd)+", "abcdab"), "alternation inside a group");
        check(matches("^a.*z$", "abcz"), "both anchors");
        check(!matches("^a.*z$", "babcz"), "the start anchor pins the beginning");
        check(matches("\\d+", "12345"), "one or more digits");
        check(!matches("\\d+", "12a45"), "the whole input must be digits");
        System.out.println("explicit     : literals, quantifiers, classes, groups and anchors");

        // ---- the tricky ones -------------------------------------------------
        check(!matches("a.b", "a\nb"), "a dot does not match a newline by default");
        check(matches(".*", ""), "a star over nothing matches the empty string");
        check(matches("(a*)*", ""), "nested empty repetition terminates rather than looping");
        check(matches("(|a)", "") && matches("(|a)", "a"), "an empty alternative");
        check(matches("a{0}", ""), "a count of zero means the atom is skipped");
        check(matches("[]]", "]"), "a class may contain a closing bracket as its first character");
        check(matches("a\\{2\\}", "a{2}"), "a brace can be escaped to a literal");
        System.out.println("edge cases   : newline, empty repetition, empty alternatives");

        // ---- searching rather than matching the whole input ------------------
        check(find("cat", "the cat sat"), "find locates a substring");
        check(findStart("cat", "the cat sat") == 4, "and reports where it starts");
        check(findStart("sat", "the cat sat") == 8, "at the end of the line");
        check(!find("dog", "the cat sat"), "a missing substring is reported as absent");
        check(findStart("dog", "the cat sat") == -1, "with a start of minus one");
        check(findStart("a+", "baaad") == 1, "the leftmost run of a's");
        check(find("^the", "the cat"), "a start anchor matches at the beginning");
        check(!find("^cat", "the cat"), "but not in the middle");
        check(find("sat$", "the cat sat"), "an end anchor matches at the end");
        check(!find("cat$", "the cat sat"), "but not in the middle");
        check(findStart("", "abc") == 0, "an empty pattern matches at the very start");
        System.out.println("searching    : find and findStart, including the anchors");

        // ---- rejected patterns -----------------------------------------------
        // Note that a lone '{' is treated as a literal, as Java does, so "a{2" is
        // a valid (if odd) pattern rather than an error.
        for (String bad : new String[] {"(", "a(", "[abc", "[]", "a{2,1}", "*a", "+a", "\\",
                "a)", "[a", "a)b", "["}) {
            try {
                matches(bad, "whatever");
                throw new AssertionError("should have been rejected: " + bad);
            } catch (IllegalArgumentException expected) {
                // expected
            }
        }
        System.out.println("rejected     : twelve malformed patterns");

        // ---- a realistic pattern, checked against the real engine ------------
        String email = "[a-z][a-z0-9.]*@[a-z0-9]+\\.[a-z]{2,3}";
        String[] addresses = {"ada@example.com", "a@b.co", "bad@", "@example.com", "ada@example"};
        for (String address : addresses) {
            check(matches(email, address)
                            == java.util.regex.Pattern.matches(email, address),
                    "the email pattern agrees with java.util.regex on " + address);
        }
        System.out.println("realistic    : an email pattern agrees on all five samples");
        System.out.println("All checks passed.");
    }
}
