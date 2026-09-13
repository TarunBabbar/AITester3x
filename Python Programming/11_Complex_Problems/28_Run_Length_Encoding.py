"""
Program 28: Run-Length Encoding (RLE)

Compresses text by replacing runs of repeated characters with a count and the
character, then decompresses it back. Also measures the compression ratio and
verifies the round trip is lossless.

Concepts: strings, loops, itertools.groupby, encode/decode symmetry, asserts.
"""

from itertools import groupby


def encode(text: str) -> str:
    """Compress text: 'aaabb' -> '3a2b'."""
    parts: list[str] = []
    for char, group in groupby(text):
        count = sum(1 for _ in group)
        parts.append(f"{count}{char}")
    return "".join(parts)


def decode(encoded: str) -> str:
    """Expand text: '3a2b' -> 'aaabb'."""
    result: list[str] = []
    digits: list[str] = []
    for char in encoded:
        if char.isdigit():
            digits.append(char)
        else:
            if not digits:
                raise ValueError(f"missing count before {char!r}")
            result.append(char * int("".join(digits)))
            digits = []
    if digits:
        raise ValueError("trailing count with no character")
    return "".join(result)


def ratio(original: str, encoded: str) -> float:
    """Compressed size as a percentage of the original size."""
    if not original:
        return 0.0
    return len(encoded) / len(original) * 100


def main() -> None:
    samples = [
        "aaabbbcccccd",
        "wwwwwwwwwwwwbbbwwwwwwwwwwwwbbbwwwwwwwwwwww",
        "abcde",
        "",
    ]

    for original in samples:
        encoded = encode(original)
        restored = decode(encoded)
        assert restored == original, "round trip failed"
        label = original if len(original) <= 30 else original[:27] + "..."
        print(f"original : {label!r}")
        print(f"encoded  : {encoded!r}")
        print(f"decoded  : {restored!r}")
        print(f"ratio    : {ratio(original, encoded):.1f}% of original")
        print()

    # Errors surface clearly instead of producing wrong output.
    for bad in ("3", "a3"):
        try:
            decode(bad)
        except ValueError as exc:
            print(f"decode({bad!r}) -> ValueError: {exc}")


if __name__ == "__main__":
    main()
