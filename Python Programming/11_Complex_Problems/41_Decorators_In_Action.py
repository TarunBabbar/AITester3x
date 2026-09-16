"""
Program 41: Decorators in Action

Builds four decorators from scratch - call counting, timing, retrying on
failure, and memoizing - plus one that takes its own arguments. Shows why
functools.wraps matters for keeping the wrapped function's identity.

Concepts: closures, *args/**kwargs, higher-order functions, functools.wraps.
"""

import functools
import time


def count_calls(func):
    """Track how many times the decorated function runs."""

    @functools.wraps(func)
    def wrapper(*args, **kwargs):
        wrapper.calls += 1
        return func(*args, **kwargs)

    wrapper.calls = 0
    return wrapper


def timed(func):
    """Record how long the last call took, in seconds."""

    @functools.wraps(func)
    def wrapper(*args, **kwargs):
        start = time.perf_counter()
        try:
            return func(*args, **kwargs)
        finally:
            wrapper.last_seconds = time.perf_counter() - start

    wrapper.last_seconds = 0.0
    return wrapper


def retry(times: int = 3):
    """Decorator WITH arguments: retry on ValueError up to `times` attempts."""

    def decorator(func):
        @functools.wraps(func)
        def wrapper(*args, **kwargs):
            last_error: Exception | None = None
            for attempt in range(1, times + 1):
                wrapper.attempts = attempt
                try:
                    return func(*args, **kwargs)
                except ValueError as error:
                    last_error = error
            raise last_error

        wrapper.attempts = 0
        return wrapper

    return decorator


def memoize(func):
    """Cache results so repeated calls with the same arguments are free."""
    cache: dict = {}

    @functools.wraps(func)
    def wrapper(*args):
        if args in cache:
            wrapper.cache_hits += 1
        else:
            cache[args] = func(*args)
            wrapper.cache_misses += 1
        return cache[args]

    wrapper.cache_hits = 0
    wrapper.cache_misses = 0
    wrapper.cache = cache
    return wrapper


@count_calls
def greet(name: str) -> str:
    """Return a greeting."""
    return f"Hello, {name}!"


@memoize
def expensive_square(number: int) -> int:
    return number * number


@retry(times=3)
def flaky_service() -> str:
    flaky_service.tries += 1
    if flaky_service.tries < 3:
        raise ValueError("temporary failure")
    return "success"


flaky_service.tries = 0


@retry(times=2)
def always_failing() -> str:
    always_failing.tries += 1
    raise ValueError("permanent failure")


always_failing.tries = 0


def main() -> None:
    for name in ("Alice", "Bob", "Alice"):
        greet(name)
    print(f"greet called {greet.calls} times (name preserved: {greet.__name__!r})")
    assert greet.calls == 3
    assert greet.__name__ == "greet", "functools.wraps keeps the original name"

    print(f"Square of 4 -> {expensive_square(4)}")
    print(f"Square of 4 -> {expensive_square(4)}")
    print(f"Square of 5 -> {expensive_square(5)}")
    print(f"cache hits={expensive_square.cache_hits}, misses={expensive_square.cache_misses}")
    assert expensive_square.cache_misses == 2
    assert expensive_square.cache_hits == 1

    result = flaky_service()
    print(f"\nRetrying flaky service -> {result!r} on attempt {flaky_service.attempts}")
    assert result == "success"
    assert flaky_service.attempts == 3, "it failed twice before succeeding on attempt 3"
    assert flaky_service.tries == 3

    try:
        always_failing()
    except ValueError as error:
        print(f"Always failing gives up after {always_failing.attempts} attempts: {error}")
    assert always_failing.attempts == 2, "retry(times=2) means at most two attempts"


if __name__ == "__main__":
    main()
