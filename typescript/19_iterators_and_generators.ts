function* countdown(start: number): Generator<number> {
  for (let i = start; i > 0; i--) {
    yield i;
  }
}

function* fibonacci(limit: number): Generator<number> {
  let [a, b] = [0, 1];
  for (let i = 0; i < limit; i++) {
    yield a;
    [a, b] = [b, a + b];
  }
}

const steps: number[] = [...countdown(3)];
console.log("countdown:", steps.join(", "));

for (const value of countdown(5)) {
  console.log("countdown value:", value);
}

const fibs: number[] = [...fibonacci(8)];
console.log("fibonacci:", fibs.join(", "));

class Range {
  constructor(
    private start: number,
    private end: number,
  ) {}

  *[Symbol.iterator](): Generator<number> {
    for (let i = this.start; i <= this.end; i++) {
      yield i;
    }
  }
}

const range = new Range(1, 5);
console.log("range:", [...range].join(", "));
