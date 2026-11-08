function hash32(value: string, seed: number): number {
  let hash = (0x811c9dc5 ^ seed) >>> 0;
  for (let index = 0; index < value.length; index += 1) {
    hash ^= value.charCodeAt(index);
    hash = Math.imul(hash, 0x01000193) >>> 0;
  }
  return hash >>> 0;
}

class BloomFilter {
  private readonly bits: Uint8Array;
  private readonly bitCount: number;
  private readonly hashCount: number;
  private setBits = 0;
  private added = 0;

  static withExpectedItems(items: number, falsePositiveRate = 0.01): BloomFilter {
    if (items <= 0 || falsePositiveRate <= 0 || falsePositiveRate >= 1) {
      throw new RangeError("items must be positive and the rate between 0 and 1");
    }
    const ln2 = Math.LN2;
    const bits = Math.ceil((-items * Math.log(falsePositiveRate)) / (ln2 * ln2));
    const hashes = Math.max(1, Math.round((bits / items) * ln2));
    return new BloomFilter(bits, hashes);
  }

  constructor(bitCount: number, hashCount: number) {
    if (!Number.isInteger(bitCount) || bitCount <= 0) {
      throw new RangeError("bit count must be a positive integer");
    }
    if (!Number.isInteger(hashCount) || hashCount <= 0) {
      throw new RangeError("hash count must be a positive integer");
    }
    this.bitCount = bitCount;
    this.hashCount = hashCount;
    this.bits = new Uint8Array(bitCount);
  }

  get size(): number {
    return this.bitCount;
  }

  get hashes(): number {
    return this.hashCount;
  }

  get itemCount(): number {
    return this.added;
  }

  get setBitCount(): number {
    return this.setBits;
  }

  get fillRatio(): number {
    return this.setBits / this.bitCount;
  }

  /** Returns true when at least one bit was newly set. */
  add(value: string): boolean {
    let changed = false;
    for (const index of this.indexes(value)) {
      if (this.bits[index] === 0) {
        this.bits[index] = 1;
        this.setBits += 1;
        changed = true;
      }
    }
    this.added += 1;
    return changed;
  }

  has(value: string): boolean {
    for (const index of this.indexes(value)) {
      if (this.bits[index] === 0) {
        return false;
      }
    }
    return true;
  }

  clear(): void {
    this.bits.fill(0);
    this.setBits = 0;
    this.added = 0;
  }

  /** Expected false positive rate for the number of values added so far. */
  get estimatedFalsePositiveRate(): number {
    if (this.added === 0) {
      return 0;
    }
    const exponent = (-this.hashCount * this.added) / this.bitCount;
    return (1 - Math.exp(exponent)) ** this.hashCount;
  }

  private indexes(value: string): number[] {
    const first = hash32(value, 0x9e3779b9);
    const second = hash32(value, 0x85ebca6b) | 1;
    const positions: number[] = [];

    for (let round = 0; round < this.hashCount; round += 1) {
      const combined = (first + round * second) % this.bitCount;
      positions.push(combined < 0 ? combined + this.bitCount : combined);
    }
    return positions;
  }
}

function check(condition: boolean, message: string): void {
  if (!condition) {
    throw new Error(message);
  }
}

