"""
Program 60: Kruskal's Minimum Spanning Tree

Sorts the edges by weight and adds each one that joins two different components,
which a union-find structure tracks in near-constant time. The result is a
spanning tree of minimum total weight, or an error when the graph is not
connected. Prim's algorithm is implemented separately as a cross-check.

Concepts: graphs, greedy algorithms, union-find, path compression, heaps.
"""

from __future__ import annotations

import heapq
import random
from collections.abc import Iterable

Edge = tuple[int, str, str]


class UnionFind:
    def __init__(self, nodes: Iterable[str]) -> None:
        self._parent = {node: node for node in nodes}
        self._rank = {node: 0 for node in self._parent}
        self._count = len(self._parent)

    def __len__(self) -> int:
        return self._count

    def __contains__(self, node: str) -> bool:
        return node in self._parent

    def find(self, node: str) -> str:
        root = node
        while self._parent[root] != root:
            root = self._parent[root]

        while self._parent[node] != root:
            self._parent[node], node = root, self._parent[node]

        return root

    def union(self, first: str, second: str) -> bool:
        root_a = self.find(first)
        root_b = self.find(second)
        if root_a == root_b:
            return False

        if self._rank[root_a] < self._rank[root_b]:
            root_a, root_b = root_b, root_a
        self._parent[root_b] = root_a
        if self._rank[root_a] == self._rank[root_b]:
            self._rank[root_a] += 1
        self._count -= 1
        return True

    def connected(self, first: str, second: str) -> bool:
        return self.find(first) == self.find(second)


def kruskal(nodes: Iterable[str], edges: Iterable[Edge]) -> tuple[int, list[Edge]]:
    node_set = set(nodes)
    for _, first, second in edges:
        node_set.add(first)
        node_set.add(second)

    if not node_set:
        return 0, []

    union_find = UnionFind(node_set)
    chosen: list[Edge] = []
    total = 0

    for weight, first, second in sorted(edges, key=lambda edge: (edge[0], edge[1], edge[2])):
        if union_find.union(first, second):
            chosen.append((weight, first, second))
            total += weight
            if len(chosen) == len(node_set) - 1:
                break

    if len(chosen) != len(node_set) - 1:
        raise ValueError("graph is disconnected; no spanning tree exists")

    return total, chosen


def prim(graph: dict[str, list[tuple[str, int]]]) -> tuple[int, list[Edge]]:
    if not graph:
        return 0, []

    start = min(graph)
    visited = {start}
    heap = [(weight, start, neighbour) for neighbour, weight in graph[start]]
    heapq.heapify(heap)

    total = 0
    chosen: list[Edge] = []

    while heap and len(visited) < len(graph):
        weight, first, second = heapq.heappop(heap)
        if second in visited:
            continue
        visited.add(second)
        total += weight
        chosen.append((weight, first, second))
        for neighbour, neighbour_weight in graph.get(second, []):
            if neighbour not in visited:
                heapq.heappush(heap, (neighbour_weight, second, neighbour))

    if len(visited) != len(graph):
        raise ValueError("graph is disconnected; no spanning tree exists")

    return total, chosen


def to_adjacency(edges: Iterable[Edge]) -> dict[str, list[tuple[str, int]]]:
    graph: dict[str, list[tuple[str, int]]] = {}
    for weight, first, second in edges:
        graph.setdefault(first, []).append((second, weight))
        graph.setdefault(second, []).append((first, weight))
    return graph


