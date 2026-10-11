type Json =
  | string
  | number
  | boolean
  | null
  | Json[]
  | { [key: string]: Json };

type DeepReadonly<T> = {
  readonly [K in keyof T]: T[K] extends object ? DeepReadonly<T[K]> : T[K];
};

const data: Json = {
  name: "Tarun",
  age: 30,
  skills: ["Java", "TypeScript"],
  address: { city: "Delhi", zip: "110001" },
  active: true,
  meta: null,
};

function countNodes(value: Json): number {
  if (Array.isArray(value)) {
    return value.reduce((total, item) => total + countNodes(item), 1);
  }
  if (value !== null && typeof value === "object") {
    return Object.values(value).reduce(
      (total, item) => total + countNodes(item),
      1,
    );
  }
  return 1;
}

const frozen: DeepReadonly<typeof data> = data;
// frozen.name = "changed"; // Error: read-only

console.log("data type:", typeof data);
console.log("total nodes:", countNodes(data));
console.log("skills:", (data as { skills: string[] }).skills.join(", "));
console.log("frozen name:", frozen.name);
