# =========================================================
# 04 - LISTS: THE WORKHORSE DATA TYPE
# Programs 31-40: create, access, add, remove, sort, search.
# =========================================================

# ---------- Program 31: Create Lists ----------
empty = []
numbers = [1, 2, 3, 4, 5]
mixed = [10, "hello", 3.14, True]
nested = [[1, 2], [3, 4]]
print("Empty:", empty)
print("Numbers:", numbers)
print("Mixed:", mixed)
print("Nested:", nested)


# ---------- Program 32: Access List Elements ----------
colors = ["red", "green", "blue"]
print("First:", colors[0])
print("Last:", colors[-1])
print("First two:", colors[0:2])
print("Reversed:", colors[::-1])


# ---------- Program 33: Add Items to a List ----------
fruits = []
fruits.append("apple")          # add at end
fruits.append("banana")
fruits.insert(1, "cherry")      # add at index 1
fruits.extend(["mango", "grape"])  # add multiple
print("Fruits:", fruits)


# ---------- Program 34: Remove Items from a List ----------
nums = [10, 20, 30, 40, 50, 30]
nums.remove(30)          # remove first 30
print("After remove 30:", nums)
removed = nums.pop()     # remove last item
print("Popped:", removed, "-> list:", nums)
removed = nums.pop(1)    # remove item at index 1
print("Popped index 1:", removed, "-> list:", nums)
del nums[0]              # delete by index
print("After del index 0:", nums)


# ---------- Program 35: Update List Items ----------
marks = [50, 60, 70]
marks[1] = 65            # change value at index
marks.append(80)         # add new
marks[0] += 5            # increment value
print("Updated marks:", marks)


# ---------- Program 36: Iterate Over a List ----------
names = ["Amit", "Riya", "John"]
print("Names in list:")
for name in names:
    print("-", name)


# ---------- Program 37: Sort and Reverse Lists ----------
scores = [88, 45, 92, 61]
print("Original:", scores)
scores.sort()                       # ascending
print("Sorted:", scores)
scores.sort(reverse=True)           # descending
print("Descending:", scores)
scores.reverse()                    # reverse order
print("Reversed:", scores)


# ---------- Program 38: List Methods Summary ----------
data = [5, 2, 8, 2, 9]
print("Count of 2:", data.count(2))
print("Index of 8:", data.index(8))
print("Length:", len(data))
print("Sum:", sum(data))
print("Max:", max(data), "Min:", min(data))
print("Contains 9?", 9 in data)


# ---------- Program 39: Copy a List (important!) ----------
original = [1, 2, 3]
shallow_copy = original            # NOT a real copy - same list!
shallow_copy.append(99)
print("Original (oops!):", original)   # changed too

real_copy = original.copy()
real_copy.append(100)
print("Original (safe):", original)
print("Real copy:", real_copy)


# ---------- Program 40: Find Largest Number in a List ----------
numbers = input("Enter numbers separated by space: ").split()
numbers = [int(n) for n in numbers]     # list comprehension (see 04)
largest = numbers[0]
for n in numbers:
    if n > largest:
        largest = n
print("Largest number:", largest)
