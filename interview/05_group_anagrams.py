"""
Group Anagrams — Classic Interview Question

Given a list of strings, group the anagrams together. Two strings are
anagrams if they use the same letters the same number of times.

Example:
    Input:  ["eat", "tea", "tan", "ate", "nat", "bat"]
    Output: [["eat", "tea", "ate"], ["tan", "nat"], ["bat"]]

Interview tips:
    - The key insight: anagrams share the same sorted character string.
      "eat", "tea", "ate" all sort to "aet".
    - A dict of lists gives O(n * k log k) where n is the word count and
      k is the longest word length.
    - Alternative key (O(n * k)): a tuple of character counts. Mention it
      as a follow-up optimization.
"""

from collections import defaultdict
from typing import Dict, List


def group_anagrams(words: List[str]) -> List[List[str]]:
    """Group words that are anagrams of each other."""
    groups: Dict[str, List[str]] = defaultdict(list)

    for word in words:
        key = "".join(sorted(word))  # canonical anagram key
        groups[key].append(word)

    # Return the groups; order doesn't matter, but sort by first word
    # for deterministic output in the demo below.
    return [sorted(group) for group in groups.values()]


if __name__ == "__main__":
    words = ["eat", "tea", "tan", "ate", "nat", "bat"]
    result = group_anagrams(words)
    print(f"group_anagrams({words}) ->")
    for group in sorted(result, key=lambda g: g[0]):
        print("  ", group)

    # Sanity checks
    expected = {frozenset(g) for g in [["ate", "eat", "tea"], ["nat", "tan"], ["bat"]]}
    actual = {frozenset(g) for g in result}
    assert actual == expected, "Anagram groups mismatch"

    assert group_anagrams([""]) == [[""]]
    assert group_anagrams(["a"]) == [["a"]]
    print("All sanity checks passed.")
