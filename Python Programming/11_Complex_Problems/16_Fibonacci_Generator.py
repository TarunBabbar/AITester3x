"""
Program 16: Fibonacci Sequence Generator

Generates the first N Fibonacci numbers three different ways: iterative,
recursive (with memoization), and a generator. Prints each approach and
compares timings.

Concepts: recursion, memoization, generators, timeit.
"""

import time
from functools import lru_cache


def fib_iterative(n: int) -> list[int]:
    """First n Fibonacci numbers, iteratively."""
    if n <= 0:
        return []
    sequence = [0, 1]
    while len(sequence) < n:
        sequence.append(sequence[-1] + sequence[-2])
    return sequence[:n]


@lru_cache(maxsize=None)
def fib_recursive(k: int) -> int:
    """The k-th Fibonacci number, recursively with memoization."""
    if k < 2:
        return k
    return fib_recursive(k - 1) + fib_recursive(k - 2)


def fib_generator(n: int):
    """Yield the first n Fibonacci numbers one at a time."""
    a, b = 0, 1
    for _ in range(n):
        yield a
        a, b = b, a + b


def main() -> None:
    try:
        n = int(input("How many Fibonacci numbers? ").strip() or "10")
    except (EOFError, KeyboardInterrupt, ValueError):
        n = 10

    if n < 1:
        print("Enter a positive number.")
        return

    print(f"\nFirst {n} Fibonacci numbers:")
    print("Iterative:", fib_iterative(n))

    rec_start = time.perf_counter()
    recursive = [fib_recursive(k) for k in range(n)]
    rec_time = time.perf_counter() - rec_start
    print(f"Recursive: {recursive}  ({rec_time:.4f}s with memoization)")

    gen_start = time.perf_counter()
    generated = list(fib_generator(n))
    gen_time = time.perf_counter() - gen_start
    print(f"Generator: {generated}  ({gen_time:.4f}s)")

    assert fib_iterative(n) == recursive == generated
    print("\nAll three approaches agree. Golden ratio check:")
    if n > 2:
        print(f"  F(n)/F(n-1) ~ {fib_recursive(n - 1) / fib_recursive(n - 2):.6f} (phi ~ 1.618034)")


if __name__ == "__main__":
    main()
