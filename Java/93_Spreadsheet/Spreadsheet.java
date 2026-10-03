import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 93 - A spreadsheet: cells holding either numbers or formulas that reference
 * other cells. Values are computed on demand, memoised, and a cell that depends
 * on itself is reported as a circular reference rather than looping forever.
 *
 * Compile and run:
 *   javac Spreadsheet.java
 *   java Spreadsheet
 */
public class Spreadsheet {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static class CircularReferenceException extends RuntimeException {
        CircularReferenceException(String message) {
            super(message);
        }
    }

    static final class Sheet {
        private final Map<String, String> formulas = new TreeMap<>();
        private final Map<String, Double> cached = new HashMap<>();
        private final Set<String> active = new HashSet<>();
        private final List<String> evaluationOrder = new ArrayList<>();

        void set(String reference, String text) {
            formulas.put(normalise(reference), text.trim());
            cached.clear();                     // every value could have moved
            evaluationOrder.clear();
        }

        boolean has(String reference) {
            return formulas.containsKey(normalise(reference));
        }

        double valueOf(String reference) {
            String cell = normalise(reference);
            if (cached.containsKey(cell)) {
                return cached.get(cell);
            }
            String formula = formulas.get(cell);
            if (formula == null) {
                throw new IllegalArgumentException("no such cell: " + cell);
            }
            if (!active.add(cell)) {
                throw new CircularReferenceException(
                        "circular reference: " + cell + " depends on itself through " + active);
            }
            try {
                double value = evaluate(formula, this);
                cached.put(cell, value);
                evaluationOrder.add(cell);
                return value;
            } finally {
                active.remove(cell);
            }
        }

        /** The order cells were first computed in, which respects dependencies. */
        List<String> evaluationOrder() {
            return List.copyOf(evaluationOrder);
        }

        Set<String> cells() {
            return formulas.keySet();
        }
    }

    static String normalise(String reference) {
        return reference.trim().toUpperCase();
    }

    /** A cell is evaluated either as a plain number or as a formula after the =. */
    static double evaluate(String text, Sheet sheet) {
        String expression = text.startsWith("=") ? text.substring(1) : text;
        Parser parser = new Parser(expression, sheet);
        double value = parser.expression();
        parser.expectEnd();
        return value;
    }

    static final class Parser {
        private final String text;
        private final Sheet sheet;
        private int position;

        Parser(String text, Sheet sheet) {
            this.text = text;
            this.sheet = sheet;
        }

        /** expression := term (('+' | '-') term)* */
        double expression() {
            double value = term();
            while (true) {
                skipSpaces();
                if (peek('+')) {
                    position++;
                    value += term();
                } else if (peek('-')) {
                    position++;
                    value -= term();
                } else {
                    return value;
                }
            }
        }

        /** term := factor (('*' | '/') factor)* */
        double term() {
            double value = factor();
            while (true) {
                skipSpaces();
                if (peek('*')) {
                    position++;
                    value *= factor();
                } else if (peek('/')) {
                    position++;
                    double divisor = factor();
                    if (divisor == 0) {
                        throw new ArithmeticException("division by zero");
                    }
                    value /= divisor;
                } else {
                    return value;
                }
            }
        }

        /** factor := number | reference | '(' expression ')' | '-' factor */
        double factor() {
            skipSpaces();
            if (peek('-')) {
                position++;
                return -factor();
            }
            if (peek('(')) {
                position++;
                double value = expression();
                skipSpaces();
                if (!peek(')')) {
                    throw new IllegalArgumentException("missing a closing bracket in " + text);
                }
                position++;
                return value;
            }
            if (position < text.length() && Character.isDigit(text.charAt(position))) {
                int start = position;
                while (position < text.length()
                        && (Character.isDigit(text.charAt(position)) || text.charAt(position) == '.')) {
                    position++;
                }
                return Double.parseDouble(text.substring(start, position));
            }
            if (position < text.length() && Character.isLetter(text.charAt(position))) {
                int start = position;
                while (position < text.length() && Character.isLetterOrDigit(text.charAt(position))) {
                    position++;
                }
                return sheet.valueOf(text.substring(start, position));
            }
            throw new IllegalArgumentException(
                    "unexpected character at " + position + " in " + text);
        }

        void expectEnd() {
            skipSpaces();
            if (position != text.length()) {
                throw new IllegalArgumentException("trailing text in " + text);
            }
        }

        private void skipSpaces() {
            while (position < text.length() && Character.isWhitespace(text.charAt(position))) {
                position++;
            }
        }

        private boolean peek(char wanted) {
            return position < text.length() && text.charAt(position) == wanted;
        }
    }

