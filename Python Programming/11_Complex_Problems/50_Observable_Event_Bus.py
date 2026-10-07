"""
Program 50: An Observable Event Bus

Publishes events to subscribers by topic, with a topic hierarchy, priorities,
one-shot subscriptions, error isolation, and subscriptions that do not keep
their owner alive.

The weak reference part is the interesting one. A bus that stores callables
directly keeps their owners alive forever, which is how event buses leak. Here
a subscription may hold an instance weakly plus the name of the method to call,
and the difference is shown by watching a subscriber disappear after garbage
collection while the strong one does not.

Concepts: weakref, callback ordering, error isolation, topic hierarchies,
           a context manager that collects events for inspection.

    python 50_Observable_Event_Bus.py
"""

from __future__ import annotations

from collections.abc import Callable, Iterator
from contextlib import contextmanager
from dataclasses import dataclass, field
import gc
import itertools
import weakref
from typing import Any


@dataclass(frozen=True)
class Event:
    topic: str
    payload: Any
    sequence: int

    def __str__(self) -> str:
        return f"{self.topic}={self.payload!r}"


class Subscription:
    """One subscriber's interest in a topic.

    Either a handler is held directly, or an instance is held weakly together
    with a method name. The second form is what stops the bus from owning its
    subscribers.
    """

    def __init__(
        self,
        topic: str,
        *,
        handler: Callable[[Event], Any] | None = None,
        owner: weakref.ref | None = None,
        method: str | None = None,
        priority: int = 0,
        once: bool = False,
        order: int = 0,
    ) -> None:
        if (handler is None) == (owner is None):
            raise ValueError("a subscription needs either a handler or an owner and method")
        if owner is not None and not method:
            raise ValueError("a weak subscription needs a method name")
        self.topic = topic
        self.handler = handler
        self.owner = owner
        self.method = method
        self.priority = priority
        self.once = once
        self.order = order

    def resolve(self) -> Callable[[Event], Any] | None:
        """The callable to run, or None if the owner has been collected."""
        if self.handler is not None:
            return self.handler
        instance = self.owner() if self.owner is not None else None
        if instance is None:
            return None
        return getattr(instance, self.method)

    def matches(self, topic: str) -> bool:
        """A subscriber to 'orders' also hears 'orders.created'."""
        return topic == self.topic or topic.startswith(self.topic + ".")

    def __repr__(self) -> str:
        who = "handler" if self.handler is not None else f"weak method {self.method}"
        return f"Subscription({self.topic!r}, {who}, priority={self.priority})"


class EventBus:
    def __init__(self) -> None:
        self._subscriptions: list[Subscription] = []
        self._order = itertools.count()
        self.published = 0
        self.delivered = 0
        self.failures: list[tuple[str, str]] = []
        self.expired = 0

    # ---- subscribing --------------------------------------------------------
    def subscribe(self, topic: str, handler: Callable[[Event], Any], *,
                  priority: int = 0, once: bool = False) -> Subscription:
        """Hold the handler directly. Whoever the handler belongs to stays alive."""
        return self._add(Subscription(topic, handler=handler, priority=priority, once=once,
                                      order=next(self._order)))

    def subscribe_method(self, topic: str, instance: object, method: str, *,
                         priority: int = 0, once: bool = False) -> Subscription:
        """Hold the instance weakly, so the bus does not own it."""
        if not callable(getattr(instance, method, None)):
            raise ValueError(f"{type(instance).__name__} has no callable {method!r}")
        return self._add(Subscription(topic, owner=weakref.ref(instance), method=method,
                                      priority=priority, once=once, order=next(self._order)))

    def _add(self, subscription: Subscription) -> Subscription:
        self._subscriptions.append(subscription)
        return subscription

    def unsubscribe(self, subscription: Subscription) -> bool:
        """True if it was there to remove. Removing twice is harmless."""
        try:
            self._subscriptions.remove(subscription)
        except ValueError:
            return False
        return True

    def prune(self) -> int:
        """Forget subscriptions whose owner has been collected."""
        before = len(self._subscriptions)
        self._subscriptions = [item for item in self._subscriptions
                               if item.handler is not None or item.resolve() is not None]
        removed = before - len(self._subscriptions)
        self.expired += removed
        return removed

    # ---- publishing ---------------------------------------------------------
    def publish(self, topic: str, payload: Any = None) -> int:
        """Run every interested subscriber, oldest first within a priority."""
        event = Event(topic, payload, next(self._order))
        self.published += 1

        interested = [item for item in self._subscriptions if item.matches(topic)]
        # Highest priority first; ties keep the order they subscribed in.
        interested.sort(key=lambda item: (-item.priority, item.order))

        ran = 0
        for subscription in interested:
            handler = subscription.resolve()
            if handler is None:
                self.expired += 1
                self._subscriptions.remove(subscription)
                continue
            if subscription.once:
                # Remove before running, so an exception cannot leave it behind.
                self._subscriptions.remove(subscription)
            try:
                handler(event)
            except Exception as error:                       # noqa: BLE001 - isolation is the point
                self.failures.append((topic, f"{type(error).__name__}: {error}"))
                continue
            ran += 1
            self.delivered += 1
        return ran

    # ---- looking at it ------------------------------------------------------
    def subscriber_count(self, topic: str | None = None) -> int:
        if topic is None:
            return len(self._subscriptions)
        return sum(1 for item in self._subscriptions if item.matches(topic))

    def topics(self) -> list[str]:
        return sorted({item.topic for item in self._subscriptions})

    @contextmanager
    def collect(self, topic: str) -> Iterator[list[Event]]:
        """Record every event on a topic for the length of a with block."""
        seen: list[Event] = []
        subscription = self.subscribe(topic, seen.append)
        try:
            yield seen
        finally:
            self.unsubscribe(subscription)


