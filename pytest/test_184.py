# test_184.py
# Demonstrates monkeypatch + tmp_path: fake external calls and test real files.
#
#   pytest test_184.py -v
#
# monkeypatch replaces attributes/functions for the DURATION of one test and
# restores them afterwards. tmp_path is a unique temp directory per test.

import os
from pathlib import Path

import pytest

# The code under test: reads a config value from the environment.
def get_debug_mode():
    return os.environ.get("APP_DEBUG", "false") == "true"


# A tiny task tracker that persists to a JSON file.
class TaskStore:
    def __init__(self, path: Path):
        self.path = path

    def add(self, name: str) -> None:
        tasks = self.load()
        tasks.append(name)
        self.path.write_text(str(tasks))

    def load(self) -> list[str]:
        if not self.path.exists():
            return []
        return eval(self.path.read_text())  # keep it simple for the demo


def test_debug_on(monkeypatch):
    monkeypatch.setenv("APP_DEBUG", "true")
    assert get_debug_mode() is True


def test_debug_off(monkeypatch):
    monkeypatch.setenv("APP_DEBUG", "false")
    assert get_debug_mode() is False


def test_debug_unset(monkeypatch):
    monkeypatch.delenv("APP_DEBUG", raising=False)
    assert get_debug_mode() is False


def test_taskstore_roundtrip(tmp_path):
    store = TaskStore(tmp_path / "tasks.json")
    store.add("code review")
    store.add("write tests")
    assert store.load() == ["code review", "write tests"]


def test_taskstore_no_file(tmp_path):
    store = TaskStore(tmp_path / "missing.json")
    assert store.load() == []