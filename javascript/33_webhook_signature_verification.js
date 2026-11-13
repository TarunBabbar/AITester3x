const crypto = require("node:crypto");
const assert = require("node:assert/strict");

function hmacSha256Hex(secret, message) {
  return crypto.createHmac("sha256", secret).update(message, "utf8").digest("hex");
}

function computeSignature(secret, timestamp, body) {
  return hmacSha256Hex(secret, `${timestamp}.${body}`);
}

function constantTimeEqual(left, right) {
  if (typeof left !== "string" || typeof right !== "string") {
    return false;
  }
  const leftBytes = Buffer.from(left, "utf8");
  const rightBytes = Buffer.from(right, "utf8");
  if (leftBytes.length !== rightBytes.length) {
    return false;
  }
  return crypto.timingSafeEqual(leftBytes, rightBytes);
}

function safeEqualHex(left, right) {
  if (typeof left !== "string" || typeof right !== "string") {
    return false;
  }
  if (!/^[0-9a-fA-F]+$/.test(left) || !/^[0-9a-fA-F]+$/.test(right)) {
    return false;
  }
  if (left.length !== right.length || left.length % 2 !== 0) {
    return false;
  }
  return constantTimeEqual(left.toLowerCase(), right.toLowerCase());
}

class WebhookVerifier {
  #secret;
  #toleranceMs;
  #now;

  constructor({ secret, toleranceMs = 300_000, now = Date.now } = {}) {
    if (typeof secret !== "string" || secret.length === 0) {
      throw new TypeError("secret must be a non-empty string");
    }
    if (!Number.isFinite(toleranceMs) || toleranceMs < 0) {
      throw new RangeError("toleranceMs must be zero or a positive number");
    }
    if (typeof now !== "function") {
      throw new TypeError("now must be a function");
    }
    this.#secret = secret;
    this.#toleranceMs = toleranceMs;
    this.#now = now;
  }

