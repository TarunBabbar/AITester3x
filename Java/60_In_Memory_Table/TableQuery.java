import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.OptionalInt;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

/**
 * 60 - An in-memory table with a small query API: filter, sort, map, limit and
 * group by, plus summary statistics (sum, mean, median, percentile, deviation).
 *
 * Compile and run:
 *   javac TableQuery.java
 *   java TableQuery
 */
public class TableQuery {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    record Employee(String name, String dept, int salary, int years) {
    }

    /** An immutable collection of rows you can query. */
    static final class Table<T> {
        private final List<T> rows;

        private Table(List<T> rows) {
            this.rows = List.copyOf(rows);
        }

        static <T> Table<T> of(List<T> rows) {
            return new Table<>(rows);
        }

        Table<T> filter(Predicate<T> predicate) {
            return new Table<>(rows.stream().filter(predicate).toList());
        }

        Table<T> sort(Comparator<T> comparator) {
            return new Table<>(rows.stream().sorted(comparator).toList());
        }

        <R> Table<R> map(Function<T, R> mapper) {
            return new Table<>(rows.stream().map(mapper).toList());
        }

        Table<T> limit(int count) {
            return new Table<>(rows.stream().limit(count).toList());
        }

        <K> Map<K, List<T>> groupBy(Function<T, K> key) {
            return rows.stream().collect(Collectors.groupingBy(key, TreeMap::new, Collectors.toList()));
        }

        <K> Map<K, Integer> groupSum(Function<T, K> key, ToIntFunction<T> field) {
            return rows.stream().collect(Collectors.groupingBy(key, TreeMap::new,
                    Collectors.summingInt(field)));
        }

        int size() {
            return rows.size();
        }

        boolean isEmpty() {
            return rows.isEmpty();
        }

        List<T> toList() {
            return rows;
        }

        int sum(ToIntFunction<T> field) {
            return rows.stream().mapToInt(field).sum();
        }

        double mean(ToIntFunction<T> field) {
            return rows.stream().mapToInt(field).average().orElse(0);
        }

        OptionalInt maximum(ToIntFunction<T> field) {
            return rows.stream().mapToInt(field).max();
        }

        double median(ToIntFunction<T> field) {
            int[] values = rows.stream().mapToInt(field).sorted().toArray();
            if (values.length == 0) {
                return 0;
            }
            int middle = values.length / 2;
            return values.length % 2 == 1
                    ? values[middle]
                    : (values[middle - 1] + values[middle]) / 2.0;
        }

        /**
         * Nearest-rank percentile: the smallest value at or above which p% of the
         * data falls. Note it need not equal the median for an even row count.
         */
        int percentile(ToIntFunction<T> field, int percent) {
            if (rows.isEmpty()) {
                throw new IllegalStateException("no rows");
            }
            int[] values = rows.stream().mapToInt(field).sorted().toArray();
            int rank = (int) Math.ceil(percent / 100.0 * values.length);
            return values[Math.clamp(rank - 1, 0, values.length - 1)];
        }

        double standardDeviation(ToIntFunction<T> field) {
            if (rows.isEmpty()) {
                return 0;
            }
            double mean = mean(field);
            double variance = rows.stream()
                    .mapToDouble(row -> Math.pow(field.applyAsInt(row) - mean, 2))
                    .sum() / rows.size();
            return Math.sqrt(variance);
        }

        T first() {
            if (rows.isEmpty()) {
                throw new NoSuchElementException("the table has no rows");
            }
            return rows.get(0);
        }
    }

    public static void main(String[] args) {
        Table<Employee> staff = Table.of(List.of(
                new Employee("Alice", "Eng", 95_000, 5),
                new Employee("Bob", "Sales", 72_000, 3),
                new Employee("Carol", "Eng", 101_000, 8),
                new Employee("Dan", "Support", 54_000, 2),
                new Employee("Erin", "Eng", 88_000, 4),
                new Employee("Frank", "Sales", 66_000, 6)));

        check(staff.size() == 6, "six rows");
        check(!staff.isEmpty(), "and it is not empty");

        // ---- filter, sort, map, limit ----------------------------------------
        check(staff.filter(person -> person.dept().equals("Eng")).size() == 3, "filter");

        List<String> engineering = staff
                .filter(person -> person.dept().equals("Eng"))
                .sort(Comparator.comparingInt(Employee::salary).reversed())
                .map(Employee::name)
                .toList();
        check(engineering.equals(List.of("Carol", "Alice", "Erin")), "a chain of query steps");
        System.out.println("eng by pay  : " + engineering);

        check(staff.sort(Comparator.comparingInt(Employee::salary)).first().name().equals("Dan"),
                "the lowest paid is Dan");
        check(staff.sort(Comparator.comparingInt(Employee::salary).reversed())
                .limit(2).toList().get(1).name().equals("Alice"), "the second highest is Alice");
        check(staff.map(Employee::name).size() == 6, "map keeps the row count");

        // ---- grouping ---------------------------------------------------------
        Map<String, List<Employee>> byDept = staff.groupBy(Employee::dept);
        check(byDept.keySet().toString().equals("[Eng, Sales, Support]"), "TreeMap keeps the keys sorted");
        check(byDept.get("Sales").size() == 2, "Sales has two people");

        Map<String, Integer> payroll = staff.groupSum(Employee::dept, Employee::salary);
        check(payroll.get("Eng") == 284_000, "Engineering payroll");
        check(payroll.get("Support") == 54_000, "Support payroll");
        System.out.println("payroll     : " + payroll);

        // ---- statistics -------------------------------------------------------
        check(staff.sum(Employee::salary) == 476_000, "total salary");
        check(Math.abs(staff.mean(Employee::salary) - 79_333.3333) < 0.001, "mean salary");
        check(staff.maximum(Employee::salary).orElse(0) == 101_000, "the maximum");
        check(staff.median(Employee::salary) == 80_000.0,
                "median of an even count averages the two middle values");
        check(staff.percentile(Employee::salary, 90) == 101_000, "the 90th percentile");
        check(staff.percentile(Employee::salary, 50) == 72_000,
                "the nearest-rank 50th percentile differs from the median here");
        check(Math.abs(staff.standardDeviation(Employee::salary) - 16_650.0) < 1.0,
                "standard deviation");
        System.out.printf("salary      : sum=%d mean=%.0f median=%.0f p90=%d sd=%.0f%n",
                staff.sum(Employee::salary), staff.mean(Employee::salary),
                staff.median(Employee::salary), staff.percentile(Employee::salary, 90),
                staff.standardDeviation(Employee::salary));

        // ---- edge cases -------------------------------------------------------
        check(staff.filter(person -> person.salary() > 1_000_000).isEmpty(), "an empty result");
        check(staff.filter(person -> person.salary() > 1_000_000).sum(Employee::salary) == 0,
                "summing nothing is 0");
        check(staff.filter(person -> person.salary() > 1_000_000).median(Employee::salary) == 0,
                "the median of nothing is 0 by convention");
        check(staff.percentile(Employee::salary, 0) == 54_000, "the 0th percentile is the minimum");
        check(staff.percentile(Employee::salary, 100) == 101_000, "the 100th is the maximum");
        try {
            staff.filter(person -> false).first();
            throw new AssertionError("first() on an empty table should fail");
        } catch (NoSuchElementException expected) {
            System.out.println("empty table : " + expected.getMessage());
        }
        System.out.println("All checks passed.");
    }
}
