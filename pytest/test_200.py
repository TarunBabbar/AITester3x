# test_200.py
# Demonstrates the caplog fixture: capture records produced through the logging
# module and assert on their messages, levels and logger names.
#
#   pytest test_200.py -v
#
# Use caplog.at_level(...) to set the level for the captured block, caplog.text
# for the formatted output, and caplog.records for the LogRecord objects.

import logging

import pytest

logger = logging.getLogger("app.orders")


def place_order(item: str, quantity: int) -> int:
    logger.info("placing order: %s x%s", item, quantity)
    if quantity <= 0:
        logger.error("invalid quantity %s", quantity)
        raise ValueError("quantity must be positive")
    logger.info("order placed: %s", item)
    return quantity


def test_info_messages_are_logged(caplog):
    with caplog.at_level(logging.INFO, logger="app.orders"):
        place_order("book", 2)

    assert "placing order: book x2" in caplog.text
    assert "order placed: book" in caplog.text
    assert all(record.name == "app.orders" for record in caplog.records)
    assert caplog.records[0].levelname == "INFO"


def test_error_is_logged_then_raised(caplog):
    with caplog.at_level(logging.ERROR, logger="app.orders"):
        with pytest.raises(ValueError):
            place_order("book", 0)

    assert any(record.levelno == logging.ERROR for record in caplog.records)
    assert "invalid quantity 0" in caplog.text


def test_no_info_records_below_the_threshold(caplog):
    with caplog.at_level(logging.WARNING, logger="app.orders"):
        place_order("book", 1)
    # INFO messages are filtered out at WARNING level, so nothing is captured.
    assert caplog.records == []