    public static void main(String[] args) {
        Sheet sheet = new Sheet();
        sheet.set("A1", "10");
        sheet.set("B1", "=A1 * 2");
        sheet.set("C1", "=A1 + B1");
        sheet.set("D1", "=(A1 + B1) * 2");
        sheet.set("E1", "=C1 + A1");
        sheet.set("F1", "=A1 / 4");
        sheet.set("G1", "=-A1");
        sheet.set("H1", "=B1 - A1 + 1");

        // ---- plain numbers and formulas --------------------------------------
        check(sheet.valueOf("A1") == 10.0, "a literal is its own value");
        check(sheet.valueOf("B1") == 20.0, "multiplication");
        check(sheet.valueOf("C1") == 30.0, "addition of a reference and a cell holding a formula");
        check(sheet.valueOf("D1") == 60.0, "brackets change the order");
        check(sheet.valueOf("E1") == 40.0, "references chain");
        check(sheet.valueOf("F1") == 2.5, "division");
        check(sheet.valueOf("G1") == -10.0, "unary minus");
        check(sheet.valueOf("H1") == 11.0, "left to right through minus and plus");
        System.out.println("values       : A1=" + sheet.valueOf("A1")
                + " B1=" + sheet.valueOf("B1") + " C1=" + sheet.valueOf("C1")
                + " D1=" + sheet.valueOf("D1") + " E1=" + sheet.valueOf("E1"));

        // ---- case and spacing do not matter ----------------------------------
        check(sheet.valueOf("a1") == 10.0, "references are case insensitive");
        check(sheet.valueOf(" b1 ") == 20.0, "and whitespace is trimmed");
        sheet.set("Z9", "=  A1+  A1 ");
        check(sheet.valueOf("Z9") == 20.0, "spaces inside a formula are fine");

        // ---- dependencies are computed before their users --------------------
        sheet.valueOf("E1");     // pulls A1, then B1, then C1 in that order
        check(sheet.evaluationOrder().contains("A1"), "A1 was computed");
        check(sheet.evaluationOrder().indexOf("A1") < sheet.evaluationOrder().indexOf("B1"),
                "before B1, which depends on it");
        check(sheet.evaluationOrder().indexOf("B1") < sheet.evaluationOrder().indexOf("C1"),
                "and B1 before C1");
        System.out.println("evaluation   : " + sheet.evaluationOrder());

        // ---- memoisation ------------------------------------------------------
        List<String> before = sheet.evaluationOrder();
        check(sheet.valueOf("E1") == 40.0, "a second read gives the same answer");
        check(sheet.evaluationOrder().size() == before.size(), "and does no further work");
        System.out.println("memoised     : reading E1 again changed nothing");

        // ---- editing invalidates --------------------------------------------
        sheet.set("A1", "100");
        check(sheet.valueOf("B1") == 200.0, "changing A1 moves B1");
        check(sheet.valueOf("C1") == 300.0, "and everything downstream");
        check(sheet.valueOf("F1") == 25.0, "including the division");
        System.out.println("recalculated : A1=100 makes B1=" + sheet.valueOf("B1")
                + " and C1=" + sheet.valueOf("C1"));

        // ---- circular references ---------------------------------------------
        Sheet circular = new Sheet();
        circular.set("X1", "=Y1");
        circular.set("Y1", "=X1");
        try {
            circular.valueOf("X1");
            throw new AssertionError("a two cell cycle should be caught");
        } catch (CircularReferenceException expected) {
            check(expected.getMessage().contains("circular"), "the error says what went wrong");
            System.out.println("cycle        : " + expected.getMessage());
        }

        Sheet selfReference = new Sheet();
        selfReference.set("S1", "=S1 + 1");
        try {
            selfReference.valueOf("S1");
            throw new AssertionError("a cell referring to itself should be caught");
        } catch (CircularReferenceException expected) {
            System.out.println("self cycle   : caught");
        }

        Sheet longCycle = new Sheet();
        longCycle.set("A1", "=B1");
        longCycle.set("B1", "=C1");
        longCycle.set("C1", "=A1");
        try {
            longCycle.valueOf("A1");
            throw new AssertionError("a three cell cycle should be caught");
        } catch (CircularReferenceException expected) {
            System.out.println("long cycle   : caught after three hops");
        }

        // a diamond is not a cycle: two cells may both depend on the same one
        Sheet diamond = new Sheet();
        diamond.set("A1", "2");
        diamond.set("B1", "=A1 * 3");
        diamond.set("C1", "=A1 * 5");
        diamond.set("D1", "=B1 + C1");
        check(diamond.valueOf("D1") == 16.0, "a diamond shape resolves fine");
        System.out.println("diamond      : D1 = " + diamond.valueOf("D1"));

        // ---- errors -----------------------------------------------------------
        try {
            sheet.valueOf("Q7");
            throw new AssertionError("an empty cell should not silently return zero");
        } catch (IllegalArgumentException expected) {
            System.out.println("missing cell : " + expected.getMessage());
        }

        Sheet broken = new Sheet();
        broken.set("A1", "=1/0");
        try {
            broken.valueOf("A1");
            throw new AssertionError("division by zero should surface");
        } catch (ArithmeticException expected) {
            System.out.println("divide by 0  : " + expected.getMessage());
        }

        for (String bad : new String[] {"=A1 +", "=(A1", "=A1 @ 2", "=A1 A1", "="}) {
            Sheet candidate = new Sheet();
            candidate.set("A1", "1");
            candidate.set("B1", bad);
            try {
                candidate.valueOf("B1");
                throw new AssertionError("should have been rejected: " + bad);
            } catch (IllegalArgumentException expected) {
                // expected
            }
        }
        System.out.println("malformed    : five broken formulas all rejected");

        // ---- a small model ---------------------------------------------------
        Sheet model = new Sheet();
        model.set("A1", "1200");          // unit price
        model.set("A2", "3");             // quantity
        model.set("A3", "0.08");          // tax rate
        model.set("A4", "=A1 * A2");      // subtotal
        model.set("A5", "=A4 * A3");      // tax
        model.set("A6", "=A4 + A5");      // total
        check(model.valueOf("A4") == 3600.0, "the subtotal is computed");
        check(Math.abs(model.valueOf("A5") - 288.0) < 1e-9, "the tax is computed from the subtotal");
        check(Math.abs(model.valueOf("A6") - 3888.0) < 1e-9, "and the total adds them");
        System.out.println("model        : subtotal " + model.valueOf("A4")
                + ", tax " + model.valueOf("A5") + ", total " + model.valueOf("A6"));

        Map<String, Double> report = new LinkedHashMap<>();
        for (String cell : model.cells()) {
            report.put(cell, model.valueOf(cell));
        }
        check(report.size() == 6, "every cell can be read");
        System.out.println("report       : " + report);
        System.out.println("All checks passed.");
    }
}
