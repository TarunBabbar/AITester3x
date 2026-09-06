"""
Program 14: Unit Converter

Converts between common units of length, weight and time. Uses a simple
"convert to base unit, then to target" approach.

Concepts: nested dictionaries, functions, loops, input validation.
"""

# Each unit maps to its value in the base unit of that category.
CONVERSIONS = {
    "length": {"base": "meter", "units": {"m": 1.0, "km": 1000.0, "cm": 0.01, "mm": 0.001, "in": 0.0254, "ft": 0.3048, "mile": 1609.344}},
    "weight": {"base": "kilogram", "units": {"kg": 1.0, "g": 0.001, "mg": 1e-6, "lb": 0.453592, "oz": 0.0283495}},
    "time": {"base": "second", "units": {"s": 1.0, "min": 60.0, "h": 3600.0, "day": 86400.0}},
}


def convert(category: str, amount: float, from_unit: str, to_unit: str) -> float:
    """Convert amount between units within a category via the base unit."""
    units = CONVERSIONS[category]["units"]
    base_value = amount * units[from_unit]
    return base_value / units[to_unit]


def main() -> None:
    print("=== Unit Converter ===")
    print("Categories:", ", ".join(CONVERSIONS))

    category = input("\nCategory (length/weight/time): ").strip().lower()
    while category not in CONVERSIONS:
        category = input("Pick length, weight or time: ").strip().lower()

    units = CONVERSIONS[category]["units"]
    print("Units:", ", ".join(units))

    while True:
        try:
            raw = input("\nAmount (or blank to quit): ").strip()
        except (EOFError, KeyboardInterrupt):
            print()
            return
        if not raw:
            return
        try:
            amount = float(raw)
        except ValueError:
            print("Enter a number.")
            continue

        from_unit = input("From: ").strip().lower()
        to_unit = input("To: ").strip().lower()
        if from_unit not in units or to_unit not in units:
            print("Unknown unit. Pick from:", ", ".join(units))
            continue

        result = convert(category, amount, from_unit, to_unit)
        print(f"{amount} {from_unit} = {result:.4f} {to_unit}")


if __name__ == "__main__":
    main()
