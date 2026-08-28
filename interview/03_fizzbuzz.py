"""
FizzBuzz — The Most Famous Screening Question

Print numbers from 1 to n, but:
    - multiples of 3 -> "Fizz"
    - multiples of 5 -> "Buzz"
    - multiples of both 3 and 5 -> "FizzBuzz"
    - otherwise -> the number itself

Example:
    fizzbuzz(15) prints:
    1, 2, Fizz, 4, Buzz, Fizz, 7, 8, Fizz, Buzz, 11, Fizz, 13, 14, FizzBuzz

Interview tips:
    - Check the "both" case FIRST, otherwise 15 would print "Fizz".
    - Many candidates solve this with if/elif/else chains or a string
      builder. Both are fine; the string builder scales better when the
      interviewer adds rules (e.g. multiples of 7 -> "Bazz").
    - Returns a list so the function is easy to unit test.
"""

from typing import List


def fizzbuzz(n: int) -> List[str]:
    """Return a FizzBuzz sequence for numbers 1..n."""
    result: List[str] = []

    for i in range(1, n + 1):
        out = ""
        if i % 3 == 0:
            out += "Fizz"
        if i % 5 == 0:
            out += "Buzz"
        # If neither rule matched, keep the number itself
        result.append(out if out else str(i))

    return result


if __name__ == "__main__":
    n = 15
    sequence = fizzbuzz(n)
    print(f"fizzbuzz({n}) ->")
    print(", ".join(sequence))

    # Sanity checks against the classic 1..15 sequence
    expected = [
        "1", "2", "Fizz", "4", "Buzz", "Fizz", "7", "8", "Fizz",
        "Buzz", "11", "Fizz", "13", "14", "FizzBuzz",
    ]
    assert sequence == expected, "FizzBuzz sequence mismatch"
    assert fizzbuzz(1) == ["1"]
    assert fizzbuzz(0) == []
    print("All sanity checks passed.")
