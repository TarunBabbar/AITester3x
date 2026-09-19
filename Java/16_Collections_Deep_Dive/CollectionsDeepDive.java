import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * 16 - Collections deep dive: Deque as queue/stack, PriorityQueue, TreeMap,
 * TreeSet and LinkedHashMap ordering guarantees.
 *
 * Compile and run:
 *   javac CollectionsDeepDive.java
 *   java CollectionsDeepDive
 */
public class CollectionsDeepDive {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) {
        Deque<String> queue = new ArrayDeque<>();
        queue.addLast("first");
        queue.addLast("second");
        queue.addLast("third");
        check(queue.removeFirst().equals("first"), "a Deque used as a queue is FIFO");
        check(queue.peekFirst().equals("second"), "peekFirst looks without removing");

        Deque<String> stack = new ArrayDeque<>();
        stack.push("a");
        stack.push("b");
        stack.push("c");
        check(stack.pop().equals("c"), "a Deque used as a stack is LIFO");

        Queue<Integer> minHeap = new PriorityQueue<>(List.of(5, 1, 9, 3));
        check(minHeap.poll() == 1, "a PriorityQueue gives the smallest first");
        check(minHeap.peek() == 3, "peek shows the next smallest");

        Queue<Integer> maxHeap = new PriorityQueue<>(Comparator.reverseOrder());
        maxHeap.addAll(List.of(5, 1, 9, 3));
        check(maxHeap.poll() == 9, "reverseOrder turns it into a max-heap");

        TreeMap<String, Integer> scores = new TreeMap<>();
        scores.put("dan", 70);
        scores.put("alice", 90);
        scores.put("carol", 80);
        check(scores.firstKey().equals("alice"), "TreeMap keeps keys sorted");
        check(scores.lastKey().equals("dan"), "lastKey returns the largest key");
        check(scores.headMap("carol").keySet().equals(Set.of("alice")), "headMap is exclusive");
        check(scores.subMap("alice", "dan").size() == 2, "subMap is half-open");

        TreeSet<Integer> unique = new TreeSet<>(List.of(4, 2, 4, 8, 2));
        check(unique.size() == 3, "a TreeSet removes duplicates");
        check(unique.first() == 2 && unique.last() == 8, "a TreeSet stays sorted");
        check(unique.higher(4) == 8 && unique.lower(4) == 2, "higher and lower neighbours");

        Map<String, Integer> insertion = new LinkedHashMap<>();
        insertion.put("z", 1);
        insertion.put("a", 2);
        insertion.put("m", 3);
        check(insertion.keySet().iterator().next().equals("z"),
                "LinkedHashMap preserves insertion order");

        Map<String, Integer> accessOrdered = new LinkedHashMap<>(16, 0.75f, true);
        accessOrdered.put("one", 1);
        accessOrdered.put("two", 2);
        accessOrdered.put("three", 3);
        accessOrdered.get("one"); // touches "one", moving it last
        check(accessOrdered.keySet().iterator().next().equals("two"),
                "access-ordered LinkedHashMap moves read keys to the end");

        System.out.println("queue after poll    : " + queue);
        System.out.println("min-heap after poll : " + minHeap);
        System.out.println("scores (sorted)     : " + scores);
        System.out.println("unique sorted       : " + unique);
        System.out.println("insertion order     : " + insertion.keySet());
        System.out.println("access order        : " + accessOrdered.keySet());
        System.out.println("All checks passed.");
    }
}
