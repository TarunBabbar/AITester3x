type Graph = {
  dictionary: Set<string>;
  patterns: Map<string, string[]>;
};

function check(condition: boolean, message: string): void {
  if (!condition) {
    throw new Error(message);
  }
}

function validate(begin: string, end: string, words: readonly string[]): void {
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

function prepare(begin: string, end: string, words: readonly string[]): Graph | null {
  validate(begin, end, words);
  const dictionary = new Set<string>();
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

function patternOf(word: string, index: number): string {
  return `${word.slice(0, index)}*${word.slice(index + 1)}`;
}

function buildPatterns(dictionary: Set<string>): Map<string, string[]> {
  const patterns = new Map<string, string[]>();
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

function neighbours(word: string, patterns: Map<string, string[]>): string[] {
  const result: string[] = [];
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

function ladderLength(begin: string, end: string, words: readonly string[]): number {
  if (begin === end) {
    validate(begin, end, words);
    return 1;
  }
  const graph = prepare(begin, end, words);
  if (graph === null) {
    return 0;
  }

  const distances = new Map<string, number>([[begin, 1]]);
  const queue: string[] = [begin];

  for (let head = 0; head < queue.length; head += 1) {
    const word = queue[head];
    const steps = distances.get(word) as number;
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

function findLadder(begin: string, end: string, words: readonly string[]): string[] {
  if (begin === end) {
    validate(begin, end, words);
    return [begin];
  }
  const graph = prepare(begin, end, words);
  if (graph === null) {
    return [];
  }

  const parents = new Map<string, string | null>([[begin, null]]);
  const queue: string[] = [begin];

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

  const path: string[] = [];
  let current: string | null = end;
  while (current !== null) {
    path.push(current);
    current = parents.get(current) ?? null;
  }
  return path.reverse();
}

function ladderLengthBidirectional(begin: string, end: string, words: readonly string[]): number {
  if (begin === end) {
    validate(begin, end, words);
    return 1;
  }
  const graph = prepare(begin, end, words);
  if (graph === null) {
    return 0;
  }

  let forwardVisited = new Map<string, number>([[begin, 1]]);
  let backwardVisited = new Map<string, number>([[end, 1]]);
  let forwardFrontier = [begin];
  let backwardFrontier = [end];

  while (forwardFrontier.length > 0 && backwardFrontier.length > 0) {
    if (forwardFrontier.length > backwardFrontier.length) {
      [forwardVisited, backwardVisited] = [backwardVisited, forwardVisited];
      [forwardFrontier, backwardFrontier] = [backwardFrontier, forwardFrontier];
    }

    const nextFrontier: string[] = [];
    for (const word of forwardFrontier) {
      const steps = forwardVisited.get(word) as number;
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

function differsByOne(left: string, right: string): boolean {
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

function validatePath(path: string[], begin: string, end: string, words: readonly string[]): void {
  check(path.length > 0, "a path exists");
  check(path[0] === begin, "the path starts at begin");
  check(path[path.length - 1] === end, "the path ends at end");
  const dictionary = new Set(words);
  for (let index = 1; index < path.length; index += 1) {
    check(differsByOne(path[index - 1], path[index]), `steps ${index - 1} and ${index} differ by one letter`);
    check(
      dictionary.has(path[index]) || path[index] === end,
      "every intermediate word comes from the dictionary",
    );
  }
}

function seededRandom(seed: number): () => number {
  let state = seed;
  return () => {
    state = (state * 1103515245 + 12345) % 2147483648;
    return state / 2147483648;
  };
}

function main(): void {
  console.log("--- the classic ladder ---");
  const classic = ["hot", "dot", "dog", "lot", "log", "cog"];
  check(ladderLength("hit", "cog", classic) === 5, "classic length");
  check(ladderLengthBidirectional("hit", "cog", classic) === 5, "classic bidirectional length");
  const classicPath = findLadder("hit", "cog", classic);
  check(classicPath.length === 5, "classic path length");
  validatePath(classicPath, "hit", "cog", classic);
  console.log("hit -> cog:", classicPath.join(" -> "));

  console.log("\n--- unreachable targets ---");
  check(ladderLength("hit", "cog", ["hot", "dot", "dog", "lot", "log"]) === 0, "no path");
  check(ladderLengthBidirectional("hit", "cog", ["hot", "dot", "dog", "lot", "log"]) === 0, "no path");
  check(findLadder("hit", "cog", ["hot", "dot", "dog", "lot", "log"]).length === 0, "empty path");
  check(ladderLength("hit", "cog", ["hot", "dot"]) === 0, "target outside the dictionary");
  check(ladderLength("hit", "cogs", ["cogs"]) === 0, "different word lengths");
  console.log("missing targets and mismatched lengths return zero");

  console.log("\n--- short ladders ---");
  check(ladderLength("a", "b", ["b"]) === 2, "one transformation");
  check(ladderLengthBidirectional("a", "b", ["b"]) === 2, "one transformation bidirectional");
  check(findLadder("a", "b", ["b"]).join(",") === "a,b", "one-step path");
  check(ladderLength("abc", "abc", []) === 1, "begin equals end");
  check(findLadder("abc", "abc", []).join(",") === "abc", "a single-word path");
  console.log("one-step and same-word ladders count correctly");

  console.log("\n--- a full hypercube ---");
  const hypercube: string[] = [];
  for (const first of ["a", "b"]) {
    for (const second of ["a", "b"]) {
      for (const third of ["a", "b"]) {
        for (const fourth of ["a", "b"]) {
          hypercube.push(first + second + third + fourth);
        }
      }
    }
  }
  check(ladderLength("aaaa", "bbbb", hypercube) === 5, "four changes");
  check(ladderLengthBidirectional("aaaa", "bbbb", hypercube) === 5, "four changes bidirectional");
  const hypercubePath = findLadder("aaaa", "bbbb", hypercube);
  check(hypercubePath.length === 5, "hypercube path length");
  validatePath(hypercubePath, "aaaa", "bbbb", hypercube);
  console.log("aaaa -> bbbb:", hypercubePath.join(" -> "));

  console.log("\n--- random graphs against breadth-first search ---");
  const random = seededRandom(90);
  const universe: string[] = [];
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
    check(
      ladderLengthBidirectional(begin, end, words) === expected,
      `bidirectional mismatch at trial ${trial}`,
    );

    const path = findLadder(begin, end, words);
    if (expected === 0) {
      check(path.length === 0, `unexpected path at trial ${trial}`);
    } else {
      check(path.length === expected, `path length mismatch at trial ${trial}`);
      validatePath(path, begin, end, words);
    }
  }
  console.log("300 random begin/end pairs agree and every path is valid");

  console.log("\n--- a large dictionary ---");
  const big: string[] = [];
  for (let number = 0; number < 10_000; number += 1) {
    big.push(`w${String(number).padStart(4, "0")}`);
  }
  const started = Date.now();
  const bigLength = ladderLengthBidirectional("w0000", "w9999", big);
  const elapsed = Date.now() - started;
  check(bigLength === 5, `large ladder length ${bigLength}`);
  check(ladderLength("w0000", "w9999", big) === 5, "large plain breadth-first length");
  check(findLadder("w0000", "w9999", big).length === 5, "large path");
  console.log(`10,000 words: w0000 -> w9999 in ${bigLength} words (${elapsed} ms bidirectional)`);

  console.log("\n--- validation ---");
  const badCalls: Array<() => unknown> = [
    () => ladderLength("", "abc", []),
    () => ladderLength("abc", "", []),
    () => ladderLength("abc", "abc", [""]),
    () => ladderLength("abc", "abc", 5 as unknown as string[]),
    () => findLadder("", "abc", []),
    () => ladderLengthBidirectional("abc", "abc", [""]),
  ];
  for (const call of badCalls) {
    let threw = false;
    try {
      call();
    } catch (error) {
      threw = error instanceof TypeError;
    }
    check(threw, "invalid input rejected");
  }
  console.log("empty words, empty endpoints, and non-array dictionaries rejected");

  console.log("\nAll checks passed.");
}

main();
