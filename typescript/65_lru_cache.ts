interface Stats {
  hits: number;
  misses: number;
}

class LRUCache<K, V> {
  private store = new Map<K, V>();
  private stats: Stats = { hits: 0, misses: 0 };

  constructor(private readonly capacity: number) {
    if (capacity < 1) {
      throw new Error("capacity must be at least 1");
    }
  }

  get(key: K): V | undefined {
    if (!this.store.has(key)) {
      this.stats.misses++;
      return undefined;
    }
    this.stats.hits++;
    const value = this.store.get(key)!;
    this.store.delete(key);
    this.store.set(key, value);
    return value;
  }

  set(key: K, value: V): void {
    if (this.store.has(key)) {
      this.store.delete(key);
    }
    this.store.set(key, value);
    if (this.store.size > this.capacity) {
      const oldest = this.store.keys().next().value as K;
      this.store.delete(oldest);
    }
  }

  has(key: K): boolean {
    return this.store.has(key);
  }

  get size(): number {
    return this.store.size;
  }

  keys(): K[] {
    return [...this.store.keys()];
  }

  get hitRate(): number {
    const total = this.stats.hits + this.stats.misses;
    return total === 0 ? 0 : this.stats.hits / total;
  }
}

const cache = new LRUCache<string, number>(3);

cache.set("a", 1);
cache.set("b", 2);
cache.set("c", 3);
console.log("keys:", cache.keys().join(", "));

console.log("get a:", cache.get("a"));
console.log("keys after reading a:", cache.keys().join(", "));

cache.set("d", 4);
console.log("keys after inserting d:", cache.keys().join(", "));
console.log("b evicted:", !cache.has("b"));

cache.get("a");
cache.get("x");
console.log("size:", cache.size);
console.log("hit rate:", cache.hitRate.toFixed(2));

try {
  new LRUCache<string, number>(0);
} catch (error) {
  console.log("error:", (error as Error).message);
}
