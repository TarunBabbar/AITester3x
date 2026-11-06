class MinHeap<T> {
  private items: T[] = [];

  constructor(private readonly compare: (left: T, right: T) => number) {}

  get size(): number {
    return this.items.length;
  }

  push(item: T): void {
    this.items.push(item);
    let index = this.items.length - 1;
    while (index > 0) {
      const parent = (index - 1) >> 1;
      if (this.compare(this.items[index], this.items[parent]) >= 0) {
        break;
      }
      [this.items[index], this.items[parent]] = [this.items[parent], this.items[index]];
      index = parent;
    }
  }

  pop(): T | undefined {
    if (this.items.length === 0) {
      return undefined;
    }
    const top = this.items[0];
    const last = this.items.pop();
    if (this.items.length > 0 && last !== undefined) {
      this.items[0] = last;
      let index = 0;
      while (true) {
        const left = index * 2 + 1;
        const right = left + 1;
        let smallest = index;
        if (left < this.items.length && this.compare(this.items[left], this.items[smallest]) < 0) {
          smallest = left;
        }
        if (right < this.items.length && this.compare(this.items[right], this.items[smallest]) < 0) {
          smallest = right;
        }
        if (smallest === index) {
          break;
        }
        [this.items[index], this.items[smallest]] = [this.items[smallest], this.items[index]];
        index = smallest;
      }
    }
    return top;
  }
}

interface HuffmanNode {
  weight: number;
  order: number;
  symbol?: string;
  left?: HuffmanNode;
  right?: HuffmanNode;
}

function frequencies(text: string): Map<string, number> {
  const counts = new Map<string, number>();
  for (const symbol of text) {
    counts.set(symbol, (counts.get(symbol) ?? 0) + 1);
  }
  return counts;
}

function buildTree(counts: Map<string, number>): HuffmanNode {
  if (counts.size === 0) {
    throw new Error("cannot build a tree without symbols");
  }

  let order = 0;
  const heap = new MinHeap<HuffmanNode>((left, right) => left.weight - right.weight || left.order - right.order);

  for (const [symbol, weight] of counts) {
    if (weight <= 0) {
      throw new Error(`weight for ${symbol} must be positive`);
    }
    heap.push({ weight, symbol, order: order++ });
  }

  while (heap.size > 1) {
    const left = heap.pop();
    const right = heap.pop();
    if (!left || !right) {
      break;
    }
    heap.push({ weight: left.weight + right.weight, left, right, order: order++ });
  }

  const root = heap.pop();
  if (!root) {
    throw new Error("failed to build a tree");
  }
  return root;
}

function buildCodes(root: HuffmanNode): Map<string, string> {
  const codes = new Map<string, string>();

  const visit = (node: HuffmanNode, prefix: string): void => {
    if (node.symbol !== undefined) {
      codes.set(node.symbol, prefix === "" ? "0" : prefix);
      return;
    }
    if (node.left) {
      visit(node.left, prefix + "0");
    }
    if (node.right) {
      visit(node.right, prefix + "1");
    }
  };

  visit(root, "");
  return codes;
}

function encode(text: string, codes: Map<string, string>): string {
  let bits = "";
  for (const symbol of text) {
    const code = codes.get(symbol);
    if (code === undefined) {
      throw new Error(`no code for symbol ${JSON.stringify(symbol)}`);
    }
    bits += code;
  }
  return bits;
}

function decode(bits: string, root: HuffmanNode): string {
  let text = "";
  let node = root;

  if (root.symbol !== undefined) {
    for (const bit of bits) {
      if (bit !== "0") {
        throw new Error(`invalid bit ${JSON.stringify(bit)}`);
      }
    }
    return root.symbol.repeat(bits.length);
  }

  for (const bit of bits) {
    if (bit !== "0" && bit !== "1") {
      throw new Error(`invalid bit ${JSON.stringify(bit)}`);
    }
    const next = bit === "0" ? node.left : node.right;
    if (!next) {
      throw new Error("truncated code sequence");
    }
    node = next;
    if (node.symbol !== undefined) {
      text += node.symbol;
      node = root;
    }
  }

  if (node !== root) {
    throw new Error("the bits end in the middle of a code");
  }
  return text;
}

function isPrefixFree(codes: Map<string, string>): boolean {
  const entries = [...codes.entries()].sort((left, right) => left[1].length - right[1].length);
  for (let i = 0; i < entries.length; i += 1) {
    for (let j = 0; j < i; j += 1) {
      if (entries[i][1].startsWith(entries[j][1])) {
        return false;
      }
    }
  }
  return true;
}

function entropy(counts: Map<string, number>): number {
  const total = [...counts.values()].reduce((sum, count) => sum + count, 0);
  let result = 0;
  for (const count of counts.values()) {
    const probability = count / total;
    result -= probability * Math.log2(probability);
  }
  return result;
}

function averageCodeLength(counts: Map<string, number>, codes: Map<string, string>): number {
  const total = [...counts.values()].reduce((sum, count) => sum + count, 0);
  let bits = 0;
  for (const [symbol, count] of counts) {
    bits += count * (codes.get(symbol) ?? "").length;
  }
  return bits / total;
}

