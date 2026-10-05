# test_207.py
# Demonstrates pytest-asyncio: async test functions, async fixtures, and awaiting
# inside tests and assertions.
#
#   pytest test_207.py -v
#
# The plugin is required. This module skips cleanly when it is absent, so the
# rest of the suite still runs.

import asyncio

import pytest

pytest.importorskip("pytest_asyncio")

import pytest_asyncio


@pytest_asyncio.fixture
async def collected():
    """An async fixture: the event loop is running while it sets up and tears down."""
    items: list[str] = []
    yield items
    items.clear()


@pytest.mark.asyncio
async def test_a_coroutine_runs():
    async def answer() -> int:
        await asyncio.sleep(0)
        return 42

    assert await answer() == 42


@pytest.mark.asyncio
async def test_gather_returns_results_in_order():
    async def slow(name: str, delay: float) -> str:
        await asyncio.sleep(delay)
        return name

    results = await asyncio.gather(slow("first", 0.02), slow("second", 0.01))
    assert results == ["first", "second"], "gather keeps the argument order"


@pytest.mark.asyncio
async def test_async_fixture_gives_a_fresh_list(collected):
    collected.append("filled")
    assert collected == ["filled"]


@pytest.mark.asyncio
async def test_async_fixture_ran_again(collected):
    # The fixture is function scoped, so this test starts with a new list.
    assert collected == []


@pytest.mark.asyncio
async def test_timeout_is_raised():
    with pytest.raises(asyncio.TimeoutError):
        await asyncio.wait_for(asyncio.sleep(1), timeout=0.01)


@pytest.mark.asyncio
async def test_a_semaphore_caps_concurrency():
    semaphore = asyncio.Semaphore(2)
    state = {"active": 0, "peak": 0}

    async def limited() -> None:
        async with semaphore:
            state["active"] += 1
            state["peak"] = max(state["peak"], state["active"])
            await asyncio.sleep(0.01)
            state["active"] -= 1

    await asyncio.gather(*(limited() for _ in range(8)))
    assert state["peak"] == 2, f"two at a time, saw {state['peak']}"
    assert state["active"] == 0, "every slot was released"


@pytest.mark.asyncio
async def test_a_task_can_be_cancelled():
    async def patient() -> str:
        await asyncio.sleep(5)
        return "never"

    task = asyncio.create_task(patient())
    await asyncio.sleep(0.01)
    task.cancel()
    with pytest.raises(asyncio.CancelledError):
        await task
    assert task.cancelled(), "the task reports itself as cancelled"


@pytest.mark.asyncio
async def test_task_group_collects_failures():
    async def boom() -> None:
        await asyncio.sleep(0.01)
        raise ValueError("boom")

    with pytest.raises(ExceptionGroup) as error:
        async with asyncio.TaskGroup() as group:
            group.create_task(boom())
            group.create_task(boom())

    assert len(error.value.exceptions) == 2, "both failures were collected"


def test_an_ordinary_sync_test_still_runs():
    # Async tests sit happily beside plain ones in the same file.
    assert sum([1, 2, 3]) == 6
