class CancellationError extends Error {
  constructor(readonly reason: string) {
    super(`operation cancelled: ${reason}`);
    this.name = "CancellationError";
  }
}

type CancelListener = (reason: string) => void;
type Unsubscribe = () => void;

class CancellationToken {
  private cancelled = false;
  private cancellationReason = "";
  private readonly listeners = new Set<CancelListener>();

  static readonly none = new CancellationToken();

  get isCancelled(): boolean {
    return this.cancelled;
  }

  get reason(): string {
    return this.cancellationReason;
  }

  throwIfCancelled(): void {
    if (this.cancelled) {
      throw new CancellationError(this.cancellationReason);
    }
  }

  onCancel(listener: CancelListener): Unsubscribe {
    if (this.cancelled) {
      listener(this.cancellationReason);
      return () => {};
    }
    this.listeners.add(listener);
    return () => {
      this.listeners.delete(listener);
    };
  }

  toPromise(): Promise<never> {
    return new Promise((_, reject) => {
      if (this.cancelled) {
        reject(new CancellationError(this.cancellationReason));
        return;
      }
      this.onCancel((reason) => reject(new CancellationError(reason)));
    });
  }

  cancel(reason = "cancelled"): void {
    if (this.cancelled) {
      return;
    }
    this.cancelled = true;
    this.cancellationReason = reason;

    const listeners = [...this.listeners];
    this.listeners.clear();
    for (const listener of listeners) {
      listener(reason);
    }
  }
}

class CancellationTokenSource {
  readonly token = new CancellationToken();
  private timer: ReturnType<typeof setTimeout> | null = null;
  private unsubscribes: Unsubscribe[] = [];

  cancel(reason = "cancelled"): void {
    if (this.token.isCancelled) {
      return;
    }
    this.token.cancel(reason);
    this.dispose();
  }

  dispose(): void {
    if (this.timer !== null) {
      clearTimeout(this.timer);
      this.timer = null;
    }
    for (const unsubscribe of this.unsubscribes) {
      unsubscribe();
    }
    this.unsubscribes = [];
  }

  static timeout(ms: number, reason = "timeout"): CancellationTokenSource {
    if (!Number.isFinite(ms) || ms < 0) {
      throw new RangeError("timeout must be a non-negative number");
    }
    const source = new CancellationTokenSource();
    source.timer = setTimeout(() => source.cancel(reason), ms);
    return source;
  }

