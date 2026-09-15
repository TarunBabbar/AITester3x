# test_199.py
# Demonstrates the FIXTURE-FACTORY pattern: a fixture RETURNS a function that
# builds objects on demand, so one fixture serves many differently-shaped test
# cases instead of one fixture per case.
#
#   pytest test_199.py -v
#
# The factory can also keep a record of what it created, which tests inspect.

import pytest


class Order:
    def __init__(self, items: list[dict]) -> None:
        self.items = items

    def total(self) -> int:
        return sum(item["price"] * item["qty"] for item in self.items)

    def __len__(self) -> int:
        return sum(item["qty"] for item in self.items)


@pytest.fixture
def make_order():
    """Return a factory that builds Orders from (price, qty) pairs."""
    created: list[Order] = []

    def _make(*pairs: tuple[int, int]) -> Order:
        order = Order([{"price": price, "qty": qty} for price, qty in pairs])
        created.append(order)
        return order

    _make.created = created
    return _make


def test_empty_order(make_order):
    assert make_order().total() == 0


def test_single_line_item(make_order):
    order = make_order((100, 2))
    assert order.total() == 200
    assert len(order) == 2


def test_multi_line_item(make_order):
    order = make_order((100, 2), (25, 4))
    assert order.total() == 300
    assert len(order) == 6


def test_factory_records_created_orders(make_order):
    make_order((10, 1))
    make_order((20, 1), (5, 4))
    assert len(make_order.created) == 2
    assert [order.total() for order in make_order.created] == [10, 40]
