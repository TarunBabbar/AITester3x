import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 95 - A SQL-like query engine: a tokeniser and recursive descent parser for
 * SELECT ... FROM ... WHERE ... ORDER BY ... LIMIT, evaluated over in-memory
 * tables.
 *
 * The WHERE clause is a small expression tree, so AND, OR and brackets behave as
 * they do in SQL rather than by precedence accident.
 *
 * Compile and run:
 *   javac SqlLikeQuery.java
 *   java SqlLikeQuery
 */
public class SqlLikeQuery {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    // ------------------------------------------------------------- the rows
    static Map<String, Object> row(Object... pairs) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            row.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return row;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static int compare(Object left, Object right) {
        if (left instanceof Number first && right instanceof Number second) {
            return Double.compare(first.doubleValue(), second.doubleValue());
        }
        return String.valueOf(left).compareToIgnoreCase(String.valueOf(right));
    }

    // ------------------------------------------------------------- filters
    @FunctionalInterface
    interface Condition {
        boolean test(Map<String, Object> row);
    }

    record Comparison(String column, String operator, Object value) implements Condition {
        @Override
        public boolean test(Map<String, Object> row) {
            Object actual = row.get(column);
            if (actual == null) {
                return false;
            }
            int order = compare(actual, value);
            return switch (operator) {
                case "=" -> order == 0;
                case "<>" -> order != 0;
                case "<" -> order < 0;
                case "<=" -> order <= 0;
                case ">" -> order > 0;
                case ">=" -> order >= 0;
                default -> throw new IllegalStateException("unknown operator " + operator);
            };
        }
    }

    record AndOr(boolean conjunction, Condition left, Condition right) implements Condition {
        @Override
        public boolean test(Map<String, Object> row) {
            return conjunction
                    ? left.test(row) && right.test(row)
                    : left.test(row) || right.test(row);
        }
    }

    record Query(List<String> columns, String table, Condition where,
                 String orderBy, boolean descending, Integer limit) {
    }

    record Result(List<String> columns, List<List<Object>> rows) {
    }

    // -------------------------------------------------------------- parser
    static final class Parser {
        private static final Set<String> OPERATORS = Set.of("=", "<>", "<", "<=", ">", ">=");

        private final List<String> tokens;
        private int position;

        Parser(String sql) {
            this.tokens = tokenise(sql);
        }

        static List<String> tokenise(String sql) {
            List<String> tokens = new ArrayList<>();
            int index = 0;
            while (index < sql.length()) {
                char character = sql.charAt(index);

                if (Character.isWhitespace(character)) {
                    index++;
                } else if (character == '\'' || character == '"') {
                    int end = sql.indexOf(character, index + 1);
                    if (end < 0) {
                        throw new IllegalArgumentException("unterminated string in " + sql);
                    }
                    tokens.add(sql.substring(index + 1, end));   // the quotes are not part of the value
                    index = end + 1;
                } else if (Character.isLetterOrDigit(character) || character == '_' || character == '.') {
                    int end = index;
                    while (end < sql.length() && (Character.isLetterOrDigit(sql.charAt(end))
                            || sql.charAt(end) == '_' || sql.charAt(end) == '.')) {
                        end++;
                    }
                    tokens.add(sql.substring(index, end));
                    index = end;
                } else {
                    String pair = index + 1 < sql.length() ? sql.substring(index, index + 2) : "";
                    if (pair.equals("<=") || pair.equals("<>") || pair.equals(">=")) {
                        tokens.add(pair);
                        index += 2;
                    } else if ("=<>*(),".indexOf(character) >= 0) {
                        tokens.add(String.valueOf(character));
                        index++;
                    } else {
                        throw new IllegalArgumentException("unexpected character " + character + " in " + sql);
                    }
                }
            }
            return tokens;
        }

        Query parse() {
            expectWord("select");
            List<String> columns = new ArrayList<>();
            if (peekToken("*")) {
                next();
            } else {
                columns.add(next());
                while (peekToken(",")) {
                    next();
                    columns.add(next());
                }
            }
            expectWord("from");
            String table = next();

            Condition where = null;
            if (peekWord("where")) {
                next();
                where = condition();
            }

            String orderBy = null;
            boolean descending = false;
            if (peekWord("order")) {
                next();
                expectWord("by");
                orderBy = next();
                if (peekWord("asc")) {
                    next();
                } else if (peekWord("desc")) {
                    next();
                    descending = true;
                }
            }

            Integer limit = null;
            if (peekWord("limit")) {
                next();
                String raw = next();
                try {
                    limit = Integer.valueOf(raw);
                } catch (NumberFormatException notANumber) {
                    throw new IllegalArgumentException("limit needs a number, got " + raw);
                }
                if (limit < 0) {
                    throw new IllegalArgumentException("limit must not be negative");
                }
            }

            if (position != tokens.size()) {
                throw new IllegalArgumentException("unexpected trailing token: " + tokens.get(position));
            }
            return new Query(columns, table.toLowerCase(), where, orderBy, descending, limit);
        }

