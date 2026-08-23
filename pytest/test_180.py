# test_180.py
# Demonstrates pytest custom markers: @pytest.mark.reg and @pytest.mark.smoke
#
# Markers let you tag tests so you can run selected groups:
#   pytest -m reg          # run only the "reg" marked tests
#   pytest -m smoke        # run only the "smoke" marked tests
#   pytest -m "not reg"    # run everything EXCEPT "reg"

import pytest


# @pytest.mark.reg is a CUSTOM marker (short for "regression").
# Custom markers should be registered in pytest.ini to avoid warnings:
#   [pytest]
#   markers =
#       reg: regression tests
#       smoke: quick sanity checks
@pytest.mark.reg
def test_anwser1():
    # Plain Python assert - pytest rewrites it to show actual values on failure
    assert 3 == 3  # this will PASS (3 is equal to 3)


@pytest.mark.smoke
def test_anwser2():
    assert 3 == 3  # this will PASS
