const assert = require("node:assert/strict");

class TimeoutError extends Error {
  constructor(ms) {
    super(`operation timed out after ${ms}ms`);
    this.name = "TimeoutError";
  }
}

class CircuitOpenError extends Error {
  constructor(retryAfterMs) {
    super(`circuit is open, retry in ${Math.max(0, Math.ceil(retryAfterMs))}ms`);
    this.name = "CircuitOpenError";
    this.retryAfterMs = retryAfterMs;
  }
}

class RetryError extends Error {
  constructor(cause, attempts) {
    super(`gave up after ${attempts} attempt(s): ${cause.message}`, { cause });
    this.name = "RetryError";
    this.attempts = attempts;
  }
}

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

function withTimeout(promise, ms) {
  let timer;
  const timeout = new Promise((_, reject) => {
    timer = setTimeout(() => reject(new TimeoutError(ms)), ms);
  });
  return Promise.race([promise, timeout]).finally(() => clearTimeout(timer));
}

class CircuitBreaker {
  #state = "closed";
  #failures = 0;
  #openedAt = 0;

  constructor({ threshold = 3, cooldownMs = 50, now = Date.now } = {}) {
    this.threshold = threshold;
    this.cooldownMs = cooldownMs;
    this.now = now;
  }

  get state() {
    if (this.#state === "open" && this.now() - this.#openedAt >= this.cooldownMs) {
      return "half-open";
    }
    return this.#state;
  }

  allow() {
    if (this.state === "open") {
      throw new CircuitOpenError(this.cooldownMs - (this.now() - this.#openedAt));
    }
  }

  recordSuccess() {
    this.#state = "closed";
    this.#failures = 0;
  }

  recordFailure() {
    this.#failures += 1;
    if (this.state === "half-open" || this.#failures >= this.threshold) {
      this.#state = "open";
      this.#openedAt = this.now();
      this.#failures = 0;
    }
  }
}

async function retry(operation, options = {}) {
  const {
    retries = 3,
    baseDelayMs = 10,
    factor = 2,
    maxDelayMs = 200,
    jitter = 0.5,
    shouldRetry = () => true,
    breaker,
    onRetry = () => {},
    sleepFn = sleep,
  } = options;

  let attempt = 0;
  let lastError;

  while (attempt <= retries) {
    attempt += 1;
    breaker?.allow();

    try {
      const value = await operation(attempt);
      breaker?.recordSuccess();
      return { value, attempts: attempt };
    } catch (error) {
      lastError = error;
      breaker?.recordFailure();

      if (attempt > retries || !shouldRetry(error, attempt)) {
        break;
      }

      const ceiling = Math.min(baseDelayMs * factor ** (attempt - 1), maxDelayMs);
      const spread = ceiling * jitter;
      const delay = Math.round(ceiling - spread + Math.random() * spread * 2);
      onRetry(error, attempt, delay);
      await sleepFn(delay);
    }
  }

  throw new RetryError(lastError, attempt);
}

function flaky(failures) {
  let calls = 0;
  return async () => {
    calls += 1;
    if (calls <= failures) {
      throw new Error(`transient failure #${calls}`);
    }
    return `ok after ${calls} call(s)`;
  };
}

async function main() {
  console.log("--- retries until success ---");
  const attempts = [];
  const recovered = await retry(flaky(2), {
    retries: 5,
    baseDelayMs: 1,
    onRetry: (error, attempt) => attempts.push(`${attempt}:${error.message}`),
  });
  console.log(recovered.value);
  console.log("retry log:", attempts.join(" | "));
  assert.equal(recovered.attempts, 3);

  console.log("\n--- exhausting the retries ---");
  try {
    await retry(flaky(Infinity), { retries: 2, baseDelayMs: 1 });
    assert.fail("should not recover from a permanent failure");
  } catch (error) {
    console.log(`${error.name}: ${error.message}`);
    console.log("cause:", error.cause.message, "| attempts:", error.attempts);
    assert.equal(error.attempts, 3);
  }

  console.log("\n--- non-retryable error ---");
  let calls = 0;
  try {
    await retry(
      async () => {
        calls += 1;
        throw new TypeError("bad input");
      },
      { retries: 5, baseDelayMs: 1, shouldRetry: (error) => !(error instanceof TypeError) },
    );
    assert.fail("a TypeError should stop the retry loop");
  } catch (error) {
    console.log(`${error.name}: ${error.message} (operation called ${calls}x)`);
    assert.equal(calls, 1);
  }

  console.log("\n--- timeout ---");
  try {
    await retry(() => withTimeout(sleep(200), 5), { retries: 1, baseDelayMs: 1 });
    assert.fail("the slow operation should time out");
  } catch (error) {
    console.log(`${error.name}: ${error.message}`);
    console.log("cause:", error.cause.name, "-", error.cause.message);
  }

  console.log("\n--- circuit breaker ---");
  let now = 1000;
  const breaker = new CircuitBreaker({ threshold: 2, cooldownMs: 100, now: () => now });
  const failing = flaky(Infinity);

  for (const label of ["call 1", "call 2"]) {
    try {
      await retry(failing, { retries: 0, breaker });
    } catch (error) {
      console.log(`${label}: ${error.cause.message} -> breaker ${breaker.state}`);
    }
  }

  try {
    await retry(failing, { retries: 0, breaker });
    assert.fail("an open breaker should reject the call");
  } catch (error) {
    console.log(`call 3: ${error.name} -> breaker ${breaker.state}`);
    assert.equal(error.name, "CircuitOpenError");
  }

  now += 100;
  console.log("after cooldown:", breaker.state);
  const probe = await retry(flaky(0), { retries: 0, breaker });
  console.log(probe.value, "-> breaker", breaker.state);
  assert.equal(breaker.state, "closed");

  console.log("\n--- backoff schedule ---");
  const delays = [];
  const result = await retry(flaky(3), {
    retries: 3,
    baseDelayMs: 10,
    factor: 2,
    maxDelayMs: 25,
    jitter: 0,
    sleepFn: async (ms) => delays.push(ms),
  });
  console.log("delays:", delays.join(" -> "));
  console.log(result.value);
  assert.deepEqual(delays, [10, 20, 25]);

  console.log("\n--- jitter stays inside the ceiling ---");
  const jittered = [];
  await retry(flaky(3), {
    retries: 3,
    baseDelayMs: 10,
    factor: 2,
    maxDelayMs: 40,
    jitter: 0.5,
    sleepFn: async (ms) => jittered.push(ms),
  });
  const ceilings = [10, 20, 40];
  console.log("jittered:", jittered.join(" -> "));
  jittered.forEach((delay, index) => {
    assert.ok(delay >= ceilings[index] * 0.5 && delay <= ceilings[index] * 1.5, `delay ${delay} out of range`);
  });

  console.log("\nAll checks passed.");
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
