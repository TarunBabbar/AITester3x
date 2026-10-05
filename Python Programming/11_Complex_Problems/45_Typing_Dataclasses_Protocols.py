"""
Program 45: Typing, Dataclasses and Protocols

Type hints that tooling checks but the interpreter does not, dataclasses for
plain data with the boilerplate generated, and protocols for structural typing:
a class satisfies a protocol by having the right shape, with no inheritance.

Concepts: dataclasses, field, frozen, replace, asdict, typing, Protocol, Generic.

    python 45_Typing_Dataclasses_Protocols.py
"""

from __future__ import annotations

from dataclasses import asdict, dataclass, field, fields, replace
from typing import (
    Callable,
    Generic,
    Iterator,
    Literal,
    Protocol,
    TypedDict,
    TypeVar,
    get_type_hints,
    runtime_checkable,
)

T = TypeVar("T")


# ---------------------------------------------------------------- dataclasses
@dataclass(frozen=True)
class Money:
    """Immutable value type: usable as a dictionary key, comparable, printable."""

    amount: int                       # in the smallest unit, to avoid floats
    currency: str = "INR"

    def __add__(self, other: Money) -> Money:
        if self.currency != other.currency:
            raise ValueError(f"cannot add {self.currency} to {other.currency}")
        return Money(self.amount + other.amount, self.currency)

    def __str__(self) -> str:
        return f"{self.amount / 100:.2f} {self.currency}"


@dataclass
class Basket:
    """Mutable, with a factory for the list so instances do not share one."""

    owner: str
    items: list[Money] = field(default_factory=list)
    notes: str = field(default="", repr=False)

    def __post_init__(self) -> None:
        if not self.owner:
            raise ValueError("a basket needs an owner")

    def add(self, price: Money) -> None:
        self.items.append(price)

    def total(self) -> Money:
        total = Money(0)
        for item in self.items:
            total = total + item
        return total


# ------------------------------------------------------------------ protocols
@runtime_checkable
class HasLength(Protocol):
    """Anything with __len__ satisfies this, no matter what it inherits from."""

    def __len__(self) -> int: ...


@runtime_checkable
class Greeter(Protocol):
    def greet(self) -> str: ...


@dataclass
class Robot:
    name: str

    def greet(self) -> str:
        return f"beep, I am {self.name}"


def describe_length(value: HasLength) -> str:
    """Accepts a list, a string, a dict, or any custom type with __len__."""
    return f"length {len(value)}"


def greet_everyone(greeters: list[Greeter]) -> list[str]:
    return [greeter.greet() for greeter in greeters]


# -------------------------------------------------------------------- generic
@dataclass
class Stack(Generic[T]):
    """A generic container; the type parameter is documentation, not enforcement."""

    items: list[T] = field(default_factory=list)

    def push(self, item: T) -> None:
        self.items.append(item)

    def pop(self) -> T:
        if not self.items:
            raise IndexError("the stack is empty")
        return self.items.pop()

    def __len__(self) -> int:
        return len(self.items)


# ------------------------------------------------------------------ TypedDict
class Ticket(TypedDict):
    """A dictionary with a declared shape. At runtime it is still just a dict."""

    key: str
    priority: Literal["low", "medium", "high"]
    estimate_hours: float


# ------------------------------------------------------------ plain functions
def add(left: int, right: int) -> int:
    """The hints say int, but nothing stops you passing strings."""
    return left + right


def find_user(user_id: str) -> str | None:
    """A union with None, the modern way of saying Optional[str]."""
    return "ada" if user_id == "1" else None


def evens(limit: int) -> Iterator[int]:
    """Annotated as an iterator, because that is what a generator returns."""
    for number in range(limit):
        if number % 2 == 0:
            yield number


def apply_twice(function: Callable[[T], T], value: T) -> T:
    """A Callable hint makes the function's shape part of the signature."""
    return function(function(value))


def summarise(level: Literal["info", "warn", "error"], message: str) -> str:
    return f"[{level.upper()}] {message}"


