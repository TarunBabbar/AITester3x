# test_197.py
# Demonstrates monkeypatch for process-wide state: setenv, delenv, chdir and
# syspath_prepend. Every change is undone automatically after the test, so the
# real environment is never left modified.
#
#   pytest test_197.py -v
#
# This is the tool for "make the code think it is running somewhere else" -
# a different environment, working directory or import path.

import os
from pathlib import Path

import pytest


def read_mode() -> str:
    """Read the app mode from the environment, falling back to a default."""
    return os.environ.get("APP_MODE", "default")


def test_setenv_changes_the_variable(monkeypatch):
    monkeypatch.setenv("APP_MODE", "production")
    assert read_mode() == "production"


def test_delenv_removes_the_variable(monkeypatch):
    monkeypatch.setenv("APP_MODE", "staging")
    assert read_mode() == "staging"
    monkeypatch.delenv("APP_MODE")
    assert read_mode() == "default"


def test_changes_do_not_leak(monkeypatch):
    # Undo everything this test did, then prove the original value came back.
    original = os.environ.get("APP_MODE")
    monkeypatch.setenv("APP_MODE", "changed")
    assert read_mode() == "changed"
    monkeypatch.undo()
    assert os.environ.get("APP_MODE") == original


@pytest.fixture
def temp_config(tmp_path, monkeypatch):
    """Run inside a temp folder with a config file and APP_MODE set."""
    monkeypatch.setenv("APP_MODE", "test")
    monkeypatch.chdir(tmp_path)
    path = tmp_path / "config.txt"
    path.write_text("mode=test\n")
    return path


def test_chdir_moves_the_working_directory(temp_config):
    assert Path("config.txt").exists()          # relative path resolves in tmp_path
    assert temp_config.read_text() == "mode=test\n"
    assert read_mode() == "test"


def test_syspath_prepend_adds_an_import_location(monkeypatch, tmp_path):
    (tmp_path / "myhelper.py").write_text("VALUE = 42\n")
    monkeypatch.syspath_prepend(str(tmp_path))
    import myhelper
    assert myhelper.VALUE == 42
