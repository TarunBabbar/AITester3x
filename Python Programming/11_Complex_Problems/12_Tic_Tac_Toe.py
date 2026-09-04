"""
Program 12: Tic-Tac-Toe

Two-player tic-tac-toe on the command line. Players take turns entering
board positions 1-9; the first to line up three marks wins.

Concepts: lists, functions, input validation, win-condition logic.
"""

BOARD = list(range(1, 10))  # shared mutable board

WIN_LINES = [
    (0, 1, 2), (3, 4, 5), (6, 7, 8),   # rows
    (0, 3, 6), (1, 4, 7), (2, 5, 8),   # columns
    (0, 4, 8), (2, 4, 6),              # diagonals
]


def show_board() -> None:
    print()
    for row in range(3):
        cells = [str(BOARD[row * 3 + col]) for col in range(3)]
        print(" " + " | ".join(cells))
        if row < 2:
            print("---+---+---")
    print()


def winner() -> str | None:
    """Return 'X', 'O' or None if no winner yet."""
    for a, b, c in WIN_LINES:
        if BOARD[a] == BOARD[b] == BOARD[c]:
            return str(BOARD[a])
    return None


def board_full() -> bool:
    return all(isinstance(cell, str) for cell in BOARD)


def play() -> None:
    print("=== Tic-Tac-Toe ===")
    print("Positions are 1-9, left to right, top to bottom.\n")
    show_board()

    current = "X"
    while True:
        try:
            raw = input(f"Player {current}, pick a position: ").strip()
        except (EOFError, KeyboardInterrupt):
            print("\nGame abandoned.")
            return
        if not raw.isdigit():
            print("Enter a number 1-9.")
            continue

        pos = int(raw)
        if pos < 1 or pos > 9 or isinstance(BOARD[pos - 1], str):
            print("That spot is taken or invalid.")
            continue

        BOARD[pos - 1] = current
        show_board()

        w = winner()
        if w:
            print(f"Player {w} wins!")
            return
        if board_full():
            print("It's a draw!")
            return
        current = "O" if current == "X" else "X"


if __name__ == "__main__":
    play()
