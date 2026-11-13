type Comparator<K> = (left: K, right: K) => number;
type RandomSource = () => number;

interface SkipNode<K, V> {
  key: K;
  value: V;
  next: Array<SkipNode<K, V> | null>;
}

interface SkipListOptions {
  maxLevel?: number;
  probability?: number;
  random?: RandomSource;
}

class SkipList<K, V> {
  private readonly head: SkipNode<K, V>;
  private readonly maxLevel: number;
  private readonly probability: number;
  private readonly random: RandomSource;
  private currentLevel = 1;
  private itemCount = 0;

  constructor(private readonly compare: Comparator<K>, options: SkipListOptions = {}) {
    const { maxLevel = 16, probability = 0.5, random = Math.random } = options;

    if (!Number.isInteger(maxLevel) || maxLevel < 1) {
      throw new RangeError("maxLevel must be a positive integer");
    }
    if (!(probability > 0 && probability < 1)) {
      throw new RangeError("probability must be between 0 and 1");
    }
    if (typeof random !== "function") {
      throw new TypeError("random must be a function");
    }

    this.maxLevel = maxLevel;
    this.probability = probability;
    this.random = random;
    this.head = {
      key: null as unknown as K,
      value: null as unknown as V,
      next: new Array<SkipNode<K, V> | null>(maxLevel).fill(null),
    };
  }

  get size(): number {
    return this.itemCount;
  }

  get isEmpty(): boolean {
    return this.itemCount === 0;
  }

  height(): number {
    return this.currentLevel;
  }

  set(key: K, value: V): boolean {
    const update = this.predecessors(key);
    const candidate = update[0].next[0];

    if (candidate !== null && this.compare(candidate.key, key) === 0) {
      candidate.value = value;
      return false;
    }

    const level = this.randomLevel();
    if (level > this.currentLevel) {
      for (let index = this.currentLevel; index < level; index += 1) {
        update[index] = this.head;
      }
      this.currentLevel = level;
    }

    const node: SkipNode<K, V> = { key, value, next: new Array<SkipNode<K, V> | null>(level).fill(null) };
    for (let index = 0; index < level; index += 1) {
      node.next[index] = update[index].next[index];
      update[index].next[index] = node;
    }

    this.itemCount += 1;
    return true;
  }

  get(key: K): V | undefined {
    const node = this.find(key);
    return node === null ? undefined : node.value;
  }

  has(key: K): boolean {
    return this.find(key) !== null;
  }

  delete(key: K): boolean {
    const update = this.predecessors(key);
    const target = update[0].next[0];

    if (target === null || this.compare(target.key, key) !== 0) {
      return false;
    }

    for (let index = 0; index < target.next.length; index += 1) {
      if (update[index].next[index] === target) {
        update[index].next[index] = target.next[index];
      }
    }

    this.itemCount -= 1;
    while (this.currentLevel > 1 && this.head.next[this.currentLevel - 1] === null) {
      this.currentLevel -= 1;
    }
    return true;
  }

  first(): K | undefined {
    return this.head.next[0]?.key;
  }

  last(): K | undefined {
    let node = this.head;
    while (node.next[0] !== null) {
      node = node.next[0];
    }
    return node === this.head ? undefined : node.key;
  }

  entries(): Array<[K, V]> {
    const entries: Array<[K, V]> = [];
    let node = this.head.next[0];
    while (node !== null) {
      entries.push([node.key, node.value]);
      node = node.next[0];
    }
    return entries;
  }

  keys(): K[] {
    return this.entries().map(([key]) => key);
  }

  values(): V[] {
    return this.entries().map(([, value]) => value);
  }

  /** Keys present at each level, bottom level first. */
  levels(): K[][] {
    const result: K[][] = [];
    for (let level = 0; level < this.currentLevel; level += 1) {
      const keys: K[] = [];
      let node = this.head.next[level];
      while (node !== null) {
        keys.push(node.key);
        node = node.next[level];
      }
      result.push(keys);
    }
    return result;
  }

