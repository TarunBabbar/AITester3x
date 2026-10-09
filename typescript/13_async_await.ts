function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function fetchValue(label: string, value: number): Promise<number> {
  await delay(10);
  console.log(`fetched ${label}`);
  return value;
}

async function main(): Promise<void> {
  console.log("start");

  const single = await fetchValue("single", 42);
  console.log("single:", single);

  const [a, b, c] = await Promise.all([
    fetchValue("a", 1),
    fetchValue("b", 2),
    fetchValue("c", 3),
  ]);
  console.log("Promise.all results:", a, b, c);

  console.log("end");
}

main();
