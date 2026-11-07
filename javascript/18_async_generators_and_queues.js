const assert = require("node:assert/strict");

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

async function* batch(items, size) {
  if (!Number.isInteger(size) || size < 1) {
    throw new RangeError("batch size must be a positive integer");
  }

  let buffer = [];
  for await (const item of items) {
    buffer.push(item);
    if (buffer.length === size) {
      yield buffer;
      buffer = [];
    }
  }
  if (buffer.length > 0) {
    yield buffer;
  }
}

async function* ticker(count, intervalMs) {
  for (let tick = 1; tick <= count; tick += 1) {
    await sleep(intervalMs);
    yield tick;
  }
}

class AsyncQueue {
  #buffer = [];
  #waiters = [];
  #closed = false;
  #error = null;

  get size() {
    return this.#buffer.length;
  }

  push(value) {
    if (this.#closed) {
      throw new Error("queue is closed");
    }
    const waiter = this.#waiters.shift();
    if (waiter) {
      waiter.resolve({ value, done: false });
    } else {
      this.#buffer.push(value);
    }
  }

  close() {
    if (this.#closed) {
      return;
    }
    this.#closed = true;
    while (this.#waiters.length > 0) {
      this.#waiters.shift().resolve({ value: undefined, done: true });
    }
  }

  fail(error) {
    this.#error = error;
    this.close();
  }

  next() {
    if (this.#buffer.length > 0) {
      return Promise.resolve({ value: this.#buffer.shift(), done: false });
    }
    if (this.#error) {
      return Promise.reject(this.#error);
    }
    if (this.#closed) {
      return Promise.resolve({ value: undefined, done: true });
    }
    return new Promise((resolve, reject) => {
      this.#waiters.push({ resolve, reject });
    });
  }

  [Symbol.asyncIterator]() {
    return this;
  }
}

async function* merge(sources) {
  const queue = new AsyncQueue();
  let active = sources.length;

  if (active === 0) {
    queue.close();
  }

  for (const source of sources) {
    (async () => {
      try {
        for await (const value of source) {
          queue.push(value);
        }
      } catch (error) {
        queue.fail(error);
      } finally {
        active -= 1;
        if (active === 0) {
          queue.close();
        }
      }
    })();
  }

  yield* queue;
}

async function collect(iterable) {
  const values = [];
  for await (const value of iterable) {
    values.push(value);
  }
  return values;
}

async function main() {
  console.log("--- batching ---");
  const batches = await collect(batch([1, 2, 3, 4, 5, 6, 7], 3));
  console.log(JSON.stringify(batches));
  assert.deepEqual(batches, [[1, 2, 3], [4, 5, 6], [7]]);

  const wholeBatch = await collect(batch([1, 2], 2));
  assert.deepEqual(wholeBatch, [[1, 2]]);

  const noItems = await collect(batch([], 2));
  assert.deepEqual(noItems, []);

  console.log("\n--- batching an async source ---");
  const fromAsync = await collect(batch(ticker(5, 1), 2));
  console.log(JSON.stringify(fromAsync));
  assert.deepEqual(fromAsync, [[1, 2], [3, 4], [5]]);

  console.log("\n--- early break runs the generator's finally block ---");
  const cleanup = [];
  async function* guarded() {
    try {
      for (let value = 1; value <= 5; value += 1) {
        yield value;
      }
    } finally {
      cleanup.push("closed");
    }
  }

  const seen = [];
  for await (const value of guarded()) {
    seen.push(value);
    if (value === 2) {
      break;
    }
  }
  console.log("seen:", seen.join(", "), "| cleanup:", cleanup.join(", "));
  assert.deepEqual(seen, [1, 2]);
  assert.deepEqual(cleanup, ["closed"]);

  console.log("\n--- queue hands values to a waiting consumer ---");
  const queue = new AsyncQueue();
  const received = [];
  const consumer = (async () => {
    for await (const value of queue) {
      received.push(value);
    }
  })();

  const first = queue.next();
  queue.push("a");
  assert.deepEqual(await first, { value: "a", done: false });
  queue.push("b");
  queue.push("c");
  queue.close();
  await consumer;
  console.log("received:", received.join(", "));
  assert.deepEqual(received, ["a", "b", "c"]);
  assert.throws(() => queue.push("late"), /queue is closed/);

  console.log("\n--- merging sources ---");
  const merged = await collect(merge([batch([1, 2, 3], 1), ticker(3, 2), batch(["x", "y"], 1)]));
  console.log(JSON.stringify(merged));
  assert.equal(merged.length, 8);
  assert.deepEqual(merged.filter((value) => typeof value === "number" && value <= 3).sort(), [1, 1, 2, 2, 3, 3]);
  assert.deepEqual(merged.filter((value) => typeof value === "string"), ["x", "y"]);

  console.log("\n--- merging no sources ---");
  assert.deepEqual(await collect(merge([])), []);

  console.log("\n--- errors propagate to the consumer ---");
  async function* failing() {
    yield 1;
    throw new Error("source exploded");
  }

  await assert.rejects(
    async () => {
      for await (const value of merge([failing(), ticker(2, 1)])) {
        void value;
      }
    },
    /source exploded/,
  );
  console.log("merge rejected with the source error");

  console.log("\n--- a consumer can stop early ---");
  const early = [];
  for await (const value of merge([ticker(10, 1), ticker(10, 1)])) {
    early.push(value);
    if (early.length === 3) {
      break;
    }
  }
  console.log("took:", early.join(", "));
  assert.equal(early.length, 3);

  console.log("\nAll checks passed.");
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
