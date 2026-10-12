function assertIsString(value: unknown): asserts value is string {
  if (typeof value !== "string") {
    throw new TypeError(`Expected string but got ${typeof value}`);
  }
}

function assertDefined<T>(value: T | null | undefined): asserts value is T {
  if (value === null || value === undefined) {
    throw new Error("Value is not defined");
  }
}

function assertArray(value: unknown): asserts value is unknown[] {
  if (!Array.isArray(value)) {
    throw new TypeError("Expected an array");
  }
}

const inputs: unknown[] = ["hello", 42, ["a", "b"]];

for (const input of inputs) {
  try {
    assertIsString(input);
    console.log("string length:", input.length);
  } catch (error) {
    console.log("not a string:", (error as Error).message);
  }
}

const maybe: string | undefined = "present";
assertDefined(maybe);
console.log("defined value:", maybe.toUpperCase());

const list: unknown = [1, 2, 3];
assertArray(list);
console.log("array items:", list.length);
