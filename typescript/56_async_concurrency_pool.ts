function delay<T>(value: T, ms: number): Promise<T> {
  return new Promise((resolve) => setTimeout(() => resolve(value), ms));
}

async function runWithLimit<T>(
  tasks: (() => Promise<T>)[],
  limit: number,
): Promise<T[]> {
  const results: T[] = new Array(tasks.length);
  let nextIndex = 0;

  async function worker(): Promise<void> {
    while (nextIndex < tasks.length) {
      const index = nextIndex++;
      console.log(`start task ${index}`);
      results[index] = await tasks[index]();
      console.log(`done task ${index}`);
    }
  }

  const workers = Array.from({ length: Math.min(limit, tasks.length) }, () => worker());
  await Promise.all(workers);
  return results;
}

async function main(): Promise<void> {
  const tasks: (() => Promise<string>)[] = [
    () => delay("a", 30),
    () => delay("b", 10),
    () => delay("c", 20),
    () => delay("d", 5),
    () => delay("e", 15),
  ];

  const results = await runWithLimit(tasks, 2);
  console.log("results:", results.join(", "));

  const numbers = await runWithLimit(
    [1, 2, 3].map((n) => () => delay(n * n, 5)),
    3,
  );
  console.log("squares:", numbers.join(", "));
}

main();
