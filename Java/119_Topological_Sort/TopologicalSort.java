import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

/**
 * 119 - Topological sorting, two ways.
 *
 * The graph is an adjacency map from a prerequisite to the things that depend on
 * it. Kahn's algorithm repeatedly takes a node with no remaining prerequisites,
 * using a priority queue so the order is deterministic (alphabetical among ready
 * nodes). If some nodes never become ready, a depth-first search finds and
 * reports the exact cycle.
 *
 * A second pass groups nodes into layers (semesters) by dependency depth, and a
 * validator checks any proposed order.
 *
 * Compile and run:
 *   javac TopologicalSort.java
 *   java TopologicalSort
 */
public class TopologicalSort {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static final class CycleException extends RuntimeException {
        final List<String> cycle;

        CycleException(List<String> cycle) {
            super("cycle detected: " + String.join(" -> ", cycle));
            this.cycle = List.copyOf(cycle);
        }
    }

    private static Set<String> nodesOf(Map<String, List<String>> graph) {
        Set<String> nodes = new TreeSet<>(graph.keySet());
        for (List<String> children : graph.values()) {
            nodes.addAll(children);
        }
        return nodes;
    }

    private static Map<String, Integer> indegrees(Map<String, List<String>> graph, Set<String> nodes) {
        Map<String, Integer> indegree = new HashMap<>();
        for (String node : nodes) {
            indegree.put(node, 0);
        }
        for (List<String> children : graph.values()) {
            for (String child : children) {
                indegree.merge(child, 1, Integer::sum);
            }
        }
        return indegree;
    }

    static List<String> topologicalOrder(Map<String, List<String>> graph) {
        Set<String> nodes = nodesOf(graph);
        Map<String, Integer> indegree = indegrees(graph, nodes);

        PriorityQueue<String> ready = new PriorityQueue<>();
        for (String node : nodes) {
            if (indegree.get(node) == 0) {
                ready.add(node);
            }
        }

        List<String> order = new ArrayList<>();
        while (!ready.isEmpty()) {
            String node = ready.poll();
            order.add(node);
            for (String child : graph.getOrDefault(node, List.of())) {
                int remaining = indegree.merge(child, -1, Integer::sum);
                if (remaining == 0) {
                    ready.add(child);
                }
            }
        }

        if (order.size() != nodes.size()) {
            throw new CycleException(findCycle(graph));
        }
        return order;
    }

    static List<String> findCycle(Map<String, List<String>> graph) {
        Set<String> nodes = nodesOf(graph);
        Map<String, Integer> state = new HashMap<>();     // 0 unseen, 1 on stack, 2 done
        List<String> stack = new ArrayList<>();
        Map<String, Integer> position = new HashMap<>();

        for (String node : nodes) {
            if (state.getOrDefault(node, 0) == 0) {
                List<String> cycle = visit(node, graph, state, stack, position);
                if (cycle != null) {
                    return cycle;
                }
            }
        }
        return List.of();
    }

    private static List<String> visit(
            String node,
            Map<String, List<String>> graph,
            Map<String, Integer> state,
            List<String> stack,
            Map<String, Integer> position) {
        state.put(node, 1);
        position.put(node, stack.size());
        stack.add(node);

        for (String child : graph.getOrDefault(node, List.of())) {
            int childState = state.getOrDefault(child, 0);
            if (childState == 0) {
                List<String> found = visit(child, graph, state, stack, position);
                if (found != null) {
                    return found;
                }
            } else if (childState == 1) {
                List<String> cycle = new ArrayList<>(stack.subList(position.get(child), stack.size()));
                cycle.add(child);
                return cycle;
            }
        }

        stack.remove(stack.size() - 1);
        position.remove(node);
        state.put(node, 2);
        return null;
    }

