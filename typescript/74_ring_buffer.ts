class RingBuffer<T> {
  private readonly slots: Array<T | undefined>;
  private head = 0;
  private count = 0;
  private overwrites = 0;

  constructor(readonly capacity: number) {
    if (!Number.isInteger(capacity) || capacity <= 0) {
      throw new RangeError("capacity must be a positive integer");
    }
    this.slots = new Array<T | undefined>(capacity);
  }

  get size(): number {
    return this.count;
  }

  get isFull(): boolean {
    return this.count === this.capacity;
  }

  get totalOverwrites(): number {
    return this.overwrites;
  }

  /** Adds a value and returns the one it displaced, if the buffer was full. */
  push(value: T): T | undefined {
    if (this.count < this.capacity) {
      const index = (this.head + this.count) % this.capacity;
      this.slots[index] = value;
      this.count += 1;
      return undefined;
    }

    const evicted = this.slots[this.head];
    this.slots[this.head] = value;
    this.head = (this.head + 1) % this.capacity;
    this.overwrites += 1;
    return evicted;
  }

  shift(): T | undefined {
    if (this.count === 0) {
      return undefined;
    }
    const value = this.slots[this.head];
    this.slots[this.head] = undefined;
    this.head = (this.head + 1) % this.capacity;
    this.count -= 1;
    if (this.count === 0) {
      this.head = 0;
    }
    return value;
  }

  peek(): T | undefined {
    return this.count === 0 ? undefined : this.slots[this.head];
  }

  at(index: number): T | undefined {
    if (index < 0 || index >= this.count) {
      return undefined;
    }
    return this.slots[(this.head + index) % this.capacity];
  }

  toArray(): T[] {
    const values: T[] = [];
    for (let index = 0; index < this.count; index += 1) {
      values.push(this.slots[(this.head + index) % this.capacity] as T);
    }
    return values;
  }

  clear(): void {
    this.slots.fill(undefined);
    this.head = 0;
    this.count = 0;
  }

  *[Symbol.iterator](): Generator<T> {
    for (let index = 0; index < this.count; index += 1) {
      yield this.slots[(this.head + index) % this.capacity] as T;
    }
  }
}

class MovingAverage {
  private readonly buffer: RingBuffer<number>;
  private sum = 0;
  private pushed = 0;

  constructor(capacity: number) {
    this.buffer = new RingBuffer<number>(capacity);
  }

  get size(): number {
    return this.buffer.size;
  }

  get totalPushed(): number {
    return this.pushed;
  }

  get average(): number {
    return this.buffer.size === 0 ? Number.NaN : this.sum / this.buffer.size;
  }

  push(value: number): number {
    if (!Number.isFinite(value)) {
      throw new TypeError("moving average needs finite numbers");
    }
    const evicted = this.buffer.push(value);
    this.sum += value;
    if (evicted !== undefined) {
      this.sum -= evicted;
    }
    this.pushed += 1;
    return this.average;
  }
}

function check(condition: boolean, message: string): void {
  if (!condition) {
    throw new Error(message);
  }
}

