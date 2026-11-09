const assert = require("node:assert/strict");

class LruCache {
  #capacity;
  #ttlMs;
  #now;
  #entries = new Map();
  #stats = { hits: 0, misses: 0, evictions: 0, expirations: 0 };

  constructor({ capacity = 100, ttlMs = 0, now = Date.now } = {}) {
    if (!Number.isInteger(capacity) || capacity < 1) {
      throw new RangeError("capacity must be a positive integer");
    }
    if (!Number.isFinite(ttlMs) || ttlMs < 0) {
      throw new RangeError("ttlMs must be zero or a positive number");
    }
    if (typeof now !== "function") {
      throw new TypeError("now must be a function");
    }
    this.#capacity = capacity;
    this.#ttlMs = ttlMs;
    this.#now = now;
  }

  get capacity() {
    return this.#capacity;
  }

  get size() {
    this.prune();
    return this.#entries.size;
  }

  get stats() {
    return { ...this.#stats };
  }

  get(key) {
    const entry = this.#entries.get(key);
    if (entry === undefined) {
      this.#stats.misses += 1;
      return undefined;
    }
    if (this.#isExpired(entry)) {
      this.#entries.delete(key);
      this.#stats.expirations += 1;
      this.#stats.misses += 1;
      return undefined;
    }
    this.#entries.delete(key);
    this.#entries.set(key, entry);
    this.#stats.hits += 1;
    return entry.value;
  }

  set(key, value, { ttlMs = this.#ttlMs } = {}) {
    if (!Number.isFinite(ttlMs) || ttlMs < 0) {
      throw new RangeError("ttlMs must be zero or a positive number");
    }

    this.prune();
    if (this.#entries.has(key)) {
      this.#entries.delete(key);
    }

    this.#entries.set(key, {
      value,
      expiresAt: ttlMs > 0 ? this.#now() + ttlMs : Number.POSITIVE_INFINITY,
    });

    while (this.#entries.size > this.#capacity) {
      const oldest = this.#entries.keys().next().value;
      this.#entries.delete(oldest);
      this.#stats.evictions += 1;
    }

    return this;
  }

  has(key) {
    const entry = this.#entries.get(key);
    if (entry === undefined) {
      return false;
    }
    if (this.#isExpired(entry)) {
      this.#entries.delete(key);
      this.#stats.expirations += 1;
      return false;
    }
    return true;
  }

  delete(key) {
    return this.#entries.delete(key);
  }

  clear() {
    this.#entries.clear();
  }

  prune() {
    for (const [key, entry] of this.#entries) {
      if (this.#isExpired(entry)) {
        this.#entries.delete(key);
        this.#stats.expirations += 1;
      }
    }
  }

