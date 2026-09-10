"""
Program 23: Anagram Detector

Checks whether two strings are anagrams (same letters, any order) using
three different techniques, and finds anagrams of a word in a wordlist
file.

Concepts: collections.Counter, sorted() comparison, prime products, files.
"""

import argparse
from collections import Counter
from math import prod
from pathlib import Path

# One prime per lowercase letter, used for the "prime product" method.
PRIMES = [2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53,
          59, 61, 67, 71, 73, 79, 83, 89, 97, 101]


def normalize(text: str) -> str:
    """Lowercase and strip anything that is not a-z."""
    return "".join(ch for ch in text.lower() if ch.isalpha())


def is_anagram_sorted(a: str, b: str) -> bool:
    """Anagram check via sorted letters."""
    return sorted(a) == sorted(b)


def is_anagram_counter(a: str, b: str) -> bool:
    """Anagram check via letter frequency counts."""
    return Counter(a) == Counter(b)


def is_anagram_prime(a: str, b: str) -> bool:
    """Anagram check via unique product of selected primes."""
    if len(a) != len(b):
        return False
    return prod(PRIMES[ord(ch) - ord("a")] for ch in a) == prod(
        PRIMES[ord(ch) - ord("a")] for ch in b
    )


def find_anagrams(word: str, words: list[str]) -> list[str]:
    """All entries in `words` that are anagrams of `word` (excluding itself)."""
    key = Counter(normalize(word))
    return [w for w in words if w != word and Counter(normalize(w)) == key]


def main() -> None:
    parser = argparse.ArgumentParser(description="Detect anagrams in words.")
    parser.add_argument("word", help="seed word")
    parser.add_argument("-f", "--file", help="wordlist file to search (one word per line)")
    args = parser.parse_args()

    a = normalize(input("First word: "))
    b = normalize(input("Second word: "))

    sorted_ok = is_anagram_sorted(a, b)
    counter_ok = is_anagram_counter(a, b)
    prime_ok = is_anagram_prime(a, b)
    print(f"\n'{a}' and '{b}' are anagrams?")
    print(f"  sorted  method: {sorted_ok}")
    print(f"  counter method: {counter_ok}")
    print(f"  prime   method: {prime_ok}")
    print(f"  all agree: {sorted_ok == counter_ok == prime_ok}")

    if args.file:
        try:
            words = Path(args.file).read_text(encoding="utf-8").splitlines()
        except OSError as exc:
            print(f"Could not read {args.file}: {exc}")
            return
        anagrams = find_anagrams(args.word.lower(), words)
        print(f"\nAnagrams of '{args.word}' in {args.file}: {anagrams or 'none'}")


if __name__ == "__main__":
    main()