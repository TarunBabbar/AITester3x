interface Timestamped {
  createdAt: Date;
}

interface Identifiable {
  id: number;
}

interface Named {
  name: string;
}

type Entity = Timestamped & Identifiable;
type UserEntity = Entity & Named & { email: string };

type Logger = { log(message: string): void } & { level: number };

function logWith(logger: Logger, message: string): void {
  logger.log(`[${logger.level}] ${message}`);
}

const user: UserEntity = {
  id: 1,
  name: "Tarun",
  email: "tarun@example.com",
  createdAt: new Date(),
};

console.log("entity id:", user.id);
console.log("entity name:", user.name);
console.log("entity email:", user.email);
console.log("created:", user.createdAt.toISOString().slice(0, 10));

const logger: Logger = {
  level: 2,
  log: (message) => console.log(message),
};

logWith(logger, "intersection types work");

type Combined = { a: string } & { b: number } & { c: boolean };
const combined: Combined = { a: "x", b: 2, c: true };
console.log("combined:", combined);
