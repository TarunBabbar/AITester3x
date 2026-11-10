const assert = require("node:assert/strict");

const identity = (value) => value;
const constant = (value) => () => value;

function pipe(...functions) {
  return (input) => functions.reduce((value, fn) => fn(value), input);
}

function compose(...functions) {
  return pipe(...[...functions].reverse());
}

function curry(fn) {
  const arity = fn.length;

  const collect = (args) => (...next) => {
    const combined = [...args, ...next];
    return combined.length >= arity ? fn(...combined) : collect(combined);
  };

  return collect([]);
}

function partial(fn, ...preset) {
  return (...rest) => fn(...preset, ...rest);
}

function flip(fn) {
  return (...args) => fn(...args.reverse());
}

function once(fn) {
  let called = false;
  let result;

  return (...args) => {
    if (!called) {
      called = true;
      result = fn(...args);
    }
    return result;
  };
}

function memoize(fn, keyOf = (...args) => JSON.stringify(args)) {
  const cache = new Map();

  const memoized = (...args) => {
    const key = keyOf(...args);
    if (cache.has(key)) {
      return cache.get(key);
    }
    const value = fn(...args);
    cache.set(key, value);
    return value;
  };

  memoized.cache = cache;
  memoized.clear = () => cache.clear();
  return memoized;
}

function tap(sideEffect) {
  return (value) => {
    sideEffect(value);
    return value;
  };
}

function pipeAsync(...functions) {
  return async (input) => {
    let value = input;
    for (const fn of functions) {
      value = await fn(value);
    }
    return value;
  };
}

async function main() {
  console.log("--- pipe and compose ---");
  const double = (value) => value * 2;
  const increment = (value) => value + 1;
  const square = (value) => value * value;

  assert.equal(pipe(double, increment, square)(3), 49);
  assert.equal(compose(square, increment, double)(3), 49);
  assert.equal(pipe()(5), 5);
  assert.equal(compose()(5), 5);
  assert.equal(identity("same"), "same");
  assert.equal(constant(7)(), 7);
  console.log("pipe(2, +1, ^2) of 3 =", pipe(double, increment, square)(3));
  console.log("compose reversed =", compose(square, increment, double)(3));

  console.log("\n--- currying ---");
  const addThree = curry((a, b, c) => a + b + c);
  const combinations = [
    addThree(1)(2)(3),
    addThree(1, 2)(3),
    addThree(1)(2, 3),
    addThree(1, 2, 3),
  ];
  console.log("four call styles:", combinations.join(", "));
  assert.deepEqual(combinations, [6, 6, 6, 6]);

  const greet = curry((greeting, name, punctuation) => `${greeting}, ${name}${punctuation}`);
  const hello = greet("Hello");
  assert.equal(hello("Ada")("!"), "Hello, Ada!");
  assert.equal(hello("Grace")("."), "Hello, Grace.");
  console.log("partial greeting:", hello("Ada")("!"));

  console.log("\n--- partial application ---");
  const subtract = (left, right) => left - right;
  const subtractFromTen = partial(subtract, 10);
  assert.equal(subtractFromTen(3), 7);
  assert.equal(partial(subtract, 10, 4)(), 6);
  assert.equal(flip(subtract)(1, 3), 2);
  console.log("10 - 3 =", subtractFromTen(3), "| flipped 1,3 =", flip(subtract)(1, 3));

  console.log("\n--- once ---");
  let initialisations = 0;
  const initialise = once(() => {
    initialisations += 1;
    return { ready: true };
  });
  const first = initialise();
  const second = initialise();
  console.log("calls:", initialisations, "| same object:", first === second);
  assert.equal(initialisations, 1);
  assert.equal(first, second);

  console.log("\n--- memoisation ---");
  let computations = 0;
  const memoFib = memoize((n) => {
    computations += 1;
    return n < 2 ? n : memoFib(n - 1) + memoFib(n - 2);
  });

  assert.equal(memoFib(30), 832040);
  console.log("fib(30):", memoFib(30), "| computations:", computations);
  assert.equal(computations, 31, "each argument is computed once");

  const afterFirst = computations;
  assert.equal(memoFib(30), 832040);
  assert.equal(computations, afterFirst, "a cache hit does not recompute");
  assert.equal(memoFib.cache.size, 31);

  const byId = memoize((user) => `${user.id}:${user.name}`, (user) => user.id);
  assert.equal(byId({ id: 1, name: "Ada" }), "1:Ada");
  assert.equal(byId({ id: 1, name: "Grace" }), "1:Ada");
  assert.equal(byId.cache.size, 1, "custom keys collapse different objects");
  byId.clear();
  assert.equal(byId.cache.size, 0);
  console.log("custom key and clear() work");

  console.log("\n--- tap ---");
  const traced = [];
  const result = pipe(
    double,
    tap((value) => traced.push(value)),
    increment,
    tap((value) => traced.push(value)),
  )(5);
  console.log("result:", result, "| traced:", traced.join(", "));
  assert.equal(result, 11);
  assert.deepEqual(traced, [10, 11]);

  console.log("\n--- async pipeline ---");
  const steps = [];
  const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));
  const asyncResult = await pipeAsync(
    async (value) => {
      await delay(2);
      steps.push("first");
      return value + 1;
    },
    async (value) => {
      await delay(1);
      steps.push("second");
      return value * 10;
    },
    (value) => {
      steps.push("third");
      return `${value}!`;
    },
  )(4);
  console.log("async result:", asyncResult, "| order:", steps.join(" -> "));
  assert.equal(asyncResult, "50!");
  assert.deepEqual(steps, ["first", "second", "third"]);

  console.log("\n--- errors pass through ---");
  assert.throws(
    () => pipe((value) => value + 1, () => {
      throw new Error("boom");
    })(1),
    /boom/,
  );
  await assert.rejects(
    pipeAsync(async () => {
      throw new Error("async boom");
    })(1),
    /async boom/,
  );
  console.log("synchronous and asynchronous failures propagate");

  console.log("\nAll checks passed.");
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
