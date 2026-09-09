# test_183.py
# Demonstrates @pytest.mark.parametrize: run the same test with many inputs.
#
#   pytest test_183.py -v   # notice each parameter set becomes its own test case
#
# The decorator takes the names of the arguments and a list of value-tuples.
# One test function turns into one test per tuple, and a failure reports
# exactly which input combination broke.

import pytest


@pytest.mark.parametrize("a,b,expected", [(1, 2, 3), (0, 0, 0), (-5, 5, 0)])
def test_add(a, b, expected):
    assert a + b == expected


@pytest.mark.parametrize("text", ["hello", "", "racecar", "12321"])
def test_palindrome(text):
    assert text == text[::-1]


@pytest.mark.parametrize("word", ["python", "pytest"])
@pytest.mark.parametrize("number", [1, 2, 3])
def test_combined(word, number):
    # Nested parametrize runs the cartesian product: 2 x 3 = 6 test cases.
    assert f"{word}{number}" == word + str(number)


@pytest.mark.parametrize("value", [0, 1, -1])
def test_division_raises_on_zero(value):
    # Parametrize also works with pytest.raises to check error cases.
    with pytest.raises(ZeroDivisionError):
        value / 0


@pytest.mark.parametrize("email", ["ada@example.com", "bob@example.org"])
def test_email_format(email):
    assert "@" in email
    assert email.endswith(".com") or email.endswith(".org")