function add(a: number, b: number): number {
  return a + b;
}

function greet(name: string, greeting: string = "Hello"): string {
  return `${greeting}, ${name}!`;
}

const multiply = (a: number, b: number): number => a * b;

function sumAll(...numbers: number[]): number {
  return numbers.reduce((total, n) => total + n, 0);
}

console.log("add:", add(2, 3));
console.log("greet:", greet("Tarun"));
console.log("greet:", greet("Tarun", "Hi"));
console.log("multiply:", multiply(4, 5));
console.log("sumAll:", sumAll(1, 2, 3, 4, 5));
