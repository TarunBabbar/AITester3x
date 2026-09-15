"""
Program 35: Hash Table from Scratch

Builds a hash map with separate chaining: a list of buckets where each bucket
holds (key, value) pairs. Supports insert, lookup, update, delete, dynamic
resizing on load factor, and reports how evenly the keys are distributed.

Concepts: lists, hashing, collision handling, resizing, dunder methods.
"""


class HashMap:
    """A minimal string-keyed hash map using separate chaining."""

    def __init__(self, initial_buckets: int = 4) -> None:
        self._buckets: list[list[tuple[str, int]]] = [[] for _ in range(initial_buckets)]
        self._size = 0

    def _bucket_for(self, key: str) -> list[tuple[str, int]]:
        return self._buckets[hash(key) % len(self._buckets)]

    def __setitem__(self, key: str, value: int) -> None:
        bucket = self._bucket_for(key)
        for position, (existing, _) in enumerate(bucket):
            if existing == key:
                bucket[position] = (key, value)   # update in place
                return
        bucket.append((key, value))
        self._size += 1
        if self.load_factor() > 0.75:
            self._resize(len(self._buckets) * 2)

    def __getitem__(self, key: str) -> int:
        for existing, value in self._bucket_for(key):
            if existing == key:
                return value
        raise KeyError(key)

    def __delitem__(self, key: str) -> None:
        bucket = self._bucket_for(key)
        for position, (existing, _) in enumerate(bucket):
            if existing == key:
                del bucket[position]
                self._size -= 1
                return
        raise KeyError(key)

    def __contains__(self, key: str) -> bool:
        try:
            self[key]
            return True
        except KeyError:
            return False

    def __len__(self) -> int:
        return self._size

    def get(self, key: str, default: int | None = None) -> int | None:
        try:
            return self[key]
        except KeyError:
            return default

    def load_factor(self) -> float:
        return self._size / len(self._buckets)

    def _resize(self, new_size: int) -> None:
        pairs = [pair for bucket in self._buckets for pair in bucket]
        self._buckets = [[] for _ in range(new_size)]
        self._size = 0
        for key, value in pairs:
            self[key] = value

    def distribution(self) -> dict:
        lengths = [len(bucket) for bucket in self._buckets]
        return {
            "buckets": len(self._buckets),
            "used": sum(1 for length in lengths if length),
            "longest_chain": max(lengths, default=0),
            "load_factor": self.load_factor(),
        }


def main() -> None:
    table = HashMap(initial_buckets=4)

    for number in range(12):
        table[f"key{number}"] = number * 10

    print(f"Inserted 12 keys, size={len(table)}, buckets={len(table._buckets)}")
    assert len(table) == 12
    assert table["key0"] == 0 and table["key11"] == 110

    table["key5"] = 999
    print(f"Updated key5 -> {table['key5']}")
    assert table["key5"] == 999
    assert len(table) == 12, "updating an existing key must not grow the map"

    del table["key5"]
    print(f"Deleted key5, size={len(table)}, contains key5: {'key5' in table}")
    assert len(table) == 11
    assert "key5" not in table
    assert table.get("key5") is None and table.get("key5", -1) == -1

    try:
        table["missing"]
    except KeyError as exc:
        print(f"Lookup of missing key -> KeyError({exc})")

    print(f"\nDistribution: {table.distribution()}")
    print("All keys still retrievable after growth:")
    assert all(table[f"key{n}"] == n * 10 for n in range(12) if n != 5)


if __name__ == "__main__":
    main()
