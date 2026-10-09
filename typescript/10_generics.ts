function identity<T>(value: T): T {
  return value;
}

function firstElement<T>(items: T[]): T | undefined {
  return items[0];
}

function logLength<T extends { length: number }>(item: T): void {
  console.log("Length:", item.length);
}

class Box<T> {
  private items: T[] = [];

  add(item: T): void {
    this.items.push(item);
  }

  getAll(): T[] {
    return this.items;
  }
}

console.log("identity:", identity<string>("hello"));
console.log("identity:", identity<number>(42));
console.log("firstElement:", firstElement([10, 20, 30]));

const numberBox = new Box<number>();
numberBox.add(1);
numberBox.add(2);
console.log("Box contents:", numberBox.getAll());

const nameBox = new Box<string>();
nameBox.add("Tarun");
console.log("Name box:", nameBox.getAll());

logLength("TypeScript");
logLength([1, 2, 3, 4]);