  keys() {
    this.prune();
    return [...this.#entries.keys()];
  }

  values() {
    this.prune();
    return [...this.#entries.values()].map((entry) => entry.value);
  }

  entries() {
    this.prune();
    return [...this.#entries.entries()].map(([key, entry]) => [key, entry.value]);
  }

  #isExpired(entry) {
    return this.#now() >= entry.expiresAt;
  }
}

function main() {
  console.log("--- least recently used eviction ---");
  let now = 0;
  const clock = () => now;
  const cache = new LruCache({ capacity: 2, ttlMs: 100, now: clock });

  cache.set("a", 1).set("b", 2);
  console.log("keys:", cache.keys().join(", "));
  assert.deepEqual(cache.keys(), ["a", "b"]);

  cache.set("c", 3);
  console.log("after c:", cache.keys().join(", "), "| evictions:", cache.stats.evictions);
  assert.deepEqual(cache.keys(), ["b", "c"]);
  assert.equal(cache.get("a"), undefined);
  assert.equal(cache.stats.evictions, 1);

  console.log("\n--- reading refreshes recency ---");
  cache.set("d", 4);
  console.log("after d:", cache.keys().join(", "));

  const fresh = new LruCache({ capacity: 2, now: clock });
  fresh.set("x", 1).set("y", 2);
  assert.equal(fresh.get("x"), 1);
  fresh.set("z", 3);
  console.log("after get(x) and set(z):", fresh.keys().join(", "));
  assert.deepEqual(fresh.keys(), ["x", "z"]);

  console.log("\n--- overwriting does not grow the cache ---");
  fresh.set("x", 10);
  assert.equal(fresh.size, 2);
  assert.equal(fresh.get("x"), 10);
  assert.equal(fresh.stats.evictions, 1);
  console.log("size:", fresh.size, "| x:", fresh.get("x"));

  console.log("\n--- expiry ---");
  now = 0;
  const expiring = new LruCache({ capacity: 5, ttlMs: 50, now: clock });
  expiring.set("short", "lived");
  now = 49;
  assert.equal(expiring.get("short"), "lived");
  now = 50;
  assert.equal(expiring.get("short"), undefined);
  console.log("value survived 49ms, gone at 50ms");
  assert.equal(expiring.stats.expirations, 1);
  assert.equal(expiring.stats.misses, 1);

  console.log("\n--- per-entry ttl ---");
  now = 0;
  const mixed = new LruCache({ capacity: 5, ttlMs: 100, now: clock });
  mixed.set("default", 1);
  mixed.set("forever", 2, { ttlMs: 0 });
  mixed.set("brief", 3, { ttlMs: 10 });

  now = 10;
  assert.equal(mixed.get("brief"), undefined);
  assert.equal(mixed.get("default"), 1);
  assert.equal(mixed.get("forever"), 2);

  now = 150;
  assert.equal(mixed.get("default"), undefined);
  assert.equal(mixed.get("forever"), 2);
  console.log("default expired, per-entry ttl=0 never expires");

  console.log("\n--- has and prune ---");
  now = 0;
  const checked = new LruCache({ capacity: 5, ttlMs: 20, now: clock });
  checked.set("a", 1);
  now = 10;
  checked.set("b", 2);
  now = 20;
  assert.equal(checked.has("a"), false);
  assert.deepEqual(checked.keys(), ["b"]);
  assert.equal(checked.stats.expirations, 1);
  now = 20;
  checked.set("c", 3);
  now = 45;
  checked.prune();
  console.log("keys after pruning:", checked.keys().join(", "), "| size:", checked.size);
  assert.deepEqual(checked.keys(), []);
  assert.equal(checked.size, 0);

  console.log("\n--- deleting and clearing ---");
  const simple = new LruCache({ capacity: 3 });
  simple.set("a", 1).set("b", 2);
  assert.equal(simple.delete("a"), true);
  assert.equal(simple.delete("a"), false);
  simple.clear();
  assert.equal(simple.size, 0);
  console.log("delete and clear work");

  console.log("\n--- stats ---");
  const counted = new LruCache({ capacity: 1 });
  counted.set("a", 1);
  counted.get("a");
  counted.get("missing");
  counted.set("b", 2);
  counted.get("a");

  console.log(JSON.stringify(counted.stats));
  assert.deepEqual(counted.stats, { hits: 1, misses: 2, evictions: 1, expirations: 0 });

  console.log("\n--- validation ---");
  const invalid = [
    () => new LruCache({ capacity: 0 }),
    () => new LruCache({ capacity: 1.5 }),
    () => new LruCache({ ttlMs: -1 }),
    () => new LruCache({ now: 5 }),
  ];
  for (const attempt of invalid) {
    let rejected = false;
    try {
      attempt();
    } catch (error) {
      rejected = error instanceof RangeError || error instanceof TypeError;
    }
    assert.equal(rejected, true);
  }
  assert.throws(() => simple.set("x", 1, { ttlMs: -5 }), RangeError);
  console.log("bad capacity, ttl, and clock are rejected");

  console.log("\n--- against a reference model ---");
  now = 0;
  const modelCache = new LruCache({ capacity: 3, ttlMs: 30, now: clock });
  const model = new Map();

  let state = 24;
  const next = () => {
    state = (state * 1103515245 + 12345) % 2147483648;
    return state;
  };

  for (let step = 0; step < 500; step += 1) {
    const key = `k${next() % 6}`;
    const action = next() % 4;
    now += next() % 5;

    if (action === 0) {
      for (const [modelKey, entry] of [...model]) {
        if (now >= entry.expiresAt) {
          model.delete(modelKey);
        }
      }
      while (model.size >= 3 && !model.has(key)) {
        model.delete(model.keys().next().value);
      }
      model.delete(key);
      model.set(key, { value: step, expiresAt: now + 30 });
      modelCache.set(key, step);
    } else if (action === 1) {
      const expected = model.get(key);
      if (expected && now >= expected.expiresAt) {
        model.delete(key);
      }
      assert.equal(modelCache.get(key), model.get(key)?.value);
    } else if (action === 2) {
      const entry = model.get(key);
      if (entry && now >= entry.expiresAt) {
        model.delete(key);
      }
      assert.equal(modelCache.has(key), model.has(key));
    } else {
      assert.equal(modelCache.delete(key), model.delete(key));
    }

    for (const [modelKey, entry] of [...model]) {
      if (now >= entry.expiresAt) {
        model.delete(modelKey);
      }
    }
    assert.deepEqual(modelCache.keys().sort(), [...model.keys()].sort(), `keys at step ${step}`);
    assert.equal(modelCache.size, model.size, `size at step ${step}`);
  }
  console.log("500 random operations match a hand-written model");

  console.log("\nAll checks passed.");
}

main();
