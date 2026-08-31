"""
Program 3: Password Strength Analyzer

Scores a password on length, character variety, and common-pattern penalties.
Prints a strength rating and tips for improvement. No external libraries.

Concepts: string methods, sets, regex, scoring logic, input handling.
"""

import re
import sys

COMMON_PASSWORDS = {
    "password", "password1", "123456", "12345678", "123456789",
    "qwerty", "abc123", "letmein", "admin", "welcome", "monkey", "iloveyou",
}


def analyze(password: str) -> dict:
    """Return a score (0-100) and feedback list for a password."""
    feedback: list[str] = []
    score = 0

    # Length
    if len(password) >= 12:
        score += 40
    elif len(password) >= 8:
        score += 25
        feedback.append("Add 4+ more characters for a stronger password.")
    else:
        score += 10
        feedback.append("Use at least 8 characters.")

    # Character variety
    has_lower = bool(re.search(r"[a-z]", password))
    has_upper = bool(re.search(r"[A-Z]", password))
    has_digit = bool(re.search(r"\d", password))
    has_symbol = bool(re.search(r"[^A-Za-z0-9]", password))
    variety = sum([has_lower, has_upper, has_digit, has_symbol])

    score += variety * 10
    if variety < 3:
        feedback.append("Mix lowercase, uppercase, digits and symbols.")

    if has_upper and has_lower:
        score += 5
    if has_digit and has_symbol:
        score += 5

    # Penalties
    if password.lower() in COMMON_PASSWORDS:
        score -= 40
        feedback.append("This is an extremely common password - avoid it.")
    if re.search(r"(.)\1{2,}", password):
        score -= 10
        feedback.append("Avoid repeated characters like 'aaa'.")
    if re.search(r"(1234|qwer|abcd|password|letmein)", password.lower()):
        score -= 10
        feedback.append("Avoid predictable sequences and words.")

    score = max(0, min(100, score))
    if score >= 80:
        rating = "Strong"
    elif score >= 50:
        rating = "Medium"
    else:
        rating = "Weak"
    return {"score": score, "rating": rating, "feedback": feedback}


def main() -> None:
    if len(sys.argv) > 1:
        passwords = sys.argv[1:]
    else:
        print("Password Strength Analyzer")
        print("Enter passwords to test (empty line quits):\n")
        passwords = []
        while True:
            try:
                line = input("password> ").strip()
            except (EOFError, KeyboardInterrupt):
                break
            if not line:
                break
            passwords.append(line)

    for pwd in passwords:
        result = analyze(pwd)
        print(f"\n{pwd!r} -> {result['rating']} ({result['score']}/100)")
        for tip in result["feedback"]:
            print(f"  - {tip}")


if __name__ == "__main__":
    main()
