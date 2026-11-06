function fnv1a(value: string): number {
  let hash = 0x811c9dc5;
  for (let index = 0; index < value.length; index += 1) {
    hash ^= value.charCodeAt(index);
    hash = Math.imul(hash, 0x01000193) >>> 0;
  }
  return hash >>> 0;
}

interface RingEntry {
  hash: number;
  node: string;
}

class ConsistentHashRing {
  private ring: RingEntry[] = [];
  private readonly members = new Map<string, number>();

  constructor(private readonly virtualNodes = 120) {
    if (virtualNodes <= 0) {
      throw new RangeError("virtual node count must be positive");
    }
  }

  get nodes(): string[] {
    return [...this.members.keys()];
  }

  get size(): number {
    return this.ring.length;
  }

  addNode(node: string): void {
    if (this.members.has(node)) {
      throw new Error(`node ${node} is already on the ring`);
    }

    this.members.set(node, this.virtualNodes);
    for (let replica = 0; replica < this.virtualNodes; replica += 1) {
      this.ring.push({ hash: fnv1a(`${node}#${replica}`), node });
    }
    this.ring.sort((left, right) => left.hash - right.hash || left.node.localeCompare(right.node));
  }

  removeNode(node: string): boolean {
    if (!this.members.delete(node)) {
      return false;
    }
    this.ring = this.ring.filter((entry) => entry.node !== node);
    return true;
  }

  get(key: string): string | undefined {
    if (this.ring.length === 0) {
      return undefined;
    }
    return this.ring[this.indexFor(fnv1a(key))].node;
  }

  getNodes(key: string, count: number): string[] {
    if (this.ring.length === 0 || count <= 0) {
      return [];
    }

    const start = this.indexFor(fnv1a(key));
    const found: string[] = [];
    const seen = new Set<string>();

    for (let step = 0; step < this.ring.length && found.length < count; step += 1) {
      const entry = this.ring[(start + step) % this.ring.length];
      if (!seen.has(entry.node)) {
        seen.add(entry.node);
        found.push(entry.node);
      }
    }

    return found;
  }

  distribution(keys: string[]): Map<string, number> {
    const counts = new Map<string, number>(this.nodes.map((node) => [node, 0]));
    for (const key of keys) {
      const node = this.get(key);
      if (node !== undefined) {
        counts.set(node, (counts.get(node) ?? 0) + 1);
      }
    }
    return counts;
  }

  shares(keys: string[]): Map<string, number> {
    const counts = this.distribution(keys);
    const shares = new Map<string, number>();
    for (const [node, count] of counts) {
      shares.set(node, keys.length === 0 ? 0 : count / keys.length);
    }
    return shares;
  }

  /** First entry at or after the hash, wrapping to the start of the ring. */
  private indexFor(hash: number): number {
    let low = 0;
    let high = this.ring.length;

    while (low < high) {
      const middle = (low + high) >> 1;
      if (this.ring[middle].hash < hash) {
        low = middle + 1;
      } else {
        high = middle;
      }
    }

    return low === this.ring.length ? 0 : low;
  }
}

function spread(shares: Map<string, number>): number {
  const values = [...shares.values()];
  return Math.max(...values) - Math.min(...values);
}

function check(condition: boolean, message: string): void {
  if (!condition) {
    throw new Error(message);
  }
}

