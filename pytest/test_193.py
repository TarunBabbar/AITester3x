# test_193.py
# Demonstrates parameterized fixtures: @pytest.fixture(params=[...]) makes the
# fixture run once per parameter, and every test that uses it runs once per
# parameter too. Combining two parameterized fixtures gives their cross product.
#
#   pytest test_193.py -v
#
# The current value is available inside the fixture as request.param. The
# ids=[...] argument gives readable test names instead of param0/param1.

import pytest


@pytest.fixture(params=[(2, 3, 6), (3, 3, 9), (0, 5, 0)], ids=["2x3", "3x3", "0x5"])
def multiply_case(request):
    """One (left, right, expected) case per parameter."""
    return request.param


def test_multiplication(multiply_case):
    # This single test body runs 3 times, once per multiply_case parameter.
    left, right, expected = multiply_case
    assert left * right == expected


@pytest.fixture(params=["alice", "bob", "carol"])
def username(request):
    return request.param


@pytest.fixture(params=["admin", "viewer"])
def role(request):
    return request.param


def test_two_fixtures_multiply_out(username, role):
    # 3 usernames x 2 roles = 6 test runs from one function.
    account = f"{username}:{role}"
    assert ":" in account
    assert account.split(":")[0] == username


def test_request_param_is_accessible_directly(username):
    # request.param is the raw value behind the fixture, useful for messages.
    assert username.islower()
