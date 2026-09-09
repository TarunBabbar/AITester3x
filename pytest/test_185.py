# test_185.py
# Demonstrates class-based tests + setup_class/teardown_class hook methods.
#
#   pytest test_185.py -v
#
# Test classes group related tests; each method is run independently.
# setup_class runs once before the class's tests, teardown_class after.

import pytest

COUNTER_START = 100


class TestCounter:
    @classmethod
    def setup_class(cls):
        # Runs ONCE for the whole class (class-level setup).
        cls.counters = {"total": COUNTER_START}

    @classmethod
    def teardown_class(cls):
        # Runs once after all tests in the class finish.
        cls.counters.clear()

    def test_increment(self):
        total = self.counters["total"] + 1
        assert total == COUNTER_START + 1

    def test_initial_value(self):
        # setup_class guarantees 'total' exists before any test runs.
        assert self.counters["total"] == COUNTER_START

    def test_two_increments(self):
        total = self.counters["total"] + 2
        assert total == COUNTER_START + 2