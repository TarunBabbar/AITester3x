# test_189.py
# Demonstrates fixtures with scope + tmp_path_factory for session-wide
# temporary directories shared across tests.
#
#   pytest test_189.py -v -s
#
# tmp_path:    unique temp dir PER TEST (fast, isolated).
# tmp_path_factory: create temp dirs with your own lifetime; here we make
#              one shared SOURCE_DIR for the whole session.
#
# A fixture's scope controls how often it runs: function, class, module,
# package, or session. Session scope = created once, reused everywhere.

import shutil
from pathlib import Path

import pytest


@pytest.fixture(scope="session")
def source_dir(tmp_path_factory) -> Path:
    """A shared directory every test can read; created once per session."""
    directory = tmp_path_factory.mktemp("shared_source")
    (directory / "data.txt").write_text("shared content")
    return directory


def test_read_shared_data(source_dir):
    assert (source_dir / "data.txt").read_text() == "shared content"


def test_list_shared_dir(source_dir):
    assert list(source_dir.iterdir()) == [source_dir / "data.txt"]


@pytest.fixture
def work_dir(tmp_path):
    """Per-test scratch space; each test gets a fresh, empty directory."""
    return tmp_path


def test_write_in_own_dir(work_dir):
    (work_dir / "note.txt").write_text("mine")
    assert (work_dir / "note.txt").exists()


def test_each_test_gets_own_work_dir(work_dir):
    # Fresh copy per test: the file written by the other test is absent here.
    assert not (work_dir / "note.txt").exists()


def test_copy_from_shared(source_dir, work_dir):
    shutil.copy(source_dir / "data.txt", work_dir / "copy.txt")
    assert (work_dir / "copy.txt").read_text() == "shared content"