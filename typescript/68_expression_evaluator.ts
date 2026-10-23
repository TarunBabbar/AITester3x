type Token =
  | { kind: "number"; value: number }
  | { kind: "op"; value: "+" | "-" | "*" | "/" }
  | { kind: "paren"; value: "(" | ")" };

function tokenize(input: string): Token[] {
  const tokens: Token[] = [];
  let index = 0;
  while (index < input.length) {
    const char = input[index];
    if (char === " ") {
      index++;
    } else if ((char >= "0" && char <= "9") || char === ".") {
      let num = "";
      while (index < input.length && /[0-9.]/.test(input[index])) {
        num += input[index++];
      }
      tokens.push({ kind: "number", value: Number(num) });
    } else if (char === "+" || char === "-" || char === "*" || char === "/") {
      tokens.push({ kind: "op", value: char });
      index++;
    } else if (char === "(" || char === ")") {
      tokens.push({ kind: "paren", value: char });
      index++;
    } else {
      throw new Error(`Unexpected character: ${char}`);
    }
  }
  return tokens;
}

class Parser {
  private pos = 0;

  constructor(private readonly tokens: Token[]) {}

  parse(): number {
    const value = this.expression();
    if (this.pos < this.tokens.length) {
      throw new Error("Unexpected trailing input");
    }
    return value;
  }

  private expression(): number {
    let left = this.term();
    while (this.matchOp("+") || this.matchOp("-")) {
      const op = this.consume() as { value: "+" | "-" };
      const right = this.term();
      left = op.value === "+" ? left + right : left - right;
    }
    return left;
  }

  private term(): number {
    let left = this.factor();
    while (this.matchOp("*") || this.matchOp("/")) {
      const op = this.consume() as { value: "*" | "/" };
      const right = this.factor();
      if (op.value === "/" && right === 0) {
        throw new Error("Division by zero");
      }
      left = op.value === "*" ? left * right : left / right;
    }
    return left;
  }

  private consume(): Token {
    return this.tokens[this.pos++];
  }

  private factor(): number {
    const token = this.tokens[this.pos];
    if (!token) throw new Error("Unexpected end of input");

    if (token.kind === "number") {
      this.pos++;
      return token.value;
    }

    if (token.kind === "paren" && token.value === "(") {
      this.pos++;
      const value = this.expression();
      const closing = this.tokens[this.pos];
      if (!closing || closing.kind !== "paren" || closing.value !== ")") {
        throw new Error("Missing closing parenthesis");
      }
      this.pos++;
      return value;
    }

    throw new Error("Expected number or '('");
  }

  private matchOp(op: "+" | "-" | "*" | "/"): boolean {
    const token = this.tokens[this.pos];
    return token?.kind === "op" && token.value === op;
  }
}

function evaluate(input: string): number {
  return new Parser(tokenize(input)).parse();
}

const expressions = [
  "2 + 3 * 4",
  "(2 + 3) * 4",
  "10 / 2 - 3",
  "1 + 2 * (3 - 1) / 2",
];

for (const expr of expressions) {
  console.log(`${expr} = ${evaluate(expr)}`);
}

for (const bad of ["1 / 0", "2 +", "(1 + 2"]) {
  try {
    evaluate(bad);
  } catch (error) {
    console.log(`"${bad}" -> ${(error as Error).message}`);
  }
}
