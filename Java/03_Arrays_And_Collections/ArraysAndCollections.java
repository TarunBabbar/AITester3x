import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 03 - Arrays and collections: arrays, List, Set and Map.
 *
 * Compile and run:
 *   javac ArraysAndCollections.java
 *   java ArraysAndCollections
 */
public class ArraysAndCollections {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) {
        int[] scores = {88, 42, 95, 67};
        int total = 0;
        for (int score : scores) {
            total += score;
        }
        double average = (double) total / scores.length;
        check(scores.length == 4, "there are four scores");
        check(Math.abs(average - 73.0) < 0.001, "average is 73");

        List<String> fruits = new ArrayList<>(List.of("banana", "apple", "cherry"));
        fruits.add("date");
        fruits.sort(Comparator.naturalOrder());
        check(fruits.get(0).equals("apple"), "sorted first element is apple");
        check(fruits.size() == 4, "there are four fruits");
        check(fruits.contains("cherry"), "the list contains cherry");

        Set<String> tags = new HashSet<>(List.of("java", "test", "java"));
        check(tags.size() == 2, "duplicates collapse inside a Set");

        Map<String, Integer> stock = new HashMap<>();
        stock.put("apple", 5);
        stock.put("banana", 3);
        stock.merge("apple", 2, Integer::sum);
        check(stock.get("apple") == 7, "merge adds to the existing value");
        check(stock.getOrDefault("pear", 0) == 0, "missing key falls back to 0");
        check(stock.containsKey("banana") && !stock.containsKey("pear"), "containsKey");

        System.out.println("scores  : " + Arrays.toString(scores));
        System.out.printf("average : %.1f%n", average);
        System.out.println("fruits  : " + fruits);
        System.out.println("tags    : " + tags);
        System.out.println("stock   : " + stock);
        System.out.println("All checks passed.");
    }
}
