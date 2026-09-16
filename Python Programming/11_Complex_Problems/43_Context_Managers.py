"""
Program 43: Context Managers & contextlib

Builds a context manager two ways - a class implementing __enter__/__exit__ and
a generator decorated with @contextmanager - then uses contextlib helpers
(suppress and ExitStack) to manage resources cleanly and safely.

Concepts: the with statement, __enter__/__exit__, exception propagation,
contextlib.contextmanager, contextlib.suppress, contextlib.ExitStack.
"""

import contextlib
import tempfile
import time
from pathlib import Path


class Timer:
    """Context manager that records how long its block took."""

    def __enter__(self) -> "Timer":
        self.start = time.perf_counter()
        return self

    def __exit__(self, exc_type, exc_value, traceback) -> bool:
        self.elapsed = time.perf_counter() - self.start
        return False  # never swallow exceptions


class Transaction:
    """Commits when the block succeeds, rolls back when it raises."""

    def __init__(self, name: str) -> None:
        self.name = name
        self.log: list[str] = []
        self.committed = False

    def __enter__(self) -> "Transaction":
        self.log.append(f"begin {self.name}")
        return self

    def __exit__(self, exc_type, exc_value, traceback) -> bool:
        if exc_type is None:
            self.committed = True
            self.log.append(f"commit {self.name}")
        else:
            self.log.append(f"rollback {self.name}: {exc_value}")
        return False  # let any exception keep travelling


@contextlib.contextmanager
def opened_report(path: Path):
    """Generator-based context manager that always closes the file."""
    handle = path.open("w", encoding="utf-8")
    try:
        yield handle
    finally:
        handle.close()


def main() -> None:
    with Timer() as timer:
        total = sum(number * number for number in range(100_000))
    print(f"Timer block took {timer.elapsed:.4f}s (total={total})")
    assert timer.elapsed >= 0 and total == 333_328_333_350_000

    with Transaction("transfer") as success:
        success.log.append("debit 100")
    print(f"\nSuccessful transaction : committed={success.committed}")
    print(f"  log: {success.log}")
    assert success.committed and success.log[-1] == "commit transfer"

    try:
        with Transaction("transfer") as failed:
            raise ValueError("insufficient funds")
    except ValueError as error:
        print(f"\nFailed transaction     : propagates {type(error).__name__}: {error}")
    print(f"  committed={failed.committed}")
    print(f"  log: {failed.log}")
    assert not failed.committed
    assert any("rollback" in line and "insufficient funds" in line for line in failed.log)

    with tempfile.TemporaryDirectory() as folder:
        report_path = Path(folder) / "report.txt"
        with opened_report(report_path) as handle:
            handle.write("line one\nline two\n")
        assert handle.closed, "the generator context manager closed the file"
        print(f"\nReport written and closed: {report_path.read_text(encoding='utf-8')!r}")

        first = Path(folder) / "a.txt"
        second = Path(folder) / "b.txt"
        with contextlib.ExitStack() as stack:
            handles = [
                stack.enter_context(path.open("w", encoding="utf-8"))
                for path in (first, second)
            ]
            for index, open_handle in enumerate(handles, start=1):
                open_handle.write(f"file {index}\n")
        assert all(item.closed for item in handles), "ExitStack closes everything on exit"
        assert first.read_text(encoding="utf-8") == "file 1\n"
        print(f"ExitStack managed {len(handles)} files and closed them all.")

    with contextlib.suppress(ZeroDivisionError):
        _ = 1 / 0
    print("\nsuppress() ignored the ZeroDivisionError and execution continued.")
    print("All context manager checks passed.")


if __name__ == "__main__":
    main()
