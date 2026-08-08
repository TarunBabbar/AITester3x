# =========================================================
# 09 - OBJECT-ORIENTED PROGRAMMING (OOP)
# Programs 81-90: classes, objects, inheritance & more.
# =========================================================

# ---------- Program 81: Create a Simple Class ----------
class Dog:
    pass     # empty class for now

my_dog = Dog()
print("Object created:", my_dog)


# ---------- Program 82: Class with Attributes ----------
class Person:
    species = "Human"     # class attribute (shared by all)

person1 = Person()
person1.name = "Amit"     # instance attribute
person1.age = 25
print(person1.name, "is", person1.age, "-", person1.species)


# ---------- Program 83: Constructor __init__ ----------
class Student:
    def __init__(self, name, roll_no):
        self.name = name       # instance attributes
        self.roll_no = roll_no

s1 = Student("Riya", 101)
s2 = Student("John", 102)
print(s1.name, "- Roll:", s1.roll_no)
print(s2.name, "- Roll:", s2.roll_no)


# ---------- Program 84: Methods Inside a Class ----------
class Rectangle:
    def __init__(self, length, width):
        self.length = length
        self.width = width

    def area(self):
        return self.length * self.width

    def perimeter(self):
        return 2 * (self.length + self.width)

rect = Rectangle(5, 3)
print("Area:", rect.area())
print("Perimeter:", rect.perimeter())


# ---------- Program 85: Inheritance ----------
class Animal:
    def __init__(self, name):
        self.name = name

    def speak(self):
        print(self.name, "makes a sound")

class Dog(Animal):          # Dog inherits from Animal
    def speak(self):        # override parent method
        print(self.name, "barks")

class Cat(Animal):
    def speak(self):
        print(self.name, "meows")

d = Dog("Buddy")
c = Cat("Kitty")
d.speak()
c.speak()


# ---------- Program 86: __str__ Method ----------
class BankAccount:
    def __init__(self, owner, balance=0):
        self.owner = owner
        self.balance = balance

    def deposit(self, amount):
        self.balance += amount

    def withdraw(self, amount):
        if amount <= self.balance:
            self.balance -= amount
        else:
            print("Insufficient funds")

    def __str__(self):
        return f"{self.owner}'s balance: Rs.{self.balance}"

account = BankAccount("Sara", 1000)
account.deposit(500)
account.withdraw(200)
print(account)     # calls __str__ automatically


# ---------- Program 87: Encapsulation (private attributes) ----------
class Secret:
    def __init__(self):
        self.__hidden = 42      # name mangled - not truly private

    def reveal(self):
        return self.__hidden

s = Secret()
print("Hidden value:", s.reveal())
# print(s.__hidden)   # AttributeError - can't access directly


# ---------- Program 88: Class Variables and Methods ----------
class Employee:
    company = "TechCorp"        # class variable
    total_employees = 0

    def __init__(self, name):
        self.name = name
        Employee.total_employees += 1

    @classmethod
    def get_total(cls):
        return cls.total_employees

e1 = Employee("Alice")
e2 = Employee("Bob")
print("Company:", Employee.company)
print("Total employees:", Employee.get_total())


# ---------- Program 89: Multiple Inheritance ----------
class Father:
    def hobby(self):
        print("Likes gardening")

class Mother:
    def talent(self):
        print("Good at painting")

class Child(Father, Mother):   # inherits from both
    pass

child = Child()
child.hobby()
child.talent()


# ---------- Program 90: Library Book System (mini project) ----------
class Book:
    def __init__(self, title, author):
        self.title = title
        self.author = author
        self.is_available = True

    def borrow(self):
        if self.is_available:
            self.is_available = False
            print(f"You borrowed '{self.title}'")
        else:
            print(f"'{self.title}' is already borrowed")

    def return_book(self):
        self.is_available = True
        print(f"'{self.title}' returned")

    def status(self):
        state = "available" if self.is_available else "borrowed"
        print(f"'{self.title}' by {self.author} - {state}")

book = Book("Python Basics", "Guido")
book.status()
book.borrow()
book.status()
book.borrow()       # should fail - already out
book.return_book()
book.status()
