const emailPattern = /^[^@\s]+@[^@\s]+\.[a-z]{2,}$/i;
const candidates = ["tarun@example.com", "not-an-email", "Asha@Mail.CO", "ravi@site"];

for (const email of candidates) {
  console.log(`${email} valid:`, emailPattern.test(email));
}

const text = "The year 2020 was followed by 2021 and then 2022.";
console.log("first year found:", text.match(/\d{4}/)[0]);
console.log("all years:", text.match(/\d{4}/g).join(", "));

const dates = "reports: 2023-01-15, 2023-02-20, and 2024-03-25";
const datePattern = /(?<year>\d{4})-(?<month>\d{2})-(?<day>\d{2})/g;

for (const match of dates.matchAll(datePattern)) {
  const { year, month, day } = match.groups;
  console.log(`date ${year}/${month}/${day} at index ${match.index}`);
}

const shout = "hello world hello javascript";
console.log("replace first:", shout.replace(/hello/, "hi"));
console.log("replace all:", shout.replace(/hello/g, "hi"));
console.log("case-insensitive:", "Hello Hello".replace(/hello/gi, "hi"));

const sentence = "Tarun, Asha; Ravi, Meera";
console.log("split on punctuation:", sentence.split(/[,;]\s*/).join(" | "));

const version = "v2.10.3";
const versionPattern = /^v(\d+)\.(\d+)\.(\d+)$/;
const parsed = version.match(versionPattern);
console.log("major:", parsed[1], "minor:", parsed[2], "patch:", parsed[3]);

const words = "one two three four five";
const shortWords = words.replace(/\b(\w{1,3})\b/g, (word) => word.toUpperCase());
console.log("uppercased short words:", shortWords);

const csv = "sku,qty,price\nABC-1,10,4.99\nDEF-2,5,12.50";
const rows = csv.split("\n").slice(1);
for (const row of rows) {
  const [sku, qty, price] = row.split(",");
  const total = Number(qty) * Number(price);
  console.log(`${sku}: ${qty} x ${price} = ${total.toFixed(2)}`);
}

const sanitized = "  Tarun   Ravi  ".replace(/\s+/g, " ").trim();
console.log("normalized spacing:", `"${sanitized}"`);

const numericInput = "12345";
console.log("digits only:", /^\d+$/.test(numericInput));
console.log("starts with A:", /^A/i.test("apple"));

let result;
const scanner = /(\w+)=(\w+)/g;
const config = "host=localhost port=8080 debug=true";
while ((result = scanner.exec(config)) !== null) {
  console.log(`setting ${result[1]} -> ${result[2]}`);
}
