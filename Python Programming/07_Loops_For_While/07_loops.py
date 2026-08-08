# =========================================================
# 07 - LOOPS: for AND while + BREAK/CONTINUE
# Programs 61-70: repetition made easy.
# =========================================================

# ---------- Program 61: for Loop with range() ----------
print("Numbers 0 to 4:")
for i in range(5):
    print(i, end=" ")
print()


# ---------- Program 62: for Loop with start, stop, step ----------
print("Even numbers 2 to 10:")
for i in range(2, 11, 2):
    print(i, end=" ")
print()

print("Countdown:")
for i in range(5, 0, -1):
    print(i, end=" ")
print()


# ---------- Program 63: Iterate Over a String ----------
word = "Python"
print("Characters in", word + ":")
for ch in word:
    print(ch)


# ---------- Program 64: while Loop ----------
count = 1
print("While loop 1 to 5:")
while count <= 5:
    print(count, end=" ")
    count += 1
print()


# ---------- Program 65: Sum of First N Numbers ----------
n = int(input("Enter a number: "))
total = 0
for i in range(1, n + 1):
    total += i
print("Sum of 1 to", n, "is", total)


# ---------- Program 66: Multiplication Table ----------
num = int(input("Enter a number for table: "))
for i in range(1, 11):
    print(f"{num} x {i} = {num * i}")


# ---------- Program 67: break Statement ----------
print("Stop at 5 using break:")
for i in range(1, 11):
    if i == 5:
        break       # exit loop immediately
    print(i, end=" ")
print()


# ---------- Program 68: continue Statement ----------
print("Skip 5 using continue:")
for i in range(1, 11):
    if i == 5:
        continue    # skip this iteration
    print(i, end=" ")
print()


# ---------- Program 69: Nested Loops (patterns) ----------
print("Pattern with nested loops:")
for row in range(1, 5):
    for col in range(row):
        print("*", end=" ")
    print()     # new line after each row


# ---------- Program 70: Print Fibonacci Series ----------
n = int(input("How many Fibonacci numbers? "))
a, b = 0, 1
count = 0
print("Fibonacci series:")
while count < n:
    print(a, end=" ")
    a, b = b, a + b
    count += 1
print()
