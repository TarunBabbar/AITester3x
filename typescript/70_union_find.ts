type Edge = readonly [from: number, to: number, weight: number];

class UnionFind {
  private parent: number[];
  private size: number[];
  private count: number;

  constructor(n: number) {
    if (!Number.isInteger(n) || n < 0) {
      throw new RangeError(`a set needs a whole, non-negative size, got ${n}`);
    }
    this.parent = Array.from({ length: n }, (_, i) => i);
    this.size = new Array(n).fill(1);
    this.count = n;
  }

  find(x: number): number {
    if (x < 0 || x >= this.parent.length) {
      throw new RangeError(`node ${x} is out of range`);
    }
    let root = x;
    while (this.parent[root] !== root) {
      root = this.parent[root];
    }
    while (this.parent[x] !== root) {
      const next = this.parent[x];
      this.parent[x] = root;
      x = next;
    }
    return root;
  }

  union(a: number, b: number): boolean {
    let rootA = this.find(a);
    let rootB = this.find(b);
    if (rootA === rootB) return false;
    if (this.size[rootA] < this.size[rootB]) {
      [rootA, rootB] = [rootB, rootA];
    }
    this.parent[rootB] = rootA;
    this.size[rootA] += this.size[rootB];
    this.count -= 1;
    return true;
  }

  connected(a: number, b: number): boolean {
    return this.find(a) === this.find(b);
  }

  get components(): number {
    return this.count;
  }

  setSize(x: number): number {
    return this.size[this.find(x)];
  }

  groups(): number[][] {
    const byRoot = new Map<number, number[]>();
    for (let node = 0; node < this.parent.length; node++) {
      const root = this.find(node);
      const bucket = byRoot.get(root);
      if (bucket) bucket.push(node);
      else byRoot.set(root, [node]);
    }
    return [...byRoot.values()].sort((a, b) => a[0] - b[0]);
  }
}

function countComponents(nodes: number, edges: Edge[]): number {
  const uf = new UnionFind(nodes);
  for (const [from, to] of edges) {
    uf.union(from, to);
  }
  return uf.components;
}

function hasCycle(nodes: number, edges: Edge[]): boolean {
  const uf = new UnionFind(nodes);
  for (const [from, to] of edges) {
    if (!uf.union(from, to)) return true;
  }
  return false;
}

function kruskal(nodes: number, edges: Edge[]): { weight: number; chosen: Edge[] } {
  const uf = new UnionFind(nodes);
  const chosen: Edge[] = [];
  let weight = 0;
  for (const edge of [...edges].sort((a, b) => a[2] - b[2])) {
    if (uf.union(edge[0], edge[1])) {
      chosen.push(edge);
      weight += edge[2];
      if (chosen.length === nodes - 1) break;
    }
  }
  return { weight, chosen };
}

function describe(edges: Edge[]): string {
  return edges.map(([from, to, weight]) => `${from}-${to}(${weight})`).join(", ");
}

const graphEdges: Edge[] = [
  [0, 1, 1],
  [1, 2, 1],
  [3, 4, 1],
  [5, 6, 1],
];

const uf = new UnionFind(7);
for (const [from, to] of graphEdges) {
  uf.union(from, to);
}
console.log("edges        :", describe(graphEdges));
console.log("components   :", uf.components, "->", uf.groups().map((g) => `{${g.join(",")}}`).join(" "));
console.log("0 and 2 joined:", uf.connected(0, 2), "| 2 and 3 joined:", uf.connected(2, 3));
console.log("set of node 0 holds", uf.setSize(0), "| set of node 5 holds", uf.setSize(5));

const treeEdges: Edge[] = [
  [0, 1, 0],
  [1, 2, 0],
  [2, 3, 0],
];
const ringEdges: Edge[] = [
  [0, 1, 0],
  [1, 2, 0],
  [2, 3, 0],
  [3, 0, 0],
];
console.log("tree cycle   :", hasCycle(4, treeEdges), "| ring cycle:", hasCycle(4, ringEdges));

const weighted: Edge[] = [
  [0, 1, 1],
  [2, 3, 2],
  [0, 2, 3],
  [1, 4, 4],
  [3, 4, 5],
  [1, 2, 6],
];
const mst = kruskal(5, weighted);
console.log("weighted     :", describe(weighted));
console.log("mst edges    :", describe(mst.chosen));
console.log("mst weight   :", mst.weight, "using", mst.chosen.length, "of", weighted.length, "edges");

for (const bad of [-1, 2.5]) {
  try {
    new UnionFind(bad);
    console.log("range        : accepted", bad, "(unexpected)");
  } catch (error) {
    console.log(`range        : "${(error as Error).message}"`);
  }
}
try {
  uf.find(99);
} catch (error) {
  console.log(`range        : "${(error as Error).message}"`);
}

if (countComponents(7, graphEdges) !== 3) throw new Error("expected three components");
if (uf.groups().length !== 3) throw new Error("groups() disagrees with components");
if (uf.setSize(0) !== 3 || uf.setSize(5) !== 2) throw new Error("component sizes are wrong");
if (hasCycle(4, treeEdges)) throw new Error("a tree should have no cycle");
if (!hasCycle(4, ringEdges)) throw new Error("a ring should have a cycle");
if (mst.weight !== 10) throw new Error(`expected an MST weight of 10, got ${mst.weight}`);
if (mst.chosen.length !== 4) throw new Error("a spanning tree of 5 nodes needs 4 edges");
console.log("All checks passed.");
