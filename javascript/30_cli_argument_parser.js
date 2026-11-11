const assert = require("node:assert/strict");

class ArgumentError extends Error {
  constructor(message) {
    super(message);
    this.name = "ArgumentError";
  }
}

function parseArgs(argv, spec) {
  const byName = new Map();
  const byAlias = new Map();
  const options = {};

  for (const definition of spec) {
    if (!["boolean", "string", "number"].includes(definition.type)) {
      throw new Error(`unsupported type ${JSON.stringify(definition.type)} for ${definition.name}`);
    }
    if (byName.has(definition.name) || byAlias.has(definition.name)) {
      throw new Error(`duplicate option name ${definition.name}`);
    }
    byName.set(definition.name, definition);
    if (definition.alias !== undefined) {
      if (byAlias.has(definition.alias) || byName.has(definition.alias)) {
        throw new Error(`duplicate option alias ${definition.alias}`);
      }
      byAlias.set(definition.alias, definition);
    }

    if (definition.multiple) {
      options[definition.name] = [];
    } else if (definition.default !== undefined) {
      options[definition.name] = definition.default;
    } else {
      options[definition.name] = definition.type === "boolean" ? false : undefined;
    }
  }

  const seen = new Set();
  const positionals = [];

  const assign = (definition, value, flag) => {
    if (definition.multiple) {
      options[definition.name].push(value);
      return;
    }
    if (seen.has(definition.name)) {
      throw new ArgumentError(`option ${flag} was given more than once`);
    }
    seen.add(definition.name);
    options[definition.name] = value;
  };

  const convert = (definition, raw, flag) => {
    if (definition.type === "number") {
      const value = Number(raw);
      if (!Number.isFinite(value)) {
        throw new ArgumentError(`option ${flag} expects a number, got ${JSON.stringify(raw)}`);
      }
      return value;
    }
    return raw;
  };

  const takeValue = (definition, inline, argvIndex, flag) => {
    if (definition.type === "boolean") {
      if (inline !== undefined) {
        throw new ArgumentError(`option ${flag} does not take a value`);
      }
      assign(definition, true, flag);
      return argvIndex + 1;
    }

    const raw = inline !== undefined ? inline : argv[argvIndex + 1];
    if (raw === undefined) {
      throw new ArgumentError(`option ${flag} requires a value`);
    }
    assign(definition, convert(definition, raw, flag), flag);
    return inline !== undefined ? argvIndex + 1 : argvIndex + 2;
  };

  let index = 0;
  while (index < argv.length) {
    const token = argv[index];

    if (token === "--") {
      positionals.push(...argv.slice(index + 1));
      break;
    }
    if (token === "-") {
      positionals.push(token);
      index += 1;
      continue;
    }

    if (token.startsWith("--")) {
      const body = token.slice(2);
      const equals = body.indexOf("=");
      const name = equals === -1 ? body : body.slice(0, equals);
      const inline = equals === -1 ? undefined : body.slice(equals + 1);
      const definition = byName.get(name);
      if (definition === undefined) {
        throw new ArgumentError(`unknown option --${name}`);
      }
      index = takeValue(definition, inline, index, `--${name}`);
      continue;
    }

    if (token.startsWith("-")) {
      const body = token.slice(1);
      const equals = body.indexOf("=");
      const name = equals === -1 ? body : body.slice(0, equals);
      const inline = equals === -1 ? undefined : body.slice(equals + 1);
      const definition = byAlias.get(name);
      if (definition === undefined) {
        throw new ArgumentError(`unknown option -${name}`);
      }
      index = takeValue(definition, inline, index, `-${name}`);
      continue;
    }

    positionals.push(token);
    index += 1;
  }

  return { options, positionals };
}

function spec() {
  return [
    { name: "verbose", alias: "v", type: "boolean" },
    { name: "output", alias: "o", type: "string" },
    { name: "count", alias: "n", type: "number", default: 1 },
    { name: "tags", type: "string", multiple: true },
    { name: "debug", type: "boolean", default: false },
    { name: "name", type: "string", default: "anonymous" },
  ];
}

