# =========================================================
# 10 - ADVANCED TOPICS: FILES, EXCEPTIONS, COMPREHENSIONS,
#     MODULES & MORE. Programs 91-100.
# =========================================================

# ---------- Program 91: List Comprehension ----------
squares = [x * x for x in range(1, 6)]
print("Squares:", squares)

evens = [x for x in range(1, 21) if x % 2 == 0]
print("Evens 1-20:", evens)


# ---------- Program 92: Dict and Set Comprehension ----------
squared = {x: x * x for x in range(1, 6)}
print("Dict of squares:", squared)

unique_chars = {ch for ch in "programming"}
print("Unique characters:", unique_chars)


# ---------- Program 93: try / except (exception handling) ----------
try:
    num = int(input("Enter a number: "))
    print("10 /", num, "=", 10 / num)
except ZeroDivisionError:
    print("Cannot divide by zero!")
except ValueError:
    print("That's not a valid number!")
finally:
    print("This always runs")


# ---------- Program 94: Custom Exception ----------
class AgeTooLowError(Exception):
    pass

try:
    age = int(input("Enter your age: "))
    if age < 18:
        raise AgeTooLowError("You must be 18+")
    print("Welcome!")
except AgeTooLowError as e:
    print("Error:", e)


# ---------- Program 95: Write to a File ----------
with open("output.txt", "w") as file:
    file.write("Line 1: Hello Python\n")
    file.write("Line 2: Learning files today\n")
    file.write("Line 3: Almost done!\n")
print("File 'output.txt' written successfully")


# ---------- Program 96: Read from a File ----------
with open("output.txt", "r") as file:
    content = file.read()
print("--- File content ---")
print(content)
print("---------------------")


# ---------- Program 97: Read File Line by Line ----------
with open("output.txt", "r") as file:
    print("Reading line by line:")
    for line in file:
        print(">>>", line.strip())


# ---------- Program 98: Modules - Random ----------
import random

print("Random number 1-100:", random.randint(1, 100))
print("Random float:", random.random())
print("Random choice:", random.choice(["apple", "banana", "cherry"]))
deck = list(range(1, 11))
random.shuffle(deck)
print("Shuffled numbers:", deck)


# ---------- Program 99: Modules - Math and Datetime ----------
import math
from datetime import datetime

print("Square root of 144:", math.sqrt(144))
print("Pi:", round(math.pi, 4))
print("2 raised to 10:", math.pow(2, 10))

now = datetime.now()
print("Current date & time:", now)
print("Today only:", now.strftime("%A, %d %B %Y"))
print("Time only:", now.strftime("%H:%M:%S"))


# ---------- Program 100: Mini Project - Number Guessing Game ----------
import random

secret = random.randint(1, 20)
attempts = 0
print("I'm thinking of a number between 1 and 20. Can you guess it?")

while True:
    guess = int(input("Your guess: "))
    attempts += 1
    if guess < secret:
        print("Too low!")
    elif guess > secret:
        print("Too high!")
    else:
        print(f"Correct! The number was {secret}")
        print(f"You got it in {attempts} attempts")
        break

print("Congratulations - you completed all 100 Python programs!")
