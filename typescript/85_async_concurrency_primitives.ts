class Semaphore {
  private available: number;
  private readonly waiters: Array<() => void> = [];

  constructor(readonly permits: number) {
    if (!Number.isInteger(permits) || permits < 1) {
      throw new RangeError("permits must be a positive integer");
    }
    this.available = permits;
  }

  get availablePermits(): number {
    return this.available;
  }

  get waiting(): number {
    return this.waiters.length;
  }

  async acquire(): Promise<void> {
    if (this.waiters.length === 0 && this.available > 0) {
      this.available -= 1;
      return;
    }
    await new Promise<void>((resolve) => this.waiters.push(resolve));
  }

  release(): void {
    const next = this.waiters.shift();
    if (next !== undefined) {
      next();
      return;
    }
    if (this.available < this.permits) {
      this.available += 1;
    }
  }

  async run<T>(task: () => Promise<T> | T): Promise<T> {
    await this.acquire();
    try {
      return await task();
    } finally {
      this.release();
    }
  }
}

class Mutex {
  private readonly semaphore = new Semaphore(1);

  get isLocked(): boolean {
    return this.semaphore.availablePermits === 0;
  }

  async runExclusive<T>(task: () => Promise<T> | T): Promise<T> {
    return this.semaphore.run(task);
  }
}

class BoundedQueue<T> {
  private readonly items: T[] = [];
  private readonly readers: Array<(value: T | undefined) => void> = [];
  private readonly writers: Array<() => void> = [];
  private closed = false;

  constructor(readonly capacity: number) {
    if (!Number.isInteger(capacity) || capacity < 1) {
      throw new RangeError("capacity must be a positive integer");
    }
  }

  get size(): number {
    return this.items.length;
  }

  get isClosed(): boolean {
    return this.closed;
  }

  get waitingReaders(): number {
    return this.readers.length;
  }

  async put(value: T): Promise<void> {
    if (this.closed) {
      throw new Error("queue is closed");
    }

    while (this.items.length >= this.capacity) {
      await new Promise<void>((resolve) => this.writers.push(resolve));
      if (this.closed) {
        throw new Error("queue is closed");
      }
    }

    const reader = this.readers.shift();
    if (reader !== undefined) {
      reader(value);
    } else {
      this.items.push(value);
    }
  }

  async take(): Promise<T | undefined> {
    if (this.items.length > 0) {
      const value = this.items.shift() as T;
      const writer = this.writers.shift();
      if (writer !== undefined) {
        writer();
      }
      return value;
    }

    if (this.closed) {
      return undefined;
    }

    return new Promise<T | undefined>((resolve) => this.readers.push(resolve));
  }

