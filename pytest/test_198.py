# test_198.py
# Demonstrates INDIRECT parametrization: @pytest.mark.parametrize(..., indirect=True)
# sends each parameter value into a FIXTURE instead of straight into the test,
# so the fixture can do setup based on the value before the test runs.
#
#   pytest test_198.py -v
#
# The test still receives the fixture's RESULT, not the raw parameter.

import pytest

PROFILES = {
    1: {"name": "alice", "role": "admin"},
    2: {"name": "bob", "role": "viewer"},
    3: {"name": "carol", "role": "editor"},
}


@pytest.fixture
def user(request, tmp_path):
    # request.param is the parametrized value (the user id).
    user_id = request.param
    profile = PROFILES[user_id]
    path = tmp_path / f"user_{user_id}.txt"
    path.write_text(profile["name"], encoding="utf-8")
    return {"id": user_id, "profile_path": path, **profile}


@pytest.mark.parametrize("user", [1, 2, 3], indirect=True, ids=["alice", "bob", "carol"])
def test_fixture_prepared_a_file_per_case(user):
    assert user["profile_path"].read_text(encoding="utf-8") == user["name"]
    assert user["id"] in PROFILES


@pytest.mark.parametrize("user", [1, 3], indirect=["user"])
def test_indirect_list_form(user):
    # indirect=["user"] works too when only some arguments are indirect.
    assert user["role"] in {"admin", "editor"}
