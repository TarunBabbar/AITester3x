const assert = require("node:assert/strict");

function validate(interval, name = "interval") {
  if (!Array.isArray(interval) || interval.length !== 2) {
    throw new TypeError(`${name} must be a [start, end] pair`);
  }
  const [start, end] = interval;
  if (!Number.isFinite(start) || !Number.isFinite(end)) {
    throw new TypeError(`${name} bounds must be finite numbers`);
  }
  if (start > end) {
    throw new RangeError(`${name} starts after it ends: [${start}, ${end}]`);
  }
}

function validateAll(intervals) {
  if (!Array.isArray(intervals)) {
    throw new TypeError("intervals must be an array");
  }
  intervals.forEach((interval, index) => validate(interval, `interval ${index}`));
}

function mergeIntervals(intervals) {
  validateAll(intervals);
  const sorted = intervals.map(([start, end]) => [start, end]).sort((a, b) => a[0] - b[0] || a[1] - b[1]);
  const merged = [];

  for (const [start, end] of sorted) {
    const last = merged[merged.length - 1];
    if (last !== undefined && start <= last[1]) {
      last[1] = Math.max(last[1], end);
    } else {
      merged.push([start, end]);
    }
  }

  return merged;
}

function insertInterval(intervals, interval) {
  validate(interval, "new interval");
  return mergeIntervals([...intervals, interval]);
}

function intersectIntervals(left, right) {
  const first = mergeIntervals(left);
  const second = mergeIntervals(right);
  const result = [];
  let a = 0;
  let b = 0;

  while (a < first.length && b < second.length) {
    const start = Math.max(first[a][0], second[b][0]);
    const end = Math.min(first[a][1], second[b][1]);
    if (start <= end) {
      result.push([start, end]);
    }
    if (first[a][1] < second[b][1]) {
      a += 1;
    } else {
      b += 1;
    }
  }

  return result;
}

/** Half-open semantics: an interval ending at t does not occupy t. */
function maxOverlaps(intervals) {
  validateAll(intervals);
  const events = [];
  for (const [start, end] of intervals) {
    events.push({ time: start, delta: 1 });
    events.push({ time: end, delta: -1 });
  }
  events.sort((left, right) => left.time - right.time || left.delta - right.delta);

  let active = 0;
  let peak = 0;
  for (const event of events) {
    active += event.delta;
    peak = Math.max(peak, active);
  }
  return peak;
}

function canAttendAll(intervals) {
  validateAll(intervals);
  const sorted = intervals.map(([start, end]) => [start, end]).sort((a, b) => a[0] - b[0] || a[1] - b[1]);

  for (let index = 1; index < sorted.length; index += 1) {
    if (sorted[index][0] < sorted[index - 1][1]) {
      return false;
    }
  }
  return true;
}

/** Greedy: pick the interval that ends earliest, then repeat. */
function scheduleMaximum(intervals) {
  validateAll(intervals);
  const sorted = intervals.map(([start, end]) => [start, end]).sort((a, b) => a[1] - b[1] || a[0] - b[0]);
  const chosen = [];

  for (const interval of sorted) {
    if (chosen.length === 0 || interval[0] >= chosen[chosen.length - 1][1]) {
      chosen.push(interval);
    }
  }

  return chosen;
}

function totalCovered(intervals) {
  return mergeIntervals(intervals).reduce((total, [start, end]) => total + (end - start), 0);
}

function coversPoint(intervals, point) {
  return intervals.some(([start, end]) => start <= point && point <= end);
}

function bruteMaxOverlaps(intervals) {
  if (intervals.length === 0) {
    return 0;
  }
  const points = new Set();
  for (const [start, end] of intervals) {
    points.add(start);
    points.add(end);
  }
  let peak = 0;
  for (const point of points) {
    const count = intervals.filter(([start, end]) => start <= point && point < end).length;
    peak = Math.max(peak, count);
  }
  return peak;
}

function bruteScheduleMaximum(intervals) {
  let best = 0;
  const count = intervals.length;
  for (let mask = 0; mask < 1 << count; mask += 1) {
    const chosen = [];
    let valid = true;
    for (let index = 0; index < count; index += 1) {
      if ((mask & (1 << index)) === 0) {
        continue;
      }
      for (const other of chosen) {
        if (intervals[index][0] < other[1] && other[0] < intervals[index][1]) {
          valid = false;
          break;
        }
      }
      if (!valid) {
        break;
      }
      chosen.push(intervals[index]);
    }
    if (valid) {
      best = Math.max(best, chosen.length);
    }
  }
  return best;
}

