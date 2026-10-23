class TrieNode {
  children = new Map<string, TrieNode>();
  isWord = false;
  weight = 0;
}

class Autocomplete {
  private root = new TrieNode();

  insert(word: string, weight = 1): void {
    let node = this.root;
    for (const char of word) {
      if (!node.children.has(char)) {
        node.children.set(char, new TrieNode());
      }
      node = node.children.get(char)!;
    }
    node.isWord = true;
    node.weight = Math.max(node.weight, weight);
  }

  has(word: string): boolean {
    return this.findNode(word)?.isWord ?? false;
  }

  private findNode(prefix: string): TrieNode | undefined {
    let node = this.root;
    for (const char of prefix) {
      const next = node.children.get(char);
      if (!next) return undefined;
      node = next;
    }
    return node;
  }

  suggest(prefix: string, limit = 5): string[] {
    const start = this.findNode(prefix);
    if (!start) return [];

    const matches: { word: string; weight: number }[] = [];
    const walk = (node: TrieNode, path: string): void => {
      if (node.isWord) {
        matches.push({ word: path, weight: node.weight });
      }
      for (const [char, child] of node.children) {
        walk(child, path + char);
      }
    };

    walk(start, prefix);
    return matches
      .sort((a, b) => b.weight - a.weight || a.word.localeCompare(b.word))
      .slice(0, limit)
      .map((entry) => entry.word);
  }
}

const dictionary = new Autocomplete();
dictionary.insert("type", 5);
dictionary.insert("typescript", 9);
dictionary.insert("typewriter", 2);
dictionary.insert("typesafe", 4);
dictionary.insert("tuple", 6);
dictionary.insert("tumble", 3);

console.log("has 'typing':", dictionary.has("typing"));
console.log("has 'type':", dictionary.has("type"));

console.log("suggestions for 'typ':", dictionary.suggest("typ").join(", "));
console.log("suggestions for 'tu':", dictionary.suggest("tu", 2).join(", "));
console.log("suggestions for 'zzz':", dictionary.suggest("zzz").length);

dictionary.insert("typescript", 20);
console.log("top after boost:", dictionary.suggest("typ", 3).join(", "));
