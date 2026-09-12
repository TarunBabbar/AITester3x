"""
Program 26: Markov Chain Text Generator

Builds a first-order Markov model from sample text and generates new
sentences by walking the chain from a random starting word.

Concepts: dictionaries, tuples as keys, random.choices with weights, text.
"""

import random
from collections import defaultdict

SAMPLE_TEXT = """
Python is a powerful language. Python is easy to learn and Python is fun.
A good programmer writes tests. A good programmer reads documentation.
Testing your code early saves time. Testing your code often saves money.
The best way to learn programming is to write programs.
The best way to learn testing is to write tests.
"""


def tokenize(text: str) -> list[str]:
    """Split text into words, keeping simple punctuation out of the way."""
    cleaned = text.replace(".", " . ").replace(",", " , ")
    return [token for token in cleaned.split() if token]


def build_chain(words: list[str]) -> dict[str, list[str]]:
    """Map each word to the list of words that follow it."""
    chain: dict[str, list[str]] = defaultdict(list)
    for current, following in zip(words, words[1:]):
        chain[current].append(following)
    return chain


def build_chain_weights(words: list[str]) -> dict[str, dict[str, int]]:
    """Alternative model: word -> {next_word: count}."""
    weights: dict[str, dict[str, int]] = defaultdict(lambda: defaultdict(int))
    for current, following in zip(words, words[1:]):
        weights[current][following] += 1
    return weights


def generate(chain: dict[str, list[str]], length: int) -> str:
    """Walk the chain from a random word for `length` tokens."""
    word = random.choice(list(chain))
    output = [word]
    for _ in range(length - 1):
        options = chain.get(word)
        if not options:
            break
        word = random.choice(options)
        output.append(word)

    text = " ".join(output)
    return text.replace(" .", ".").replace(" ,", ",")


def main() -> None:
    words = tokenize(SAMPLE_TEXT)
    chain = build_chain(words)
    weights = build_chain_weights(words)

    print(f"Sample text has {len(words)} tokens, {len(chain)} unique states.")
    print("Example transitions:")
    for word in list(chain)[:5]:
        print(f"  {word!r} -> {chain[word]}")

    most_common = max(weights, key=lambda w: sum(weights[w].values()))
    print(f"\nBusiest state: {most_common!r} "
          f"(followed by {dict(weights[most_common])})")

    print("\nGenerated sentences:")
    for _ in range(5):
        print(f"  {generate(chain, 12)}")


if __name__ == "__main__":
    main()