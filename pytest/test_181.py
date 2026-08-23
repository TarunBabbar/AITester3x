# test_181.py
# Demonstrates custom markers (smoke / regression) with print() output.
#
#   pytest -m smoke           # run only smoke-marked tests
#   pytest -m regression      # run only regression-marked tests
#   pytest -v -s              # verbose + show print() output
#
# NOTE: print() output is captured by default and only shown on failure.
# Use pytest -s to see prints live.

import pytest


@pytest.mark.smoke
def test_method2():
    print("test1")
    # 1 - 1 == 0, so this assertion is FALSE -> test will FAIL
    assert 1 - 1 == 2


@pytest.mark.regression
def test_login():
    print("test2")
    # 1 + 1 == 2, so this assertion is TRUE -> test will PASS
    assert 1 + 1 == 2