function check(condition: boolean, message: string): void {
  if (!condition) {
    throw new Error(message);
  }
}

function main(): void {
  console.log("--- a classic example ---");
  const text = "abracadabra";
  const counts = frequencies(text);
  const root = buildTree(counts);
  const codes = buildCodes(root);

  console.log("frequencies:", [...counts.entries()].map(([symbol, count]) => `${symbol}:${count}`).join(" "));
  for (const [symbol, code] of [...codes.entries()].sort()) {
    console.log(`  ${symbol} -> ${code}`);
  }

  const bits = encode(text, codes);
  const restored = decode(bits, root);
  console.log(`encoded: ${bits} (${bits.length} bits)`);
  console.log("round trip:", restored);
  check(restored === text, "decoding should restore the text");
  check(isPrefixFree(codes), "huffman codes are prefix free");
  check(bits.length < text.length * 8, "the encoding should be smaller than 8 bits per symbol");

  console.log("\n--- code lengths for a skewed distribution ---");
  const skewed = frequencies("aaaaabcde");
  const skewedCodes = buildCodes(buildTree(skewed));
  const lengths = [...skewedCodes.entries()].sort().map(([symbol, code]) => `${symbol}:${code.length}`).join(" ");
  console.log("lengths:", lengths);
  check(skewedCodes.get("a")?.length === 1, "the most frequent symbol gets one bit");
  check((skewedCodes.get("d")?.length ?? 0) >= 3, "rare symbols get longer codes");
  check(isPrefixFree(skewedCodes), "still prefix free");

  console.log("\n--- optimality bounds ---");
  const paragraph =
    "the quick brown fox jumps over the lazy dog. " +
    "pack my box with five dozen liquor jugs. " +
    "the five boxing wizards jump quickly.";
  const paragraphCounts = frequencies(paragraph);
  const paragraphCodes = buildCodes(buildTree(paragraphCounts));
  const bitsPerSymbol = averageCodeLength(paragraphCounts, paragraphCodes);
  const entropyBits = entropy(paragraphCounts);
  console.log("symbols:", paragraphCounts.size);
  console.log("entropy:", entropyBits.toFixed(4), "bits per symbol");
  console.log("average code length:", bitsPerSymbol.toFixed(4), "bits per symbol");
  check(bitsPerSymbol >= entropyBits - 1e-9, "a code cannot beat the entropy");
  check(bitsPerSymbol < entropyBits + 1, "and huffman is within one bit of it");

  const paragraphBits = encode(paragraph, paragraphCodes);
  check(decode(paragraphBits, buildTree(paragraphCounts)) === paragraph, "paragraph round trip");
  console.log(
    `encoded size: ${paragraphBits.length} bits vs ${paragraph.length * 8} bits raw (${(
      (1 - paragraphBits.length / (paragraph.length * 8)) *
      100
    ).toFixed(1)}% smaller)`,
  );

  console.log("\n--- single symbol ---");
  const singleCounts = frequencies("aaaa");
  const singleRoot = buildTree(singleCounts);
  const singleCodes = buildCodes(singleRoot);
  const singleBits = encode("aaaa", singleCodes);
  console.log("code:", singleCodes.get("a"), "| bits:", singleBits, "| decoded:", decode(singleBits, singleRoot));
  check(singleCodes.get("a") === "0", "a lone symbol still needs a bit");
  check(decode(singleBits, singleRoot) === "aaaa", "single symbol round trip");

  console.log("\n--- invalid input ---");
  const longerCode = [...codes.values()].find((code) => code.length > 1) ?? "0";
  const invalidChecks: Array<() => void> = [
    () => buildTree(new Map()),
    () => encode("z", codes),
    () => decode("2", root),
    () => decode(longerCode.slice(0, -1), root),
  ];
  for (const attempt of invalidChecks) {
    try {
      attempt();
      throw new Error("expected a failure");
    } catch (error) {
      console.log(`  rejected: ${(error as Error).message}`);
    }
  }

  console.log("\n--- randomised round trips ---");
  let state = 12345;
  const next = (): number => {
    state = (state * 1103515245 + 12345) % 2147483648;
    return state;
  };
  const alphabet = "abcdefgh";
  let checked = 0;
  for (let trial = 0; trial < 200; trial += 1) {
    const length = 1 + (next() % 120);
    let sample = "";
    for (let index = 0; index < length; index += 1) {
      sample += alphabet[next() % alphabet.length];
    }
    const sampleCodes = buildCodes(buildTree(frequencies(sample)));
    const sampleBits = encode(sample, sampleCodes);
    check(decode(sampleBits, buildTree(frequencies(sample))) === sample, "random round trip");
    check(isPrefixFree(sampleCodes), "random codes are prefix free");
    check(sampleBits.length <= length * 3, "eight symbols encode in at most three bits");
    checked += 1;
  }
  console.log(`${checked} random strings encoded and decoded`);

  console.log("\nAll checks passed.");
}

main();
