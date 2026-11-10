const assert = require("node:assert/strict");

function typeName(value) {
  if (value === null) {
    return "null";
  }
  if (Array.isArray(value)) {
    return "array";
  }
  return typeof value;
}

function fail(errors, path, message) {
  errors.push({ path: path === "" ? "<root>" : path, message });
}

function matchesType(value, type) {
  if (type === "integer") {
    return Number.isInteger(value);
  }
  if (type === "number") {
    return typeof value === "number" && !Number.isNaN(value);
  }
  return typeName(value) === type;
}

function validateInto(value, schema, path, errors) {
  if (schema === undefined || schema === null) {
    return;
  }
  if (typeof schema !== "object" || Array.isArray(schema)) {
    fail(errors, path, "schema must be an object");
    return;
  }

  if (schema.type !== undefined) {
    const expected = schema.type;
    const known = ["string", "number", "integer", "boolean", "array", "object", "null"];
    if (!known.includes(expected)) {
      fail(errors, path, `unknown schema type ${JSON.stringify(expected)}`);
      return;
    }
    if (!matchesType(value, expected)) {
      const actual = Number.isInteger(value) ? "integer" : typeName(value);
      fail(errors, path, `expected ${expected}, got ${actual}`);
      return;
    }
  }

  if (schema.enum !== undefined) {
    if (!schema.enum.some((option) => Object.is(option, value))) {
      fail(errors, path, `must be one of ${schema.enum.map((option) => JSON.stringify(option)).join(", ")}`);
    }
  }

  if (typeof value === "string") {
    if (schema.minLength !== undefined && value.length < schema.minLength) {
      fail(errors, path, `must be at least ${schema.minLength} characters`);
    }
    if (schema.maxLength !== undefined && value.length > schema.maxLength) {
      fail(errors, path, `must be at most ${schema.maxLength} characters`);
    }
    if (schema.pattern !== undefined && !(schema.pattern instanceof RegExp)) {
      fail(errors, path, "pattern must be a RegExp");
    } else if (schema.pattern !== undefined && !schema.pattern.test(value)) {
      fail(errors, path, `must match ${schema.pattern}`);
    }
  }

  if (typeof value === "number") {
    if (schema.min !== undefined && value < schema.min) {
      fail(errors, path, `must be at least ${schema.min}`);
    }
    if (schema.max !== undefined && value > schema.max) {
      fail(errors, path, `must be at most ${schema.max}`);
    }
  }

  if (Array.isArray(value)) {
    if (schema.minItems !== undefined && value.length < schema.minItems) {
      fail(errors, path, `must have at least ${schema.minItems} items`);
    }
    if (schema.maxItems !== undefined && value.length > schema.maxItems) {
      fail(errors, path, `must have at most ${schema.maxItems} items`);
    }
    if (schema.uniqueItems === true) {
      const seen = new Set(value.map((item) => JSON.stringify(item)));
      if (seen.size !== value.length) {
        fail(errors, path, "must not contain duplicates");
      }
    }
    if (schema.items !== undefined) {
      value.forEach((item, index) => {
        validateInto(item, schema.items, `${path}[${index}]`, errors);
      });
    }
  }

  if (typeName(value) === "object") {
    const properties = schema.properties ?? {};
    const required = schema.required ?? [];

    for (const name of required) {
      if (!Object.prototype.hasOwnProperty.call(value, name)) {
        fail(errors, path === "" ? name : `${path}.${name}`, "is required");
      }
    }

    for (const [name, propertySchema] of Object.entries(properties)) {
      if (Object.prototype.hasOwnProperty.call(value, name)) {
        validateInto(value[name], propertySchema, path === "" ? name : `${path}.${name}`, errors);
      }
    }

    if (schema.additionalProperties === false) {
      for (const name of Object.keys(value)) {
        if (!Object.prototype.hasOwnProperty.call(properties, name)) {
          fail(errors, path === "" ? name : `${path}.${name}`, "is not allowed");
        }
      }
    }
  }
}

function validate(value, schema) {
  const errors = [];
  validateInto(value, schema, "", errors);
  return { valid: errors.length === 0, errors };
}

function show(result) {
  if (result.valid) {
    return "valid";
  }
  return result.errors.map((error) => `${error.path}: ${error.message}`).join(" | ");
}

