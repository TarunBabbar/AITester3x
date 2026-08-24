"""
Sample OOP examples — inheritance and polymorphism.
"""


class Animal:
    """Base class."""

    def __init__(self, name: str):
        self.name = name

    def speak(self) -> str:
        raise NotImplementedError("Subclasses must implement speak()")


class Dog(Animal):
    def speak(self) -> str:
        return f"{self.name} says Woof!"


class Cat(Animal):
    def speak(self) -> str:
        return f"{self.name} says Meow!"


class Duck(Animal):
    def speak(self) -> str:
        return f"{self.name} says Quack!"


def make_them_speak(animals: list[Animal]) -> list[str]:
    """Polymorphism: same interface, different behaviour."""
    return [a.speak() for a in animals]


if __name__ == "__main__":
    pets = [Dog("Rex"), Cat("Whiskers"), Duck("Donald")]
    for line in make_them_speak(pets):
        print(line)
