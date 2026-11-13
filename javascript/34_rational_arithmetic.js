const assert = require("node:assert/strict");

function absBigInt(value) {
  return value < 0n ? -value : value;
}

function gcd(left, right) {
  let a = absBigInt(left);
  let b = absBigInt(right);
  while (b !== 0n) {
    const remainder = a % b;
    a = b;
    b = remainder;
  }
  return a;
}

function toBigInt(value) {
  if (typeof value === "bigint") {
    return value;
  }
  if (typeof value === "number") {
    if (!Number.isFinite(value)) {
      throw new RangeError("numbers must be finite");
    }
    if (!Number.isInteger(value)) {
      throw new TypeError("use Rational.from for fractional numbers");
    }
    return BigInt(value);
  }
  if (typeof value === "string" && /^-?\d+$/.test(value.trim())) {
    return BigInt(value.trim());
  }
  throw new TypeError(`cannot convert ${String(value)} to a whole number`);
}

class Rational {
  #numerator;
  #denominator;

  constructor(numerator, denominator = 1n) {
    let n = toBigInt(numerator);
    let d = toBigInt(denominator);

    if (d === 0n) {
      throw new RangeError("denominator must not be zero");
    }
    if (d < 0n) {
      n = -n;
      d = -d;
    }

    const divisor = gcd(n, d) || 1n;
    this.#numerator = n / divisor;
    this.#denominator = d / divisor;
  }

  static from(value) {
    if (value instanceof Rational) {
      return value;
    }
    if (typeof value === "number") {
      if (!Number.isFinite(value)) {
        throw new RangeError("numbers must be finite");
      }
      if (Number.isInteger(value)) {
        return new Rational(BigInt(value));
      }
      return Rational.#parseDecimal(String(value));
    }
    if (typeof value === "string") {
      const trimmed = value.trim();
      const parts = trimmed.split("/");
      if (parts.length === 2) {
        return new Rational(parts[0].trim(), parts[1].trim());
      }
      if (parts.length > 2) {
        throw new TypeError(`invalid fraction ${value}`);
      }
      if (trimmed.includes(".")) {
        return Rational.#parseDecimal(trimmed);
      }
      return new Rational(trimmed);
    }
    if (typeof value === "bigint") {
      return new Rational(value);
    }
    throw new TypeError(`cannot build a rational from ${String(value)}`);
  }