function main() {
  console.log("--- mixed long options and positionals ---");
  let result = parseArgs(["--verbose", "--output", "out.txt", "input.txt", "second.txt"], spec());
  console.log(JSON.stringify(result));
  assert.equal(result.options.verbose, true);
  assert.equal(result.options.output, "out.txt");
  assert.equal(result.options.count, 1);
  assert.deepEqual(result.positionals, ["input.txt", "second.txt"]);

  console.log("\n--- defaults ---");
  result = parseArgs([], spec());
  console.log(JSON.stringify(result.options));
  assert.equal(result.options.verbose, false);
  assert.equal(result.options.debug, false);
  assert.equal(result.options.count, 1);
  assert.equal(result.options.name, "anonymous");
  assert.deepEqual(result.options.tags, []);
  assert.deepEqual(result.positionals, []);

  console.log("\n--- inline values ---");
  result = parseArgs(["--output=out.txt", "--count=3", "--name=Ada"], spec());
  console.log(JSON.stringify(result.options));
  assert.equal(result.options.output, "out.txt");
  assert.equal(result.options.count, 3);
  assert.equal(result.options.name, "Ada");

  console.log("\n--- short options ---");
  result = parseArgs(["-v", "-o", "result.bin", "-n", "5"], spec());
  assert.equal(result.options.verbose, true);
  assert.equal(result.options.output, "result.bin");
  assert.equal(result.options.count, 5);

  result = parseArgs(["-n=7", "-o=file.txt"], spec());
  assert.equal(result.options.count, 7);
  assert.equal(result.options.output, "file.txt");
  console.log("short flags with separate and inline values work");

  console.log("\n--- repeated multi-value options ---");
  result = parseArgs(["--tags", "a", "--tags", "b", "--tags=c"], spec());
  console.log(JSON.stringify(result.options.tags));
  assert.deepEqual(result.options.tags, ["a", "b", "c"]);

  console.log("\n--- negative numbers as values ---");
  result = parseArgs(["--count", "-2"], spec());
  assert.equal(result.options.count, -2);
  result = parseArgs(["-n", "-10"], spec());
  assert.equal(result.options.count, -10);
  console.log("negative numeric values are accepted");

  console.log("\n--- the double dash terminator ---");
  result = parseArgs(["--verbose", "--", "--output", "-v", "file"], spec());
  console.log(JSON.stringify(result));
  assert.equal(result.options.verbose, true);
  assert.equal(result.options.output, undefined);
  assert.deepEqual(result.positionals, ["--output", "-v", "file"]);

  console.log("\n--- lone dash is a positional ---");
  result = parseArgs(["-", "file"], spec());
  assert.deepEqual(result.positionals, ["-", "file"]);
  console.log("dash kept as a positional");

  console.log("\n--- errors ---");
  const failures = [
    [["--unknown"], /unknown option --unknown/],
    [["-x"], /unknown option -x/],
    [["--output"], /--output requires a value/],
    [["-o"], /-o requires a value/],
    [["--count", "many"], /expects a number/],
    [["--verbose=true"], /does not take a value/],
    [["--count", "1", "--count", "2"], /more than once/],
    [["-vo", "file"], /unknown option -vo/],
  ];
  for (const [argv, pattern] of failures) {
    assert.throws(() => parseArgs(argv, spec()), pattern);
    console.log(`  rejected: ${argv.join(" ")}`);
  }

  console.log("\n--- invalid definitions ---");
  const invalidSpecs = [
    [{ name: "x", type: "date" }],
    [{ name: "x", type: "boolean" }, { name: "x", type: "string" }],
    [{ name: "x", alias: "a", type: "boolean" }, { name: "y", alias: "a", type: "boolean" }],
    [{ name: "x", alias: "y", type: "boolean" }, { name: "y", type: "boolean" }],
  ];
  for (const invalid of invalidSpecs) {
    assert.throws(() => parseArgs([], invalid));
  }
  console.log("bad types, duplicate names, and colliding aliases rejected");

  console.log("\n--- defaults are per call ---");
  const first = parseArgs(["--tags", "one"], spec());
  const second = parseArgs([], spec());
  first.options.tags.push("mutated");
  console.log("first tags:", first.options.tags.join(","), "| second tags:", second.options.tags.join(","));
  assert.deepEqual(second.options.tags, [], "the default array must not be shared");

  console.log("\nAll checks passed.");
}

main();
