type Validation<T> = { ok: true; value: T } | { ok: false; errors: string[] };

function success<T>(value: T): Validation<T> {
  return { ok: true, value };
}

function failure<T>(...errors: string[]): Validation<T> {
  return { ok: false, errors };
}

function string(name: string): (value: unknown) => Validation<string> {
  return (value) => (typeof value === "string" ? success(value) : failure(`${name} must be a string`));
}

function number(name: string): (value: unknown) => Validation<number> {
  return (value) =>
    typeof value === "number" && !Number.isNaN(value)
      ? success(value)
      : failure(`${name} must be a number`);
}

function minLength(length: number): (value: string) => Validation<string> {
  return (value) =>
    value.length >= length
      ? success(value)
      : failure(`expected at least ${length} characters, got ${value.length}`);
}

interface User {
  name: string;
  age: number;
}

function validateUser(input: unknown): Validation<User> {
  if (typeof input !== "object" || input === null) {
    return failure("input must be an object");
  }
  const record = input as Record<string, unknown>;
  const errors: string[] = [];

  const nameResult = string("name")(record.name);
  const ageResult = number("age")(record.age);

  if (!nameResult.ok) {
    errors.push(...nameResult.errors);
  }
  if (!ageResult.ok) {
    errors.push(...ageResult.errors);
  }

  if (errors.length > 0) {
    return failure(...errors);
  }

  return success({ name: nameResult.value, age: ageResult.value });
}

const candidates: unknown[] = [
  { name: "Tarun", age: 30 },
  { name: "Ravi", age: "old" },
  { name: 42, age: null },
  "not an object",
];

for (const candidate of candidates) {
  const result = validateUser(candidate);
  console.log(result.ok ? `valid: ${JSON.stringify(result.value)}` : `invalid: ${result.errors.join("; ")}`);
}

console.log("minLength ok:", minLength(3)("hello"));
console.log("minLength fail:", minLength(8)("hi").ok);
