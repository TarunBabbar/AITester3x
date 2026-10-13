type ApiResponse<T, E = Error> =
  | { status: "success"; data: T }
  | { status: "error"; error: E };

function createPair<K, V = K>(key: K, value?: V): { key: K; value: V | undefined } {
  return { key, value };
}

function wrap<T, E = Error>(data: T): ApiResponse<T, E> {
  return { status: "success", data };
}

function fail<T, E = Error>(error: E): ApiResponse<T, E> {
  return { status: "error", error };
}

interface User {
  id: number;
  name: string;
}

const sameType = createPair("id", "value");
console.log("same type pair:", sameType);

const mixedType = createPair<string, number>("age", 30);
console.log("mixed pair:", mixedType);

const ok = wrap<User>({ id: 1, name: "Tarun" });
const bad = fail<User>(new Error("not found"));

function render<T, E extends Error>(response: ApiResponse<T, E>): string {
  if (response.status === "success") {
    return `ok: ${JSON.stringify(response.data)}`;
  }
  return `error: ${response.error.message}`;
}

console.log(render(ok));
console.log(render(bad));

const responses: ApiResponse<string>[] = [wrap("first"), wrap("second")];
for (const response of responses) {
  console.log("response status:", response.status, "->", JSON.stringify(response.data));
}
