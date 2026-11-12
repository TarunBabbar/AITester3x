const assert = require("node:assert/strict");

function parseLiteral(text) {
  const trimmed = text.trim();
  if (trimmed === "true") return true;
  if (trimmed === "false") return false;
  if (trimmed === "null") return null;
  if (/^-?\d+(\.\d+)?$/.test(trimmed)) return Number(trimmed);
  if (
    (trimmed.startsWith('"') && trimmed.endsWith('"')) ||
    (trimmed.startsWith("'") && trimmed.endsWith("'"))
  ) {
    return trimmed.slice(1, -1);
  }
  throw new Error(`unsupported literal ${trimmed}`);
}

function parseFilter(body) {
  const inner = body.trim().replace(/^\(\s*/, "").replace(/\s*\)$/, "");
  const match = /^@\.([A-Za-z_$][\w$]*)\s*(==|!=|<=|>=|<|>)\s*(.+)$/.exec(inner);
  if (match === null) {
    throw new Error(`unsupported filter ${body}`);
  }
  return { field: match[1], operator: match[2], value: parseLiteral(match[3]) };
}

function parsePath(path) {
  if (!path.startsWith("$")) {
    throw new Error("path must start with $");
  }

  const segments = [];
  let index = 1;

  while (index < path.length) {
    if (path.startsWith("..", index)) {
      index += 2;
      const match = /^([A-Za-z_$][\w$]*|\*)/.exec(path.slice(index));
      if (match === null) {
        throw new Error(`bad recursive segment at ${path.slice(index)}`);
      }
      segments.push({ type: "descend", name: match[1] });
      index += match[1].length;
      continue;
    }

    if (path[index] === ".") {
      index += 1;
      const match = /^([A-Za-z_$][\w$]*)/.exec(path.slice(index));
      if (match === null) {
        throw new Error(`bad property at ${path.slice(index)}`);
      }
      segments.push({ type: "child", name: match[1] });
      index += match[1].length;
      continue;
    }

    if (path[index] === "[") {
      let end = -1;
      let depth = 0;
      for (let scan = index; scan < path.length; scan += 1) {
        if (path[scan] === "[") {
          depth += 1;
        } else if (path[scan] === "]") {
          depth -= 1;
          if (depth === 0) {
            end = scan;
            break;
          }
        }
      }
      if (end === -1) {
        throw new Error("missing closing bracket");
      }

      const body = path.slice(index + 1, end).trim();
      index = end + 1;

      if (body === "*") {
        segments.push({ type: "wildcard" });
      } else if (body.startsWith("?")) {
        segments.push({ type: "filter", filter: parseFilter(body.slice(1)) });
      } else if (/^-?\d+$/.test(body)) {
        segments.push({ type: "index", index: Number(body) });
      } else if (/^-?\d*:-?\d*$/.test(body)) {
        const [startText, endText] = body.split(":");
        segments.push({
          type: "slice",
          start: startText === "" ? undefined : Number(startText),
          end: endText === "" ? undefined : Number(endText),
        });
      } else if (
        (body.startsWith("'") && body.endsWith("'")) ||
        (body.startsWith('"') && body.endsWith('"'))
      ) {
        segments.push({ type: "child", name: body.slice(1, -1) });
      } else {
        throw new Error(`unsupported bracket ${body}`);
      }
      continue;
    }

    throw new Error(`unexpected character ${path[index]}`);
  }

  return segments;
}

function collectDescendants(value, name, results) {
  const walk = (node) => {
    if (node === null || typeof node !== "object") {
      return;
    }
    if (Array.isArray(node)) {
      for (const item of node) {
        walk(item);
      }
      return;
    }
    for (const [key, child] of Object.entries(node)) {
      if (name === "*" || key === name) {
        results.push(child);
      }
      walk(child);
    }
  };

  if (Array.isArray(value)) {
    for (const item of value) {
      walk(item);
    }
  } else {
    walk(value);
  }
}

function matchesFilter(item, filter) {
  if (item === null || typeof item !== "object" || Array.isArray(item)) {
    return false;
  }
  if (!Object.prototype.hasOwnProperty.call(item, filter.field)) {
    return false;
  }

  const actual = item[filter.field];
  switch (filter.operator) {
    case "==":
      return actual === filter.value;
    case "!=":
      return actual !== filter.value;
    case "<":
      return actual < filter.value;
    case "<=":
      return actual <= filter.value;
    case ">":
      return actual > filter.value;
    case ">=":
      return actual >= filter.value;
    default:
      return false;
  }
}

function applySegment(segment, values) {
  const results = [];

  for (const value of values) {
    switch (segment.type) {
      case "child":
        if (
          value !== null &&
          typeof value === "object" &&
          !Array.isArray(value) &&
          Object.prototype.hasOwnProperty.call(value, segment.name)
        ) {
          results.push(value[segment.name]);
        }
        break;
      case "index":
        if (Array.isArray(value)) {
          const position = segment.index < 0 ? value.length + segment.index : segment.index;
          if (position >= 0 && position < value.length) {
            results.push(value[position]);
          }
        }
        break;
      case "wildcard":
        if (Array.isArray(value)) {
          results.push(...value);
        } else if (value !== null && typeof value === "object") {
          results.push(...Object.values(value));
        }
        break;
      case "slice":
        if (Array.isArray(value)) {
          results.push(...value.slice(segment.start, segment.end));
        }
        break;
      case "filter":
        if (Array.isArray(value)) {
          for (const item of value) {
            if (matchesFilter(item, segment.filter)) {
              results.push(item);
            }
          }
        }
        break;
      case "descend":
        collectDescendants(value, segment.name, results);
        break;
      default:
        break;
    }
  }

  return results;
}

