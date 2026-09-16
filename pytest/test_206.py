# test_206.py
# Demonstrates unittest.mock used from pytest: MagicMock as a stand-in
# collaborator, patch.object to swap a method for one test, side_effect for
# sequences or errors, and call assertions (assert_called_once_with, call_args_list).
#
#   pytest test_206.py -v
#
# Difference from monkeypatch (test_184/191/197): monkeypatch edits an attribute
# and restores it, while unittest.mock ALSO records how the fake was called,
# which is what these tests assert on.

from unittest.mock import MagicMock, call, patch

import pytest


class WeatherClient:
    def get_temperature(self, city: str) -> float:
        raise RuntimeError("real network call - patch this in tests")


def report(client: WeatherClient, city: str) -> str:
    temperature = client.get_temperature(city)
    fahrenheit = temperature * 9 / 5 + 32
    return f"{city}: {temperature:.0f}C / {fahrenheit:.0f}F"


@patch.object(WeatherClient, "get_temperature", return_value=20.0)
def test_patch_object_replaces_method(mock_get):
    assert report(WeatherClient(), "London") == "London: 20C / 68F"
    mock_get.assert_called_once_with("London")


def test_magic_mock_as_collaborator():
    # spec= keeps the mock honest: only real attribute names are allowed.
    client = MagicMock(spec=WeatherClient)
    client.get_temperature.return_value = 30.0
    assert report(client, "Cairo") == "Cairo: 30C / 86F"
    assert client.get_temperature.call_count == 1


def test_call_arguments_are_recorded():
    client = MagicMock(spec=WeatherClient)
    client.get_temperature.return_value = 5.0
    report(client, "Paris")
    report(client, "Rome")
    assert client.get_temperature.call_args_list == [call("Paris"), call("Rome")]

    client.get_temperature.assert_called_with("Rome")
    client.get_temperature.reset_mock()
    assert client.get_temperature.call_count == 0


def test_side_effect_returns_a_sequence_then_raises():
    fake = MagicMock(side_effect=[10.0, 20.0, ValueError("service offline")])
    assert fake("first") == 10.0
    assert fake("second") == 20.0
    with pytest.raises(ValueError, match="offline"):
        fake("third")


def test_real_method_is_back_after_patching():
    # The patch above was undone automatically, so the real method runs again.
    with pytest.raises(RuntimeError, match="network call"):
        WeatherClient().get_temperature("London")
