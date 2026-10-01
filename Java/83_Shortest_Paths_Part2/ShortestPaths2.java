import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 83 - Shortest paths part two: Bellman-Ford, which tolerates negative edges and
 * detects negative cycles, and Floyd-Warshall for every pair at once.
 *
 * Compile and run:
 *   javac ShortestPaths2.java
 *   java ShortestPaths2
 */
public class ShortestPaths2 {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /** A large finite value, so distances can be added without overflowing. */
    static final int INF = 1_000_000;

    record Edge(String from, String to, int weight) {
        @Override
        public String toString() {
            return from + "->" + to + "(" + weight + ")";
        }
    }

    record PathResult(Map<String, Integer> distances, Map<String, String> previous) {
    }

    static Map<String, Integer> index(List<String> nodes) {
        Map<String, Integer> positions = new LinkedHashMap<>();
        for (int i = 0; i < nodes.size(); i++) {
            positions.put(nodes.get(i), i);
        }
        return positions;
    }

    /**
     * Relax every edge |V|-1 times, then once more: if anything still improves, a
     * negative cycle is reachable and the distances are meaningless.
     */
    static PathResult bellmanFord(List<String> nodes, List<Edge> edges, String source) {
        Map<String, Integer> distance = new TreeMap<>();
        Map<String, String> previous = new TreeMap<>();
        for (String node : nodes) {
            distance.put(node, INF);
        }
        distance.put(source, 0);

        for (int round = 0; round < nodes.size() - 1; round++) {
            boolean improved = false;
            for (Edge edge : edges) {
                int from = distance.get(edge.from());
                if (from == INF) {
                    continue;                  // not reachable yet
                }
                int candidate = from + edge.weight();
                if (candidate < distance.get(edge.to())) {
                    distance.put(edge.to(), candidate);
                    previous.put(edge.to(), edge.from());
                    improved = true;
                }
            }
            if (!improved) {
                break;                         // settled early
            }
        }

        for (Edge edge : edges) {
            int from = distance.get(edge.from());
            if (from != INF && from + edge.weight() < distance.get(edge.to())) {
                throw new IllegalStateException(
                        "a negative cycle is reachable from " + source + " via " + edge);
            }
        }
        return new PathResult(distance, previous);
    }

    static List<String> reconstruct(Map<String, String> previous, String target) {
        LinkedList<String> path = new LinkedList<>();
        String node = target;
        while (node != null) {
            path.addFirst(node);
            node = previous.get(node);
        }
        return path;
    }

    /** All pairs at once: is a detour through `via` shorter than going direct? */
    static int[][] floydWarshall(List<String> nodes, List<Edge> edges) {
        int size = nodes.size();
        Map<String, Integer> positions = index(nodes);
        int[][] distance = new int[size][size];
        for (int row = 0; row < size; row++) {
            Arrays.fill(distance[row], INF);
            distance[row][row] = 0;
        }
        for (Edge edge : edges) {
            int from = positions.get(edge.from());
            int to = positions.get(edge.to());
            distance[from][to] = Math.min(distance[from][to], edge.weight());
        }
        for (int via = 0; via < size; via++) {
            for (int from = 0; from < size; from++) {
                if (distance[from][via] == INF) {
                    continue;
                }
                for (int to = 0; to < size; to++) {
                    if (distance[via][to] == INF) {
                        continue;
                    }
                    int candidate = distance[from][via] + distance[via][to];
                    if (candidate < distance[from][to]) {
                        distance[from][to] = candidate;
                    }
                }
            }
        }
        return distance;
    }

