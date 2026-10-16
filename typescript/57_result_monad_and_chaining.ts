class Result<T, E> {
  private constructor(
    private readonly isOk: boolean,
    private readonly value?: T,
    private readonly error?: E,
  ) {}

  static ok<T, E = string>(value: T): Result<T, E> {
    return new Result<T, E>(true, value);
  }

  static err<T, E = string>(error: E): Result<T, E> {
    return new Result<T, E>(false, undefined, error);
  }

  map<U>(fn: (value: T) => U): Result<U, E> {
    return this.isOk ? Result.ok<U, E>(fn(this.value as T)) : Result.err<U, E>(this.error as E);
  }

  flatMap<U>(fn: (value: T) => Result<U, E>): Result<U, E> {
    return this.isOk ? fn(this.value as T) : Result.err<U, E>(this.error as E);
  }

  unwrapOr(fallback: T): T {
    return this.isOk ? (this.value as T) : fallback;
  }

  match<U>(onOk: (value: T) => U, onErr: (error: E) => U): U {
    return this.isOk ? onOk(this.value as T) : onErr(this.error as E);
  }

  get ok(): boolean {
    return this.isOk;
  }
}

function parseNumber(text: string): Result<number, string> {
  const value = Number(text);
  return Number.isNaN(value) ? Result.err(`"${text}" is not a number`) : Result.ok(value);
}

function positive(value: number): Result<number, string> {
  return value > 0 ? Result.ok(value) : Result.err(`${value} is not positive`);
}

const inputs = ["5", "-3", "abc"];

for (const input of inputs) {
  const chained = parseNumber(input)
    .map((n) => n * 2)
    .flatMap(positive);

  const message = chained.match(
    (value) => `ok: ${value}`,
    (error) => `error: ${error}`,
  );
  console.log(`${input} -> ${message}`);
}

console.log("unwrapOr:", parseNumber("bad").unwrapOr(0));
console.log("ok flag:", parseNumber("10").ok);
