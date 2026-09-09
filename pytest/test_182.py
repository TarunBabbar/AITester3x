# test_182.py
# Demonstrates pytest fixtures: shared setup/teardown run automatically.
#
#   pytest test_182.py -v -s    # -s shows the fixture's print() output
#
# A fixture is a function decorated with @pytest.fixture that returns
# something tests can request by declaring it as a parameter. This keeps
# setup code in one place instead of repeating it in every test.

import pytest


@pytest.fixture
def numbers():
    """Provides a fresh list for each test (a new fixture instance per test)."""
    print("\n[fixture] setting up numbers...")
    data = [3, 1, 2]
    yield data  # <-- everything before yield is setup, after is teardown
    print("[fixture] tearing down numbers...")
    # Even if the test fails, teardown still runs.


def test_sum(numbers):
    assert sum(numbers) == 6


def test_sorted(numbers):
    numbers.sort()
    assert numbers == [1, 2, 3]
    # Each test gets its OWN copy, so the sort above does not leak anywhere.


@pytest.fixture
def person():
    return {"name": "Ada", "age": 36}


def test_person_fields(person):
    assert person["name"] == "Ada"
    assert person["age"] == 36