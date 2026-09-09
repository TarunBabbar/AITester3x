"""
Program 19: Caesar Cipher Encryption

Encrypts or decrypts a message with a classic Caesar shift, and
offers a brute-force cracker that finds the key by scoring every
possible shift against common English words.

Concepts: string translation, modular arithmetic, frequency scoring.
"""

import string

ALPHABET = string.ascii_lowercase
CHARSET = ALPHABET + string.digits + " .,!?;:'\"()"


def shift_text(text: str, key: int, decrypt: bool) -> str:
    """Apply a Caesar shift of `key` to every character in `text`."""
    if decrypt:
        key = -key
    n = len(CHARSET)
    table = str.maketrans(CHARSET, CHARSET[key:] + CHARSET[:key])
    return text.translate(table)


def crack(text: str) -> None:
    """Try every shift and report the ones that look most English-like."""
    common = {"the", "and", "that", "you", "have", "ing"}

    def score(candidate: str) -> int:
        lowered = candidate.lower()
        return sum(1 for word in common if word in lowered)

    best: list[tuple[int, int, str]] = []
    for key in range(1, len(CHARSET)):
        candidate = shift_text(text, key, decrypt=True)
        best.append((score(candidate), -key, candidate))

    best.sort(reverse=True)
    for _, neg_key, candidate in best[:3]:
        print(f"  key {-neg_key:>2}: {candidate.strip()}")


def main() -> None:
    print("Caesar Cipher")
    mode = input("Encrypt (e), decrypt (d), or crack (c)? ").strip().lower()
    text = input("Message: ")

    if mode == "c":
        print("Top candidate shifts:")
        crack(text)
        return

    try:
        key = int(input("Shift amount (1-93): ").strip())
    except ValueError:
        print("Shift must be a whole number.")
        return
    if not 1 <= key < len(CHARSET):
        print(f"Shift must be between 1 and {len(CHARSET) - 1}.")
        return

    decrypted = mode == "d"
    print(f"\nResult: {shift_text(text, key, decrypted)}")


if __name__ == "__main__":
    main()