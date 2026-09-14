# test_195.py
# Demonstrates @pytest.mark.usefixtures: attach a fixture to a test (or a whole
# class) WITHOUT naming it as an argument. Use this when the fixture only needs
# to run setup/teardown and the test body does not use its return value.
#
#   pytest test_195.py -v
#
# A normal fixture must be listed as a parameter; usefixtures keeps the test
# signature clean and still runs the fixture.

import pytest

events: list[str] = []


@pytest.fixture
def audit_log():
    events.append("setup: audit started")
    yield
    events.append("teardown: audit finished")


@pytest.fixture
def connect_db():
    events.append("setup: db connected")
    yield
    events.append("teardown: db closed")


@pytest.mark.usefixtures("audit_log")
def test_fixture_ran_without_parameter():
    # audit_log is not an argument here, yet its setup already ran.
    assert events[-1] == "setup: audit started"


@pytest.mark.usefixtures("connect_db")
def test_second_fixture_ran():
    assert events[-1] == "setup: db connected"


@pytest.mark.usefixtures("connect_db")
class TestGroup:
    """usefixtures on a class applies to every test method inside it."""

    def test_first_in_group(self):
        assert events[-1] == "setup: db connected"

    def test_second_in_group(self):
        # Fresh setup ran again for this test method.
        assert events[-1] == "setup: db connected"