def main() -> None:
    # ---- dataclass behaviour ------------------------------------------------
    coffee = Money(250)
    cake = Money(375)
    assert coffee == Money(250), "equal by value, not by identity"
    assert coffee != cake
    assert str(cake) == "3.75 INR", "a custom __str__ is used"
    assert str(coffee + cake) == "6.25 INR", "and the operators work"
    assert repr(Money(100, "USD")) == "Money(amount=100, currency='USD')", "a generated repr"
    print(f"dataclasses : {coffee} + {cake} = {coffee + cake}")

    try:
        Money(100, "USD") + Money(100, "INR")
        raise AssertionError("different currencies should not add")
    except ValueError as error:
        print(f"guard       : {error}")

    # frozen means immutable, and hashable
    try:
        coffee.amount = 999
        raise AssertionError("a frozen dataclass should reject assignment")
    except Exception as error:
        assert type(error).__name__ == "FrozenInstanceError", f"unexpected error {error!r}"
        print(f"frozen      : assignment raised {type(error).__name__}")
    assert {coffee: "cold brew"}[Money(250)] == "cold brew", \
        "frozen dataclasses work as dictionary keys"

    # default_factory gives every instance its own list
    first = Basket("ada")
    second = Basket("grace")
    first.add(Money(100))
    assert len(first.items) == 1 and second.items == [], "the lists are not shared"
    print(f"factory     : {first.owner} has {len(first.items)} item, {second.owner} has none")

    # __post_init__ runs validation
    try:
        Basket("")
        raise AssertionError("an empty owner should be rejected")
    except ValueError as error:
        print(f"validated   : {error}")

    # asdict, replace and fields
    basket = Basket("ada", [Money(100), Money(250)], notes="for the team")
    as_dict = asdict(basket)
    assert as_dict["owner"] == "ada", "asdict converts the nested dataclasses too"
    assert as_dict["items"][0] == {"amount": 100, "currency": "INR"}, "and recurses into the list"
    assert "notes" in as_dict, "even the repr=False field is included"
    bigger = replace(basket, owner="grace")
    assert bigger.owner == "grace" and len(bigger.items) == 2, "replace copies the rest"
    assert [f.name for f in fields(Money)] == ["amount", "currency"], "fields reports the order"
    print(f"helpers     : total {basket.total()}, replace -> {bigger.owner}")

    # ---- protocols are structural -------------------------------------------
    assert isinstance([1, 2, 3], HasLength), "a list satisfies the protocol"
    assert isinstance("hello", HasLength), "so does a string"
    assert isinstance({"a": 1}, HasLength), "and a dict"

    class Wrapper:
        def __len__(self) -> int:
            return 7

    assert isinstance(Wrapper(), HasLength), "a class that never heard of the protocol satisfies it"
    assert not isinstance(42, HasLength), "an int has no __len__"
    assert not isinstance(None, HasLength), "and neither does None"
    print(f"protocol    : list -> {describe_length([1, 2, 3])}, "
          f"wrapper -> {describe_length(Wrapper())}")

    assert greet_everyone([Robot("r2d2"), Robot("c3po")]) == ["beep, I am r2d2", "beep, I am c3po"]
    assert isinstance(Robot("x"), Greeter), "the robot satisfies Greeter without inheriting it"
    print(f"protocol    : {greet_everyone([Robot('r2d2')])}")

    # runtime_checkable only checks names, not signatures
    class WrongShape:
        def greet(self, name: str) -> str:      # takes an argument, unlike the protocol
            return f"hello {name}"

    assert isinstance(WrongShape(), Greeter), "isinstance passes: only the name is checked"
    print("protocol    : isinstance checks names only, not signatures")

    # ---- generics ------------------------------------------------------------
    stack: Stack[int] = Stack()
    stack.push(1)
    stack.push(2)
    assert stack.pop() == 2 and len(stack) == 1, "last in, first out"
    try:
        Stack().pop()
        raise AssertionError("an empty stack should raise")
    except IndexError as error:
        print(f"generic     : {error}")

    # ---- TypedDict ----------------------------------------------------------
    ticket: Ticket = {"key": "VWO-114", "priority": "high", "estimate_hours": 4.5}
    assert isinstance(ticket, dict), "a TypedDict is a plain dict at runtime"
    assert ticket["priority"] == "high"
    assert set(Ticket.__annotations__) == {"key", "priority", "estimate_hours"}
    print(f"typeddict   : keys {sorted(Ticket.__annotations__)}")

    # ---- hints are not enforced ---------------------------------------------
    assert add(2, 3) == 5, "with ints it behaves as documented"
    assert add("a", "b") == "ab", "with strings it still runs: the hint is not a check"
    print("hints       : add('a', 'b') returned 'ab' despite the int hints")

    assert find_user("1") == "ada"
    assert find_user("2") is None, "the union with None is the documented way to say 'maybe'"
    assert list(evens(10)) == [0, 2, 4, 6, 8], "a generator annotated as Iterator"
    assert apply_twice(lambda x: x + 3, 1) == 7, "a Callable hint with a generic type"
    assert summarise("warn", "disk almost full") == "[WARN] disk almost full"
    print(f"annotations : {summarise('warn', 'disk almost full')}")

    # get_type_hints resolves the annotations at runtime
    hints = get_type_hints(Money)
    assert set(hints) == {"amount", "currency"}, f"unexpected hints {hints}"
    assert hints["currency"] is str, "and resolves to real types"
    print(f"hints       : Money -> {hints}")

    # ---- the mutable default that dataclasses prevent -----------------------
    class HandWritten:
        def __init__(self, items: list[int] = []) -> None:      # noqa: B006 - the bug
            self.items = items

    one = HandWritten()
    two = HandWritten()
    one.items.append(1)
    assert two.items == [1], "the hand written default is shared, which is the classic bug"
    assert Basket("x").items == [], "while the dataclass factory avoids it"
    print("gotcha      : a shared default list versus a default_factory")
    print("All checks passed.")


if __name__ == "__main__":
    main()
