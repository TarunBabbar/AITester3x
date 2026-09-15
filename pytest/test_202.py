# test_202.py
# Demonstrates the built-in `request` fixture: inspect the currently running
# test - its name, node id, file path, markers and the pytest configuration -
# and use that information during setup.
#
#   pytest test_202.py -v

import pytest


def test_request_exposes_identity(request):
    assert request.node.name == "test_request_exposes_identity"
    assert request.node.nodeid.endswith("test_202.py::test_request_exposes_identity")
    assert request.function is test_request_exposes_identity
    assert str(request.path).endswith("test_202.py")


@pytest.mark.reg
def test_request_sees_markers(request):
    marker = request.node.get_closest_marker("reg")
    assert marker is not None
    assert marker.name == "reg"
    assert request.node.get_closest_marker("smoke") is None


def test_request_exposes_config(request):
    assert request.config.rootpath.exists()
    assert isinstance(request.config.args, list)


@pytest.fixture
def named_tmp_file(request, tmp_path):
    """Create a temp file named after the test that requested it."""
    path = tmp_path / f"{request.node.name}.txt"
    path.write_text("written by the fixture", encoding="utf-8")
    return path


def test_fixture_uses_the_test_name(named_tmp_file):
    assert named_tmp_file.name == "test_fixture_uses_the_test_name.txt"
    assert named_tmp_file.read_text(encoding="utf-8") == "written by the fixture"