  close(): void {
    if (this.closed) {
      return;
    }
    this.closed = true;
    for (const reader of this.readers.splice(0)) {
      reader(undefined);
    }
    for (const writer of this.writers.splice(0)) {
      writer();
    }
  }
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function check(condition: boolean, message: string): void {
  if (!condition) {
    throw new Error(message);
  }
}

async function main(): Promise<void> {
  console.log("--- validation ---");
  const invalid: Array<() => void> = [
    () => new Semaphore(0),
    () => new Semaphore(-1),
    () => new Semaphore(1.5),
    () => new BoundedQueue<number>(0),
    () => new BoundedQueue<number>(-2),
  ];
  for (const attempt of invalid) {
    let threw = false;
    try {
      attempt();
    } catch (error) {
      threw = error instanceof RangeError;
    }
    check(threw, "invalid size should throw RangeError");
  }
  console.log("non-positive and fractional sizes rejected");

  console.log("\n--- semaphore limits concurrency ---");
  const semaphore = new Semaphore(2);
  let active = 0;
  let peak = 0;
  let completed = 0;

  const tasks = Array.from({ length: 8 }, (_, index) =>
    semaphore.run(async () => {
      active += 1;
      peak = Math.max(peak, active);
      await sleep(5);
      active -= 1;
      completed += 1;
      return index;
    }),
  );

  const results = await Promise.all(tasks);
  console.log("completed:", completed, "| peak concurrency:", peak, "| permits left:", semaphore.availablePermits);
  check(completed === 8, "all tasks completed");
  check(peak === 2, `peak concurrency should be 2, got ${peak}`);
  check(semaphore.availablePermits === 2, "permits are returned");
  check(results.join(",") === "0,1,2,3,4,5,6,7", "results keep their order");

  console.log("\n--- first in, first served ---");
  const ordered = new Semaphore(1);
  const order: string[] = [];
  await ordered.acquire();

  const queued = ["a", "b", "c"].map((label) =>
    ordered.acquire().then(() => {
      order.push(label);
    }),
  );
  await sleep(0);
  check(ordered.waiting === 3, "three waiters queued");

  ordered.release();
  await Promise.all(queued.slice(0, 1));
  ordered.release();
  await Promise.all(queued.slice(1, 2));
  ordered.release();
  await Promise.all(queued.slice(2));
  console.log("acquisition order:", order.join(" -> "));
  check(order.join(",") === "a,b,c", "waiters are served in order");

  console.log("\n--- run releases on failure ---");
  const failing = new Semaphore(1);
  let failure = false;
  try {
    await failing.run(async () => {
      throw new Error("task failed");
    });
  } catch (error) {
    failure = (error as Error).message === "task failed";
  }
  check(failure, "the task error propagates");
  check(failing.availablePermits === 1, "the permit is released after a failure");
  console.log("permit restored after a thrown task");

  console.log("\n--- extra releases are capped ---");
  const capped = new Semaphore(2);
  capped.release();
  capped.release();
  capped.release();
  console.log("available after extra releases:", capped.availablePermits);
  check(capped.availablePermits === 2, "available permits never exceed the capacity");

  console.log("\n--- mutex serialises ---");
  const mutex = new Mutex();
  let inside = 0;
  let maximum = 0;
  const sequence: number[] = [];

  await Promise.all(
    [1, 2, 3].map((value) =>
      mutex.runExclusive(async () => {
        inside += 1;
        maximum = Math.max(maximum, inside);
        sequence.push(value);
        await sleep(3);
        inside -= 1;
      }),
    ),
  );
  console.log("mutex order:", sequence.join(" -> "), "| max inside:", maximum);
  check(maximum === 1, "only one task holds the mutex");
  check(sequence.join(",") === "1,2,3", "mutex preserves request order");
  check(!mutex.isLocked, "the mutex is free again");
  check(mutex.isLocked === false, "isLocked reports the state");

  console.log("\n--- bounded queue backpressure ---");
  const queue = new BoundedQueue<number>(2);
  await queue.put(1);
  await queue.put(2);
  check(queue.size === 2, "the queue is full");

  let thirdDone = false;
  const third = queue.put(3).then(() => {
    thirdDone = true;
  });
  await sleep(10);
  console.log("third put pending:", !thirdDone, "| size:", queue.size);
  check(!thirdDone, "a put blocks while the queue is full");
  check(queue.size <= queue.capacity, "the buffer never exceeds capacity");

  const first = await queue.take();
  check(first === 1, "first item out");
  await sleep(0);
  console.log("third put completed after take:", thirdDone, "| size:", queue.size);
  check(thirdDone, "space wakes the blocked producer");
  await third;

  const remaining: number[] = [];
  remaining.push((await queue.take()) as number);
  remaining.push((await queue.take()) as number);
  console.log("drained:", remaining.join(", "));
  check(remaining.join(",") === "2,3", "items come out in order");

  console.log("\n--- waiting readers ---");
  const readerQueue = new BoundedQueue<string>(2);
  const pendingTake = readerQueue.take();
  check(readerQueue.waitingReaders === 1, "a take waits when empty");
  await readerQueue.put("hello");
  check((await pendingTake) === "hello", "the waiting reader receives the value");
  check(readerQueue.waitingReaders === 0, "the reader is gone");

  console.log("\n--- closing ---");
  const closing = new BoundedQueue<number>(2);
  await closing.put(1);
  closing.close();
  check(closing.isClosed, "closed flag set");
  check((await closing.take()) === 1, "buffered items are still readable");
  check((await closing.take()) === undefined, "then the queue reports done");

  let putRejected = false;
  try {
    await closing.put(2);
  } catch (error) {
    putRejected = (error as Error).message === "queue is closed";
  }
  check(putRejected, "put rejects after close");
  console.log("closed queue drains and then refuses writes");

  const blockedReader = new BoundedQueue<number>(1);
  const blockedTake = blockedReader.take();
  blockedReader.close();
  check((await blockedTake) === undefined, "close releases a waiting reader");

  const blockedWriter = new BoundedQueue<number>(1);
  await blockedWriter.put(1);
  const blockedPut = blockedWriter.put(2);
  await sleep(0);
  blockedWriter.close();
  let writerRejected = false;
  try {
    await blockedPut;
  } catch (error) {
    writerRejected = (error as Error).message === "queue is closed";
  }
  check(writerRejected, "close rejects a blocked producer");
  console.log("close unblocks readers and rejects blocked producers");

  console.log("\n--- producer and consumer together ---");
  const pipeline = new BoundedQueue<number>(3);
  const received: number[] = [];

  const producer = (async () => {
    for (let value = 0; value < 20; value += 1) {
      await pipeline.put(value);
    }
    pipeline.close();
  })();

  const consumer = (async () => {
    while (true) {
      const value = await pipeline.take();
      if (value === undefined) {
        break;
      }
      received.push(value);
      await sleep(0);
    }
  })();

  await Promise.all([producer, consumer]);
  console.log("received:", received.length, "values | buffer size:", pipeline.size);
  check(received.length === 20, "every produced value was consumed");
  check(received.join(",") === Array.from({ length: 20 }, (_, index) => index).join(","), "order preserved");
  check(pipeline.size === 0, "nothing left buffered");

  console.log("\nAll checks passed.");
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
