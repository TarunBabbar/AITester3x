const assert = require("node:assert/strict");

function typeTag(value) {
  if (value === null) {
    return "null";
  }
  if (Array.isArray(value)) {
    return "array";
  }
  if (value instanceof Date) {
    return "date";
  }
  if (value instanceof RegExp) {
    return "regexp";
  }
  if (value instanceof Map) {
    return "map";
  }
  if (value instanceof Set) {
    return "set";
  }
  return typeof value;
}

function deepEqual(left, right, seen = new WeakMap()) {
  if (Object.is(left, right)) {
    return true;
  }
  if (typeof left !== "object" || typeof right !== "object" || left === null || right === null) {
    return false;
  }

  const tag = typeTag(left);
  if (tag !== typeTag(right)) {
    return false;
  }

  let visited = seen.get(left);
  if (visited?.has(right)) {
    return true;
  }
  if (!visited) {
    visited = new WeakSet();
    seen.set(left, visited);
  }
  visited.add(right);

  switch (tag) {
    case "date":
      return left.getTime() === right.getTime();
    case "regexp":
      return left.source === right.source && left.flags === right.flags;
    case "array":
      return left.length === right.length && left.every((value, index) => deepEqual(value, right[index], seen));
    case "map": {
      if (left.size !== right.size) {
        return false;
      }
      const unmatched = [...right.entries()];
      for (const [key, value] of left) {
        const index = unmatched.findIndex(
          ([otherKey, otherValue]) => deepEqual(key, otherKey, seen) && deepEqual(value, otherValue, seen),
        );
        if (index === -1) {
          return false;
        }
        unmatched.splice(index, 1);
      }
      return true;
    }
    case "set": {
      if (left.size !== right.size) {
        return false;
      }
      const unmatched = [...right];
      for (const value of left) {
        const index = unmatched.findIndex((other) => deepEqual(value, other, seen));
        if (index === -1) {
          return false;
        }
        unmatched.splice(index, 1);
      }
      return true;
    }
    default: {
      const keys = Object.keys(left);
      if (keys.length !== Object.keys(right).length) {
        return false;
      }
      return keys.every(
        (key) => Object.prototype.hasOwnProperty.call(right, key) && deepEqual(left[key], right[key], seen),
      );
    }
  }
}

function joinPath(path, key) {
  return path ? `${path}.${key}` : key;
}

function diff(left, right, path = "") {
  const changes = [];
  walk(left, right, path, changes);
  return changes;
}

function walk(left, right, path, changes) {
  if (deepEqual(left, right)) {
    return;
  }

  const leftTag = typeTag(left);
  const rightTag = typeTag(right);

  if (leftTag === "array" && rightTag === "array") {
    const length = Math.max(left.length, right.length);
    for (let index = 0; index < length; index += 1) {
      const childPath = `${path}[${index}]`;
      if (index >= left.length) {
        changes.push({ path: childPath, kind: "added", to: right[index] });
      } else if (index >= right.length) {
        changes.push({ path: childPath, kind: "removed", from: left[index] });
      } else {
        walk(left[index], right[index], childPath, changes);
      }
    }
    return;
  }

  if (leftTag === "object" && rightTag === "object") {
    const keys = new Set([...Object.keys(left), ...Object.keys(right)]);
    for (const key of keys) {
      const childPath = joinPath(path, key);
      if (!Object.prototype.hasOwnProperty.call(left, key)) {
        changes.push({ path: childPath, kind: "added", to: right[key] });
      } else if (!Object.prototype.hasOwnProperty.call(right, key)) {
        changes.push({ path: childPath, kind: "removed", from: left[key] });
      } else {
        walk(left[key], right[key], childPath, changes);
      }
    }
    return;
  }

  changes.push({ path: path || "<root>", kind: "changed", from: left, to: right });
}

