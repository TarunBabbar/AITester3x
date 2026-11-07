"""
Program 54: Longest Common Subsequence (LCS)

Finds the length and one actual longest common subsequence of two strings
using dynamic programming, then builds a two-row alignment and a similarity
score. The length is also computed with a rolling row, and everything is
cross-checked against brute force on small inputs.

Concepts: dynamic programming, backtracking, sequence alignment, recursion.
"""

from __future__ import annotations

import random


def lcs_table(left: str, right: str) -> list[list[int]]:
    """Full DP table; the value at [i][j] is the LCS length of the prefixes."""
    table = [[0] * (len(right) + 1) for _ in range(len(left) + 1)]

    for i in range(1, len(left) + 1):
        for j in range(1, len(right) + 1):
            if left[i - 1] == right[j - 1]:
                table[i][j] = table[i - 1][j - 1] + 1
            else:
                table[i][j] = max(table[i - 1][j], table[i][j - 1])

    return table


def lcs_length(left: str, right: str) -> int:
    """The same length, keeping only the previous row in memory."""
    previous = [0] * (len(right) + 1)

    for left_char in left:
        current = [0]
        for j, right_char in enumerate(right, start=1):
            if left_char == right_char:
                current.append(previous[j - 1] + 1)
            else:
                current.append(max(previous[j], current[j - 1]))
        previous = current

    return previous[-1]


def lcs_string(left: str, right: str) -> str:
    """One longest common subsequence, rebuilt by walking the table backwards."""
    table = lcs_table(left, right)
    i, j = len(left), len(right)
    characters: list[str] = []

    while i > 0 and j > 0:
        if left[i - 1] == right[j - 1]:
            characters.append(left[i - 1])
            i -= 1
            j -= 1
        elif table[i - 1][j] >= table[i][j - 1]:
            i -= 1
        else:
            j -= 1

    characters.reverse()
    return "".join(characters)


def alignment(left: str, right: str) -> list[tuple[str, str]]:
    """Pairs of aligned characters; an empty side means a gap."""
    table = lcs_table(left, right)
    pairs: list[tuple[str, str]] = []
    i, j = len(left), len(right)

    while i > 0 or j > 0:
        if i > 0 and j > 0 and left[i - 1] == right[j - 1]:
            pairs.append((left[i - 1], right[j - 1]))
            i -= 1
            j -= 1
        elif j > 0 and (i == 0 or table[i][j - 1] >= table[i - 1][j]):
            pairs.append(("", right[j - 1]))
            j -= 1
        else:
            pairs.append((left[i - 1], ""))
            i -= 1

    pairs.reverse()
    return pairs


def similarity(left: str, right: str) -> float:
    if not left and not right:
        return 1.0
    return lcs_length(left, right) / max(len(left), len(right))


def is_subsequence(needle: str, haystack: str) -> bool:
    iterator = iter(haystack)
    return all(character in iterator for character in needle)


def brute_force_lcs(left: str, right: str) -> int:
    """Tries every subsequence of the shorter string. Only for tiny inputs."""
    if len(left) > len(right):
        left, right = right, left

    best = 0
    for mask in range(1 << len(left)):
        candidate = "".join(
            character for index, character in enumerate(left) if mask & (1 << index)
        )
        if len(candidate) > best and is_subsequence(candidate, right):
            best = len(candidate)
    return best


def main() -> None:
    print("--- known sequences ---")
    cases = [
        ("AGGTAB", "GXTXAYB", 4),
        ("ABCBDAB", "BDCABA", 4),
        ("kitten", "sitting", 4),
        ("abcdef", "abcdef", 6),
        ("abc", "def", 0),
    ]
    for left, right, expected in cases:
        length = lcs_length(left, right)
        sequence = lcs_string(left, right)
        print(f"  {left!r:10} vs {right!r:10} -> length {length}, lcs {sequence!r}")
        assert length == expected, f"{left!r} vs {right!r}: {length} != {expected}"
        assert len(sequence) == length
        assert is_subsequence(sequence, left) and is_subsequence(sequence, right)

    assert lcs_string("AGGTAB", "GXTXAYB") == "GTAB"

    print("\n--- edge cases ---")
    edge_cases = [("", "", 0), ("", "abc", 0), ("abc", "", 0), ("a", "a", 1), ("a", "b", 0)]
    for left, right, expected in edge_cases:
        assert lcs_length(left, right) == expected
        assert len(lcs_string(left, right)) == expected
    print("  empty and single-character inputs behave")

    print("\n--- symmetry ---")
    rng = random.Random(54)
    for _ in range(50):
        left = "".join(rng.choice("abcd") for _ in range(rng.randint(0, 8)))
        right = "".join(rng.choice("abcd") for _ in range(rng.randint(0, 8)))
        assert lcs_length(left, right) == lcs_length(right, left)
        assert len(lcs_string(left, right)) == len(lcs_string(right, left))
    print("  50 random pairs give the same length in both directions")

    print("\n--- alignment ---")
    left, right = "kitten", "sitting"
    pairs = alignment(left, right)
    rebuilt_left = "".join(pair[0] for pair in pairs)
    rebuilt_right = "".join(pair[1] for pair in pairs)
    matches = sum(1 for pair in pairs if pair[0] and pair[1])
    print("  " + " ".join(f"{a or '-'}/{b or '-'}" for a, b in pairs))
    assert rebuilt_left == left and rebuilt_right == right
    assert matches == lcs_length(left, right)
    assert all(a == b for a, b in pairs if a and b)
    print(f"  {len(pairs)} aligned columns, {matches} matches")

    print("\n--- similarity ---")
    assert similarity("", "") == 1.0
    assert similarity("abc", "abc") == 1.0
    assert similarity("abc", "xyz") == 0.0
    score = similarity("kitten", "sitting")
    print(f"  kitten vs sitting: {score:.2f}")
    assert 0.0 < score < 1.0

    print("\n--- brute-force cross-check ---")
    checked = 0
    for _ in range(300):
        left = "".join(rng.choice("abc") for _ in range(rng.randint(0, 8)))
        right = "".join(rng.choice("abc") for _ in range(rng.randint(0, 8)))
        expected = brute_force_lcs(left, right)
        actual = lcs_length(left, right)
        assert actual == expected, f"{left!r} vs {right!r}: {actual} != {expected}"

        sequence = lcs_string(left, right)
        assert len(sequence) == actual
        assert is_subsequence(sequence, left) and is_subsequence(sequence, right)
        checked += 1
    print(f"  {checked} random pairs match exhaustive search")

    print("\n--- unicode ---")
    left, right = "naïve café", "naive cafe"
    sequence = lcs_string(left, right)
    print(f"  {left!r} vs {right!r} -> {sequence!r}")
    assert is_subsequence(sequence, left) and is_subsequence(sequence, right)
    assert len(sequence) == lcs_length(left, right)

    print("\n--- a longer example ---")
    a = "the quick brown fox jumps over the lazy dog"
    b = "the quick brown cat jumps over a lazy dog"
    sequence = lcs_string(a, b)
    print(f"  lcs length {len(sequence)}: {sequence!r}")
    assert len(sequence) == lcs_length(a, b)
    assert is_subsequence(sequence, a) and is_subsequence(sequence, b)
    assert sum(1 for first, second in alignment(a, b) if first and second) == len(sequence)

    print("\nAll checks passed.")


if __name__ == "__main__":
    main()
