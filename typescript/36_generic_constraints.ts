interface WithId {
  id: number;
}

interface Product extends WithId {
  name: string;
  price: number;
}

interface Customer extends WithId {
  name: string;
  email: string;
}

function indexById<T extends WithId>(items: T[]): Map<number, T> {
  const map = new Map<number, T>();
  for (const item of items) {
    map.set(item.id, item);
  }
  return map;
}

function pluck<T, K extends keyof T>(items: T[], key: K): T[K][] {
  return items.map((item) => item[key]);
}

function longest<T extends { length: number }>(a: T, b: T): T {
  return a.length >= b.length ? a : b;
}

const products: Product[] = [
  { id: 1, name: "Laptop", price: 75000 },
  { id: 2, name: "Mouse", price: 1500 },
];

const customers: Customer[] = [
  { id: 10, name: "Tarun", email: "tarun@example.com" },
];

const productIndex = indexById(products);
console.log("product #2:", productIndex.get(2));

const customerIndex = indexById(customers);
console.log("customer #10:", customerIndex.get(10));

console.log("names:", pluck(products, "name"));
console.log("prices:", pluck(products, "price"));

console.log("longest name:", longest("Tarun", "Ravi"));
console.log("longest list:", longest([1, 2, 3], [1, 2]).length);
console.log("longest string:", longest("TypeScript", "Java"));