function main(): void {
  console.log("--- construction ---");
  let rejected = 0;
  for (const capacity of [0, -1, 1.5, Number.NaN]) {
    try {
      new RingBuffer<number>(capacity);
    } catch (error) {
      rejected += 1;
      void error;
    }
  }
  console.log(`rejected ${rejected} invalid capacities`);
  check(rejected === 4, "invalid capacities should be rejected");

  console.log("\n--- empty buffer ---");
  const empty = new RingBuffer<string>(3);
  console.log("size:", empty.size, "| peek:", empty.peek(), "| shift:", empty.shift(), "| at(0):", empty.at(0));
  check(empty.size === 0 && !empty.isFull, "starts empty");
  check(empty.peek() === undefined && empty.shift() === undefined, "nothing to read");
  check(empty.at(0) === undefined && empty.toArray().length === 0, "nothing to index or list");
  check([...empty].length === 0, "iterating an empty buffer yields nothing");

  console.log("\n--- filling and reading in order ---");
  const buffer = new RingBuffer<number>(3);
  console.log("push 1 returns:", buffer.push(1));
  buffer.push(2);
  buffer.push(3);
  console.log("full:", buffer.isFull, "| contents:", buffer.toArray().join(", "));
  check(buffer.isFull && buffer.size === 3, "three values fit");
  check(buffer.peek() === 1, "the oldest value is at the head");
  check(buffer.at(0) === 1 && buffer.at(2) === 3 && buffer.at(3) === undefined, "indexing is relative to the head");
  check(buffer.toArray().join(",") === "1,2,3", "array order is oldest to newest");

  console.log("\n--- overwriting ---");
  console.log("push 4 evicts:", buffer.push(4));
  console.log("contents:", buffer.toArray().join(", "), "| overwrites:", buffer.totalOverwrites);
  check(buffer.totalOverwrites === 1, "one overwrite recorded");
  check(buffer.toArray().join(",") === "2,3,4", "the oldest value was dropped");
  check(buffer.push(5) === 2, "pushing 5 evicts 2");
  check(buffer.toArray().join(",") === "3,4,5", "and the window moved");

  console.log("\n--- shifting ---");
  check(buffer.shift() === 3, "shift removes the oldest");
  check(buffer.size === 2 && buffer.toArray().join(",") === "4,5", "two left");
  buffer.push(6);
  check(buffer.toArray().join(",") === "4,5,6", "pushing after a shift fills the gap");
  buffer.shift();
  buffer.shift();
  check(buffer.size === 1 && buffer.peek() === 6, "draining keeps the newest");
  buffer.shift();
  check(buffer.size === 0 && buffer.head === 0 && buffer.peek() === undefined, "fully drained");
  buffer.push(7);
  check(buffer.toArray().join(",") === "7", "a drained buffer can be reused");
  console.log("shift drained and refilled the buffer");

  console.log("\n--- iteration ---");
  const iterated = new RingBuffer<string>(4);
  for (const value of ["a", "b", "c", "d", "e"]) {
    iterated.push(value);
  }
  const fromIterator = [...iterated];
  console.log("iterated:", fromIterator.join(", "));
  check(fromIterator.join(",") === "b,c,d,e", "iteration matches toArray");

  console.log("\n--- clear ---");
  iterated.clear();
  check(iterated.size === 0 && iterated.toArray().length === 0, "clear empties the buffer");
  check(iterated.totalOverwrites === 1, "statistics survive a clear");
  iterated.push("x");
  check(iterated.peek() === "x", "usable after clear");

  console.log("\n--- against a reference model ---");
  const modelBuffer = new RingBuffer<number>(5);
  const model: number[] = [];
  let state = 20261106;
  const next = (): number => {
    state = (state * 1103515245 + 12345) % 2147483648;
    return state;
  };

  for (let step = 0; step < 2_000; step += 1) {
    if (next() % 3 === 0 && model.length > 0) {
      const expected = model.shift();
      check(modelBuffer.shift() === expected, `shift mismatch at step ${step}`);
    } else {
      const value = next() % 1_000;
      if (model.length === 5) {
        const expected = model.shift();
        check(modelBuffer.push(value) === expected, `eviction mismatch at step ${step}`);
      } else {
        check(modelBuffer.push(value) === undefined, `unexpected eviction at step ${step}`);
      }
      model.push(value);
    }
    check(modelBuffer.size === model.length, `size mismatch at step ${step}`);
    check(modelBuffer.toArray().join(",") === model.join(","), `contents mismatch at step ${step}`);
  }
  console.log("2000 random operations match a plain array");

  console.log("\n--- moving average ---");
  const moving = new MovingAverage(3);
  check(Number.isNaN(moving.average), "an empty window has no average");

  const samples = [10, 20, 30, 40, 50, 60];
  for (const sample of samples) {
    const average = moving.push(sample);
    console.log(`push ${sample} -> average ${average.toFixed(2)} (window ${moving.size})`);
  }
  check(moving.size === 3, "the window stays at its capacity");
  check(moving.average === 50, "the last three samples average to 50");
  check(moving.totalPushed === samples.length, "every push is counted");

  let threw = false;
  try {
    moving.push(Number.POSITIVE_INFINITY);
  } catch (error) {
    threw = true;
    void error;
  }
  check(threw, "non-finite samples are rejected");

  console.log("\n--- moving average against brute force ---");
  const capacity = 4;
  const brute = new MovingAverage(capacity);
  const window: number[] = [];
  for (let step = 0; step < 500; step += 1) {
    const value = next() % 200;
    window.push(value);
    if (window.length > capacity) {
      window.shift();
    }
    const expected = window.reduce((sum, current) => sum + current, 0) / window.length;
    check(Math.abs(brute.push(value) - expected) < 1e-9, `average mismatch at step ${step}`);
  }
  console.log("500 random samples match the brute-force window average");

  console.log("\nAll checks passed.");
}

main();
