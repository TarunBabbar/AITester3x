type Expr =
  | { kind: "num"; value: number }
  | { kind: "add"; left: Expr; right: Expr }
  | { kind: "mul"; left: Expr; right: Expr };

interface Visitor<R> {
  num(node: Extract<Expr, { kind: "num" }>): R;
  add(node: Extract<Expr, { kind: "add" }>): R;
  mul(node: Extract<Expr, { kind: "mul" }>): R;
}

function visit<R>(node: Expr, visitor: Visitor<R>): R {
  switch (node.kind) {
    case "num":
      return visitor.num(node);
    case "add":
      return visitor.add(node);
    case "mul":
      return visitor.mul(node);
  }
}

const evaluate: Visitor<number> = {
  num: (node) => node.value,
  add: (node) => visit(node.left, evaluate) + visit(node.right, evaluate),
  mul: (node) => visit(node.left, evaluate) * visit(node.right, evaluate),
};

const print: Visitor<string> = {
  num: (node) => String(node.value),
  add: (node) => `(${visit(node.left, print)} + ${visit(node.right, print)})`,
  mul: (node) => `(${visit(node.left, print)} * ${visit(node.right, print)})`,
};

const tree: Expr = {
  kind: "add",
  left: { kind: "num", value: 2 },
  right: { kind: "mul", left: { kind: "num", value: 3 }, right: { kind: "num", value: 4 } },
};

console.log("expression:", visit(tree, print));
console.log("value:", visit(tree, evaluate));
