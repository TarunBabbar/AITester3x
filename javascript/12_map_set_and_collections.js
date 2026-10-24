const visits = new Map();
visits.set("Tarun", 5);
visits.set("Asha", 3);
visits.set("Ravi", 7);
visits.set("Tarun", visits.get("Tarun") + 1);

console.log("map size:", visits.size);
console.log("Tarun visits:", visits.get("Tarun"));
console.log("has Ravi:", visits.has("Ravi"));
console.log("keys:", [...visits.keys()].join(", "));
console.log("values:", [...visits.values()].join(", "));

for (const [name, count] of visits) {
  console.log(`${name} has ${count} visits`);
}

const objectKey = { id: 1 };
visits.set(objectKey, "object value");
console.log("object key lookup:", visits.get(objectKey));

const tags = new Set(["javascript", "node", "javascript", "es2020"]);
tags.add("closures");
tags.delete("node");

console.log("set size:", tags.size);
console.log("unique tags:", [...tags].join(", "));
console.log("has node:", tags.has("node"));

const listA = [1, 2, 3, 4];
const listB = [3, 4, 5, 6];
const listBSet = new Set(listB);
const shared = listA.filter((item) => listBSet.has(item));
console.log("shared items:", shared.join(", "));

const counts = new Map();
for (const word of ["red", "blue", "red", "green", "blue", "red"]) {
  counts.set(word, (counts.get(word) ?? 0) + 1);
}

const sortedCounts = [...counts.entries()].sort((a, b) => b[1] - a[1]);
console.log("word counts:", sortedCounts);

const inventory = new WeakMap();
const product = { sku: "ABC-1" };
inventory.set(product, { stock: 42 });
console.log("stock from weakmap:", inventory.get(product)?.stock);

const queue = new Map();
queue.set(1, "task-one");
queue.set(2, "task-two");
const firstKey = queue.keys().next().value;
console.log("dequeued:", queue.get(firstKey));
queue.delete(firstKey);
console.log("remaining:", queue.size);
