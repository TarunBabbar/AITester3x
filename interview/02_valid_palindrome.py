"""
Valid Palindrome — Classic Interview Question

A phrase is a palindrome if, after converting all uppercase letters into
lowercase letters and removing all non-alphanumeric characters, it reads
the same forward and backward. Alphanumeric characters include letters
and numbers.

Example:
    Input:  "A man, a plan, a canal: Panama"
    Output: True

    Input:  "race a car"
    Output: False

Interview tips:
    - Two-pointer technique: start one pointer at each end, move inward.
    - This is O(n) time and O(1) space — better than building a filtered
      copy of the string.
    - isalnum() + lower() handle the clean-up for you.
"""


def is_palindrome(s: str) -> bool:
    """Return True if s is a valid palindrome (case-insensitive, ignore
    non-alphanumeric characters)."""
    left, right = 0, len(s) - 1

    while left < right:
        # Skip non-alphanumeric characters from both ends
        while left < right and not s[left].isalnum():
            left += 1
        while left < right and not s[right].isalnum():
            right -= 1

        if s[left].lower() != s[right].lower():
            return False

        left += 1
        right -= 1

    return True


if __name__ == "__main__":
    test_cases = [
        ("A man, a plan, a canal: Panama", True),
        ("race a car", False),
        (" ", True),
        ("0P", False),
        ("Never odd or even", True),
    ]

    for phrase, expected in test_cases:
        result = is_palindrome(phrase)
        print(f"is_palindrome({phrase!r}) -> {result} (expected {expected})")
        assert result == expected, f"Failed for {phrase!r}"

    print("All sanity checks passed.")
