function* counter(start = 1, end = Infinity, step = 1) {
  for (let value = start; value <= end; value += step) {
    yield value;
  }
}

function take(iterable, count) {
  const result = [];
  for (const value of iterable) {
    if (result.length >= count) break;
    result.push(value);
  }
  return result;
}

console.log("first five:", take(counter(), 5).join(", "));
console.log("even numbers:", take(counter(2, 10, 2)).join(", "));

function* names() {
  yield "Tarun";
  yield "Asha";
  yield "Ravi";
}

console.log("generated names:", [...names()].join(", "));

function* letters() {
  yield "a";
  yield* names();
  yield "z";
}

console.log("delegated:", [...letters()].join(", "));

const range = {
  from: 1,
  to: 4,
  [Symbol.iterator]() {
    let current = this.from;
    const last = this.to;
    return {
      next() {
        if (current <= last) {
          return { value: current++, done: false };
        }
        return { value: undefined, done: true };
      },
    };
  },
};

console.log("custom range:", [...range].join(", "));
console.log("sum of range:", [...range].reduce((total, n) => total + n, 0));

function* fibonacci() {
  let [prev, curr] = [0, 1];
  while (true) {
    yield curr;
    [prev, curr] = [curr, prev + curr];
  }
}

console.log("fibonacci:", take(fibonacci(), 8).join(", "));

function* echo() {
  let received = yield "ready";
  while (true) {
    received = yield `got: ${received}`;
  }
}

const talk = echo();
console.log("echo 1:", talk.next().value);
console.log("echo 2:", talk.next("hello").value);
console.log("echo 3:", talk.next("world").value);