function main() {
  console.log("--- merging ---");
  assert.deepEqual(
    mergeIntervals([[1, 3], [2, 6], [8, 10], [15, 18]]),
    [[1, 6], [8, 10], [15, 18]],
  );
  assert.deepEqual(mergeIntervals([[1, 4], [4, 5]]), [[1, 5]]);
  assert.deepEqual(mergeIntervals([[1, 10], [2, 3], [4, 5]]), [[1, 10]]);
  assert.deepEqual(mergeIntervals([[5, 6], [1, 2]]), [[1, 2], [5, 6]]);
  assert.deepEqual(mergeIntervals([]), []);
  assert.deepEqual(mergeIntervals([[3, 3]]), [[3, 3]]);
  console.log("merge:", JSON.stringify(mergeIntervals([[1, 3], [2, 6], [8, 10], [15, 18]])));

  console.log("\n--- inserting ---");
  assert.deepEqual(insertInterval([[1, 3], [6, 9]], [2, 5]), [[1, 5], [6, 9]]);
  assert.deepEqual(insertInterval([[1, 2], [3, 5], [6, 7], [8, 10], [12, 16]], [4, 8]), [
    [1, 2], [3, 10], [12, 16],
  ]);
  assert.deepEqual(insertInterval([], [4, 8]), [[4, 8]]);
  assert.deepEqual(insertInterval([[1, 5]], [6, 8]), [[1, 5], [6, 8]]);
  console.log("insertion merges when needed and keeps gaps otherwise");

  console.log("\n--- intersections ---");
  assert.deepEqual(intersectIntervals([[0, 2], [5, 10], [13, 23], [24, 25]], [[1, 5], [8, 12], [15, 24], [25, 26]]), [
    [1, 2], [5, 5], [8, 10], [15, 23], [24, 24], [25, 25],
  ]);
  assert.deepEqual(intersectIntervals([[1, 3]], [[4, 6]]), []);
  assert.deepEqual(intersectIntervals([], [[1, 2]]), []);
  console.log("intersections:", JSON.stringify(intersectIntervals([[0, 5]], [[2, 3], [4, 6]])));

  console.log("\n--- overlap counting (half-open) ---");
  assert.equal(maxOverlaps([[0, 30], [5, 10], [15, 20]]), 2);
  assert.equal(maxOverlaps([[0, 5], [5, 10]]), 1);
  assert.equal(maxOverlaps([[0, 5], [4, 10]]), 2);
  assert.equal(maxOverlaps([]), 0);
  assert.equal(maxOverlaps([[1, 2], [2, 3], [3, 4]]), 1);
  console.log("peak concurrency:", maxOverlaps([[0, 30], [5, 10], [15, 20]]));

  console.log("\n--- scheduling ---");
  assert.equal(canAttendAll([[0, 30], [5, 10]]), false);
  assert.equal(canAttendAll([[0, 5], [5, 10]]), true);
  assert.equal(canAttendAll([]), true);
  assert.deepEqual(scheduleMaximum([[1, 2], [2, 3], [3, 4], [1, 3]]), [[1, 2], [2, 3], [3, 4]]);
  assert.deepEqual(scheduleMaximum([[1, 4], [2, 5], [3, 6]]), [[1, 4]]);
  console.log("greedy picks:", JSON.stringify(scheduleMaximum([[1, 2], [2, 3], [3, 4], [1, 3]])));

  console.log("\n--- coverage ---");
  assert.equal(totalCovered([[1, 3], [2, 6], [8, 10]]), 7);
  assert.equal(totalCovered([]), 0);
  assert.equal(coversPoint([[1, 3], [5, 7]], 2), true);
  assert.equal(coversPoint([[1, 3], [5, 7]], 4), false);
  console.log("total covered length:", totalCovered([[1, 3], [2, 6], [8, 10]]));

  console.log("\n--- validation ---");
  const invalid = [
    () => mergeIntervals([[3, 1]]),
    () => mergeIntervals([[1]]),
    () => mergeIntervals([1, 2]),
    () => mergeIntervals([[Number.NaN, 2]]),
    () => insertInterval([[1, 2]], [5, 4]),
    () => scheduleMaximum([[1, 2], "x"]),
  ];
  for (const attempt of invalid) {
    assert.throws(attempt, Error);
  }
  console.log("bad intervals are rejected");

  console.log("\n--- random properties ---");
  let state = 31;
  const next = () => {
    state = (state * 1103515245 + 12345) % 2147483648;
    return state;
  };

  for (let trial = 0; trial < 300; trial += 1) {
    const count = next() % 9;
    const intervals = [];
    for (let index = 0; index < count; index += 1) {
      const start = next() % 20;
      const end = start + (next() % 6);
      intervals.push([start, end]);
    }

    const merged = mergeIntervals(intervals);
    for (let index = 1; index < merged.length; index += 1) {
      assert.ok(merged[index - 1][1] < merged[index][0], "merged intervals must be disjoint");
    }
    for (let point = 0; point <= 25; point += 1) {
      assert.equal(
        coversPoint(merged, point),
        coversPoint(intervals, point),
        `coverage mismatch at ${point}`,
      );
    }

    assert.equal(maxOverlaps(intervals), bruteMaxOverlaps(intervals), "overlap count");
    assert.equal(
      scheduleMaximum(intervals).length,
      bruteScheduleMaximum(intervals),
      "greedy scheduling must be optimal",
    );
  }
  console.log("300 random interval sets satisfy every property");

  console.log("\n--- a larger input ---");
  const large = [];
  for (let index = 0; index < 10_000; index += 1) {
    const start = next() % 1_000_000;
    large.push([start, start + (next() % 1_000)]);
  }
  const largeMerged = mergeIntervals(large);
  console.log("10,000 intervals merged into", largeMerged.length, "blocks");
  assert.ok(largeMerged.length <= large.length);
  assert.ok(totalCovered(largeMerged) >= 0);

  console.log("\nAll checks passed.");
}

main();
