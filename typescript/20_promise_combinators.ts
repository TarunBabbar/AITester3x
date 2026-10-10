function resolveAfter<T>(value: T, ms: number): Promise<T> {
  return new Promise((resolve) => setTimeout(() => resolve(value), ms));
}

function rejectAfter(ms: number): Promise<never> {
  return new Promise((_, reject) => setTimeout(() => reject(new Error("boom")), ms));
}

async function main(): Promise<void> {
  const settled = await Promise.allSettled([
    resolveAfter("fast", 10),
    rejectAfter(20),
    resolveAfter("slow", 30),
  ]);

  for (const result of settled) {
    if (result.status === "fulfilled") {
      console.log("fulfilled:", result.value);
    } else {
      console.log("rejected:", result.reason.message);
    }
  }

  const raced = await Promise.race([resolveAfter("winner", 10), resolveAfter("loser", 50)]);
  console.log("race winner:", raced);

  const anyResult = await Promise.any([rejectAfter(10), resolveAfter("first success", 20)]);
  console.log("any result:", anyResult);

  const all = await Promise.all([resolveAfter(1, 10), resolveAfter(2, 10)]);
  console.log("all:", all);
}

main();
