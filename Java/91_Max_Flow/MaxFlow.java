import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

/**
 * 91 - Maximum flow and minimum cut, by Edmonds-Karp: repeatedly find the
 * shortest augmenting path with a breadth-first search over the residual graph.
 *
 * The theorem at the end of the run is the interesting part: the capacity of the
 * cut you can see in the residual graph equals the flow you just pushed.
 *
 * Compile and run:
 *   javac MaxFlow.java
 *   java MaxFlow
 */
public class MaxFlow {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static final class FlowNetwork {
        private final int size;
        private final int[][] capacity;
        private final int[][] flow;

        FlowNetwork(int size) {
            this.size = size;
            this.capacity = new int[size][size];
            this.flow = new int[size][size];
        }

        /** Parallel edges simply add, which is what a capacity should do. */
        void addEdge(int from, int to, int edgeCapacity) {
            if (edgeCapacity < 0) {
                throw new IllegalArgumentException("capacity must not be negative");
            }
            capacity[from][to] += edgeCapacity;
        }

        private int residual(int from, int to) {
            return capacity[from][to] - flow[from][to];
        }

        int maxFlow(int source, int sink) {
            // Start from nothing, so the result is the max flow of the network
            // rather than however much a previous call happened to add.
            for (int[] row : flow) {
                Arrays.fill(row, 0);
            }
            int total = 0;
            while (true) {
                int[] parent = new int[size];
                Arrays.fill(parent, -1);
                parent[source] = source;

                Deque<Integer> queue = new ArrayDeque<>();
                queue.addLast(source);
                while (!queue.isEmpty() && parent[sink] == -1) {
                    int node = queue.pollFirst();
                    for (int next = 0; next < size; next++) {
                        if (parent[next] == -1 && residual(node, next) > 0) {
                            parent[next] = node;
                            queue.addLast(next);
                        }
                    }
                }
                if (parent[sink] == -1) {
                    break;                      // no augmenting path: the flow is maximal
                }

                int bottleneck = Integer.MAX_VALUE;
                for (int node = sink; node != source; node = parent[node]) {
                    bottleneck = Math.min(bottleneck, residual(parent[node], node));
                }
                for (int node = sink; node != source; node = parent[node]) {
                    flow[parent[node]][node] += bottleneck;
                    flow[node][parent[node]] -= bottleneck;   // the residual back edge
                }
                total += bottleneck;
            }
            return total;
        }

        /** Nodes still reachable from the source once no augmenting path remains. */
        boolean[] reachableFrom(int source) {
            boolean[] seen = new boolean[size];
            Deque<Integer> stack = new ArrayDeque<>();
            seen[source] = true;
            stack.push(source);
            while (!stack.isEmpty()) {
                int node = stack.pop();
                for (int next = 0; next < size; next++) {
                    if (!seen[next] && residual(node, next) > 0) {
                        seen[next] = true;
                        stack.push(next);
                    }
                }
            }
            return seen;
        }

        /** Total capacity of the edges crossing from the source side to the other. */
        int cutCapacity(boolean[] sourceSide) {
            int total = 0;
            for (int from = 0; from < size; from++) {
                for (int to = 0; to < size; to++) {
                    if (sourceSide[from] && !sourceSide[to]) {
                        total += capacity[from][to];
                    }
                }
            }
            return total;
        }

        int flowOn(int from, int to) {
            return flow[from][to];
        }

        int capacityOf(int from, int to) {
            return capacity[from][to];
        }

        int size() {
            return size;
        }

        /** Nothing is created or destroyed except at the source and the sink. */
        boolean conservesFlow(int source, int sink) {
            for (int node = 0; node < size; node++) {
                if (node == source || node == sink) {
                    continue;
                }
                int incoming = 0;
                int outgoing = 0;
                for (int other = 0; other < size; other++) {
                    incoming += flow[other][node];
                    outgoing += flow[node][other];
                }
                if (incoming != outgoing) {
                    return false;
                }
            }
            return true;
        }

