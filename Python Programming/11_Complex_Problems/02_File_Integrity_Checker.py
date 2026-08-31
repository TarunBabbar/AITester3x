"""
Program 2: File Integrity Checker

Hashes every file in a folder into a manifest, then verifies later that no
file was changed, added, or removed. Useful for detecting tampering or
accidental edits in a directory of important files.

Concepts: hashlib, pathlib, JSON persistence, comparing file state over time.
"""

import hashlib
import json
from pathlib import Path

MANIFEST = Path(__file__).with_name("integrity_manifest.json")


def file_hash(path: Path) -> str:
    """SHA-256 of a file, streamed in chunks so big files are fine."""
    digest = hashlib.sha256()
    with open(path, "rb") as fh:
        for chunk in iter(lambda: fh.read(65536), b""):
            digest.update(chunk)
    return digest.hexdigest()


def build_manifest(folder: Path) -> dict:
    """Hash every file under folder, keyed by relative path."""
    manifest = {}
    for path in sorted(folder.rglob("*")):
        if path.is_file() and path != MANIFEST:
            manifest[str(path.relative_to(folder))] = file_hash(path)
    return manifest


def save_manifest(manifest: dict, path: Path = MANIFEST) -> None:
    path.write_text(json.dumps(manifest, indent=2), encoding="utf-8")
    print(f"Saved {len(manifest)} file hashes to {path.name}")


def load_manifest(path: Path = MANIFEST) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def verify(folder: Path, manifest: dict) -> bool:
    """Compare current files against the manifest; report differences."""
    current = build_manifest(folder)
    ok = True

    for rel_path, old_hash in manifest.items():
        if rel_path not in current:
            print(f"MISSING  {rel_path}")
            ok = False
        elif current[rel_path] != old_hash:
            print(f"MODIFIED {rel_path}")
            ok = False

    for rel_path in current:
        if rel_path not in manifest:
            print(f"ADDED    {rel_path}")
            ok = False

    return ok


def demo() -> None:
    folder = Path(__file__).parent / "demo_files"
    folder.mkdir(exist_ok=True)
    (folder / "readme.txt").write_text("important file one\n", encoding="utf-8")
    (folder / "config.json").write_text('{"key": "value"}\n', encoding="utf-8")

    manifest = build_manifest(folder)
    save_manifest(manifest)

    print("\nFirst verification (nothing changed):")
    print("  All good!" if verify(folder, manifest) else "  PROBLEMS FOUND")

    # Simulate tampering: edit a file and add a new one.
    (folder / "readme.txt").write_text("important file one -- EDITED\n", encoding="utf-8")
    (folder / "extra.txt").write_text("sneaky new file\n", encoding="utf-8")

    print("\nAfter tampering:")
    print("  All good!" if verify(folder, manifest) else "  PROBLEMS FOUND")


if __name__ == "__main__":
    demo()
