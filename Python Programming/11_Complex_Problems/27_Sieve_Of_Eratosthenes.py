"""
Program 27: Sieve of Eratosthenes & Prime Factorization

Finds every prime up to N with the sieve, checks single numbers for primality
with trial division, and breaks a number into its prime factors.

Concepts: lists, loops, math.isqrt, algorithm efficiency, functions.
"""

import math


def sieve(limit: int) -> list[int]:
    """Return all primes <= limit using the Sieve of Eratosthenes."""
    if limit < 2:
        return []
    is_prime = [True] * (limit + 1)
    is_prime[0] = is_prime[1] = False
    for number in range(2, math.isqrt(limit) + 1):
        if is_prime[number]:
            for multiple in range(number * number, limit + 1, number):
                is_prime[multiple] = False
    return [n for n, prime in enumerate(is_prime) if prime]


def is_prime(n: int) -> bool:
    """Trial-division primality check (6k +/- 1 optimization)."""
    if n < 2:
        return False
    if n in (2, 3):
        return True
    if n % 2 == 0 or n % 3 == 0:
        return False
    divisor = 5
    while divisor * divisor <= n:
        if n % divisor == 0 or n % (divisor + 2) == 0:
            return False
        divisor += 6
    return True


def prime_factors(n: int) -> list[int]:
    """Return the prime factors of n in ascending order."""
    if n < 2:
        return []
    factors: list[int] = []
    while n % 2 == 0:
        factors.append(2)
        n //= 2
    divisor = 3
    while divisor * divisor <= n:
        while n % divisor == 0:
            factors.append(divisor)
            n //= divisor
        divisor += 2
    if n > 1:
        factors.append(n)
    return factors


def main() -> None:
    try:
        limit = int(input("Find all primes up to: ").strip() or "50")
    except (EOFError, KeyboardInterrupt, ValueError):
        limit = 50

    primes = sieve(limit)
    print(f"\n{len(primes)} primes up to {limit}:")
    print(primes)

    # Every prime found by the sieve must agree with the trial-division check.
    assert all(is_prime(p) for p in primes)
    assert primes == [n for n in range(limit + 1) if is_prime(n)]

    if primes:
        sample = primes[-1]
        print(f"\nFactorization of {sample}: {prime_factors(sample)}")
    for value in (360, 97, 1001):
        print(f"Factorization of {value}: {prime_factors(value)}")


if __name__ == "__main__":
    main()
