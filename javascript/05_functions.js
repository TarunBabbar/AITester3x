function add(a, b) {
  return a + b;
}

function greet(name, greeting = "Hello") {
  return `${greeting}, ${name}!`;
}

const multiply = (a, b) => a * b;

function sumAll(...numbers) {
  return numbers.reduce((total, n) => total + n, 0);
}

const counter = (() => {
  let count = 0;
  return {
    increment: () => ++count,
    value: () => count,
  };
})();

console.log("add:", add(2, 3));
console.log("greet:", greet("Tarun"));
console.log("greet:", greet("Tarun", "Hi"));
console.log("multiply:", multiply(4, 5));
console.log("sumAll:", sumAll(1, 2, 3, 4, 5));

counter.increment();
counter.increment();
console.log("closure counter:", counter.value());

const functions = { add, greet };
console.log("via object:", functions.add(10, 5), "|", functions.greet("Ravi"));
