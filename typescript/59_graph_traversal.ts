type Graph = Map<string, string[]>;

function buildGraph(edges: [string, string][]): Graph {
  const graph: Graph = new Map();
  const addEdge = (from: string, to: string): void => {
    const list = graph.get(from) ?? [];
    list.push(to);
    graph.set(from, list);
  };
  for (const [from, to] of edges) {
    addEdge(from, to);
    addEdge(to, from);
  }
  return graph;
}

function bfs(graph: Graph, start: string, goal: string): string[] | null {
  const queue: string[] = [start];
  const visited = new Set<string>([start]);
  const previous = new Map<string, string>();

  while (queue.length > 0) {
    const node = queue.shift()!;
    if (node === goal) {
      const path: string[] = [];
      let current: string | undefined = goal;
      while (current) {
        path.unshift(current);
        current = previous.get(current);
      }
      return path;
    }
    for (const neighbour of graph.get(node) ?? []) {
      if (!visited.has(neighbour)) {
        visited.add(neighbour);
        previous.set(neighbour, node);
        queue.push(neighbour);
      }
    }
  }
  return null;
}

function dfs(graph: Graph, start: string): string[] {
  const visited = new Set<string>();
  const order: string[] = [];
  const visit = (node: string): void => {
    if (visited.has(node)) {
      return;
    }
    visited.add(node);
    order.push(node);
    for (const neighbour of graph.get(node) ?? []) {
      visit(neighbour);
    }
  };
  visit(start);
  return order;
}

const graph = buildGraph([
  ["A", "B"],
  ["A", "C"],
  ["B", "D"],
  ["C", "D"],
  ["D", "E"],
]);

console.log("nodes:", [...graph.keys()].join(", "));
console.log("bfs A->E:", bfs(graph, "A", "E")?.join(" -> "));
console.log("bfs A->Z:", bfs(graph, "A", "Z"));
console.log("dfs from A:", dfs(graph, "A").join(" -> "));
console.log("neighbours of D:", graph.get("D")?.join(", "));
