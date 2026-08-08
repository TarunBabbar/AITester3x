# =========================================================
# 11 - ALL PYTHON KEYWORDS (W3Schools reference list)
# Programs 1-37 - one simple program per keyword.
# Source: https://www.w3schools.com/python/python_ref_keywords.asp
# Note: 'match' and 'case' need Python 3.10+. This file needs 3.10+.
# =========================================================

# ---------- Program 1: and (logical operator) ----------
age = 25
has_id = True
if age >= 18 and has_id:
    print("and: Entry allowed")


# ---------- Program 2: as (create an alias) ----------
import datetime as dt
print("as:", dt.date.today())


# ---------- Program 3: assert (debugging check) ----------
x = 10
assert x > 5, "x should be greater than 5"
print("assert: passed, x > 5 is True")


# ---------- Program 4: async (define async function) ----------
import asyncio

async def say_hi():
    print("async: inside async function")
    await asyncio.sleep(0.1)

asyncio.run(say_hi())


# ---------- Program 5: await (wait for an awaitable) ----------
# (shown with async above - simple version here)
async def fetch_value():
    await asyncio.sleep(0.1)
    return 42

async def show_await():
    value = await fetch_value()
    print("await: got value", value)

asyncio.run(show_await())


# ---------- Program 6: break (break out of a loop) ----------
for i in range(1, 10):
    if i == 4:
        break
print("break: stopped at", i)


# ---------- Program 7: case (pattern in a match statement) ----------
def describe(value):
    match value:
        case 0:
            return "zero"
        case 1 | 2:
            return "small number"
        case int(n) if n > 10:
            return "big number"
        case _:
            return "something else"

print("case:", describe(0), "|", describe(2), "|", describe(50), "|", describe("hi"))


# ---------- Program 8: class (define a class) ----------
class Dog:
    def __init__(self, name):
        self.name = name

    def bark(self):
        print(f"class: {self.name} says woof!")

Dog("Buddy").bark()


# ---------- Program 9: continue (skip to next iteration) ----------
print("continue: odds below 6:")
for i in range(1, 7):
    if i % 2 == 0:
        continue
    print("  ", i)


# ---------- Program 10: def (define a function) ----------
def greet(name):
    return "Hello, " + name

print("def:", greet("Python"))


# ---------- Program 11: del (delete an object) ----------
fruits = ["apple", "banana", "cherry"]
del fruits[1]
print("del: removed item ->", fruits)

num = 5
del num


# ---------- Program 12: elif (else if) ----------
score = 75
if score >= 90:
    grade = "A"
elif score >= 70:
    grade = "B"
elif score >= 50:
    grade = "C"
else:
    grade = "F"
print("elif: grade is", grade)


# ---------- Program 13: else (conditional fallback) ----------
number = 7
if number % 2 == 0:
    print("else: even")
else:
    print("else: odd")


# ---------- Program 14: except (handle an exception) ----------
try:
    result = 10 / 0
except ZeroDivisionError:
    print("except: caught division by zero")


# ---------- Program 15: False (boolean value) ----------
is_raining = False
print("False:", is_raining, "| not False =", not is_raining)


# ---------- Program 16: finally (always runs) ----------
try:
    result = 10 / 2
except ZeroDivisionError:
    print("can't divide by zero")
finally:
    print("finally: this runs no matter what")


# ---------- Program 17: for (loop) ----------
print("for:", end=" ")
for fruit in ["apple", "banana", "cherry"]:
    print(fruit, end=" ")
print()


# ---------- Program 18: from (import specific parts) ----------
from math import sqrt, pi
print("from: sqrt(144) =", sqrt(144), "| pi =", round(pi, 2))


# ---------- Program 19: global (declare a global variable) ----------
counter = 0

def increment():
    global counter
    counter += 1

increment()
increment()
print("global: counter =", counter)


# ---------- Program 20: if (conditional statement) ----------
age = 20
if age >= 18:
    print("if: you are an adult")


# ---------- Program 21: import (import a module) ----------
import random
print("import:", random.randint(1, 10))


# ---------- Program 22: in (membership check) ----------
colors = ["red", "green", "blue"]
print("in:", "green" in colors, "| 'yellow' in colors?", "yellow" in colors)


# ---------- Program 23: is (identity check) ----------
a = [1, 2, 3]
b = a          # b refers to the SAME list
c = [1, 2, 3]  # c is a NEW list
print("is: a is b ->", a is b, "| a is c ->", a is c, "| a == c ->", a == c)


# ---------- Program 24: lambda (anonymous function) ----------
square = lambda n: n * n
print("lambda:", square(7))


# ---------- Program 25: match (compare value against cases) ----------
def day_type(day):
    match day.lower():
        case "saturday" | "sunday":
            return "weekend"
        case _:
            return "weekday"

print("match:", day_type("Sunday"), "|", day_type("Monday"))


# ---------- Program 26: None (null value) ----------
result = None
print("None:", result, "| is None?", result is None)


# ---------- Program 27: nonlocal (declare a non-local variable) ----------
def outer():
    value = 10

    def inner():
        nonlocal value
        value += 5

    inner()
    return value

print("nonlocal:", outer())


# ---------- Program 28: not (logical operator) ----------
is_student = False
print("not:", not is_student)


# ---------- Program 29: or (logical operator) ----------
age = 16
parent_approval = True
if age >= 18 or parent_approval:
    print("or: allowed to attend")


# ---------- Program 30: pass (do nothing) ----------
def future_function():
    pass   # placeholder - no error

print("pass: future_function defined, does nothing")


# ---------- Program 31: raise (raise an exception) ----------
def check_age(age):
    if age < 0:
        raise ValueError("age cannot be negative")

try:
    check_age(-5)
except ValueError as e:
    print("raise:", e)


# ---------- Program 32: return (exit function with a value) ----------
def multiply(a, b):
    return a * b

print("return:", multiply(6, 7))


# ---------- Program 33: True (boolean value) ----------
is_sunny = True
print("True:", is_sunny)


# ---------- Program 34: try (make a try...except) ----------
try:
    text = "abc"
    number = int(text)
except ValueError:
    print("try/except: could not convert 'abc' to int")


# ---------- Program 35: while (loop) ----------
count = 1
print("while:", end=" ")
while count <= 5:
    print(count, end=" ")
    count += 1
print()


# ---------- Program 36: with (simplify exception handling) ----------
from io import StringIO

buffer = StringIO()
with buffer as file:
    file.write("Hello with keyword")
    print("with:", file.getvalue())


# ---------- Program 37: yield (return values from a generator) ----------
def countdown(n):
    while n > 0:
        yield n
        n -= 1

print("yield:", list(countdown(5)))
