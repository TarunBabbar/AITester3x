"""
Program 11: Quiz Game

Multiple-choice quiz with scoring and instant feedback. Question set is a
simple list of dicts, easy to extend with your own questions.

Concepts: lists of dicts, loops, input validation, score tracking.
"""

QUESTIONS = [
    {
        "question": "What is the correct file extension for Python files?",
        "options": [".pyth", ".pt", ".py", ".p"],
        "answer": 2,
    },
    {
        "question": "Which keyword is used to define a function in Python?",
        "options": ["func", "def", "function", "define"],
        "answer": 1,
    },
    {
        "question": "Which data type is used to store True or False values?",
        "options": ["int", "str", "bool", "float"],
        "answer": 2,
    },
    {
        "question": "How do you create a list in Python?",
        "options": ["list = (1, 2, 3)", "list = [1, 2, 3]", "list = {1, 2, 3}", "list = <1, 2, 3>"],
        "answer": 1,
    },
    {
        "question": "Which method is used to remove the last item from a list?",
        "options": ["delete()", "remove()", "pop()", "drop()"],
        "answer": 2,
    },
]


def run_quiz() -> int:
    """Ask every question; return the number of correct answers."""
    score = 0

    for i, item in enumerate(QUESTIONS, 1):
        print(f"\nQ{i}. {item['question']}")
        for letter, option in zip("abcd", item["options"]):
            print(f"  {letter}) {option}")

        while True:
            try:
                choice = input("Your answer (a-d): ").strip().lower()
            except (EOFError, KeyboardInterrupt):
                print()
                return score
            if choice in ("a", "b", "c", "d"):
                break
            print("Enter a, b, c or d.")

        picked = "abcd".index(choice)
        if picked == item["answer"]:
            score += 1
            print("Correct!")
        else:
            correct = item["options"][item["answer"]]
            print(f"Wrong. The answer was: {correct}")

    return score


def main() -> None:
    print("=== Python Quiz ===")
    print(f"{len(QUESTIONS)} questions. Good luck!")

    score = run_quiz()

    print(f"\nFinal score: {score}/{len(QUESTIONS)}")
    pct = score / len(QUESTIONS) * 100
    if pct == 100:
        print("Perfect score - excellent!")
    elif pct >= 60:
        print("Good job!")
    else:
        print("Keep practicing!")


if __name__ == "__main__":
    main()
