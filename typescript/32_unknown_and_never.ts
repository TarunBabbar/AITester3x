function processInput(value: unknown): string {
  if (typeof value === "string") {
    return `string: ${value}`;
  }
  if (typeof value === "number") {
    return `number: ${value}`;
  }
  if (Array.isArray(value)) {
    return `array of ${value.length}`;
  }
  if (value !== null && typeof value === "object") {
    return `object with ${Object.keys(value).length} keys`;
  }
  return `other: ${String(value)}`;
}

function assertNever(value: never): never {
  throw new Error(`Unhandled case: ${JSON.stringify(value)}`);
}

type Action = "create" | "read" | "update" | "delete";

function describeAction(action: Action): string {
  switch (action) {
    case "create":
      return "creating";
    case "read":
      return "reading";
    case "update":
      return "updating";
    case "delete":
      return "deleting";
    default:
      return assertNever(action);
  }
}

const values: unknown[] = ["hello", 42, [1, 2, 3], { a: 1, b: 2 }, null, true];

for (const value of values) {
  console.log(processInput(value));
}

const actions: Action[] = ["create", "read", "update", "delete"];
for (const action of actions) {
  console.log(`${action} -> ${describeAction(action)}`);
}
