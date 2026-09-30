import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeMap;

/**
 * 76 - Graphs part two: topological sorting with cycle detection, a disjoint set
 * with path compression and union by rank, and Kruskal's minimum spanning tree.
 *
 * Compile and run:
 *   javac GraphAlgorithms2.java
 *   java GraphAlgorithms2
 */
public class GraphAlgorithms2 {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    // ------------------------------------------------- topological sorting
    /**
     * Kahn's algorithm: repeatedly take a node with no remaining prerequisites.
     * A priority queue makes the output deterministic instead of arbitrary.
     */
    static List<String> topologicalSort(Map<String, List<String>> dependencies) {
        Map<String, Integer> indegree = new TreeMap<>();
        dependencies.keySet().forEach(node -> indegree.putIfAbsent(node, 0));
        for (List<String> targets : dependencies.values()) {
            for (String target : targets) {
                indegree.merge(target, 1, Integer::sum);
            }
        }

        PriorityQueue<String> ready = new PriorityQueue<>();
        indegree.forEach((node, count) -> {
            if (count == 0) {
                ready.add(node);
            }
        });

        List<String> order = new ArrayList<>();
        while (!ready.isEmpty()) {
            String node = ready.poll();
            order.add(node);
            for (String target : dependencies.getOrDefault(node, List.of())) {
                if (indegree.merge(target, -1, Integer::sum) == 0) {
                    ready.add(target);
                }
            }
        }
        if (order.size() != indegree.size()) {
            throw new IllegalStateException("the graph contains a cycle, so it cannot be ordered");
        }
        return order;
    }

    // ------------------------------------------------------- disjoint set
    static final class DisjointSet {
        private final int[] parent;
        private final int[] rank;
        private int components;

        DisjointSet(int size) {
            parent = new int[size];
            rank = new int[size];
            for (int i = 0; i < size; i++) {
                parent[i] = i;
            }
            components = size;
        }

        int find(int value) {
            if (parent[value] != value) {
                parent[value] = find(parent[value]);   // path compression
            }
            return parent[value];
        }

        /** Returns false when the two are already connected, which means a cycle. */
        boolean union(int left, int right) {
            int rootLeft = find(left);
            int rootRight = find(right);
            if (rootLeft == rootRight) {
                return false;
            }
            if (rank[rootLeft] < rank[rootRight]) {
                int swap = rootLeft;
                rootLeft = rootRight;
                rootRight = swap;
            }
            parent[rootRight] = rootLeft;               // attach the shorter tree
            if (rank[rootLeft] == rank[rootRight]) {
                rank[rootLeft]++;
            }
            components--;
            return true;
        }

        boolean connected(int left, int right) {
            return find(left) == find(right);
        }

        int components() {
            return components;
        }
    }

    // ------------------------------------------------------------- Kruskal
    record Edge(String from, String to, int weight) {
        @Override
        public String toString() {
            return from + "-" + to + "(" + weight + ")";
        }
    }

    record Mst(int totalWeight, List<Edge> edges) {
    }

    static Mst kruskal(List<String> nodes, List<Edge> edges) {
        Map<String, Integer> index = new LinkedHashMap<>();
        for (int i = 0; i < nodes.size(); i++) {
            index.put(nodes.get(i), i);
        }

        List<Edge> ordered = new ArrayList<>(edges);
        ordered.sort(Comparator.comparingInt(Edge::weight)
                .thenComparing(Edge::from)
                .thenComparing(Edge::to));

        DisjointSet sets = new DisjointSet(nodes.size());
        List<Edge> chosen = new ArrayList<>();
        int total = 0;
        for (Edge edge : ordered) {
            if (sets.union(index.get(edge.from()), index.get(edge.to()))) {
                chosen.add(edge);
                total += edge.weight();
            }
        }
        return new Mst(total, chosen);
    }

