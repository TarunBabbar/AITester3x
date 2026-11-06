"""
Program 51: Minimum Edit Distance (Levenshtein)

Computes the edit distance between two strings with dynamic programming,
reconstructs one optimal sequence of insert/delete/substitute operations,
and ranks spelling suggestions by distance. The distance itself is computed
with a rolling row, while the script needs the full table so it can backtrack.

Concepts: dynamic programming, backtracking, dataclasses, space optimisation.
"""

from __future__ import annotations

import random
from dataclasses import dataclass


@dataclass(frozen=True)
class Edit:
    kind: str
    source: str
    target: str

    def __str__(self) -> str:
        if self.kind == "match":
            return f"  match      {self.source!r}"
        if self.kind == "substitute":
            return f"  substitute {self.source!r} -> {self.target!r}"
        if self.kind == "delete":
            return f"  delete     {self.source!r}"
        return f"  insert     {self.target!r}"


def edit_distance(source: str, target: str) -> int:
    """Levenshtein distance keeping only the previous row in memory."""
    previous = list(range(len(target) + 1))

    for i, source_char in enumerate(source, start=1):
        current = [i]
        for j, target_char in enumerate(target, start=1):
            substitution_cost = 0 if source_char == target_char else 1
            current.append(
                min(
                    previous[j] + 1,                    # delete source_char
                    current[j - 1] + 1,                 # insert target_char
                    previous[j - 1] + substitution_cost,  # keep or substitute
                )
            )
        previous = current

    return previous[-1]


def edit_table(source: str, target: str) -> list[list[int]]:
    rows, columns = len(source) + 1, len(target) + 1
    table = [[0] * columns for _ in range(rows)]

    for row in range(rows):
        table[row][0] = row
    for column in range(columns):
        table[0][column] = column

    for row in range(1, rows):
        for column in range(1, columns):
            substitution_cost = 0 if source[row - 1] == target[column - 1] else 1
            table[row][column] = min(
                table[row - 1][column] + 1,
                table[row][column - 1] + 1,
                table[row - 1][column - 1] + substitution_cost,
            )

    return table


def edit_script(source: str, target: str) -> list[Edit]:
    """One optimal sequence of operations that turns source into target."""
    table = edit_table(source, target)
    edits: list[Edit] = []
    row, column = len(source), len(target)

    while row > 0 or column > 0:
        if row > 0 and column > 0:
            substitution_cost = 0 if source[row - 1] == target[column - 1] else 1
            if table[row][column] == table[row - 1][column - 1] + substitution_cost:
                if substitution_cost == 0:
                    edits.append(Edit("match", source[row - 1], target[column - 1]))
                else:
                    edits.append(Edit("substitute", source[row - 1], target[column - 1]))
                row -= 1
                column -= 1
                continue

        if row > 0 and table[row][column] == table[row - 1][column] + 1:
            edits.append(Edit("delete", source[row - 1], ""))
            row -= 1
        else:
            edits.append(Edit("insert", "", target[column - 1]))
            column -= 1

    edits.reverse()
    return edits


def apply_script(source: str, edits: list[Edit]) -> str:
    """Run the operations against source without looking at target."""
    produced: list[str] = []
    for edit in edits:
        if edit.kind in ("match", "substitute", "insert"):
            produced.append(edit.target)
        elif edit.kind != "delete":
            raise ValueError(f"unknown edit kind: {edit.kind}")
    return "".join(produced)


def suggest(word: str, vocabulary: list[str], limit: int = 3) -> list[tuple[str, int]]:
    scored = [(edit_distance(word, candidate), candidate) for candidate in vocabulary]
    scored.sort(key=lambda pair: (pair[0], pair[1]))
    return [(candidate, distance) for distance, candidate in scored[:limit]]


def main() -> None:
    print("--- known distances ---")
    cases = [
        ("", "", 0),
        ("abc", "abc", 0),
        ("", "abc", 3),
        ("abc", "", 3),
        ("kitten", "sitting", 3),
        ("flaw", "lawn", 2),
        ("saturday", "sunday", 3),
        ("intention", "execution", 5),
        ("café", "cafe", 1),
    ]
    for source, target, expected in cases:
        distance = edit_distance(source, target)
        print(f"  {source!r:12} -> {target!r:12} = {distance}")
        assert distance == expected, f"{source!r} -> {target!r}: {distance} != {expected}"
        assert edit_table(source, target)[-1][-1] == distance, "row and table disagree"

    print("\n--- edit script for kitten -> sitting ---")
    edits = edit_script("kitten", "sitting")
    for edit in edits:
        print(edit)
    print(f"  operations: {len(edits)}, distance: {edit_distance('kitten', 'sitting')}")
    assert apply_script("kitten", edits) == "sitting"
    assert len([edit for edit in edits if edit.kind != "match"]) == edit_distance("kitten", "sitting")

    print("\n--- script for an insertion and a deletion ---")
    for source, target in [("", "abc"), ("abc", ""), ("python", "python"), ("book", "back")]:
        script = edit_script(source, target)
        print(f"  {source!r} -> {target!r}")
        for edit in script:
            if edit.kind != "match":
                print(edit)
        assert apply_script(source, script) == target

    print("\n--- randomised cross-check ---")
    rng = random.Random(7)
    alphabet = "abcd"
    checked = 0
    for _ in range(200):
        source = "".join(rng.choice(alphabet) for _ in range(rng.randint(0, 8)))
        target = "".join(rng.choice(alphabet) for _ in range(rng.randint(0, 8)))
        distance = edit_distance(source, target)
        script = edit_script(source, target)
        changes = [edit for edit in script if edit.kind != "match"]
        assert len(changes) == distance, f"{source!r} -> {target!r}: {len(changes)} != {distance}"
        assert apply_script(source, script) == target, f"script failed for {source!r} -> {target!r}"
        assert edit_distance(target, source) == distance, "distance should be symmetric"
        checked += 1

    print(f"  {checked} random pairs: script length matches the distance, output matches the target")

    print("\n--- spelling suggestions ---")
    vocabulary = ["kitten", "sitting", "kitchen", "mitten", "written", "smitten", "listen"]
    for word in ["kittin", "sittng", "writen"]:
        print(f"  {word!r}: {suggest(word, vocabulary)}")
        assert suggest(word, vocabulary)[0][0] in vocabulary

    best = suggest("kittin", vocabulary, limit=1)
    assert best[0][0] == "kitten" and best[0][1] == 1, best
    print("\nAll checks passed.")


if __name__ == "__main__":
    main()
