type EditCosts = {
  insert: number;
  delete: number;
  substitute: number;
};

type Edit =
  | { op: "match"; from: string; to: string }
  | { op: "substitute"; from: string; to: string }
  | { op: "insert"; to: string }
  | { op: "delete"; from: string };

const UNIT_COSTS: EditCosts = { insert: 1, delete: 1, substitute: 1 };

function check(condition: boolean, message: string): void {
  if (!condition) {
    throw new Error(message);
  }
}

function symbols(text: string): string[] {
  return Array.from(text);
}

function normaliseCosts(costs: Partial<EditCosts> = {}): EditCosts {
  const merged: EditCosts = {
    insert: costs.insert ?? UNIT_COSTS.insert,
    delete: costs.delete ?? UNIT_COSTS.delete,
    substitute: costs.substitute ?? UNIT_COSTS.substitute,
  };

  for (const [name, value] of Object.entries(merged)) {
    if (!Number.isInteger(value) || value < 1) {
      throw new RangeError(`${name} cost must be a positive integer`);
    }
  }

  return merged;
}

function table(left: string, right: string, costs: Partial<EditCosts> = {}): number[][] {
  const { insert, delete: remove, substitute } = normaliseCosts(costs);
  const a = symbols(left);
  const b = symbols(right);
  const values: number[][] = Array.from({ length: a.length + 1 }, () =>
    new Array<number>(b.length + 1).fill(0),
  );

  for (let i = 0; i <= a.length; i += 1) {
    values[i][0] = i * remove;
  }
  for (let j = 0; j <= b.length; j += 1) {
    values[0][j] = j * insert;
  }

  for (let i = 1; i <= a.length; i += 1) {
    for (let j = 1; j <= b.length; j += 1) {
      const substitution = values[i - 1][j - 1] + (a[i - 1] === b[j - 1] ? 0 : substitute);
      values[i][j] = Math.min(
        values[i - 1][j] + remove,
        values[i][j - 1] + insert,
        substitution,
      );
    }
  }

  return values;
}

function distance(left: string, right: string, costs: Partial<EditCosts> = {}): number {
  const { insert, delete: remove, substitute } = normaliseCosts(costs);
  const a = symbols(left);
  const b = symbols(right);
  let previous = Array.from({ length: b.length + 1 }, (_, index) => index * insert);

  for (let i = 1; i <= a.length; i += 1) {
    const current = new Array<number>(b.length + 1).fill(0);
    current[0] = i * remove;
    for (let j = 1; j <= b.length; j += 1) {
      const substitution = previous[j - 1] + (a[i - 1] === b[j - 1] ? 0 : substitute);
      current[j] = Math.min(previous[j] + remove, current[j - 1] + insert, substitution);
    }
    previous = current;
  }

  return previous[b.length];
}

function editScript(left: string, right: string, costs: Partial<EditCosts> = {}): Edit[] {
  const { insert, delete: remove, substitute } = normaliseCosts(costs);
  const a = symbols(left);
  const b = symbols(right);
  const values = table(left, right, costs);
  const edits: Edit[] = [];
  let i = a.length;
  let j = b.length;

  while (i > 0 || j > 0) {
    if (i > 0 && j > 0 && a[i - 1] === b[j - 1] && values[i][j] === values[i - 1][j - 1]) {
      edits.push({ op: "match", from: a[i - 1], to: b[j - 1] });
      i -= 1;
      j -= 1;
    } else if (i > 0 && j > 0 && values[i][j] === values[i - 1][j - 1] + substitute) {
      edits.push({ op: "substitute", from: a[i - 1], to: b[j - 1] });
      i -= 1;
      j -= 1;
    } else if (i > 0 && values[i][j] === values[i - 1][j] + remove) {
      edits.push({ op: "delete", from: a[i - 1] });
      i -= 1;
    } else {
      check(j > 0, "an insertion needs a target symbol");
      edits.push({ op: "insert", to: b[j - 1] });
      j -= 1;
    }
  }

  return edits.reverse();
}

