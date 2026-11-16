const assert = require("node:assert/strict");

function draw(random, limit) {
  const value = random();
  if (!(value >= 0 && value < 1)) {
    throw new RangeError(`random source returned ${value}`);
  }
  return Math.floor(value * limit);
}

class ReservoirSampler {
  #capacity;
  #random;
  #items = [];
  #seen = 0;

  constructor(capacity, random = Math.random) {
    if (!Number.isInteger(capacity) || capacity < 0) {
      throw new RangeError("capacity must be a non-negative integer");
    }
    if (typeof random !== "function") {
      throw new TypeError("random must be a function");
    }
    this.#capacity = capacity;
    this.#random = random;
  }

  get capacity() {
    return this.#capacity;
  }

  get size() {
    return this.#items.length;
  }

  get seenCount() {
    return this.#seen;
  }

  get isFull() {
    return this.#items.length === this.#capacity;
  }

  add(item) {
    this.#seen += 1;
    if (this.#items.length < this.#capacity) {
      this.#items.push(item);
      return;
    }
    if (this.#capacity === 0) {
      return;
    }
    const slot = draw(this.#random, this.#seen);
    if (slot < this.#capacity) {
      this.#items[slot] = item;
    }
  }

  addAll(items) {
    for (const item of items) {
      this.add(item);
    }
  }

  sample() {
    return [...this.#items];
  }
}

function seededRandom(seed) {
  let state = seed;
  return () => {
    state = (state * 1103515245 + 12345) % 2147483648;
    return state / 2147483648;
  };
}

function inclusionCounts(trials, capacity, stream, seed) {
  const counts = new Map();
  for (const item of stream) {
    counts.set(item, 0);
  }

  const random = seededRandom(seed);
  for (let trial = 0; trial < trials; trial += 1) {
    const sampler = new ReservoirSampler(capacity, random);
    sampler.addAll(stream);
    for (const item of sampler.sample()) {
      counts.set(item, counts.get(item) + 1);
    }
  }

  return counts;
}

function main() {
  console.log("--- empty and zero capacity ---");
  const empty = new ReservoirSampler(3, seededRandom(2));
  assert.equal(empty.size, 0);
  assert.equal(empty.seenCount, 0);
  assert.deepEqual(empty.sample(), []);
  assert.equal(empty.isFull, false);

  const zero = new ReservoirSampler(0, seededRandom(1));
  zero.addAll([1, 2, 3, 4, 5]);
  assert.equal(zero.size, 0);
  assert.deepEqual(zero.sample(), []);
  assert.equal(zero.seenCount, 5);
  assert.equal(zero.isFull, true);
  console.log("zero capacity:", zero.size, "items after", zero.seenCount, "additions");

  console.log("\n--- a reservoir larger than the stream ---");
  const stream = [0, 1, 2, 3, 4, 5, 6, 7];
  const all = new ReservoirSampler(10, seededRandom(3));
  all.addAll(stream);
  assert.deepEqual(all.sample(), stream);
  assert.equal(all.size, stream.length);
  assert.equal(all.seenCount, stream.length);
  assert.equal(all.isFull, false);

  const exact = new ReservoirSampler(stream.length, seededRandom(4));
  exact.addAll(stream);
  assert.deepEqual(exact.sample(), stream);
  assert.equal(exact.isFull, true);
  console.log("sample:", all.sample().join(", "));

  console.log("\n--- the sample is a copy ---");
  const copied = new ReservoirSampler(3, seededRandom(5));
  copied.addAll([1, 2, 3]);
  const returned = copied.sample();
  returned.push(99);
  returned[0] = 42;
  assert.deepEqual(copied.sample(), [1, 2, 3]);
  console.log("internal sample:", copied.sample().join(", "));

  console.log("\n--- replacements keep the size ---");
  const fixed = new ReservoirSampler(3, seededRandom(6));
  for (let item = 0; item < 1_000; item += 1) {
    fixed.add(item);
    assert.ok(fixed.size <= 3);
    assert.equal(fixed.size, Math.min(item + 1, 3));
  }
  assert.equal(fixed.seenCount, 1_000);
  assert.equal(fixed.isFull, true);
  assert.equal(new Set(fixed.sample()).size, 3);
  assert.ok(fixed.sample().every((item) => item >= 0 && item < 1_000));
  console.log("sample after 1,000 items:", fixed.sample().join(", "));

  console.log("\n--- determinism ---");
  const first = new ReservoirSampler(4, seededRandom(2026));
  const second = new ReservoirSampler(4, seededRandom(2026));
  assert.deepEqual(first.sample(), second.sample());
  for (let item = 0; item < 100; item += 1) {
    first.add(item);
    second.add(item);
  }
  assert.deepEqual(first.sample(), second.sample());
  assert.equal(first.seenCount, second.seenCount);
  console.log("seeded sample:", first.sample().join(", "));

  console.log("\n--- inclusion frequencies ---");
  const trials = 5_000;
  const universe = Array.from({ length: 10 }, (_, index) => index);
  const counts = inclusionCounts(trials, 3, universe, 87);
  const expected = (trials * 3) / universe.length;
  for (const item of universe) {
    const count = counts.get(item);
    assert.ok(
      count > expected * 0.85 && count < expected * 1.15,
      `item ${item} was included ${count} times, expected about ${expected}`,
    );
  }
  const total = [...counts.values()].reduce((sum, count) => sum + count, 0);
  assert.equal(total, trials * 3);
  console.log(
    `${trials} trials, expected ${expected} each | observed ${[...counts.values()].join(", ")}`,
  );

  console.log("\n--- small streams ---");
  const single = new ReservoirSampler(1, seededRandom(9));
  single.add("only");
  assert.deepEqual(single.sample(), ["only"]);
  assert.equal(single.isFull, true);

  const singleton = new ReservoirSampler(1, seededRandom(10));
  for (let item = 0; item < 100; item += 1) {
    singleton.add(item);
  }
  assert.equal(singleton.size, 1);
  assert.equal(singleton.seenCount, 100);
  console.log("one slot, 100 items:", singleton.sample().join(", "));

  console.log("\n--- validation ---");
  for (const capacity of [-1, 1.5, Number.NaN, Number.POSITIVE_INFINITY]) {
    assert.throws(() => new ReservoirSampler(capacity), RangeError);
  }
  assert.throws(() => new ReservoirSampler(1, 5), TypeError);

  const badSources = [
    () => 1,
    () => -0.1,
    () => Number.NaN,
    () => Number.POSITIVE_INFINITY,
  ];
  for (const source of badSources) {
    const sampler = new ReservoirSampler(1, source);
    sampler.add(1);
    assert.throws(() => sampler.add(2), RangeError);
  }
  console.log("negative and fractional capacities, bad sources, and out-of-range draws rejected");

  console.log("\nAll checks passed.");
}

main();
