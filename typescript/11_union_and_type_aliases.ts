type ID = string | number;

type Point = {
  x: number;
  y: number;
};

type Status = "success" | "error" | "loading";

type Shape =
  | { kind: "circle"; radius: number }
  | { kind: "square"; side: number };

function formatId(id: ID): string {
  if (typeof id === "string") {
    return id.toUpperCase();
  }
  return `#${id}`;
}

function area(shape: Shape): number {
  switch (shape.kind) {
    case "circle":
      return Math.round(Math.PI * shape.radius ** 2);
    case "square":
      return shape.side * shape.side;
  }
}

const origin: Point = { x: 0, y: 0 };
const current: Status = "loading";

console.log("formatId('abc'):", formatId("abc"));
console.log("formatId(42):", formatId(42));
console.log("origin:", origin);
console.log("status:", current);
console.log("circle area:", area({ kind: "circle", radius: 3 }));
console.log("square area:", area({ kind: "square", side: 4 }));
