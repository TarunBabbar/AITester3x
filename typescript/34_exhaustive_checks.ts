type Shape =
  | { kind: "circle"; radius: number }
  | { kind: "square"; side: number }
  | { kind: "triangle"; base: number; height: number };

function assertNever(value: never): never {
  throw new Error(`Unhandled shape: ${JSON.stringify(value)}`);
}

function area(shape: Shape): number {
  switch (shape.kind) {
    case "circle":
      return Math.round(Math.PI * shape.radius ** 2);
    case "square":
      return shape.side * shape.side;
    case "triangle":
      return (shape.base * shape.height) / 2;
    default:
      return assertNever(shape);
  }
}

function perimeter(shape: Shape): number {
  if (shape.kind === "circle") {
    return Math.round(2 * Math.PI * shape.radius);
  }
  if (shape.kind === "square") {
    return shape.side * 4;
  }
  return 0;
}

const shapes: Shape[] = [
  { kind: "circle", radius: 2 },
  { kind: "square", side: 3 },
  { kind: "triangle", base: 4, height: 5 },
];

for (const shape of shapes) {
  console.log(`${shape.kind} -> area ${area(shape)}, perimeter ${perimeter(shape)}`);
}
