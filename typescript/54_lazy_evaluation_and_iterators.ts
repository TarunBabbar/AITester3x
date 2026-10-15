function* naturals(): Generator<number> {
  let n = 1;
  while (true) {
    yield n++;
  }
}

function* take<T>(source: Iterable<T>, count: number): Generator<T> {
  let index = 0;
  for (const value of source) {
    if (index >= count) {
      return;
    }
    yield value;
    index++;
  }
}

function* map<T, R>(source: Iterable<T>, fn: (value: T) => R): Generator<R> {
  for (const value of source) {
    yield fn(value);
  }
}

function* filter<T>(source: Iterable<T>, predicate: (value: T) => boolean): Generator<T> {
  for (const value of source) {
    if (predicate(value)) {
      yield value;
    }
  }
}

const evens = filter(naturals(), (n) => n % 2 === 0);
const squared = map(evens, (n) => n * n);
const firstFive = [...take(squared, 5)];
console.log("first five even squares:", firstFive.join(", "));

const stepped = take(map(naturals(), (n) => n * 10), 4);
console.log("stepped:", [...stepped].join(", "));

function* range(start: number, end: number): Generator<number> {
  for (let i = start; i < end; i++) {
    yield i;
  }
}

const pipeline = take(filter(map(range(1, 21), (n) => n * 3), (n) => n % 2 === 1), 4);
console.log("lazy pipeline:", [...pipeline].join(", "));
console.log("infinite generator untouched:", naturals().next().value);