function query(data, path) {
  return parsePath(path).reduce((values, segment) => applySegment(segment, values), [data]);
}

function main() {
  const store = {
    store: {
      book: [
        { title: "A", author: "X", price: 8.95, tags: ["tech"] },
        { title: "B", author: "Y", price: 12.99, tags: ["fiction", "classic"] },
        { title: "C", author: "X", price: 8.99 },
      ],
      bicycle: { color: "red", price: 19.95 },
    },
    authors: ["X", "Y"],
  };

  console.log("--- child access ---");
  assert.deepEqual(query(store, "$.store.bicycle.color"), ["red"]);
  assert.deepEqual(query(store, "$.store.book[0].title"), ["A"]);
  assert.deepEqual(query(store, "$.store['bicycle'].price"), [19.95]);
  console.log("$.store.bicycle.color ->", query(store, "$.store.bicycle.color"));

  console.log("\n--- indexes, wildcards, and slices ---");
  assert.deepEqual(query(store, "$.store.book[*].author"), ["X", "Y", "X"]);
  assert.deepEqual(query(store, "$.authors[1]"), ["Y"]);
  assert.deepEqual(query(store, "$.authors[-1]"), ["Y"]);
  assert.deepEqual(query(store, "$.store.book[0:2].title"), ["A", "B"]);
  assert.deepEqual(query(store, "$.store.book[1:].title"), ["B", "C"]);
  assert.deepEqual(query(store, "$.store.book[:1].title"), ["A"]);
  assert.equal(query(store, "$.store.book[*]").length, 3);
  console.log("wildcards and slices work, including negative indexes");

  console.log("\n--- filters ---");
  assert.deepEqual(query(store, "$.store.book[?(@.price < 10)].title"), ["A", "C"]);
  assert.deepEqual(query(store, "$.store.book[?(@.price >= 10)].title"), ["B"]);
  assert.deepEqual(query(store, "$.store.book[?(@.author == 'X')].title"), ["A", "C"]);
  assert.deepEqual(query(store, "$.store.book[?(@.author != 'X')].title"), ["B"]);
  assert.deepEqual(query(store, "$.store.book[?(@.price > 8.96)].title"), ["B", "C"]);
  assert.deepEqual(query(store, "$.store.book[?(@.missing == 1)]"), []);
  console.log("numeric and string comparisons filter arrays");

  console.log("\n--- recursive descent ---");
  assert.deepEqual(query(store, "$..author"), ["X", "Y", "X"]);
  assert.deepEqual(query(store, "$..price"), [8.95, 12.99, 8.99, 19.95]);
  assert.equal(query(store, "$..*").length > 10, true);
  console.log("$..author ->", query(store, "$..author"));

  console.log("\n--- missing data ---");
  assert.deepEqual(query(store, "$.store.magazine"), []);
  assert.deepEqual(query(store, "$.store.book[99]"), []);
  assert.deepEqual(query(store, "$.authors[0].name"), []);
  assert.deepEqual(query({ a: 1 }, "$.a.b.c"), []);
  console.log("missing paths produce empty results");

  console.log("\n--- root and scalars ---");
  assert.deepEqual(query(store, "$"), [store]);
  assert.deepEqual(query(42, "$"), [42]);
  assert.deepEqual(query({ a: 1 }, "$.a"), [1]);
  console.log("root selection returns the document");

  console.log("\n--- errors ---");
  const badPaths = [
    "store.book",
    "$.store.book[0",
    "$.store.book[?(@.price ~ 10)]",
    "$.store.book[foo]",
    "$.store.book[?(@.price <)]",
    "$.",
    "$.store.book[0]]",
  ];
  for (const path of badPaths) {
    assert.throws(() => query(store, path), Error, path);
    console.log("  rejected:", path);
  }

  console.log("\n--- nested descent through arrays ---");
  const nested = {
    groups: [
      { name: "one", members: [{ name: "a" }, { name: "b" }] },
      { name: "two", members: [{ name: "c" }] },
    ],
  };
  assert.deepEqual(query(nested, "$..name"), ["one", "a", "b", "two", "c"]);
  assert.deepEqual(query(nested, "$.groups[*].members[*].name"), ["a", "b", "c"]);
  console.log("descent visits objects inside arrays");

  console.log("\n--- filter on nested numeric fields ---");
  const orders = {
    orders: [
      { id: 1, total: 5, paid: true },
      { id: 2, total: 15, paid: false },
      { id: 3, total: 25, paid: true },
    ],
  };
  assert.deepEqual(query(orders, "$.orders[?(@.paid == true)].id"), [1, 3]);
  assert.deepEqual(query(orders, "$.orders[?(@.total <= 15)].id"), [1, 2]);
  console.log("boolean and numeric filters combine");

  console.log("\nAll checks passed.");
}

main();
