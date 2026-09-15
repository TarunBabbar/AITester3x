"""
Program 36: Roman Numeral Converter

Converts integers to Roman numerals and back. The decoder uses the subtractive
rule (IV, IX, XL, ...), validates its input, and every value from 1 to 3999 is
round-tripped to prove the two directions agree.

Concepts: tuples as lookup tables, loops, string handling, validation.
"""

ROMAN_VALUES = [
    (1000, "M"), (900, "CM"), (500, "D"), (400, "CD"),
    (100, "C"), (90, "XC"), (50, "L"), (40, "XL"),
    (10, "X"), (9, "IX"), (5, "V"), (4, "IV"), (1, "I"),
]

SYMBOL_VALUES = {
    "I": 1, "V": 5, "X": 10, "L": 50,
    "C": 100, "D": 500, "M": 1000,
}

MAX_VALUE = 3999


def to_roman(number: int) -> str:
    """Convert 1..3999 to a Roman numeral."""
    if not isinstance(number, int) or isinstance(number, bool):
        raise TypeError("number must be an int")
    if not 1 <= number <= MAX_VALUE:
        raise ValueError(f"number must be between 1 and {MAX_VALUE}, got {number}")
    remaining = number
    parts: list[str] = []
    for value, symbol in ROMAN_VALUES:
        while remaining >= value:
            parts.append(symbol)
            remaining -= value
    return "".join(parts)


def from_roman(text: str) -> int:
    """Convert a Roman numeral to an integer using the subtractive rule."""
    if not text:
        raise ValueError("empty Roman numeral")
    upper = text.upper()
    unknown = set(upper) - set(SYMBOL_VALUES)
    if unknown:
        raise ValueError(f"invalid Roman characters: {sorted(unknown)}")
    total = 0
    for index, symbol in enumerate(upper):
        value = SYMBOL_VALUES[symbol]
        following = SYMBOL_VALUES[upper[index + 1]] if index + 1 < len(upper) else 0
        total += -value if value < following else value
    if not 1 <= total <= MAX_VALUE:
        raise ValueError(f"value out of range: {total}")
    if to_roman(total) != upper:
        raise ValueError(f"not a canonical Roman numeral: {text!r}")
    return total


def main() -> None:
    examples = [1, 4, 9, 14, 40, 90, 400, 1987, 2024, 3999]
    for number in examples:
        roman = to_roman(number)
        back = from_roman(roman)
        assert back == number
        print(f"{number:>4} -> {roman:<15} -> {back}")

    print("\nRound-tripping every value from 1 to 3999...")
    for number in range(1, MAX_VALUE + 1):
        assert from_roman(to_roman(number)) == number
    print("  all 3999 values round-tripped correctly.")

    print("\nInvalid input is rejected:")
    for bad_number in (0, -3, 4000, "ten"):
        try:
            to_roman(bad_number)
        except (TypeError, ValueError) as exc:
            print(f"  to_roman({bad_number!r}) -> {type(exc).__name__}: {exc}")
    for bad_text in ("", "ABC", "IIII", "VX"):
        try:
            from_roman(bad_text)
        except ValueError as exc:
            print(f"  from_roman({bad_text!r}) -> ValueError: {exc}")


if __name__ == "__main__":
    main()
