"""
Program 46: A Genetic Algorithm

Evolves a population of strings towards a target: fitness counts matching
characters, and each generation selects parents by tournament, crosses them
over and mutates the children. Elitism carries the best few forward, which is
what makes the best fitness monotonic.

Everything is driven by a seeded random generator, so a run is reproducible.

Concepts: evolutionary search, selection pressure, crossover, mutation, elitism.

    python 46_Genetic_Algorithm.py
"""

import random
from dataclasses import dataclass


@dataclass
class Individual:
    genes: str
    fitness: int = 0


def fitness(genes: str, target: str) -> int:
    """How many characters are already in the right place."""
    return sum(1 for actual, wanted in zip(genes, target) if actual == wanted)


def random_individual(rng: random.Random, length: int, alphabet: str) -> Individual:
    genes = "".join(rng.choice(alphabet) for _ in range(length))
    return Individual(genes)


def tournament(rng: random.Random, scored: list[Individual], size: int = 3) -> Individual:
    """Pick the best of a small random sample, which keeps some pressure but not too much."""
    contenders = rng.sample(scored, min(size, len(scored)))
    return max(contenders, key=lambda individual: individual.fitness)


def crossover(rng: random.Random, mother: str, father: str) -> str:
    """Single point crossover: take a prefix from one parent and the rest from the other."""
    if len(mother) != len(father):
        raise ValueError("parents must be the same length")
    if len(mother) < 2:
        return mother
    point = rng.randint(1, len(mother) - 1)
    return mother[:point] + father[point:]


def mutate(rng: random.Random, genes: str, rate: float, alphabet: str) -> str:
    """Each character has a small chance of becoming a different letter."""
    if not 0 <= rate <= 1:
        raise ValueError("the mutation rate must be between 0 and 1")
    letters = list(genes)
    for index in range(len(letters)):
        if rng.random() < rate:
            letters[index] = rng.choice(alphabet)
    return "".join(letters)


@dataclass
class Result:
    best: str
    generations: int
    best_history: list[int]
    converged: bool


def evolve(
    target: str,
    population_size: int = 200,
    mutation_rate: float = 0.01,
    elite_count: int = 2,
    seed: int = 0,
    max_generations: int = 500,
) -> Result:
    if not target:
        raise ValueError("the target must not be empty")
    rng = random.Random(seed)
    alphabet = "".join(sorted(set(target)))
    length = len(target)

    population = [random_individual(rng, length, alphabet) for _ in range(population_size)]
    history: list[int] = []

    for generation in range(1, max_generations + 1):
        for individual in population:
            individual.fitness = fitness(individual.genes, target)
        scored = sorted(population, key=lambda individual: (-individual.fitness, individual.genes))
        history.append(scored[0].fitness)

        if scored[0].fitness == length:
            return Result(scored[0].genes, generation, history, True)

        # Elitism: the best few survive untouched, so the peak never slips back.
        next_generation = [Individual(individual.genes, individual.fitness)
                           for individual in scored[:elite_count]]
        while len(next_generation) < population_size:
            mother = tournament(rng, scored)
            father = tournament(rng, scored)
            child = mutate(rng, crossover(rng, mother.genes, father.genes), mutation_rate, alphabet)
            next_generation.append(Individual(child))
        population = next_generation

    for individual in population:
        individual.fitness = fitness(individual.genes, target)
    scored = sorted(population, key=lambda individual: (-individual.fitness, individual.genes))
    return Result(scored[0].genes, max_generations, history, False)


