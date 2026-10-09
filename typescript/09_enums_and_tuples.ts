enum Direction {
  North,
  South,
  East,
  West,
}

enum Status {
  Active = "ACTIVE",
  Inactive = "INACTIVE",
}

const point: [number, number] = [10, 20];
const [x, y] = point;

const namedPair: [name: string, age: number] = ["Tarun", 30];

const directions: Direction[] = [Direction.North, Direction.East];
console.log("Directions:", directions.join(", "));
console.log("Status.Active:", Status.Active);
console.log(`Point x=${x}, y=${y}`);
console.log(`Named pair: ${namedPair[0]} is ${namedPair[1]}`);
