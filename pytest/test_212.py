# test_212.py
# Writing the tests once and running them against several implementations, by
# subclassing a base class and overriding the fixture it depends on.
#
#   pytest test_212.py -v
#
# The base class below is named BaseCacheTests rather than TestCacheTests, and
# that is the whole trick. pytest collects classes whose name starts with Test,
# so BaseCacheTests is only ever a source of inherited test methods. Each
# subclass that does start with Test supplies its own implementation through the
# fixture, and pytest runs every inherited test against it. Adding a third
# implementation means adding a class with one fixture in it.
#
# The base class is a plain class, not a subclass of anything from pytest, so
# this is just Python inheritance being used to share test bodies.

import pytest


# --------------------------------------------------------- the implementations
class PlainCache:
    """An unbounded dictionary."""

    def __init__(self) -> None:
        self._items: dict[str, object] = {}

    def put(self, key: str, value: object) -> None:
        self._items[key] = value

    def get(self, key: str, default: object = None) -> object:
        return self._items.get(key, default)

    def __len__(self) -> int:
        return len(self._items)

    def describe(self) -> str:
        return "plain"


class BoundedCache(PlainCache):
    """Keeps only the most recent few keys, dropping the oldest first."""

    def __init__(self, limit: int = 3) -> None:
        super().__init__()
        self.limit = limit
        self._order: list[str] = []

    def put(self, key: str, value: object) -> None:
        if key not in self._items and len(self._order) >= self.limit:
            oldest = self._order.pop(0)
            del self._items[oldest]
        if key not in self._order:
            self._order.append(key)
        super().put(key, value)

    def describe(self) -> str:
        return f"bounded({self.limit})"


# ----------------------------------------------------- tests written once
class BaseCacheTests:
    """Not collected: the name does not start with Test.

    Every test here needs a `cache`, and each subclass decides what that is.
    """

    @pytest.fixture
    def cache(self) -> PlainCache:
        raise NotImplementedError("a subclass has to say what cache to use")

    def test_a_new_cache_is_empty(self, cache):
        assert len(cache) == 0

    def test_a_value_can_be_read_back(self, cache):
        cache.put("answer", 42)
        assert cache.get("answer") == 42

    def test_a_missing_key_gives_the_default(self, cache):
        assert cache.get("absent") is None
        assert cache.get("absent", "fallback") == "fallback"

    def test_storing_a_key_twice_keeps_the_latest(self, cache):
        cache.put("key", "first")
        cache.put("key", "second")
        assert cache.get("key") == "second"
        assert len(cache) == 1, "and it is still one key"

    def test_it_can_hold_several_keys(self, cache):
        for index in range(3):
            cache.put(f"key-{index}", index)
        assert len(cache) == 3
        assert cache.get("key-2") == 2

    @pytest.mark.parametrize("value", [0, "", None, [], {"a": 1}])
    def test_any_value_survives_a_round_trip(self, cache, value):
        # An inherited parametrized test runs once per value, per subclass.
        cache.put("thing", value)
        assert cache.get("thing") == value

    def test_the_subclass_says_which_cache_it_is(self, cache):
        # Overridden by one of the subclasses below.
        assert cache.describe() == "plain"


class TestPlainCache(BaseCacheTests):
    @pytest.fixture
    def cache(self):
        return PlainCache()

    def test_it_grows_without_limit(self, cache):
        for index in range(10):
            cache.put(f"key-{index}", index)
        assert len(cache) == 10, "the plain cache drops nothing"
        assert cache.get("key-0") == 0, "so the first key is still there"


class TestBoundedCache(BaseCacheTests):
    @pytest.fixture
    def cache(self):
        return BoundedCache(limit=3)

    def test_it_evicts_the_oldest(self, cache):
        for key in "abcd":
            cache.put(key, key)
        assert len(cache) == 3, "the limit is respected"
        assert cache.get("a") is None, "and the oldest key went first"
        assert cache.get("d") == "d", "while the newest is there"

    def test_eviction_order_protects_what_fits(self, cache):
        cache.put("a", 1)
        cache.put("b", 2)
        cache.put("c", 3)
        assert set("abc") <= {"a", "b", "c"}
        assert cache.get("a") == 1, "the first three all fit exactly"

    def test_rewriting_a_key_does_not_evict(self, cache):
        cache.put("a", 1)
        cache.put("a", 2)
        cache.put("b", 3)
        cache.put("c", 4)
        assert len(cache) == 3, "updating an existing key is not a new one"

    def test_the_subclass_says_which_cache_it_is(self, cache):
        # This overrides the base version, which expected "plain".
        assert cache.describe() == "bounded(3)"


def test_the_base_class_itself_was_not_collected(request):
    # The name is the reason: pytest only collects classes starting with Test.
    nodeids = {item.nodeid for item in request.session.items}
    assert not any("BaseCacheTests" in nodeid for nodeid in nodeids), sorted(nodeids)
    assert any("TestPlainCache" in nodeid for nodeid in nodeids), "the subclasses are collected"


def test_both_implementations_are_collected(request):
    # A node id carries the class, so it is the place to look: item.name is only
    # the test function's own name.
    nodeids = {item.nodeid for item in request.session.items}
    assert any("TestPlainCache" in nodeid for nodeid in nodeids), sorted(nodeids)
    assert any("TestBoundedCache" in nodeid for nodeid in nodeids), sorted(nodeids)


def test_there_are_two_classes_worth_of_inherited_tests(request):
    inheritance = [
        item.nodeid for item in request.session.items
        if item.name == "test_a_new_cache_is_empty" and "test_212" in str(item.fspath)
    ]
    assert len(inheritance) == 2, inheritance
    assert len(set(inheritance)) == 2, "with distinct node ids"


def test_the_two_implementations_really_do_differ():
    # Fetching each by hand, to make the point that the inherited tests pass
    # against both, and the extra tests are where the difference shows.
    plain = PlainCache()
    bounded = BoundedCache(limit=3)
    for key in "abcd":
        plain.put(key, key)
        bounded.put(key, key)

    assert len(plain) == 4, "the plain cache kept everything"
    assert len(bounded) == 3, "the bounded one did not"
    assert plain.get("a") == "a" and bounded.get("a") is None, "and that is the only difference here"