function applyScript(source: string, edits: Edit[]): string {
  const input = symbols(source);
  const output: string[] = [];
  let position = 0;

  for (const edit of edits) {
    if (edit.op !== "insert") {
      check(position < input.length, "the script reads past the source");
      check(input[position] === edit.from, `expected ${edit.from} at position ${position}`);
      position += 1;
    }

    if (edit.op === "match") {
      check(edit.from === edit.to, "a match keeps the symbol");
      output.push(edit.to);
    } else if (edit.op === "substitute" || edit.op === "insert") {
      output.push(edit.to);
    }
  }

  check(position === input.length, "the script consumes the whole source");
  return output.join("");
}

function scriptCost(edits: Edit[], costs: Partial<EditCosts> = {}): number {
  const { insert, delete: remove, substitute } = normaliseCosts(costs);
  let total = 0;
  for (const edit of edits) {
    if (edit.op === "insert") {
      total += insert;
    } else if (edit.op === "delete") {
      total += remove;
    } else if (edit.op === "substitute") {
      total += substitute;
    }
  }
  return total;
}

function similarity(left: string, right: string): number {
  const longest = Math.max(symbols(left).length, symbols(right).length);
  if (longest === 0) {
    return 1;
  }
  return 1 - distance(left, right) / longest;
}

function bruteDistance(left: readonly string[], right: readonly string[], i = 0, j = 0): number {
  if (i === left.length) {
    return right.length - j;
  }
  if (j === right.length) {
    return left.length - i;
  }
  if (left[i] === right[j]) {
    return bruteDistance(left, right, i + 1, j + 1);
  }
  return 1 + Math.min(
    bruteDistance(left, right, i + 1, j),
    bruteDistance(left, right, i, j + 1),
    bruteDistance(left, right, i + 1, j + 1),
  );
}

function seededRandom(seed: number): () => number {
  let state = seed;
  return () => {
    state = (state * 1103515245 + 12345) % 2147483648;
    return state / 2147483648;
  };
}