  sign(timestamp, body) {
    return computeSignature(this.#secret, timestamp, body);
  }

  verify({ timestamp, body, signature } = {}) {
    if (typeof body !== "string") {
      return { valid: false, reason: "body must be a string" };
    }
    if (!Number.isInteger(timestamp) || timestamp < 0) {
      return { valid: false, reason: "invalid timestamp" };
    }

    const skew = Math.abs(this.#now() - timestamp);
    if (skew > this.#toleranceMs) {
      return { valid: false, reason: "timestamp outside tolerance" };
    }

    const expected = this.sign(timestamp, body);
    if (!safeEqualHex(signature, expected)) {
      return { valid: false, reason: "signature mismatch" };
    }

    return { valid: true, reason: "ok" };
  }
}

class ReplayGuard {
  #ttlMs;
  #now;
  #seen = new Map();

  constructor({ ttlMs = 300_000, now = Date.now } = {}) {
    if (!Number.isFinite(ttlMs) || ttlMs < 0) {
      throw new RangeError("ttlMs must be zero or a positive number");
    }
    if (typeof now !== "function") {
      throw new TypeError("now must be a function");
    }
    this.#ttlMs = ttlMs;
    this.#now = now;
  }

  get size() {
    this.#prune();
    return this.#seen.size;
  }

  /** True the first time a key is seen, false for a replay. */
  check(key) {
    if (typeof key !== "string" || key.length === 0) {
      throw new TypeError("replay key must be a non-empty string");
    }
    this.#prune();
    if (this.#seen.has(key)) {
      return false;
    }
    this.#seen.set(key, this.#now() + this.#ttlMs);
    return true;
  }

  #prune() {
    const now = this.#now();
    for (const [key, expiresAt] of this.#seen) {
      if (expiresAt <= now) {
        this.#seen.delete(key);
      }
    }
  }
}

function main() {
  console.log("--- the hmac primitive ---");
  const known = hmacSha256Hex("key", "The quick brown fox jumps over the lazy dog");
  console.log("known vector:", known);
  assert.equal(known, "f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8");
  assert.notEqual(hmacSha256Hex("key", "different"), known);
  assert.notEqual(hmacSha256Hex("other", "The quick brown fox jumps over the lazy dog"), known);

  console.log("\n--- constant-time comparison ---");
  assert.equal(constantTimeEqual("abc", "abc"), true);
  assert.equal(constantTimeEqual("abc", "abd"), false);
  assert.equal(constantTimeEqual("abc", "abcd"), false);
  assert.equal(safeEqualHex("00ff", "00FF"), true);
  assert.equal(safeEqualHex("00ff", "0f"), false);
  assert.equal(safeEqualHex("zz", "zz"), false);
  assert.equal(safeEqualHex("abc", "abd"), false);
  assert.equal(safeEqualHex(null, "abc"), false);
  console.log("equal strings match, length and malformed hex are rejected");

  console.log("\n--- signing and verifying ---");
  let now = 1_700_000_000_000;
  const verifier = new WebhookVerifier({ secret: "s3cret", toleranceMs: 60_000, now: () => now });
  const body = JSON.stringify({ event: "order.created", id: 42 });
  const timestamp = now;
  const signature = verifier.sign(timestamp, body);

  const accepted = verifier.verify({ timestamp, body, signature });
  console.log("valid delivery:", accepted);
  assert.deepEqual(accepted, { valid: true, reason: "ok" });

  console.log("\n--- rejection cases ---");
  const wrongSecret = new WebhookVerifier({ secret: "wrong", toleranceMs: 60_000, now: () => now });
  assert.equal(wrongSecret.verify({ timestamp, body, signature }).reason, "signature mismatch");
  assert.equal(verifier.verify({ timestamp, body: body + " ", signature }).reason, "signature mismatch");
  assert.equal(verifier.verify({ timestamp, body, signature: signature.toUpperCase() }).reason, "ok");

  now += 60_001;
  assert.equal(verifier.verify({ timestamp, body, signature }).reason, "timestamp outside tolerance");
  now -= 120_002;
  assert.equal(verifier.verify({ timestamp, body, signature }).reason, "timestamp outside tolerance");
  now = timestamp + 60_000;
  assert.equal(verifier.verify({ timestamp, body, signature }).valid, true, "the boundary is inclusive");
  now = timestamp;

  const malformed = [
    { timestamp: "1700000000000", body, signature },
    { timestamp: -1, body, signature },
    { timestamp: 1.5, body, signature },
    { timestamp, body: 42, signature },
    { timestamp, body, signature: "zzzz" },
    { timestamp, body, signature: signature.slice(0, -2) },
    { timestamp, body, signature: undefined },
  ];
  for (const delivery of malformed) {
    const result = verifier.verify(delivery);
    assert.equal(result.valid, false, JSON.stringify(delivery));
    console.log("  rejected:", result.reason);
  }

  console.log("\n--- unicode payloads ---");
  const unicodeBody = JSON.stringify({ message: "café 🚀 naïve" });
  const unicodeSignature = verifier.sign(timestamp, unicodeBody);
  assert.equal(verifier.verify({ timestamp, body: unicodeBody, signature: unicodeSignature }).valid, true);
  console.log("utf-8 body round-trips through the signature");

  console.log("\n--- replay protection ---");
  now = 0;
  const guard = new ReplayGuard({ ttlMs: 1_000, now: () => now });
  assert.equal(guard.check("sig-1"), true);
  assert.equal(guard.check("sig-1"), false);
  assert.equal(guard.check("sig-2"), true);
  assert.equal(guard.size, 2);

  now = 999;
  assert.equal(guard.check("sig-1"), false, "still inside the window");
  now = 1_000;
  assert.equal(guard.check("sig-1"), true, "expired entries are forgotten");
  assert.equal(guard.check("sig-2"), true, "the other key expired too");
  assert.equal(guard.size, 2);
  console.log("keys are accepted once, then accepted again after the ttl");

  for (const invalidKey of ["", null, 42]) {
    assert.throws(() => guard.check(invalidKey), TypeError);
  }

  console.log("\n--- verification plus replay detection ---");
  now = 1_700_000_000_000;
  const freshVerifier = new WebhookVerifier({ secret: "s3cret", now: () => now });
  const replayGuard = new ReplayGuard({ now: () => now });
  const delivery = {
    timestamp: now,
    body: JSON.stringify({ event: "payment.succeeded", id: "evt_1" }),
    signature: freshVerifier.sign(now, JSON.stringify({ event: "payment.succeeded", id: "evt_1" })),
  };

  assert.equal(freshVerifier.verify(delivery).valid, true);
  assert.equal(replayGuard.check(delivery.signature), true);
  assert.equal(replayGuard.check(delivery.signature), false);
  console.log("a valid signature is still rejected the second time");

  console.log("\nAll checks passed.");
}

main();