function formatValue(value) {
  if (typeof value === "string") {
    return JSON.stringify(value);
  }
  if (value instanceof Date) {
    return `Date(${value.toISOString()})`;
  }
  if (value instanceof Map) {
    return `Map(${value.size})`;
  }
  if (value instanceof Set) {
    return `Set(${value.size})`;
  }
  if (Array.isArray(value)) {
    return `Array(${value.length})`;
  }
  if (value !== null && typeof value === "object") {
    return "{...}";
  }
  return String(value);
}

function formatDiff(changes) {
  return changes
    .map(({ path, kind, from, to }) => {
      if (kind === "added") {
        return `+ ${path} = ${formatValue(to)}`;
      }
      if (kind === "removed") {
        return `- ${path} (was ${formatValue(from)})`;
      }
      return `~ ${path}: ${formatValue(from)} -> ${formatValue(to)}`;
    })
    .join("\n");
}

function main() {
  console.log("--- deepEqual ---");
  const left = {
    id: 1,
    tags: ["a", "b"],
    meta: { active: true, score: NaN },
    when: new Date("2024-01-01T00:00:00Z"),
    lookup: new Map([[{ key: 1 }, "value"]]),
    flags: new Set([1, 2, 3]),
  };
  const right = {
    id: 1,
    tags: ["a", "b"],
    meta: { active: true, score: NaN },
    when: new Date("2024-01-01T00:00:00Z"),
    lookup: new Map([[{ key: 1 }, "value"]]),
    flags: new Set([3, 2, 1]),
  };

  console.log("structurally equal:", deepEqual(left, right));
  assert.equal(deepEqual(left, right), true);

  const checks = [
    ["identical primitives", deepEqual("x", "x"), true],
    ["NaN equals NaN", deepEqual(NaN, NaN), true],
    ["number vs string", deepEqual({ a: 1 }, { a: "1" }), false],
    ["nested arrays", deepEqual([1, [2, 3]], [1, [2, 3]]), true],
    ["array length", deepEqual([1, 2], [1, 2, 3]), false],
    ["set order ignored", deepEqual(new Set([1, 2]), new Set([2, 1])), true],
    ["map values", deepEqual(new Map([["a", 1]]), new Map([["a", 2]])), false],
    ["dates", deepEqual(new Date(0), new Date(1)), false],
    ["regex flags", deepEqual(/abc/gi, /abc/g), false],
    ["extra key", deepEqual({ a: 1 }, { a: 1, b: 2 }), false],
  ];
  for (const [label, actual, expected] of checks) {
    console.log(`${label}: ${actual}`);
    assert.equal(actual, expected, label);
  }

  const cyclicA = { name: "a" };
  cyclicA.self = cyclicA;
  const cyclicB = { name: "a" };
  cyclicB.self = cyclicB;
  console.log("cyclic graphs equal:", deepEqual(cyclicA, cyclicB));
  assert.equal(deepEqual(cyclicA, cyclicB), true);

  console.log("\n--- diff ---");
  const before = {
    name: "Ada",
    role: "engineer",
    skills: ["js", "sql"],
    address: { city: "Delhi", zip: "110001" },
  };
  const after = {
    name: "Ada Lovelace",
    role: "engineer",
    skills: ["js", "rust", "sql"],
    address: { city: "London" },
  };

  const changes = diff(before, after);
  console.log(formatDiff(changes));
  console.log("change count:", changes.length);

  assert.deepEqual(
    changes.map((change) => change.path),
    ["name", "skills[1]", "skills[2]", "address.city", "address.zip"],
  );
  assert.deepEqual(changes.map((change) => change.kind), ["changed", "changed", "added", "changed", "removed"]);

  console.log("\n--- no changes ---");
  console.log("empty diff:", diff({ a: [1, 2] }, { a: [1, 2] }).length);

  console.log("\n--- root replacement ---");
  const rootChange = diff(1, "one");
  console.log(formatDiff(rootChange));
  assert.deepEqual(rootChange, [{ path: "<root>", kind: "changed", from: 1, to: "one" }]);

  console.log("\nAll checks passed.");
}

main();
