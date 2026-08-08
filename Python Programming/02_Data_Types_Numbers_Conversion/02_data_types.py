# =========================================================
# 02 - DATA TYPES: NUMBERS & TYPE CONVERSION
# Programs 11-20: int, float, complex, bool, type() & casting.
# =========================================================

# ---------- Program 11: Show Python Built-in Data Types ----------
print(type(10))            # <class 'int'>
print(type(3.14))          # <class 'float'>
print(type("Python"))      # <class 'str'>
print(type(True))          # <class 'bool'>
print(type([1, 2, 3]))     # <class 'list'>


# ---------- Program 12: Integer Operations ----------
a = 17
b = 5
print("Addition:", a + b)       # 22
print("Subtraction:", a - b)    # 12
print("Multiplication:", a * b) # 85
print("Division:", a / b)       # 3.4  (float division)
print("Floor Division:", a // b) # 3   (integer division)
print("Modulus:", a % b)        # 2    (remainder)
print("Power:", a ** b)         # 1419857


# ---------- Program 13: Float Precision Demo ----------
x = 0.1
y = 0.2
print("0.1 + 0.2 =", x + y)  # Notice floating point rounding


# ---------- Program 14: Complex Numbers ----------
z1 = 3 + 4j
z2 = 1 - 2j
print("Real part:", z1.real)
print("Imaginary part:", z1.imag)
print("Sum:", z1 + z2)
print("Product:", z1 * z2)


# ---------- Program 15: Boolean Logic ----------
is_raining = True
has_umbrella = False
print("Will you get wet?", is_raining and not has_umbrella)
print("Should you stay in?", is_raining or has_umbrella)
print("Not raining?", not is_raining)


# ---------- Program 16: Type Conversion (Casting) ----------
num_str = "123"
print("String to int:", int(num_str))
print("Int to float:", float(10))
print("Number to string:", str(42) + " is a string")
print("Float to int (truncates):", int(9.99))
print("Int to complex:", complex(5))


# ---------- Program 17: Find Maximum and Minimum ----------
print("Maximum of 3, 7, 5:", max(3, 7, 5))
print("Minimum of 3, 7, 5:", min(3, 7, 5))
print("Absolute value:", abs(-42))
print("Rounded value:", round(3.14159, 2))


# ---------- Program 18: Average Marks (mixed types) ----------
marks = [85, 92, 78, 88]
total = sum(marks)
average = total / len(marks)
print("Total marks:", total)
print("Average marks:", average)
print("Average as int:", int(average))


# ---------- Program 19: Simple Interest ----------
principal = 10000
rate = 5.5          # percent per year
time = 3            # years
interest = (principal * rate * time) / 100
print("Simple Interest:", interest)
print("Total Amount:", principal + interest)


# ---------- Program 20: Check Odd or Even (user input) ----------
number = int(input("Enter a number: "))
if number % 2 == 0:
    print(number, "is even")
else:
    print(number, "is odd")
