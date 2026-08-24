"""
Sample OOP examples — Python classes and objects.
"""


class Car:
    """A simple Car class demonstrating attributes and methods."""

    def __init__(self, make: str, model: str, year: int):
        self.make = make
        self.model = model
        self.year = year
        self.is_running = False

    def start(self) -> str:
        self.is_running = True
        return f"{self.make} {self.model} started."

    def stop(self) -> str:
        self.is_running = False
        return f"{self.make} {self.model} stopped."

    def describe(self) -> str:
        state = "running" if self.is_running else "off"
        return f"{self.year} {self.make} {self.model} ({state})"


if __name__ == "__main__":
    my_car = Car("Toyota", "Camry", 2022)
    print(my_car.describe())
    print(my_car.start())
    print(my_car.describe())
    print(my_car.stop())
