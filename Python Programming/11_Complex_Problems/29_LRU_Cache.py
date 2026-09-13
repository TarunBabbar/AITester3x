"""
Program 29: LRU Cache (Least Recently Used)

Implements a fixed-capacity cache that evicts the least recently used entry
when full, using a dict for O(1) lookups. A second version wraps Python's
built-in functools.lru_cache to show the standard-library equivalent.

Concepts: classes, dict ordering, OOP, functools, cache hit/miss metrics.
"""

from collections import OrderedDict
from functools import lru_cache


class LRUCache:
    """A cache with a maximum size; the oldest unused key is evicted first."""

    def __init__(self, capacity: int) -> None:
        if capacity < 1:
            raise ValueError("capacity must be at least 1")
        self.capacity = capacity
        self._store: OrderedDict[str, int] = OrderedDict()
        self.hits = 0
        self.misses = 0

    def get(self, key: str) -> int | None:
        if key in self._store:
            self._store.move_to_end(key)
            self.hits += 1
            return self._store[key]
        self.misses += 1
        return None

    def put(self, key: str, value: int) -> None:
        if key in self._store:
            self._store.move_to_end(key)
        self._store[key] = value
        if len(self._store) > self.capacity:
            evicted, _ = self._store.popitem(last=False)
            print(f"  evicted {evicted!r} (least recently used)")

    def __len__(self) -> int:
        return len(self._store)

    def __repr__(self) -> str:
        items = ", ".join(f"{k}={v}" for k, v in self._store.items())
        return f"LRUCache(capacity={self.capacity}, [{items}])"


@lru_cache(maxsize=3)
def slow_square(n: int) -> int:
    """Pretend this is expensive; lru_cache gives it the same behaviour."""
    print(f"  computing square of {n}")
    return n * n


def main() -> None:
    cache = LRUCache(capacity=3)
    print("Inserting a, b, c:")
    cache.put("a", 1)
    cache.put("b", 2)
    cache.put("c", 3)
    print(f"  {cache}")

    print("\nAccessing 'a' (makes it most recently used):")
    assert cache.get("a") == 1

    print("Inserting 'd' -> 'b' should be evicted:")
    cache.put("d", 4)
    print(f"  {cache}")

    assert cache.get("b") is None, "b should have been evicted"
    assert cache.get("a") == 1 and cache.get("d") == 4
    print(f"\nHits: {cache.hits}, Misses: {cache.misses}")

    print("\nfunctools.lru_cache(maxsize=3):")
    for n in (2, 3, 2, 4, 5, 2):
        slow_square(n)
    print(f"  cache info: {slow_square.cache_info()}")


if __name__ == "__main__":
    main()
