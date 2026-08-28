"""
Two Sum — Classic Interview Question

Given an array of integers nums and an integer target, return the indices of
the two numbers that add up to target. Each input has exactly one solution,
and you may not use the same element twice.

Example:
    Input:  nums = [2, 7, 11, 15], target = 9
    Output: [0, 1]   (because nums[0] + nums[1] == 2 + 7 == 9)

Interview tips:
    - The brute force (double loop) is O(n^2). Interviewers want O(n).
    - Use a hash map (dict) to remember seen values -> O(n) time, O(n) space.
    - Watch out: do NOT reuse the same index (the "seen" check must come
      before adding the current element).
"""

from typing import List


def two_sum(nums: List[int], target: int) -> List[int]:
    """Return indices of the two numbers that add up to target."""
    seen: dict = {}  # value -> index

    for i, num in enumerate(nums):
        complement = target - num
        if complement in seen:
            return [seen[complement], i]
        seen[num] = i

    # Problem guarantees a solution exists; this is just for safety.
    raise ValueError("No two numbers sum to target")


if __name__ == "__main__":
    # Example from above
    nums = [2, 7, 11, 15]
    target = 9
    result = two_sum(nums, target)
    print(f"nums={nums}, target={target} -> indices {result}")

    # A couple more quick sanity checks
    assert two_sum([3, 2, 4], 6) == [1, 2]
    assert two_sum([3, 3], 6) == [0, 1]
    assert two_sum([-1, -2, -3, -4, -5], -8) == [2, 4]
    print("All sanity checks passed.")