function main() {
  const schema = {
    type: "object",
    required: ["name", "age"],
    additionalProperties: false,
    properties: {
      name: { type: "string", minLength: 2, maxLength: 40 },
      age: { type: "integer", min: 0, max: 150 },
      email: { type: "string", pattern: /@/ },
      role: { type: "string", enum: ["admin", "editor", "viewer"] },
      tags: {
        type: "array",
        maxItems: 3,
        uniqueItems: true,
        items: { type: "string", minLength: 1 },
      },
      address: {
        type: "object",
        required: ["city"],
        properties: {
          city: { type: "string" },
          zip: { type: "string", pattern: /^\d{5}$/ },
        },
      },
    },
  };

  console.log("--- a valid object ---");
  const validUser = {
    name: "Ada",
    age: 36,
    email: "ada@example.com",
    role: "admin",
    tags: ["math", "code"],
    address: { city: "London", zip: "12345" },
  };
  const valid = validate(validUser, schema);
  console.log(show(valid));
  assert.equal(valid.valid, true);

  console.log("\n--- every broken rule at once ---");
  const broken = {
    name: "A",
    age: 200,
    email: "nope",
    role: "owner",
    tags: ["x", "x", "y", "z"],
    address: { zip: "abc" },
    extra: true,
  };
  const failed = validate(broken, schema);
  console.log(show(failed));
  assert.equal(failed.valid, false);
  assert.deepEqual(
    failed.errors.map((error) => error.path),
    ["name", "age", "email", "role", "tags", "tags", "address.city", "address.zip", "extra"],
  );
  assert.ok(failed.errors[0].message.includes("at least 2"));
  assert.ok(failed.errors[1].message.includes("at most 150"));
  assert.deepEqual(failed.errors[4].path, "tags");

  console.log("\n--- missing required fields ---");
  const missing = validate({}, schema);
  console.log(show(missing));
  assert.deepEqual(missing.errors.map((error) => error.path), ["name", "age"]);

  console.log("\n--- type mismatches stop rule checks ---");
  const wrongTypes = validate(
    { name: 42, age: "old", tags: "not-an-array" },
    schema,
  );
  console.log(show(wrongTypes));
  assert.deepEqual(
    wrongTypes.errors.map((error) => error.message),
    ["expected string, got integer", "expected integer, got string", "expected array, got string"],
  );

  console.log("\n--- numbers and integers ---");
  assert.equal(validate(36, { type: "integer" }).valid, true);
  assert.equal(validate(36.5, { type: "integer" }).valid, false);
  assert.equal(validate(36.5, { type: "number" }).valid, true);
  assert.equal(validate(Number.NaN, { type: "number" }).valid, false);
  assert.equal(validate(Number.POSITIVE_INFINITY, { type: "number" }).valid, true);
  console.log("NaN is not a number for validation purposes; integers are whole");

  console.log("\n--- arrays ---");
  const arraySchema = {
    type: "array",
    items: { type: "number", min: 0 },
    minItems: 2,
    maxItems: 4,
  };
  assert.equal(validate([1, 2, 3], arraySchema).valid, true);
  const badArray = validate([1, -2, 3, 4, 5], arraySchema);
  console.log(show(badArray));
  assert.deepEqual(
    badArray.errors.map((error) => error.path),
    ["<root>", "[1]"],
  );

  const nestedArrays = validate([[1, -1]], {
    type: "array",
    items: { type: "array", items: { type: "number", min: 0 } },
  });
  assert.equal(nestedArrays.errors[0].path, "[0][1]");

  console.log("\n--- unique items ---");
  assert.equal(validate([1, 2, 3], { type: "array", uniqueItems: true }).valid, true);
  assert.equal(validate([1, 2, 1], { type: "array", uniqueItems: true }).valid, false);
  assert.equal(validate([{ a: 1 }, { a: 1 }], { type: "array", uniqueItems: true }).valid, false);
  console.log("duplicates are detected structurally");

  console.log("\n--- enums and patterns ---");
  assert.equal(validate("admin", { type: "string", enum: ["admin", "editor"] }).valid, true);
  assert.equal(validate("owner", { type: "string", enum: ["admin", "editor"] }).valid, false);
  assert.equal(validate("ABC-123", { type: "string", pattern: /^[A-Z]{3}-\d{3}$/ }).valid, true);
  assert.equal(validate("abc-123", { type: "string", pattern: /^[A-Z]{3}-\d{3}$/ }).valid, false);

  console.log("\n--- additional properties ---");
  const strict = { type: "object", properties: { a: { type: "number" } }, additionalProperties: false };
  assert.equal(validate({ a: 1 }, strict).valid, true);
  const extra = validate({ a: 1, b: 2, c: 3 }, strict);
  console.log(show(extra));
  assert.deepEqual(extra.errors.map((error) => error.path), ["b", "c"]);
  assert.equal(validate({ a: 1, b: 2 }, { type: "object", properties: { a: { type: "number" } } }).valid, true);

  console.log("\n--- empty and unknown schemas ---");
  assert.equal(validate("anything", {}).valid, true);
  assert.equal(validate(null, {}).valid, true);
  const unknown = validate(1, { type: "date" });
  console.log(show(unknown));
  assert.equal(unknown.valid, false);
  assert.ok(unknown.errors[0].message.includes("unknown schema type"));

  console.log("\n--- deep nesting ---");
  const deep = validate(
    { orders: [{ items: [{ price: -1 }] }] },
    {
      type: "object",
      properties: {
        orders: {
          type: "array",
          items: {
            type: "object",
            properties: {
              items: {
                type: "array",
                items: {
                  type: "object",
                  properties: { price: { type: "number", min: 0 } },
                },
              },
            },
          },
        },
      },
    },
  );
  console.log(show(deep));
  assert.equal(deep.errors[0].path, "orders[0].items[0].price");

  console.log("\nAll checks passed.");
}

main();
