"""
Program 52: Topological Sort (Course Schedule)

Orders tasks so every prerequisite comes before the task that needs it.
Kahn's algorithm gives a breadth-first order and detects cycles by leaving
nodes unplaced; a depth-first search is used to report the exact cycle.
A second pass groups the work into semesters by dependency depth.

Concepts: directed graphs, topological sorting, cycle detection, heapq.
"""

from __future__ import annotations

import heapq
import random
from collections import defaultdict


class CycleError(ValueError):
    """Raised when the prerequisites contain a cycle."""

    def __init__(self, cycle: list[str]) -> None:
        self.cycle = cycle
        shown = " -> ".join(cycle) if cycle else "unknown"
        super().__init__(f"cycle detected: {shown}")


def _adjacency(prerequisites: dict[str, set[str]]) -> tuple[set[str], dict[str, set[str]]]:
    """Nodes (including prerequisites that are not keys) and prerequisite -> dependents."""
    nodes: set[str] = set(prerequisites)
    dependents: dict[str, set[str]] = defaultdict(set)

    for course, required in prerequisites.items():
        for prerequisite in required:
            nodes.add(prerequisite)
            dependents[prerequisite].add(course)

    return nodes, dependents


def topological_order(prerequisites: dict[str, set[str]]) -> list[str]:
    """Kahn's algorithm, breaking ties alphabetically for a stable order."""
    nodes, dependents = _adjacency(prerequisites)
    indegree = {node: len(prerequisites.get(node, set())) for node in nodes}

    ready = [node for node, degree in indegree.items() if degree == 0]
    heapq.heapify(ready)

    order: list[str] = []
    while ready:
        node = heapq.heappop(ready)
        order.append(node)
        for dependent in sorted(dependents[node]):
            indegree[dependent] -= 1
            if indegree[dependent] == 0:
                heapq.heappush(ready, dependent)

    if len(order) != len(nodes):
        raise CycleError(find_cycle(prerequisites) or [])

    return order


def find_cycle(prerequisites: dict[str, set[str]]) -> list[str] | None:
    """Depth-first search that returns the first cycle it walks into."""
    nodes, _ = _adjacency(prerequisites)
    state: dict[str, int] = {}          # 0 unseen, 1 in progress, 2 done
    stack: list[str] = []
    position: dict[str, int] = {}

    def visit(node: str) -> list[str] | None:
        state[node] = 1
        position[node] = len(stack)
        stack.append(node)

        for neighbour in sorted(prerequisites.get(node, set())):
            if state.get(neighbour, 0) == 0:
                found = visit(neighbour)
                if found is not None:
                    return found
            elif state[neighbour] == 1:
                return stack[position[neighbour]:] + [neighbour]

        stack.pop()
        position.pop(node, None)
        state[node] = 2
        return None

    for node in sorted(nodes):
        if state.get(node, 0) == 0:
            found = visit(node)
            if found is not None:
                return found

    return None


def semester_plan(prerequisites: dict[str, set[str]]) -> list[list[str]]:
    """Layers of courses where every course only depends on earlier layers."""
    nodes, dependents = _adjacency(prerequisites)
    indegree = {node: len(prerequisites.get(node, set())) for node in nodes}

    ready = sorted(node for node, degree in indegree.items() if degree == 0)
    semesters: list[list[str]] = []
    placed = 0

    while ready:
        semesters.append(ready)
        next_ready: list[str] = []
        for node in ready:
            placed += 1
            for dependent in sorted(dependents[node]):
                indegree[dependent] -= 1
                if indegree[dependent] == 0:
                    next_ready.append(dependent)
        ready = sorted(next_ready)

    if placed != len(nodes):
        raise CycleError(find_cycle(prerequisites) or [])

    return semesters


def is_valid_order(prerequisites: dict[str, set[str]], order: list[str]) -> bool:
    """True when order contains every node once and no course precedes its prerequisites."""
    nodes, _ = _adjacency(prerequisites)
    if len(order) != len(set(order)) or set(order) != nodes:
        return False

    seen: set[str] = set()
    for course in order:
        if any(required not in seen for required in prerequisites.get(course, set())):
            return False
        seen.add(course)

    return True


