# test_186.py
# Demonstrates skip and xfail markers: skipping unwanted tests and
# expecting known failures.
#
#   pytest test_186.py -v -rsx
#
# -r option: -r shows a summary report at the end (s=skipped, x=xfailed).
# skip    : test is NOT run at all.
# skipif  : skip only when a condition is true (platform check, version...).
# xfail   : test is RUN, but a failure is expected and is not reported as
#           a normal failure.

import sys

import pytest

PYTHON_12 = sys.version_info >= (3, 12)


@pytest.mark.skip(reason="Feature not implemented yet")
def test_not_implemented():
    assert False  # never runs


@pytest.mark.skipif(sys.platform != "win32", reason="Windows-only behavior")
def test_windows_only():
    assert sys.platform == "win32"


@pytest.mark.skipif(PYTHON_12, reason="Only relevant on Python < 3.12")
def test_old_python_behavior():
    assert not PYTHON_12


@pytest.mark.xfail(reason="Known bug #123, awaiting fix", strict=False)
def test_known_bug():
    assert 1 + 1 == 3  # fails as expected -> XFAIL not FAILED


@pytest.mark.xfail(reason="Ordering is not guaranteed")
def test_flaky_order():
    # This one actually passes, producing XPASS.
    assert sorted([3, 1, 2]) == [1, 2, 3]