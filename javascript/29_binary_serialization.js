const assert = require("node:assert/strict");

const TAG_NULL = 0;
const TAG_FALSE = 1;
const TAG_TRUE = 2;
const TAG_FLOAT = 3;
const TAG_STRING = 4;
const TAG_ARRAY = 5;
const TAG_OBJECT = 6;

function writeVarint(bytes, value) {
  if (!Number.isInteger(value) || value < 0) {
    throw new RangeError("varint values must be non-negative integers");
  }
  while (value >= 0x80) {
    bytes.push((value & 0x7f) | 0x80);
    value = Math.floor(value / 128);
  }
  bytes.push(value);
}

function readVarint(buffer, state) {
  let result = 0;
  let shift = 0;

  while (true) {
    if (state.offset >= buffer.length) {
      throw new Error("unexpected end of buffer while reading a length");
    }
    const byte = buffer[state.offset];
    state.offset += 1;
    result += (byte & 0x7f) * 2 ** shift;

    if (result > Number.MAX_SAFE_INTEGER) {
      throw new Error("length is too large");
    }
    if ((byte & 0x80) === 0) {
      return result;
    }

    shift += 7;
    if (shift > 49) {
      throw new Error("length encoding is too long");
    }
  }
}

function writeValue(bytes, value, seen) {
  if (value === null) {
    bytes.push(TAG_NULL);
    return;
  }
  if (value === false) {
    bytes.push(TAG_FALSE);
    return;
  }
  if (value === true) {
    bytes.push(TAG_TRUE);
    return;
  }

  if (typeof value === "number") {
    bytes.push(TAG_FLOAT);
    const scratch = Buffer.allocUnsafe(8);
    scratch.writeDoubleLE(value, 0);
    for (const byte of scratch) {
      bytes.push(byte);
    }
    return;
  }

  if (typeof value === "string") {
    bytes.push(TAG_STRING);
    const encoded = Buffer.from(value, "utf8");
    writeVarint(bytes, encoded.length);
    for (const byte of encoded) {
      bytes.push(byte);
    }
    return;
  }

  if (Array.isArray(value)) {
    if (seen.has(value)) {
      throw new Error("cannot encode a cyclic structure");
    }
    seen.add(value);
    bytes.push(TAG_ARRAY);
    writeVarint(bytes, value.length);
    for (const item of value) {
      writeValue(bytes, item, seen);
    }
    seen.delete(value);
    return;
  }

  if (typeof value === "object") {
    if (seen.has(value)) {
      throw new Error("cannot encode a cyclic structure");
    }
    seen.add(value);
    const keys = Object.keys(value);
    bytes.push(TAG_OBJECT);
    writeVarint(bytes, keys.length);
    for (const key of keys) {
      writeValue(bytes, key, seen);
      writeValue(bytes, value[key], seen);
    }
    seen.delete(value);
    return;
  }

  throw new TypeError(`unsupported type: ${typeof value}`);
}

function encode(value) {
  const bytes = [];
  writeValue(bytes, value, new Set());
  return Buffer.from(bytes);
}

function readValue(buffer, state) {
  if (state.offset >= buffer.length) {
    throw new Error("unexpected end of buffer");
  }

  const tag = buffer[state.offset];
  state.offset += 1;

  switch (tag) {
    case TAG_NULL:
      return null;
    case TAG_FALSE:
      return false;
    case TAG_TRUE:
      return true;
    case TAG_FLOAT: {
      if (state.offset + 8 > buffer.length) {
        throw new Error("unexpected end of buffer in a number");
      }
      const value = buffer.readDoubleLE(state.offset);
      state.offset += 8;
      return value;
    }
    case TAG_STRING: {
      const length = readVarint(buffer, state);
      if (state.offset + length > buffer.length) {
        throw new Error("unexpected end of buffer in a string");
      }
      const value = buffer.toString("utf8", state.offset, state.offset + length);
      state.offset += length;
      return value;
    }
    case TAG_ARRAY: {
      const length = readVarint(buffer, state);
      if (length > buffer.length) {
        throw new Error("array length exceeds the buffer");
      }
      const values = [];
      for (let index = 0; index < length; index += 1) {
        values.push(readValue(buffer, state));
      }
      return values;
    }
    case TAG_OBJECT: {
      const count = readVarint(buffer, state);
      if (count > buffer.length) {
        throw new Error("object size exceeds the buffer");
      }
      const result = {};
      for (let index = 0; index < count; index += 1) {
        const key = readValue(buffer, state);
        if (typeof key !== "string") {
          throw new Error("object keys must be strings");
        }
        result[key] = readValue(buffer, state);
      }
      return result;
    }
    default:
      throw new Error(`unknown tag ${tag}`);
  }
}

