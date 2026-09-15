"""
Program 34: Singly Linked List

Implements a singly linked list from scratch - append, prepend, remove, index,
reverse and iteration - and exercises each operation with assertions.

Concepts: classes, dataclasses, references, loops, dunder methods, generators.
"""

from dataclasses import dataclass


@dataclass
class Node:
    value: int
    next: "Node | None" = None


class LinkedList:
    """A singly linked list keeping head, tail and size in sync."""

    def __init__(self) -> None:
        self.head: Node | None = None
        self.tail: Node | None = None
        self._size = 0

    def append(self, value: int) -> None:
        node = Node(value)
        if self.tail is None:
            self.head = self.tail = node
        else:
            self.tail.next = node
            self.tail = node
        self._size += 1

    def prepend(self, value: int) -> None:
        node = Node(value, self.head)
        self.head = node
        if self.tail is None:
            self.tail = node
        self._size += 1

    def remove(self, value: int) -> bool:
        previous: Node | None = None
        current = self.head
        while current is not None:
            if current.value == value:
                if previous is None:
                    self.head = current.next
                else:
                    previous.next = current.next
                if current is self.tail:
                    self.tail = previous
                self._size -= 1
                return True
            previous, current = current, current.next
        return False

    def index(self, value: int) -> int:
        for position, item in enumerate(self):
            if item == value:
                return position
        raise ValueError(f"{value!r} is not in the list")

    def reverse(self) -> None:
        """Reverse in place, keeping head and tail correct."""
        previous: Node | None = None
        current = self.head
        self.tail = self.head
        while current is not None:
            following = current.next
            current.next = previous
            previous = current
            current = following
        self.head = previous

    def to_list(self) -> list[int]:
        return list(self)

    def __iter__(self):
        current = self.head
        while current is not None:
            yield current.value
            current = current.next

    def __len__(self) -> int:
        return self._size

    def __contains__(self, value: int) -> bool:
        return any(item == value for item in self)

    def __repr__(self) -> str:
        return f"LinkedList({self.to_list()})"


def main() -> None:
    numbers = LinkedList()
    for value in range(1, 6):
        numbers.append(value)
    numbers.prepend(0)
    print(f"Built:            {numbers}")
    assert numbers.to_list() == [0, 1, 2, 3, 4, 5]
    assert len(numbers) == 6
    assert numbers.head.value == 0 and numbers.tail.value == 5

    assert numbers.remove(3) is True
    print(f"After remove(3):  {numbers}")
    assert numbers.to_list() == [0, 1, 2, 4, 5]
    assert numbers.remove(99) is False, "removing a missing value returns False"

    assert numbers.index(4) == 3
    assert 2 in numbers and 99 not in numbers

    numbers.reverse()
    print(f"After reverse():  {numbers}")
    assert numbers.to_list() == [5, 4, 2, 1, 0]
    assert numbers.head.value == 5 and numbers.tail.value == 0

    numbers.append(9)
    print(f"After append(9):  {numbers}")
    assert numbers.tail.value == 9, "tail must still be valid after a reverse"

    single = LinkedList()
    single.append(42)
    single.remove(42)
    assert single.head is None and single.tail is None and len(single) == 0
    print("\nEdge cases (empty list, remove head/tail) behave correctly.")


if __name__ == "__main__":
    main()
