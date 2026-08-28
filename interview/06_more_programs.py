"""
More Interview Programs — Quick Favorites

A bundled set of short, frequently-asked interview problems:

    1. reverse_string(s)   — reverse a string (no s[::-1] shortcut)
    2. factorial(n)        — iterative factorial
    3. is_prime(n)         — primality check with early exit
    4. binary_search(...)  — binary search over a sorted list

Each function is self-contained, documented, and sanity-checked below so
this file can be run directly:  python 06_more_programs.py
"""

from typing import List, Optional


# ---------------------------------------------------------------------------
# 1. Reverse a string
# ---------------------------------------------------------------------------
def reverse_string(s: str) -> str:
    """Return s reversed, using a two-pointer swap on a character list."""
    chars = list(s)
    left, right = 0, len(chars) - 1

    while left < right:
        chars[left], chars[right] = chars[right], chars[left]
        left += 1
        right -= 1

    return "".join(chars)


# ---------------------------------------------------------------------------
# 2. Factorial
# ---------------------------------------------------------------------------
def factorial(n: int) -> int:
    """Return n! for n >= 0. Raises ValueError for negative n."""
    if n < 0:
        raise ValueError("factorial is undefined for negative numbers")

    result = 1
    for i in range(2, n + 1):
        result *= i
    return result


# ---------------------------------------------------------------------------
# 3. Prime check
# ---------------------------------------------------------------------------
def is_prime(n: int) -> bool:
    """Return True if n is prime. Handles 0, 1, negatives, and evens."""
    if n < 2:
        return False
    if n == 2:
        return True
    if n % 2 == 0:
        return False

    # Only check odd divisors up to sqrt(n)
    divisor = 3
    while divisor * divisor <= n:
        if n % divisor == 0:
            return False
        divisor += 2
    return True


# ---------------------------------------------------------------------------
# 4. Binary search
# ---------------------------------------------------------------------------
def binary_search(nums: List[int], target: int) -> Optional[int]:
    """Return the index of target in a sorted list, or None if absent."""
    low, high = 0, len(nums) - 1

    while low <= high:
        mid = (low + high) // 2
        if nums[mid] == target:
            return mid
        if nums[mid] < target:
            low = mid + 1
        else:
            high = mid - 1

    return None


if __name__ == "__main__":
    # 1. reverse_string
    assert reverse_string("hello") == "olleh"
    assert reverse_string("racecar") == "racecar"
    assert reverse_string("a") == "a"
    assert reverse_string("") == ""
    print("reverse_string: OK")

    # 2. factorial
    assert factorial(0) == 1
    assert factorial(1) == 1
    assert factorial(5) == 120
    assert factorial(10) == 3628800
    print("factorial: OK")

    # 3. is_prime
    primes = [2, 3, 5, 7, 11, 13, 97]
    non_primes = [0, 1, 4, 6, 9, 15, 100]
    assert all(is_prime(p) for p in primes)
    assert all(not is_prime(np) for np in non_primes)
    print("is_prime: OK")

    # 4. binary_search
    nums = [1, 3, 5, 7, 9, 11, 13]
    assert binary_search(nums, 7) == 3
    assert binary_search(nums, 1) == 0
    assert binary_search(nums, 13) == 6
    assert binary_search(nums, 4) is None
    assert binary_search([], 4) is None
    print("binary_search: OK")

    print("\nAll programs passed their sanity checks.")
