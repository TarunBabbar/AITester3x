# test_211.py
# Testing asynchronous code from ordinary synchronous tests: driving coroutines
# with asyncio.run, checking that work really overlaps, and asserting on
# timeouts and cancellation.
#
#   pytest test_211.py -v
#
# pytest runs synchronous tests by default. Without a plugin such as
# pytest-asyncio there is no "async def test_..." support, and the way to test
# a coroutine is to hand it to asyncio.run from inside a normal test function.
# Each test then gets a fresh event loop that is closed when it returns, which
# is a clean slate and easy to reason about. The one thing to remember is that
# asyncio.run cannot be called from a thread that already has a running loop.

import asyncio
import gc
import time

import pytest


async def add_numbers(*values: int) -> int:
    await asyncio.sleep(0)          # yield once, as real code would at an await
    return sum(values)


async def fetch(name: str, delay: float) -> tuple[str, float]:
    await asyncio.sleep(delay)
    return name, delay


async def fetch_all(*delays: float) -> list[tuple[str, float]]:
    return await asyncio.gather(*(fetch(str(index), delay) for index, delay in enumerate(delays)))


async def fetch_one_at_a_time(*delays: float) -> list[tuple[str, float]]:
    return [await fetch(str(index), delay) for index, delay in enumerate(delays)]


# gather has to be awaited from inside a coroutine. Calling asyncio.gather(...)
# in a test body would need a running loop, and there is not one outside
# asyncio.run, which is why these helpers exist.
async def gather_leniently(*coroutines) -> list:
    """Failures come back as values instead of being raised."""
    return await asyncio.gather(*coroutines, return_exceptions=True)


async def gather_strictly(*coroutines) -> list:
    """The default: the first failure is raised."""
    return await asyncio.gather(*coroutines)


def test_a_coroutine_runs_under_asyncio_run():
    assert asyncio.run(add_numbers(1, 2, 3)) == 6


def test_asyncio_run_gives_back_whatever_the_coroutine_returned():
    assert asyncio.run(fetch("thing", 0)) == ("thing", 0)


def test_each_asyncio_run_gets_its_own_event_loop():
    first = asyncio.run(current_loop_id())
    second = asyncio.run(current_loop_id())
    # Different loop objects, because each call creates and closes one.
    assert first != second, "asyncio.run should not hand back the same loop twice"


async def current_loop_id() -> int:
    return id(asyncio.get_running_loop())


def test_a_coroutine_object_does_nothing_until_it_is_awaited():
    coroutine = add_numbers(1, 2, 3)
    # Creating a coroutine runs no code at all; it is a lazy object.
    assert asyncio.iscoroutine(coroutine), "and it is recognisable as one"
    with pytest.warns(RuntimeWarning, match="never awaited"):
        del coroutine
        gc.collect()        # the warning is raised when it is collected


def test_gather_overlaps_what_a_loop_of_awaits_cannot():
    start = time.perf_counter()
    concurrent = asyncio.run(fetch_all(0.2, 0.2, 0.2))
    together = time.perf_counter() - start

    start = time.perf_counter()
    sequential = asyncio.run(fetch_one_at_a_time(0.2, 0.2, 0.2))
    one_at_a_time = time.perf_counter() - start

    assert sorted(concurrent) == sorted(sequential), "the same three results either way"
    assert len(concurrent) == 3 and len(sequential) == 3
    # Three 200ms waits started together cost about 200ms. Awaited in turn they
    # cost 600ms. Comparing the two against each other, rather than against a
    # fixed number of seconds, keeps this honest on a slow or busy machine.
    assert together < one_at_a_time * 0.75, (
        f"gather should overlap the waits: {together:.3f}s together against "
        f"{one_at_a_time:.3f}s one at a time")


def test_the_overlap_is_the_whole_difference():
    # The same coroutines, the same delays, so the only variable is how they are
    # started. Anything that overlapped would show up here.
    async def three_sleeps_together() -> float:
        start = time.perf_counter()
        await asyncio.gather(asyncio.sleep(0.1), asyncio.sleep(0.1), asyncio.sleep(0.1))
        return time.perf_counter() - start

    async def three_sleeps_in_turn() -> float:
        start = time.perf_counter()
        for _ in range(3):
            await asyncio.sleep(0.1)
        return time.perf_counter() - start

    together = asyncio.run(three_sleeps_together())
    in_turn = asyncio.run(three_sleeps_in_turn())

    assert together < in_turn, f"{together:.3f}s together, {in_turn:.3f}s in turn"
    assert together < in_turn * 0.75, "and clearly so, not by a hair"


