"""
Program 7: Rock-Paper-Scissors Game

Play best-of-N rounds against the computer. Tracks scores, handles ties,
and validates input.

Concepts: random, dictionaries, loops, input validation.
"""

import random

CHOICES = {"r": "rock", "p": "paper", "s": "scissors"}

# key beats value
BEATS = {"rock": "scissors", "paper": "rock", "scissors": "paper"}


def round_winner(player: str, computer: str) -> str:
    """Return 'player', 'computer' or 'tie'."""
    if player == computer:
        return "tie"
    return "player" if BEATS[player] == computer else "computer"


def main() -> None:
    print("=== Rock-Paper-Scissors ===")
    print("Best of 3 rounds. Press enter to quit.\n")

    player_score = 0
    computer_score = 0

    while player_score < 2 and computer_score < 2:
        pick = input("Your move (r/p/s): ").strip().lower()
        if not pick:
            break
        if pick not in CHOICES:
            print("Enter r (rock), p (paper) or s (scissors).")
            continue

        computer = random.choice(list(CHOICES.values()))
        player = CHOICES[pick]
        print(f"Computer: {computer}")

        result = round_winner(player, computer)
        if result == "player":
            player_score += 1
            print(f"You win this round! ({player} beats {computer})")
        elif result == "computer":
            computer_score += 1
            print(f"Computer wins this round. ({computer} beats {player})")
        else:
            print("Tie!")

        print(f"Score: You {player_score} - {computer_score} Computer\n")

    if player_score > computer_score:
        print("You win the match!")
    elif computer_score > player_score:
        print("Computer wins the match.")
    else:
        print("Match unfinished - thanks for playing!")


if __name__ == "__main__":
    main()
