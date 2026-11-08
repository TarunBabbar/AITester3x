const assert = require("node:assert/strict");

/** First index whose value is not less than the target. */
function lowerBound(values, target) {
  let low = 0;
  let high = values.length;
  while (low < high) {
    const middle = (low + high) >> 1;
    if (values[middle] < target) {
      low = middle + 1;
    } else {
      high = middle;
    }
  }
  return low;
}

/** First index whose value is greater than the target. */
function upperBound(values, target) {
  let low = 0;
  let high = values.length;
  while (low < high) {
    const middle = (low + high) >> 1;
    if (values[middle] <= target) {
      low = middle + 1;
    } else {
      high = middle;
    }
  }
  return low;
}

function binarySearch(values, target) {
  let low = 0;
  let high = values.length - 1;
  while (low <= high) {
    const middle = (low + high) >> 1;
    if (values[middle] === target) {
      return middle;
    }
    if (values[middle] < target) {
      low = middle + 1;
    } else {
      high = middle - 1;
    }
  }
  return -1;
}

function searchRange(values, target) {
  const start = lowerBound(values, target);
  if (start === values.length || values[start] !== target) {
    return [-1, -1];
  }
  return [start, upperBound(values, target) - 1];
}

function searchInsert(values, target) {
  return lowerBound(values, target);
}

/** Smallest index where predicate turns true, on a false...true sequence. */
function firstTrue(predicate, length) {
  let low = 0;
  let high = length;
  while (low < high) {
    const middle = (low + high) >> 1;
    if (predicate(middle)) {
      high = middle;
    } else {
      low = middle + 1;
    }
  }
  return low === length ? -1 : low;
}

function integerSquareRoot(value) {
  if (!Number.isInteger(value) || value < 0) {
    throw new RangeError("value must be a non-negative integer");
  }
  let low = 0;
  let high = value;
  while (low <= high) {
    const middle = Math.floor((low + high) / 2);
    if (middle * middle <= value) {
      low = middle + 1;
    } else {
      high = middle - 1;
    }
  }
  return high;
}

/** Search in a rotated sorted array with distinct values. */
function rotatedSearch(values, target) {
  let low = 0;
  let high = values.length - 1;

  while (low <= high) {
    const middle = (low + high) >> 1;
    if (values[middle] === target) {
      return middle;
    }

    if (values[low] <= values[middle]) {
      if (values[low] <= target && target < values[middle]) {
        high = middle - 1;
      } else {
        low = middle + 1;
      }
    } else if (values[middle] < target && target <= values[high]) {
      low = middle + 1;
    } else {
      high = middle - 1;
    }
  }

  return -1;
}

