interface Edge {
  to: string;
  weight: number;
}

type Graph = Map<string, Edge[]>;

interface Paths {
  distances: Map<string, number>;
  previous: Map<string, string | null>;
}

function addEdge(graph: Graph, from: string, to: string, weight: number, directed = false): void {
  if (!Number.isFinite(weight)) {
    throw new TypeError("edge weight must be finite");
  }
  if (!graph.has(from)) {
    graph.set(from, []);
  }
  graph.get(from)!.push({ to, weight });
  if (!graph.has(to)) {
    graph.set(to, []);
  }
  if (!directed) {
    graph.get(to)!.push({ to: from, weight });
  }
}

function nodesOf(graph: Graph): string[] {
  const nodes = new Set<string>(graph.keys());
  for (const edges of graph.values()) {
    for (const edge of edges) {
      nodes.add(edge.to);
    }
  }
  return [...nodes].sort();
}

class MinHeap {
  private items: Array<[number, string]> = [];

  get size(): number {
    return this.items.length;
  }

  push(item: [number, string]): void {
    this.items.push(item);
    let index = this.items.length - 1;
    while (index > 0) {
      const parent = (index - 1) >> 1;
      if (this.items[parent][0] <= this.items[index][0]) {
        break;
      }
      [this.items[parent], this.items[index]] = [this.items[index], this.items[parent]];
      index = parent;
    }
  }

  pop(): [number, string] | undefined {
    if (this.items.length === 0) {
      return undefined;
    }
    const top = this.items[0];
    const last = this.items.pop();
    if (this.items.length > 0 && last) {
      this.items[0] = last;
      let index = 0;
      while (true) {
        const left = index * 2 + 1;
        const right = left + 1;
        let smallest = index;
        if (left < this.items.length && this.items[left][0] < this.items[smallest][0]) {
          smallest = left;
        }
        if (right < this.items.length && this.items[right][0] < this.items[smallest][0]) {
          smallest = right;
        }
        if (smallest === index) {
          break;
        }
        [this.items[index], this.items[smallest]] = [this.items[smallest], this.items[index]];
        index = smallest;
      }
    }
    return top;
  }
}

function dijkstra(graph: Graph, source: string): Paths {
  const distances = new Map<string, number>();
  const previous = new Map<string, string | null>();

  for (const node of nodesOf(graph)) {
    distances.set(node, Number.POSITIVE_INFINITY);
    previous.set(node, null);
  }
  distances.set(source, 0);

  const heap = new MinHeap();
  heap.push([0, source]);

  while (heap.size > 0) {
    const [distance, node] = heap.pop()!;
    if (distance > (distances.get(node) ?? Number.POSITIVE_INFINITY)) {
      continue;
    }

    for (const edge of graph.get(node) ?? []) {
      if (edge.weight < 0) {
        throw new Error("dijkstra requires non-negative weights");
      }
      const candidate = distance + edge.weight;
      if (candidate < (distances.get(edge.to) ?? Number.POSITIVE_INFINITY)) {
        distances.set(edge.to, candidate);
        previous.set(edge.to, node);
        heap.push([candidate, edge.to]);
      }
    }
  }

  return { distances, previous };
}

/** Handles negative edges and reports negative cycles. */
function bellmanFord(graph: Graph, source: string): Paths {
  const nodes = nodesOf(graph);
  const distances = new Map<string, number>(nodes.map((node) => [node, Number.POSITIVE_INFINITY]));
  const previous = new Map<string, string | null>(nodes.map((node) => [node, null]));
  distances.set(source, 0);

  for (let round = 0; round < nodes.length - 1; round += 1) {
    let changed = false;
    for (const from of nodes) {
      const distance = distances.get(from)!;
      if (distance === Number.POSITIVE_INFINITY) {
        continue;
      }
      for (const edge of graph.get(from) ?? []) {
        const candidate = distance + edge.weight;
        if (candidate < (distances.get(edge.to) ?? Number.POSITIVE_INFINITY)) {
          distances.set(edge.to, candidate);
          previous.set(edge.to, from);
          changed = true;
        }
      }
    }
    if (!changed) {
      break;
    }
  }

  for (const from of nodes) {
    const distance = distances.get(from)!;
    if (distance === Number.POSITIVE_INFINITY) {
      continue;
    }
    for (const edge of graph.get(from) ?? []) {
      if (distance + edge.weight < (distances.get(edge.to) ?? Number.POSITIVE_INFINITY)) {
        throw new Error("graph contains a negative cycle");
      }
    }
  }

  return { distances, previous };
}

function shortestPath(paths: Paths, target: string): string[] | null {
  if ((paths.distances.get(target) ?? Number.POSITIVE_INFINITY) === Number.POSITIVE_INFINITY) {
    return null;
  }

  const path: string[] = [];
  let current: string | null = target;
  while (current !== null) {
    path.push(current);
    current = paths.previous.get(current) ?? null;
  }
  return path.reverse();
}

function check(condition: boolean, message: string): void {
  if (!condition) {
    throw new Error(message);
  }
}

