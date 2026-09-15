# test_201.py
# Demonstrates pytest.importorskip: skip a test cleanly when an optional
# dependency is not installed, instead of failing with an ImportError.
#
#   pytest test_201.py -v
#
# Called at module level it skips the whole file; called inside a test it skips
# just that test. A reason can be supplied for the report.

import pytest

# Always available, so this module-level call simply returns the module.
json_module = pytest.importorskip("json", reason="the standard library is missing?")


def test_json_round_trip():
    payload = {"a": 1, "b": [1, 2, 3], "c": {"nested": True}}
    assert json_module.loads(json_module.dumps(payload)) == payload


def test_optional_dependency_present():
    # collections is always present, so this runs normally.
    counter_module = pytest.importorskip("collections")
    assert counter_module.Counter("aabbb")["b"] == 3


def test_optional_dependency_missing_is_skipped():
    # This package does not exist, so importorskip raises Skipped here and the
    # rest of the test never runs - it is reported as SKIPPED, not FAILED.
    pytest.importorskip("a_package_that_is_definitely_not_installed",
                        reason="optional extra not installed")
    assert False, "unreachable: the import above skipped this test"
