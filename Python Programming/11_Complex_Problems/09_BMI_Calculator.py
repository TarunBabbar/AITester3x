"""
Program 9: BMI Calculator

Computes Body Mass Index from height and weight, classifies the result,
and supports both metric and imperial units.

Concepts: functions, conditionals, input validation, formatting.
"""


def bmi_metric(weight_kg: float, height_cm: float) -> float:
    """BMI from kilograms and centimeters."""
    height_m = height_cm / 100
    return weight_kg / (height_m ** 2)


def bmi_imperial(weight_lb: float, height_in: float) -> float:
    """BMI from pounds and inches."""
    return 703 * weight_lb / (height_in ** 2)


def classify(bmi: float) -> str:
    if bmi < 18.5:
        return "Underweight"
    if bmi < 25:
        return "Normal weight"
    if bmi < 30:
        return "Overweight"
    return "Obese"


def read_float(prompt: str) -> float:
    while True:
        try:
            return float(input(prompt))
        except ValueError:
            print("Please enter a number.")


def main() -> None:
    print("=== BMI Calculator ===")
    unit = input("Units (metric/imperial): ").strip().lower()
    while unit not in ("metric", "imperial"):
        unit = input("Pick metric or imperial: ").strip().lower()

    if unit == "metric":
        weight = read_float("Weight (kg): ")
        height = read_float("Height (cm): ")
        bmi = bmi_metric(weight, height)
    else:
        weight = read_float("Weight (lb): ")
        height = read_float("Height (in): ")
        bmi = bmi_imperial(weight, height)

    print(f"\nYour BMI: {bmi:.1f}")
    print(f"Category : {classify(bmi)}")


if __name__ == "__main__":
    main()