def main() -> None:
    # ---- the pieces on their own --------------------------------------------
    assert fitness("abc", "abc") == 3, "an exact match scores everything"
    assert fitness("abd", "abc") == 2, "one wrong letter loses one point"
    assert fitness("xyz", "abc") == 0, "nothing in common scores nothing"
    assert fitness("", "abc") == 0, "a short string scores nothing"
    print(f"fitness      : fitness('abd', 'abc') = {fitness('abd', 'abc')}")

    rng = random.Random(1)
    child = crossover(rng, "aaaa", "bbbb")
    assert len(child) == 4, "the child is the same length as its parents"
    assert set(child) <= {"a", "b"}, "and only contains genes from the parents"
    assert crossover(random.Random(3), "a", "b") == "a", "a one letter parent cannot be split"
    print(f"crossover    : aaaa x bbbb -> {child}")

    assert mutate(random.Random(4), "aaaa", 0.0, "ab") == "aaaa", "a zero rate changes nothing"
    mutated = mutate(random.Random(5), "aaaaaaaaaa", 1.0, "b")
    assert mutated == "bbbbbbbbbb", "a rate of one changes everything"
    assert len(mutate(random.Random(6), "hello", 0.5, "abc")) == 5, "mutation keeps the length"
    print(f"mutation     : rate 1.0 turned aaaaaaaaaa into {mutated}")

    try:
        mutate(random.Random(7), "abc", 1.5, "abc")
        raise AssertionError("a rate above one should be rejected")
    except ValueError as error:
        print(f"validated    : {error}")

    # ---- a full run ---------------------------------------------------------
    target = "the quick brown fox"
    result = evolve(target, population_size=200, mutation_rate=0.01, seed=42)
    assert result.converged, f"should have converged, best was {result.best!r}"
    assert result.best == target, f"the best individual should be the target, got {result.best!r}"
    assert len(result.best_history) == result.generations, "one entry per generation"
    print(f"evolved      : {target!r} in {result.generations} generations")

    # ---- elitism makes the peak monotonic -----------------------------------
    history = result.best_history
    assert history[0] > 0, "even a random start matches something"
    assert history[-1] == len(target), "and it finishes perfect"
    for earlier, later in zip(history, history[1:]):
        assert later >= earlier, "the best fitness never falls, thanks to elitism"
    print(f"history      : {history[0]} -> {history[-1]} in {len(history)} steps, never decreasing")

    # ---- reproducibility ----------------------------------------------------
    again = evolve(target, population_size=200, mutation_rate=0.01, seed=42)
    assert again.best == result.best, "the same seed gives the same answer"
    assert again.generations == result.generations, "in the same number of generations"
    print(f"reproducible : seed 42 always needs {again.generations} generations")

    other_seed = evolve(target, population_size=200, mutation_rate=0.01, seed=7)
    assert other_seed.best == target, "another seed also converges"
    assert other_seed.generations != result.generations, "but takes its own path"
    print(f"other seed   : seed 7 needed {other_seed.generations} generations")

    # ---- a longer target takes longer ---------------------------------------
    short = evolve("hello", seed=3)
    long = evolve("a much longer target string to chase", seed=3)
    assert long.generations > short.generations, "a longer target needs more generations"
    print(f"length       : 'hello' in {short.generations}, "
          f"{len('a much longer target string to chase')} characters in {long.generations}")

    # ---- a tiny population still gets there, just slower --------------------
    small = evolve("small", population_size=10, mutation_rate=0.05, seed=11)
    assert small.converged and small.best == "small", "ten individuals are enough here"
    print(f"population   : ten individuals solved 'small' in {small.generations} generations")

    # ---- an unreachable target stops at the cap -----------------------------
    # A mutation rate of zero means no new letters ever appear, so unless the
    # first random population happens to contain the answer it cannot finish.
    capped = evolve("zzzzzzzzzz", population_size=20, mutation_rate=0.0, seed=13,
                    max_generations=25)
    if not capped.converged:
        assert len(capped.best_history) == 25, "the run stopped at the cap"
        print(f"no mutation  : gave up after {capped.generations} generations "
              f"with best fitness {capped.best_history[-1]}")
    else:
        print("no mutation  : the starting population happened to match")

    # ---- the target is reached exactly, character for character -------------
    solution = evolve("genetic algorithms are fun", seed=99)
    assert solution.best == "genetic algorithms are fun"
    assert all(actual == wanted for actual, wanted in zip(solution.best, "genetic algorithms are fun"))
    print(f"exact        : {solution.best!r}")
    print("All checks passed.")


if __name__ == "__main__":
    main()
