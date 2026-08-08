# =========================================================
# 03 - STRINGS: METHODS, SLICING & FORMATTING
# Programs 21-30. Strings are everywhere - master them early.
# =========================================================

# ---------- Program 21: String Basics ----------
message = "Hello Python"
print("Length:", len(message))
print("Uppercase:", message.upper())
print("Lowercase:", message.lower())
print("Title case:", message.title())


# ---------- Program 22: String Indexing ----------
word = "PYTHON"
print("First char:", word[0])    # P
print("Last char:", word[-1])    # N
print("Second char:", word[1])   # Y


# ---------- Program 23: String Slicing ----------
text = "Programming"
print(text[0:7])    # Program
print(text[7:])     # ming
print(text[:5])     # Progr
print(text[::-1])   # gnimmargorP (reversed)


# ---------- Program 24: Concatenation and Repetition ----------
first = "Hello"
second = "World"
print(first + " " + second)   # Hello World
print("Ha" * 3)               # HaHaHa


# ---------- Program 25: Check Character Types ----------
value = "Python123"
print("Is all alphabets?", value.isalpha())
print("Is all digits?", value.isdigit())
print("Is alphanumeric?", value.isalnum())
print("Is uppercase?", value.isupper())
print("Is lowercase?", value.islower())


# ---------- Program 26: Search and Count in String ----------
sentence = "The cat sat on the mat"
print("Position of 'cat':", sentence.find("cat"))
print("Count of 'the':", sentence.lower().count("the"))
print("Starts with 'The'?", sentence.startswith("The"))
print("Ends with 'mat'?", sentence.endswith("mat"))


# ---------- Program 27: Replace and Split ----------
data = "apple,banana,cherry"
print("Replaced:", data.replace("apple", "mango"))
fruits = data.split(",")
print("Split into list:", fruits)
print("Joined back:", "-".join(fruits))


# ---------- Program 28: Strip Whitespace ----------
messy = "   Python is fun   "
print("Original:", repr(messy))
print("Stripped:", repr(messy.strip()))
print("Left stripped:", repr(messy.lstrip()))
print("Right stripped:", repr(messy.rstrip()))


# ---------- Program 29: String Formatting Methods ----------
name = "Sara"
age = 26

# f-string (Python 3.6+ - modern way)
print(f"f-string: {name} is {age} years old")

# format() method
print("format(): {} is {} years old".format(name, age))

# old % style
print("% style: %s is %d years old" % (name, age))


# ---------- Program 30: Palindrome Check ----------
word = input("Enter a word: ").lower()
if word == word[::-1]:
    print(word, "is a palindrome")
else:
    print(word, "is not a palindrome")