def main() -> None:
    schedule: dict[str, set[str]] = {
        "intro": set(),
        "algorithms": {"intro"},
        "data-structures": {"intro"},
        "databases": {"intro"},
        "operating-systems": {"data-structures"},
        "networks": {"data-structures"},
        "compilers": {"algorithms", "operating-systems"},
        "distributed-systems": {"networks", "operating-systems"},
        "capstone": {"compilers", "distributed-systems", "databases"},
    }

    print("--- topological order ---")
    order = topological_order(schedule)
    print("  " + " -> ".join(order))
    assert is_valid_order(schedule, order)
    assert len(order) == len(schedule)
    assert order.index("intro") < order.index("algorithms") < order.index("compilers")

    print("\n--- semester plan ---")
    plan = semester_plan(schedule)
    flat = [course for semester in plan for course in semester]
    for number, semester in enumerate(plan, start=1):
        print(f"  semester {number}: {', '.join(semester)}")
    assert is_valid_order(schedule, flat)
    assert plan[0] == ["intro"]
    assert "capstone" in plan[-1]

    print("\n--- cycle detection ---")
    cyclic = {
        "a": {"c"},
        "b": {"a"},
        "c": {"b"},
    }
    try:
        topological_order(cyclic)
        raise AssertionError("a cycle should not produce an order")
    except CycleError as error:
        print(f"  {error}")
        print(f"  cycle: {error.cycle}")
        assert error.cycle[0] == error.cycle[-1], "the reported cycle should loop back"
        assert set(error.cycle[:-1]) == {"a", "b", "c"}

    self_loop = {"only": {"only"}}
    try:
        topological_order(self_loop)
        raise AssertionError("a self loop should be a cycle")
    except CycleError as error:
        print(f"  self loop: {error.cycle}")

    print("\n--- order is not unique ---")
    diamond = {"a": set(), "b": {"a"}, "c": {"a"}, "d": {"b", "c"}}
    other_order = ["a", "c", "b", "d"]
    print(f"  stable order: {topological_order(diamond)}")
    print(f"  another order: {other_order}")
    assert is_valid_order(diamond, other_order)
    assert is_valid_order(diamond, topological_order(diamond))
    assert not is_valid_order(diamond, ["a", "d", "b", "c"])
    assert not is_valid_order(diamond, ["a", "b", "c"])

    print("\n--- prerequisites that are not keys ---")
    implicit = {"course": {"math", "logic"}, "math": set()}
    implicit_order = topological_order(implicit)
    print(f"  {implicit_order}")
    assert is_valid_order(implicit, implicit_order)
    assert implicit_order.index("logic") < implicit_order.index("course")

    print("\n--- empty and single node graphs ---")
    print(f"  empty: {topological_order({})}, semesters: {semester_plan({})}")
    print(f"  single: {topological_order({'solo': set()})}")
    assert topological_order({}) == []
    assert semester_plan({}) == []
    assert topological_order({"solo": set()}) == ["solo"]

    print("\n--- randomised DAGs ---")
    rng = random.Random(11)
    checked = 0
    for _ in range(200):
        nodes = [f"c{index:02d}" for index in range(12)]
        graph: dict[str, set[str]] = {node: set() for node in nodes}
        for dependent in range(len(nodes)):
            for prerequisite in range(dependent):
                if rng.random() < 0.2:
                    graph[nodes[dependent]].add(nodes[prerequisite])

        dag_order = topological_order(graph)
        assert is_valid_order(graph, dag_order)
        assert len(dag_order) == len(nodes)
        assert find_cycle(graph) is None

        plan = semester_plan(graph)
        assert is_valid_order(graph, [course for semester in plan for course in semester])

        graph[nodes[-1]].add(nodes[0])   # close the loop...
        graph[nodes[0]].add(nodes[-1])   # ...into a guaranteed two-node cycle
        assert find_cycle(graph) is not None
        try:
            topological_order(graph)
            raise AssertionError("the closing edge should create a cycle")
        except CycleError:
            pass
        checked += 1

    print(f"  {checked} random graphs built, ordered, and forced into a cycle")
    print("\nAll checks passed.")


if __name__ == "__main__":
    main()
