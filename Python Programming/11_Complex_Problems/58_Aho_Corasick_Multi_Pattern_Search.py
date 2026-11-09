"""
Program 58: Aho-Corasick Multi-Pattern Search

Finds every occurrence of a set of patterns in one pass over the text. The trie
of patterns gains failure links, so a mismatch falls back to the longest suffix
that is still a prefix of some pattern instead of restarting the scan. Overlapping
matches are reported, and results are checked against one brute-force scan per
pattern.

Concepts: tries, breadth-first search, failure links, multi-pattern matching.
"""

from __future__ import annotations

import random
from collections import deque
from collections.abc import Iterable


class AhoCorasick:
    def __init__(self, patterns: Iterable[str]) -> None:
        self._goto: list[dict[str, int]] = [{}]
        self._fail: list[int] = [0]
        self._output: list[list[str]] = [[]]
        self._patterns: list[str] = []

        seen: set[str] = set()
        for pattern in patterns:
            if not pattern:
                raise ValueError("patterns must not be empty")
            if pattern in seen:
                continue
            seen.add(pattern)
            self._patterns.append(pattern)
            self._insert(pattern)

        self._build_failure_links()

    @property
    def patterns(self) -> list[str]:
        return list(self._patterns)

    @property
    def node_count(self) -> int:
        return len(self._goto)

    def __len__(self) -> int:
        return len(self._patterns)

    def _insert(self, pattern: str) -> None:
        node = 0
        for character in pattern:
            child = self._goto[node].get(character)
            if child is None:
                child = len(self._goto)
                self._goto[node][character] = child
                self._goto.append({})
                self._fail.append(0)
                self._output.append([])
            node = child
        self._output[node].append(pattern)

    def _build_failure_links(self) -> None:
        queue: deque[int] = deque()

        for child in self._goto[0].values():
            queue.append(child)

        while queue:
            node = queue.popleft()
            for character, child in self._goto[node].items():
                queue.append(child)
                fallback = self._fail[node]
                while fallback and character not in self._goto[fallback]:
                    fallback = self._fail[fallback]
                self._fail[child] = self._goto[fallback].get(character, 0)

    def find_all(self, text: str) -> dict[str, list[int]]:
        """Start index of every occurrence of every pattern, overlaps included."""
        results: dict[str, list[int]] = {pattern: [] for pattern in self._patterns}
        state = 0

        for index, character in enumerate(text):
            while state and character not in self._goto[state]:
                state = self._fail[state]
            state = self._goto[state].get(character, 0)

            current = state
            while current:
                for pattern in self._output[current]:
                    results[pattern].append(index - len(pattern) + 1)
                current = self._fail[current]

        return results

    def find_matches(self, text: str) -> list[tuple[int, str]]:
        matches = [
            (position, pattern)
            for pattern, positions in self.find_all(text).items()
            for position in positions
        ]
        matches.sort(key=lambda pair: (pair[0], pair[1]))
        return matches

    def contains_any(self, text: str) -> bool:
        return any(self.find_all(text).values())


def brute_force(text: str, patterns: list[str]) -> dict[str, list[int]]:
    results: dict[str, list[int]] = {}
    for pattern in patterns:
        results[pattern] = [
            start
            for start in range(len(text) - len(pattern) + 1)
            if text.startswith(pattern, start)
        ]
    return results


def main() -> None:
    print("--- the classic example ---")
    machine = AhoCorasick(["he", "she", "his", "hers"])
    text = "ushers"
    found = machine.find_all(text)
    print(f"  {text!r}: {found}")
    assert found == {"he": [2], "she": [1], "his": [], "hers": [2]}

    matches = machine.find_matches(text)
    print(f"  matches: {matches}")
    assert matches == [(1, "she"), (2, "he"), (2, "hers")]

    print("\n--- numbers and ordering ---")
    numeric = AhoCorasick(["123", "23", "456", "56"])
    assert numeric.find_all("123456") == {"123": [0], "23": [1], "456": [3], "56": [4]}
    print("  123456 ->", numeric.find_all("123456"))

    print("\n--- overlapping matches ---")
    overlapping = AhoCorasick(["a", "aa", "aaa"])
    result = overlapping.find_all("aaaa")
    print(f"  aaaa: {result}")
    assert result == {"a": [0, 1, 2, 3], "aa": [0, 1, 2], "aaa": [0, 1]}

    print("\n--- duplicate patterns collapse ---")
    duplicated = AhoCorasick(["cat", "cat", "at"])
    assert duplicated.patterns == ["cat", "at"], duplicated.patterns
    assert len(duplicated) == 2
    assert duplicated.find_all("cat") == {"cat": [0], "at": [1]}
    print("  patterns:", duplicated.patterns)

    print("\n--- no patterns and no matches ---")
    empty_machine = AhoCorasick([])
    assert empty_machine.find_all("anything") == {}
    assert empty_machine.find_matches("anything") == []
    assert not empty_machine.contains_any("anything")

    no_match = AhoCorasick(["xyz"])
    assert no_match.find_all("abc") == {"xyz": []}
    print("  empty machine and empty result handled")

    print("\n--- unicode ---")
    unicode_machine = AhoCorasick(["café", "fé", "αβ"])
    assert unicode_machine.find_all("un café αβ") == {"café": [3], "fé": [5], "αβ": [8]}
    print("  accented and greek patterns found")

    print("\n--- patterns that are suffixes of each other ---")
    suffixes = AhoCorasick(["bc", "abc", "c"])
    assert suffixes.find_all("zabc") == {"bc": [2], "abc": [1], "c": [3]}
    print("  zabc ->", suffixes.find_all("zabc"))

    print("\n--- invalid patterns ---")
    for invalid in [[""], ["ok", ""]]:
        try:
            AhoCorasick(invalid)
            raise AssertionError("empty patterns must be rejected")
        except ValueError:
            pass
    print("  empty patterns rejected")

    print("\n--- against brute force ---")
    rng = random.Random(58)
    checked = 0

    for _ in range(300):
        text = "".join(rng.choice("abc") for _ in range(rng.randint(0, 30)))
        pattern_count = rng.randint(1, 4)
        patterns = sorted(
            {
                "".join(rng.choice("abc") for _ in range(rng.randint(1, 4)))
                for _ in range(pattern_count)
            }
        )

        machine = AhoCorasick(patterns)
        expected = brute_force(text, patterns)
        actual = machine.find_all(text)
        assert actual == expected, f"{patterns} in {text!r}: {actual} != {expected}"

        for pattern, positions in actual.items():
            for start in positions:
                assert text[start : start + len(pattern)] == pattern

        checked += 1

    print(f"  {checked} random pattern sets match brute force")

    print("\n--- a larger text ---")
    large_text = "ab" * 5_000
    large_machine = AhoCorasick(["ab", "ba", "aba", "bab"])
    large_found = large_machine.find_all(large_text)
    print(f"  counts: { {pattern: len(positions) for pattern, positions in large_found.items()} }")
    assert len(large_found["ab"]) == 5_000
    assert len(large_found["ba"]) == 4_999
    assert len(large_found["aba"]) == 4_999
    assert len(large_found["bab"]) == 4_999

    print("\nAll checks passed.")


if __name__ == "__main__":
    main()