function main(): void {
  console.log("--- sizing ---");
  const sized = BloomFilter.withExpectedItems(10_000, 0.01);
  console.log(`10,000 items at 1%: ${sized.size} bits, ${sized.hashes} hashes`);
  check(sized.size > 0 && sized.hashes > 0, "sizing produces usable numbers");
  check(sized.size / 10_000 > 9, "roughly ten bits per item at 1%");

  const rejects: Array<() => void> = [
    () => new BloomFilter(0, 3),
    () => new BloomFilter(10, 0),
    () => new BloomFilter(1.5, 2),
    () => BloomFilter.withExpectedItems(0, 0.01),
    () => BloomFilter.withExpectedItems(10, 0),
    () => BloomFilter.withExpectedItems(10, 1),
  ];
  for (const attempt of rejects) {
    let threw = false;
    try {
      attempt();
    } catch (error) {
      threw = true;
      void error;
    }
    check(threw, "invalid configuration should be rejected");
  }
  console.log("invalid sizes and rates rejected");

  console.log("\n--- no false negatives ---");
  const filter = BloomFilter.withExpectedItems(10_000, 0.01);
  const members: string[] = [];
  for (let index = 0; index < 10_000; index += 1) {
    const value = `user-${index}`;
    members.push(value);
    filter.add(value);
  }
  for (const value of members) {
    check(filter.has(value), `lost member ${value}`);
  }
  console.log(`${members.length} members all reported present`);
  console.log(`fill ratio: ${(filter.fillRatio * 100).toFixed(1)}%`);

  console.log("\n--- false positives ---");
  let falsePositives = 0;
  const nonMembers = 10_000;
  for (let index = 0; index < nonMembers; index += 1) {
    if (filter.has(`absent-${index}`)) {
      falsePositives += 1;
    }
  }
  const measured = falsePositives / nonMembers;
  const estimate = filter.estimatedFalsePositiveRate;
  console.log(`measured ${(measured * 100).toFixed(2)}%, estimated ${(estimate * 100).toFixed(2)}%`);
  check(measured < 0.05, `false positive rate too high: ${measured}`);
  check(measured <= estimate * 3 + 0.005, "measured rate should be near the estimate");

  console.log("\n--- fill ratio matches the bit array ---");
  check(filter.setBitCount > 0, "some bits are set");
  check(filter.fillRatio > 0 && filter.fillRatio < 1, "a loaded filter is partially full");
  console.log(`set bits: ${filter.setBitCount} of ${filter.size}`);

  console.log("\n--- duplicate inserts do not lose members ---");
  const distinctBits = filter.setBitCount;
  const before = filter.itemCount;
  for (const value of members.slice(0, 100)) {
    filter.add(value);
  }
  check(filter.itemCount === before + 100, "inserts are counted");
  check(filter.setBitCount >= distinctBits, "bits only ever get set");
  for (const value of members) {
    check(filter.has(value), "still present after re-inserts");
  }
  console.log("re-inserting 100 members changed no membership answers");

  console.log("\n--- determinism ---");
  const left = BloomFilter.withExpectedItems(500, 0.05);
  const right = BloomFilter.withExpectedItems(500, 0.05);
  for (let index = 0; index < 500; index += 1) {
    left.add(`key-${index}`);
    right.add(`key-${index}`);
  }
  check(left.fillRatio === right.fillRatio, "two identical filters have identical fill");
  for (let index = 0; index < 100; index += 1) {
    check(left.has(`probe-${index}`) === right.has(`probe-${index}`), "identical answers");
  }
  console.log("two filters built from the same values agree on every probe");

  console.log("\n--- clearing ---");
  filter.clear();
  check(filter.fillRatio === 0 && filter.itemCount === 0, "clear empties the filter");
  check(!filter.has(members[0]), "no members remain after clear");
  console.log("cleared filter rejects everything");

  console.log("\n--- a saturated filter ---");
  const tiny = new BloomFilter(16, 4);
  for (let index = 0; index < 200; index += 1) {
    tiny.add(`value-${index}`);
  }
  let tinyFalsePositives = 0;
  for (let index = 0; index < 100; index += 1) {
    if (tiny.has(`missing-${index}`)) {
      tinyFalsePositives += 1;
    }
  }
  console.log(`16-bit filter: fill ${(tiny.fillRatio * 100).toFixed(0)}%, false positives ${tinyFalsePositives}%`);
  check(tiny.fillRatio === 1, "tiny filter is completely full");
  check(tinyFalsePositives > 50, "a saturated filter says yes to almost everything");

  console.log("\n--- empty filter ---");
  const empty = new BloomFilter(64, 3);
  check(!empty.has("anything"), "an empty filter contains nothing");
  check(empty.estimatedFalsePositiveRate === 0, "with no values the estimate is zero");
  check(empty.add("first"), "the first add sets bits");
  check(!empty.add("first"), "re-adding the same value changes nothing");
  console.log("empty filter behaviour is sane");

  console.log("\nAll checks passed.");
}

main();
