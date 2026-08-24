"""
Sample OOP examples — abstract base classes and interfaces.
"""

from abc import ABC, abstractmethod
import math


class Shape(ABC):
    """Abstract base class enforcing area() on subclasses."""

    @abstractmethod
    def area(self) -> float:
        pass

    @abstractmethod
    def perimeter(self) -> float:
        pass


class Rectangle(Shape):
    def __init__(self, width: float, height: float):
        self.width = width
        self.height = height

    def area(self) -> float:
        return self.width * self.height

    def perimeter(self) -> float:
        return 2 * (self.width + self.height)


class Circle(Shape):
    def __init__(self, radius: float):
        self.radius = radius

    def area(self) -> float:
        return math.pi * self.radius**2

    def perimeter(self) -> float:
        return 2 * math.pi * self.radius


if __name__ == "__main__":
    shapes: list[Shape] = [Rectangle(3, 4), Circle(5)]
    for s in shapes:
        print(f"{s.__class__.__name__}: area={s.area():.2f}, perimeter={s.perimeter():.2f}")
