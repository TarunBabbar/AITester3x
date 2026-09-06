"""
Program 13: Currency Converter

Converts between a small set of currencies using fixed example exchange
rates (rates change daily - update RATES for real values).

Concepts: dictionaries, functions, input validation, formatting.
"""

# Example rates: 1 unit of the key currency in USD
RATES = {
    "usd": 1.0,
    "eur": 1.09,
    "gbp": 1.27,
    "inr": 0.012,
    "jpy": 0.0067,
    "aud": 0.66,
}


def convert(amount: float, from_cur: str, to_cur: str) -> float:
    """Convert amount from one currency to another via USD."""
    usd_value = amount * RATES[from_cur]
    return usd_value / RATES[to_cur]


def main() -> None:
    print("=== Currency Converter ===")
    print("Supported:", ", ".join(c.upper() for c in RATES), "\n")

    while True:
        try:
            raw = input("Amount (or blank to quit): ").strip()
        except (EOFError, KeyboardInterrupt):
            print()
            break
        if not raw:
            break
        try:
            amount = float(raw)
        except ValueError:
            print("Enter a number.")
            continue

        from_cur = input("From (e.g. usd): ").strip().lower()
        to_cur = input("To (e.g. eur): ").strip().lower()
        if from_cur not in RATES or to_cur not in RATES:
            print("Unknown currency. Pick from:", ", ".join(RATES))
            continue

        result = convert(amount, from_cur, to_cur)
        print(f"{amount:,.2f} {from_cur.upper()} = {result:,.2f} {to_cur.upper()}\n")


if __name__ == "__main__":
    main()
