# test_196.py
# Demonstrates pytest.param(): give an individual parametrized case its own id
# and its own marks, so one row can be xfail or skipped while the rest of the
# rows run normally - all from a single test function.
#
#   pytest test_196.py -v
#
# The id appears in the test name instead of param0/param1, which makes
# failures much easier to read.

import pytest


def safe_divide(a: float, b: float) -> float:
    if b == 0:
        raise ZeroDivisionError("cannot divide by zero")
    return a / b


@pytest.mark.parametrize(
    "a, b, expected",
    [
        pytest.param(10, 2, 5.0, id="even-split"),
        pytest.param(9, 3, 3.0, id="whole-number"),
        pytest.param(5, 4, 1.25, id="terminating-decimal"),
        pytest.param(
            1, 0, None,
            marks=pytest.mark.xfail(raises=ZeroDivisionError, reason="division by zero"),
        ),
        pytest.param(
            7, 2, 3.5,
            marks=pytest.mark.skip(reason="not ready yet"),
        ),
    ],
)
def test_safe_divide(a, b, expected):
    # Runs for every row whose marks allow it; the xfail and skip rows are
    # reported as XFAIL and SKIPPED rather than failures.
    assert safe_divide(a, b) == expected


@pytest.mark.parametrize(
    "value, is_positive",
    [
        pytest.param(5, True, id="positive", marks=pytest.mark.smoke),
        pytest.param(-5, False, id="negative", marks=[pytest.mark.reg]),
    ],
)
def test_marks_stack_on_cases(value, is_positive):
    # Marks on pytest.param can be selected with -m, e.g. pytest -m smoke.
    assert (value > 0) is is_positive
