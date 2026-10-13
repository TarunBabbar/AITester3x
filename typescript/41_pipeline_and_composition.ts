type Unary<T> = (value: T) => T;

function pipe<T>(...fns: Unary<T>[]): Unary<T> {
  return (value: T) => fns.reduce((acc, fn) => fn(acc), value);
}

const addOne: Unary<number> = (n) => n + 1;
const double: Unary<number> = (n) => n * 2;
const square: Unary<number> = (n) => n * n;

const pipeline = pipe(addOne, double, square);
console.log("pipeline(1):", pipeline(1));
console.log("pipeline(3):", pipeline(3));

function compose<A, B, C>(f: (b: B) => C, g: (a: A) => B): (a: A) => C {
  return (a) => f(g(a));
}

const trim = (s: string): string => s.trim();
const upper = (s: string): string => s.toUpperCase();
const exclaim = (s: string): string => `${s}!`;

const shout = compose(exclaim, compose(upper, trim));
console.log("shout:", shout("  hello world  "));

const toLength = (s: string): number => s.length;
const pipelineStr = compose(toLength, compose(upper, trim));
console.log("length after trim+upper:", pipelineStr("  typescript  "));

const applyAll = (value: number, fns: Unary<number>[]): number =>
  fns.reduce((acc, fn) => fn(acc), value);

console.log("applyAll:", applyAll(2, [addOne, double, square]));
