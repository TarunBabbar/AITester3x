type IsString<T> = T extends string ? true : false;
type ElementType<T> = T extends (infer U)[] ? U : never;
type Flatten<T> = T extends Array<infer U> ? U : T;
type NonNullable<T> = T extends null | undefined ? never : T;

type Mixed = string | number | null | undefined;
type Defined = NonNullable<Mixed>;

type Flags = { a: IsString<string>; b: IsString<number> };
type NumberElement = ElementType<number[]>;
type PlainNumber = Flatten<number>;

function describeValue<T>(value: T): string {
  if (Array.isArray(value)) {
    return `array with ${value.length} items`;
  }
  return `value: ${String(value)}`;
}

const flags: Flags = { a: true, b: false };
console.log("IsString<string>:", flags.a);
console.log("IsString<number>:", flags.b);

const element: NumberElement = 99;
console.log("element:", element);

const plain: PlainNumber = 5;
console.log("plain:", plain);

const defined: Defined[] = ["keep", 42];
console.log("defined:", defined);

console.log(describeValue([1, 2, 3]));
console.log(describeValue("hello"));
