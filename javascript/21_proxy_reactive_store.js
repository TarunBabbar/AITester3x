const assert = require("node:assert/strict");

function createStore(initial) {
  const listeners = new Set();
  const proxies = new WeakMap();
  let pending = [];
  let depth = 0;

  const flush = () => {
    if (pending.length === 0) {
      return;
    }
    const batch = pending;
    pending = [];
    for (const listener of [...listeners]) {
      listener(batch);
    }
  };

  const notify = (change) => {
    pending.push(change);
    if (depth === 0) {
      flush();
    }
  };

  const wrap = (value, path) => {
    if (value === null || typeof value !== "object") {
      return value;
    }
    const cached = proxies.get(value);
    if (cached) {
      return cached;
    }

    const proxy = new Proxy(value, {
      get(target, key, receiver) {
        return wrap(Reflect.get(target, key, receiver), [...path, key]);
      },
      set(target, key, newValue, receiver) {
        const existed = Object.prototype.hasOwnProperty.call(target, key);
        const previous = target[key];
        if (existed && Object.is(previous, newValue)) {
          return true;
        }
        const success = Reflect.set(target, key, newValue, receiver);
        notify({
          path: [...path, key],
          kind: existed ? "set" : "add",
          previous,
          value: newValue,
        });
        return success;
      },
      deleteProperty(target, key) {
        const existed = Object.prototype.hasOwnProperty.call(target, key);
        const previous = target[key];
        const success = Reflect.deleteProperty(target, key);
        if (existed) {
          notify({ path: [...path, key], kind: "delete", previous });
        }
        return success;
      },
    });

    proxies.set(value, proxy);
    return proxy;
  };

  const state = wrap(initial, []);

  const subscribe = (listener) => {
    assert.equal(typeof listener, "function", "listener must be a function");
    listeners.add(listener);
    return () => listeners.delete(listener);
  };

  const batch = (callback) => {
    depth += 1;
    try {
      return callback();
    } finally {
      depth -= 1;
      if (depth === 0) {
        flush();
      }
    }
  };

  const watch = (selector, listener) => {
    let current = selector(state);
    return subscribe(() => {
      const next = selector(state);
      if (!Object.is(next, current)) {
        const previous = current;
        current = next;
        listener(next, previous);
      }
    });
  };

  return { state, subscribe, batch, watch };
}

function pathOf(change) {
  return change.path.map(String).join(".");
}

function main() {
  console.log("--- basic updates ---");
  const store = createStore({
    user: { name: "Ada", age: 36 },
    todos: [{ title: "write", done: false }],
  });

  const log = [];
  store.subscribe((changes) => {
    for (const change of changes) {
      log.push(`${change.kind} ${pathOf(change)}`);
    }
  });

  store.state.user.name = "Grace";
  store.state.user.email = "grace@example.com";
  delete store.state.user.age;
  console.log(log.join(" | "));
  assert.deepEqual(log, ["set user.name", "add user.email", "delete user.age"]);
  assert.equal(store.state.user.name, "Grace");

  console.log("\n--- no notification for the same value ---");
  log.length = 0;
  store.state.user.name = "Grace";
  console.log("changes:", log.length);
  assert.equal(log.length, 0);

  console.log("\n--- batching ---");
  log.length = 0;
  const notifications = [];
  const unsubscribeCount = store.subscribe((changes) => notifications.push(changes.length));

  store.batch(() => {
    store.state.user.name = "Linus";
    store.state.user.email = "linus@example.com";
    store.state.todos[0].done = true;
  });
  console.log("batches:", notifications.join(", "), "| changes:", log.join(" | "));
  assert.deepEqual(notifications, [3]);
  assert.deepEqual(log, ["set user.name", "set user.email", "set todos.0.done"]);

  console.log("\n--- nested objects keep their identity ---");
  assert.equal(store.state.user, store.state.user);
  assert.equal(store.state.todos, store.state.todos);
  console.log("stable proxy identity:", store.state.user === store.state.user);

  console.log("\n--- watching a selection ---");
  const nameEvents = [];
  const stopWatching = store.watch(
    (state) => state.user.name,
    (next, previous) => nameEvents.push(`${previous} -> ${next}`),
  );

  store.state.todos[0].done = false;
  store.state.user.name = "Margaret";
  store.state.user.name = "Barbara";
  console.log("watch events:", nameEvents.join(" | "));
  assert.deepEqual(nameEvents, ["Linus -> Margaret", "Margaret -> Barbara"]);

  stopWatching();
  store.state.user.name = "Katherine";
  assert.equal(nameEvents.length, 2, "unsubscribed watcher must not fire");
  console.log("after unsubscribe:", nameEvents.length, "events");

  console.log("\n--- watching an array length ---");
  const lengths = [];
  const stopLengths = store.watch(
    (state) => state.todos.length,
    (next) => lengths.push(next),
  );
  store.state.todos.push({ title: "review", done: false });
  store.state.todos.splice(0, 1);
  console.log("lengths seen:", lengths.join(", "));
  assert.deepEqual(lengths, [2, 1]);
  stopLengths();

  console.log("\n--- unsubscribe ---");
  unsubscribeCount();
  notifications.length = 0;
  store.state.user.name = "Dorothy";
  console.log("notifications after unsubscribe:", notifications.length);
  assert.equal(notifications.length, 0);

  console.log("\n--- snapshot ---");
  const snapshot = JSON.parse(JSON.stringify(store.state));
  console.log(JSON.stringify(snapshot));
  assert.equal(snapshot.user.name, "Dorothy");
  assert.equal(snapshot.todos.length, 1);

  console.log("\n--- independent stores ---");
  const left = createStore({ value: 1 });
  const right = createStore({ value: 1 });
  let rightChanges = 0;
  right.subscribe(() => {
    rightChanges += 1;
  });
  left.state.value = 2;
  console.log("left:", left.state.value, "| right:", right.state.value, "| right changes:", rightChanges);
  assert.equal(right.state.value, 1);
  assert.equal(rightChanges, 0);

  console.log("\nAll checks passed.");
}

main();
