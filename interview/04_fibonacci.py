"""
Fibonacci Sequence — Classic Interview Question

Return the n-th Fibonacci number, where:
    fib(0) = 0
    fib(1) = 1
    fib(n) = fib(n - 1) + fib(n - 2)

Example:
    fib(10) -> 55

Interview tips:
    - Naive recursion is O(2^n) — the classic trap.
    - Memoization (cache) turns it into O(n).
    - Iterative version is O(n) time, O(1) space — best for a follow-up.
    - Bonus answer if asked: Binet's formula gives O(log n) via matrix
      exponentiation, but iterative is what interviewers usually want.
"""

from functools import lru_cache


@lru_cache(maxsize=None)
def fib_memo(n: int) -> int:
    """Return the n-th Fibonacci number using cached recursion."""
    if n < 2:
        return n
    return fib_memo(n - 1) + fib_memo(n - 2)


def fib_iterative(n: int) -> int:
    """Return the n-th Fibonacci number iteratively (O(1) space)."""
    if n < 2:
        return n

    prev, curr = 0, 1
    for _ in range(2, n + 1):
        prev, curr = curr, prev + curr
    return curr


if __name__ == "__main__":
    for i in range(11):
        print(f"fib({i}) = {fib_memo(i)}")

    # Both implementations must agree on the same sequence
    for i in range(30):
        assert fib_memo(i) == fib_iterative(i) == fib_iterative(i) == fib_memo(i)

    assert fib_memo(10) == 55
    assert fib_memo(20) == 6765
    print("All sanity checks passed.")
