# test_213.py
# Calling pytest.skip, pytest.xfail and pytest.fail from inside a test body,
# rather than using the skipif and xfail decorators.
#
#   pytest test_213.py -v
#
# A decorator decides before the test runs. The functions here decide while it
# is running, which is what you need when the reason only becomes known partway
# through - a feature probe, a missing permission, a file that turned out to be
# absent. Each one works by raising: pytest.skip raises Skipped, pytest.xfail
# raises XFailed and pytest.fail raises Failed, and pytest catches them at the
# top of the test. The last few tests in this file show the real thing, and they
# are reported as skipped and xfailed rather than as failures.

import sys

import pytest


# ---------------------------------------------------------------- the forms
def test_skip_raises_the_same_exception_pytest_uses():
    with pytest.raises(pytest.skip.Exception) as info:
        pytest.skip("a reason given at runtime")
    assert info.value.msg == "a reason given at runtime"


def test_xfail_raises_its_own_exception():
    with pytest.raises(pytest.xfail.Exception) as info:
        pytest.xfail("not implemented yet")
    assert info.value.msg == "not implemented yet"


def test_fail_raises_the_same_exception_a_failed_assertion_does():
    with pytest.raises(pytest.fail.Exception) as info:
        pytest.fail("something went wrong")
    assert "something went wrong" in str(info.value)


def test_skip_and_fail_are_separate_outcomes():
    # Neither is a kind of the other. They are siblings under one base class,
    # which is how pytest can tell them apart at the top of the test.
    assert not issubclass(pytest.skip.Exception, pytest.fail.Exception)
    assert not issubclass(pytest.fail.Exception, pytest.skip.Exception)
    # xfail, on the other hand, really is a kind of failure with a name of its own.
    assert issubclass(pytest.xfail.Exception, pytest.fail.Exception)


# ------------------------------------------------- skipping from inside
def test_a_skip_anywhere_in_the_call_stack_skips_the_test():
    steps: list[str] = []

    def helper() -> None:
        steps.append("checked the feature")
        pytest.skip("the feature is not available")
        steps.append("never reached")

    with pytest.raises(pytest.skip.Exception):
        helper()

    assert steps == ["checked the feature"], f"the skip propagated straight out: {steps}"


def test_the_condition_can_be_anything_discovered_at_runtime():
    # A decorator cannot look at something a previous line computed.
    available = sys.version_info >= (3, 8)
    if not available:
        pytest.skip(f"needs Python 3.8 or newer, this is {sys.version_info[:2]}")
    assert available is True


def test_a_unix_only_check_that_skips_on_windows():
    if sys.platform.startswith("win"):
        pytest.skip("this code path is for unix-like systems")
    # Deliberately nothing to assert: on Windows this test is skipped, elsewhere
    # it runs and passes. Either way the suite is green.
    assert sys.platform != ""


# --------------------------------------------- expected failures from inside
def test_xfail_marks_the_running_test_as_expected_to_fail():
    with pytest.raises(pytest.xfail.Exception):
        pytest.xfail("the parser cannot handle this shape yet")


def test_a_test_that_calls_xfail_is_reported_as_xfailed():
    pytest.xfail("the format is not supported yet")
    # Nothing can reach this line: pytest.xfail raises, and pytest turns that
    # into an xfailed result rather than a failure.


def test_xfail_can_be_decided_after_doing_some_work():
    results = {"parsed": 0, "rejected": 0}
    for text in ("1+1", "2*3", "not valid (("):
        if text.count("(") != text.count(")"):
            # Discovered partway through, which a decorator could not have known.
            assert results["parsed"] == 2, "the first two really did parse"
            pytest.xfail("unbalanced brackets are not handled")
        results["parsed"] += 1
    raise AssertionError("the third input should have stopped the loop")


# -------------------------------------------------------- the decorators
@pytest.mark.skip(reason="kept as an example of the decorator form")
def test_decorated_skip():
    raise AssertionError("a skipped test never runs")


@pytest.mark.skipif(sys.version_info < (3, 0), reason="needs Python 3")
def test_decorated_skipif_runs_here():
    # The condition is false on any supported interpreter, so this one runs.
    assert sys.version_info.major == 3


@pytest.mark.xfail(reason="a known bug, kept visible on purpose")
def test_decorated_xfail_that_does_fail():
    assert 1 == 2, "this really does fail, which is the point"


@pytest.mark.xfail(reason="it passes now, which pytest reports as xpass")
def test_decorated_xfail_that_passes():
    assert 1 == 1


@pytest.mark.xfail(sys.platform == "win32", reason="platform specific, expected to fail here")
def test_conditional_xfail():
    assert False, "expected, because this runs on Windows"


# ------------------------------------------------------------- how it looks
def test_the_skip_reason_is_carried_on_the_exception():
    # The reason is what appears as the SKIPPED line in the report, so it is
    # worth writing for whoever reads the output.
    try:
        pytest.skip("  a reason worth reading  ")
    except pytest.skip.Exception as skipped:
        assert "worth reading" in str(skipped)
        assert skipped.msg.strip() == "a reason worth reading"


def test_fail_can_carry_a_message_for_the_report():
    with pytest.raises(pytest.fail.Exception) as info:
        pytest.fail("expected 3 but got 4")
    assert "expected 3 but got 4" in str(info.value)
