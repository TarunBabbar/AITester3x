"""
Program 6: Number Guessing Game

Guess a random number between 1 and 100 in as few tries as possible.
Tracks best score across rounds and supports difficulty levels.

Concepts: random, loops, input validation, score tracking.
"""

import random


def play_round(level: str) -> int:
    """Play one round; return the number of guesses taken."""
    if level == "easy":
        upper, max_guesses = 50, 10
    elif level == "hard":
        upper, max_guesses = 200, 6
    else:
        upper, max_guesses = 100, 8

    target = random.randint(1, upper)
    print(f"\nI picked a number between 1 and {upper}. You have {max_guesses} guesses.")

    for attempt in range(1, max_guesses + 1):
        try:
            guess = int(input(f"Guess #{attempt}: "))
        except (ValueError, EOFError):
            print("Please enter a whole number.")
            continue
        if guess < 1 or guess > upper:
            print(f"Stay between 1 and {upper}.")
            continue
        if guess == target:
            print(f"Correct! The number was {target}. You got it in {attempt} tries.")
            return attempt
        print("Higher." if guess < target else "Lower.")

    print(f"Out of guesses. The number was {target}.")
    return max_guesses + 1


def main() -> None:
    print("=== Number Guessing Game ===")
    best = None

    while True:
        try:
            level = input("\nDifficulty (easy/normal/hard, enter to quit): ").strip().lower()
        except (EOFError, KeyboardInterrupt):
            print()
            break
        if not level:
            break
        if level not in ("easy", "normal", "hard"):
            print("Pick easy, normal or hard.")
            continue

        tries = play_round(level)
        if best is None or tries < best:
            best = tries
            print(f"New best score: {best} tries!")

        try:
            again = input("Play again? (y/n): ").strip().lower()
        except (EOFError, KeyboardInterrupt):
            print()
            break
        if again != "y":
            break

    print(f"\nThanks for playing! Best score: {best if best else 'n/a'}")


if __name__ == "__main__":
    main()
