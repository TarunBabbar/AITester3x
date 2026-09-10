"""
Program 21: Password Generator

Creates cryptographically-random passwords with configurable length and
character sets, then scores the result with a simple strength check.

Concepts: secrets for secure randomness, argparse, set operations.
"""

import argparse
import re
import secrets
import string


def build_pool(use_lower: bool, use_upper: bool, use_digits: bool,
               use_symbols: bool) -> str:
    """Combine the requested character classes into one pool."""
    pool = ""
    if use_lower:
        pool += string.ascii_lowercase
    if use_upper:
        pool += string.ascii_uppercase
    if use_digits:
        pool += string.digits
    if use_symbols:
        pool += string.punctuation
    if not pool:
        raise ValueError("At least one character class must be enabled.")
    return pool


def generate(password_len: int, pool: str) -> str:
    """Return a password of random characters drawn securely from `pool`."""
    return "".join(secrets.choice(pool) for _ in range(password_len))


def strength(password: str) -> str:
    """Classify a password as weak/ok/strong based on length and variety."""
    score = 0
    if len(password) >= 12:
        score += 1
    if re.search(r"[a-z]", password) and re.search(r"[A-Z]", password):
        score += 1
    if re.search(r"\d", password):
        score += 1
    if re.search(r"[^A-Za-z0-9]", password):
        score += 1
    return {0: "weak", 1: "weak", 2: "ok", 3: "strong", 4: "strong"}[score]


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate secure passwords.")
    parser.add_argument("-n", "--length", type=int, default=16, help="password length")
    parser.add_argument("--no-lower", action="store_true", help="exclude lowercase")
    parser.add_argument("--no-upper", action="store_true", help="exclude uppercase")
    parser.add_argument("--no-digits", action="store_true", help="exclude digits")
    parser.add_argument("--no-symbols", action="store_true", help="exclude symbols")
    parser.add_argument("-c", "--count", type=int, default=1, help="how many to print")
    args = parser.parse_args()

    if args.length < 1:
        print("Password length must be at least 1.")
        return

    try:
        pool = build_pool(not args.no_lower, not args.no_upper,
                          not args.no_digits, not args.no_symbols)
    except ValueError as exc:
        print(exc)
        return

    print(f"Generating {args.count} password(s) of length {args.length}:")
    for _ in range(args.count):
        password = generate(args.length, pool)
        print(f"  {password}  [{strength(password)}]")


if __name__ == "__main__":
    main()