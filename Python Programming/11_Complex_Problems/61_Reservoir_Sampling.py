"""
Program 61: Reservoir Sampling

Picks a uniform random sample of k items from a stream of unknown length in a
single pass with O(k) memory. Algorithm R keeps the first k items and replaces a
random slot with probability k / (i + 1) for each later item. A weighted variant
(A-Res) gives each item a key derived from its weight, so heavier items are kept
proportionally more often.

Concepts: streaming algorithms, uniform sampling, weighted sampling, randomness.
"""

from __future__ import annotations

import random
from collections.abc import Iterable
from typing import TypeVar

T = TypeVar("T")


def reservoir_sample(
    stream: Iterable[T],
    size: int,
    rng: random.Random | None = None,
) -> list[T]:
    if size < 0:
        raise ValueError("sample size must not be negative")
    if size == 0:
        return []

    generator = rng or random.Random()
    reservoir: list[T] = []

    for index, item in enumerate(stream):
        if index < size:
            reservoir.append(item)
            continue

        slot = generator.randint(0, index)
        if slot < size:
            reservoir[slot] = item

    return reservoir


def weighted_reservoir_sample(
    items: Iterable[tuple[T, float]],
    size: int,
    rng: random.Random | None = None,
) -> list[T]:
    """A-Res: keep the items with the largest random() ** (1 / weight) keys."""
    if size < 0:
        raise ValueError("sample size must not be negative")
    if size == 0:
        return []

    generator = rng or random.Random()
    keyed: list[tuple[float, T]] = []

    for item, weight in items:
        if weight <= 0:
            raise ValueError("weights must be positive")
        key = generator.random() ** (1.0 / weight)
        keyed.append((key, item))

    keyed.sort(key=lambda pair: pair[0], reverse=True)
    return [item for _, item in keyed[:size]]


def main() -> None:
    print("--- basic sampling ---")
    items = list(range(20))
    sample = reservoir_sample(items, 5, random.Random(61))
    print("sample:", sample)
    assert len(sample) == 5
    assert len(set(sample)) == 5, "sampling is without replacement"
    assert all(item in items for item in sample)

    print("\n--- short streams ---")
    assert reservoir_sample([1, 2, 3], 5) == [1, 2, 3]
    assert reservoir_sample([], 3) == []
    assert reservoir_sample([1, 2, 3], 0) == []
    print("streams shorter than the sample are returned whole")

    print("\n--- argument validation ---")
    for invalid in (-1, -10):
        try:
            reservoir_sample([1, 2], invalid)
            raise AssertionError("negative sample size must be rejected")
        except ValueError:
            pass
        try:
            weighted_reservoir_sample([(1, 1.0)], invalid)
            raise AssertionError("negative sample size must be rejected")
        except ValueError:
            pass
    try:
        weighted_reservoir_sample([("x", 0)], 1)
        raise AssertionError("zero weight must be rejected")
    except ValueError:
        pass
    try:
        weighted_reservoir_sample([("x", -1)], 1)
        raise AssertionError("negative weight must be rejected")
    except ValueError:
        pass
    print("negative sizes and non-positive weights rejected")

    print("\n--- determinism ---")
    first = reservoir_sample(range(100), 10, random.Random(123))
    second = reservoir_sample(range(100), 10, random.Random(123))
    print("same seed:", first == second)
    assert first == second

    other = reservoir_sample(range(100), 10, random.Random(124))
    assert first != other, "different seeds should usually give a different sample"
    print("different seed gives a different sample")

    print("\n--- one pass over the stream ---")
    consumed = 0

    def counted() -> Iterable[int]:
        nonlocal consumed
        for value in range(100):
            consumed += 1
            yield value

    sample = reservoir_sample(counted(), 5, random.Random(7))
    print("items consumed:", consumed, "| sample size:", len(sample))
    assert consumed == 100, "the stream is read exactly once"
    assert len(sample) == 5

    print("\n--- uniformity: one of ten ---")
    trials = 20_000
    counts = {value: 0 for value in range(10)}
    generator = random.Random(61)
    for _ in range(trials):
        chosen = reservoir_sample(range(10), 1, generator)[0]
        counts[chosen] += 1

    expected = trials / 10
    print("counts:", counts)
    for value, count in counts.items():
        assert abs(count - expected) < expected * 0.1, f"value {value} was picked {count} times"
    print(f"each value was picked about {expected:.0f} times")

    print("\n--- uniformity: three of twenty ---")
    trials = 10_000
    inclusions = {value: 0 for value in range(20)}
    generator = random.Random(62)
    for _ in range(trials):
        chosen = reservoir_sample(range(20), 3, generator)
        assert len(set(chosen)) == 3
        for value in chosen:
            inclusions[value] += 1

    expected = trials * 3 / 20
    print("range of counts:", min(inclusions.values()), "-", max(inclusions.values()))
    for value, count in inclusions.items():
        assert abs(count - expected) < expected * 0.1, f"value {value}: {count} vs {expected:.0f}"
    print(f"each value was included about {expected:.0f} times")

    print("\n--- weighted sampling ---")
    weighted_items = [("heavy", 9.0), ("light", 1.0)]
    heavy = 0
    generator = random.Random(63)
    trials = 5_000
    for _ in range(trials):
        if weighted_reservoir_sample(weighted_items, 1, generator)[0] == "heavy":
            heavy += 1

    share = heavy / trials
    print(f"heavy chosen {share:.1%} of the time")
    assert 0.85 < share < 0.95, f"expected about 90%, got {share:.1%}"

    weighted_sample = weighted_reservoir_sample(
        [(f"item{index}", index + 1) for index in range(10)],
        3,
        random.Random(64),
    )
    print("weighted sample:", weighted_sample)
    assert len(weighted_sample) == 3
    assert len(set(weighted_sample)) == 3

    print("\n--- weighted determinism ---")
    items_with_weights = [(f"item{index}", float(index + 1)) for index in range(10)]
    one = weighted_reservoir_sample(items_with_weights, 4, random.Random(99))
    two = weighted_reservoir_sample(items_with_weights, 4, random.Random(99))
    assert one == two
    print("same seed gives the same weighted sample")

    print("\n--- a single item stream ---")
    assert reservoir_sample([42], 1) == [42]
    assert reservoir_sample([42], 5) == [42]
    assert weighted_reservoir_sample([(42, 1.0)], 1) == [42]
    print("single-item streams handled")

    print("\nAll checks passed.")


if __name__ == "__main__":
    main()
