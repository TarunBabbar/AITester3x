"""
Program 47: Dynamic Imports and a Plugin Registry

Builds a small plugin package on disk, then discovers it at runtime: modules are
imported by name, inspected for the plugin object they expose, and registered if
they match the protocol. Nothing is known at import time except the package name.

The working directory is temporary, and sys.path and sys.modules are restored
afterwards, so the demo leaves nothing behind.

Concepts: importlib, pkgutil, sys.path, Protocol, reload, error containment.

    python 47_Dynamic_Imports_Plugins.py
"""

from __future__ import annotations

import importlib
import importlib.util
import pkgutil
import shutil
import sys
import tempfile
from pathlib import Path
from typing import Protocol, runtime_checkable


@runtime_checkable
class TextPlugin(Protocol):
    """A plugin transforms text. Any object with the right shape qualifies."""

    name: str

    def transform(self, text: str) -> str: ...


PACKAGE = "demo_plugins"

UPPERCASE = '''
class Uppercase:
    name = "uppercase"

    def transform(self, text):
        return text.upper()


PLUGIN = Uppercase()
'''

REVERSE = '''
class Reverse:
    name = "reverse"

    def transform(self, text):
        return text[::-1]


PLUGIN = Reverse()
'''

# Exposes no PLUGIN at all, so discovery should pass it over.
NOT_A_PLUGIN = '''
HELPER_VALUE = 1

def helper():
    return HELPER_VALUE
'''

# Raises on import, so discovery has to report it rather than crash.
BROKEN = '''
raise RuntimeError("this plugin is broken on import")
'''


def build_package(root: Path, *, include_broken: bool = False) -> Path:
    """Write a plugin package somewhere temporary."""
    package = root / PACKAGE
    package.mkdir(exist_ok=True)
    (package / "__init__.py").write_text("", encoding="utf-8")
    (package / "uppercase.py").write_text(UPPERCASE, encoding="utf-8")
    (package / "reverse.py").write_text(REVERSE, encoding="utf-8")
    (package / "not_a_plugin.py").write_text(NOT_A_PLUGIN, encoding="utf-8")
    if include_broken:
        (package / "broken.py").write_text(BROKEN, encoding="utf-8")
    return package


def unload(prefix: str) -> None:
    """Forget every module whose name starts with the prefix."""
    for name in list(sys.modules):
        if name == prefix or name.startswith(f"{prefix}."):
            del sys.modules[name]


def discover(package_name: str) -> tuple[dict[str, TextPlugin], list[tuple[str, str]]]:
    """Import every module in the package and collect the plugins it exposes.

    Returns the plugins found and the failures, so one bad module cannot take
    the whole discovery down with it.
    """
    package = importlib.import_module(package_name)
    if not hasattr(package, "__path__"):
        raise ValueError(f"{package_name} is a module, not a package")

    plugins: dict[str, TextPlugin] = {}
    failures: list[tuple[str, str]] = []

    for module_info in pkgutil.iter_modules(package.__path__):
        full_name = f"{package_name}.{module_info.name}"
        try:
            module = importlib.import_module(full_name)
        except Exception as error:                      # noqa: BLE001 - that is the point
            failures.append((module_info.name, f"{type(error).__name__}: {error}"))
            continue
        candidate = getattr(module, "PLUGIN", None)
        if isinstance(candidate, TextPlugin):
            plugins[module_info.name] = candidate
    return plugins, failures


