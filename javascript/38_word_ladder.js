const assert = require("node:assert/strict");

function validate(begin, end, words) {
  if (typeof begin !== "string" || begin.length === 0) {
    throw new TypeError("begin must be a non-empty string");
  }
  if (typeof end !== "string" || end.length === 0) {
    throw new TypeError("end must be a non-empty string");
  }
  if (!Array.isArray(words)) {
    throw new TypeError("words must be an array");
  }
  for (const word of words) {
    if (typeof word !== "string" || word.length === 0) {
      throw new TypeError("every dictionary word must be a non-empty string");
    }
  }
}

function prepare(begin, end, words) {
  validate(begin, end, words);
  const dictionary = new Set();
  for (const word of words) {
    if (word.length === begin.length) {
      dictionary.add(word);
    }
  }
  if (begin.length !== end.length || !dictionary.has(end)) {
    return null;
  }
  dictionary.add(begin);
  return { dictionary, patterns: buildPatterns(dictionary) };
}

function patternOf(word, index) {
  return `${word.slice(0, index)}*${word.slice(index + 1)}`;
}

function buildPatterns(dictionary) {
  const patterns = new Map();
  for (const word of dictionary) {
    for (let index = 0; index < word.length; index += 1) {
      const key = patternOf(word, index);
      const bucket = patterns.get(key);
      if (bucket === undefined) {
        patterns.set(key, [word]);
      } else {
        bucket.push(word);
      }
    }
  }
  return patterns;
}

function neighbours(word, patterns) {
  const result = [];
  for (let index = 0; index < word.length; index += 1) {
    const bucket = patterns.get(patternOf(word, index));
    if (bucket === undefined) {
      continue;
    }
    for (const candidate of bucket) {
      if (candidate !== word) {
        result.push(candidate);
      }
    }
  }
  return result;
}

function ladderLength(begin, end, words) {
  if (begin === end) {
    validate(begin, end, words);
    return 1;
  }
  const graph = prepare(begin, end, words);
  if (graph === null) {
    return 0;
  }

  const distances = new Map([[begin, 1]]);
  const queue = [begin];

  for (let head = 0; head < queue.length; head += 1) {
    const word = queue[head];
    const steps = distances.get(word);
    for (const next of neighbours(word, graph.patterns)) {
      if (next === end) {
        return steps + 1;
      }
      if (distances.has(next)) {
        continue;
      }
      distances.set(next, steps + 1);
      queue.push(next);
    }
  }

  return 0;
}

function findLadder(begin, end, words) {
  if (begin === end) {
    validate(begin, end, words);
    return [begin];
  }
  const graph = prepare(begin, end, words);
  if (graph === null) {
    return [];
  }

  const parents = new Map([[begin, null]]);
  const queue = [begin];

  for (let head = 0; head < queue.length; head += 1) {
    const word = queue[head];
    if (word === end) {
      break;
    }
    for (const next of neighbours(word, graph.patterns)) {
      if (parents.has(next)) {
        continue;
      }
      parents.set(next, word);
      queue.push(next);
    }
  }

  if (!parents.has(end)) {
    return [];
  }

  const path = [];
  let current = end;
  while (current !== null) {
    path.push(current);
    current = parents.get(current) ?? null;
  }
  return path.reverse();
}

function ladderLengthBidirectional(begin, end, words) {
  if (begin === end) {
    validate(begin, end, words);
    return 1;
  }
  const graph = prepare(begin, end, words);
  if (graph === null) {
    return 0;
  }

  let forwardVisited = new Map([[begin, 1]]);
  let backwardVisited = new Map([[end, 1]]);
  let forwardFrontier = [begin];
  let backwardFrontier = [end];

  while (forwardFrontier.length > 0 && backwardFrontier.length > 0) {
    if (forwardFrontier.length > backwardFrontier.length) {
      [forwardVisited, backwardVisited] = [backwardVisited, forwardVisited];
      [forwardFrontier, backwardFrontier] = [backwardFrontier, forwardFrontier];
    }

    const nextFrontier = [];
    for (const word of forwardFrontier) {
      const steps = forwardVisited.get(word);
      for (const next of neighbours(word, graph.patterns)) {
        if (forwardVisited.has(next)) {
          continue;
        }
        const other = backwardVisited.get(next);
        if (other !== undefined) {
          return steps + other;
        }
        forwardVisited.set(next, steps + 1);
        nextFrontier.push(next);
      }
    }
    forwardFrontier = nextFrontier;
  }

  return 0;
}

function differsByOne(left, right) {
  if (left.length !== right.length) {
    return false;
  }
  let differences = 0;
  for (let index = 0; index < left.length; index += 1) {
    if (left[index] !== right[index]) {
      differences += 1;
    }
  }
  return differences === 1;
}

