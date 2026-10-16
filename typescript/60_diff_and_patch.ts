type Dictionary = Record<string, unknown>;

type Patch =
  | { op: "set"; key: string; value: unknown }
  | { op: "remove"; key: string };

function diff(before: Dictionary, after: Dictionary): Patch[] {
  const patches: Patch[] = [];

  for (const key of Object.keys(after)) {
    if (before[key] !== after[key]) {
      patches.push({ op: "set", key, value: after[key] });
    }
  }

  for (const key of Object.keys(before)) {
    if (!(key in after)) {
      patches.push({ op: "remove", key });
    }
  }

  return patches;
}

function apply(target: Dictionary, patches: Patch[]): Dictionary {
  const result: Dictionary = { ...target };
  for (const patch of patches) {
    if (patch.op === "set") {
      result[patch.key] = patch.value;
    } else {
      delete result[patch.key];
    }
  }
  return result;
}

function formatPatch(patches: Patch[]): string {
  return patches
    .map((patch) =>
      patch.op === "set"
        ? `set ${patch.key}=${JSON.stringify(patch.value)}`
        : `remove ${patch.key}`,
    )
    .join(", ");
}

const before: Dictionary = { name: "Tarun", age: 30, city: "Delhi", temp: true };
const after: Dictionary = { name: "Tarun", age: 31, city: "Gurgaon", role: "admin" };

const patches = diff(before, after);
console.log("patches:", formatPatch(patches));

const rebuilt = apply(before, patches);
console.log("rebuilt:", rebuilt);
console.log("matches after:", JSON.stringify(rebuilt) === JSON.stringify({ ...after }));

console.log("original unchanged:", before.age, before.city);
