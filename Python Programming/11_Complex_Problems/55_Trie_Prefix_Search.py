"""
Program 55: Trie (Prefix Tree) with Autocomplete

Stores words in a character tree so prefix lookups, autocomplete, and
prefix counting do not need to scan the whole dictionary. Deletion prunes
nodes that become dead, wildcard search matches "." against any character,
and the structure is cross-checked against plain dictionaries.

Concepts: trees, dictionaries, recursion, prefix search, deletion with pruning.
"""

from __future__ import annotations

import random
from collections import Counter
from dataclasses import dataclass, field


@dataclass
class TrieNode:
    children: dict[str, "TrieNode"] = field(default_factory=dict)
    terminal: bool = False
    weight: int = 0


class Trie:
    def __init__(self) -> None:
        self._root = TrieNode()
        self._words = 0

    def __len__(self) -> int:
        return self._words

    def insert(self, word: str, weight: int = 1) -> None:
        if not word:
            raise ValueError("word must not be empty")
        if weight < 1:
            raise ValueError("weight must be positive")

        node = self._root
        for character in word:
            node = node.children.setdefault(character, TrieNode())

        if not node.terminal:
            node.terminal = True
            self._words += 1
        node.weight += weight

    def contains(self, word: str) -> bool:
        node = self._find(word)
        return node is not None and node.terminal

    def startswith(self, prefix: str) -> bool:
        return self._find(prefix) is not None

    def count_prefix(self, prefix: str) -> int:
        node = self._find(prefix)
        return 0 if node is None else self._count_words(node)

    def words(self, prefix: str = "") -> list[str]:
        node = self._find(prefix)
        if node is None:
            return []
        found: list[tuple[str, int]] = []
        self._collect(node, prefix, found)
        return sorted(word for word, _ in found)

    def autocomplete(self, prefix: str, limit: int = 5) -> list[tuple[str, int]]:
        node = self._find(prefix)
        if node is None:
            return []
        found: list[tuple[str, int]] = []
        self._collect(node, prefix, found)
        found.sort(key=lambda pair: (-pair[1], pair[0]))
        return found[:limit]

    def delete(self, word: str) -> bool:
        if not word:
            return False

        path: list[tuple[TrieNode, str]] = []
        node = self._root
        for character in word:
            child = node.children.get(character)
            if child is None:
                return False
            path.append((node, character))
            node = child

        if not node.terminal:
            return False

        node.terminal = False
        node.weight = 0
        self._words -= 1

        for parent, character in reversed(path):
            child = parent.children[character]
            if child.terminal or child.children:
                break
            del parent.children[character]

        return True

    def longest_common_prefix(self) -> str:
        if self._words == 0:
            return ""

        prefix: list[str] = []
        node = self._root
        while len(node.children) == 1 and not node.terminal:
            character, child = next(iter(node.children.items()))
            prefix.append(character)
            node = child
        return "".join(prefix)

    def wildcard(self, pattern: str) -> list[str]:
        """'.' matches exactly one character."""
        results: list[str] = []

        def walk(node: TrieNode, index: int, built: list[str]) -> None:
            if index == len(pattern):
                if node.terminal:
                    results.append("".join(built))
                return

            character = pattern[index]
            if character == ".":
                for next_character, child in node.children.items():
                    built.append(next_character)
                    walk(child, index + 1, built)
                    built.pop()
            else:
                child = node.children.get(character)
                if child is not None:
                    built.append(character)
                    walk(child, index + 1, built)
                    built.pop()

        walk(self._root, 0, [])
        return sorted(results)

    def _find(self, prefix: str) -> TrieNode | None:
        node = self._root
        for character in prefix:
            node = node.children.get(character)
            if node is None:
                return None
        return node

    def _count_words(self, node: TrieNode) -> int:
        total = 1 if node.terminal else 0
        return total + sum(self._count_words(child) for child in node.children.values())

    def _collect(self, node: TrieNode, prefix: str, found: list[tuple[str, int]]) -> None:
        if node.terminal:
            found.append((prefix, node.weight))
        for character, child in node.children.items():
            self._collect(child, prefix + character, found)


