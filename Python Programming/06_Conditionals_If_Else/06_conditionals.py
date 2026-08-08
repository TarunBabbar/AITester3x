# =========================================================
# 06 - CONDITIONALS: if / elif / else
# Programs 51-60: decision making in Python.
# =========================================================

# ---------- Program 51: Simple if Statement ----------
age = 18
if age >= 18:
    print("You are an adult")
print("Program continues...")


# ---------- Program 52: if / else ----------
number = 7
if number % 2 == 0:
    print(number, "is even")
else:
    print(number, "is odd")


# ---------- Program 53: if / elif / else Chain ----------
marks = 78
if marks >= 90:
    grade = "A"
elif marks >= 75:
    grade = "B"
elif marks >= 60:
    grade = "C"
elif marks >= 40:
    grade = "D"
else:
    grade = "F"
print("Grade:", grade)


# ---------- Program 54: Nested Conditions ----------
age = 25
has_id = True
if age >= 18:
    if has_id:
        print("Entry allowed - adult with ID")
    else:
        print("Entry denied - need ID")
else:
    print("Entry denied - under 18")


# ---------- Program 55: Multiple Conditions with and / or ----------
username = "admin"
password = "secret123"
if username == "admin" and password == "secret123":
    print("Login successful")
elif username != "admin":
    print("Unknown username")
else:
    print("Wrong password")


# ---------- Program 56: Ternary Operator (one-line if) ----------
age = 20
status = "Adult" if age >= 18 else "Minor"
print("Status:", status)


# ---------- Program 57: Membership Test with in ----------
allowed_users = ["Amit", "Riya", "John"]
user = "Riya"
if user in allowed_users:
    print(user, "is allowed")
else:
    print(user, "is NOT allowed")


# ---------- Program 58: Check Leap Year ----------
year = int(input("Enter a year: "))
if (year % 4 == 0 and year % 100 != 0) or year % 400 == 0:
    print(year, "is a leap year")
else:
    print(year, "is not a leap year")


# ---------- Program 59: Find Largest of Three Numbers ----------
a = int(input("Enter first number: "))
b = int(input("Enter second number: "))
c = int(input("Enter third number: "))

if a >= b and a >= c:
    largest = a
elif b >= a and b >= c:
    largest = b
else:
    largest = c
print("Largest number:", largest)


# ---------- Program 60: Simple ATM Menu ----------
balance = 5000
choice = input("Enter option (1=Check, 2=Deposit, 3=Withdraw): ")

if choice == "1":
    print("Your balance is:", balance)
elif choice == "2":
    amount = float(input("Deposit amount: "))
    balance += amount
    print("New balance:", balance)
elif choice == "3":
    amount = float(input("Withdraw amount: "))
    if amount <= balance:
        balance -= amount
        print("New balance:", balance)
    else:
        print("Insufficient funds!")
else:
    print("Invalid option")
