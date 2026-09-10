"""
Program 22: Email Validator

Validates email addresses with a practical regex plus basic checks, and
compiles a report of what passed and what failed.

Concepts: regular expressions, validation, CLI flag handling.
"""

import argparse
import re

EMAIL_RE = re.compile(
    r"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$"
)


def validate(email: str) -> list[str]:
    """Return a list of problems found; empty list means the address is valid."""
    problems: list[str] = []

    if not email or "@" not in email:
        problems.append("missing '@'")
        return problems

    local, _, domain = email.partition("@")
    if not local:
        problems.append("empty local part (before @)")
    elif len(local) > 64:
        problems.append("local part longer than 64 characters")

    if not domain:
        problems.append("empty domain (after @)")
    elif len(domain) > 255:
        problems.append("domain longer than 255 characters")
    elif not EMAIL_RE.match(email):
        problems.append("does not match general pattern")
    elif ".." in domain or domain.startswith(".") or domain.endswith("."):
        problems.append("malformed dot placement in domain")

    return problems


def main() -> None:
    parser = argparse.ArgumentParser(description="Validate email addresses.")
    parser.add_argument("emails", nargs="*", help="addresses to check")
    parser.add_argument("-f", "--file", help="read addresses from a file (one per line)")
    args = parser.parse_args()

    candidates = list(args.emails)
    if args.file:
        try:
            with open(args.file, encoding="utf-8") as handle:
                candidates.extend(line.strip() for line in handle if line.strip())
        except OSError as exc:
            print(f"Could not read {args.file}: {exc}")
            return

    if not candidates:
        print("No addresses given. Pass them as arguments or via --file.")
        return

    for email in candidates:
        problems = validate(email)
        status = "VALID" if not problems else f"INVALID ({', '.join(problems)})"
        print(f"{email:<30} {status}")


if __name__ == "__main__":
    main()