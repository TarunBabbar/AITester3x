"""
Program 18: Matrix Operations Calculator

Reads two matrices (or one, for transpose/determinant) and performs
addition, subtraction, multiplication, transposition, or determinant.

Concepts: nested lists, list comprehensions, input validation, 2D math.
"""


def read_matrix(name: str) -> list[list[int]]:
    """Prompt the user for a matrix and return it as a list of rows."""
    while True:
        try:
            rows = int(input(f"Rows in {name}: ").strip())
            cols = int(input(f"Columns in {name}: ").strip())
        except ValueError:
            print("Row/column counts must be whole numbers.")
            continue
        if rows < 1 or cols < 1:
            print("Both dimensions must be at least 1.")
            continue

        matrix: list[list[int]] = []
        print(f"Enter {name} values, {cols} numbers per row, separated by spaces:")
        for r in range(rows):
            while True:
                raw = input(f"  Row {r + 1}: ").strip().split()
                if len(raw) != cols:
                    print(f"Expected {cols} numbers, got {len(raw)}. Try again.")
                    continue
                try:
                    matrix.append([int(item) for item in raw])
                    break
                except ValueError:
                    print("All values must be integers. Try again.")
        return matrix


def add_subtract(a: list[list[int]], b: list[list[int]], subtract: bool) -> list[list[int]]:
    """Element-wise addition (subtract=False) or subtraction."""
    return [
        [x - y if subtract else x + y for x, y in zip(row_a, row_b)]
        for row_a, row_b in zip(a, b)
    ]


def multiply(a: list[list[int]], b: list[list[int]]) -> list[list[int]]:
    """Standard matrix product of a (m x k) and b (k x n)."""
    k = len(b)
    n = len(b[0])
    result = [[0] * n for _ in range(len(a))]
    for i in range(len(a)):
        for j in range(n):
            result[i][j] = sum(a[i][t] * b[t][j] for t in range(k))
    return result


def transpose(a: list[list[int]]) -> list[list[int]]:
    """Swap rows and columns."""
    return [list(row) for row in zip(*a)]


def determinant(a: list[list[int]]) -> int:
    """Determinant of a square matrix (generic recursive expansion)."""
    n = len(a)
    if n == 1:
        return a[0][0]
    if n == 2:
        return a[0][0] * a[1][1] - a[0][1] * a[1][0]

    det = 0
    for col in range(n):
        minor = [row[:col] + row[col + 1 :] for row in a[1:]]
        sign = 1 if col % 2 == 0 else -1
        det += sign * a[0][col] * determinant(minor)
    return det


def show_matrix(name: str, matrix: list[list[int]]) -> None:
    print(f"{name}:")
    for row in matrix:
        print("  " + "  ".join(f"{value:>5}" for value in row))


def main() -> None:
    print("Matrix Operations Calculator")
    print("Commands: add | subtract | multiply | transpose | determinant\n")

    command = input("Operation? ").strip().lower()
    if command not in ("add", "subtract", "multiply", "transpose", "determinant"):
        print(f"Unknown operation {command!r}.")
        return

    if command == "transpose":
        matrix = read_matrix("the matrix")
        show_matrix("Original", matrix)
        show_matrix("Transposed", transpose(matrix))
        return

    if command == "determinant":
        size = input("Matrix size (1-4): ").strip()
        if not size.isdigit() or not 1 <= int(size) <= 4:
            print("Determinant supported for sizes 1 through 4.")
            return
        matrix = read_matrix("the matrix")
        while len(matrix) != len(matrix[0]):
            print("Determinant needs a square matrix.")
            matrix = read_matrix("the matrix")
        show_matrix("Matrix", matrix)
        print(f"Determinant: {determinant(matrix)}")
        return

    a = read_matrix("matrix A")
    b = read_matrix("matrix B")

    if command == "add" or command == "subtract":
        if len(a) != len(b) or len(a[0]) != len(b[0]):
            print("Add/subtract require both matrices to have the same shape.")
            return
        result = add_subtract(a, b, subtract=(command == "subtract"))
    else:  # multiply
        if len(a[0]) != len(b):
            print("Multiply requires columns(A) == rows(B).")
            return
        result = multiply(a, b)

    show_matrix("Matrix A", a)
    show_matrix("Matrix B", b)
    show_matrix("Result", result)


if __name__ == "__main__":
    main()
