interface Cat {
  kind: "cat";
  meow(): void;
}

interface Dog {
  kind: "dog";
  bark(): void;
}

type Pet = Cat | Dog;

function isString(value: unknown): value is string {
  return typeof value === "string";
}

function isCat(pet: Pet): pet is Cat {
  return pet.kind === "cat";
}

function describe(value: string | number | Date): string {
  if (typeof value === "string") {
    return `string of length ${value.length}`;
  }
  if (typeof value === "number") {
    return `number doubled = ${value * 2}`;
  }
  return `date year = ${value.getFullYear()}`;
}

function speak(pet: Pet): void {
  if ("meow" in pet) {
    pet.meow();
  } else {
    pet.bark();
  }
}

console.log("isString('hi'):", isString("hi"));
console.log("isString(42):", isString(42));
console.log(describe("hello"));
console.log(describe(21));
console.log(describe(new Date()));

const pets: Pet[] = [
  { kind: "cat", meow: () => console.log("meow!") },
  { kind: "dog", bark: () => console.log("woof!") },
];

for (const pet of pets) {
  console.log("isCat:", isCat(pet));
  speak(pet);
}