        /**
         * The two feasibility rules. Only the upper bound applies: a residual
         * back edge carries negative flow on purpose. Skew symmetry is what makes
         * that consistent.
         */
        boolean respectsCapacity() {
            for (int from = 0; from < size; from++) {
                for (int to = 0; to < size; to++) {
                    if (flow[from][to] > capacity[from][to]) {
                        return false;
                    }
                    if (flow[from][to] != -flow[to][from]) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    public static void main(String[] args) {
        // ---- the textbook network --------------------------------------------
        // 0 = source, 1..4 = intermediate, 5 = sink
        FlowNetwork network = new FlowNetwork(6);
        network.addEdge(0, 1, 16);
        network.addEdge(0, 2, 13);
        network.addEdge(1, 2, 10);
        network.addEdge(2, 1, 4);
        network.addEdge(1, 3, 12);
        network.addEdge(3, 2, 9);
        network.addEdge(2, 4, 14);
        network.addEdge(4, 3, 7);
        network.addEdge(3, 5, 20);
        network.addEdge(4, 5, 4);

        int flow = network.maxFlow(0, 5);
        check(flow == 23, "the classic network carries 23 units");
        check(network.conservesFlow(0, 5), "flow in equals flow out at every intermediate node");
        check(network.respectsCapacity(), "no edge carries more than its capacity");
        System.out.println("max flow     : " + flow);

        // the cut the residual graph reveals
        boolean[] sourceSide = network.reachableFrom(0);
        check(!sourceSide[5], "the sink is not on the source side, by definition");
        int cut = network.cutCapacity(sourceSide);
        check(cut == flow, "the minimum cut equals the maximum flow");
        check(sourceSide[0] && sourceSide[1] && sourceSide[2] && sourceSide[4],
                "the source side is the source, v1, v2 and v4");
        check(!sourceSide[3], "and v3 is on the sink side");
        System.out.println("min cut      : " + cut + " across "
                + Arrays.toString(sourceSide));

        // the three edges that make up that cut
        check(network.capacityOf(1, 3) + network.capacityOf(4, 3) + network.capacityOf(4, 5) == cut,
                "the cut is exactly v1->v3, v4->v3 and v4->t");
        System.out.println("cut edges    : v1->v3 " + network.capacityOf(1, 3)
                + ", v4->v3 " + network.capacityOf(4, 3)
                + ", v4->t " + network.capacityOf(4, 5));

        // ---- a small network, worked by hand ---------------------------------
        FlowNetwork small = new FlowNetwork(3);          // 0 = s, 1 = a, 2 = t
        small.addEdge(0, 1, 3);
        small.addEdge(1, 2, 2);
        small.addEdge(0, 2, 1);
        check(small.maxFlow(0, 2) == 3, "one unit direct plus two through a");
        check(small.conservesFlow(0, 2), "and the books balance");

        // ---- a single bottleneck ---------------------------------------------
        FlowNetwork bottleneck = new FlowNetwork(3);
        bottleneck.addEdge(0, 1, 5);
        bottleneck.addEdge(1, 2, 5);
        check(bottleneck.maxFlow(0, 2) == 5, "the pipe is only as wide as its narrowest part");

        FlowNetwork narrow = new FlowNetwork(3);
        narrow.addEdge(0, 1, 9);
        narrow.addEdge(1, 2, 4);
        check(narrow.maxFlow(0, 2) == 4, "even when one end is wider");

        // ---- parallel edges add ------------------------------------------------
        FlowNetwork parallel = new FlowNetwork(3);
        parallel.addEdge(0, 1, 2);
        parallel.addEdge(0, 1, 3);
        parallel.addEdge(1, 2, 10);
        check(parallel.maxFlow(0, 2) == 5, "two edges from s to a carry five together");

        // ---- disconnected, and a second run on the same network --------------
        FlowNetwork island = new FlowNetwork(4);
        island.addEdge(0, 1, 5);                          // 3 is unreachable
        check(island.maxFlow(0, 3) == 0, "an unreachable sink carries nothing");
        check(island.reachableFrom(0)[1], "but the reachable part is still reported");

        // maxFlow recomputes from scratch, so calling it again gives the same answer
        FlowNetwork reused = new FlowNetwork(3);
        reused.addEdge(0, 1, 4);
        reused.addEdge(1, 2, 4);
        int firstRun = reused.maxFlow(0, 2);
        int secondRun = reused.maxFlow(0, 2);
        check(firstRun == 4 && secondRun == 4, "running it twice gives the same answer, not twice the flow");
        check(reused.flowOn(0, 1) == 4, "and the recorded flow is unchanged");
        System.out.println("idempotent   : two runs both report " + firstRun);

        // ---- a bipartite matching shape, decided by the cut -------------------
        // three workers, three jobs, each edge a possible assignment
        FlowNetwork matching = new FlowNetwork(8);        // 0 source, 1-3 workers, 4-6 jobs, 7 sink
        for (int worker = 1; worker <= 3; worker++) {
            matching.addEdge(0, worker, 1);
        }
        for (int job = 4; job <= 6; job++) {
            matching.addEdge(job, 7, 1);
        }
        matching.addEdge(1, 4, 1);   // worker 1 can do job 1
        matching.addEdge(1, 5, 1);   // and job 2
        matching.addEdge(2, 4, 1);   // worker 2 can do job 1
        matching.addEdge(3, 6, 1);   // worker 3 can do job 3
        int assignments = matching.maxFlow(0, 7);
        check(assignments == 3, "all three workers can be placed");
        check(matching.cutCapacity(matching.reachableFrom(0)) == assignments,
                "the cut matches the matching size");
        System.out.println("matching     : " + assignments + " workers placed");

        FlowNetwork tight = new FlowNetwork(8);
        for (int worker = 1; worker <= 3; worker++) {
            tight.addEdge(0, worker, 1);
        }
        for (int job = 4; job <= 6; job++) {
            tight.addEdge(job, 7, 1);
        }
        tight.addEdge(1, 4, 1);      // everyone can only do job 1
        tight.addEdge(2, 4, 1);
        tight.addEdge(3, 4, 1);
        check(tight.maxFlow(0, 7) == 1, "only one worker can be placed when the jobs overlap");
        System.out.println("tight        : " + 1 + " worker placed, as the cut predicts");
        System.out.println("All checks passed.");
    }
}
