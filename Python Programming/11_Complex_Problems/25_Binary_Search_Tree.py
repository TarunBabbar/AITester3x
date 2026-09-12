"""
Program 25: Binary Search Tree

Implements a BST with insert, search, delete, and the three depth-first
traversals, plus a height/balance report. Adds sample values and prints
the tree structure.

Concepts: classes, recursion, tree traversal, data structures.
"""

from __future__ import annotations


class Node:
    def __init__(self, value: int):
        self.value = value
        self.left: Node | None = None
        self.right: Node | None = None


class BinarySearchTree:
    def __init__(self):
        self.root: Node | None = None

    def insert(self, value: int) -> None:
        """Insert a value, ignoring duplicates."""
        if self.root is None:
            self.root = Node(value)
            return
        current = self.root
        while True:
            if value < current.value:
                if current.left is None:
                    current.left = Node(value)
                    return
                current = current.left
            elif value > current.value:
                if current.right is None:
                    current.right = Node(value)
                    return
                current = current.right
            else:
                return  # duplicate

    def contains(self, value: int) -> bool:
        current = self.root
        while current is not None:
            if value == current.value:
                return True
            current = current.left if value < current.value else current.right
        return False

    def _delete(self, node: Node | None, value: int) -> Node | None:
        if node is None:
            return None
        if value < node.value:
            node.left = self._delete(node.left, value)
        elif value > node.value:
            node.right = self._delete(node.right, value)
        else:
            if node.left is None:
                return node.right
            if node.right is None:
                return node.left
            successor = node.right
            while successor.left is not None:
                successor = successor.left
            node.value = successor.value
            node.right = self._delete(node.right, successor.value)
        return node

    def delete(self, value: int) -> None:
        self.root = self._delete(self.root, value)

    def inorder(self) -> list[int]:
        """Left, node, right -> sorted ascending."""
        result: list[int] = []

        def walk(node: Node | None) -> None:
            if node:
                walk(node.left)
                result.append(node.value)
                walk(node.right)

        walk(self.root)
        return result

    def preorder(self) -> list[int]:
        result: list[int] = []

        def walk(node: Node | None) -> None:
            if node:
                result.append(node.value)
                walk(node.left)
                walk(node.right)

        walk(self.root)
        return result

    def postorder(self) -> list[int]:
        result: list[int] = []

        def walk(node: Node | None) -> None:
            if node:
                walk(node.left)
                walk(node.right)
                result.append(node.value)

        walk(self.root)
        return result

    def height(self) -> int:
        def walk(node: Node | None) -> int:
            if node is None:
                return 0
            return 1 + max(walk(node.left), walk(node.right))

        return walk(self.root)

    def show(self) -> None:
        """Pretty-print the tree rotated 90 degrees (root on the left)."""
        def walk(node: Node | None, depth: int) -> None:
            if node is None:
                return
            walk(node.right, depth + 1)
            print("    " * depth + str(node.value))
            walk(node.left, depth + 1)

        walk(self.root, 0)


def main() -> None:
    tree = BinarySearchTree()
    sample = [50, 30, 70, 20, 40, 60, 80, 35, 65]
    for value in sample:
        tree.insert(value)

    print("Inserted:", sample)
    print("\nTree structure (rotated, root first):")
    tree.show()

    print(f"\nInorder:   {tree.inorder()}  (sorted)")
    print(f"Preorder:  {tree.preorder()}")
    print(f"Postorder: {tree.postorder()}")
    print(f"Height:    {tree.height()}")

    print(f"\nContains 40? {tree.contains(40)}")
    print(f"Contains 41? {tree.contains(41)}")

    tree.delete(30)
    print("\nAfter deleting 30:")
    print(f"  Inorder: {tree.inorder()}")


if __name__ == "__main__":
    main()