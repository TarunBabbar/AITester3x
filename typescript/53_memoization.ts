function memoize<Args extends unknown[], R>(fn: (...args: Args) => R): (...args: Args) => R {
  const cache = new Map<string, R>();
  return (...args: Args): R => {
    const key = JSON.stringify(args);
    if (cache.has(key)) {
      console.log(`cache hit for ${key}`);
      return cache.get(key)!;
    }
    const result = fn(...args);
    cache.set(key, result);
    return result;
  };
}

const slowSquare = (n: number): number => {
  console.log(`computing square of ${n}`);
  return n * n;
};

const memoSquare = memoize(slowSquare);

console.log("result:", memoSquare(4));
console.log("result:", memoSquare(4));
console.log("result:", memoSquare(5));
console.log("result:", memoSquare(5));

const memoFib = memoize(function fib(n: number): number {
  return n <= 1 ? n : memoFib(n - 1) + memoFib(n - 2);
});

console.log("fib(10):", memoFib(10));
console.log("fib(20):", memoFib(20));
console.log("fib(20) again:", memoFib(20));

const memoAdd = memoize((a: number, b: number): number => a + b);
console.log("add:", memoAdd(2, 3));
console.log("add:", memoAdd(2, 3));
