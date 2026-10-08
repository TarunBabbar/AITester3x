interface Shape {
  area(): number;
  describe(): string;
}

class Rectangle implements Shape {
  constructor(
    private width: number,
    private height: number,
  ) {}

  area(): number {
    return this.width * this.height;
  }

  describe(): string {
    return `Rectangle ${this.width}x${this.height}`;
  }
}

class Circle implements Shape {
  constructor(private radius: number) {}

  area(): number {
    return Math.round(Math.PI * this.radius ** 2);
  }

  describe(): string {
    return `Circle r=${this.radius}`;
  }
}

const shapes: Shape[] = [new Rectangle(4, 5), new Circle(3)];

for (const shape of shapes) {
  console.log(`${shape.describe()} -> area: ${shape.area()}`);
}
