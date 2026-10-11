function format(value: string): string;
function format(value: number): string;
function format(value: boolean): string;
function format(value: Date): string;
function format(value: string | number | boolean | Date): string {
  if (typeof value === "string") {
    return `string: ${value}`;
  }
  if (typeof value === "number") {
    return `number: ${value.toFixed(2)}`;
  }
  if (typeof value === "boolean") {
    return `boolean: ${value ? "yes" : "no"}`;
  }
  return `date: ${value.toISOString().slice(0, 10)}`;
}

function createLabel(size: "small"): { size: string };
function createLabel(size: "large"): { size: string; padding: number };
function createLabel(size: "small" | "large"): { size: string; padding?: number } {
  if (size === "large") {
    return { size, padding: 10 };
  }
  return { size };
}

console.log(format("hello"));
console.log(format(3.14159));
console.log(format(true));
console.log(format(new Date()));
console.log(createLabel("small"));
console.log(createLabel("large"));
