"""
Sample OOP examples — class methods, static methods, and dunder methods.
"""


class Temperature:
    """Demonstrates @classmethod, @staticmethod, and __repr__/__eq__."""

    def __init__(self, celsius: float):
        self.celsius = celsius

    @classmethod
    def from_fahrenheit(cls, fahrenheit: float) -> "Temperature":
        celsius = (fahrenheit - 32) * 5 / 9
        return cls(celsius)

    @staticmethod
    def celsius_to_fahrenheit(celsius: float) -> float:
        return celsius * 9 / 5 + 32

    def __repr__(self) -> str:
        return f"Temperature({self.celsius:.1f}C)"

    def __eq__(self, other: object) -> bool:
        if not isinstance(other, Temperature):
            return NotImplemented
        return self.celsius == other.celsius


if __name__ == "__main__":
    t1 = Temperature.from_fahrenheit(212)  # boiling point
    t2 = Temperature(100.0)
    print("t1:", t1)
    print("t2:", t2)
    print("Equal?", t1 == t2)
    print("212F ->", Temperature.celsius_to_fahrenheit(100), "F")
