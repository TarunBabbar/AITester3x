var legacy = "var is function scoped";
let mutable = 10;
const fixed = "cannot be reassigned";

mutable = 20;

console.log("legacy:", legacy);
console.log("mutable:", mutable);
console.log("fixed:", fixed);

const values = [42, "text", true, null, undefined, { a: 1 }, [1, 2]];

for (const value of values) {
  console.log(`${String(value)} -> ${typeof value}`);
}

console.log("coercion:", 1 + "2");
console.log("coercion:", "3" * "4");
console.log("loose vs strict:", 0 == false, 0 === false);
