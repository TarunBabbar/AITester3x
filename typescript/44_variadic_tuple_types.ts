function tuple<T extends unknown[]>(...args: T): T {
  return args;
}

type Coordinates = [latitude: number, longitude: number];
type Pair<A, B> = [first: A, second: B];

function distance(a: Coordinates, b: Coordinates): number {
  const dx = a[0] - b[0];
  const dy = a[1] - b[1];
  return Math.sqrt(dx * dx + dy * dy);
}

function callWithArgs<A extends unknown[], R>(fn: (...args: A) => R, args: A): R {
  return fn(...args);
}

function addAll(...nums: number[]): number {
  return nums.reduce((total, n) => total + n, 0);
}

const delhi: Coordinates = [28.61, 77.2];
const noida: Coordinates = [28.53, 77.39];

console.log("tuple:", tuple("a", 1, true));
console.log("distance:", distance(delhi, noida).toFixed(2));

const pair: Pair<string, number> = ["age", 30];
console.log("pair:", pair[0], "=", pair[1]);

const result = callWithArgs(addAll, [1, 2, 3, 4]);
console.log("callWithArgs:", result);

const [lat, lng] = delhi;
console.log(`lat=${lat}, lng=${lng}`);
