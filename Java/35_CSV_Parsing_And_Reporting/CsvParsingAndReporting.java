import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 35 - CSV parsing and reporting: a small quoted-field parser, aggregation with
 * streams, and a written report - all in a temporary folder.
 *
 * Compile and run:
 *   javac CsvParsingAndReporting.java
 *   java CsvParsingAndReporting
 */
public class CsvParsingAndReporting {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    // Built with \n escapes rather than a text block: a row ending in a quoted
    // field has three quote characters in a row, which would close a text block.
    static final String CSV = "name,department,salary\n"
            + "Alice,Engineering,95000\n"
            + "\"Bob, Jr.\",Sales,72000\n"
            + "Carol,Engineering,101000\n"
            + "\"Dan \"\"The Man\"\"\",Support,54000\n";

    /** Handle quoted fields, embedded commas and doubled quotes. */
    static List<String> parseLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char character = line.charAt(i);
            if (inQuotes) {
                if (character == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');   // "" is one literal quote
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(character);
                }
            } else if (character == '"') {
                inQuotes = true;
            } else if (character == ',') {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }
        fields.add(current.toString());
        return fields;
    }

    static String quote(String value) {
        return value.contains(",") || value.contains("\"")
                ? "\"" + value.replace("\"", "\"\"") + "\""
                : value;
    }

    record Employee(String name, String department, int salary) {
    }

    static void deleteTree(Path root) throws IOException {
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        Path dir = Files.createTempDirectory("java-csv-");
        try {
            List<String> lines = CSV.lines().filter(line -> !line.isBlank()).toList();
            check(lines.size() == 5, "one header plus four rows");
            check(parseLine(lines.get(2)).get(0).equals("Bob, Jr."),
                    "a comma inside quotes stays in one field");
            check(parseLine(lines.get(4)).get(0).equals("Dan \"The Man\""),
                    "doubled quotes become one quote");
            check(parseLine(lines.get(1)).size() == 3, "a plain row has three fields");

            List<Employee> staff = lines.stream()
                    .skip(1)
                    .map(CsvParsingAndReporting::parseLine)
                    .map(fields -> new Employee(fields.get(0), fields.get(1),
                            Integer.parseInt(fields.get(2))))
                    .toList();
            check(staff.size() == 4, "four employees parsed");
            check(staff.get(1).name().equals("Bob, Jr."), "quoted name survived parsing");

            int total = staff.stream().mapToInt(Employee::salary).sum();
            double average = staff.stream().mapToInt(Employee::salary).average().orElse(0);
            check(total == 322_000, "total salary");
            check(Math.abs(average - 80_500.0) < 0.001, "average salary");

            Map<String, Long> headcount = staff.stream().collect(Collectors.groupingBy(
                    Employee::department, TreeMap::new, Collectors.counting()));
            Map<String, Integer> departmentTotals = staff.stream().collect(Collectors.groupingBy(
                    Employee::department, TreeMap::new, Collectors.summingInt(Employee::salary)));
            check(headcount.get("Engineering") == 2L, "Engineering has two people");
            check(departmentTotals.get("Sales") == 72_000, "Sales total");
            System.out.println("headcount    : " + headcount);
            System.out.println("totals       : " + departmentTotals);

            // write the report back out as CSV, quoting where needed
            Path report = dir.resolve("report.csv");
            StringBuilder out = new StringBuilder("department,headcount,total_salary"
                    + System.lineSeparator());
            for (Map.Entry<String, Long> entry : headcount.entrySet()) {
                out.append(quote(entry.getKey())).append(',')
                        .append(entry.getValue()).append(',')
                        .append(departmentTotals.get(entry.getKey()))
                        .append(System.lineSeparator());
            }
            Files.writeString(report, out.toString());

            List<String> written = Files.readAllLines(report);
            check(written.size() == 4, "report has a header plus three departments");
            check(written.get(0).equals("department,headcount,total_salary"), "report header");
            check(written.get(1).equals("Engineering,2,196000"), "first report row");
            System.out.println("report       : " + report.getFileName() + " -> " + written.get(1));

            check(quote("plain").equals("plain"), "no quoting when it is not needed");
            check(quote("a,b").equals("\"a,b\""), "quoting when a comma is present");
            System.out.printf("summary      : %d people, total %d, average %.0f%n",
                    staff.size(), total, average);
        } finally {
            deleteTree(dir);
        }
        System.out.println("All checks passed (temp tree cleaned up).");
    }
}
