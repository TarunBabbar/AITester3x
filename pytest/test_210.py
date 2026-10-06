# test_210.py
# Demonstrates monkeypatch.context(): a set of patches that is undone at the end
# of a with block rather than at the end of the test, plus monkeypatch.undo().
#
#   pytest test_210.py -v
#
# The plain monkeypatch object undoes everything after the test. A context gives
# you a smaller scope, which is what you want when only part of a test needs the
# patched behaviour.

import os

import pytest

CONFIG = {"timeout": 30, "retries": 3, "verbose": False}

# Whatever the real environment holds, so the assertions do not assume it is unset.
ORIGINAL_DEMO_MODE = os.environ.get("DEMO_MODE")


def test_context_restores_at_the_end_of_the_block(monkeypatch):
    monkeypatch.setitem(CONFIG, "timeout", 60)
    assert CONFIG["timeout"] == 60

    with monkeypatch.context() as scoped:
        scoped.setitem(CONFIG, "timeout", 99)
        scoped.setitem(CONFIG, "retries", 9)
        assert CONFIG == {"timeout": 99, "retries": 9, "verbose": False}

    # The inner patch is gone; the outer one is still in force.
    assert CONFIG == {"timeout": 60, "retries": 3, "verbose": False}


def test_nested_contexts_unwind_inside_out(monkeypatch):
    with monkeypatch.context() as outer:
        outer.setitem(CONFIG, "timeout", 1)
        with monkeypatch.context() as inner:
            inner.setitem(CONFIG, "timeout", 2)
            assert CONFIG["timeout"] == 2
        assert CONFIG["timeout"] == 1, "the inner value was undone first"
    assert CONFIG["timeout"] == 30, "and then the outer one"


class Settings:
    timeout = 30


def test_a_context_can_patch_attributes_and_environment(monkeypatch):
    original = os.environ.get("DEMO_MODE")
    original_timeout = Settings.timeout

    with monkeypatch.context() as scoped:
        scoped.setenv("DEMO_MODE", "scoped")
        scoped.setattr(Settings, "timeout", 7)      # a real attribute, not a dict key
        assert os.environ["DEMO_MODE"] == "scoped"
        assert Settings.timeout == 7

    assert os.environ.get("DEMO_MODE") == original, "the environment was restored"
    assert Settings.timeout == original_timeout, "and so was the attribute"


def test_undo_restores_everything_at_once(monkeypatch):
    monkeypatch.setitem(CONFIG, "timeout", 5)
    monkeypatch.setitem(CONFIG, "verbose", True)
    original = os.environ.get("DEMO_MODE")
    monkeypatch.setenv("DEMO_MODE", "on")

    assert CONFIG["timeout"] == 5 and CONFIG["verbose"] is True
    assert os.environ["DEMO_MODE"] == "on"

    monkeypatch.undo()

    assert CONFIG["timeout"] == 30, "the first patch was undone"
    assert CONFIG["verbose"] is False, "and the second"
    assert os.environ.get("DEMO_MODE") == original, "along with the environment"


def test_the_test_itself_is_not_leaking(monkeypatch):
    # Every other test's patches are gone by the time this one runs.
    assert CONFIG == {"timeout": 30, "retries": 3, "verbose": False}
    assert os.environ.get("DEMO_MODE") == ORIGINAL_DEMO_MODE


def test_a_stale_context_object_does_nothing_after_the_block(monkeypatch):
    with monkeypatch.context() as scoped:
        scoped.setitem(CONFIG, "timeout", 1)
    # The context is finished, so its undo no longer has anything to reverse.
    scoped.undo()
    assert CONFIG["timeout"] == 30, "undoing twice is harmless"


def test_context_is_a_fresh_scope(monkeypatch):
    monkeypatch.setitem(CONFIG, "timeout", 11)
    with monkeypatch.context() as scoped:
        # The scoped context starts from the state at this point, so it sees 11.
        assert CONFIG["timeout"] == 11
        scoped.setitem(CONFIG, "timeout", 22)
        assert CONFIG["timeout"] == 22
    assert CONFIG["timeout"] == 11, "back to the outer value"