function main(): void {
  console.log("--- known cases ---");
  check(distance("kitten", "sitting") === 3, "kitten/sitting");
  check(distance("flaw", "lawn") === 2, "flaw/lawn");
  check(distance("intention", "execution") === 5, "intention/execution");
  check(distance("", "") === 0, "both empty");
  check(distance("", "abc") === 3, "left empty");
  check(distance("abc", "") === 3, "right empty");
  check(distance("abc", "abc") === 0, "identical");
  console.log(
    "kitten/sitting:",
    distance("kitten", "sitting"),
    "| intention/execution:",
    distance("intention", "execution"),
  );

  console.log("\n--- edit scripts ---");
  const pairs: Array<[string, string]> = [
    ["kitten", "sitting"],
    ["flaw", "lawn"],
    ["", "abc"],
    ["abc", ""],
    ["naïve", "naive"],
  ];
  for (const [left, right] of pairs) {
    const edits = editScript(left, right);
    check(applyScript(left, edits) === right, `script rebuilds "${left}" -> "${right}"`);
    check(
      scriptCost(edits) === distance(left, right),
      `script cost matches the distance for "${left}" -> "${right}"`,
    );
    const changes = edits.filter((edit) => edit.op !== "match").length;
    console.log(`  "${left}" -> "${right}": ${edits.length} steps, ${changes} changes`);
  }

  console.log("\n--- weighted costs ---");
  check(distance("abc", "adc", { substitute: 5 }) === 2, "delete and insert beat a dear substitution");
  check(distance("abc", "adc", { substitute: 1 }) === 1, "a cheap substitution is used");
  const expensiveInsert: Partial<EditCosts> = { insert: 10, delete: 1, substitute: 4 };
  check(distance("abc", "bc", expensiveInsert) === 1, "a cheap deletion");
  check(distance("bc", "abc", expensiveInsert) === 10, "an expensive insertion");
  check(
    distance("kitten", "sitting", { insert: 2, delete: 3, substitute: 1 }) ===
      distance("sitting", "kitten", { insert: 3, delete: 2, substitute: 1 }),
    "swapping the inputs swaps insertion and deletion costs",
  );
  const weighted = { substitute: 5 };
  const weightedEdits = editScript("kitten", "sitting", weighted);
  check(applyScript("kitten", weightedEdits) === "sitting", "weighted script rebuilds the target");
  check(
    scriptCost(weightedEdits, weighted) === distance("kitten", "sitting", weighted),
    "weighted script cost matches the distance",
  );
  console.log("weighted kitten -> sitting costs", scriptCost(weightedEdits, weighted));

  console.log("\n--- unicode ---");
  check(distance("naïve", "naive") === 1, "accented letters count once");
  check(distance("a😀b", "a😞b") === 1, "emoji surrogate pairs count once");
  check(distance("😀😀", "😀") === 1, "repeated emoji");
  check(distance("héllo", "hello") === 1, "another accent");
  const emojiEdits = editScript("a😀b", "a😞b");
  check(applyScript("a😀b", emojiEdits) === "a😞b", "emoji script rebuilds the target");
  console.log(
    "naïve/naive:",
    distance("naïve", "naive"),
    "| a😀b/a😞b:",
    distance("a😀b", "a😞b"),
  );

  console.log("\n--- similarity ---");
  check(similarity("abc", "abc") === 1, "identical strings are fully similar");
  check(similarity("", "") === 1, "two empty strings are similar");
  check(Math.abs(similarity("abc", "abd") - 2 / 3) < 1e-9, "one change in three symbols");
  check(similarity("abc", "xyz") === 0, "nothing shared");
  check(similarity("kitten", "sitting") > 0.5 && similarity("kitten", "sitting") < 1, "partial similarity");
  console.log("kitten/sitting similarity:", similarity("kitten", "sitting").toFixed(4));

  console.log("\n--- metric properties ---");
  const random = seededRandom(87);
  const randomText = (length: number): string => {
    let text = "";
    for (let index = 0; index < length; index += 1) {
      text += "abcd".charAt(Math.floor(random() * 4));
    }
    return text;
  };

  for (let trial = 0; trial < 200; trial += 1) {
    const a = randomText(Math.floor(random() * 9));
    const b = randomText(Math.floor(random() * 9));
    const c = randomText(Math.floor(random() * 9));
    check(distance(a, b) === distance(b, a), `symmetry at trial ${trial}`);
    check(distance(a, a) === 0, `identity at trial ${trial}`);
    check(distance(a, c) <= distance(a, b) + distance(b, c), `triangle inequality at trial ${trial}`);
  }
  console.log("200 random triples satisfy symmetry, identity, and the triangle inequality");

  console.log("\n--- against brute force ---");
  for (let trial = 0; trial < 150; trial += 1) {
    const a = randomText(Math.floor(random() * 6));
    const b = randomText(Math.floor(random() * 6));
    check(
      distance(a, b) === bruteDistance(symbols(a), symbols(b)),
      `brute force mismatch for "${a}" / "${b}"`,
    );
  }
  console.log("150 random pairs match the recursive search");

  console.log("\n--- a large input ---");
  const first = randomText(2_000);
  const second = randomText(2_000);
  const started = Date.now();
  const large = distance(first, second);
  const elapsed = Date.now() - started;
  check(distance(second, first) === large, "large distance is symmetric");
  check(large <= 4_000, "distance never exceeds the total length");
  console.log(`2,000 x 2,000 symbols: distance ${large} in ${elapsed} ms`);

  console.log("\n--- validation ---");
  const badCosts: Array<Partial<EditCosts>> = [
    { insert: 0 },
    { insert: -1 },
    { delete: 1.5 },
    { substitute: Number.NaN },
    { insert: Number.POSITIVE_INFINITY },
  ];
  for (const costs of badCosts) {
    let threw = false;
    try {
      distance("abc", "abd", costs);
    } catch (error) {
      threw = error instanceof RangeError;
    }
    check(threw, `costs ${JSON.stringify(costs)} rejected`);
  }
  console.log("zero, negative, fractional, and non-finite costs rejected");

  console.log("\nAll checks passed.");
}

main();