    public static void main(String[] args) {
        // ---- topological sorting ---------------------------------------------
        Map<String, List<String>> build = new LinkedHashMap<>();
        build.put("A", List.of("C"));
        build.put("B", List.of("C", "D"));
        build.put("C", List.of("D"));
        build.put("D", List.of("E"));
        build.put("E", List.of());

        List<String> order = topologicalSort(build);
        check(order.equals(List.of("A", "B", "C", "D", "E")), "dependencies come before dependents");
        check(order.indexOf("A") < order.indexOf("C"), "A before C");
        check(order.indexOf("B") < order.indexOf("D"), "B before D");
        check(order.indexOf("C") < order.indexOf("D"), "C before D");
        check(order.indexOf("D") < order.indexOf("E"), "D before E");
        System.out.println("topological : " + order);

        check(topologicalSort(Map.of("solo", List.of())).equals(List.of("solo")), "a single node");
        check(topologicalSort(Map.of()).isEmpty(), "an empty graph");

        Map<String, List<String>> cyclic = new LinkedHashMap<>();
        cyclic.put("A", List.of("B"));
        cyclic.put("B", List.of("A"));
        try {
            topologicalSort(cyclic);
            throw new AssertionError("a cycle should have been rejected");
        } catch (IllegalStateException expected) {
            System.out.println("cycle       : " + expected.getMessage());
        }

        Map<String, List<String>> selfLoop = new LinkedHashMap<>();
        selfLoop.put("A", List.of("A"));
        try {
            topologicalSort(selfLoop);
            throw new AssertionError("a self loop is a cycle");
        } catch (IllegalStateException expected) {
            System.out.println("self loop   : rejected as well");
        }

        // ---- disjoint set -----------------------------------------------------
        DisjointSet sets = new DisjointSet(6);
        check(sets.components() == 6, "six singletons to start with");
        check(sets.union(0, 1), "0 and 1 join");
        check(sets.union(1, 2), "then 2 joins their group");
        check(!sets.union(0, 2), "0 and 2 were already connected, so this would be a cycle");
        check(sets.connected(0, 2), "and they are connected");
        check(!sets.connected(3, 0), "3 is still its own group");
        check(sets.components() == 4, "four groups after two merges");
        check(sets.find(2) == sets.find(0), "find returns the same root after compression");
        System.out.println("components  : " + sets.components() + " after joining 0-1-2");

        // ---- Kruskal ----------------------------------------------------------
        List<String> nodes = List.of("A", "B", "C", "D", "E", "F");
        List<Edge> edges = List.of(
                new Edge("A", "B", 4), new Edge("A", "C", 2), new Edge("B", "C", 5),
                new Edge("B", "D", 10), new Edge("C", "E", 3), new Edge("E", "D", 4),
                new Edge("D", "F", 11), new Edge("E", "F", 9));

        Mst mst = kruskal(nodes, edges);
        check(mst.totalWeight() == 22, "the minimum spanning tree weighs 22");
        check(mst.edges().size() == nodes.size() - 1, "a spanning tree of six nodes has five edges");
        check(mst.edges().equals(List.of(
                new Edge("A", "C", 2), new Edge("C", "E", 3), new Edge("A", "B", 4),
                new Edge("E", "D", 4), new Edge("E", "F", 9))), "and it picks the cheap edges first");
        System.out.println("mst         : weight " + mst.totalWeight() + " via " + mst.edges());

        // the MST is cheaper than any other spanning tree: check the obvious rival
        int rival = 2 + 3 + 4 + 9 + 10;
        check(mst.totalWeight() < rival, "swapping E-D for B-D would cost more");

        // ---- cycles via the disjoint set --------------------------------------
        DisjointSet forest = new DisjointSet(nodes.size());
        Map<String, Integer> index = new LinkedHashMap<>();
        for (int i = 0; i < nodes.size(); i++) {
            index.put(nodes.get(i), i);
        }
        List<Edge> cycleClosers = new ArrayList<>();
        for (Edge edge : edges) {
            if (!forest.union(index.get(edge.from()), index.get(edge.to()))) {
                cycleClosers.add(edge);
            }
        }
        check(cycleClosers.contains(new Edge("B", "C", 5)), "B-C closes a cycle");
        check(forest.components() == 1, "the graph is fully connected");
        System.out.println("cycle edges : " + cycleClosers);

        // a disconnected graph keeps separate groups
        DisjointSet split = new DisjointSet(5);
        split.union(0, 1);
        split.union(3, 4);
        check(split.components() == 3, "two islands plus one isolated node is three groups");
        check(!split.connected(1, 3), "the islands are separate");
        System.out.println("All checks passed.");
    }
}