function decode(buffer) {
  if (!Buffer.isBuffer(buffer)) {
    throw new TypeError("decode expects a Buffer");
  }
  const state = { offset: 0 };
  const value = readValue(buffer, state);
  if (state.offset !== buffer.length) {
    throw new Error(`trailing data after the value (${buffer.length - state.offset} bytes)`);
  }
  return value;
}

function deepCopyCheck(actual, expected) {
  assert.deepEqual(actual, expected);
}

function sampleValue(random, depth) {
  const choice = random() % 6;

  if (depth <= 0 || choice === 0) {
    return null;
  }
  if (choice === 1) {
    return random() % 2 === 0;
  }
  if (choice === 2) {
    return (random() % 2000) - 1000;
  }
  if (choice === 3) {
    const alphabet = "abcXYZ019 ";
    const length = random() % 12;
    let text = "";
    for (let index = 0; index < length; index += 1) {
      text += alphabet[random() % alphabet.length];
    }
    return text;
  }
  if (choice === 4) {
    const length = random() % 5;
    const items = [];
    for (let index = 0; index < length; index += 1) {
      items.push(sampleValue(random, depth - 1));
    }
    return items;
  }

  const size = random() % 4;
  const result = {};
  for (let index = 0; index < size; index += 1) {
    result[`key${index}`] = sampleValue(random, depth - 1);
  }
  return result;
}

function main() {
  console.log("--- primitives ---");
  const primitives = [
    null,
    true,
    false,
    0,
    -0,
    42,
    -17.5,
    Number.NaN,
    Number.POSITIVE_INFINITY,
    Number.NEGATIVE_INFINITY,
    "",
    "hello",
    "héllo wörld",
    "emoji 🚀",
  ];
  for (const value of primitives) {
    const decoded = decode(encode(value));
    assert.ok(Object.is(decoded, value), `round trip failed for ${String(value)}`);
  }
  console.log("null, booleans, numbers (including -0 and NaN), and strings round-trip");

  console.log("\n--- nested structures ---");
  const nested = {
    name: "Ada",
    age: 36,
    active: true,
    scores: [1, 2.5, NaN],
    address: { city: "London", zip: null },
    tags: [],
    meta: { nested: { deep: [ { value: "x" } ] } },
  };
  const decodedNested = decode(encode(nested));
  deepCopyCheck(decodedNested, nested);
  assert.ok(Object.is(decodedNested.scores[2], NaN), "NaN survives");
  assert.deepEqual(Object.keys(decodedNested), Object.keys(nested));
  console.log(JSON.stringify(decodedNested));

  console.log("\n--- deterministic bytes ---");
  const first = encode(nested);
  const second = encode(nested);
  assert.ok(first.equals(second), "encoding is deterministic");
  console.log("encoded size:", first.length, "bytes | JSON size:", Buffer.byteLength(JSON.stringify(nested)), "bytes");

  console.log("\n--- empty containers ---");
  assert.deepEqual(decode(encode([])), []);
  assert.deepEqual(decode(encode({})), {});
  assert.equal(encode([]).length, 2);
  console.log("empty array and object use two bytes");

  console.log("\n--- long strings use multi-byte lengths ---");
  const long = "x".repeat(10_000);
  const longDecoded = decode(encode(long));
  assert.equal(longDecoded.length, 10_000);
  assert.equal(longDecoded, long);
  console.log("10,000 character string round-tripped");

  console.log("\n--- random values ---");
  let state = 29;
  const next = () => {
    state = (state * 1103515245 + 12345) % 2147483648;
    return state;
  };

  for (let trial = 0; trial < 200; trial += 1) {
    const value = sampleValue(next, 3);
    deepCopyCheck(decode(encode(value)), value);
  }
  console.log("200 random nested values round-tripped");

  console.log("\n--- failures ---");
  assert.throws(() => encode(undefined), TypeError);
  assert.throws(() => encode(() => 1), TypeError);
  assert.throws(() => encode(BigInt(1)), TypeError);
  assert.throws(() => encode(Symbol("x")), TypeError);

  const cyclic = {};
  cyclic.self = cyclic;
  assert.throws(() => encode(cyclic), /cyclic/);

  const truncated = encode({ a: [1, 2, 3] }).subarray(0, 6);
  assert.throws(() => decode(truncated), /unexpected end/);
  assert.throws(() => decode(Buffer.from([99])), /unknown tag/);
  assert.throws(() => decode(Buffer.concat([encode(1), Buffer.from([0])])), /trailing data/);
  assert.throws(() => decode("not a buffer"), TypeError);
  console.log("unsupported types, cycles, truncation, bad tags, and trailing data rejected");

  console.log("\n--- key order is preserved ---");
  const ordered = { z: 1, a: 2, m: 3 };
  assert.deepEqual(Object.keys(decode(encode(ordered))), ["z", "a", "m"]);
  console.log("keys:", Object.keys(decode(encode(ordered))).join(", "));

  console.log("\nAll checks passed.");
}

main();
