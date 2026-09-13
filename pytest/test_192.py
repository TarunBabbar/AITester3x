# test_192.py
# Demonstrates autouse fixtures: setup/teardown that runs automatically for
# every test in this file WITHOUT being named in the test's arguments.
#
#   pytest test_192.py -v
#
# @pytest.fixture(autouse=True) runs before each test. If the fixture uses
# `yield`, the code after the yield runs as teardown when the test finishes.
# Handy for shared state such as counters, temp files or clocks.

import pytest


# ---- a counter that is reset before every single test -----------------------

@pytest.fixture(autouse=True)
def reset_usage_counter():
    """Reset the shared counter automatically; no test has to ask for it."""
    Service.calls = 0
    yield
    # Teardown runs after each test, even if the test fails.
    Service.last_count = Service.calls


class Service:
    calls = 0
    last_count = 0

    @classmethod
    def do_work(cls) -> str:
        cls.calls += 1
        return f"work #{cls.calls}"


def test_first_call_starts_at_one():
    # The autouse fixture already reset the counter, so this is always "work #1".
    assert Service.do_work() == "work #1"


def test_second_test_also_starts_at_one():
    assert Service.do_work() == "work #1"


def test_counter_can_reach_two_within_one_test():
    Service.do_work()
    assert Service.do_work() == "work #2"


def test_teardown_recorded_the_count():
    # last_count was written by the PREVIOUS test's teardown (which called
    # do_work twice), proving the code after `yield` ran automatically.
    assert Service.last_count == 2


# ---- autouse fixture that prepares a temp clock -----------------------------

@pytest.fixture(autouse=True)
def freeze_time():
    """Pretend the clock always reads the same value for every test."""
    Clock.now = 1_700_000_000.0
    yield
    Clock.now = None


class Clock:
    now = None


def test_autouse_applied_without_asking():
    assert Clock.now == 1_700_000_000.0
