const assert = require("node:assert/strict");

function tokenize(text) {
  return text.toLowerCase().match(/[\p{L}\p{N}']+/gu) ?? [];
}

class InvertedIndex {
  #documents = new Map();
  #postings = new Map();

  get size() {
    return this.#documents.size;
  }

  get vocabularySize() {
    return this.#postings.size;
  }

  add(id, text) {
    if (this.#documents.has(id)) {
      throw new Error(`document ${id} already exists`);
    }

    const tokens = tokenize(text);
    const frequencies = new Map();
    for (const token of tokens) {
      frequencies.set(token, (frequencies.get(token) ?? 0) + 1);
    }

    for (const [term, frequency] of frequencies) {
      let postings = this.#postings.get(term);
      if (postings === undefined) {
        postings = new Map();
        this.#postings.set(term, postings);
      }
      postings.set(id, frequency);
    }

    this.#documents.set(id, { tokens, frequencies });
    return this;
  }

  remove(id) {
    const document = this.#documents.get(id);
    if (document === undefined) {
      return false;
    }

    for (const term of document.frequencies.keys()) {
      const postings = this.#postings.get(term);
      postings.delete(id);
      if (postings.size === 0) {
        this.#postings.delete(term);
      }
    }

    this.#documents.delete(id);
    return true;
  }

  documentFrequency(term) {
    return this.#postings.get(term.toLowerCase())?.size ?? 0;
  }

  termFrequency(id, term) {
    return this.#documents.get(id)?.frequencies.get(term.toLowerCase()) ?? 0;
  }

  documentIds() {
    return [...this.#documents.keys()];
  }

  search(query) {
    const requirements = parseQuery(query);
    if (requirements.length === 0) {
      return [];
    }

    let candidates = null;
    for (const requirement of requirements) {
      const matches = new Set();
      for (const [id, document] of this.#documents) {
        const found = requirement.phrase === null
          ? document.frequencies.has(requirement.phrase ?? requirement.terms[0]) ||
            document.frequencies.has(requirement.terms[0])
          : containsPhrase(document.tokens, requirement.terms);
        if (found) {
          matches.add(id);
        }
      }

      if (candidates === null) {
        candidates = matches;
      } else {
        for (const id of [...candidates]) {
          if (!matches.has(id)) {
            candidates.delete(id);
          }
        }
      }
    }

    if (candidates === null || candidates.size === 0) {
      return [];
    }

    const total = this.#documents.size;
    const scored = [];

    for (const id of candidates) {
      const document = this.#documents.get(id);
      let score = 0;

      for (const requirement of requirements) {
        if (requirement.phrase !== null) {
          score += 3;
          continue;
        }
        for (const term of requirement.terms) {
          const frequency = document.frequencies.get(term) ?? 0;
          const documentsWithTerm = this.documentFrequency(term);
          const inverse = Math.log((total + 1) / (documentsWithTerm + 1)) + 1;
          if (frequency > 0) {
            score += (frequency / document.tokens.length) * inverse;
          }
        }
      }

      scored.push({ id, score });
    }

    scored.sort((left, right) => right.score - left.score || String(left.id).localeCompare(String(right.id)));
    return scored.map((entry) => entry.id);
  }
}

function parseQuery(query) {
  const requirements = [];
  const pattern = /"([^"]+)"|(\S+)/g;
  let match;
  while ((match = pattern.exec(query)) !== null) {
    if (match[1] !== undefined) {
      const terms = tokenize(match[1]);
      if (terms.length > 0) {
        requirements.push({ phrase: match[1], terms });
      }
    } else {
      const terms = tokenize(match[2]);
      for (const term of terms) {
        requirements.push({ phrase: null, terms: [term] });
      }
    }
  }
  return requirements;
}

function containsPhrase(tokens, phraseTokens) {
  if (phraseTokens.length === 0) {
    return false;
  }
  for (let start = 0; start + phraseTokens.length <= tokens.length; start += 1) {
    let matches = true;
    for (let offset = 0; offset < phraseTokens.length; offset += 1) {
      if (tokens[start + offset] !== phraseTokens[offset]) {
        matches = false;
        break;
      }
    }
    if (matches) {
      return true;
    }
  }
  return false;
}

function main() {
  console.log("--- tokenizing ---");
  assert.deepEqual(tokenize("The QUICK, brown fox!"), ["the", "quick", "brown", "fox"]);
  assert.deepEqual(tokenize("don't stop"), ["don't", "stop"]);
  assert.deepEqual(tokenize("café naïve"), ["café", "naïve"]);
  assert.deepEqual(tokenize("   "), []);
  console.log("punctuation, case, apostrophes, and accents handled");

  console.log("\n--- adding documents ---");
  const index = new InvertedIndex();
  index
    .add(1, "The quick brown fox")
    .add(2, "The lazy brown dog")
    .add(3, "Quick brown dogs and cats")
    .add(4, "Brown foxes are quick");

  console.log("documents:", index.size, "| vocabulary:", index.vocabularySize);
  assert.equal(index.size, 4);
  assert.equal(index.vocabularySize, 11);
  assert.throws(() => index.add(1, "duplicate"), /already exists/);
  assert.equal(index.documentFrequency("brown"), 4);
  assert.equal(index.termFrequency(1, "the"), 1);
  console.log("brown appears in", index.documentFrequency("brown"), "documents");

  console.log("\n--- term search is an AND ---");
  console.log("quick brown ->", index.search("quick brown"));
  assert.deepEqual(index.search("quick brown"), [1, 4, 3]);
  assert.deepEqual(index.search("quick BROWN"), [1, 4, 3]);
  assert.deepEqual(index.search("quick cats"), [3]);
  assert.deepEqual(index.search("quick elephant"), []);
  assert.deepEqual(index.search(""), []);
  console.log("unknown and empty queries return nothing");

  console.log("\n--- phrase search ---");
  console.log('"quick brown" ->', index.search('"quick brown"'));
  assert.deepEqual(index.search('"quick brown"'), [1, 3]);
  assert.deepEqual(index.search('"brown quick"'), []);
  assert.deepEqual(index.search('"quick brown" fox'), [1]);
  assert.deepEqual(index.search('"brown dog"'), [2]);
  console.log("phrases respect order and combine with bare terms");

  console.log("\n--- ranking favours shorter documents ---");
  const ranked = index.search("brown");
  console.log("brown ->", ranked);
  assert.deepEqual(ranked, [1, 2, 4, 3]);

  console.log("\n--- removing documents ---");
  assert.equal(index.remove(3), true);
  assert.equal(index.remove(3), false);
  assert.equal(index.size, 3);
  assert.equal(index.documentFrequency("cats"), 0);
  assert.deepEqual(index.search("cats"), []);
  assert.deepEqual(index.search("quick brown"), [1, 4]);
  console.log("after removing 3:", index.search("quick brown"));

  assert.equal(index.remove(1), true);
  assert.equal(index.remove(2), true);
  assert.equal(index.remove(4), true);
  assert.equal(index.size, 0);
  assert.equal(index.vocabularySize, 0);
  assert.deepEqual(index.search("brown"), []);
  console.log("removing everything empties the index");

  console.log("\n--- rebuild and check every hit ---");
  const rebuilt = new InvertedIndex();
  rebuilt.add("a", "red green blue").add("b", "green yellow").add("c", "blue blue green");
  const hits = rebuilt.search("blue green");
  console.log("blue green ->", hits);
  assert.deepEqual(hits.sort(), ["a", "c"]);
  for (const id of hits) {
    assert.ok(rebuilt.termFrequency(id, "blue") > 0 || rebuilt.termFrequency(id, "green") > 0);
  }

  const phraseHits = rebuilt.search('"green blue"');
  assert.deepEqual(phraseHits, ["a"]);
  assert.deepEqual(rebuilt.search('"blue green"'), ["c"]);

  console.log("\n--- numeric and unicode documents ---");
  const mixed = new InvertedIndex();
  mixed.add("doc1", "2024 report café");
  mixed.add("doc2", "café naïve 2025");
  assert.deepEqual(mixed.search("café"), ["doc1", "doc2"]);
  assert.deepEqual(mixed.search("2025"), ["doc2"]);
  console.log("unicode and numbers are searchable");

  console.log("\nAll checks passed.");
}

main();
