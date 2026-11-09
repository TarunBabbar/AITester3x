function hash32(value: string, seed: number): number {
  let hash = (0x811c9dc5 ^ seed) >>> 0;
  for (let index = 0; index < value.length; index += 1) {
    hash ^= value.charCodeAt(index);
    hash = Math.imul(hash, 0x01000193) >>> 0;
  }
  hash ^= hash >>> 16;
  hash = Math.imul(hash, 0x85ebca6b);
  hash ^= hash >>> 13;
  hash = Math.imul(hash, 0xc2b2ae35);
  hash ^= hash >>> 16;
  return hash >>> 0;
}

class HyperLogLog {
  private readonly registers: Uint8Array;

  constructor(readonly precision = 14) {
    if (!Number.isInteger(precision) || precision < 4 || precision > 16) {
      throw new RangeError("precision must be an integer between 4 and 16");
    }
    this.registers = new Uint8Array(1 << precision);
  }

  get registerCount(): number {
    return this.registers.length;
  }

  /** One standard deviation of the estimate, as a fraction of the true count. */
  static relativeError(precision: number): number {
    return 1.04 / Math.sqrt(1 << precision);
  }

  add(value: string): void {
    const hash = hash32(value, 0x9e3779b9);
    const index = hash >>> (32 - this.precision);
    const remaining = (hash << this.precision) >>> 0;
    const rank = Math.min(Math.clz32(remaining) + 1, 32 - this.precision + 1);
    if (rank > this.registers[index]) {
      this.registers[index] = rank;
    }
  }

  addAll(values: Iterable<string>): void {
    for (const value of values) {
      this.add(value);
    }
  }

  count(): number {
    const m = this.registers.length;
    let sum = 0;
    let zeros = 0;

    for (const register of this.registers) {
      sum += 2 ** -register;
      if (register === 0) {
        zeros += 1;
      }
    }

    const alpha = m === 16 ? 0.673 : m === 32 ? 0.697 : m === 64 ? 0.709 : 0.7213 / (1 + 1.079 / m);
    const raw = (alpha * m * m) / sum;

    // Linear counting is much better while many registers are still empty.
    if (raw <= 2.5 * m && zeros > 0) {
      return m * Math.log(m / zeros);
    }

    return raw;
  }

  merge(other: HyperLogLog): void {
    if (other.precision !== this.precision) {
      throw new Error("can only merge sketches with the same precision");
    }
    for (let index = 0; index < this.registers.length; index += 1) {
      if (other.registers[index] > this.registers[index]) {
        this.registers[index] = other.registers[index];
      }
    }
  }

  clear(): void {
    this.registers.fill(0);
  }

  snapshot(): Uint8Array {
    return this.registers.slice();
  }
}

function check(condition: boolean, message: string): void {
  if (!condition) {
    throw new Error(message);
  }
}

function errorPercent(estimate: number, actual: number): number {
  return Math.abs(estimate - actual) / actual * 100;
}

