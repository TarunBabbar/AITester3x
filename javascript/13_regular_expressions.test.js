const test = require("node:test");
const assert = require("node:assert/strict");
const { execFileSync } = require("node:child_process");
const path = require("node:path");

const scriptPath = path.join(__dirname, "13_regular_expressions.js");

const output = execFileSync(process.execPath, [scriptPath], { encoding: "utf8" });

test("validates email addresses", () => {
  assert.match(output, /^tarun@example\.com valid: true$/m);
  assert.match(output, /^not-an-email valid: false$/m);
  assert.match(output, /^Asha@Mail\.CO valid: true$/m);
  assert.match(output, /^ravi@site valid: false$/m);
});

test("finds numbers with and without the global flag", () => {
  assert.match(output, /^first year found: 2020$/m);
  assert.match(output, /^all years: 2020, 2021, 2022$/m);
});

test("reads named capture groups and match index", () => {
  assert.match(output, /^date 2023\/01\/15 at index 9$/m);
  assert.match(output, /^date 2023\/02\/20 at index 21$/m);
  assert.match(output, /^date 2024\/03\/25 at index 37$/m);
});

test("replaces first, all, and case-insensitive matches", () => {
  assert.match(output, /^replace first: hi world hello javascript$/m);
  assert.match(output, /^replace all: hi world hi javascript$/m);
  assert.match(output, /^case-insensitive: hi hi$/m);
});

test("splits on punctuation", () => {
  assert.match(output, /^split on punctuation: Tarun \| Asha \| Ravi \| Meera$/m);
});

test("parses a version string with numbered groups", () => {
  assert.match(output, /^major: 2 minor: 10 patch: 3$/m);
});

test("uppercases short words with a replace callback", () => {
  assert.match(output, /^uppercased short words: ONE TWO three four five$/m);
});

test("computes totals from csv rows", () => {
  assert.match(output, /^ABC-1: 10 x 4\.99 = 49\.90$/m);
  assert.match(output, /^DEF-2: 5 x 12\.50 = 62\.50$/m);
});

test("collapses and trims whitespace", () => {
  assert.match(output, /^normalized spacing: "Tarun Ravi"$/m);
});

test("checks anchored patterns", () => {
  assert.match(output, /^digits only: true$/m);
  assert.match(output, /^starts with A: true$/m);
});

test("scans key/value pairs with a stateful exec loop", () => {
  assert.match(output, /^setting host -> localhost$/m);
  assert.match(output, /^setting port -> 8080$/m);
  assert.match(output, /^setting debug -> true$/m);
});
