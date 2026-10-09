interface Todo {
  id: number;
  title: string;
  completed: boolean;
}

const todo: Todo = { id: 1, title: "Learn TypeScript", completed: false };

const draft: Partial<Todo> = { title: "Draft todo" };
const preview: Pick<Todo, "id" | "title"> = { id: 2, title: "Preview" };
const withoutId: Omit<Todo, "id"> = {
  title: "No id here",
  completed: true,
};

const frozen: Readonly<Todo> = { ...todo };

const scores: Record<string, number> = {
  Tarun: 95,
  Ravi: 88,
};

console.log("draft:", draft);
console.log("preview:", preview);
console.log("withoutId:", withoutId);
console.log("frozen:", frozen);

// frozen.title = "changed"; // Error: cannot assign, property is read-only

console.log("scores:", scores);
console.log("Tarun score:", scores["Tarun"]);
