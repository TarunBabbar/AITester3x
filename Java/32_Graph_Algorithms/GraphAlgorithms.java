import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;

/**
 * 32 - Graph algorithms: adjacency lists with breadth-first search, depth-first
 * search and Dijkstra's shortest path.
 *
 * Compile and run:
 *   javac GraphAlgorithms.java
 *   java GraphAlgorithms
 */
public class GraphAlgorithms {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    record Edge(String to, int weight) {
    }

    static Map<String, List<Edge>> buildGraph() {
        Map<String, List<Edge>> graph = new TreeMap<>();
        String[][] edges = {
                {"A", "B", "4"}, {"A", "C", "2"}, {"B", "C", "5"}, {"B", "D", "10"},
                {"C", "E", "3"}, {"E", "D", "4"}, {"D", "F", "11"}, {"E", "F", "9"},
        };
        for (String[] edge : edges) {
            int weight = Integer.parseInt(edge[2]);
            graph.computeIfAbsent(edge[0], key -> new ArrayList<>()).add(new Edge(edge[1], weight));
            graph.computeIfAbsent(edge[1], key -> new ArrayList<>()).add(new Edge(edge[0], weight));
        }
        return graph;
    }

    /** Fewest hops, ignoring weights. */
    static List<String> bfs(Map<String, List<Edge>> graph, String start, String goal) {
        Deque<List<String>> queue = new ArrayDeque<>();
        Set<String> seen = new HashSet<>();
        queue.add(List.of(start));
        seen.add(start);
        while (!queue.isEmpty()) {
            List<String> path = queue.removeFirst();
            String node = path.get(path.size() - 1);
            if (node.equals(goal)) {
                return path;
            }
            for (Edge edge : graph.getOrDefault(node, List.of())) {
                if (seen.add(edge.to())) {
                    List<String> longer = new ArrayList<>(path);
                    longer.add(edge.to());
                    queue.addLast(longer);
                }
            }
        }
        return List.of();
    }

    /** Every node reachable from start, in depth-first order. */
    static List<String> dfs(Map<String, List<Edge>> graph, String start) {
        List<String> visited = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        Deque<String> stack = new ArrayDeque<>();
        stack.push(start);
        while (!stack.isEmpty()) {
            String node = stack.pop();
            if (!seen.add(node)) {
                continue;
            }
            visited.add(node);
            List<Edge> neighbours = new ArrayList<>(graph.getOrDefault(node, List.of()));
            neighbours.sort(Comparator.comparing(Edge::to).reversed());
            for (Edge edge : neighbours) {
                if (!seen.contains(edge.to())) {
                    stack.push(edge.to());
                }
            }
        }
        return visited;
    }

    /** Cheapest total distance from start to every node, plus the predecessors. */
    static Map<String, Integer> dijkstra(Map<String, List<Edge>> graph, String start,
                                         Map<String, String> previous) {
        Map<String, Integer> distances = new HashMap<>();
        for (String node : graph.keySet()) {
            distances.put(node, Integer.MAX_VALUE);
        }
        distances.put(start, 0);
        Set<String> settled = new HashSet<>();
        PriorityQueue<Edge> heap = new PriorityQueue<>(Comparator.comparingInt(Edge::weight));
        heap.add(new Edge(start, 0));

        while (!heap.isEmpty()) {
            Edge cheapest = heap.poll();
            String node = cheapest.to();
            if (!settled.add(node)) {
                continue;
            }
            for (Edge edge : graph.getOrDefault(node, List.of())) {
                int candidate = distances.get(node) + edge.weight();
                if (candidate < distances.getOrDefault(edge.to(), Integer.MAX_VALUE)) {
                    distances.put(edge.to(), candidate);
                    previous.put(edge.to(), node);
                    heap.add(new Edge(edge.to(), candidate));
                }
            }
        }
        return distances;
    }

    static List<String> pathTo(Map<String, String> previous, String goal) {
        List<String> path = new ArrayList<>();
        String node = goal;
        while (node != null) {
            path.add(0, node);
            node = previous.get(node);
        }
        return path;
    }

    public static void main(String[] args) {
        Map<String, List<Edge>> graph = buildGraph();

        List<String> hops = bfs(graph, "A", "F");
        check(!hops.isEmpty() && hops.get(0).equals("A") && hops.get(hops.size() - 1).equals("F"),
                "BFS reaches F");
        check(hops.size() - 1 <= 3, "BFS finds a three-hop route");
        System.out.println("BFS  A -> F : " + String.join(" -> ", hops) + " (" + (hops.size() - 1) + " hops)");

        List<String> order = dfs(graph, "A");
        check(new HashSet<>(order).equals(graph.keySet()), "DFS visits every node");
        System.out.println("DFS  from A : " + order);

        Map<String, String> previous = new HashMap<>();
        Map<String, Integer> distances = dijkstra(graph, "A", previous);
        List<String> cheapest = pathTo(previous, "F");
        check(String.join(" -> ", cheapest).equals("A -> C -> E -> F"), "cheapest route is A-C-E-F");
        check(distances.get("F") == 14, "cost to F is 14");
        check(distances.get("C") == 2 && distances.get("B") == 4, "costs to C and B");
        check(distances.get("D") == 9, "the detour through E beats the direct B-D edge");
        System.out.println("Dijkstra A->F: " + String.join(" -> ", cheapest)
                + " (cost " + distances.get("F") + ")");
        System.out.println("distances    : " + new TreeMap<>(distances));
        System.out.println("All checks passed.");
    }
}