def main() -> None:
    print("--- union-find ---")
    union_find = UnionFind(["a", "b", "c", "d", "e"])
    assert len(union_find) == 5
    assert union_find.union("a", "b")
    assert union_find.union("c", "d")
    assert not union_find.union("a", "b")
    assert union_find.connected("a", "b")
    assert not union_find.connected("a", "c")
    assert union_find.union("b", "c")
    assert union_find.connected("a", "e") is False
    assert len(union_find) == 2
    assert union_find.union("d", "e")
    assert len(union_find) == 1
    assert all(union_find.find(node) == union_find.find("a") for node in "abcde")
    print("unions:", len(union_find), "component, all five connected")

    print("\n--- a known graph ---")
    nodes = ["A", "B", "C", "D", "E", "F"]
    edges: list[Edge] = [
        (4, "A", "B"),
        (2, "A", "F"),
        (6, "B", "C"),
        (5, "B", "F"),
        (3, "C", "D"),
        (1, "C", "F"),
        (2, "D", "E"),
        (4, "E", "F"),
    ]

    total, chosen = kruskal(nodes, edges)
    print(f"  total weight {total}, edges {chosen}")
    assert total == 12
    assert len(chosen) == len(nodes) - 1
    assert total == sum(weight for weight, _, _ in chosen)

    prim_total, prim_edges = prim(to_adjacency(edges))
    print(f"  prim: {prim_total}, edges {prim_edges}")
    assert prim_total == total
    assert len(prim_edges) == len(chosen)

    print("\n--- empty and single-node graphs ---")
    assert kruskal([], []) == (0, [])
    assert prim({}) == (0, [])
    assert kruskal(["solo"], []) == (0, [])
    assert prim({"solo": []}) == (0, [])
    print("  empty and single-node graphs have weight 0")

    print("\n--- disconnected graphs ---")
    try:
        kruskal(["a", "b", "c"], [(1, "a", "b")])
        raise AssertionError("a disconnected graph must fail")
    except ValueError as error:
        print("  kruskal:", error)
    try:
        prim({"a": [("b", 1)], "b": [("a", 1)], "c": []})
        raise AssertionError("a disconnected graph must fail")
    except ValueError as error:
        print("  prim:   ", error)

    print("\n--- negative and parallel edges ---")
    negative_total, negative_edges = kruskal(
        ["a", "b", "c"],
        [(-5, "a", "b"), (-2, "b", "c"), (4, "a", "c")],
    )
    print(f"  negative weights: {negative_total}, {negative_edges}")
    assert negative_total == -7
    assert len(negative_edges) == 2

    parallel_total, parallel_edges = kruskal(
        ["a", "b"],
        [(5, "a", "b"), (2, "a", "b"), (9, "a", "b")],
    )
    assert parallel_total == 2
    assert parallel_edges == [(2, "a", "b")]
    print(f"  parallel edges: picks the cheapest, total {parallel_total}")

    loops_total, loops_edges = kruskal(["a", "b"], [(1, "a", "a"), (3, "a", "b")])
    assert loops_total == 3
    assert loops_edges == [(3, "a", "b")]
    print("  self loops are ignored")

    print("\n--- against prim on random connected graphs ---")
    rng = random.Random(60)
    checked = 0

    for _ in range(150):
        size = rng.randint(2, 12)
        names = [f"n{index}" for index in range(size)]
        random_edges: list[Edge] = []

        # A random tree keeps the graph connected.
        for index in range(1, size):
            parent = rng.randrange(index)
            random_edges.append((rng.randint(-5, 20), names[parent], names[index]))

        for _ in range(rng.randint(0, 15)):
            first = rng.randrange(size)
            second = rng.randrange(size)
            if first != second:
                random_edges.append((rng.randint(-5, 20), names[first], names[second]))

        total_k, edges_k = kruskal(names, random_edges)
        total_p, edges_p = prim(to_adjacency(random_edges))

        assert total_k == total_p, f"mismatch: {total_k} != {total_p}"
        assert len(edges_k) == size - 1
        assert len(edges_p) == size - 1
        assert sum(weight for weight, _, _ in edges_k) == total_k

        # The chosen edges must form one component over every node.
        check_union = UnionFind(names)
        for _, first, second in edges_k:
            assert check_union.union(first, second)
        assert len(check_union) == 1

        checked += 1

    print(f"  {checked} random graphs: kruskal and prim agree on the total")

    print("\n--- minimum weight property ---")
    # Kruskal stops exactly when every node is in one component, so the chosen
    # edges form a spanning tree; the total matches Prim's independent answer.
    total_k, edges_k = kruskal(nodes, edges)
    final_union = UnionFind(nodes)
    for _, first, second in edges_k:
        final_union.union(first, second)
    assert len(final_union) == 1
    assert total_k == prim(to_adjacency(edges))[0]
    print("  the chosen edges connect every node at the same total as Prim")

    print("\n--- a larger graph ---")
    large_nodes = [f"v{index}" for index in range(500)]
    large_edges: list[Edge] = []
    for index in range(1, len(large_nodes)):
        large_edges.append((rng.randint(1, 1_000), large_nodes[rng.randrange(index)], large_nodes[index]))
    for _ in range(2_000):
        first = rng.randrange(len(large_nodes))
        second = rng.randrange(len(large_nodes))
        if first != second:
            large_edges.append((rng.randint(1, 1_000), large_nodes[first], large_nodes[second]))

    large_total, large_chosen = kruskal(large_nodes, large_edges)
    print(f"  500 nodes, {len(large_edges)} edges -> weight {large_total}, {len(large_chosen)} chosen")
    assert len(large_chosen) == 499
    assert large_total == prim(to_adjacency(large_edges))[0]

    print("\nAll checks passed.")


if __name__ == "__main__":
    main()
