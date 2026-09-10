# test_187.py
# Demonstrates pytest.approx for floating-point comparisons.
#
#   pytest test_187.py -v
#
# Exact equality fails for floats due to binary representation
# (0.1 + 0.2 != 0.3). approx is a relative/absolute tolerance wrapper.

import pytest


def test_float_exact_equality_is_dangerous():
    assert 0.1 + 0.2 != 0.3  # this is the trap, not a pass/fail check
    assert 0.1 + 0.2 == pytest.approx(0.3)


def test_approx_default_tolerance():
    value = 1.0 / 3.0
    assert value == pytest.approx(0.33333333)  # relative tolerance is plenty


def test_approx_abs_tolerance():
    # Around zero, relative tolerance collapses, so absolute tolerance is used.
    assert 1e-15 == pytest.approx(0.0, abs=1e-14)
    assert 1e-15 != pytest.approx(0.0, abs=1e-16)  # outside the window


def test_approx_on_collections():
    measurements = [0.1, 0.2, 0.3]
    expected = [0.1, 0.2, 0.3]
    # approx also works on lists, sets, dicts (element-wise).
    assert measurements == pytest.approx(expected)


def test_approx_rel_tolerance():
    # Allow a 1% relative difference around the expected value.
    assert 101.0 == pytest.approx(100.0, rel=0.01)   # 1% of 100 = 1.0 -> in range
    assert 98.0 == pytest.approx(100.0, rel=0.02)    # 2% tolerance
    assert 103.0 != pytest.approx(100.0, rel=0.01)   # outside the window