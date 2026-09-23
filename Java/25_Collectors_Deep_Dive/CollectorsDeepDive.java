import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 25 - Stream collectors: groupingBy, partitioningBy, mapping, joining, toMap,
 * summarizing and teeing, with and without downstream collectors.
 *
 * Compile and run:
 *   javac CollectorsDeepDive.java
 *   java CollectorsDeepDive
 */
public class CollectorsDeepDive {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    record Employee(String name, String dept, int salary, boolean active) {
    }

    public static void main(String[] args) {
        List<Employee> staff = List.of(
                new Employee("Alice", "Eng", 95_000, true),
                new Employee("Bob", "Sales", 72_000, true),
                new Employee("Carol", "Eng", 101_000, false),
                new Employee("Dan", "Support", 54_000, true),
                new Employee("Erin", "Eng", 88_000, true),
                new Employee("Frank", "Sales", 66_000, false));

        // groupingBy -> the whole objects
        Map<String, List<Employee>> byDept = staff.stream()
                .collect(Collectors.groupingBy(Employee::dept));
        check(byDept.size() == 3, "three departments");
        check(byDept.get("Eng").size() == 3, "Eng has three people");

        // groupingBy + counting
        Map<String, Long> countByDept = staff.stream()
                .collect(Collectors.groupingBy(Employee::dept, Collectors.counting()));
        check(countByDept.get("Sales") == 2L, "Sales has two");
        System.out.println("count by dept : " + countByDept);

        // groupingBy + averagingInt, into a sorted TreeMap
        Map<String, Double> avgByDept = staff.stream().collect(Collectors.groupingBy(
                Employee::dept, TreeMap::new, Collectors.averagingInt(Employee::salary)));
        check(Math.abs(avgByDept.get("Eng") - (95_000 + 101_000 + 88_000) / 3.0) < 0.001,
                "Eng average salary");
        System.out.println("avg by dept   : " + avgByDept);

        // groupingBy + mapping (collect just the names)
        Map<String, List<String>> namesByDept = staff.stream().collect(Collectors.groupingBy(
                Employee::dept, Collectors.mapping(Employee::name, Collectors.toList())));
        check(namesByDept.get("Sales").equals(List.of("Bob", "Frank")), "names by dept");
        System.out.println("names by dept : " + namesByDept);

        // partitioningBy always returns exactly two keys, true and false
        Map<Boolean, List<Employee>> byActive = staff.stream()
                .collect(Collectors.partitioningBy(Employee::active));
        check(byActive.size() == 2, "partition has two keys");
        check(byActive.get(true).size() == 4 && byActive.get(false).size() == 2, "active split");
        Map<Boolean, Long> activeCounts = staff.stream()
                .collect(Collectors.partitioningBy(Employee::active, Collectors.counting()));
        check(activeCounts.get(false) == 2L, "partition + counting");

        // joining with a delimiter, prefix and suffix
        String engNames = staff.stream()
                .filter(person -> person.dept().equals("Eng"))
                .map(Employee::name)
                .sorted()
                .collect(Collectors.joining(", ", "[", "]"));
        check(engNames.equals("[Alice, Carol, Erin]"), "joining");
        System.out.println("eng names     : " + engNames);

        // toMap
        Map<String, Integer> salaryByName = staff.stream()
                .collect(Collectors.toMap(Employee::name, Employee::salary));
        check(salaryByName.get("Carol") == 101_000, "toMap lookup");

        // summarizingInt gives count, sum, min, max and average in one pass
        IntSummaryStatistics stats = staff.stream()
                .collect(Collectors.summarizingInt(Employee::salary));
        check(stats.getCount() == 6 && stats.getMin() == 54_000 && stats.getMax() == 101_000,
                "summary statistics");
        System.out.println("salaries      : " + stats);

        // teeing: two different results from one pass
        record Summary(double avgActiveSalary, long inactiveCount) {
        }
        Summary summary = staff.stream().collect(Collectors.teeing(
                Collectors.filtering(Employee::active, Collectors.averagingInt(Employee::salary)),
                Collectors.filtering(person -> !person.active(), Collectors.counting()),
                Summary::new));
        check(Math.abs(summary.avgActiveSalary() - 77_250.0) < 0.001, "average of active salaries");
        check(summary.inactiveCount() == 2, "inactive count from the same pass");
        System.out.println("teeing        : " + summary);
        System.out.println("All checks passed.");
    }
}