def main() -> None:
    # ---- the protocol decides, not the class hierarchy ----------------------
    class Shout:
        name = "shout"

        def transform(self, text: str) -> str:
            return text.upper() + "!"

    assert isinstance(Shout(), TextPlugin), "a plain class satisfies the protocol"
    assert not isinstance("a string", TextPlugin), "a string does not"
    print("protocol     : a plain class satisfies TextPlugin without inheriting it")

    root = Path(tempfile.mkdtemp(prefix="plugins-"))
    original_path = list(sys.path)
    try:
        build_package(root)
        sys.path.insert(0, str(root))
        importlib.invalidate_caches()          # the directory is new to Python

        # ---- discovery ------------------------------------------------------
        plugins, failures = discover(PACKAGE)
        assert failures == [], f"nothing should have failed: {failures}"
        assert set(plugins) == {"uppercase", "reverse"}, f"unexpected plugins {sorted(plugins)}"
        assert "not_a_plugin" not in plugins, "a module without PLUGIN is skipped"
        print(f"discovered   : {sorted(plugins)}")

        # ---- the plugins work ------------------------------------------------
        assert plugins["uppercase"].transform("hello") == "HELLO"
        assert plugins["reverse"].transform("hello") == "olleh"
        assert plugins["uppercase"].name == "uppercase"
        print(f"uppercase    : {plugins['uppercase'].transform('hello')}")
        print(f"reverse      : {plugins['reverse'].transform('hello')}")

        # ---- an unknown module -----------------------------------------------
        assert importlib.util.find_spec(f"{PACKAGE}.missing") is None, "no spec for a module that is absent"
        assert importlib.util.find_spec(f"{PACKAGE}.uppercase") is not None, "but a real one has a spec"
        print("find_spec    : returns None for a module that does not exist")

        # ---- importing by name ------------------------------------------------
        imported = importlib.import_module(f"{PACKAGE}.uppercase")
        assert imported.PLUGIN is plugins["uppercase"], "the cache hands back the same plugin object"
        assert sys.modules[f"{PACKAGE}.uppercase"] is imported, "and it is the cached one"
        print("import_module: importing the same module twice returns one object")

        # ---- a reload re-runs the module -------------------------------------
        before = id(imported.PLUGIN)
        reloaded = importlib.reload(imported)
        assert reloaded is imported, "reload returns the same module object"
        assert id(reloaded.PLUGIN) != before, "but the plugin object inside is rebuilt"
        print("reload       : the module is re-executed in place")

        # ---- a plugin added after the first sweep ----------------------------
        extra = root / PACKAGE / "shout.py"
        extra.write_text(
            "class Shout:\n"
            "    name = 'shout'\n"
            "    def transform(self, text):\n"
            "        return text.upper() + '!'\n"
            "\n"
            "PLUGIN = Shout()\n",
            encoding="utf-8",
        )
        importlib.invalidate_caches()
        sys.path.insert(0, str(root))          # already there, but harmless
        more, more_failures = discover(PACKAGE)
        assert more_failures == [], "still nothing broken"
        assert set(more) == {"uppercase", "reverse", "shout"}, f"the new plugin appeared: {sorted(more)}"
        assert more["shout"].transform("hi") == "HI!"
        print(f"late arrival : {sorted(more)}")

        # ---- one broken module does not stop the rest ------------------------
        unload(PACKAGE)
        build_package(root, include_broken=True)
        importlib.invalidate_caches()
        surviving, broken = discover(PACKAGE)
        assert set(surviving) == {"uppercase", "reverse", "shout"}, "the good ones still loaded"
        assert len(broken) == 1, f"exactly one failure was reported: {broken}"
        name, message = broken[0]
        assert name == "broken", "and it names the module"
        assert "RuntimeError" in message and "broken on import" in message, "with the real error"
        print(f"contained    : {name} failed with {message}")

        # importing it directly still raises, of course
        try:
            importlib.import_module(f"{PACKAGE}.broken")
            raise AssertionError("importing the broken module should raise")
        except RuntimeError as error:
            assert "broken on import" in str(error)
            print("direct import: still raises, as it should")

        # ---- a module is not a package ---------------------------------------
        try:
            discover(f"{PACKAGE}.uppercase")
            raise AssertionError("a plain module has no plugins to discover")
        except ValueError as error:
            print(f"not a package: {error}")
    finally:
        sys.path[:] = original_path
        unload(PACKAGE)
        shutil.rmtree(root, ignore_errors=True)

    # ---- and the cleanup really happened -------------------------------------
    assert sys.path == original_path, "sys.path was restored"
    assert not any(name.startswith(PACKAGE) for name in sys.modules), "sys.modules was cleaned"
    assert not root.exists(), "the temporary package is gone"
    print("cleanup      : sys.path, sys.modules and the temporary directory are restored")
    print("All checks passed.")


if __name__ == "__main__":
    main()
