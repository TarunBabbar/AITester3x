"""
Program 44: Asyncio Concurrency

Runs coroutines concurrently rather than one after another: gather,
as_completed, task groups, semaphores, timeouts, queues and cancellation.

The sleeps are deliberately short and the checks are on outcomes and orderings,
so nothing depends on how fast the machine is.

Concepts: async/await, the event loop, tasks, cancellation, back pressure.

    python 44_Asyncio_Concurrency.py
"""

import asyncio
import time


async def work(name: str, delay: float, fail: bool = False) -> str:
    """A stand-in for slow I/O: wait, then return a result or raise."""
    await asyncio.sleep(delay)
    if fail:
        raise ValueError(f"{name} failed")
    return f"{name} finished after {delay}s"


async def main() -> None:
    # ---- one after another, then all at once --------------------------------
    start = time.perf_counter()
    sequential = [await work(f"seq{i}", 0.05) for i in range(5)]
    sequential_time = time.perf_counter() - start

    start = time.perf_counter()
    concurrent = await asyncio.gather(*(work(f"par{i}", 0.05) for i in range(5)))
    concurrent_time = time.perf_counter() - start

    assert len(sequential) == len(concurrent) == 5
    assert concurrent_time < sequential_time / 2, "gather should overlap the waits"
    print(f"sequential : 5 tasks of 0.05s in {sequential_time:.3f}s")
    print(f"concurrent : 5 tasks of 0.05s in {concurrent_time:.3f}s")

    # ---- the order results arrive in ----------------------------------------
    finished: list[str] = []
    slow = asyncio.create_task(work("slow", 0.05))
    fast = asyncio.create_task(work("fast", 0.01))
    for completed in asyncio.as_completed([slow, fast]):
        finished.append((await completed).split()[0])
    assert finished == ["fast", "slow"], f"the faster task should finish first: {finished}"
    print(f"completion : {finished}")

    # ---- a semaphore caps how many run at once ------------------------------
    semaphore = asyncio.Semaphore(2)
    state = {"active": 0, "peak": 0}

    async def limited(name: str) -> None:
        async with semaphore:
            state["active"] += 1
            state["peak"] = max(state["peak"], state["active"])
            await asyncio.sleep(0.02)
            state["active"] -= 1

    await asyncio.gather(*(limited(f"job{i}") for i in range(8)))
    assert state["peak"] == 2, f"two should run at a time, saw {state['peak']}"
    assert state["active"] == 0, "and all of them should have been released"
    print(f"semaphore  : peak concurrency {state['peak']} across 8 jobs")

    # ---- timeouts ------------------------------------------------------------
    try:
        await asyncio.wait_for(work("patient", 0.5), timeout=0.05)
        raise AssertionError("wait_for should have timed out")
    except asyncio.TimeoutError:
        print("wait_for   : gave up on a 0.5s task after 0.05s")

    try:
        async with asyncio.timeout(0.05):
            await work("also patient", 0.5)
        raise AssertionError("the timeout block should have raised")
    except TimeoutError:
        print("timeout    : the asyncio.timeout block raised TimeoutError")

    assert await asyncio.wait_for(work("quick", 0.01), timeout=1.0) == "quick finished after 0.01s"
    print("wait_for   : a fast task under a generous timeout returns normally")

    # ---- a failing task cancels its siblings --------------------------------
    cancelled = {"survivor": False}

    async def survivor() -> None:
        try:
            await asyncio.sleep(1.0)
        except asyncio.CancelledError:
            cancelled["survivor"] = True
            raise

    async def boom() -> None:
        await asyncio.sleep(0.01)
        raise ValueError("boom")

    try:
        async with asyncio.TaskGroup() as group:
            group.create_task(survivor())
            group.create_task(boom())
        raise AssertionError("the task group should have raised")
    except* ValueError as group_error:
        assert len(group_error.exceptions) == 1, "one task failed"
        assert cancelled["survivor"], "the sibling should have been cancelled"
        print("task group : a failure cancelled the sibling and raised ValueError")

    # many failures arrive together, wrapped in one group
    try:
        async with asyncio.TaskGroup() as group:
            for index in range(3):
                group.create_task(boom())
        raise AssertionError("the group should have raised")
    except* ValueError as group_error:
        assert len(group_error.exceptions) == 3, "all three failures were collected"
        print(f"task group : collected {len(group_error.exceptions)} failures at once")

    # ---- gather and failures ------------------------------------------------
    results = await asyncio.gather(
        work("ok", 0.01), work("bad", 0.01, fail=True), return_exceptions=True
    )
    assert isinstance(results[0], str), "the good result came back"
    assert isinstance(results[1], ValueError), "and the failure was handed over, not raised"
    print("gather     : return_exceptions=True handed back the ValueError")

    try:
        await asyncio.gather(work("ok", 0.01), work("bad", 0.01, fail=True))
        raise AssertionError("gather should propagate the first failure by default")
    except ValueError as error:
        assert "bad failed" in str(error)
        print(f"gather     : by default it raises, here {error}")

    # ---- a bounded queue as back pressure -----------------------------------
    queue: asyncio.Queue[int] = asyncio.Queue(maxsize=5)
    consumed: list[int] = []

    async def producer() -> None:
        for value in range(20):
            await queue.put(value)
        await queue.put(-1)          # a poison pill ends the consumer

    async def consumer() -> None:
        while True:
            value = await queue.get()
            if value < 0:
                break
            consumed.append(value)

    async with asyncio.TaskGroup() as group:
        group.create_task(producer())
        group.create_task(consumer())

    assert consumed == list(range(20)), "the queue preserves order"
    assert queue.empty(), "and it is drained"
    assert queue.maxsize == 5, "the queue was bounded, so the producer had to wait"
    print(f"queue      : {len(consumed)} items in order through a queue of 5")

    # ---- cancellation ------------------------------------------------------
    task = asyncio.create_task(work("victim", 5.0))
    await asyncio.sleep(0.01)
    assert not task.done(), "the task is still running"
    task.cancel()
    try:
        await task
        raise AssertionError("the cancelled task should raise CancelledError")
    except asyncio.CancelledError:
        assert task.cancelled(), "and the task reports itself as cancelled"
        print("cancel     : a running task was cancelled and cleaned up")

    # a cancelled task's finally block still runs
    cleaned = {"done": False}

    async def tidy() -> None:
        try:
            await asyncio.sleep(5.0)
        finally:
            cleaned["done"] = True

    tidy_task = asyncio.create_task(tidy())
    await asyncio.sleep(0.01)
    tidy_task.cancel()
    try:
        await tidy_task
    except asyncio.CancelledError:
        pass
    assert cleaned["done"], "the finally block ran despite the cancellation"
    print("cancel     : the cancelled task's finally block still ran")

    # ---- sleeping zero yields to the loop -----------------------------------
    order: list[str] = []

    async def ticker(name: str) -> None:
        for _ in range(3):
            order.append(name)
            await asyncio.sleep(0)       # a chance for the other task to run

    await asyncio.gather(ticker("a"), ticker("b"))
    assert order == ["a", "b", "a", "b", "a", "b"], f"the tasks interleaved: {order}"
    print(f"interleave : {order}")

    print("All checks passed.")


if __name__ == "__main__":
    asyncio.run(main())
