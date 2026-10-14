interface Config {
  readonly host: string;
  readonly port: number;
  readonly tags: readonly string[];
}

const config: Config = {
  host: "localhost",
  port: 8080,
  tags: ["api", "v1"],
};

// config.port = 9090; // Error: cannot assign to read-only property
// config.tags.push("new"); // Error: push does not exist on readonly array

const original = { theme: "dark", level: 3 };
const frozen: Readonly<typeof original> = Object.freeze({ ...original });

// frozen.level = 5; // Error: read-only

const mutableSnapshot = { ...config, port: 9090 };
console.log("original port:", config.port);
console.log("snapshot port:", mutableSnapshot.port);

function appendTag(tags: readonly string[], tag: string): string[] {
  return [...tags, tag];
}

const moreTags = appendTag(config.tags, "v2");
console.log("original tags:", config.tags.join(", "));
console.log("new tags:", moreTags.join(", "));

const tuple = [1, 2, 3] as const;
console.log("const tuple:", tuple.join(", "));

const cart = ["book", "pen"] as const;
type Item = (typeof cart)[number];
const chosen: Item = "book";
console.log("chosen item:", chosen);
console.log("frozen:", frozen, "config host:", config.host);
