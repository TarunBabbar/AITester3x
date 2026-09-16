# test_204.py
# Demonstrates STACKED @pytest.mark.parametrize decorators: stacking two or more
# runs the test once for every COMBINATION of their values (the Cartesian
# product), which is how you build a small test matrix in one function.
#
#   pytest test_204.py -v
#
# The decorator closest to the function is applied last. Use ids=[...] on each
# stack level to keep the generated test names readable.

import pytest


@pytest.mark.parametrize("x", [1, 2, 3], ids=["x1", "x2", "x3"])
@pytest.mark.parametrize("y", [10, 20], ids=["y10", "y20"])
def test_multiplication_commutes(x, y):
    # 3 x-values * 2 y-values = 6 test runs from a single function.
    assert x * y == y * x
    assert x * y in {10, 20, 30, 40, 60}


@pytest.mark.parametrize("number", [2, 4, 6], ids=["two", "four", "six"])
@pytest.mark.parametrize("divisor", [1, 2], ids=["by1", "by2"])
def test_even_numbers_divide_evenly(number, divisor):
    assert number % divisor == 0


@pytest.mark.parametrize("unit", ["celsius", "fahrenheit"])
@pytest.mark.parametrize("value", [0, 100])
def test_conversion_pair_is_reversible(value, unit):
    # A tiny matrix: 2 units x 2 values = 4 runs.
    converted = value * 9 / 5 + 32 if unit == "celsius" else (value - 32) * 5 / 9
    restored = (converted - 32) * 5 / 9 if unit == "celsius" else converted * 9 / 5 + 32
    assert restored == pytest.approx(value)