  /** Level zero is sorted, every higher level is a subsequence of it, and counts match. */
  validate(): boolean {
    const bottom = this.keys();
    if (bottom.length !== this.itemCount) {
      return false;
    }
    for (let index = 1; index < bottom.length; index += 1) {
      if (this.compare(bottom[index - 1], bottom[index]) >= 0) {
        return false;
      }
    }

    for (let level = 1; level < this.maxLevel; level += 1) {
      let position = 0;
      let node = this.head.next[level];
      while (node !== null) {
        if (node.next.length <= level) {
          return false;
        }
        while (position < bottom.length && this.compare(bottom[position], node.key) !== 0) {
          position += 1;
        }
        if (position === bottom.length) {
          return false;
        }
        position += 1;
        node = node.next[level];
      }
      if (level >= this.currentLevel && this.head.next[level] !== null) {
        return false;
      }
    }

    return true;
  }

  private find(key: K): SkipNode<K, V> | null {
    let node = this.head;
    for (let level = this.currentLevel - 1; level >= 0; level -= 1) {
      while (node.next[level] !== null && this.compare(node.next[level]!.key, key) < 0) {
        node = node.next[level]!;
      }
    }
    const candidate = node.next[0];
    return candidate !== null && this.compare(candidate.key, key) === 0 ? candidate : null;
  }

  private predecessors(key: K): Array<SkipNode<K, V>> {
    const update = new Array<SkipNode<K, V>>(this.maxLevel).fill(this.head);
    let node = this.head;

    for (let level = this.currentLevel - 1; level >= 0; level -= 1) {
      while (node.next[level] !== null && this.compare(node.next[level]!.key, key) < 0) {
        node = node.next[level]!;
      }
      update[level] = node;
    }

    return update;
  }

  private randomLevel(): number {
    let level = 1;
    while (level < this.maxLevel && this.random() < this.probability) {
      level += 1;
    }
    return level;
  }
}

function check(condition: boolean, message: string): void {
  if (!condition) {
    throw new Error(message);
  }
}

function seededRandom(seed: number): RandomSource {
  let state = seed;
  return () => {
    state = (state * 1103515245 + 12345) % 2147483648;
    return state / 2147483648;
  };
}

