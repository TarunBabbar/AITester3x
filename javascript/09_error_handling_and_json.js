class ValidationError extends Error {
  constructor(field, message) {
    super(message);
    this.name = "ValidationError";
    this.field = field;
  }
}

function parseAge(value) {
  const age = Number(value);
  if (!Number.isFinite(age)) {
    throw new ValidationError("age", `"${value}" is not a number`);
  }
  if (age < 0) {
    throw new ValidationError("age", "age must not be negative");
  }
  return age;
}

function safeParseAge(value) {
  try {
    return { ok: true, age: parseAge(value) };
  } catch (error) {
    if (error instanceof ValidationError) {
      return { ok: false, field: error.field, reason: error.message };
    }
    throw error;
  } finally {
    console.log(`safeParseAge(${value}) handled`);
  }
}

const profile = {
  name: "Tarun",
  age: 30,
  skills: ["JavaScript", "Node.js"],
};

const json = JSON.stringify(profile);
console.log("json:", json);

const parsed = JSON.parse(json);
console.log("parsed name:", parsed.name, "| first skill:", parsed.skills[0]);

console.log("safe parse 42:", safeParseAge(42));
console.log("safe parse abc:", safeParseAge("abc"));
console.log("safe parse -5:", safeParseAge(-5));

const result = safeParseAge("25");
if (result.ok) {
  console.log("validated age:", result.age);
}

try {
  JSON.parse("{ invalid json }");
} catch (error) {
  console.log("syntax error:", error.name);
}
