# test_209.py
# Demonstrates request.getfixturevalue: a test can ask for a fixture by name at
# runtime, which is how you choose a fixture from data rather than from code.
#
#   pytest test_209.py -v
#
# A fixture named in the test signature is built before the test runs. One
# fetched with getfixturevalue is built only when the call happens - which is
# both the point of it and the reason it is easy to misuse.

import pytest


@pytest.fixture
def small() -> int:
    return 10


@pytest.fixture
def large() -> int:
    return 1000


@pytest.fixture
def greeting() -> str:
    return "hello"


@pytest.mark.parametrize(
    "fixture_name, expected",
    [("small", 10), ("large", 1000), ("greeting", "hello")],
    ids=["small", "large", "greeting"],
)
def test_fixture_chosen_at_runtime(request, fixture_name, expected):
    # The name comes from the parametrization, so the fixture is picked by data
    # rather than by writing a separate test for each one.
    assert request.getfixturevalue(fixture_name) == expected


def test_fixtures_can_be_chained_by_value(request):
    # Fetch one fixture, then let its value decide what to fetch next.
    chosen = "small" if request.getfixturevalue("small") == 10 else "large"
    assert request.getfixturevalue(chosen) == 10


def test_fixturenames_always_include_request(request, small):
    assert "request" in request.fixturenames
    # "small" was named in the signature, so it is certainly there.
    assert "small" in request.fixturenames


built: list[str] = []


@pytest.fixture
def measured() -> str:
    built.append("built")
    return "measured value"


def test_a_fixture_is_not_built_until_it_is_asked_for(request):
    before = built.count("built")
    # Nothing in this test's signature asks for "measured", so it has not run.
    assert built.count("built") == before, f"it should not have run early: {built}"
    assert request.getfixturevalue("measured") == "measured value"
    assert built.count("built") == before + 1, "and now it has run, exactly once"


def test_the_second_fetch_comes_from_the_cache(request):
    # A fresh build for this test, then a second fetch served from the cache.
    before = built.count("built")
    first = request.getfixturevalue("measured")
    second = request.getfixturevalue("measured")
    assert first == second == "measured value"
    assert built.count("built") == before + 1, f"one new build served two fetches: {built}"


@pytest.fixture
def tripled(request) -> int:
    # This fixture pulls another one itself, at runtime.
    return request.getfixturevalue("small") * 3


def test_a_fixture_can_pull_another_at_runtime(tripled):
    assert tripled == 30


def test_an_unknown_name_is_reported(request):
    with pytest.raises(LookupError) as error:
        request.getfixturevalue("no_such_fixture")
    assert "no_such_fixture" in str(error.value), "the error names the missing fixture"
