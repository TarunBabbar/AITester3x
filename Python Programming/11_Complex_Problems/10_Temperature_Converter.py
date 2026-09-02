"""
Program 10: Temperature Converter

Converts between Celsius, Fahrenheit and Kelvin, with a built-in self-check
that verifies the conversion formulas round-trip correctly.

Concepts: functions, dictionaries, loops, input validation, assertions.
"""


def c_to_f(celsius: float) -> float:
    return celsius * 9 / 5 + 32


def f_to_c(fahrenheit: float) -> float:
    return (fahrenheit - 32) * 5 / 9


def c_to_k(celsius: float) -> float:
    return celsius + 273.15


def k_to_c(kelvin: float) -> float:
    return kelvin - 273.15


def convert(value: float, from_unit: str, to_unit: str) -> float:
    """Convert value between c, f, k using celsius as the middle step."""
    if from_unit == to_unit:
        return value
    if from_unit == "c":
        celsius = value
    elif from_unit == "f":
        celsius = f_to_c(value)
    else:
        celsius = k_to_c(value)

    if to_unit == "c":
        return celsius
    if to_unit == "f":
        return c_to_f(celsius)
    return c_to_k(celsius)


def self_check() -> None:
    """Verify the conversions with known values."""
    assert abs(c_to_f(0) - 32) < 1e-9          # 0 C = 32 F
    assert abs(c_to_f(100) - 212) < 1e-9       # 100 C = 212 F
    assert abs(c_to_k(0) - 273.15) < 1e-9      # 0 C = 273.15 K
    assert abs(convert(212, "f", "c") - 100) < 1e-9
    assert abs(convert(300, "k", "f") - 80.33) < 0.01
    # Round trips come back to the start.
    for value in (0.0, 37.0, -40.0, 100.0):
        assert abs(convert(convert(value, "c", "f"), "f", "c") - value) < 1e-9
    print("Self-check passed: all conversions verified.")


def main() -> None:
    self_check()
    print("\n=== Temperature Converter ===\n")

    while True:
        try:
            raw = input("Enter value (or blank to quit): ").strip()
        except (EOFError, KeyboardInterrupt):
            print()
            break
        if not raw:
            break
        try:
            value = float(raw)
        except ValueError:
            print("Enter a number.")
            continue

        from_unit = input("From (c/f/k): ").strip().lower()
        if from_unit not in ("c", "f", "k"):
            print("Pick c, f or k.")
            continue
        to_unit = input("To (c/f/k): ").strip().lower()
        if to_unit not in ("c", "f", "k"):
            print("Pick c, f or k.")
            continue

        result = convert(value, from_unit, to_unit)
        print(f"{value} {from_unit} = {result:.2f} {to_unit}\n")


if __name__ == "__main__":
    main()
