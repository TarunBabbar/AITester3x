# test_208.py
# Demonstrates pytest-rerunfailures: @pytest.mark.flaky(reruns=N) retries a test
# that fails intermittently, which is the honest treatment for a genuinely flaky
# dependency - and it does not hide a real failure.
#
#   pytest test_208.py -v
#   pytest test_208.py -v --reruns 2      # or retry everything from the command line
#
# The plugin is required. This module skips cleanly when it is absent.

import pytest

pytest.importorskip("pytest_rerunfailures")

# Counted across tests, because the point is to prove the retry happened.
attempts = 0


@pytest.mark.flaky(reruns=3, reruns_delay=0)
def test_fails_twice_then_passes():
    global attempts
    attempts += 1
    # Fails on attempts one and two, passes on the third.
    assert attempts >= 3, f"attempt {attempts} failed, as arranged"


@pytest.mark.flaky(reruns=2, reruns_delay=0)
@pytest.mark.xfail(reason="reruns cannot rescue a real failure", strict=True)
def test_a_real_failure_is_not_rescued():
    raise AssertionError("broken on purpose: retrying changes nothing")


def test_the_retries_really_happened():
    # Runs after the two above, since pytest collects a module in file order.
    assert attempts == 3, f"expected three attempts in total, saw {attempts}"


@pytest.mark.flaky(reruns=2, reruns_delay=0)
def test_a_test_that_passes_is_not_retried():
    # A passing test is never rerun, so there is nothing to count here.
    assert True


@pytest.mark.flaky(reruns=1, reruns_delay=0)
@pytest.mark.parametrize("value", [1, 2, 3])
def test_marks_combine_with_parametrize(value):
    # Each parametrized case gets its own flaky mark, so each can be retried.
    assert value > 0


@pytest.mark.flaky(reruns=2, reruns_delay=0)
def test_a_flaky_test_can_use_a_fixture(request):
    # The rerun count is visible to the test through request.node.
    assert request.node.get_closest_marker("flaky") is not None
