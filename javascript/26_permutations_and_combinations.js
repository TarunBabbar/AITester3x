const assert = require("node:assert/strict");

function* permutations(items) {
  if (items.length <= 1) {
    yield [...items];
    return;
  }

  for (let index = 0; index < items.length; index += 1) {
    const rest = [...items.slice(0, index), ...items.slice(index + 1)];
    for (const permutation of permutations(rest)) {
      yield [items[index], ...permutation];
    }
  }
}

function* uniquePermutations(items) {
  const seen = new Set();
  for (const permutation of permutations(items)) {
    const key = JSON.stringify(permutation);
    if (!seen.has(key)) {
      seen.add(key);
      yield permutation;
    }
  }
}

function* combinations(items, size) {
  if (size < 0 || size > items.length) {
    return;
  }
  if (size === 0) {
    yield [];
    return;
  }

  function* walk(start, current) {
    if (current.length === size) {
      yield [...current];
      return;
    }
    const remaining = size - current.length;
    for (let index = start; index <= items.length - remaining; index += 1) {
      current.push(items[index]);
      yield* walk(index + 1, current);
      current.pop();
    }
  }

  yield* walk(0, []);
}

function* subsets(items) {
  for (let size = 0; size <= items.length; size += 1) {
    yield* combinations(items, size);
  }
}

function* cartesian(...groups) {
  if (groups.some((group) => group.length === 0)) {
    return;
  }
  if (groups.length === 0) {
    yield [];
    return;
  }

  const [first, ...rest] = groups;
  for (const value of first) {
    for (const suffix of cartesian(...rest)) {
      yield [value, ...suffix];
    }
  }
}

function take(iterable, count) {
  const values = [];
  for (const value of iterable) {
    values.push(value);
    if (values.length === count) {
      break;
    }
  }
  return values;
}

function count(iterable) {
  let total = 0;
  for (const value of iterable) {
    void value;
    total += 1;
  }
  return total;
}

function factorial(value) {
  let result = 1;
  for (let index = 2; index <= value; index += 1) {
    result *= index;
  }
  return result;
}

function binomial(n, k) {
  if (k < 0 || k > n) {
    return 0;
  }
  let result = 1;
  for (let index = 1; index <= k; index += 1) {
    result = (result * (n - k + index)) / index;
  }
  return Math.round(result);
}

function main() {
  console.log("--- permutations ---");
  const three = [...permutations([1, 2, 3])];
  console.log(JSON.stringify(three));
  assert.equal(three.length, factorial(3));
  assert.deepEqual(three[0], [1, 2, 3]);
  assert.deepEqual(three[three.length - 1], [3, 2, 1]);
  assert.equal(new Set(three.map((value) => value.join(","))).size, 6);

  assert.deepEqual([...permutations([])], [[]]);
  assert.deepEqual([...permutations([7])], [[7]]);
  console.log("empty and single inputs yield one arrangement");

  console.log("\n--- duplicate permutations ---");
  const duplicates = [...uniquePermutations([1, 1, 2])];
  console.log(JSON.stringify(duplicates));
  assert.equal(duplicates.length, 3);
  assert.deepEqual(
    duplicates.map((value) => value.join("")).sort(),
    ["112", "121", "211"],
  );

  console.log("\n--- combinations ---");
  assert.equal([...combinations([1, 2, 3, 4], 2)].length, binomial(4, 2));
  assert.deepEqual([...combinations([1, 2, 3], 0)], [[]]);
  assert.deepEqual([...combinations([1, 2, 3], 4)], []);
  assert.deepEqual([...combinations([1, 2, 3], 3)], [[1, 2, 3]]);

  const pairs = [...combinations(["a", "b", "c"], 2)];
  console.log(JSON.stringify(pairs));
  assert.deepEqual(pairs, [["a", "b"], ["a", "c"], ["b", "c"]]);
  assert.equal(new Set(pairs.map((pair) => pair.join(""))).size, 3);

  console.log("\n--- subsets (the power set) ---");
  const power = [...subsets([1, 2, 3])];
  console.log(JSON.stringify(power));
  assert.equal(power.length, 2 ** 3);
  assert.deepEqual(power[0], []);
  assert.deepEqual(power[power.length - 1], [1, 2, 3]);

  const powerSet = new Set(power.map((subset) => subset.join(",")));
  assert.equal(powerSet.size, 8);
  for (const subset of power) {
    assert.ok(subset.every((value) => [1, 2, 3].includes(value)));
  }
  console.log("every subset is unique and uses only original elements");

  console.log("\n--- cartesian product ---");
  const product = [...cartesian([1, 2], ["a", "b"], [true, false])];
  console.log(JSON.stringify(product));
  assert.equal(product.length, 8);
  assert.deepEqual(product[0], [1, "a", true]);
  assert.deepEqual(product[7], [2, "b", false]);
  assert.deepEqual([...cartesian()], [[]]);
  assert.deepEqual([...cartesian([1], [])], []);
  console.log("empty groups short-circuit to no output");

  console.log("\n--- generation is lazy ---");
  let generated = 0;
  function* counted() {
    for (let index = 0; index < 1_000; index += 1) {
      generated += 1;
      yield index;
    }
  }
  const firstThree = take(counted(), 3);
  console.log("took", firstThree.length, "values after generating", generated);
  assert.deepEqual(firstThree, [0, 1, 2]);
  assert.equal(generated, 3);

  let permutationCount = 0;
  const huge = (function* () {
    for (const value of permutations(new Array(9).fill(0).map((_, index) => index))) {
      permutationCount += 1;
      yield value;
    }
  })();
  take(huge, 5);
  assert.equal(permutationCount, 5);
  console.log("permutations of 9 items are produced on demand");

  console.log("\n--- counts match the combinatorics ---");
  for (let size = 0; size <= 8; size += 1) {
    const items = Array.from({ length: size }, (_, index) => index);
    assert.equal(count(permutations(items)), factorial(size), `permutations of ${size}`);
    const totalSubsets = count(subsets(items));
    assert.equal(totalSubsets, 2 ** size, `subsets of ${size}`);
    for (let pick = 0; pick <= size; pick += 1) {
      assert.equal(count(combinations(items, pick)), binomial(size, pick), `${pick} of ${size}`);
    }
  }
  console.log("permutations, combinations, and power sets match the formulas up to size 8");

  console.log("\n--- contents are valid ---");
  const values = ["x", "y", "z", "w"];
  for (const permutation of permutations(values)) {
    assert.deepEqual([...permutation].sort(), [...values].sort());
  }
  for (const combination of combinations(values, 3)) {
    assert.equal(combination.length, 3);
    assert.equal(new Set(combination).size, 3);
    for (const value of combination) {
      assert.ok(values.includes(value));
    }
  }
  console.log("every generated arrangement uses distinct source elements");

  console.log("\nAll checks passed.");
}

main();
