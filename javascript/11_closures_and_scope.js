function makeCounter(start = 0) {
  let count = start;
  return {
    increment() {
      count += 1;
      return count;
    },
    decrement() {
      count -= 1;
      return count;
    },
    value() {
      return count;
    },
  };
}

const counter = makeCounter();
counter.increment();
counter.increment();
counter.decrement();
console.log("counter value:", counter.value());

function makeGreeter(greeting) {
  return (name) => `${greeting}, ${name}!`;
}

const hello = makeGreeter("Hello");
console.log("closure greeting:", hello("Tarun"));

function curryAdd(a) {
  return (b) => (c) => a + b + c;
}

console.log("curried sum:", curryAdd(1)(2)(3));

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

let initializations = 0;
const initOnce = once(() => {
  initializations += 1;
  return "initialized";
});

console.log("first call:", initOnce());
console.log("second call:", initOnce());
console.log("init count:", initializations);

function memoize(fn) {
  const cache = new Map();
  return (input) => {
    if (!cache.has(input)) {
      cache.set(input, fn(input));
    }
    return cache.get(input);
  };
}

let computed = 0;
const slowSquare = (n) => {
  computed += 1;
  return n * n;
};
const fastSquare = memoize(slowSquare);

console.log("square 6:", fastSquare(6));
console.log("square 6 again:", fastSquare(6));
console.log("computations:", computed);

const handlers = [];
for (let i = 1; i <= 3; i += 1) {
  handlers.push(() => i);
}

console.log("captured via let:", handlers.map((fn) => fn()).join(", "));

function compose(...fns) {
  return (value) => fns.reduceRight((acc, fn) => fn(acc), value);
}

const transform = compose(
  (n) => n * 2,
  (n) => n + 3,
  (n) => n - 1
);
console.log("composed result:", transform(4));