def main() -> None:
    # ---- the basics ---------------------------------------------------------
    bus = EventBus()
    seen: list[str] = []
    bus.subscribe("orders", lambda event: seen.append(f"first:{event.payload}"))
    bus.subscribe("orders", lambda event: seen.append(f"second:{event.payload}"))
    assert bus.publish("orders", 1) == 2, "both subscribers ran"
    assert seen == ["first:1", "second:1"], f"in subscription order: {seen}"

    seen.clear()
    bus.subscribe("orders", lambda event: seen.append("high"), priority=10)
    bus.publish("orders", 2)
    assert seen == ["high", "first:2", "second:2"], f"priority wins, ties keep order: {seen}"
    print(f"ordering     : {seen}")

    # ---- a subscriber to a parent topic hears about its children -------------
    seen.clear()
    assert bus.publish("orders.created", 3) == 3, "the 'orders' subscribers heard 'orders.created'"
    assert seen == ["high", "first:3", "second:3"], seen
    assert bus.publish("orders.created.retry", 4) == 3, "and grandchildren"
    assert bus.publish("users", 5) == 0, "but an unrelated topic reaches nobody"
    seen.clear()
    assert bus.publish("ordersX", 6) == 0, "and neither does a topic that merely starts the same"
    print("hierarchy    : 'orders' hears 'orders.created' but not 'ordersX'")

    # ---- one-shot subscriptions ---------------------------------------------
    bus = EventBus()
    fired: list[int] = []
    bus.subscribe("tick", lambda event: fired.append(event.payload), once=True)
    bus.subscribe("tick", lambda event: fired.append(-1))
    bus.publish("tick", 1)
    bus.publish("tick", 2)
    assert fired == [1, -1, -1], f"the one-shot fired once, the other twice: {fired}"
    assert bus.subscriber_count("tick") == 1, "and it removed itself"
    print(f"one shot     : {fired}")

    # a one-shot is removed even when it raises
    bus = EventBus()

    def explode_once(event: Event) -> None:
        raise ValueError("boom")

    bus.subscribe("tick", explode_once, once=True)
    bus.publish("tick", 1)
    assert bus.subscriber_count("tick") == 0, "it does not come back"
    assert len(bus.failures) == 1, "and the failure was recorded"
    print("one shot     : removed even though it raised")

    # ---- unsubscribing ------------------------------------------------------
    bus = EventBus()
    calls: list[int] = []
    subscription = bus.subscribe("tick", lambda event: calls.append(event.payload))
    assert bus.publish("tick", 1) == 1
    assert bus.unsubscribe(subscription), "it was there to remove"
    assert bus.publish("tick", 2) == 0, "and now nobody is listening"
    assert not bus.unsubscribe(subscription), "removing it twice reports that it was already gone"
    assert calls == [1], calls
    print("unsubscribe  : stops delivery, and doing it twice is harmless")

    # ---- a failing subscriber does not stop the others ----------------------
    bus = EventBus()
    survived: list[str] = []

    def explode(event: Event) -> None:
        raise RuntimeError("this subscriber is broken")

    bus.subscribe("job", explode, priority=100)
    bus.subscribe("job", lambda event: survived.append("after"))
    assert bus.publish("job", "payload") == 1, "only the working subscriber counts as delivered"
    assert survived == ["after"], "the later subscriber still ran"
    assert bus.failures == [("job", "RuntimeError: this subscriber is broken")], bus.failures
    assert bus.delivered == 1 and bus.published == 1, (bus.delivered, bus.published)
    print(f"isolation    : {bus.failures[0][1]} was contained")

    # ---- weak subscriptions do not own their subscriber ---------------------
    class Widget:
        def __init__(self, name: str) -> None:
            self.name = name
            self.seen: list[Any] = []

        def on_event(self, event: Event) -> None:
            self.seen.append(event.payload)

    bus = EventBus()

    def strong_subscriber() -> weakref.ref:
        widget = Widget("strong")
        bus.subscribe("ui", widget.on_event)           # a bound method holds the widget
        return weakref.ref(widget)

    def weak_subscriber() -> weakref.ref:
        widget = Widget("weak")
        bus.subscribe_method("ui", widget, "on_event")  # the bus holds only a weak reference
        return weakref.ref(widget)

    strong = strong_subscriber()
    weak = weak_subscriber()
    gc.collect()
    assert strong() is not None, "the bus is holding the strong subscriber alive"
    assert weak() is None, "but not the weak one"
    print("lifetime     : the bound method kept its widget alive, the weak one did not")

    assert bus.subscriber_count("ui") == 2, "both subscriptions are still registered"
    assert bus.publish("ui", "hello") == 1, "but only the live one is delivered to"
    assert len(bus.failures) == 0, "an expired subscription is not an error"
    assert bus.subscriber_count("ui") == 1, "publishing noticed the dead one and dropped it"
    assert bus.expired == 1, "and counted it"
    print(f"lifetime     : after publishing, {bus.subscriber_count('ui')} subscription remains")

    # prune does the same thing without publishing
    weak = weak_subscriber()
    gc.collect()
    assert bus.subscriber_count("ui") == 2, "the new weak subscription is registered"
    assert bus.prune() == 1, "prune removed the expired one"
    assert bus.subscriber_count("ui") == 1
    print("prune        : drops expired subscriptions without waiting for a publish")

    # a weak subscription needs a real method
    try:
        bus.subscribe_method("ui", Widget("w"), "no_such_method")
        raise AssertionError("that method does not exist")
    except ValueError as error:
        print(f"validation   : {error}")
    try:
        Subscription("topic")
        raise AssertionError("a subscription needs a handler or an owner")
    except ValueError as error:
        print(f"validation   : {error}")

    # ---- collecting events for inspection -----------------------------------
    bus = EventBus()
    with bus.collect("audit") as recorded:
        bus.publish("audit", "one")
        bus.publish("audit", "two")
        bus.publish("other", "ignored")
        assert [event.payload for event in recorded] == ["one", "two"], recorded
        assert all(event.topic == "audit" for event in recorded)
    bus.publish("audit", "after the block")
    assert len(recorded) == 2, "the recorder was unsubscribed when the block ended"
    assert [event.sequence for event in recorded] == sorted(event.sequence for event in recorded), \
        "sequences increase"
    print(f"collecting   : {[str(event) for event in recorded]}")

    # ---- a worked example ---------------------------------------------------
    bus = EventBus()
    log: list[str] = []

    class Recorder:
        def note(self, event: Event) -> None:
            log.append(f"recorded {event.payload}")

    def reserve_stock(event: Event) -> None:
        log.append(f"reserve stock for {event.payload}")

    def out_of_stock(event: Event) -> None:
        raise KeyError("no stock")

    recorder = Recorder()
    bus.subscribe("orders", lambda event: log.append(f"email about {event.payload}"), priority=-1)
    bus.subscribe("orders.created", reserve_stock, priority=5)
    bus.subscribe("orders.created", out_of_stock, priority=2)
    bus.subscribe_method("orders", recorder, "note")

    delivered = bus.publish("orders.created", "order-1")
    assert delivered == 3, f"three of the four handlers ran, {delivered} did"
    assert log == ["reserve stock for order-1", "recorded order-1", "email about order-1"], log
    assert len(bus.failures) == 1 and "no stock" in bus.failures[0][1], bus.failures
    assert bus.topics() == ["orders", "orders.created"], bus.topics()
    print(f"example      : {log}")
    print(f"metrics      : published {bus.published}, delivered {bus.delivered}, "
          f"failed {len(bus.failures)}, expired {bus.expired}")
    print("All checks passed.")


if __name__ == "__main__":
    main()