function main() {
  console.log("--- bounds ---");
  const values = [1, 2, 2, 2, 4, 6, 6, 9];
  console.log("values:", values.join(", "));
  assert.equal(lowerBound(values, 2), 1);
  assert.equal(upperBound(values, 2), 4);
  assert.equal(lowerBound(values, 0), 0);
  assert.equal(lowerBound(values, 10), values.length);
  assert.equal(lowerBound(values, 3), 4);
  assert.equal(upperBound(values, 9), values.length);
  console.log("lower/upper for 2:", lowerBound(values, 2), upperBound(values, 2));

  console.log("\n--- exact search ---");
  assert.equal(binarySearch(values, 4), 4);
  assert.equal(binarySearch(values, 9), 7);
  assert.equal(binarySearch(values, 5), -1);
  assert.equal(binarySearch([], 1), -1);
  assert.equal(binarySearch([1], 1), 0);
  assert.equal(binarySearch([1], 2), -1);
  console.log("binarySearch(4):", binarySearch(values, 4), "| binarySearch(5):", binarySearch(values, 5));

  console.log("\n--- range of duplicates ---");
  assert.deepEqual(searchRange(values, 2), [1, 3]);
  assert.deepEqual(searchRange(values, 6), [5, 6]);
  assert.deepEqual(searchRange(values, 9), [7, 7]);
  assert.deepEqual(searchRange(values, 5), [-1, -1]);
  assert.deepEqual(searchRange([], 1), [-1, -1]);
  console.log("range of 2:", searchRange(values, 2));

  console.log("\n--- insertion point ---");
  assert.equal(searchInsert([1, 3, 5], 4), 2);
  assert.equal(searchInsert([1, 3, 5], 0), 0);
  assert.equal(searchInsert([1, 3, 5], 6), 3);
  assert.equal(searchInsert([1, 3, 5], 3), 1);
  console.log("insert 4 -> 2, insert 6 -> 3");

  console.log("\n--- first true ---");
  assert.equal(firstTrue((index) => index >= 3, 10), 3);
  assert.equal(firstTrue(() => false, 5), -1);
  assert.equal(firstTrue(() => true, 5), 0);
  console.log("first index >= 3:", firstTrue((index) => index >= 3, 10));

  console.log("\n--- integer square root ---");
  for (const value of [0, 1, 2, 3, 4, 8, 9, 15, 16, 99, 100]) {
    const root = integerSquareRoot(value);
    assert.ok(root * root <= value && (root + 1) * (root + 1) > value, `sqrt(${value}) = ${root}`);
  }
  assert.throws(() => integerSquareRoot(-1), RangeError);
  console.log("sqrt(99):", integerSquareRoot(99), "| sqrt(100):", integerSquareRoot(100));

  console.log("\n--- rotated array ---");
  const rotated = [4, 5, 6, 7, 9, 10, 1, 2];
  for (const target of rotated) {
    const index = rotatedSearch(rotated, target);
    assert.ok(index >= 0 && rotated[index] === target, `rotated search for ${target}`);
  }
  assert.equal(rotatedSearch(rotated, 3), -1);
  assert.equal(rotatedSearch(rotated, 8), -1);
  assert.equal(rotatedSearch([], 1), -1);
  assert.equal(rotatedSearch([1], 1), 0);
  console.log("all rotated targets found; missing values rejected");

  console.log("\n--- against linear search ---");
  let state = 23;
  const next = () => {
    state = (state * 1103515245 + 12345) % 2147483648;
    return state;
  };

  for (let trial = 0; trial < 300; trial += 1) {
    const length = next() % 40;
    const values = Array.from({ length }, () => next() % 50).sort((left, right) => left - right);

    for (let probe = 0; probe < 5; probe += 1) {
      const target = (next() % 60) - 5;
      const exact = values.indexOf(target);
      if (exact === -1) {
        assert.equal(binarySearch(values, target), -1);
      } else {
        assert.equal(values[binarySearch(values, target)], target);
      }

      const first = values.findIndex((value) => value >= target);
      assert.equal(lowerBound(values, target), first === -1 ? values.length : first);

      const firstGreater = values.findIndex((value) => value > target);
      assert.equal(upperBound(values, target), firstGreater === -1 ? values.length : firstGreater);

      const expectedRange = exact === -1
        ? [-1, -1]
        : [values.indexOf(target), values.lastIndexOf(target)];
      assert.deepEqual(searchRange(values, target), expectedRange);
      assert.equal(searchInsert(values, target), first === -1 ? values.length : first);
    }
  }
  console.log("300 random sorted arrays agree with linear scans");

  console.log("\n--- rotated property check ---");
  for (let trial = 0; trial < 200; trial += 1) {
    const length = 1 + (next() % 25);
    const base = Array.from({ length }, () => next() % 10_000);
    const unique = [...new Set(base)].sort((left, right) => left - right);
    if (unique.length === 0) {
      continue;
    }
    const pivot = next() % unique.length;
    const rotated = [...unique.slice(pivot), ...unique.slice(0, pivot)];

    for (const target of unique) {
      const index = rotatedSearch(rotated, target);
      assert.ok(index >= 0 && rotated[index] === target, `rotated search failed for ${target}`);
    }
    assert.equal(rotatedSearch(rotated, -1), -1);
  }
  console.log("200 random rotations found every member");

  console.log("\nAll checks passed.");
}

main();