    static List<List<String>> semesterPlan(Map<String, List<String>> graph) {
        Set<String> nodes = nodesOf(graph);
        Map<String, Integer> indegree = indegrees(graph, nodes);

        List<String> ready = new ArrayList<>();
        for (String node : nodes) {
            if (indegree.get(node) == 0) {
                ready.add(node);
            }
        }
        ready.sort(String::compareTo);

        List<List<String>> semesters = new ArrayList<>();
        int placed = 0;

        while (!ready.isEmpty()) {
            semesters.add(List.copyOf(ready));
            List<String> nextReady = new ArrayList<>();

            for (String node : ready) {
                placed++;
                for (String child : graph.getOrDefault(node, List.of())) {
                    int remaining = indegree.merge(child, -1, Integer::sum);
                    if (remaining == 0) {
                        nextReady.add(child);
                    }
                }
            }

            nextReady.sort(String::compareTo);
            ready = nextReady;
        }

        if (placed != nodes.size()) {
            throw new CycleException(findCycle(graph));
        }
        return semesters;
    }

    static boolean isValidOrder(Map<String, List<String>> graph, List<String> order) {
        Set<String> nodes = nodesOf(graph);
        if (order.size() != nodes.size() || new HashSet<>(order).size() != order.size()) {
            return false;
        }
        if (!new HashSet<>(order).equals(nodes)) {
            return false;
        }

        Map<String, Integer> position = new HashMap<>();
        for (int index = 0; index < order.size(); index++) {
            position.put(order.get(index), index);
        }

        for (Map.Entry<String, List<String>> entry : graph.entrySet()) {
            Integer parentPosition = position.get(entry.getKey());
            if (parentPosition == null) {
                continue;
            }
            for (String child : entry.getValue()) {
                if (position.get(child) < parentPosition) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void main(String[] args) {
        // ---- a realistic schedule -------------------------------------------
        Map<String, List<String>> schedule = new HashMap<>();
        schedule.put("intro", List.of("algorithms", "data-structures", "databases"));
        schedule.put("algorithms", List.of("compilers"));
        schedule.put("data-structures", List.of("operating-systems", "networks"));
        schedule.put("databases", List.of("capstone"));
        schedule.put("operating-systems", List.of("compilers", "distributed-systems"));
        schedule.put("networks", List.of("distributed-systems"));
        schedule.put("compilers", List.of("capstone"));
        schedule.put("distributed-systems", List.of("capstone"));

        List<String> order = topologicalOrder(schedule);
        System.out.println("order        : " + String.join(" -> ", order));
        check(order.size() == 9, "nine courses");
        check(isValidOrder(schedule, order), "order respects prerequisites");
        check(order.get(0).equals("intro"), "intro first");
        check(order.indexOf("compilers") < order.indexOf("capstone"), "compilers before capstone");

        List<List<String>> plan = semesterPlan(schedule);
        for (int index = 0; index < plan.size(); index++) {
            System.out.println("semester " + (index + 1) + "   : " + String.join(", ", plan.get(index)));
        }
        check(plan.get(0).equals(List.of("intro")), "first semester has only intro");
        check(plan.get(plan.size() - 1).contains("capstone"), "capstone is last");
        check(isValidOrder(schedule, plan.stream().flatMap(List::stream).toList()), "plan is a valid order");

        // ---- cycles ----------------------------------------------------------
        Map<String, List<String>> cyclic = new HashMap<>();
        cyclic.put("a", List.of("c"));
        cyclic.put("b", List.of("a"));
        cyclic.put("c", List.of("b"));

        try {
            topologicalOrder(cyclic);
            throw new AssertionError("a cycle should not produce an order");
        } catch (CycleException expected) {
            System.out.println("cycle        : " + expected.getMessage());
            check(expected.cycle.size() == 4, "cycle path has four entries: " + expected.cycle);
            check(expected.cycle.get(0).equals(expected.cycle.get(expected.cycle.size() - 1)),
                    "the reported cycle loops back to its start");
            check(new HashSet<>(expected.cycle.subList(0, 3)).equals(Set.of("a", "b", "c")),
                    "the cycle covers all three nodes");
        }

        Map<String, List<String>> selfLoop = new HashMap<>();
        selfLoop.put("only", List.of("only"));
        try {
            topologicalOrder(selfLoop);
            throw new AssertionError("a self loop is a cycle");
        } catch (CycleException expected) {
            check(expected.cycle.equals(List.of("only", "only")), "self loop reported");
            System.out.println("self loop    : " + expected.getMessage());
        }

        // ---- empty and isolated nodes ---------------------------------------
        check(topologicalOrder(new HashMap<>()).isEmpty(), "empty graph has no order");
        check(semesterPlan(new HashMap<>()).isEmpty(), "empty graph has no semesters");
        check(topologicalOrder(Map.of("solo", List.of())).equals(List.of("solo")), "single node");
        check(isValidOrder(Map.of("solo", List.of()), List.of("solo")), "single node valid");
        System.out.println("edge cases   : empty and single-node graphs handled");

        // ---- nodes that only appear as children ------------------------------
        Map<String, List<String>> implicit = new HashMap<>();
        implicit.put("math", List.of("course"));
        implicit.put("logic", List.of("course"));
        List<String> implicitOrder = topologicalOrder(implicit);
        check(isValidOrder(implicit, implicitOrder), "implicit nodes are ordered");
        check(implicitOrder.indexOf("logic") < implicitOrder.indexOf("course"), "logic before course");
        System.out.println("implicit     : " + implicitOrder);

        // ---- validation rejects bad orders -----------------------------------
        check(!isValidOrder(schedule, List.of("intro")), "too short");
        check(!isValidOrder(schedule, List.of("intro", "intro")), "duplicate entries");
        check(!isValidOrder(schedule, List.of("capstone", "intro")), "wrong direction");
        check(isValidOrder(schedule, topologicalOrder(schedule)), "generated order is accepted");
        System.out.println("validation   : short, duplicate, and reversed orders rejected");

        // ---- randomised DAGs -------------------------------------------------
        Random random = new Random(20261106);
        int checked = 0;

        for (int trial = 0; trial < 300; trial++) {
            int size = 12;
            List<String> names = new ArrayList<>();
            for (int index = 0; index < size; index++) {
                names.add(String.format("c%02d", index));
            }

            Map<String, List<String>> graph = new HashMap<>();
            for (String name : names) {
                graph.put(name, new ArrayList<>());
            }
            for (int dependent = 0; dependent < size; dependent++) {
                for (int prerequisite = 0; prerequisite < dependent; prerequisite++) {
                    if (random.nextInt(5) == 0) {
                        graph.get(names.get(prerequisite)).add(names.get(dependent));
                    }
                }
            }

            List<String> randomOrder = topologicalOrder(graph);
            check(randomOrder.size() == size, "every node ordered");
            check(isValidOrder(graph, randomOrder), "random DAG order is valid");
            check(findCycle(graph).isEmpty(), "no cycle in a DAG");

            List<String> flattened = semesterPlan(graph).stream().flatMap(List::stream).toList();
            check(isValidOrder(graph, flattened), "semester plan is valid");

            // A pair of back edges between the first and last node creates a cycle.
            graph.get(names.get(0)).add(names.get(size - 1));
            graph.get(names.get(size - 1)).add(names.get(0));
            check(!findCycle(graph).isEmpty(), "back edge creates a cycle");
            try {
                topologicalOrder(graph);
                throw new AssertionError("back edge should be detected");
            } catch (CycleException expected) {
                check(expected.cycle.size() >= 2, "cycle path is reported");
            }
            checked++;
        }
        System.out.println("random check : " + checked + " DAGs ordered and forced into a cycle");

        // ---- determinism and a larger graph ---------------------------------
        List<String> first = topologicalOrder(schedule);
        List<String> second = topologicalOrder(schedule);
        check(first.equals(second), "the order is deterministic");
        check(first.equals(List.of(
                "intro", "algorithms", "data-structures", "databases",
                "networks", "operating-systems", "compilers",
                "distributed-systems", "capstone")),
                "lexicographic Kahn order: " + first);
        System.out.println("determinism  : same graph, same order");

        Map<String, List<String>> chain = new HashMap<>();
        for (int index = 0; index < 2_000; index++) {
            chain.put("n" + index, List.of("n" + (index + 1)));
        }
        List<String> chainOrder = topologicalOrder(chain);
        check(chainOrder.size() == 2_001, "chain includes the final node");
        check(chainOrder.get(0).equals("n0") && chainOrder.get(chainOrder.size() - 1).equals("n2000"),
                "chain order follows the edges");
        System.out.println("large graph  : 2,000-edge chain ordered");

        System.out.println("All checks passed.");
    }
}
