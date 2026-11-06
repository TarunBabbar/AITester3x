const assert = require("node:assert/strict");

function matches(pattern, event) {
  if (pattern === "*") {
    return true;
  }
  if (pattern === event) {
    return true;
  }
  if (pattern.endsWith(":*")) {
    return event.startsWith(pattern.slice(0, -1));
  }
  return false;
}

class EventEmitter {
  #listeners = new Map();
  #maxListeners;

  constructor({ maxListeners = 10 } = {}) {
    this.#maxListeners = maxListeners;
  }

  on(event, handler, { once = false } = {}) {
    assert.equal(typeof handler, "function", "handler must be a function");
    const entries = this.#listeners.get(event) ?? [];
    entries.push({ handler, once });
    this.#listeners.set(event, entries);

    if (entries.length > this.#maxListeners) {
      console.warn(`warning: ${entries.length} listeners for "${event}"`);
    }

    return () => this.off(event, handler);
  }

  once(event, handler) {
    return this.on(event, handler, { once: true });
  }

  off(event, handler) {
    const entries = this.#listeners.get(event);
    if (!entries) {
      return false;
    }
    const index = entries.findIndex((entry) => entry.handler === handler);
    if (index === -1) {
      return false;
    }
    entries.splice(index, 1);
    if (entries.length === 0) {
      this.#listeners.delete(event);
    }
    return true;
  }

  emit(event, ...args) {
    const snapshot = this.#snapshot(event);
    for (const { pattern, entry } of snapshot) {
      if (entry.once) {
        this.#remove(pattern, entry);
      }
      entry.handler(...args);
    }
    return snapshot.length;
  }

  async emitAsync(event, ...args) {
    const snapshot = this.#snapshot(event);
    for (const { pattern, entry } of snapshot) {
      if (entry.once) {
        this.#remove(pattern, entry);
      }
      await entry.handler(...args);
    }
    return snapshot.length;
  }

  listenerCount(event) {
    return this.#listeners.get(event)?.length ?? 0;
  }

  eventNames() {
    return [...this.#listeners.keys()];
  }

  removeAll(event) {
    if (event === undefined) {
      this.#listeners.clear();
      return;
    }
    this.#listeners.delete(event);
  }

  #snapshot(event) {
    const snapshot = [];
    for (const [pattern, entries] of this.#listeners) {
      if (!matches(pattern, event)) {
        continue;
      }
      for (const entry of entries) {
        snapshot.push({ pattern, entry });
      }
    }
    return snapshot;
  }

  #remove(pattern, entry) {
    const entries = this.#listeners.get(pattern);
    if (!entries) {
      return;
    }
    const index = entries.indexOf(entry);
    if (index !== -1) {
      entries.splice(index, 1);
    }
    if (entries.length === 0) {
      this.#listeners.delete(pattern);
    }
  }
}

async function main() {
  console.log("--- on / emit ---");
  const emitter = new EventEmitter();
  const received = [];

  emitter.on("greet", (name, count) => received.push(`${name}#${count}`));
  const delivered = emitter.emit("greet", "Ada", 1);
  emitter.emit("greet", "Grace", 2);
  console.log("delivered:", delivered, "| received:", received.join(", "));
  assert.equal(delivered, 1);
  assert.deepEqual(received, ["Ada#1", "Grace#2"]);

  console.log("\n--- once ---");
  const emitterOnce = new EventEmitter();
  const onceCalls = [];
  emitterOnce.once("ready", (value) => onceCalls.push(value));
  emitterOnce.emit("ready", "first");
  emitterOnce.emit("ready", "second");
  emitterOnce.emit("ready", "third");
  console.log("once handler calls:", onceCalls.join(", "));
  console.log("listeners left:", emitterOnce.listenerCount("ready"));
  assert.deepEqual(onceCalls, ["first"]);
  assert.equal(emitterOnce.listenerCount("ready"), 0);

  console.log("\n--- unsubscribe ---");
  const unsubscribe = emitter.on("tick", () => received.push("tick"));
  console.log("off by returned function:", unsubscribe());
  console.log("off missing handler:", emitter.off("tick", () => {}));
  assert.equal(emitter.emit("tick"), 0);

  console.log("\n--- wildcards ---");
  const wild = new EventEmitter();
  const wildcardLog = [];
  wild.on("user:*", (name) => wildcardLog.push(`user:* -> ${name}`));
  wild.on("*", (name) => wildcardLog.push(`* -> ${name}`));
  wild.on("user:created", (name) => wildcardLog.push(`exact -> ${name}`));

  const wildDelivered = wild.emit("user:created", "Ada");
  wild.emit("order:placed", "A-1");
  wild.emit("user:deleted", "Bob");
  console.log("delivered for user:created:", wildDelivered);
  console.log(wildcardLog.join("\n"));
  assert.equal(wildDelivered, 3);
  assert.equal(wild.listenerCount("user:*"), 1);

  console.log("\n--- listeners added during emit are not called in that emit ---");
  const late = new EventEmitter();
  const lateLog = [];
  late.on("start", () => {
    lateLog.push("first");
    late.on("start", () => lateLog.push("late"));
  });
  late.emit("start");
  console.log("after first emit:", lateLog.join(", "));
  late.emit("start");
  console.log("after second emit:", lateLog.join(", "));
  assert.deepEqual(lateLog, ["first", "first", "late"]);

  console.log("\n--- async handlers run in order ---");
  const asyncBus = new EventEmitter();
  const order = [];
  asyncBus.on("job", async (name) => {
    await new Promise((resolve) => setTimeout(resolve, 5));
    order.push(`slow ${name}`);
  });
  asyncBus.on("job", (name) => order.push(`fast ${name}`));
  const asyncDelivered = await asyncBus.emitAsync("job", "build");
  console.log("delivered:", asyncDelivered, "| order:", order.join(", "));
  assert.deepEqual(order, ["slow build", "fast build"]);

  console.log("\n--- bookkeeping ---");
  console.log("event names:", wild.eventNames().join(", "));
  wild.removeAll("user:*");
  console.log("after removeAll(user:*):", wild.eventNames().join(", "));
  console.log("listener counts:", wild.listenerCount("user:*"), wild.listenerCount("*"));

  assert.deepEqual(wild.eventNames(), ["*", "user:created"]);
  console.log("\nAll checks passed.");
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
