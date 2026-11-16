type RandomSource = () => number;

function check(condition: boolean, message: string): void {
  if (!condition) {
    throw new Error(message);
  }
}

function draw(random: RandomSource, limit: number): number {
  const value = random();
  if (!(value >= 0 && value < 1)) {
    throw new RangeError(`random source returned ${value}`);
  }
  return Math.floor(value * limit);
}

class ReservoirSampler<T> {
  #capacity: number;
  #random: RandomSource;
  #items: T[] = [];
  #seen = 0;

  constructor(capacity: number, random: RandomSource = Math.random) {
    if (!Number.isInteger(capacity) || capacity < 0) {
      throw new RangeError("capacity must be a non-negative integer");
    }
    if (typeof random !== "function") {
      throw new TypeError("random must be a function");
    }
    this.#capacity = capacity;
    this.#random = random;
  }

  get capacity(): number {
    return this.#capacity;
  }

  get size(): number {
    return this.#items.length;
  }

  get seenCount(): number {
    return this.#seen;
  }

  get isFull(): boolean {
    return this.#items.length === this.#capacity;
  }

  add(item: T): void {
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

  addAll(items: Iterable<T>): void {
    for (const item of items) {
      this.add(item);
    }
  }

  sample(): T[] {
    return [...this.#items];
  }
}

function seededRandom(seed: number): RandomSource {
  let state = seed;
  return () => {
    state = (state * 1103515245 + 12345) % 2147483648;
    return state / 2147483648;
  };
}

function inclusionCounts<T>(
  trials: number,
  capacity: number,
  stream: readonly T[],
  seed: number,
): Map<T, number> {
  const counts = new Map<T, number>();
  for (const item of stream) {
    counts.set(item, 0);
  }

  const random = seededRandom(seed);
  for (let trial = 0; trial < trials; trial += 1) {
    const sampler = new ReservoirSampler<T>(capacity, random);
    sampler.addAll(stream);
    for (const item of sampler.sample()) {
      counts.set(item, (counts.get(item) ?? 0) + 1);
    }
  }

  return counts;
}

function main(): void {
  console.log("--- empty and zero capacity ---");
  const empty = new ReservoirSampler<string>(3, seededRandom(2));
  check(empty.size === 0 && empty.seenCount === 0, "starts empty");
  check(empty.sample().length === 0, "no sample yet");
  check(!empty.isFull, "not full yet");

  const zero = new ReservoirSampler<number>(0, seededRandom(1));
  zero.addAll([1, 2, 3, 4, 5]);
  check(zero.size === 0 && zero.sample().length === 0, "keeps nothing");
  check(zero.seenCount === 5, "still counts the stream");
  check(zero.isFull, "a zero-capacity reservoir is full");
  console.log("zero capacity:", zero.size, "items after", zero.seenCount, "additions");

  console.log("\n--- a reservoir larger than the stream ---");
  const stream = [0, 1, 2, 3, 4, 5, 6, 7];
  const all = new ReservoirSampler<number>(10, seededRandom(3));
  all.addAll(stream);
  check(all.sample().join(",") === stream.join(","), "keeps the whole stream in order");
  check(all.size === stream.length && all.seenCount === stream.length, "counts match");
  check(!all.isFull, "a larger reservoir has room left");

  const exact = new ReservoirSampler<number>(stream.length, seededRandom(4));
  exact.addAll(stream);
  check(exact.sample().join(",") === stream.join(","), "an exactly-sized reservoir keeps everything");
  check(exact.isFull, "an exactly-sized reservoir is full");
  console.log("sample:", all.sample().join(", "));

  console.log("\n--- the sample is a copy ---");
  const copied = new ReservoirSampler<number>(3, seededRandom(5));
  copied.addAll([1, 2, 3]);
  const returned = copied.sample();
  returned.push(99);
  returned[0] = 42;
  check(copied.size === 3 && copied.sample().join(",") === "1,2,3", "mutating the sample leaves the reservoir alone");
  console.log("internal sample:", copied.sample().join(", "));

  console.log("\n--- replacements keep the size ---");
  const fixed = new ReservoirSampler<number>(3, seededRandom(6));
  for (let item = 0; item < 1_000; item += 1) {
    fixed.add(item);
    check(fixed.size <= 3, "never grows past capacity");
    check(fixed.size === Math.min(item + 1, 3), "fills up and then stays full");
  }
  check(fixed.seenCount === 1_000 && fixed.isFull, "all items counted");
  check(new Set(fixed.sample()).size === 3, "no duplicates from distinct items");
  check(fixed.sample().every((item) => item >= 0 && item < 1_000), "values come from the stream");
  console.log("sample after 1,000 items:", fixed.sample().join(", "));

  console.log("\n--- determinism ---");
  const first = new ReservoirSampler<number>(4, seededRandom(2026));
  const second = new ReservoirSampler<number>(4, seededRandom(2026));
  check(first.sample().join(",") === second.sample().join(","), "two empty reservoirs agree");
  for (let item = 0; item < 100; item += 1) {
    first.add(item);
    second.add(item);
  }
  check(first.sample().join(",") === second.sample().join(","), "same seed, same sample");
  check(first.seenCount === second.seenCount, "same seed, same count");
  console.log("seeded sample:", first.sample().join(", "));

  console.log("\n--- inclusion frequencies ---");
  const trials = 5_000;
  const universe = Array.from({ length: 10 }, (_, index) => index);
  const counts = inclusionCounts(trials, 3, universe, 87);
  const expected = (trials * 3) / universe.length;
  for (const item of universe) {
    const count = counts.get(item) ?? 0;
    check(
      count > expected * 0.85 && count < expected * 1.15,
      `item ${item} was included ${count} times, expected about ${expected}`,
    );
  }
  const total = [...counts.values()].reduce((sum, count) => sum + count, 0);
  check(total === trials * 3, "each trial returns exactly three items");
  console.log(
    `${trials} trials, expected ${expected} each | observed ${[...counts.values()].join(", ")}`,
  );

  console.log("\n--- small streams ---");
  const single = new ReservoirSampler<string>(1, seededRandom(9));
  single.add("only");
  check(single.sample().join(",") === "only" && single.isFull, "one item fills a one-slot reservoir");

  const singleton = new ReservoirSampler<number>(1, seededRandom(10));
  for (let item = 0; item < 100; item += 1) {
    singleton.add(item);
  }
  check(singleton.size === 1 && singleton.seenCount === 100, "a one-slot reservoir keeps one item");
  console.log("one slot, 100 items:", singleton.sample().join(", "));

  console.log("\n--- validation ---");
  const badCapacities = [-1, 1.5, Number.NaN, Number.POSITIVE_INFINITY];
  for (const capacity of badCapacities) {
    let threw = false;
    try {
      new ReservoirSampler<number>(capacity);
    } catch (error) {
      threw = error instanceof RangeError;
    }
    check(threw, `capacity ${capacity} rejected`);
  }

  let typeErrorThrown = false;
  try {
    new ReservoirSampler<number>(1, 5 as unknown as RandomSource);
  } catch (error) {
    typeErrorThrown = error instanceof TypeError;
  }
  check(typeErrorThrown, "a non-function random source rejected");

  const badSources: RandomSource[] = [
    () => 1,
    () => -0.1,
    () => Number.NaN,
    () => Number.POSITIVE_INFINITY,
  ];
  for (const source of badSources) {
    const sampler = new ReservoirSampler<number>(1, source);
    sampler.add(1);
    let threw = false;
    try {
      sampler.add(2);
    } catch (error) {
      threw = error instanceof RangeError;
    }
    check(threw, "an out-of-range draw rejected");
  }
  console.log("negative and fractional capacities, bad sources, and out-of-range draws rejected");

  console.log("\nAll checks passed.");
}

main();
