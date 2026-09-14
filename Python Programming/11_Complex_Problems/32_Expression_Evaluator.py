"""
Program 32: Expression Evaluator (Shunting-Yard)

Parses infix arithmetic such as "3 + 4 * (2 - 1)" into reverse Polish notation
with the shunting-yard algorithm, then evaluates the RPN with a stack. Handles
the four operators, exponentiation, parentheses and unary minus.

Concepts: stacks, operator precedence, tokenizing, algorithm design.
"""

PRECEDENCE = {"+": 1, "-": 1, "*": 2, "/": 2, "^": 3, "u": 4}
RIGHT_ASSOCIATIVE = {"^", "u"}


def tokenize(expression: str) -> list[str]:
    """Split an expression into numbers, operators and parentheses."""
    tokens: list[str] = []
    index = 0
    while index < len(expression):
        char = expression[index]
        if char.isspace():
            index += 1
        elif char.isdigit() or char == ".":
            end = index
            while end < len(expression) and (expression[end].isdigit() or expression[end] == "."):
                end += 1
            tokens.append(expression[index:end])
            index = end
        elif char in "+-*/^()":
            tokens.append(char)
            index += 1
        else:
            raise ValueError(f"unexpected character {char!r}")
    return tokens


def to_rpn(tokens: list[str]) -> list:
    """Shunting-yard: infix tokens -> reverse Polish notation."""
    output: list = []
    stack: list[str] = []
    previous: str | None = None

    for token in tokens:
        if token[0].isdigit() or token[0] == ".":
            output.append(float(token))
        elif token == "(":
            stack.append(token)
        elif token == ")":
            while stack and stack[-1] != "(":
                output.append(stack.pop())
            if not stack:
                raise ValueError("mismatched parentheses: extra ')'")
            stack.pop()
        else:
            # A '-' is unary at the start or straight after another operator/paren.
            operator = "u" if token == "-" and (previous is None or previous in "+-*/^(") else token
            while (
                stack
                and stack[-1] != "("
                and (
                    PRECEDENCE[stack[-1]] > PRECEDENCE[operator]
                    or (
                        PRECEDENCE[stack[-1]] == PRECEDENCE[operator]
                        and operator not in RIGHT_ASSOCIATIVE
                    )
                )
            ):
                output.append(stack.pop())
            stack.append(operator)
        previous = token

    while stack:
        top = stack.pop()
        if top == "(":
            raise ValueError("mismatched parentheses: extra '('")
        output.append(top)
    return output


def eval_rpn(rpn: list) -> float:
    """Evaluate reverse Polish notation with an operand stack."""
    stack: list[float] = []
    for token in rpn:
        if isinstance(token, float):
            stack.append(token)
        elif token == "u":
            if not stack:
                raise ValueError("malformed expression: unary minus without operand")
            stack.append(-stack.pop())
        else:
            if len(stack) < 2:
                raise ValueError(f"malformed expression: not enough operands for {token!r}")
            right = stack.pop()
            left = stack.pop()
            if token == "+":
                stack.append(left + right)
            elif token == "-":
                stack.append(left - right)
            elif token == "*":
                stack.append(left * right)
            elif token == "/":
                if right == 0:
                    raise ValueError("division by zero")
                stack.append(left / right)
            else:  # "^"
                stack.append(left ** right)
    if len(stack) != 1:
        raise ValueError("malformed expression: leftover operands")
    return stack[0]


def evaluate(expression: str) -> float:
    """Convenience wrapper: infix string -> value."""
    return eval_rpn(to_rpn(tokenize(expression)))


def main() -> None:
    cases = [
        ("3 + 4 * 2", 11),
        ("(3 + 4) * 2", 14),
        ("2 ^ 3 ^ 2", 512),        # right-associative
        ("8 / 2 / 2", 2),          # left-associative
        ("-5 + 10", 5),            # unary minus
        ("-(3 + 2) * 2", -10),
        ("2 * (3 + (4 - 1))", 12),
        ("10 / 4 - 1", 1.5),
    ]

    for expression, expected in cases:
        result = evaluate(expression)
        assert abs(result - expected) < 1e-9, f"{expression} -> {result}, expected {expected}"
        print(f"{expression:<20} = {result:g}   (RPN: {to_rpn(tokenize(expression))})")

    print("\nBad input raises clear errors instead of guessing:")
    for bad in ("2 * (3 + 4", "1 / 0", "3 $ 4", "+"):
        try:
            evaluate(bad)
        except ValueError as exc:
            print(f"  {bad!r:<14} -> ValueError: {exc}")


if __name__ == "__main__":
    main()
