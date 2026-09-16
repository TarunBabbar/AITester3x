# test_205.py
# Demonstrates that pytest also runs plain unittest.TestCase classes, so a suite
# can mix unittest-style tests (setUp/tearDown, self.assertEqual, subTest) with
# ordinary pytest functions in the same file.
#
#   pytest test_205.py -v
#
# pytest adds its own features on top: -k selection, fixtures and marks still
# work alongside the unittest class.

import unittest


def clamp(value: int, low: int, high: int) -> int:
    """Restrict value to the inclusive range [low, high]."""
    if low > high:
        raise ValueError("low must not exceed high")
    return max(low, min(value, high))


class TestClamp(unittest.TestCase):
    def setUp(self):
        # setUp runs before every test method in this class.
        self.low, self.high = 0, 10

    def tearDown(self):
        self.low = self.high = None

    def test_value_inside_range(self):
        self.assertEqual(clamp(5, self.low, self.high), 5)

    def test_value_below_range(self):
        self.assertEqual(clamp(-3, self.low, self.high), 0)

    def test_value_above_range(self):
        self.assertEqual(clamp(99, self.low, self.high), 10)

    def test_invalid_range_raises(self):
        with self.assertRaises(ValueError):
            clamp(5, 10, 0)

    def test_many_cases_with_subtest(self):
        # subTest keeps going after a failure and names the failing case.
        for value, expected in [(-1, 0), (0, 0), (7, 7), (10, 10), (11, 10)]:
            with self.subTest(value=value):
                self.assertEqual(clamp(value, 0, 10), expected)


def test_plain_pytest_function_in_same_file():
    # A normal pytest-style test living beside the unittest class.
    assert clamp(42, 0, 100) == 42
