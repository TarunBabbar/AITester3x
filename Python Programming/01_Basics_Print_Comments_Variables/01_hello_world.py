# =========================================================
# 01 - BASICS: PRINT, COMMENTS, VARIABLES & USER INPUT
# Programs 1-10. Run each one, then tweak and re-run.
# =========================================================

# ---------- Program 1: Hello, World! ----------
print("Hello, World!")


# ---------- Program 2: Print Your Name ----------
name = "Tarun"
print("My name is", name)


# ---------- Program 3: Comments Demo ----------
# This is a single-line comment. Python ignores it.

"""
This is a multi-line comment (docstring).
You can write many lines here and Python ignores them too.
"""
print("Comments are ignored by Python - see the source code above.")


# ---------- Program 4: Variables Demo ----------
age = 25          # int
height = 5.9      # float
name = "Amit"     # str
is_student = True # bool

print(name, age, height, is_student)


# ---------- Program 5: Add Two Numbers ----------
num1 = 10
num2 = 20
total = num1 + num2
print("Sum of", num1, "and", num2, "is", total)


# ---------- Program 6: Swap Two Numbers ----------
a = 5
b = 10
print("Before swap: a =", a, ", b =", b)

a, b = b, a   # Python trick - swap in one line

print("After swap: a =", a, ", b =", b)


# ---------- Program 7: Area of a Rectangle ----------
length = 8
width = 4
area = length * width
print("Rectangle area:", area)


# ---------- Program 8: Find the Average of Three Numbers ----------
x = 10
y = 20
z = 30
average = (x + y + z) / 3
print("Average:", average)


# ---------- Program 9: Take User Input ----------
name = input("Enter your name: ")
print("Hello", name + "! Welcome to Python.")


# ---------- Program 10: Simple Calculator (input) ----------
num1 = float(input("Enter first number: "))
num2 = float(input("Enter second number: "))
print("Addition:", num1 + num2)
print("Subtraction:", num1 - num2)
print("Multiplication:", num1 * num2)
print("Division:", num1 / num2)