function validatePath(path, begin, end, words) {
  assert.ok(path.length > 0);
  assert.equal(path[0], begin);
  assert.equal(path[path.length - 1], end);
  const dictionary = new Set(words);
  for (let index = 1; index < path.length; index += 1) {
    assert.ok(
      differsByOne(path[index - 1], path[index]),
      `steps ${index - 1} and ${index} differ by one letter`,
    );
    assert.ok(
      dictionary.has(path[index]) || path[index] === end,
      "every intermediate word comes from the dictionary",
    );
  }
}

function seededRandom(seed) {
  let state = seed;
  return () => {
    state = (state * 1103515245 + 12345) % 2147483648;
    return state / 2147483648;
  };
}

function main() {
  console.log("--- the classic ladder ---");
  const classic = ["hot", "dot", "dog", "lot", "log", "cog"];
  assert.equal(ladderLength("hit", "cog", classic), 5);
  assert.equal(ladderLengthBidirectional("hit", "cog", classic), 5);
  const classicPath = findLadder("hit", "cog", classic);
  assert.equal(classicPath.length, 5);
  validatePath(classicPath, "hit", "cog", classic);
  console.log("hit -> cog:", classicPath.join(" -> "));

  console.log("\n--- unreachable targets ---");
  const withoutEnd = ["hot", "dot", "dog", "lot", "log"];
  assert.equal(ladderLength("hit", "cog", withoutEnd), 0);
  assert.equal(ladderLengthBidirectional("hit", "cog", withoutEnd), 0);
  assert.deepEqual(findLadder("hit", "cog", withoutEnd), []);
  assert.equal(ladderLength("hit", "cog", ["hot", "dot"]), 0);
  assert.equal(ladderLength("hit", "cogs", ["cogs"]), 0);
  console.log("missing targets and mismatched lengths return zero");

  console.log("\n--- short ladders ---");
  assert.equal(ladderLength("a", "b", ["b"]), 2);
  assert.equal(ladderLengthBidirectional("a", "b", ["b"]), 2);
  assert.equal(findLadder("a", "b", ["b"]).join(","), "a,b");
  assert.equal(ladderLength("abc", "abc", []), 1);
  assert.equal(findLadder("abc", "abc", []).join(","), "abc");
  console.log("one-step and same-word ladders count correctly");

  console.log("\n--- a full hypercube ---");
  const hypercube = [];
  for (const first of ["a", "b"]) {
    for (const second of ["a", "b"]) {
      for (const third of ["a", "b"]) {
        for (const fourth of ["a", "b"]) {
          hypercube.push(first + second + third + fourth);
        }
      }
    }
  }
  assert.equal(ladderLength("aaaa", "bbbb", hypercube), 5);
  assert.equal(ladderLengthBidirectional("aaaa", "bbbb", hypercube), 5);
  const hypercubePath = findLadder("aaaa", "bbbb", hypercube);
  assert.equal(hypercubePath.length, 5);
  validatePath(hypercubePath, "aaaa", "bbbb", hypercube);
  console.log("aaaa -> bbbb:", hypercubePath.join(" -> "));

  console.log("\n--- random graphs against breadth-first search ---");
  const random = seededRandom(38);
  const universe = [];
  for (const first of ["a", "b", "c"]) {
    for (const second of ["a", "b", "c"]) {
      for (const third of ["a", "b", "c"]) {
        universe.push(first + second + third);
      }
    }
  }

  for (let trial = 0; trial < 300; trial += 1) {
    const words = universe.filter(() => random() < 0.5);
    const begin = universe[Math.floor(random() * universe.length)];
    const end = universe[Math.floor(random() * universe.length)];
    const expected = ladderLength(begin, end, words);
    assert.equal(ladderLengthBidirectional(begin, end, words), expected);

    const path = findLadder(begin, end, words);
    if (expected === 0) {
      assert.deepEqual(path, []);
    } else {
      assert.equal(path.length, expected);
      validatePath(path, begin, end, words);
    }
  }
  console.log("300 random begin/end pairs agree and every path is valid");

  console.log("\n--- a large dictionary ---");
  const big = [];
  for (let number = 0; number < 10_000; number += 1) {
    big.push(`w${String(number).padStart(4, "0")}`);
  }
  const started = Date.now();
  const bigLength = ladderLengthBidirectional("w0000", "w9999", big);
  const elapsed = Date.now() - started;
  assert.equal(bigLength, 5);
  assert.equal(ladderLength("w0000", "w9999", big), 5);
  assert.equal(findLadder("w0000", "w9999", big).length, 5);
  console.log(`10,000 words: w0000 -> w9999 in ${bigLength} words (${elapsed} ms bidirectional)`);

  console.log("\n--- validation ---");
  const badCalls = [
    () => ladderLength("", "abc", []),
    () => ladderLength("abc", "", []),
    () => ladderLength("abc", "abc", [""]),
    () => ladderLength("abc", "abc", 5),
    () => findLadder("", "abc", []),
    () => ladderLengthBidirectional("abc", "abc", [""]),
  ];
  for (const call of badCalls) {
    assert.throws(call, TypeError);
  }
  console.log("empty words, empty endpoints, and non-array dictionaries rejected");

  console.log("\nAll checks passed.");
}

main();