function main(): void {
  console.log("--- configuration ---");
  const sketch = new HyperLogLog(14);
  console.log(`precision 14 -> ${sketch.registerCount} registers`);
  check(sketch.registerCount === 16_384, "2^14 registers");
  check(HyperLogLog.relativeError(14) < 0.01, "sub-1% standard error");

  const rejects: Array<() => void> = [
    () => new HyperLogLog(3),
    () => new HyperLogLog(17),
    () => new HyperLogLog(1.5),
  ];
  for (const attempt of rejects) {
    let threw = false;
    try {
      attempt();
    } catch (error) {
      threw = true;
      void error;
    }
    check(threw, "invalid precision rejected");
  }
  console.log("invalid precision values rejected");

  console.log("\n--- empty sketch ---");
  check(sketch.count() === 0, "no items means zero");

  console.log("\n--- ten thousand distinct values ---");
  for (let index = 0; index < 10_000; index += 1) {
    sketch.add(`user-${index}`);
  }
  const estimate = sketch.count();
  const error = errorPercent(estimate, 10_000);
  console.log(`estimate ${estimate.toFixed(0)} for 10,000 values (${error.toFixed(2)}% error)`);
  check(error < 5, `estimate should be within 5%, got ${error.toFixed(2)}%`);

  console.log("\n--- duplicates do not inflate the count ---");
  const before = sketch.count();
  for (let index = 0; index < 1_000; index += 1) {
    sketch.add(`user-${index}`);
  }
  const after = sketch.count();
  console.log(`re-adding 1000 members: ${before.toFixed(0)} -> ${after.toFixed(0)}`);
  check(after === before, "the register maxima are unchanged, so the estimate is identical");

  console.log("\n--- small counts use linear counting ---");
  const small = new HyperLogLog(14);
  check(small.count() === 0, "zero for an empty sketch");
  for (let index = 0; index < 10; index += 1) {
    small.add(`small-${index}`);
    const current = small.count();
    check(Math.abs(current - (index + 1)) < 1, `small count should be exact enough, got ${current}`);
  }
  console.log(`10 members estimated as ${small.count().toFixed(2)}`);

  console.log("\n--- merge equals the union ---");
  const left = new HyperLogLog(12);
  const right = new HyperLogLog(12);
  for (let index = 0; index < 5_000; index += 1) {
    left.add(`a-${index}`);
  }
  for (let index = 0; index < 5_000; index += 1) {
    right.add(`b-${index}`);
  }

  left.merge(right);
  const mergedEstimate = left.count();
  const unionError = errorPercent(mergedEstimate, 10_000);
  console.log(`merged estimate ${mergedEstimate.toFixed(0)} (${unionError.toFixed(2)}% error)`);
  check(unionError < 8, "merged estimate should stay within a few percent");

  const direct = new HyperLogLog(12);
  for (let index = 0; index < 5_000; index += 1) {
    direct.add(`a-${index}`);
    direct.add(`b-${index}`);
  }
  console.log("merged and directly built registers equal:", left.snapshot().join(",") === direct.snapshot().join(","));
  check(left.snapshot().join(",") === direct.snapshot().join(","), "union is register-wise maximum");

  console.log("\n--- incompatible merges ---");
  let mergeThrew = false;
  try {
    left.merge(new HyperLogLog(14));
  } catch (error) {
    mergeThrew = true;
    void error;
  }
  check(mergeThrew, "different precisions cannot be merged");

  console.log("\n--- determinism ---");
  const first = new HyperLogLog(10);
  const second = new HyperLogLog(10);
  for (let index = 0; index < 1_000; index += 1) {
    first.add(`value-${index}`);
    second.add(`value-${index}`);
  }
  check(first.count() === second.count(), "same input, same estimate");
  check(first.snapshot().join(",") === second.snapshot().join(","), "same registers");
  console.log("two sketches built from the same values are identical");

  console.log("\n--- clearing ---");
  first.clear();
  check(first.count() === 0, "clear resets the registers");
  check(first.snapshot().every((value) => value === 0), "all registers are zero");
  console.log("cleared sketch estimates zero");

  console.log("\n--- precision versus error ---");
  const samples = Array.from({ length: 8_000 }, (_, index) => `sample-${index}`);
  for (const precision of [4, 6, 8, 10, 12]) {
    const sketchAt = new HyperLogLog(precision);
    sketchAt.addAll(samples);
    const estimateAt = sketchAt.count();
    console.log(
      `  p=${precision}: ${sketchAt.registerCount} registers, estimate ${estimateAt.toFixed(0)}, error ${errorPercent(estimateAt, samples.length).toFixed(1)}%`,
    );
  }

  console.log("\n--- beyond the hash range ---");
  const big = new HyperLogLog(14);
  for (let index = 0; index < 100_000; index += 1) {
    big.add(`big-${index}`);
  }
  const bigEstimate = big.count();
  const bigError = errorPercent(bigEstimate, 100_000);
  console.log(`estimate ${bigEstimate.toFixed(0)} for 100,000 values (${bigError.toFixed(2)}% error)`);
  check(bigError < 5, "large cardinality stays close");

  console.log("\nAll checks passed.");
}

main();
