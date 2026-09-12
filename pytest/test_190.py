# test_190.py
# Demonstrates the capsys fixture: capturing stdout and stderr inside tests.
#
#   pytest test_190.py -v
#
# The capsys fixture replaces sys.stdout / sys.stderr for the duration of
# the test, so you can assert on what your code printed instead of eyeballing
# it. capsys.readouterr() returns an object with .out and .err strings.

import sys


def greet(name: str) -> None:
    print(f"Hello, {name}!")


def warn(message: str) -> None:
    print(f"WARNING: {message}", file=sys.stderr)


def test_greet_prints_expected(capsys):
    greet("Ada")
    captured = capsys.readouterr()
    assert captured.out == "Hello, Ada!\n"
    assert captured.err == ""


def test_warning_goes_to_stderr(capsys):
    warn("disk almost full")
    captured = capsys.readouterr()
    assert "disk almost full" in captured.err
    assert captured.out == ""


def test_multiline_output(capsys):
    for name in ("A", "B", "C"):
        greet(name)
    lines = capsys.readouterr().out.splitlines()
    assert lines == ["Hello, A!", "Hello, B!", "Hello, C!"]


def test_readouterr_resets_between_calls(capsys):
    greet("first")
    first = capsys.readouterr()
    greet("second")
    second = capsys.readouterr()
    # Each readouterr() only returns what happened since the last call.
    assert first.out == "Hello, first!\n"
    assert second.out == "Hello, second!\n"