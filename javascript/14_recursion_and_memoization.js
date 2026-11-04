// 14 - Recursion and Memoization
//
// Recursion is a function that calls itself on a smaller version of the same
// problem; the base case is what makes it stop. Memoization is the trick of
// remembering a result so the same subproblem is never solved twice, which
// turns an exponential recursion like naive Fibonacci into a linear one.
//
// The call counters below make that difference visible: the same fib(25) costs
// hundreds of thousands of calls without a cache and under fifty with one.
//
// Run with: node 14_recursion_and_memoization.js

// ---------------------------------------------------------------- fibonacci
let naiveCalls = 0;

function fibNaive(n) {
  naiveCalls += 1;
  if (n < 2) return n;
  return fibNaive(n - 1) + fibNaive(n - 2);
}

function makeMemoFib() {
  const cache = new Map();
  let calls = 0;
  const fib = (n) => {
    calls += 1;
    if (n < 2) return n;
    if (cache.has(n)) return cache.get(n);
    const value = fib(n - 1) + fib(n - 2);
    cache.set(n, value);
    return value;
  };
  return { fib, calls: () => calls };
}

const NTH = 25;
const naiveValue = fibNaive(NTH);
const memo = makeMemoFib();
const memoValue = memo.fib(NTH);

console.log("fib naive    :", naiveValue, `in ${naiveCalls} calls`);
console.log("fib memoized :", memoValue, `in ${memo.calls()} calls`);
console.log("same answer  :", naiveValue === memoValue);

// ------------------------------------------------------------------ factorial
const factorialCache = new Map();

function factorial(n) {
  if (n < 0) throw new RangeError("factorial needs a non-negative number");
  if (n < 2) return 1;
  if (factorialCache.has(n)) return factorialCache.get(n);
  const value = n * factorial(n - 1);
  factorialCache.set(n, value);
  return value;
}

console.log("5! recursive :", factorial(5));
console.log("20! grown    :", factorial(20));
console.log("cache keys   :", [...factorialCache.keys()].join(", "));

// ---------------------------------------------------------------- memoize
// A generic wrapper: any function in, a caching version out.
function memoize(fn, keyFn = (...args) => JSON.stringify(args)) {
  const cache = new Map();
  return function (...args) {
    const key = keyFn(...args);
    if (!cache.has(key)) cache.set(key, fn.apply(this, args));
    return cache.get(key);
  };
}

let slowCalls = 0;
function slowSquare(n) {
  slowCalls += 1;
  return n * n;
}

const fastSquare = memoize(slowSquare);
console.log("memo results :", fastSquare(4), fastSquare(4), fastSquare(9), fastSquare(4));
console.log("real work    :", slowCalls, "call(s) for 4 lookups");

// a custom key lets objects be cached by one of their fields
const area = memoize(
  (rect) => rect.width * rect.height,
  (rect) => `${rect.width}x${rect.height}`
);
console.log("area         :", area({ width: 3, height: 4 }), area({ width: 3, height: 4 }));

// ------------------------------------------------ recursion over structures
function flatten(items, depth = Infinity) {
  const out = [];
  for (const item of items) {
    if (Array.isArray(item) && depth > 0) out.push(...flatten(item, depth - 1));
    else out.push(item);
  }
  return out;
}

const nested = [1, [2, [3, [4, [5]]]], 6];
console.log("flatten all  :", flatten(nested).join(", "));
console.log("flatten 1    :", flatten(nested, 1).join(", "));
console.log("flatten 2    :", flatten(nested, 2).join(", "));

function deepClone(value, seen = new WeakMap()) {
  if (value === null || typeof value !== "object") return value;
  if (seen.has(value)) return seen.get(value);
  const copy = Array.isArray(value) ? [] : {};
  seen.set(value, copy);
  for (const [key, inner] of Object.entries(value)) copy[key] = deepClone(inner, seen);
  return copy;
}

const original = { name: "root", tags: ["a", "b"], child: { depth: 2 } };
original.self = original; // a cycle: naive cloning would recurse forever
const clone = deepClone(original);
clone.child.depth = 99;
console.log("clone edited :", clone.child.depth, "original kept:", original.child.depth);
console.log("cycle cloned :", clone.self.name === "root" && clone.self !== original);

const tree = {
  value: 1,
  children: [
    { value: 2, children: [{ value: 4, children: [] }] },
    { value: 3, children: [{ value: 5, children: [] }, { value: 6, children: [] }] },
  ],
};

function treeSum(node) {
  return node.value + node.children.reduce((total, child) => total + treeSum(child), 0);
}

function treeDepth(node) {
  if (node.children.length === 0) return 1;
  return 1 + Math.max(...node.children.map(treeDepth));
}

console.log("tree sum     :", treeSum(tree));
console.log("tree depth   :", treeDepth(tree));

// ------------------------------------------------------- generating results
function permutations(items) {
  if (items.length <= 1) return [items.slice()];
  const out = [];
  items.forEach((item, index) => {
    const rest = [...items.slice(0, index), ...items.slice(index + 1)];
    for (const tail of permutations(rest)) out.push([item, ...tail]);
  });
  return out;
}

console.log("perms of 3   :", permutations([1, 2, 3]).map((p) => p.join("")).join(" "));

function power(base, exponent) {
  if (exponent === 0) return 1;
  const half = power(base, Math.floor(exponent / 2));
  return exponent % 2 === 0 ? half * half : half * half * base;
}

const steps = [];
function powerLogged(base, exponent) {
  steps.push(exponent);
  if (exponent === 0) return 1;
  const half = powerLogged(base, Math.floor(exponent / 2));
  return exponent % 2 === 0 ? half * half : half * half * base;
}

const powered = powerLogged(2, 10);
console.log("2^10 fast     :", power(2, 10), "in", steps.length, "steps halving through", steps.join(", "));
console.log("same result   :", powered === power(2, 10));

// ----------------------------------------------------------------- searching
function binarySearch(sorted, target, low = 0, high = sorted.length - 1) {
  if (low > high) return -1;
  const mid = (low + high) >> 1;
  if (sorted[mid] === target) return mid;
  return sorted[mid] < target
    ? binarySearch(sorted, target, mid + 1, high)
    : binarySearch(sorted, target, low, mid - 1);
}

const numbers = [3, 7, 11, 19, 24, 31, 42, 56, 68, 77];
console.log("find 42      :", binarySearch(numbers, 42), "of", numbers.length);
console.log("find 8       :", binarySearch(numbers, 8));

function gcd(a, b) {
  return b === 0 ? a : gcd(b, a % b);
}

console.log("gcd(48, 36)  :", gcd(48, 36));

// ------------------------------------------------------------ sanity checks
if (naiveValue !== memoValue) throw new Error("the two fibonacci versions disagree");
if (memo.calls() >= naiveCalls) throw new Error("memoization should cost fewer calls");
if (slowCalls !== 2) throw new Error(`expected 2 real square calls, saw ${slowCalls}`);
if (flatten(nested).length !== 6) throw new Error("full flatten lost items");
if (clone.self !== clone) throw new Error("the cloned cycle should point at itself");
if (treeSum(tree) !== 21) throw new Error("tree sum is wrong");
if (binarySearch(numbers, 42) !== 6) throw new Error("binary search is off");
if (binarySearch(numbers, 8) !== -1) throw new Error("a missing target should be -1");
if (gcd(48, 36) !== 12) throw new Error("gcd is wrong");
console.log("All checks passed.");
