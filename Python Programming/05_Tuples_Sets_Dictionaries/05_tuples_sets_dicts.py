# =========================================================
# 05 - TUPLES, SETS & DICTIONARIES
# Programs 41-50: immutable, unique, and key-value data.
# =========================================================

# ---------- Program 41: Tuple Basics ----------
point = (3, 4)
print("Tuple:", point)
print("x =", point[0], "y =", point[1])
print("Length:", len(point))

# Tuples are IMMUTABLE - this would fail:
# point[0] = 10   -> TypeError


# ---------- Program 42: Unpack Tuples ----------
person = ("Alice", 30, "Engineer")
name, age, job = person
print("Name:", name, "| Age:", age, "| Job:", job)


# ---------- Program 43: Tuple Functions ----------
numbers = (5, 2, 8, 2, 9, 2)
print("Count of 2:", numbers.count(2))
print("Index of 9:", numbers.index(9))
print("Max:", max(numbers), "Min:", min(numbers), "Sum:", sum(numbers))


# ---------- Program 44: Return Multiple Values (tuple) ----------
def divide(a, b):
    quotient = a // b
    remainder = a % b
    return quotient, remainder   # returns a tuple

q, r = divide(17, 5)
print("17 / 5 -> quotient:", q, "remainder:", r)


# ---------- Program 45: Set Basics ----------
fruits = {"apple", "banana", "cherry"}
print("Set:", fruits)
print("Is 'banana' in set?", "banana" in fruits)

# Sets do NOT allow duplicates
numbers = {1, 2, 2, 3, 3, 3}
print("Set with duplicates removed:", numbers)


# ---------- Program 46: Set Operations ----------
a = {1, 2, 3, 4}
b = {3, 4, 5, 6}
print("Union:", a | b)           # all elements
print("Intersection:", a & b)    # common elements
print("Difference a-b:", a - b)  # only in a
print("Symmetric diff:", a ^ b)  # in one, not both


# ---------- Program 47: Set Methods ----------
s = {1, 2, 3}
s.add(4)
s.add(4)          # no effect - already present
s.update([5, 6])  # add multiple
print("After add/update:", s)
s.discard(99)     # safe remove - no error
s.remove(1)       # raises error if missing
print("After discard/remove:", s)


# ---------- Program 48: Dictionary Basics ----------
student = {
    "name": "Ravi",
    "age": 22,
    "course": "BCA",
}
print("Dictionary:", student)
print("Name:", student["name"])
print("Age:", student.get("age"))
print("Keys:", student.keys())
print("Values:", student.values())


# ---------- Program 49: Update and Remove Dictionary Items ----------
phone_book = {"Alice": "1234", "Bob": "5678"}
phone_book["Charlie"] = "9999"      # add new
phone_book["Alice"] = "1111"        # update existing
print("After updates:", phone_book)
del phone_book["Bob"]               # delete key
removed = phone_book.pop("Charlie") # remove and get value
print("Removed:", removed)
print("Final phone book:", phone_book)


# ---------- Program 50: Iterate Over a Dictionary ----------
scores = {"Math": 90, "Science": 85, "English": 88}
print("Subject scores:")
for subject, score in scores.items():
    print(f"  {subject}: {score}")
print("Average score:", sum(scores.values()) / len(scores))