  static #parseDecimal(text) {
    const match = /^(-?)(\d+)(?:\.(\d+))?$/.exec(text);
    if (match === null) {
      throw new TypeError(`unsupported decimal ${text}`);
    }
    const sign = match[1] === "-" ? -1n : 1n;
    const whole = BigInt(match[2]);
    const fraction = match[3] ?? "";
    const denominator = 10n ** BigInt(fraction.length);
    const numerator = sign * (whole * denominator + BigInt(fraction === "" ? "0" : fraction));
    return new Rational(numerator, denominator);
  }

  get numerator() {
    return this.#numerator;
  }

  get denominator() {
    return this.#denominator;
  }

  get isZero() {
    return this.#numerator === 0n;
  }

  add(other) {
    const right = Rational.from(other);
    return new Rational(
      this.#numerator * right.denominator + right.numerator * this.#denominator,
      this.#denominator * right.denominator,
    );
  }

  subtract(other) {
    const right = Rational.from(other);
    return new Rational(
      this.#numerator * right.denominator - right.numerator * this.#denominator,
      this.#denominator * right.denominator,
    );
  }

  multiply(other) {
    const right = Rational.from(other);
    return new Rational(this.#numerator * right.numerator, this.#denominator * right.denominator);
  }

  divide(other) {
    const right = Rational.from(other);
    if (right.isZero) {
      throw new RangeError("cannot divide by zero");
    }
    return new Rational(this.#numerator * right.denominator, this.#denominator * right.numerator);
  }

  negate() {
    return new Rational(-this.#numerator, this.#denominator);
  }

  abs() {
    return new Rational(absBigInt(this.#numerator), this.#denominator);
  }

  inverse() {
    if (this.isZero) {
      throw new RangeError("zero has no inverse");
    }
    return new Rational(this.#denominator, this.#numerator);
  }

  compare(other) {
    const right = Rational.from(other);
    const difference = this.#numerator * right.denominator - right.numerator * this.#denominator;
    if (difference < 0n) {
      return -1;
    }
    return difference > 0n ? 1 : 0;
  }

  equals(other) {
    return this.compare(other) === 0;
  }

  toNumber() {
    return Number(this.#numerator) / Number(this.#denominator);
  }

  toDecimal(digits = 4) {
    if (!Number.isInteger(digits) || digits < 0) {
      throw new RangeError("digits must be a non-negative integer");
    }

    const negative = this.#numerator < 0n;
    const numerator = absBigInt(this.#numerator);
    const scale = 10n ** BigInt(digits);
    const scaled = numerator * scale;
    let quotient = scaled / this.#denominator;
    const remainder = scaled % this.#denominator;
    if (remainder * 2n >= this.#denominator) {
      quotient += 1n;
    }

    const text = quotient.toString().padStart(digits + 1, "0");
    const wholePart = text.slice(0, text.length - digits);
    const fractionPart = digits === 0 ? "" : `.${text.slice(text.length - digits)}`;
    return `${negative ? "-" : ""}${wholePart}${fractionPart}`;
  }

  toString() {
    return this.#denominator === 1n
      ? `${this.#numerator}`
      : `${this.#numerator}/${this.#denominator}`;
  }
}

function main() {
  console.log("--- normalisation ---");
  assert.equal(new Rational(2, 4).toString(), "1/2");
  assert.equal(new Rational(-2, -4).toString(), "1/2");
  assert.equal(new Rational(2, -4).toString(), "-1/2");
  assert.equal(new Rational(-2, 4).toString(), "-1/2");
  assert.equal(new Rational(0, 5).toString(), "0");
  assert.equal(new Rational(6, 3).toString(), "2");
  assert.equal(new Rational(7n).toString(), "7");
  console.log("fractions reduce and the sign lives on the numerator");

  console.log("\n--- arithmetic ---");
  const third = new Rational(1, 3);
  const sixth = new Rational(1, 6);
  assert.equal(third.add(sixth).toString(), "1/2");
  assert.equal(third.subtract(sixth).toString(), "1/6");
  assert.equal(new Rational(2, 3).multiply(new Rational(3, 4)).toString(), "1/2");
  assert.equal(new Rational(1, 2).divide(new Rational(3, 4)).toString(), "2/3");
  assert.equal(third.add(1).toString(), "4/3");
  assert.equal(third.add("1/3").toString(), "2/3");
  assert.equal(third.add(0n).toString(), "1/3");
  console.log("1/3 + 1/6 =", third.add(sixth).toString());
  console.log("(1/2) / (3/4) =", new Rational(1, 2).divide(new Rational(3, 4)).toString());

  console.log("\n--- negation, absolute value, and inverse ---");
  assert.equal(third.negate().toString(), "-1/3");
  assert.equal(third.negate().abs().toString(), "1/3");
  assert.equal(third.inverse().toString(), "3");
  assert.equal(new Rational(-2, 3).inverse().toString(), "-3/2");
  assert.throws(() => new Rational(0).inverse(), RangeError);
  assert.throws(() => third.divide(0), RangeError);
  console.log("sign handling and zero guards work");

  console.log("\n--- fractions from decimals ---");
  assert.equal(Rational.from("1.25").toString(), "5/4");
  assert.equal(Rational.from("-0.5").toString(), "-1/2");
  assert.equal(Rational.from("3.1415").toString(), "6283/2000");
  assert.equal(Rational.from(0.1).toString(), "1/10");
  assert.equal(Rational.from("1/3").toString(), "1/3");
  assert.equal(Rational.from("7").toString(), "7");
  assert.equal(Rational.from(7n).toString(), "7");
  assert.equal(Rational.from(third), third);
  console.log("decimals are read as written, not as binary floating point");

  console.log("\n--- comparison ---");
  assert.equal(third.compare(new Rational(1, 2)), -1);
  assert.equal(new Rational(2, 4).compare(new Rational(1, 2)), 0);
  assert.equal(new Rational(-1, 3).compare(new Rational(1, 3)), -1);
  assert.equal(third.equals(0.3333333333333333), false);
  assert.equal(Rational.from(0.5).equals("1/2"), true);
  console.log("comparisons are exact cross-multiplications");

  console.log("\n--- printing decimals ---");
  assert.equal(new Rational(1, 3).toDecimal(4), "0.3333");
  assert.equal(new Rational(2, 3).toDecimal(4), "0.6667");
  assert.equal(new Rational(1, 2).toDecimal(1), "0.5");
  assert.equal(new Rational(22, 7).toDecimal(5), "3.14286");
  assert.equal(new Rational(-22, 7).toDecimal(5), "-3.14286");
  assert.equal(new Rational(7, 2).toDecimal(0), "4");
  assert.equal(new Rational(1, 3).toDecimal(0), "0");
  console.log("1/3 ->", new Rational(1, 3).toDecimal(6), "| 22/7 ->", new Rational(22, 7).toDecimal(5));

  console.log("\n--- floating point comparison ---");
  const tenth = Rational.from("0.1");
  const sum = tenth.add(tenth).add(tenth);
  console.log("0.1 + 0.1 + 0.1 =", sum.toString(), "| as a number:", sum.toNumber());
  assert.equal(sum.toString(), "3/10");
  assert.notEqual(0.1 + 0.1 + 0.1, 0.3);
  assert.equal(sum.equals("0.3"), true);

  console.log("\n--- large values ---");
  const tiny = new Rational(1n, 10n ** 30n);
  const large = new Rational(10n ** 30n, 1n);
  assert.equal(tiny.multiply(large).toString(), "1");
  assert.equal(large.multiply(large).toString(), (10n ** 60n).toString());
  console.log("bigint numerators and denominators stay exact");

  console.log("\n--- validation ---");
  const invalid = [
    () => new Rational(1, 0),
    () => new Rational(1.5, 2),
    () => new Rational(Number.NaN),
    () => new Rational(Number.POSITIVE_INFINITY),
    () => new Rational("abc"),
    () => Rational.from("1/2/3"),
    () => Rational.from("1e3"),
    () => Rational.from({}),
  ];
  for (const attempt of invalid) {
    assert.throws(attempt, Error);
  }
  console.log("zero denominators, non-integers, and bad strings rejected");

  console.log("\n--- arithmetic against floating point ---");
  let state = 34;
  const next = () => {
    state = (state * 1103515245 + 12345) % 2147483648;
    return state;
  };

  for (let trial = 0; trial < 200; trial += 1) {
    const a = new Rational(next() % 200 - 100, 1 + (next() % 50));
    const b = new Rational(next() % 200 - 100, 1 + (next() % 50));

    const sum = a.add(b);
    assert.equal(sum.subtract(b).toString(), a.toString());
    assert.equal(sum.subtract(a).toString(), b.toString());
    if (!b.isZero) {
      assert.equal(a.multiply(b).divide(b).toString(), a.toString());
    }
    assert.ok(Math.abs(sum.toNumber() - (a.toNumber() + b.toNumber())) < 1e-12);
    assert.equal(a.compare(b) === 0, a.equals(b));
  }
  console.log("200 random pairs satisfy the algebraic identities");

  console.log("\nAll checks passed.");
}

main();