function main(): void {
  const keys = Array.from({ length: 10_000 }, (_, index) => `user-${index}`);

  console.log("--- an empty ring ---");
  const empty = new ConsistentHashRing();
  console.log("node for any key:", empty.get("user-1"), "| nodes:", empty.nodes.length);
  check(empty.get("user-1") === undefined, "an empty ring maps nothing");
  check(empty.getNodes("user-1", 2).length === 0, "and hits nothing");
  check(!empty.removeNode("missing"), "removing an unknown node reports false");

  console.log("\n--- three nodes ---");
  const ring = new ConsistentHashRing();
  for (const node of ["cache-a", "cache-b", "cache-c"]) {
    ring.addNode(node);
  }
  console.log(`ring size: ${ring.size} entries for ${ring.nodes.length} nodes`);

  const shares = ring.shares(keys);
  console.log(
    "shares:",
    [...shares.entries()].map(([node, share]) => `${node}=${(share * 100).toFixed(1)}%`).join(" "),
  );
  check(ring.nodes.length === 3, "three nodes on the ring");
  check(ring.size === 3 * 120, "each node contributes its virtual nodes");
  check([...shares.values()].reduce((sum, share) => sum + share, 0) === 1, "shares add up to one");
  for (const share of shares.values()) {
    check(share > 0.2 && share < 0.5, `share ${share} should be near a third`);
  }

  console.log("\n--- a key is stable ---");
  const sample = ["user-1", "user-2", "user-3", "user-42", "user-9999"];
  for (const key of sample) {
    check(ring.get(key) === ring.get(key), "the same key maps to the same node");
  }
  console.log(sample.map((key) => `${key}->${ring.get(key)}`).join(" "));

  console.log("\n--- minimal disruption on removal ---");
  const before = new Map(keys.map((key) => [key, ring.get(key) as string]));
  check(ring.removeNode("cache-b"), "cache-b was on the ring");
  check(!ring.removeNode("cache-b"), "removing it twice reports false");

  let movedFromOthers = 0;
  let movedFromRemoved = 0;
  for (const key of keys) {
    const now = ring.get(key);
    const was = before.get(key);
    if (was === now) {
      continue;
    }
    if (was === "cache-b") {
      movedFromRemoved += 1;
    } else {
      movedFromOthers += 1;
    }
  }
  console.log("keys that changed node:", movedFromRemoved + movedFromOthers);
  console.log("previously on cache-b:", movedFromRemoved, "| from other nodes:", movedFromOthers);
  check(movedFromOthers === 0, "only keys on the removed node move");
  check(movedFromRemoved > 0, "and all of its keys move somewhere");

  console.log("\n--- minimal disruption on addition ---");
  const beforeAdd = new Map(keys.map((key) => [key, ring.get(key) as string]));
  ring.addNode("cache-d");
  let moved = 0;
  let movedToNew = 0;
  for (const key of keys) {
    const now = ring.get(key);
    if (now === beforeAdd.get(key)) {
      continue;
    }
    moved += 1;
    if (now === "cache-d") {
      movedToNew += 1;
    }
  }
  console.log("keys moved:", moved, "| all of them to cache-d:", moved === movedToNew);
  check(moved === movedToNew, "a new node only takes keys, it never reshuffles the rest");

  console.log("\n--- replicas ---");
  const replicas = ring.getNodes("user-1", 3);
  console.log("first three nodes for user-1:", replicas.join(" -> "));
  check(replicas.length === 3, "three distinct nodes returned");
  check(new Set(replicas).size === 3, "no duplicates");
  check(replicas[0] === ring.get("user-1"), "the first is the primary");
  check(ring.getNodes("user-1", 10).length === 3, "cannot return more nodes than exist");

  console.log("\n--- insertion order does not matter ---");
  const left = new ConsistentHashRing();
  const right = new ConsistentHashRing();
  for (const node of ["cache-a", "cache-b", "cache-c"]) {
    left.addNode(node);
  }
  for (const node of ["cache-c", "cache-a", "cache-b"]) {
    right.addNode(node);
  }
  for (const key of keys.slice(0, 1_000)) {
    check(left.get(key) === right.get(key), "the ring is order independent");
  }
  console.log("two rings built in different orders map 1000 keys identically");

  console.log("\n--- virtual nodes smooth the distribution ---");
  const coarse = new ConsistentHashRing(1);
  const fine = new ConsistentHashRing(200);
  for (const node of ["a", "b", "c", "d"]) {
    coarse.addNode(node);
    fine.addNode(node);
  }
  const coarseSpread = spread(coarse.shares(keys));
  const fineSpread = spread(fine.shares(keys));
  console.log(`one replica spread: ${(coarseSpread * 100).toFixed(1)} points`);
  console.log(`200 replicas spread: ${(fineSpread * 100).toFixed(1)} points`);
  check(fineSpread < coarseSpread, "more virtual nodes should balance the load better");
  check(fineSpread < 0.15, "and keep every node within a few points of a quarter");

  console.log("\n--- draining the ring ---");
  for (const node of ring.nodes) {
    check(ring.removeNode(node), `removing ${node}`);
  }
  check(ring.nodes.length === 0 && ring.size === 0, "the ring is empty");
  check(ring.get("user-1") === undefined, "and maps nothing");
  console.log("all nodes removed, ring empty");

  console.log("\nAll checks passed.");
}

main();
