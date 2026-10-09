class ValidationError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "ValidationError";
  }
}

function parseAge(value: string): number {
  const age = Number(value);
  if (Number.isNaN(age)) {
    throw new ValidationError(`Not a number: ${value}`);
  }
  if (age < 0) {
    throw new ValidationError(`Age cannot be negative: ${age}`);
  }
  return age;
}

const inputs: string[] = ["30", "abc", "-5"];

for (const input of inputs) {
  try {
    const age = parseAge(input);
    console.log(`Parsed "${input}" -> ${age}`);
  } catch (error) {
    if (error instanceof ValidationError) {
      console.log(`Validation failed for "${input}": ${error.message}`);
    } else {
      console.log(`Unexpected error for "${input}"`);
    }
  } finally {
    console.log(`Done checking "${input}"`);
  }
}