        /** condition := conjunction (OR conjunction)* */
        private Condition condition() {
            Condition left = conjunction();
            while (peekWord("or")) {
                next();
                left = new AndOr(false, left, conjunction());
            }
            return left;
        }

        /** conjunction := primary (AND primary)* */
        private Condition conjunction() {
            Condition left = primary();
            while (peekWord("and")) {
                next();
                left = new AndOr(true, left, primary());
            }
            return left;
        }

        /** primary := '(' condition ')' | column operator value */
        private Condition primary() {
            if (peekToken("(")) {
                next();
                Condition inner = condition();
                if (!peekToken(")")) {
                    throw new IllegalArgumentException("missing a closing bracket");
                }
                next();
                return inner;
            }
            String column = next().toLowerCase();
            if (position >= tokens.size()) {
                throw new IllegalArgumentException("expected an operator after " + column);
            }
            String operator = next();
            if (!OPERATORS.contains(operator)) {
                throw new IllegalArgumentException("not a comparison operator: " + operator);
            }
            if (position >= tokens.size()) {
                throw new IllegalArgumentException("expected a value after " + operator);
            }
            return new Comparison(column, operator, literal(next()));
        }

        private Object literal(String raw) {
            try {
                return raw.contains(".") ? Double.valueOf(raw) : Long.valueOf(raw);
            } catch (NumberFormatException notANumber) {
                return raw;      // it was a string, quoted or not
            }
        }

        private String next() {
            if (position >= tokens.size()) {
                throw new IllegalArgumentException("the statement ends too early");
            }
            return tokens.get(position++);
        }

        private boolean peekToken(String token) {
            return position < tokens.size() && tokens.get(position).equals(token);
        }

        private boolean peekWord(String word) {
            return position < tokens.size() && tokens.get(position).equalsIgnoreCase(word);
        }

        private void expectWord(String word) {
            if (!peekWord(word)) {
                throw new IllegalArgumentException("expected " + word.toUpperCase()
                        + " but found " + (position < tokens.size() ? tokens.get(position) : "the end"));
            }
            next();
        }
    }

    // ------------------------------------------------------------ execution
    static Result execute(String sql, Map<String, List<Map<String, Object>>> tables) {
        Query query = new Parser(sql).parse();

        List<Map<String, Object>> source = tables.get(query.table());
        if (source == null) {
            throw new IllegalArgumentException("no such table: " + query.table());
        }

        List<String> columns = query.columns().isEmpty()
                ? (source.isEmpty() ? List.of() : List.copyOf(source.get(0).keySet()))
                : query.columns().stream().map(String::toLowerCase).toList();

        List<Map<String, Object>> selected = new ArrayList<>();
        for (Map<String, Object> candidate : source) {
            if (query.where() == null || query.where().test(candidate)) {
                selected.add(candidate);
            }
        }

        if (query.orderBy() != null) {
            String column = query.orderBy().toLowerCase();
            Comparator<Map<String, Object>> comparator =
                    (left, right) -> compare(left.get(column), right.get(column));
            if (query.descending()) {
                comparator = comparator.reversed();
            }
            selected.sort(comparator);
        }

        if (query.limit() != null && query.limit() < selected.size()) {
            selected = new ArrayList<>(selected.subList(0, query.limit()));
        }

        List<List<Object>> rows = new ArrayList<>();
        for (Map<String, Object> candidate : selected) {
            List<Object> values = new ArrayList<>();
            for (String column : columns) {
                if (!candidate.containsKey(column)) {
                    throw new IllegalArgumentException("no such column: " + column);
                }
                values.add(candidate.get(column));
            }
            rows.add(values);
        }
        return new Result(columns, rows);
    }

