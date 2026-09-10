# test_188.py
# Demonstrates pytest.raises for asserting exceptions, including message
# matching and inspecting the raised exception object.
#
#   pytest test_188.py -v

import pytest


def divide(a: float, b: float) -> float:
    if b == 0:
        raise ValueError("division by zero is not allowed")
    return a / b


def get_user(user_id: int) -> str:
    if user_id <= 0:
        raise KeyError(f"no user with id {user_id}")
    return f"user-{user_id}"


def test_divide_raises_value_error():
    with pytest.raises(ValueError):
        divide(10, 0)


def test_divide_raises_exception_type():
    # The outer class covers subclasses too (ZeroDivisionError is a ValueError).
    with pytest.raises(ValueError):
        divide(10, 0)


def test_raises_matches_message():
    # match checks the message with a regex search.
    with pytest.raises(ValueError, match="division by zero"):
        divide(10, 0)
    with pytest.raises(ValueError, match="not allowed"):
        divide(10, 0)


def test_raises_returns_exception_object():
    with pytest.raises(KeyError) as excinfo:
        get_user(-5)
    assert excinfo.value.args[0] == "no user with id -5"


def test_no_exception_fails_raises():
    # If the code does NOT raise, pytest.raises fails the test itself.
    with pytest.raises(ValueError):
        divide(10, 2)  # would fail: no ValueError raised


def test_raises_multiple_exceptions():
    with pytest.raises((ValueError, TypeError)):
        divide(10, "oops")  # TypeError from dividing int by str