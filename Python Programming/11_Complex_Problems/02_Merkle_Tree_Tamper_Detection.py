"""
Program 2: Merkle Tree with Tamper Detection and JSON Persistence

A memory-safe Merkle tree (hash tree) that:
- Builds a balanced tree of SHA-256 hashes from leaf data.
- Produces a compact root hash that fingerprints the entire dataset.
- Detects which leaf was tampered with by comparing proofs against the root.
- Serializes to / loads from JSON.

Concepts: hashing (hashlib), recursion, tree traversal, data integrity,
          immutable dataclasses, JSON round-tripping.
"""

from __future__ import annotations

import hashlib
import json
from dataclasses import dataclass
from pathlib import Path

STORAGE_FILE = Path(__file__).with_name("merkle_tree.json")


def sha256(data: bytes) -> str:
    """Hex digest helper."""
    return hashlib.sha256(data).hexdigest()


@dataclass(frozen=True)
class MerkleNode:
    """A node in the tree; leaves hold data, internal nodes hold child refs."""
    hash: str
    data: bytes | None = None
    left: "MerkleNode | None" = None
    right: "MerkleNode | None" = None

    def is_leaf(self) -> bool:
        return self.data is not None


class MerkleTree:
    def __init__(self, leaves: list[bytes]) -> None:
        if not leaves:
            raise ValueError("Merkle tree needs at least one leaf")
        self.leaves = list(leaves)
        self.root = self._build(self.leaves)

    def _build(self, level: list[bytes | MerkleNode]) -> MerkleNode:
        """Recursively pair up nodes until one root remains."""
        if len(level) == 1:
            node = level[0]
            return node if isinstance(node, MerkleNode) else MerkleNode(sha256(node), data=node)
        # Duplicate the last node when the level has an odd count.
        if len(level) % 2 == 1:
            level = level + [level[-1]]
        parents: list[MerkleNode] = []
        for i in range(0, len(level), 2):
            left = self._node_or_leaf(level[i])
            right = self._node_or_leaf(level[i + 1])
            parents.append(MerkleNode(sha256((left.hash + right.hash).encode()), left=left, right=right))
        return self._build(parents)

    @staticmethod
    def _node_or_leaf(item: bytes | MerkleNode) -> MerkleNode:
        return item if isinstance(item, MerkleNode) else MerkleNode(sha256(item), data=item)

    def root_hash(self) -> str:
        return self.root.hash

    def verify(self) -> bool:
        """Recompute the tree from the leaves and compare root hashes."""
        return self._build(self.leaves).hash == self.root.hash

    def proof(self, index: int) -> list[tuple[str, str]]:
        """Return [(sibling_hash, 'left'|'right')] proving leaf `index` is in the tree."""
        if not 0 <= index < len(self.leaves):
            raise IndexError(f"leaf index {index} out of range")
        width = 1
        while width < len(self.leaves):
            width *= 2
        return self._proof(self.root, index, width)

    def _proof(self, node: MerkleNode, index: int, width: int) -> list[tuple[str, str]]:
        if node.is_leaf():
            return []
        half = width // 2
        if index < half:
            assert node.right is not None
            return self._proof(node.left, index, half) + [(node.right.hash, "right")]
        assert node.left is not None
        return self._proof(node.right, index - half, half) + [(node.left.hash, "left")]

    def check_proof(self, leaf: bytes, index: int, proof: list[tuple[str, str]]) -> bool:
        """Verify a leaf against the root using its proof (no tree needed)."""
        digest = sha256(leaf)
        for sibling_hash, side in proof:
            combined = (digest + sibling_hash).encode() if side == "right" else (sibling_hash + digest).encode()
            digest = sha256(combined)
        return digest == self.root.hash

    def to_dict(self) -> dict:
        def serialize(node: MerkleNode) -> dict:
            if node.is_leaf():
                return {"hash": node.hash, "data": node.data.hex()}
            return {"hash": node.hash, "left": serialize(node.left), "right": serialize(node.right)}

        return {"leaves": [leaf.hex() for leaf in self.leaves], "tree": serialize(self.root)}

    @classmethod
    def from_dict(cls, data: dict) -> "MerkleTree":
        leaves = [bytes.fromhex(h) for h in data["leaves"]]

        def deserialize(item: dict) -> MerkleNode:
            if "data" in item:
                return MerkleNode(item["hash"], data=bytes.fromhex(item["data"]))
            return MerkleNode(item["hash"], left=deserialize(item["left"]), right=deserialize(item["right"]))

        tree = cls.__new__(cls)
        tree.leaves = leaves
        tree.root = deserialize(data["tree"])
        return tree

    def save(self, path: Path = STORAGE_FILE) -> None:
        path.write_text(json.dumps(self.to_dict(), indent=2), encoding="utf-8")

    @classmethod
    def load(cls, path: Path = STORAGE_FILE) -> "MerkleTree":
        return cls.from_dict(json.loads(path.read_text(encoding="utf-8")))


def demo() -> None:
    records = [f"record-{i}: salary $1000{i}".encode() for i in range(1, 9)]
    tree = MerkleTree(records)
    print("Leaves:", len(records))
    print("Root hash:", tree.root_hash())
    print("Tree verifies:", tree.verify())
    assert tree.verify()

    # Tamper detection: change one leaf in the STORED tree copy.
    corrupted = MerkleTree(records)
    corrupted.leaves[3] = b"record-4: salary $99999"   # attacker edits data
    print("\nAfter tampering with leaf 3:")
    print("  Stored root unchanged:", corrupted.root_hash() == tree.root_hash())
    print("  Recompute root differs:", corrupted.verify() is False)
    assert corrupted.root_hash() == tree.root_hash()
    assert corrupted.verify() is False

    # Proof-based check: honest leaf passes, tampered leaf fails.
    proof = tree.proof(3)
    print("\nProof for leaf 3 has", len(proof), "sibling hashes")
    print("  Honest leaf 3 passes proof:", tree.check_proof(records[3], 3, proof))
    print("  Tampered leaf 3 fails proof:", not corrupted.check_proof(corrupted.leaves[3], 3, proof))
    assert tree.check_proof(records[3], 3, proof)
    assert not corrupted.check_proof(corrupted.leaves[3], 3, proof)

    # Every honest leaf must verify, including odd-sized trees.
    for count in range(1, 10):
        leaves = [f"leaf-{index}".encode() for index in range(count)]
        sample = MerkleTree(leaves)
        for index, leaf in enumerate(leaves):
            assert sample.check_proof(leaf, index, sample.proof(index))
    print("  Proofs verify for every leaf of trees with 1..9 leaves")

    # Persistence round-trip.
    tree.save()
    loaded = MerkleTree.load()
    print("\nSaved to", STORAGE_FILE.name)
    print("Round-trip root matches:", loaded.root_hash() == tree.root_hash())


if __name__ == "__main__":
    demo()
