interface Shape {
  kind: string;
  area(): number;
}

type ShapeFactory = (params: Record<string, number>) => Shape;

class ShapeRegistry {
  private factories = new Map<string, ShapeFactory>();

  register(kind: string, factory: ShapeFactory): void {
    this.factories.set(kind, factory);
  }

  create(kind: string, params: Record<string, number>): Shape {
    const factory = this.factories.get(kind);
    if (!factory) {
      throw new Error(`Unknown shape kind: ${kind}`);
    }
    return factory(params);
  }

  kinds(): string[] {
    return [...this.factories.keys()];
  }
}

const registry = new ShapeRegistry();

registry.register("circle", ({ radius }) => ({
  kind: "circle",
  area: () => Math.round(Math.PI * radius ** 2),
}));

registry.register("rectangle", ({ width, height }) => ({
  kind: "rectangle",
  area: () => width * height,
}));

registry.register("triangle", ({ base, height }) => ({
  kind: "triangle",
  area: () => (base * height) / 2,
}));

console.log("registered kinds:", registry.kinds().join(", "));

const requests: [string, Record<string, number>][] = [
  ["circle", { radius: 3 }],
  ["rectangle", { width: 4, height: 5 }],
  ["triangle", { base: 6, height: 2 }],
];

for (const [kind, params] of requests) {
  const shape = registry.create(kind, params);
  console.log(`${shape.kind} area = ${shape.area()}`);
}

try {
  registry.create("hexagon", {});
} catch (error) {
  console.log("error:", (error as Error).message);
}