function main(): void {
  console.log("--- empty list ---");
  const empty = new SkipList<number, string>((left, right) => left - right);
  console.log("size:", empty.size, "| height:", empty.height(), "| first:", empty.first(), "| last:", empty.last());
  check(empty.isEmpty && empty.size === 0, "starts empty");
  check(empty.height() === 1, "a single level");
  check(empty.get(1) === undefined, "no lookups");
  check(!empty.has(1), "no membership");
  check(!empty.delete(1), "nothing to delete");
  check(empty.first() === undefined && empty.last() === undefined, "no endpoints");
  check(empty.validate(), "an empty list is valid");

  console.log("\n--- insert and lookup ---");
  const list = new SkipList<number, string>((left, right) => left - right, {
    random: seededRandom(86),
  });
  const values = [5, 1, 9, 3, 7, 2, 8, 4, 6, 0];
  for (const value of values) {
    check(list.set(value, `v${value}`), `insert ${value}`);
  }
  console.log("size:", list.size, "| height:", list.height(), "| keys:", list.keys().join(", "));
  check(list.size === values.length, "all inserted");
  check(list.keys().join(",") === "0,1,2,3,4,5,6,7,8,9", "sorted iteration");
  check(list.get(4) === "v4", "lookup");
  check(list.get(42) === undefined, "missing key");
  check(list.has(9) && !list.has(42), "membership");
  check(list.first() === 0 && list.last() === 9, "endpoints");
  check(list.validate(), "valid after inserts");

  console.log("\n--- replacing a value ---");
  check(!list.set(4, "updated"), "existing key is not new");
  check(list.get(4) === "updated", "value replaced");
  check(list.size === 10, "size unchanged");

  console.log("\n--- deletion ---");
  check(list.delete(0), "delete the first");
  check(!list.delete(0), "deleting twice reports false");
  check(list.first() === 1, "the first moves in");
  check(list.delete(9), "delete the last");
  check(list.last() === 8, "the last moves in");
  check(list.keys().join(",") === "1,2,3,4,5,6,7,8", "keys after deletions");
  check(list.validate(), "valid after deletions");
  console.log("after deletions:", list.keys().join(", "));

  list.set(0, "zero");
  check(list.keys()[0] === 0, "re-inserting below the minimum works");

  console.log("\n--- string keys ---");
  const words = new SkipList<string, number>((left, right) => left.localeCompare(right), {
    random: seededRandom(7),
  });
  for (const word of ["pear", "apple", "orange", "banana", "kiwi"]) {
    words.set(word, word.length);
  }
  console.log("words:", words.keys().join(", "));
  check(words.keys().join(",") === "apple,banana,kiwi,orange,pear", "locale ordering");
  check(words.values().join(",") === "5,6,4,6,4", "values follow keys");
  check(words.get("kiwi") === 4, "string lookup");

  console.log("\n--- levels ---");
  const layered = new SkipList<number, number>((left, right) => left - right, {
    maxLevel: 8,
    probability: 0.5,
    random: seededRandom(123),
  });
  for (let key = 0; key < 200; key += 1) {
    layered.set(key, key);
  }
  const levels = layered.levels();
  console.log("height:", layered.height(), "| level sizes:", levels.map((keys) => keys.length).join(", "));
  check(levels[0].length === 200, "every key is on level zero");
  check(layered.height() > 1, "promotion happened");
  for (let level = 1; level < levels.length; level += 1) {
    check(levels[level].length <= levels[level - 1].length, "higher levels hold no more keys");
    const bottom = new Set(levels[0]);
    for (const key of levels[level]) {
      check(bottom.has(key), "higher level keys come from level zero");
    }
  }
  check(layered.validate(), "structure is sound");

  console.log("\n--- against a map ---");
  let state = 86;
  const next = (): number => {
    state = (state * 1103515245 + 12345) % 2147483648;
    return state;
  };

  const randomList = new SkipList<number, string>((left, right) => left - right, {
    random: seededRandom(2026),
  });
  const reference = new Map<number, string>();

  for (let step = 0; step < 2_000; step += 1) {
    const key = next() % 300;
    if (next() % 3 === 0) {
      check(randomList.delete(key) === reference.delete(key), `delete mismatch at ${step}`);
    } else {
      const value = `v${step}`;
      const expected = !reference.has(key);
      check(randomList.set(key, value) === expected, `insert mismatch at ${step}`);
      reference.set(key, value);
      check(randomList.get(key) === value, `lookup mismatch at ${step}`);
    }

    check(randomList.size === reference.size, `size at ${step}`);
    if (step % 100 === 0) {
      const expectedKeys = [...reference.keys()].sort((left, right) => left - right);
      check(randomList.keys().join(",") === expectedKeys.join(","), `keys at ${step}`);
      check(randomList.validate(), `invariant at ${step}`);
    }
  }
  console.log("2000 random operations match a Map");

  console.log("\n--- a large list ---");
  const large = new SkipList<number, number>((left, right) => left - right, {
    maxLevel: 24,
    random: seededRandom(99),
  });
  for (let key = 0; key < 10_000; key += 1) {
    large.set(key * 2, key);
  }
  console.log("size:", large.size, "| height:", large.height(), "| first:", large.first(), "| last:", large.last());
  check(large.size === 10_000, "all keys stored");
  check(large.first() === 0 && large.last() === 19_998, "endpoints");
  for (let key = 0; key < 10_000; key += 1) {
    check(large.get(key * 2) === key, `large lookup ${key}`);
  }
  check(large.validate(), "large structure is sound");
  check(large.height() >= 8 && large.height() <= 24, `height ${large.height()} should be logarithmic`);

  console.log("\n--- clearing by deletion ---");
  while (!large.isEmpty) {
    const key = large.first() as number;
    check(large.delete(key), "delete the first");
  }
  check(large.size === 0 && large.height() === 1, "back to an empty one-level list");
  check(large.validate(), "empty again");

  console.log("\n--- validation ---");
  const badOptions: Array<() => void> = [
    () => new SkipList<number, number>((a, b) => a - b, { maxLevel: 0 }),
    () => new SkipList<number, number>((a, b) => a - b, { maxLevel: 1.5 }),
    () => new SkipList<number, number>((a, b) => a - b, { probability: 0 }),
    () => new SkipList<number, number>((a, b) => a - b, { probability: 1 }),
    () => new SkipList<number, number>((a, b) => a - b, { random: 5 as unknown as RandomSource }),
  ];
  for (const attempt of badOptions) {
    let threw = false;
    try {
      attempt();
    } catch (error) {
      threw = error instanceof RangeError || error instanceof TypeError;
    }
    check(threw, "invalid options rejected");
  }
  console.log("bad level, probability, and random source rejected");

  console.log("\nAll checks passed.");
}

main();
