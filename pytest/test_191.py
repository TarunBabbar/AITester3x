# test_191.py
# Demonstrates pytest.warns (warning assertions) and monkeypatch.setattr
# (patching functions/attributes for one test).
#
#   pytest test_191.py -v
#
# pytest.warns asserts a warning of a given category was raised.
# monkeypatch.setattr swaps an object's attribute and restores it after the
# test, so the real implementation is never permanently changed.

import warnings

import pytest


def deprecated_add(a: int, b: int) -> int:
    warnings.warn("deprecated_add is deprecated, use + instead",
                  DeprecationWarning, stacklevel=2)
    return a + b


def test_deprecated_add_warns_and_works():
    with pytest.warns(DeprecationWarning, match="deprecated"):
        result = deprecated_add(2, 3)
    assert result == 5


def test_wrong_warning_category_fails():
    # If the warning category or pattern does not match, pytest.warns fails.
    with pytest.warns(UserWarning, match="something else"):
        warnings.warn("a different problem", UserWarning)


# ---- monkeypatch.setattr ----------------------------------------------------

class Bank:
    def balance(self) -> float:
        return 1000.0  # pretend this hits a real account service


def test_real_balance():
    assert Bank().balance() == 1000.0


def test_patched_balance(monkeypatch):
    # Replace the method ONLY for this test; restored automatically after.
    monkeypatch.setattr(Bank, "balance", lambda self: 42.0)
    assert Bank().balance() == 42.0


def test_patch_does_not_leak():
    # Because monkeypatch undoes its changes, the real value is back here.
    assert Bank().balance() == 1000.0