function main(): void {
  console.log("--- dijkstra on a small graph ---");
  const graph: Graph = new Map();
  addEdge(graph, "A", "B", 4);
  addEdge(graph, "A", "C", 2);
  addEdge(graph, "B", "C", 5);
  addEdge(graph, "B", "D", 10);
  addEdge(graph, "C", "E", 3);
  addEdge(graph, "E", "D", 4);
  addEdge(graph, "D", "F", 11);

  const fromA = dijkstra(graph, "A");
  const summary = [...fromA.distances.entries()].map(([node, distance]) => `${node}=${distance}`).join(" ");
  console.log("distances:", summary);
  check(fromA.distances.get("A") === 0, "source distance is zero");
  check(fromA.distances.get("B") === 4, "A->B is 4");
  check(fromA.distances.get("C") === 2, "A->C is 2");
  check(fromA.distances.get("D") === 9, "A->C->E->D is 9");
  check(fromA.distances.get("E") === 5, "A->C->E is 5");
  check(fromA.distances.get("F") === 20, "A->C->E->D->F is 20");

  console.log("path to F:", shortestPath(fromA, "F")?.join(" -> "));
  check(shortestPath(fromA, "F")?.join(",") === "A,C,E,D,F", "path is reconstructed");
  check(shortestPath(fromA, "A")?.join(",") === "A", "path to itself");
  check(shortestPath(fromA, "Z") === null, "unknown target has no path");

  console.log("\n--- unreachable nodes ---");
  const island: Graph = new Map();
  addEdge(island, "A", "B", 1);
  island.set("C", []);
  const islandPaths = dijkstra(island, "A");
  check(islandPaths.distances.get("C") === Number.POSITIVE_INFINITY, "C is unreachable");
  check(shortestPath(islandPaths, "C") === null, "no path to C");
  console.log("C stays at infinity");

  console.log("\n--- zero weight edges ---");
  const zero: Graph = new Map();
  addEdge(zero, "A", "B", 0);
  addEdge(zero, "B", "C", 0);
  addEdge(zero, "A", "C", 5);
  check(dijkstra(zero, "A").distances.get("C") === 0, "zero-weight chain wins");
  console.log("A->B->C costs 0");

  console.log("\n--- bellman-ford with negative edges ---");
  const negative: Graph = new Map();
  addEdge(negative, "S", "A", 4, true);
  addEdge(negative, "S", "B", 5, true);
  addEdge(negative, "A", "B", -3, true);
  addEdge(negative, "B", "T", 2, true);

  const bellman = bellmanFord(negative, "S");
  check(bellman.distances.get("B") === 1, "S->A->B beats S->B");
  check(bellman.distances.get("T") === 3, "and T costs 3");
  check(shortestPath(bellman, "T")?.join(",") === "S,A,B,T", "path uses the negative edge");
  console.log("distances:", [...bellman.distances.entries()].map(([node, distance]) => `${node}=${distance}`).join(" "));

  console.log("\n--- negative cycle detection ---");
  const cycle: Graph = new Map();
  addEdge(cycle, "A", "B", 1, true);
  addEdge(cycle, "B", "C", -2, true);
  addEdge(cycle, "C", "A", -1, true);
  let cycleThrew = false;
  try {
    bellmanFord(cycle, "A");
  } catch (error) {
    cycleThrew = true;
    console.log("rejected:", (error as Error).message);
  }
  check(cycleThrew, "a negative cycle must be reported");

  console.log("\n--- dijkstra rejects negative weights ---");
  let negativeThrew = false;
  try {
    dijkstra(cycle, "A");
  } catch (error) {
    negativeThrew = true;
    console.log("rejected:", (error as Error).message);
  }
  check(negativeThrew, "dijkstra must refuse negative edges");

  console.log("\n--- randomised comparison ---");
  let state = 7901;
  const next = (): number => {
    state = (state * 1103515245 + 12345) % 2147483648;
    return state;
  };

  let compared = 0;
  for (let trial = 0; trial < 300; trial += 1) {
    const size = 2 + (next() % 8);
    const randomGraph: Graph = new Map();
    for (let index = 0; index < size; index += 1) {
      randomGraph.set(`n${index}`, []);
    }
    for (let from = 0; from < size; from += 1) {
      for (let to = 0; to < size; to += 1) {
        if (from !== to && next() % 4 === 0) {
          addEdge(randomGraph, `n${from}`, `n${to}`, next() % 20, true);
        }
      }
    }

    const source = `n${next() % size}`;
    const fast = dijkstra(randomGraph, source);
    const slow = bellmanFord(randomGraph, source);

    for (const node of nodesOf(randomGraph)) {
      check(fast.distances.get(node) === slow.distances.get(node), "distance mismatch");
    }

    for (const node of nodesOf(randomGraph)) {
      const distance = fast.distances.get(node)!;
      if (distance === Number.POSITIVE_INFINITY) {
        continue;
      }
      const path = shortestPath(fast, node);
      check(path !== null && path[0] === source && path[path.length - 1] === node, "path endpoints");

      let total = 0;
      for (let index = 1; index < path.length; index += 1) {
        const edge = (randomGraph.get(path[index - 1]) ?? []).find((candidate) => candidate.to === path[index]);
        check(edge !== undefined, "path uses a real edge");
        total += edge!.weight;
      }
      check(total === distance, `path cost ${total} != distance ${distance}`);
    }
    compared += 1;
  }
  console.log(`${compared} random graphs: dijkstra, bellman-ford, and path costs agree`);

  console.log("\n--- triangle inequality ---");
  const triangle = dijkstra(graph, "A");
  for (const [from, edges] of graph) {
    const fromDistance = triangle.distances.get(from)!;
    if (fromDistance === Number.POSITIVE_INFINITY) {
      continue;
    }
    for (const edge of edges) {
      const toDistance = triangle.distances.get(edge.to)!;
      check(toDistance <= fromDistance + edge.weight, "relaxation invariant");
    }
  }
  console.log("every edge satisfies d(to) <= d(from) + weight");

  console.log("\nAll checks passed.");
}

main();