  static linked(...tokens: CancellationToken[]): CancellationTokenSource {
    const source = new CancellationTokenSource();
    source.unsubscribes = tokens.map((token) => token.onCancel((reason) => source.cancel(reason)));
    if (source.token.isCancelled) {
      source.dispose();
    }
    return source;
  }
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function cancellableDelay(ms: number, token: CancellationToken): Promise<void> {
  return new Promise((resolve, reject) => {
    if (token.isCancelled) {
      reject(new CancellationError(token.reason));
      return;
    }

    const timer = setTimeout(() => {
      unsubscribe();
      resolve();
    }, ms);

    const unsubscribe = token.onCancel((reason) => {
      clearTimeout(timer);
      reject(new CancellationError(reason));
    });
  });
}

async function countTicks(token: CancellationToken, limit: number): Promise<number> {
  let ticks = 0;
  for (let index = 0; index < limit; index += 1) {
    token.throwIfCancelled();
    await sleep(1);
    ticks += 1;
  }
  return ticks;
}

function check(condition: boolean, message: string): void {
  if (!condition) {
    throw new Error(message);
  }
}

async function main(): Promise<void> {
  console.log("--- states ---");
  const source = new CancellationTokenSource();
  const token = source.token;
  console.log("before cancel:", token.isCancelled, "| reason:", JSON.stringify(token.reason));
  check(!token.isCancelled, "a fresh token is live");
  check(token.reason === "", "no reason yet");
  token.throwIfCancelled();

  source.cancel("user navigated away");
  console.log("after cancel:", token.isCancelled, "| reason:", JSON.stringify(token.reason));
  check(token.isCancelled, "token is cancelled");
  check(token.reason === "user navigated away", "reason is kept");

  let thrown: unknown = null;
  try {
    token.throwIfCancelled();
  } catch (error) {
    thrown = error;
  }
  check(thrown instanceof CancellationError, "throwIfCancelled throws CancellationError");
  check((thrown as CancellationError).reason === "user navigated away", "error carries the reason");
  console.log("throwIfCancelled:", (thrown as Error).message);

  console.log("\n--- idempotent cancellation ---");
  source.cancel("second reason");
  check(token.reason === "user navigated away", "the first reason wins");
  console.log("reason after a second cancel:", JSON.stringify(token.reason));

  console.log("\n--- listeners ---");
  const listenerSource = new CancellationTokenSource();
  const events: string[] = [];
  const unsubscribe = listenerSource.token.onCancel((reason) => events.push(`first:${reason}`));
  listenerSource.token.onCancel((reason) => events.push(`second:${reason}`));

  listenerSource.cancel("stop");
  console.log("events:", events.join(", "));
  check(events.length === 2, "both listeners fired");

  events.length = 0;
  const lateUnsubscribe = listenerSource.token.onCancel((reason) => events.push(`late:${reason}`));
  console.log("late listener fired immediately:", events.join(", "));
  check(events.join(",") === "late:stop", "listeners added after cancellation fire at once");
  lateUnsubscribe();

  const beforeUnsubscribeSource = new CancellationTokenSource();
  const beforeUnsubscribeEvents: string[] = [];
  const stop = beforeUnsubscribeSource.token.onCancel(() => beforeUnsubscribeEvents.push("kept"));
  const removed = beforeUnsubscribeSource.token.onCancel(() => beforeUnsubscribeEvents.push("removed"));
  stop();
  removed();
  beforeUnsubscribeSource.cancel();
  console.log("events after unsubscribing:", beforeUnsubscribeEvents.length);
  check(beforeUnsubscribeEvents.length === 0, "unsubscribed listeners do not fire");
  unsubscribe();

  console.log("\n--- timeout ---");
  const timeoutSource = CancellationTokenSource.timeout(10, "too slow");
  check(!timeoutSource.token.isCancelled, "not cancelled yet");
  await sleep(30);
  console.log("timeout token:", timeoutSource.token.isCancelled, "| reason:", timeoutSource.token.reason);
  check(timeoutSource.token.isCancelled, "the timeout fires");
  check(timeoutSource.token.reason === "too slow", "the timeout reason is kept");

  const clearedSource = CancellationTokenSource.timeout(100);
  clearedSource.dispose();
  await sleep(120);
  check(!clearedSource.token.isCancelled, "dispose clears the timer");
  console.log("disposed timeout never fires");

  let invalid = false;
  try {
    CancellationTokenSource.timeout(-1);
  } catch (error) {
    invalid = true;
    void error;
  }
  check(invalid, "negative timeout rejected");

  console.log("\n--- linked sources ---");
  const first = new CancellationTokenSource();
  const second = new CancellationTokenSource();
  const linked = CancellationTokenSource.linked(first.token, second.token);
  console.log("linked live:", !linked.token.isCancelled);
  first.cancel("parent one");
  console.log("after first parent:", linked.token.isCancelled, linked.token.reason);
  check(linked.token.isCancelled && linked.token.reason === "parent one", "parent cancellation propagates");
  check(!second.token.isCancelled, "the other parent is untouched");

  const third = new CancellationTokenSource();
  const fourth = new CancellationTokenSource();
  const detached = CancellationTokenSource.linked(third.token);
  detached.dispose();
  third.cancel("ignored");
  check(!detached.token.isCancelled, "disposing the link stops propagation");
  console.log("detached link ignores the parent");

  const alreadyCancelled = new CancellationTokenSource();
  alreadyCancelled.cancel("early");
  const lateLink = CancellationTokenSource.linked(alreadyCancelled.token);
  check(lateLink.token.isCancelled, "linking an already-cancelled token cancels immediately");
  console.log("linking a cancelled token propagates at once");

  console.log("\n--- cooperative work ---");
  const workSource = new CancellationTokenSource();
  const work = countTicks(workSource.token, 1_000);
  setTimeout(() => workSource.cancel("enough"), 10);
  let cancelledWork = false;
  try {
    await work;
  } catch (error) {
    cancelledWork = error instanceof CancellationError;
  }
  check(cancelledWork, "cooperative work stops with a CancellationError");
  console.log("cooperative loop stopped early");

  const completedSource = new CancellationTokenSource();
  const completed = await countTicks(completedSource.token, 5);
  check(completed === 5, "an uncancelled loop finishes");
  console.log("uncancelled loop completed", completed, "ticks");

  console.log("\n--- cancellable delay ---");
  const delaySource = new CancellationTokenSource();
  await cancellableDelay(5, delaySource.token);
  console.log("delay completed");

  const interruptedSource = new CancellationTokenSource();
  const interrupted = cancellableDelay(1_000, interruptedSource.token);
  setTimeout(() => interruptedSource.cancel("no longer needed"), 10);
  let delayCancelled = false;
  try {
    await interrupted;
  } catch (error) {
    delayCancelled = error instanceof CancellationError;
  }
  check(delayCancelled, "a cancelled delay rejects early");
  console.log("long delay rejected early");

  const preCancelled = new CancellationTokenSource();
  preCancelled.cancel("already");
  let immediate = false;
  try {
    await cancellableDelay(100, preCancelled.token);
  } catch (error) {
    immediate = error instanceof CancellationError;
  }
  check(immediate, "an already-cancelled token rejects without waiting");
  console.log("pre-cancelled delay rejects immediately");

  console.log("\n--- token promise ---");
  const promiseSource = new CancellationTokenSource();
  const pending = promiseSource.token.toPromise();
  setTimeout(() => promiseSource.cancel("promise stop"), 5);
  let promiseCancelled = false;
  try {
    await pending;
  } catch (error) {
    promiseCancelled = error instanceof CancellationError;
  }
  check(promiseCancelled, "toPromise rejects on cancellation");
  console.log("toPromise rejected with the cancellation error");

  const noneRejected = await Promise.race([
    CancellationToken.none.toPromise().then(() => "cancelled"),
    Promise.resolve("still alive"),
  ]);
  check(noneRejected === "still alive", "the shared none token never cancels");
  console.log("CancellationToken.none stays live");

  console.log("\nAll checks passed.");
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