    static boolean hasNegativeCycle(int[][] distances) {
        for (int i = 0; i < distances.length; i++) {
            if (distances[i][i] < 0) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        // ---- the graph from the Dijkstra program, as directed edges ----------
        List<String> nodes = List.of("A", "B", "C", "D", "E", "F");
        List<Edge> edges = List.of(
                new Edge("A", "B", 4), new Edge("A", "C", 2), new Edge("B", "C", 5),
                new Edge("B", "D", 10), new Edge("C", "E", 3), new Edge("E", "D", 4),
                new Edge("D", "F", 11), new Edge("E", "F", 9));

        PathResult fromA = bellmanFord(nodes, edges, "A");
        Map<String, Integer> expected = Map.of("A", 0, "B", 4, "C", 2, "D", 9, "E", 5, "F", 14);
        check(fromA.distances().equals(new TreeMap<>(expected)),
                "Bellman-Ford finds the same distances Dijkstra did");
        check(reconstruct(fromA.previous(), "F").equals(List.of("A", "C", "E", "F")),
                "and rebuilds the same route");
        System.out.println("bellman-ford: " + fromA.distances());

        // ---- all pairs --------------------------------------------------------
        Map<String, Integer> positions = index(nodes);
        int[][] allPairs = floydWarshall(nodes, edges);
        check(allPairs[positions.get("A")][positions.get("F")] == 14, "Floyd-Warshall agrees on A to F");
        check(allPairs[positions.get("A")][positions.get("A")] == 0, "a node is zero away from itself");
        check(!hasNegativeCycle(allPairs), "no negative cycle here");
        check(allPairs[positions.get("F")][positions.get("A")] == INF,
                "F cannot reach A: the edges only go one way");

        // every pair must agree with a Bellman-Ford run from that source
        for (String from : nodes) {
            PathResult single = bellmanFord(nodes, edges, from);
            for (String to : nodes) {
                check(single.distances().get(to) == allPairs[positions.get(from)][positions.get(to)],
                        "the two algorithms agree on " + from + " to " + to);
            }
        }
        System.out.println("all pairs   : both algorithms agree on all 36 pairs");

        // ---- negative edges, where Dijkstra would be wrong -------------------
        List<String> negNodes = List.of("A", "B", "C");
        List<Edge> negEdges = List.of(
                new Edge("A", "B", 4), new Edge("A", "C", 5), new Edge("B", "C", -3));

        PathResult negResult = bellmanFord(negNodes, negEdges, "A");
        check(negResult.distances().get("B") == 4, "B is four away");
        check(negResult.distances().get("C") == 1,
                "C is one away: 4 + (-3) beats the direct edge of 5");
        check(reconstruct(negResult.previous(), "C").equals(List.of("A", "B", "C")),
                "and the route goes through B");
        System.out.println("negative    : A to C is " + negResult.distances().get("C")
                + " via " + reconstruct(negResult.previous(), "C"));
        // Dijkstra would have settled A->C at 5 and never looked back, because it
        // assumes a later detour cannot be cheaper.

        int[][] negPairs = floydWarshall(negNodes, negEdges);
        check(negPairs[0][2] == 1, "Floyd-Warshall handles the negative edge too");
        check(!hasNegativeCycle(negPairs), "a single negative edge is not a cycle");

        // ---- negative cycles -------------------------------------------------
        List<Edge> cycleEdges = List.of(
                new Edge("A", "B", 1), new Edge("B", "C", -2), new Edge("C", "A", -1));

        try {
            bellmanFord(List.of("A", "B", "C"), cycleEdges, "A");
            throw new AssertionError("a negative cycle should have been detected");
        } catch (IllegalStateException negativeCycle) {
            System.out.println("negative cycle: " + negativeCycle.getMessage());
        }

        int[][] cyclic = floydWarshall(List.of("A", "B", "C"), cycleEdges);
        check(hasNegativeCycle(cyclic), "and Floyd-Warshall sees it on the diagonal");
        check(cyclic[0][0] < 0, "A can reach itself for less than nothing: " + cyclic[0][0]);

        // a zero delta cycle is fine: the total is not negative
        List<Edge> zeroCycle = List.of(
                new Edge("A", "B", 2), new Edge("B", "C", -2), new Edge("C", "A", 0));
        check(!hasNegativeCycle(floydWarshall(List.of("A", "B", "C"), zeroCycle)),
                "a cycle summing to zero is not negative");
        check(bellmanFord(List.of("A", "B", "C"), zeroCycle, "A").distances().get("C") == 0,
                "and its distances are still usable");
        System.out.println("zero cycle  : sums to zero, so it is allowed");

        // a self loop with a negative weight is a negative cycle of one
        List<Edge> selfLoop = List.of(new Edge("A", "A", -1));
        check(hasNegativeCycle(floydWarshall(List.of("A"), selfLoop)),
                "a negative self loop is caught too");

        // ---- unreachable nodes ------------------------------------------------
        List<String> withIsland = List.of("A", "B", "Z");
        PathResult island = bellmanFord(withIsland, List.of(new Edge("A", "B", 1)), "A");
        check(island.distances().get("B") == 1, "B is reachable");
        check(island.distances().get("Z") == INF, "Z stays at infinity");
        check(reconstruct(island.previous(), "Z").equals(List.of("Z")),
                "an unreachable node has no predecessors to walk back through");

        int[][] islandPairs = floydWarshall(withIsland, List.of(new Edge("A", "B", 1)));
        check(islandPairs[0][1] == 1, "Floyd-Warshall agrees on the reachable pair");
        check(islandPairs[0][2] == INF, "and leaves the island alone");
        System.out.println("unreachable : stays at infinity in both algorithms");
        System.out.println("All checks passed.");
    }
}
