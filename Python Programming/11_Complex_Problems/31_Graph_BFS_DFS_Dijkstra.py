"""
Program 31: Graph Traversal - BFS, DFS and Dijkstra

Builds a small weighted, undirected graph and explores it three ways:
breadth-first search for the fewest hops, depth-first search to walk the whole
component, and Dijkstra's algorithm for the cheapest weighted route.

Concepts: dicts, sets, deque, heapq, graphs, shortest paths.
"""

from collections import deque
import heapq

Graph = dict[str, list[tuple[str, int]]]

EDGES = [
    ("A", "B", 4), ("A", "C", 2),
    ("B", "C", 5), ("B", "D", 10),
    ("C", "E", 3), ("E", "D", 4),
    ("D", "F", 11), ("E", "F", 9),
]


def build_graph(edges: list[tuple[str, str, int]] = EDGES) -> Graph:
    """Build an undirected adjacency list from (left, right, weight) edges."""
    graph: Graph = {}
    for left, right, weight in edges:
        graph.setdefault(left, []).append((right, weight))
        graph.setdefault(right, []).append((left, weight))
    return graph


def bfs(graph: Graph, start: str, goal: str) -> list[str] | None:
    """Fewest-hops path from start to goal, ignoring weights."""
    queue = deque([[start]])
    seen = {start}
    while queue:
        path = queue.popleft()
        node = path[-1]
        if node == goal:
            return path
        for neighbour, _ in graph[node]:
            if neighbour not in seen:
                seen.add(neighbour)
                queue.append(path + [neighbour])
    return None


def dfs(graph: Graph, start: str) -> list[str]:
    """Every node reachable from start, in depth-first order."""
    visited: list[str] = []
    seen: set[str] = set()
    stack = [start]
    while stack:
        node = stack.pop()
        if node in seen:
            continue
        seen.add(node)
        visited.append(node)
        # Push in reverse so neighbours come off the stack alphabetically.
        for neighbour, _ in sorted(graph[node], reverse=True):
            if neighbour not in seen:
                stack.append(neighbour)
    return visited


def dijkstra(graph: Graph, start: str) -> tuple[dict[str, float], dict[str, str | None]]:
    """Cheapest distance to every node, plus each node's predecessor."""
    distances = {node: float("inf") for node in graph}
    distances[start] = 0.0
    previous: dict[str, str | None] = {node: None for node in graph}
    settled: set[str] = set()
    heap: list[tuple[float, str]] = [(0.0, start)]

    while heap:
        cost, node = heapq.heappop(heap)
        if node in settled:
            continue
        settled.add(node)
        for neighbour, weight in graph[node]:
            candidate = cost + weight
            if candidate < distances[neighbour]:
                distances[neighbour] = candidate
                previous[neighbour] = node
                heapq.heappush(heap, (candidate, neighbour))
    return distances, previous


def path_to(previous: dict[str, str | None], goal: str) -> list[str]:
    """Rebuild a path by walking predecessors backwards from the goal."""
    path: list[str] = []
    node: str | None = goal
    while node is not None:
        path.append(node)
        node = previous[node]
    return path[::-1]


def main() -> None:
    graph = build_graph()

    hops = bfs(graph, "A", "F")
    print(f"BFS  A -> F (fewest hops): {' -> '.join(hops or [])}  ({len(hops or []) - 1} hops)")

    order = dfs(graph, "A")
    print(f"DFS  from A visits: {order}")
    assert set(order) == set(graph), "DFS should reach the whole component"

    distances, previous = dijkstra(graph, "A")
    cheapest = path_to(previous, "F")
    print(f"Dijkstra A -> F (cheapest): {' -> '.join(cheapest)}  (cost {distances['F']:.0f})")

    print("\nCheapest distance from A to every node:")
    for node in sorted(distances):
        print(f"  {node}: {distances[node]:.0f}")

    assert distances["C"] == 2
    assert distances["F"] == 14
    assert previous["F"] == "E"
    assert hops is not None and hops[0] == "A" and hops[-1] == "F"


if __name__ == "__main__":
    main()