    public static void main(String[] args) {
        Map<String, List<Map<String, Object>>> tables = new LinkedHashMap<>();
        tables.put("employees", List.of(
                row("name", "alice", "dept", "eng", "salary", 95_000),
                row("name", "bob", "dept", "sales", "salary", 72_000),
                row("name", "carol", "dept", "eng", "salary", 101_000),
                row("name", "dan", "dept", "support", "salary", 54_000),
                row("name", "erin", "dept", "eng", "salary", 88_000),
                row("name", "frank", "dept", "sales", "salary", 66_000)));

        // ---- select all -------------------------------------------------------
        Result all = execute("SELECT * FROM employees", tables);
        check(all.rows().size() == 6, "six rows");
        check(all.columns().equals(List.of("name", "dept", "salary")),
                "star takes the table's column order");
        check(all.rows().get(0).equals(List.of("alice", "eng", 95_000)),
                "and the first row is intact");

        // ---- columns, filter and order ---------------------------------------
        Result engineers = execute(
                "SELECT name FROM employees WHERE dept = 'eng' ORDER BY salary DESC", tables);
        check(engineers.rows().equals(List.of(List.of("carol"), List.of("alice"), List.of("erin"))),
                "engineers by pay, best first");
        System.out.println("engineers    : " + engineers.rows());

        Result sales = execute(
                "select name, salary from employees where salary >= 66000 and dept = 'sales' order by name",
                tables);
        check(sales.rows().equals(List.of(List.of("bob", 72_000), List.of("frank", 66_000))),
                "lowercase keywords work the same");
        check(sales.columns().equals(List.of("name", "salary")), "only the chosen columns come back");
        System.out.println("sales        : " + sales.rows());

        // ---- and, or and brackets --------------------------------------------
        Result senior = execute("SELECT name FROM employees "
                + "WHERE (dept = 'eng' OR dept = 'sales') AND salary > 80000 "
                + "ORDER BY salary DESC", tables);
        check(senior.rows().equals(List.of(List.of("carol"), List.of("alice"), List.of("erin"))),
                "the bracket groups the two departments before the salary test");
        System.out.println("senior       : " + senior.rows());

        // without the brackets, AND would bind tighter and change the answer
        Result grouped = execute("SELECT name FROM employees "
                + "WHERE dept = 'eng' OR dept = 'sales' AND salary > 80000 "
                + "ORDER BY name", tables);
        check(grouped.rows().equals(List.of(List.of("alice"), List.of("carol"), List.of("erin"))),
                "and without them, every engineer qualifies regardless of pay");
        System.out.println("precedence   : " + grouped.rows());

        // ---- the other comparison operators ----------------------------------
        check(execute("SELECT name FROM employees WHERE dept <> 'eng' ORDER BY name", tables)
                .rows().equals(List.of(List.of("bob"), List.of("dan"), List.of("frank"))),
                "not equal");
        check(execute("SELECT name FROM employees WHERE salary < 70000 ORDER BY salary", tables)
                .rows().equals(List.of(List.of("dan"), List.of("frank"))),
                "less than, ascending by default");
        check(execute("SELECT name FROM employees WHERE salary <= 72000 ORDER BY salary", tables)
                .rows().equals(List.of(List.of("dan"), List.of("frank"), List.of("bob"))),
                "less than or equal");
        check(execute("SELECT name FROM employees WHERE name = 'dan'", tables)
                .rows().equals(List.of(List.of("dan"))), "a string match");
        check(execute("SELECT name FROM employees WHERE dept = 'SALES' ORDER BY name", tables)
                .rows().equals(List.of(List.of("bob"), List.of("frank"))),
                "string comparisons ignore case, like many databases");
        System.out.println("operators    : <, <=, <>, = all behave");

        // ---- limit ------------------------------------------------------------
        check(execute("SELECT name FROM employees ORDER BY salary DESC LIMIT 2", tables)
                .rows().equals(List.of(List.of("carol"), List.of("alice"))),
                "the limit is applied after the sort");
        check(execute("SELECT name FROM employees LIMIT 0", tables).rows().isEmpty(),
                "a limit of zero returns nothing");
        check(execute("SELECT name FROM employees LIMIT 99", tables).rows().size() == 6,
                "a limit bigger than the table changes nothing");

        // ---- empty results and empty tables ----------------------------------
        check(execute("SELECT name FROM employees WHERE salary > 1000000", tables).rows().isEmpty(),
                "no rows match, so none come back");
        check(execute("SELECT name FROM employees WHERE dept = 'eng' AND salary > 1000000", tables)
                .rows().isEmpty(), "and the tree handles an empty result");
        tables.put("empty", List.of());
        check(execute("SELECT * FROM empty", tables).rows().isEmpty(),
                "an empty table with a star selects no columns and no rows");
        check(execute("SELECT name FROM empty", tables).rows().isEmpty(),
                "and with a column named it still returns nothing");

        // ---- errors -----------------------------------------------------------
        for (String bad : new String[] {
                "SELECT FROM employees", "SELECT * employees", "SELECT * FROM",
                "SELECT * FROM employees WHERE", "SELECT * FROM employees WHERE dept",
                "SELECT * FROM employees WHERE dept =", "SELECT * FROM employees ORDER BY",
                "SELECT * FROM employees ORDER BY salary SIDEWAYS", "SELECT * FROM employees LIMIT x",
                "SELECT * FROM employees EXTRA", "SELECT * FROM employees WHERE (dept = 'eng'",
                "SELECT * FROM employees WHERE dept ?? 'eng'"}) {
            try {
                execute(bad, tables);
                throw new AssertionError("should have been rejected: " + bad);
            } catch (IllegalArgumentException expected) {
                // expected
            }
        }
        System.out.println("syntax       : twelve broken statements all rejected");

        try {
            execute("SELECT * FROM nowhere", tables);
            throw new AssertionError("the table does not exist");
        } catch (IllegalArgumentException expected) {
            System.out.println("table        : " + expected.getMessage());
        }

        try {
            execute("SELECT manager FROM employees", tables);
            throw new AssertionError("the column does not exist");
        } catch (IllegalArgumentException expected) {
            System.out.println("column       : " + expected.getMessage());
        }

        // an unknown column in the filter simply matches nothing, as in SQL nulls
        check(execute("SELECT name FROM employees WHERE missing = 'x'", tables).rows().isEmpty(),
                "a missing column never matches");
        System.out.println("All checks passed.");
    }
}
