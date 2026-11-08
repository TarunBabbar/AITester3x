"""
Program 56: KMP String Search

Finds every occurrence of a pattern in a text using the prefix function, the
longest proper prefix of the pattern that is also a suffix. The text is scanned
once and never rewound, so the search is linear and reports overlapping matches.
Results are checked against a brute-force scan on random inputs.

Concepts: string algorithms, prefix function, linear-time search, overlaps.
"""

from __future__ import annotations

import random


def prefix_function(pattern: str) -> list[int]:
    """pi[i] is the length of the longest border of pattern[:i + 1]."""
    table = [0] * len(pattern)
    length = 0

    for index in range(1, len(pattern)):
        while length > 0 and pattern[index] != pattern[length]:
            length = table[length - 1]
        if pattern[index] == pattern[length]:
            length += 1
        table[index] = length

    return table


def kmp_search(text: str, pattern: str) -> list[int]:
    """Start index of every match, including overlapping ones."""
    if pattern == "":
        return list(range(len(text) + 1))

    table = prefix_function(pattern)
    matches: list[int] = []
    matched = 0

    for index, character in enumerate(text):
        while matched > 0 and character != pattern[matched]:
            matched = table[matched - 1]
        if character == pattern[matched]:
            matched += 1
        if matched == len(pattern):
            matches.append(index - len(pattern) + 1)
            matched = table[matched - 1]

    return matches


def brute_force_search(text: str, pattern: str) -> list[int]:
    if pattern == "":
        return list(range(len(text) + 1))
    return [
        start
        for start in range(len(text) - len(pattern) + 1)
        if text.startswith(pattern, start)
    ]


def longest_border(text: str) -> str:
    """Longest proper prefix that is also a suffix."""
    table = prefix_function(text)
    return text[: table[-1]] if table else ""


def minimal_period(text: str) -> int:
    """Shortest repeating block, or len(text) when there is none."""
    if not text:
        return 0
    table = prefix_function(text)
    candidate = len(text) - table[-1]
    return candidate if len(text) % candidate == 0 else len(text)


def main() -> None:
    print("--- prefix function ---")
    cases = [
        ("ababaca", [0, 0, 1, 2, 3, 0, 1]),
        ("aabaaab", [0, 1, 0, 1, 2, 2, 3]),
        ("aaaa", [0, 1, 2, 3]),
        ("abcd", [0, 0, 0, 0]),
        ("a", [0]),
        ("", []),
    ]
    for pattern, expected in cases:
        actual = prefix_function(pattern)
        print(f"  {pattern!r:10} -> {actual}")
        assert actual == expected, f"{pattern!r}: {actual} != {expected}"

    print("\n--- searching ---")
    assert kmp_search("abababab", "abab") == [0, 2, 4]
    assert kmp_search("aaaaa", "aa") == [0, 1, 2, 3]
    assert kmp_search("hello world", "o") == [4, 7]
    assert kmp_search("hello world", "world") == [6]
    assert kmp_search("hello", "bye") == []
    assert kmp_search("abc", "abcdef") == []
    assert kmp_search("abc", "") == [0, 1, 2, 3]
    assert kmp_search("", "") == [0]
    print("  overlapping, missing, and empty patterns behave")

    print("\n--- a case where naive scanning backtracks ---")
    text = "aaaaab"
    pattern = "aaab"
    print(f"  {text!r} / {pattern!r} -> {kmp_search(text, pattern)}")
    assert kmp_search(text, pattern) == [2]
    assert brute_force_search(text, pattern) == [2]

    print("\n--- borders and periods ---")
    assert longest_border("ababab") == "abab"
    assert longest_border("aaaa") == "aaa"
    assert longest_border("abcd") == ""
    assert minimal_period("abcabcabc") == 3
    assert minimal_period("ababab") == 2
    assert minimal_period("abcd") == 4
    assert minimal_period("a") == 1
    assert minimal_period("") == 0
    print("  borders: ababab -> abab, aaaa -> aaa, abcd -> ''")
    print("  periods: abcabcabc -> 3, ababab -> 2, abcd -> 4")

    print("\n--- unicode ---")
    assert kmp_search("café au lait café", "café") == [0, 13]
    assert kmp_search("ααβααβ", "αβ") == [1, 4]
    print("  accented and greek text searched correctly")

    print("\n--- against brute force ---")
    rng = random.Random(56)
    checked = 0
    for _ in range(500):
        text = "".join(rng.choice("ab") for _ in range(rng.randint(0, 40)))
        pattern = "".join(rng.choice("ab") for _ in range(rng.randint(0, 5)))

        expected = brute_force_search(text, pattern)
        actual = kmp_search(text, pattern)
        assert actual == expected, f"{pattern!r} in {text!r}: {actual} != {expected}"

        for start in actual:
            assert text[start : start + len(pattern)] == pattern
        checked += 1

    print(f"  {checked} random text/pattern pairs match the naive scan")

    print("\n--- every reported match is real, every real match is reported ---")
    large_text = "ab" * 500
    matches = kmp_search(large_text, "aba")
    assert len(matches) == 499
    assert all(large_text[start : start + 3] == "aba" for start in matches)
    assert matches == list(range(0, 997, 2))
    print(f"  1000-character text, 499 overlapping matches")

    print("\nAll checks passed.")


if __name__ == "__main__":
    main()
