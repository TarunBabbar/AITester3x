"""
Sample OOP examples — encapsulation, properties, and private members.
"""


class BankAccount:
    """Encapsulates a balance; mutations go through methods only."""

    def __init__(self, owner: str, initial_balance: float = 0.0):
        self.owner = owner
        self.__balance = initial_balance  # name-mangled private field

    def deposit(self, amount: float) -> float:
        if amount <= 0:
            raise ValueError("Deposit must be positive")
        self.__balance += amount
        return self.__balance

    def withdraw(self, amount: float) -> float:
        if amount <= 0:
            raise ValueError("Withdrawal must be positive")
        if amount > self.__balance:
            raise ValueError("Insufficient funds")
        self.__balance -= amount
        return self.__balance

    @property
    def balance(self) -> float:
        """Read-only view of the balance."""
        return self.__balance

    def __str__(self) -> str:
        return f"Account[{self.owner}] balance=${self.balance:.2f}"


if __name__ == "__main__":
    acc = BankAccount("Alice", 100.0)
    print(acc)
    acc.deposit(50.0)
    print("After deposit:", acc)
    acc.withdraw(30.0)
    print("After withdrawal:", acc)
    try:
        acc.withdraw(500.0)
    except ValueError as e:
        print("Expected error:", e)
