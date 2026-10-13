type Result<T, E = string> =
  | { ok: true; value: T }
  | { ok: false; error: E };

function ok<T>(value: T): Result<T, never> {
  return { ok: true, value };
}

function err<E>(error: E): Result<never, E> {
  return { ok: false, error };
}

function parseJson<T>(text: string): Result<T> {
  try {
    return ok(JSON.parse(text) as T);
  } catch (error) {
    return err((error as Error).message);
  }
}

function divide(a: number, b: number): Result<number> {
  if (b === 0) {
    return err("division by zero");
  }
  return ok(a / b);
}

const samples: string[] = ['{"id":1,"name":"Tarun"}', "not json"];

for (const sample of samples) {
  const result = parseJson<{ id: number; name: string }>(sample);
  if (result.ok) {
    console.log("parsed name:", result.value.name);
  } else {
    console.log("parse error:", result.error);
  }
}

const divisions: [number, number][] = [
  [10, 2],
  [5, 0],
];

for (const [a, b] of divisions) {
  const result = divide(a, b);
  console.log(`${a}/${b} =`, result.ok ? result.value : `error: ${result.error}`);
}