def main() -> None:
    print("--- inserting words ---")
    trie = Trie()
    entries = [
        ("car", 3),
        ("cart", 5),
        ("carbon", 2),
        ("care", 4),
        ("cat", 6),
        ("dog", 7),
        ("do", 1),
        ("door", 2),
    ]
    for word, weight in entries:
        trie.insert(word, weight)

    print(f"  {len(trie)} distinct words")
    assert len(trie) == len(entries)
    assert all(trie.contains(word) for word, _ in entries)
    assert not trie.contains("ca")
    assert trie.startswith("ca") and not trie.startswith("z")

    print("\n--- prefix counts ---")
    expected_counts = {"c": 5, "ca": 5, "car": 4, "cart": 1, "do": 3, "d": 3, "z": 0, "": 8}
    for prefix, expected in expected_counts.items():
        actual = trie.count_prefix(prefix)
        print(f"  {prefix!r:6} -> {actual}")
        assert actual == expected, f"{prefix!r}: {actual} != {expected}"

    print("\n--- autocomplete ---")
    suggestions = trie.autocomplete("car")
    print("  car:", suggestions)
    assert [word for word, _ in suggestions] == ["cart", "care", "car", "carbon"]
    assert [weight for _, weight in suggestions] == [5, 4, 3, 2]

    top_two = trie.autocomplete("ca", limit=2)
    print("  ca (limit 2):", top_two)
    assert [word for word, _ in top_two] == ["cat", "cart"]
    assert trie.autocomplete("zzz") == []

    print("\n--- listing ---")
    print("  car*:", trie.words("car"))
    assert trie.words("car") == ["car", "carbon", "care", "cart"]
    assert trie.words("d") == ["do", "dog", "door"]
    assert trie.words() == sorted(word for word, _ in entries)

    print("\n--- duplicate inserts raise the weight ---")
    before = trie.autocomplete("cat", limit=1)
    trie.insert("cat", 4)
    after = trie.autocomplete("cat", limit=1)
    print(f"  cat weight {before[0][1]} -> {after[0][1]}")
    assert after[0][1] == before[0][1] + 4
    assert len(trie) == len(entries), "a duplicate must not add a word"

    print("\n--- longest common prefix ---")
    lcp_trie = Trie()
    for word in ["interstellar", "internet", "internal"]:
        lcp_trie.insert(word)
    print(f"  {lcp_trie.longest_common_prefix()!r}")
    assert lcp_trie.longest_common_prefix() == "inter"
    lcp_trie.insert("inter")
    assert lcp_trie.longest_common_prefix() == "inter", "a terminal node stops the walk"

    print("\n--- deletion prunes dead branches ---")
    pruning = Trie()
    for word in ["car", "cart", "carbon", "cat"]:
        pruning.insert(word)
    assert pruning.delete("car")
    assert not pruning.delete("car"), "deleting twice reports false"
    assert pruning.delete("carbon")
    assert pruning.delete("cart")
    assert pruning.delete("cat")
    assert len(pruning) == 0
    assert not pruning.startswith("c"), "empty branches were pruned"
    assert pruning.longest_common_prefix() == ""

    print("\n--- deleting an internal word keeps its children ---")
    internal = Trie()
    for word in ["car", "cart", "cargo"]:
        internal.insert(word)
    assert internal.delete("car")
    assert not internal.contains("car")
    assert internal.contains("cart") and internal.contains("cargo")
    assert internal.count_prefix("car") == 2
    print("  car removed; cart and cargo survive")

    print("\n--- wildcard search ---")
    assert trie.wildcard("ca.") == ["car", "cat"]
    assert trie.wildcard("ca..") == ["care", "cart"]
    assert trie.wildcard("d..") == ["dog"]
    assert trie.wildcard("...") == ["car", "cat", "dog"]
    assert trie.wildcard("zzz") == []
    print("  patterns match single characters only")

    print("\n--- unicode words ---")
    unicode_trie = Trie()
    for word in ["café", "caffeine", "cafétéria", "naïve"]:
        unicode_trie.insert(word)
    assert unicode_trie.count_prefix("caf") == 3
    assert unicode_trie.autocomplete("café")[0][0] == "café"
    print(f"  café*: {unicode_trie.words('café')}")

    print("\n--- invalid input ---")
    for action in [lambda: Trie().insert(""), lambda: Trie().insert("ok", 0), lambda: Trie().insert("ok", -1)]:
        try:
            action()
            raise AssertionError("expected a ValueError")
        except ValueError:
            pass
    assert Trie().delete("") is False
    print("  empty words and non-positive weights are rejected")

    print("\n--- against a dictionary ---")
    rng = random.Random(55)
    sample = Trie()
    reference: Counter[str] = Counter()
    alphabet = "abcd"

    for _ in range(400):
        word = "".join(rng.choice(alphabet) for _ in range(rng.randint(1, 5)))
        weight = rng.randint(1, 9)
        sample.insert(word, weight)
        reference[word] += weight

    assert len(sample) == len(reference)
    for word in reference:
        assert sample.contains(word)
    for _ in range(100):
        prefix = "".join(rng.choice(alphabet) for _ in range(rng.randint(0, 3)))
        expected = sum(1 for word in reference if word.startswith(prefix))
        assert sample.count_prefix(prefix) == expected, f"prefix {prefix!r}"
    print(f"  {len(reference)} random words: membership and prefix counts agree")

    to_delete = [word for word in reference if word.endswith("a")]
    for word in to_delete:
        assert sample.delete(word)
        del reference[word]
    assert len(sample) == len(reference)
    for word in reference:
        assert sample.contains(word)
    for word in to_delete:
        assert not sample.contains(word)
    for _ in range(50):
        prefix = "".join(rng.choice(alphabet) for _ in range(rng.randint(0, 3)))
        expected = sum(1 for word in reference if word.startswith(prefix))
        assert sample.count_prefix(prefix) == expected

    print(f"  deleted {len(to_delete)} words ending in 'a'; counts still agree")

    print("\nAll checks passed.")


if __name__ == "__main__":
    main()
