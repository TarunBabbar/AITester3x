const assert = require("node:assert/strict");

function parseCsv(text, { delimiter = ",", header = true } = {}) {
  if (delimiter.length !== 1) {
    throw new Error("delimiter must be exactly one character");
  }
  if (text.charCodeAt(0) === 0xfeff) {
    text = text.slice(1);
  }
  if (text === "") {
    return { headers: [], matrix: [], records: [] };
  }

  const rows = [];
  let row = [];
  let field = "";
  let inQuotes = false;
  let afterQuote = false;
  let endedWithNewline = false;
  let line = 1;
  let quoteStartLine = 0;

  const pushField = () => {
    row.push(field);
    field = "";
  };

  const endRow = () => {
    pushField();
    rows.push(row);
    row = [];
    endedWithNewline = true;
  };

  for (let index = 0; index < text.length; index += 1) {
    const char = text[index];
    endedWithNewline = false;

    if (inQuotes) {
      if (char === '"') {
        if (text[index + 1] === '"') {
          field += '"';
          index += 1;
        } else {
          inQuotes = false;
          afterQuote = true;
        }
      } else {
        if (char === "\n") {
          line += 1;
        }
        field += char;
      }
      continue;
    }

    if (afterQuote) {
      if (char === delimiter) {
        pushField();
        afterQuote = false;
        continue;
      }
      if (char === "\r" || char === "\n") {
        if (char === "\r" && text[index + 1] === "\n") {
          index += 1;
        }
        afterQuote = false;
        endRow();
        line += 1;
        continue;
      }
      throw new Error(`unexpected character ${JSON.stringify(char)} after a closing quote on line ${line}`);
    }

    if (char === '"') {
      if (field !== "") {
        throw new Error(`unexpected quote in the middle of a field on line ${line}`);
      }
      inQuotes = true;
      quoteStartLine = line;
    } else if (char === delimiter) {
      pushField();
    } else if (char === "\r") {
      if (text[index + 1] === "\n") {
        index += 1;
      }
      endRow();
      line += 1;
    } else if (char === "\n") {
      endRow();
      line += 1;
    } else {
      field += char;
    }
  }

  if (inQuotes) {
    throw new Error(`unterminated quote starting on line ${quoteStartLine}`);
  }

  if (!endedWithNewline) {
    pushField();
    rows.push(row);
  }

  if (rows.length === 0) {
    return { headers: [], matrix: [], records: [] };
  }

  const width = rows[0].length;
  rows.forEach((current, index) => {
    if (current.length !== width) {
      const label = header ? `row ${index}` : `line ${index + 1}`;
      throw new Error(`ragged ${label}: expected ${width} fields, found ${current.length}`);
    }
  });

  if (!header) {
    return { headers: [], matrix: rows, records: [] };
  }

  const headers = rows[0];
  const matrix = rows.slice(1);
  const records = matrix.map((values) => Object.fromEntries(headers.map((name, index) => [name, values[index]])));

  return { headers, matrix, records };
}

function toCsv(rows, { delimiter = ",", eol = "\n" } = {}) {
  const escape = (value) => {
    const text = String(value ?? "");
    if (text.includes('"') || text.includes(delimiter) || text.includes("\n") || text.includes("\r")) {
      return `"${text.replaceAll('"', '""')}"`;
    }
    return text;
  };

  return rows.map((row) => row.map(escape).join(delimiter)).join(eol);
}

function main() {
  console.log("--- simple table ---");
  const simple = parseCsv("name,age,city\nAda,36,London\nGrace,45,New York");
  console.log("headers:", simple.headers.join(", "));
  console.log("records:", JSON.stringify(simple.records));
  assert.deepEqual(simple.headers, ["name", "age", "city"]);
  assert.deepEqual(simple.matrix, [["Ada", "36", "London"], ["Grace", "45", "New York"]]);
  assert.deepEqual(simple.records[1], { name: "Grace", age: "45", city: "New York" });

  console.log("\n--- quoted fields ---");
  const quoted = parseCsv('name,role,quote\nAda,"Engineer, Mathematician","She said ""hello"""');
  console.log(JSON.stringify(quoted.records));
  assert.deepEqual(quoted.records[0], {
    name: "Ada",
    role: "Engineer, Mathematician",
    quote: 'She said "hello"',
  });

  console.log("\n--- newlines inside quotes ---");
  const multiline = parseCsv('id,note\n1,"first line\nsecond line"\n2,plain');
  console.log(JSON.stringify(multiline.records));
  assert.equal(multiline.records[0].note, "first line\nsecond line");
  assert.equal(multiline.records[1].note, "plain");
  assert.equal(multiline.matrix.length, 2);

  console.log("\n--- empty fields ---");
  const empties = parseCsv("a,b,c\n1,,3\n,,", { header: true });
  console.log(JSON.stringify(empties.matrix));
  assert.deepEqual(empties.matrix, [["1", "", "3"], ["", "", ""]]);

  console.log("\n--- CRLF and a trailing newline ---");
  const crlf = parseCsv("a,b\r\n1,2\r\n");
  console.log(JSON.stringify(crlf.matrix), "| rows:", crlf.matrix.length);
  assert.deepEqual(crlf.matrix, [["1", "2"]]);

  console.log("\n--- semicolon delimiter ---");
  const semicolon = parseCsv('name;score\n"Ada; L";10', { delimiter: ";" });
  console.log(JSON.stringify(semicolon.matrix));
  assert.deepEqual(semicolon.matrix, [["Ada; L", "10"]]);

  console.log("\n--- headerless ---");
  const headerless = parseCsv("1,2\n3,4", { header: false });
  console.log(JSON.stringify(headerless.matrix));
  assert.deepEqual(headerless.matrix, [["1", "2"], ["3", "4"]]);
  assert.deepEqual(headerless.headers, []);

  console.log("\n--- byte order mark ---");
  const bom = parseCsv("\uFEFFa,b\n1,2");
  console.log("first header:", JSON.stringify(bom.headers[0]));
  assert.equal(bom.headers[0], "a");

  console.log("\n--- empty input ---");
  assert.deepEqual(parseCsv(""), { headers: [], matrix: [], records: [] });
  assert.deepEqual(parseCsv("\n"), { headers: [""], matrix: [], records: [] });
  console.log("empty text and a single newline handled");

  console.log("\n--- round trip ---");
  const original = [
    ["id", "note", "value"],
    ["1", 'contains, comma', "10"],
    ['2', 'says "hi"', "20"],
    ["3", "line\nbreak", "30"],
  ];
  const encoded = toCsv(original);
  console.log(encoded);
  const decoded = parseCsv(encoded, { header: false });
  assert.deepEqual(decoded.matrix, original);
  console.log("write then read gave the same table back");

  console.log("\n--- errors ---");
  const badInputs = [
    ['a,b\n"unterminated,2', /unterminated quote starting on line 2/],
    ["a,b\n1,2,3", /ragged/],
    ["a,b\n1,2,3\n4,5", /ragged row 1/],
    ['a,b\n1,"x"y', /after a closing quote/],
    ['a,b\n1,x"y', /unexpected quote in the middle/],
  ];
  for (const [input, pattern] of badInputs) {
    assert.throws(() => parseCsv(input), pattern);
    console.log("rejected:", JSON.stringify(input));
  }

  console.log("\nAll checks passed.");
}

main();
