"""Sample pytest tests for the OOP examples."""

from encapsulation import BankAccount
from inheritance import Dog, Cat, make_them_speak


def test_bank_account_deposit():
    acc = BankAccount("Bob", 100.0)
    assert acc.deposit(50.0) == 150.0


def test_bank_account_withdraw_insufficient():
    acc = BankAccount("Bob", 10.0)
    try:
        acc.withdraw(50.0)
        assert False, "should have raised"
    except ValueError:
        pass


def test_polymorphism():
    results = make_them_speak([Dog("Rex"), Cat("Kitty")])
    assert results == ["Rex says Woof!", "Kitty says Meow!"]
