const assert = require("node:assert/strict");

const UNIT_COSTS = { insert: 1, delete: 1, substitute: 1 };

function symbols(text) {
  return Array.from(text);
}

function normaliseCosts(costs = {}) {
  const merged = {
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

function table(left, right, costs = {}) {
  const { insert, delete: remove, substitute } = normaliseCosts(costs);
  const a = symbols(left);
  const b = symbols(right);
  const values = Array.from({ length: a.length + 1 }, () => new Array(b.length + 1).fill(0));

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

function distance(left, right, costs = {}) {
  const { insert, delete: remove, substitute } = normaliseCosts(costs);
  const a = symbols(left);
  const b = symbols(right);
  let previous = Array.from({ length: b.length + 1 }, (_, index) => index * insert);

  for (let i = 1; i <= a.length; i += 1) {
    const current = new Array(b.length + 1).fill(0);
    current[0] = i * remove;
    for (let j = 1; j <= b.length; j += 1) {
      const substitution = previous[j - 1] + (a[i - 1] === b[j - 1] ? 0 : substitute);
      current[j] = Math.min(previous[j] + remove, current[j - 1] + insert, substitution);
    }
    previous = current;
  }

  return previous[b.length];
}

function editScript(left, right, costs = {}) {
  const { insert, delete: remove, substitute } = normaliseCosts(costs);
  const a = symbols(left);
  const b = symbols(right);
  const values = table(left, right, costs);
  const edits = [];
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
      assert.ok(j > 0, "an insertion needs a target symbol");
      edits.push({ op: "insert", to: b[j - 1] });
      j -= 1;
    }
  }

  return edits.reverse();
}

function applyScript(source, edits) {
  const input = symbols(source);
  const output = [];
  let position = 0;

  for (const edit of edits) {
    if (edit.op !== "insert") {
      assert.ok(position < input.length, "the script reads past the source");
      assert.equal(input[position], edit.from);
      position += 1;
    }

    if (edit.op === "match") {
      assert.equal(edit.from, edit.to);
      output.push(edit.to);
    } else if (edit.op === "substitute" || edit.op === "insert") {
      output.push(edit.to);
    }
  }

  assert.equal(position, input.length, "the script consumes the whole source");
  return output.join("");
}

function scriptCost(edits, costs = {}) {
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

function similarity(left, right) {
  const longest = Math.max(symbols(left).length, symbols(right).length);
  if (longest === 0) {
    return 1;
  }
  return 1 - distance(left, right) / longest;
}

function bruteDistance(left, right, i = 0, j = 0) {
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

function seededRandom(seed) {
  let state = seed;
  return () => {
    state = (state * 1103515245 + 12345) % 2147483648;
    return state / 2147483648;
  };
}

function main() {
  console.log("--- known cases ---");
  assert.equal(distance("kitten", "sitting"), 3);
  assert.equal(distance("flaw", "lawn"), 2);
  assert.equal(distance("intention", "execution"), 5);
  assert.equal(distance("", ""), 0);
  assert.equal(distance("", "abc"), 3);
  assert.equal(distance("abc", ""), 3);
  assert.equal(distance("abc", "abc"), 0);
  console.log(
    "kitten/sitting:",
    distance("kitten", "sitting"),
    "| intention/execution:",
    distance("intention", "execution"),
  );

  console.log("\n--- edit scripts ---");
  const pairs = [
    ["kitten", "sitting"],
    ["flaw", "lawn"],
    ["", "abc"],
    ["abc", ""],
    ["naïve", "naive"],
  ];
  for (const [left, right] of pairs) {
    const edits = editScript(left, right);
    assert.equal(applyScript(left, edits), right);
    assert.equal(scriptCost(edits), distance(left, right));
    const changes = edits.filter((edit) => edit.op !== "match").length;
    console.log(`  "${left}" -> "${right}": ${edits.length} steps, ${changes} changes`);
  }

  console.log("\n--- weighted costs ---");
  assert.equal(distance("abc", "adc", { substitute: 5 }), 2);
  assert.equal(distance("abc", "adc", { substitute: 1 }), 1);
  const expensiveInsert = { insert: 10, delete: 1, substitute: 4 };
  assert.equal(distance("abc", "bc", expensiveInsert), 1);
  assert.equal(distance("bc", "abc", expensiveInsert), 10);
  assert.equal(
    distance("kitten", "sitting", { insert: 2, delete: 3, substitute: 1 }),
    distance("sitting", "kitten", { insert: 3, delete: 2, substitute: 1 }),
  );
  const weighted = { substitute: 5 };
  const weightedEdits = editScript("kitten", "sitting", weighted);
  assert.equal(applyScript("kitten", weightedEdits), "sitting");
  assert.equal(scriptCost(weightedEdits, weighted), distance("kitten", "sitting", weighted));
  console.log("weighted kitten -> sitting costs", scriptCost(weightedEdits, weighted));

  console.log("\n--- unicode ---");
  assert.equal(distance("naïve", "naive"), 1);
  assert.equal(distance("a😀b", "a😞b"), 1);
  assert.equal(distance("😀😀", "😀"), 1);
  assert.equal(distance("héllo", "hello"), 1);
  const emojiEdits = editScript("a😀b", "a😞b");
  assert.equal(applyScript("a😀b", emojiEdits), "a😞b");
  console.log(
    "naïve/naive:",
    distance("naïve", "naive"),
    "| a😀b/a😞b:",
    distance("a😀b", "a😞b"),
  );

  console.log("\n--- similarity ---");
  assert.equal(similarity("abc", "abc"), 1);
  assert.equal(similarity("", ""), 1);
  assert.ok(Math.abs(similarity("abc", "abd") - 2 / 3) < 1e-9);
  assert.equal(similarity("abc", "xyz"), 0);
  assert.ok(similarity("kitten", "sitting") > 0.5 && similarity("kitten", "sitting") < 1);
  console.log("kitten/sitting similarity:", similarity("kitten", "sitting").toFixed(4));

  console.log("\n--- metric properties ---");
  const random = seededRandom(35);
  const randomText = (length) => {
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
    assert.equal(distance(a, b), distance(b, a));
    assert.equal(distance(a, a), 0);
    assert.ok(distance(a, c) <= distance(a, b) + distance(b, c));
  }
  console.log("200 random triples satisfy symmetry, identity, and the triangle inequality");

  console.log("\n--- against brute force ---");
  for (let trial = 0; trial < 150; trial += 1) {
    const a = randomText(Math.floor(random() * 6));
    const b = randomText(Math.floor(random() * 6));
    assert.equal(distance(a, b), bruteDistance(symbols(a), symbols(b)), `"${a}" / "${b}"`);
  }
  console.log("150 random pairs match the recursive search");

  console.log("\n--- a large input ---");
  const first = randomText(2_000);
  const second = randomText(2_000);
  const started = Date.now();
  const large = distance(first, second);
  const elapsed = Date.now() - started;
  assert.equal(distance(second, first), large);
  assert.ok(large <= 4_000);
  console.log(`2,000 x 2,000 symbols: distance ${large} in ${elapsed} ms`);

  console.log("\n--- validation ---");
  const badCosts = [
    { insert: 0 },
    { insert: -1 },
    { delete: 1.5 },
    { substitute: Number.NaN },
    { insert: Number.POSITIVE_INFINITY },
  ];
  for (const costs of badCosts) {
    assert.throws(() => distance("abc", "abd", costs), RangeError);
  }
  console.log("zero, negative, fractional, and non-finite costs rejected");

  console.log("\nAll checks passed.");
}

main();
