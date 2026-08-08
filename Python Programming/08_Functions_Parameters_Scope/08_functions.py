# =========================================================
# 08 - FUNCTIONS: DEFINE, CALL, PARAMETERS & SCOPE
# Programs 71-80: reusable blocks of code.
# =========================================================

# ---------- Program 71: First Function ----------
def greet():
    print("Hello from my first function!")

greet()     # call the function
greet()     # can call many times


# ---------- Program 72: Function with Parameters ----------
def greet_person(name):
    print("Hello", name + "!")

greet_person("Amit")
greet_person("Riya")


# ---------- Program 73: Function with Return Value ----------
def add(a, b):
    return a + b

result = add(5, 7)
print("5 + 7 =", result)


# ---------- Program 74: Default Parameter Values ----------
def greet_with_age(name, age=18):
    print(name, "is", age, "years old")

greet_with_age("John", 25)   # uses given age
greet_with_age("Sara")       # uses default 18


# ---------- Program 75: Keyword Arguments ----------
def describe_person(name, age, city):
    print(f"{name} is {age} years old and lives in {city}")

describe_person(city="Delhi", name="Ravi", age=22)   # order doesn't matter


# ---------- Program 76: Multiple Return Values ----------
def get_min_max(numbers):
    return min(numbers), max(numbers)

smallest, largest = get_min_max([10, 4, 22, 7, 15])
print("Smallest:", smallest, "| Largest:", largest)


# ---------- Program 77: Arbitrary Arguments (*args) ----------
def total_sum(*numbers):
    return sum(numbers)

print("Sum of 1,2,3:", total_sum(1, 2, 3))
print("Sum of 1,2,3,4,5:", total_sum(1, 2, 3, 4, 5))


# ---------- Program 78: Lambda (anonymous) Functions ----------
square = lambda x: x * x
add = lambda a, b: a + b
print("Square of 6:", square(6))
print("Add 4 + 9:", add(4, 9))

# lambda with map()
nums = [1, 2, 3, 4]
doubled = list(map(lambda x: x * 2, nums))
print("Doubled list:", doubled)


# ---------- Program 79: Global vs Local Variables ----------
counter = 10   # global variable

def modify_counter():
    global counter     # tell Python to use global variable
    counter += 5

print("Before:", counter)
modify_counter()
print("After:", counter)


# ---------- Program 80: Recursion - Factorial ----------
def factorial(n):
    if n == 0 or n == 1:
        return 1               # base case - stops recursion
    return n * factorial(n - 1)  # recursive call

number = 5
print("Factorial of", number, "is", factorial(number))
