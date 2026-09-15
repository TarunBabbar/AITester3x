"""
Program 38: Dynamic Programming - Coin Change and 0/1 Knapsack

Solves two classic optimisation problems with bottom-up dynamic programming:
the fewest coins that add up to an amount, and the most valuable subset of
items that fits a knapsack. Both print their DP table and rebuild the answer.

Concepts: dynamic programming, tables, optimisation, solution reconstruction.
"""


def coin_table(amount: int, coins: list[int]) -> list[float]:
    """best[i] = fewest coins summing to i (inf when impossible)."""
    best: list[float] = [0] + [float("inf")] * amount
    for target in range(1, amount + 1):
        for coin in coins:
            if coin <= target and best[target - coin] + 1 < best[target]:
                best[target] = best[target - coin] + 1
    return best


def min_coins(amount: int, coins: list[int]) -> int | None:
    """Fewest coins summing to amount, or None if it cannot be made."""
    best = coin_table(amount, coins)[amount]
    return None if best == float("inf") else int(best)


def coin_combination(amount: int, coins: list[int]) -> list[int]:
    """Rebuild one optimal multiset of coins from the DP table."""
    best = coin_table(amount, coins)
    if best[amount] == float("inf"):
        return []
    used: list[int] = []
    remaining = amount
    while remaining > 0:
        for coin in coins:
            if coin <= remaining and best[remaining - coin] == best[remaining] - 1:
                used.append(coin)
                remaining -= coin
                break
    return used


def knapsack(items: list[tuple[str, int, int]], capacity: int) -> tuple[int, list[str]]:
    """0/1 knapsack over (name, weight, value); returns (best value, names)."""
    table = [[0] * (capacity + 1) for _ in range(len(items) + 1)]
    for index, (_, weight, value) in enumerate(items, start=1):
        for room in range(capacity + 1):
            table[index][room] = table[index - 1][room]
            if weight <= room:
                table[index][room] = max(
                    table[index][room], table[index - 1][room - weight] + value
                )

    chosen: list[str] = []
    room = capacity
    for index in range(len(items), 0, -1):
        if table[index][room] != table[index - 1][room]:
            name, weight, _ = items[index - 1]
            chosen.append(name)
            room -= weight
    chosen.reverse()
    return table[-1][capacity], chosen


def main() -> None:
    coins = [1, 5, 10, 25]
    amount = 63
    best = coin_table(amount, coins)
    combination = coin_combination(amount, coins)

    print(f"Coin change for {amount} using {coins}")
    print("  fewest coins :", min_coins(amount, coins))
    print("  one solution :", sorted(combination, reverse=True))
    assert min_coins(amount, coins) == 6
    assert sum(combination) == amount and len(combination) == 6
    assert min_coins(30, coins) == 2

    impossible = min_coins(7, [2, 4])
    print(f"  min_coins(7, [2, 4]) -> {impossible}  (odd amount, impossible)")
    assert impossible is None

    items = [("gold", 10, 60), ("silver", 20, 100), ("bronze", 30, 120)]
    capacity = 50
    value, chosen = knapsack(items, capacity)

    print(f"\n0/1 knapsack (capacity {capacity}) from {items}")
    print(f"  best value   : {value}")
    print(f"  chosen items : {chosen}")
    assert value == 220
    assert chosen == ["silver", "bronze"]
    assert sum(weight for name, weight, _ in items if name in chosen) == capacity

    print("\nDP table for coin change (index: fewest coins):")
    print("  " + " ".join(f"{index}:{int(best[index])}" for index in range(0, amount + 1, 10)))


if __name__ == "__main__":
    main()