def test_wait_for_reports_a_timeout():
    async def slow() -> str:
        await asyncio.sleep(5)
        return "eventually"

    with pytest.raises(asyncio.TimeoutError):
        asyncio.run(asyncio.wait_for(slow(), timeout=0.05))


def test_a_timeout_cancels_the_coroutine_underneath():
    cancelled = []

    async def slow() -> str:
        try:
            await asyncio.sleep(5)
        except asyncio.CancelledError:
            cancelled.append(True)
            raise                       # a well behaved coroutine re-raises it
        return "finished"

    with pytest.raises(asyncio.TimeoutError):
        asyncio.run(asyncio.wait_for(slow(), timeout=0.05))

    assert cancelled == [True], "the timeout cancelled the work rather than abandoning it"


def test_wait_for_returns_the_value_when_it_finishes_in_time():
    async def quick() -> str:
        await asyncio.sleep(0)
        return "done"

    assert asyncio.run(asyncio.wait_for(quick(), timeout=5)) == "done"


def test_gather_can_collect_failures_instead_of_raising_them():
    async def broken() -> str:
        raise ValueError("this task failed")

    results = asyncio.run(gather_leniently(add_numbers(1, 1), broken()))

    assert results[0] == 2, "the good result is still there"
    assert isinstance(results[1], ValueError), "and the failure came back as a value"


def test_gather_raises_the_first_failure_by_default():
    async def broken() -> str:
        raise ValueError("this task failed")

    with pytest.raises(ValueError, match="this task failed"):
        asyncio.run(gather_strictly(add_numbers(1, 1), broken()))


def test_a_semaphore_holds_the_limit():
    active = 0
    peak = 0

    async def worker(semaphore: asyncio.Semaphore) -> None:
        nonlocal active, peak
        async with semaphore:
            active += 1
            peak = max(peak, active)
            await asyncio.sleep(0.01)
            active -= 1

    async def run_all() -> None:
        semaphore = asyncio.Semaphore(2)
        await asyncio.gather(*(worker(semaphore) for _ in range(6)))

    asyncio.run(run_all())
    assert peak == 2, f"never more than two at once, but saw {peak}"
    assert active == 0, "and every worker released it again"


def test_a_queue_passes_values_between_tasks():
    async def producer(queue: asyncio.Queue) -> None:
        for value in range(5):
            await queue.put(value)
        await queue.put(None)           # the sentinel that says stop

    async def consumer(queue: asyncio.Queue) -> list[int]:
        collected = []
        while True:
            item = await queue.get()
            if item is None:
                return collected
            collected.append(item)

    async def run_both() -> list[int]:
        queue: asyncio.Queue = asyncio.Queue(maxsize=2)
        _, collected = await asyncio.gather(producer(queue), consumer(queue))
        return collected

    assert asyncio.run(run_both()) == [0, 1, 2, 3, 4]


def test_an_async_generator_is_consumed_with_async_for():
    async def countdown(start: int):
        for value in range(start, 0, -1):
            await asyncio.sleep(0)
            yield value

    async def collect() -> list[int]:
        return [value async for value in countdown(3)]

    assert asyncio.run(collect()) == [3, 2, 1]


def test_a_single_run_drives_several_coroutines_in_sequence():
    async def scenario() -> tuple[int, list[tuple[str, float]], list[int]]:
        total = await add_numbers(1, 2, 3)
        fetched = await fetch_all(0, 0)
        counted = [value async for value in count_up(3)]
        return total, fetched, counted

    async def count_up(limit: int):
        for value in range(limit):
            yield value

    assert asyncio.run(scenario()) == (6, [("0", 0), ("1", 0)], [0, 1, 2])


def test_running_loop_is_absent_outside_a_coroutine():
    # This is why asyncio.run is needed: outside a coroutine there is no loop.
    with pytest.raises(RuntimeError, match="no running event loop"):
        asyncio.get_running_loop()
