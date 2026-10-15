interface Handler<Req, Res> {
  handle(request: Req): Res;
}

function withLogging<Req, Res>(
  handler: Handler<Req, Res>,
  label: string,
): Handler<Req, Res> {
  return {
    handle(request: Req): Res {
      console.log(`[${label}] request:`, JSON.stringify(request));
      const response = handler.handle(request);
      console.log(`[${label}] response:`, JSON.stringify(response));
      return response;
    },
  };
}

function withRetry<Req, Res>(
  handler: Handler<Req, Res>,
  attempts: number,
): Handler<Req, Res> {
  return {
    handle(request: Req): Res {
      let lastError: unknown;
      for (let i = 1; i <= attempts; i++) {
        try {
          return handler.handle(request);
        } catch (error) {
          lastError = error;
          console.log(`attempt ${i} failed`);
        }
      }
      throw lastError;
    },
  };
}

const divideHandler: Handler<{ a: number; b: number }, number> = {
  handle: ({ a, b }) => {
    if (b === 0) {
      throw new Error("division by zero");
    }
    return a / b;
  },
};

const decorated = withLogging(withRetry(divideHandler, 2), "divide");

console.log("result:", decorated.handle({ a: 10, b: 2 }));

try {
  decorated.handle({ a: 10, b: 0 });
} catch (error) {
  console.log("final error:", (error as Error).message);
}
