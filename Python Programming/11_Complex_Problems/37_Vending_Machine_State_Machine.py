"""
Program 37: Vending Machine State Machine

Models a vending machine as an explicit state machine: IDLE -> HAS_CREDIT ->
DISPENSING -> IDLE, with OUT_OF_STOCK when a slot is empty. Coins are inserted
in cents, products have prices and stock, and every state change is recorded.

Concepts: enums, classes, dataclasses, state machines, custom exceptions.
"""

from dataclasses import dataclass, field
from enum import Enum, auto


class State(Enum):
    IDLE = auto()
    HAS_CREDIT = auto()
    DISPENSING = auto()
    OUT_OF_STOCK = auto()


class VendingError(Exception):
    """Base class for vending machine problems."""


class UnknownProduct(VendingError):
    pass


class OutOfStock(VendingError):
    pass


class InsufficientCredit(VendingError):
    pass


VALID_COINS = {5, 10, 25, 50, 100}


@dataclass
class Product:
    name: str
    price: int
    stock: int


@dataclass
class VendingMachine:
    products: dict[str, Product]
    credit: int = 0
    state: State = State.IDLE
    history: list[str] = field(default_factory=list)

    def _transition(self, new_state: State) -> None:
        self.history.append(f"{self.state.name} -> {new_state.name}")
        self.state = new_state

    def insert_coin(self, cents: int) -> None:
        if cents not in VALID_COINS:
            raise VendingError(f"unsupported coin: {cents} cents")
        if self.state in (State.DISPENSING, State.OUT_OF_STOCK):
            raise VendingError(f"cannot insert a coin while {self.state.name}")
        self.credit += cents
        self._transition(State.HAS_CREDIT)

    def select(self, name: str) -> dict:
        product = self.products.get(name)
        if product is None:
            raise UnknownProduct(f"no such product: {name!r}")
        if product.stock == 0:
            self._transition(State.OUT_OF_STOCK)
            self._transition(State.IDLE)
            raise OutOfStock(f"{product.name} is sold out")
        if self.credit < product.price:
            raise InsufficientCredit(
                f"{product.name} costs {product.price}, only {self.credit} inserted"
            )
        self._transition(State.DISPENSING)
        product.stock -= 1
        change = self.credit - product.price
        self.credit = 0
        self._transition(State.IDLE)
        return {"product": product.name, "change": change}

    def refund(self) -> int:
        amount = self.credit
        self.credit = 0
        self._transition(State.IDLE)
        return amount


def main() -> None:
    machine = VendingMachine(products={
        "cola": Product("cola", 150, 2),
        "chips": Product("chips", 100, 1),
        "water": Product("water", 75, 0),
    })

    for coin in (100, 50, 25):
        machine.insert_coin(coin)
    print(f"Credit after 100+50+25: {machine.credit}, state: {machine.state.name}")

    purchase = machine.select("cola")
    print(f"Bought cola -> {purchase}, credit now {machine.credit}, state {machine.state.name}")
    assert purchase == {"product": "cola", "change": 25}
    assert machine.products["cola"].stock == 1

    machine.insert_coin(100)
    assert machine.select("chips") == {"product": "chips", "change": 0}
    assert machine.products["chips"].stock == 0
    print("Bought chips with exact change; chips stock is now 0.")

    machine.insert_coin(100)
    try:
        machine.select("chips")
    except OutOfStock as exc:
        print(f"Sold out -> {type(exc).__name__}: {exc}")
    assert machine.credit == 100, "credit must survive a failed selection"
    assert machine.refund() == 100

    for bad_action, exception in (
        (lambda: machine.select("soda"), UnknownProduct),
        (lambda: machine.insert_coin(7), VendingError),
        (lambda: (machine.insert_coin(25), machine.select("cola")), InsufficientCredit),
    ):
        try:
            bad_action()
        except exception as exc:
            print(f"{exception.__name__}: {exc}")
    machine.refund()

    print("\nState transitions:")
    for step in machine.history:
        print(f"  {step}")
    assert machine.state is State.IDLE and machine.credit == 0


if __name__ == "__main__":
    main()
