"""
Program 17: Number System Converter

Converts a number between binary, octal, decimal, and hexadecimal, and
performs addition/subtraction directly on numbers given in any base.

Concepts: base conversion, string parsing, dict lookups, input validation.
"""

import re

BASES = {"bin": 2, "oct": 8, "dec": 10, "hex": 16}

DIGITS = "0123456789ABCDEF"


def to_decimal(value: str, base: int) -> int:
    """Convert a string in the given base to an integer."""
    return int(value, base)


def from_decimal(number: int, base: int) -> str:
    """Convert an integer to a string in the given base."""
    if number == 0:
        return "0"
    sign = "-" if number < 0 else ""
    number = abs(number)
    digits = []
    while number > 0:
        number, remainder = divmod(number, base)
        digits.append(DIGITS[remainder])
    return sign + "".join(reversed(digits))


def is_valid(value: str, base: int) -> bool:
    """Check that every digit is legal for the given base."""
    pattern = re.compile(rf"^-?[{DIGITS[:base]}]+$")
    return bool(pattern.match(value.upper()))


def convert() -> None:
    print("Number System Converter")
    print("Bases: bin=2, oct=8, dec=10, hex=16\n")

    from_base = input("From base (bin/oct/dec/hex)? ").strip().lower()
    to_base = input("To base (bin/oct/dec/hex)? ").strip().lower()

    if from_base not in BASES or to_base not in BASES:
        print("Unknown base. Use bin, oct, dec, or hex.")
        return

    value = input(f"Number in {from_base}: ").strip()
    if not is_valid(value, BASES[from_base]):
        print(f"{value!r} is not a valid {from_base} number.")
        return

    decimal = to_decimal(value, BASES[from_base])
    result = from_decimal(decimal, BASES[to_base])
    print(f"{value} ({from_base}) = {result} ({to_base})")


def arithmetic() -> None:
    print("\nBase Arithmetic (add or subtract two numbers)")
    base_name = input("Base for both operands (bin/oct/dec/hex)? ").strip().lower()
    if base_name not in BASES:
        print("Unknown base.")
        return

    base = BASES[base_name]
    a = input(f"First number ({base_name}): ").strip()
    b = input(f"Second number ({base_name}): ").strip()
    op = input("Operation (+ or -): ").strip()

    if not (is_valid(a, base) and is_valid(b, base)):
        print("One of the numbers is not valid for that base.")
        return

    x, y = to_decimal(a, base), to_decimal(b, base)
    if op == "+":
        total = x + y
    elif op == "-":
        total = x - y
    else:
        print("Use + or -.")
        return

    print(f"{a} {op} {b} = {from_decimal(total, base)} ({base_name})")
    print(f"Decimal check: {x} {op} {y} = {total}")


def main() -> None:
    try:
        convert()
        arithmetic()
    except (EOFError, KeyboardInterrupt):
        print("\nGoodbye.")


if __name__ == "__main__":
    main()
