"""
Program 30: Bank Account Ledger

Models bank accounts with deposits, withdrawals and transfers. Valid amounts
are applied and recorded in a per-account ledger; invalid amounts raise clear
errors. At the end each balance is reconciled against its ledger.

Concepts: classes, dataclasses, exceptions, validation, list of records.
"""

from dataclasses import dataclass, field


class ValidationError(Exception):
    """Raised when a transaction amount is not usable."""


class InsufficientFunds(Exception):
    """Raised when a withdrawal or transfer exceeds the balance."""


@dataclass
class Transaction:
    """One recorded movement of money on an account."""

    kind: str
    amount: float
    balance_after: float

    def __str__(self) -> str:
        return f"{self.kind:<10} {self.amount:>8.2f}  ->  balance {self.balance_after:>8.2f}"


def _check_amount(amount: float) -> float:
    """Validate and normalise a transaction amount."""
    if isinstance(amount, bool) or not isinstance(amount, (int, float)):
        raise ValidationError(f"amount must be a number, got {type(amount).__name__}")
    value = float(amount)
    if value <= 0:
        raise ValidationError(f"amount must be positive, got {value}")
    return value


@dataclass
class Account:
    owner: str
    balance: float = 0.0
    ledger: list[Transaction] = field(default_factory=list)

    def _record(self, kind: str, amount: float) -> Transaction:
        entry = Transaction(kind, amount, self.balance)
        self.ledger.append(entry)
        return entry

    def deposit(self, amount: float) -> Transaction:
        value = _check_amount(amount)
        self.balance += value
        return self._record("deposit", value)

    def withdraw(self, amount: float) -> Transaction:
        value = _check_amount(amount)
        if value > self.balance:
            raise InsufficientFunds(
                f"{self.owner} has {self.balance:.2f}, cannot withdraw {value:.2f}"
            )
        self.balance -= value
        return self._record("withdraw", value)

    def reconcile(self) -> bool:
        """True when the balance matches deposits minus withdrawals."""
        expected = sum(
            tx.amount if tx.kind == "deposit" else -tx.amount for tx in self.ledger
        )
        return abs(self.balance - expected) < 1e-9

    def statement(self) -> str:
        lines = [f"Statement for {self.owner}  (balance {self.balance:.2f})"]
        lines += [f"  {tx}" for tx in self.ledger]
        return "\n".join(lines)


def transfer(source: Account, target: Account, amount: float) -> None:
    """Move money between two accounts, recording both sides in the ledgers."""
    source.withdraw(amount)
    target.deposit(amount)


def main() -> None:
    alice = Account("Alice")
    bob = Account("Bob")

    alice.deposit(100)          # opening balance
    alice.deposit(250)
    alice.withdraw(50)
    transfer(alice, bob, 75)
    bob.deposit(25)
    bob.withdraw(10)

    for account in (alice, bob):
        print(account.statement())
        assert account.reconcile(), f"ledger mismatch for {account.owner}"
        print()

    print("Both ledgers reconcile with their balances.")

    print("\nInvalid operations are rejected:")
    for bad in (0, -5, "ten"):
        try:
            alice.deposit(bad)
        except ValidationError as exc:
            print(f"  deposit({bad!r}) -> ValidationError: {exc}")
    try:
        bob.withdraw(10_000)
    except InsufficientFunds as exc:
        print(f"  withdraw(10000) -> InsufficientFunds: {exc}")


if __name__ == "__main__":
    main()
