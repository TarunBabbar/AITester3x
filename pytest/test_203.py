# test_203.py
# Demonstrates @pytest.mark.filterwarnings: control warnings per test - ignore a
# known noisy warning, or promote a category of warning into an error so it
# fails the test.
#
#   pytest test_203.py -v
#
# Filter syntax is "action:message:category:module:lineno", e.g.
#   @pytest.mark.filterwarnings("ignore:deprecated")
#   @pytest.mark.filterwarnings("error::UserWarning")

import warnings

import pytest


def noisy() -> str:
    warnings.warn("this endpoint is deprecated", DeprecationWarning, stacklevel=2)
    return "ok"


def suspicious() -> str:
    warnings.warn("something looks wrong", UserWarning, stacklevel=2)
    return "ok"


@pytest.mark.filterwarnings("ignore:this endpoint is deprecated")
def test_known_warning_is_ignored():
    # Without the mark this DeprecationWarning would still be reported; here it
    # is filtered out for this test only.
    assert noisy() == "ok"


@pytest.mark.filterwarnings("error::UserWarning")
def test_warning_promoted_to_error():
    # Any UserWarning raised in this test becomes a failure we can catch.
    with pytest.raises(UserWarning, match="looks wrong"):
        suspicious()


def test_warnings_are_recorded_normally(recwarn):
    # The recwarn fixture collects the warnings emitted inside the test.
    noisy()
    assert len(recwarn) == 1
    assert issubclass(recwarn[0].category, DeprecationWarning)
    assert "deprecated" in str(recwarn[0].message)
