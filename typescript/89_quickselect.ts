function check(condition: boolean, message: string): void {
  if (!condition) {
    throw new Error(message);
  }
}

function swap(values: number[], left: number, right: number): void {
  const temporary = values[left];
  values[left] = values[right];
  values[right] = temporary;
}

function medianOfThree(values: number[], left: number, right: number): number {
  const middle = Math.floor((left + right) / 2);
  const a = values[left];
  const b = values[middle];
  const c = values[right];
  if ((a <= b && b <= c) || (c <= b && b <= a)) {
    return b;
  }
  if ((b <= a && a <= c) || (c <= a && a <= b)) {
    return a;
  }
  return c;
}

function selectInPlace(values: number[], target: number): number {
  let left = 0;
  let right = values.length - 1;

  while (true) {
    if (left === right) {
      return values[left];
    }

    const pivot = medianOfThree(values, left, right);
    let lower = left;
    let index = left;
    let upper = right;

    while (index <= upper) {
      if (values[index] < pivot) {
        swap(values, lower, index);
        lower += 1;
        index += 1;
      } else if (values[index] > pivot) {
        swap(values, index, upper);
        upper -= 1;
      } else {
        index += 1;
      }
    }

    if (target < lower) {
      right = lower - 1;
    } else if (target > upper) {
      left = upper + 1;
    } else {
      return values[target];
    }
  }
}

function kthSmallest(values: readonly number[], k: number): number {
  if (values.length === 0) {
    throw new RangeError("values must not be empty");
  }
  if (!Number.isInteger(k) || k < 1 || k > values.length) {
    throw new RangeError(`k must be an integer between 1 and ${values.length}`);
  }
  return selectInPlace([...values], k - 1);
}

function kthLargest(values: readonly number[], k: number): number {
  if (values.length === 0) {
    throw new RangeError("values must not be empty");
  }
  if (!Number.isInteger(k) || k < 1 || k > values.length) {
    throw new RangeError(`k must be an integer between 1 and ${values.length}`);
  }
  return selectInPlace([...values], values.length - k);
}

function median(values: readonly number[]): number {
  if (values.length === 0) {
    throw new RangeError("values must not be empty");
  }
  const upper = kthSmallest(values, Math.floor(values.length / 2) + 1);
  if (values.length % 2 === 1) {
    return upper;
  }
  return (kthSmallest(values, values.length / 2) + upper) / 2;
}

function seededRandom(seed: number): () => number {
  let state = seed;
  return () => {
    state = (state * 1103515245 + 12345) % 2147483648;
    return state / 2147483648;
  };
}

function main(): void {
  console.log("--- known selections ---");
  const sample = [7, 1, 5, 3, 9, 2];
  check(kthSmallest(sample, 1) === 1, "smallest");
  check(kthSmallest(sample, 2) === 2, "second smallest");
  check(kthSmallest(sample, 6) === 9, "largest");
  check(kthLargest(sample, 1) === 9, "largest through kthLargest");
  check(kthLargest(sample, 6) === 1, "smallest through kthLargest");
  console.log("input:", sample.join(", "), "| third smallest:", kthSmallest(sample, 3));

  console.log("\n--- sorted and reversed inputs ---");
  const sorted = [1, 2, 3, 4, 5];
  for (let k = 1; k <= sorted.length; k += 1) {
    check(kthSmallest(sorted, k) === k, `sorted k=${k}`);
  }
  const reversed = [5, 4, 3, 2, 1];
  for (let k = 1; k <= reversed.length; k += 1) {
    check(kthSmallest(reversed, k) === k, `reversed k=${k}`);
  }
  console.log("sorted and reversed inputs return every order statistic");

  console.log("\n--- duplicates ---");
  const repeated = [5, 1, 5, 1, 5];
  const expected = [1, 1, 5, 5, 5];
  for (let k = 1; k <= repeated.length; k += 1) {
    check(kthSmallest(repeated, k) === expected[k - 1], `duplicates k=${k}`);
  }
  const equal = new Array<number>(1_000).fill(7);
  check(kthSmallest(equal, 1) === 7 && kthSmallest(equal, 500) === 7, "all equal");
  check(kthSmallest(equal, 1_000) === 7, "all equal at the end");
  console.log("three-way partitioning handles repeated values");

  console.log("\n--- input is untouched ---");
  const original = [9, -3, 14, 0, 7, 7, -8];
  const before = original.join(",");
  kthSmallest(original, 3);
  kthLargest(original, 2);
  check(original.join(",") === before, "selecting does not reorder the input");
  console.log("input after selecting:", original.join(", "));

  console.log("\n--- medians ---");
  check(median([9, 1, 5]) === 5, "odd median");
  check(median([4, 1, 3, 2]) === 2.5, "even median");
  check(median([-5]) === -5, "single value");
  check(median([1, 2]) === 1.5, "two values");
  console.log("median of [4, 1, 3, 2]:", median([4, 1, 3, 2]));

  console.log("\n--- random cross-check ---");
  const random = seededRandom(89);
  for (let trial = 0; trial < 300; trial += 1) {
    const length = 1 + Math.floor(random() * 40);
    const values: number[] = [];
    for (let index = 0; index < length; index += 1) {
      values.push(Math.floor(random() * 41) - 20);
    }
    const sortedValues = [...values].sort((a, b) => a - b);
    const k = 1 + Math.floor(random() * length);
    check(kthSmallest(values, k) === sortedValues[k - 1], `smallest mismatch at trial ${trial}`);
    check(kthLargest(values, k) === sortedValues[length - k], `largest mismatch at trial ${trial}`);
  }
  console.log("300 random arrays match a full sort for smallest and largest");

  console.log("\n--- a large input ---");
  const large: number[] = [];
  for (let index = 0; index < 200_000; index += 1) {
    large.push(Math.floor(random() * 1_000_000) - 500_000);
  }
  const startedSort = Date.now();
  const largeSorted = [...large].sort((a, b) => a - b);
  const sortElapsed = Date.now() - startedSort;
  const startedSelect = Date.now();
  const middle = kthSmallest(large, large.length / 2);
  const selectElapsed = Date.now() - startedSelect;
  check(kthSmallest(large, 1) === largeSorted[0], "large minimum");
  check(middle === largeSorted[largeSorted.length / 2 - 1], "large median");
  check(kthSmallest(large, large.length) === largeSorted[largeSorted.length - 1], "large maximum");
  check(kthLargest(large, 1) === largeSorted[largeSorted.length - 1], "large maximum through kthLargest");
  console.log(
    `200,000 values: middle ${middle} in ${selectElapsed} ms | sort ${sortElapsed} ms`,
  );

  console.log("\n--- validation ---");
  const badCalls: Array<() => unknown> = [
    () => kthSmallest([], 1),
    () => kthSmallest([1, 2, 3], 0),
    () => kthSmallest([1, 2, 3], 4),
    () => kthSmallest([1, 2, 3], 1.5),
    () => kthLargest([], 1),
    () => kthLargest([1, 2], 3),
    () => median([]),
  ];
  for (const call of badCalls) {
    let threw = false;
    try {
      call();
    } catch (error) {
      threw = error instanceof RangeError;
    }
    check(threw, "invalid selection rejected");
  }
  console.log("empty inputs and out-of-range k rejected");

  console.log("\nAll checks passed.");
}

main();
