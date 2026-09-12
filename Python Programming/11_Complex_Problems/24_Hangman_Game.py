"""
Program 24: Hangman Game

Classic word-guessing game. The player reveals a hidden word one letter
at a time and loses after a limited number of wrong guesses.

Concepts: sets, string building, loops, input validation.
"""

import random

WORDS = [
    "python", "pytest", "variable", "function", "iterator",
    "dictionary", "recursion", "algorithm", "exception", "module",
]

MAX_WRONG = 6


def mask_word(word: str, guessed: set[str]) -> str:
    """Return the word with unguessed letters replaced by underscores."""
    return " ".join(letter if letter in guessed else "_" for letter in word)


def get_guess(guessed: set[str]) -> str:
    """Ask for a single new letter, re-prompting until valid."""
    while True:
        guess = input("Guess a letter: ").strip().lower()
        if len(guess) != 1 or not guess.isalpha():
            print("Enter exactly one letter (a-z).")
        elif guess in guessed:
            print(f"You already tried '{guess}'. Pick another.")
        else:
            return guess


def main() -> None:
    word = random.choice(WORDS)
    guessed: set[str] = set()
    wrong = 0

    print("Hangman - guess the word!")
    while wrong < MAX_WRONG:
        print(f"\nWord:  {mask_word(word, guessed)}")
        print(f"Wrong: {wrong}/{MAX_WRONG}  Tried: {''.join(sorted(guessed)) or '-'}")

        guess = get_guess(guessed)
        guessed.add(guess)

        if guess in word:
            print(f"Good guess! '{guess}' is in the word.")
            if all(letter in guessed for letter in word):
                print(f"\nYou win! The word was '{word}'.")
                return
        else:
            wrong += 1
            print(f"Sorry, no '{guess}'.")

    print(f"\nOut of guesses. The word was '{word}'.")


if __name__ == "__main__":
    main()