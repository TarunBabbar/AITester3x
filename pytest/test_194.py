# test_194.py
# Demonstrates doctest: tests written as interactive examples INSIDE docstrings.
# pytest can collect them, and a normal test can run them too.
#
#   pytest test_194.py -v                       # runs test_doctests_in_this_module
#   pytest test_194.py -v --doctest-modules     # collects the doctests directly
#
# Doctests are real assertions: the output after >>> must match exactly.

import doctest
import sys


def add(a: int, b: int) -> int:
    """Return the sum of a and b.

    >>> add(2, 3)
    5
    >>> add(-1, 1)
    0
    >>> add(0, 0)
    0
    """
    return a + b


def shout(text: str) -> str:
    """Upper-case the text and add an exclamation mark.

    >>> shout("hi")
    'HI!'
    >>> shout("")
    '!'
    """
    return text.upper() + "!"


def average(numbers: list[float]) -> float:
    """Return the mean of numbers, rounded to 2 decimal places.

    >>> average([1, 2, 3])
    2.0
    >>> average([1.5, 2.5])
    2.0
    """
    return round(sum(numbers) / len(numbers), 2)


def test_doctests_in_this_module():
    """Run every doctest in this file as an ordinary pytest test."""
    module = sys.modules[__name__]
    results = doctest.testmod(module, verbose=False)
    assert results.failed == 0
    assert results.attempted >= 7


def test_normal_assertion_still_works():
    assert add(10, 5) == 15
    assert shout("pytest") == "PYTEST!"
