type Comparator<K> = (left: K, right: K) => number;

class TreeNode<K, V> {
  key: K;
  value: V;
  left: TreeNode<K, V> | null = null;
  right: TreeNode<K, V> | null = null;

  constructor(key: K, value: V) {
    this.key = key;
    this.value = value;
  }
}

class BinarySearchTree<K, V> {
  private root: TreeNode<K, V> | null = null;
  private count = 0;

  constructor(private readonly compare: Comparator<K>) {}

  get size(): number {
    return this.count;
  }

  get isEmpty(): boolean {
    return this.count === 0;
  }

  height(): number {
    return this.heightOf(this.root);
  }

  private heightOf(node: TreeNode<K, V> | null): number {
    if (node === null) {
      return 0;
    }
    return 1 + Math.max(this.heightOf(node.left), this.heightOf(node.right));
  }

  /** Returns true when the key was new, false when an existing value was replaced. */
  set(key: K, value: V): boolean {
    if (this.root === null) {
      this.root = new TreeNode(key, value);
      this.count = 1;
      return true;
    }

    let node = this.root;
    while (true) {
      const comparison = this.compare(key, node.key);
      if (comparison === 0) {
        node.value = value;
        return false;
      }
      if (comparison < 0) {
        if (node.left === null) {
          node.left = new TreeNode(key, value);
          this.count += 1;
          return true;
        }
        node = node.left;
      } else {
        if (node.right === null) {
          node.right = new TreeNode(key, value);
          this.count += 1;
          return true;
        }
        node = node.right;
      }
    }
  }

  get(key: K): V | undefined {
    let node = this.root;
    while (node !== null) {
      const comparison = this.compare(key, node.key);
      if (comparison === 0) {
        return node.value;
      }
      node = comparison < 0 ? node.left : node.right;
    }
    return undefined;
  }

  has(key: K): boolean {
    return this.get(key) !== undefined;
  }

  delete(key: K): boolean {
    const before = this.count;
    this.root = this.deleteNode(this.root, key);
    return this.count < before;
  }

  private deleteNode(node: TreeNode<K, V> | null, key: K): TreeNode<K, V> | null {
    if (node === null) {
      return null;
    }

    const comparison = this.compare(key, node.key);
    if (comparison < 0) {
      node.left = this.deleteNode(node.left, key);
    } else if (comparison > 0) {
      node.right = this.deleteNode(node.right, key);
    } else if (node.left === null) {
      this.count -= 1;
      return node.right;
    } else if (node.right === null) {
      this.count -= 1;
      return node.left;
    } else {
      const successor = this.minNode(node.right);
      node.key = successor.key;
      node.value = successor.value;
      node.right = this.deleteNode(node.right, successor.key);
    }

    return node;
  }

  private minNode(node: TreeNode<K, V>): TreeNode<K, V> {
    let current = node;
    while (current.left !== null) {
      current = current.left;
    }
    return current;
  }

  min(): K | undefined {
    return this.root === null ? undefined : this.minNode(this.root).key;
  }

  max(): K | undefined {
    let node = this.root;
    if (node === null) {
      return undefined;
    }
    while (node.right !== null) {
      node = node.right;
    }
    return node.key;
  }

  inOrder(): Array<[K, V]> {
    const entries: Array<[K, V]> = [];
    const visit = (node: TreeNode<K, V> | null): void => {
      if (node === null) {
        return;
      }
      visit(node.left);
      entries.push([node.key, node.value]);
      visit(node.right);
    };
    visit(this.root);
    return entries;
  }

  keys(): K[] {
    return this.inOrder().map(([key]) => key);
  }

  values(): V[] {
    return this.inOrder().map(([, value]) => value);
  }

  kthSmallest(k: number): K | undefined {
    if (!Number.isInteger(k) || k < 1 || k > this.count) {
      return undefined;
    }
    const entries = this.inOrder();
    return entries[k - 1][0];
  }

  range(minimum: K, maximum: K): V[] {
    if (this.compare(minimum, maximum) > 0) {
      throw new RangeError("minimum must not be greater than maximum");
    }
    const found: V[] = [];
    const visit = (node: TreeNode<K, V> | null): void => {
      if (node === null) {
        return;
      }
      if (this.compare(node.key, minimum) > 0) {
        visit(node.left);
      }
      if (this.compare(node.key, minimum) >= 0 && this.compare(node.key, maximum) <= 0) {
        found.push(node.value);
      }
      if (this.compare(node.key, maximum) < 0) {
        visit(node.right);
      }
    };
    visit(this.root);
    return found;
  }

  /** In-order keys are strictly increasing, which is the whole invariant. */
  validate(): boolean {
    const entries = this.inOrder();
    if (entries.length !== this.count) {
      return false;
    }
    for (let index = 1; index < entries.length; index += 1) {
      if (this.compare(entries[index - 1][0], entries[index][0]) >= 0) {
        return false;
      }
    }
    return true;
  }
}

function check(condition: boolean, message: string): void {
  if (!condition) {
    throw new Error(message);
  }
}

function main(): void {
  console.log("--- insert and lookup ---");
  const tree = new BinarySearchTree<number, string>((left, right) => left - right);
  for (const [key, value] of [
    [50, "fifty"],
    [30, "thirty"],
    [70, "seventy"],
    [20, "twenty"],
    [40, "forty"],
    [60, "sixty"],
    [80, "eighty"],
  ] as Array<[number, string]>) {
    check(tree.set(key, value), `insert ${key}`);
  }

  console.log("size:", tree.size, "| height:", tree.height(), "| keys:", tree.keys().join(", "));
  check(tree.size === 7, "seven keys");
  check(tree.height() === 3, "balanced height is three");
  check(tree.get(40) === "forty", "lookup by key");
  check(tree.get(41) === undefined, "missing key is undefined");
  check(tree.min() === 20 && tree.max() === 80, "extremes");
  check(tree.validate(), "valid after inserts");

  console.log("\n--- replacing a value ---");
  check(!tree.set(40, "FORTY"), "existing key is not new");
  check(tree.get(40) === "FORTY", "value was replaced");
  check(tree.size === 7, "size unchanged");

  console.log("\n--- ordered traversal ---");
  console.log(tree.inOrder().map(([key, value]) => `${key}:${value}`).join(" "));
  check(tree.keys().join(",") === "20,30,40,50,60,70,80", "in-order keys");
  check(tree.kthSmallest(1) === 20 && tree.kthSmallest(4) === 50 && tree.kthSmallest(7) === 80, "kth smallest");
  check(tree.kthSmallest(0) === undefined && tree.kthSmallest(8) === undefined, "k out of range");

  console.log("\n--- range queries ---");
  console.log("values 30..60:", tree.range(30, 60).join(", "));
  check(tree.range(30, 60).join(",") === "thirty,FORTY,fifty,sixty", "inclusive range");
  check(tree.range(31, 39).length === 0, "empty range");
  check(tree.range(80, 80).join(",") === "eighty", "single point range");
  let rangeThrew = false;
  try {
    tree.range(60, 30);
  } catch (error) {
    rangeThrew = true;
    void error;
  }
  check(rangeThrew, "reversed range should throw");

  console.log("\n--- deletion cases ---");
  check(tree.delete(20), "delete a leaf");
  check(!tree.delete(20), "deleting twice reports false");
  check(tree.keys().join(",") === "30,40,50,60,70,80", "leaf removed");

  check(tree.delete(30), "delete a node with one child");
  check(tree.keys().join(",") === "40,50,60,70,80", "one-child node removed");

  check(tree.delete(50), "delete the root with two children");
  check(tree.keys().join(",") === "40,60,70,80", "two-child node removed");
  check(tree.get(60) === "sixty", "successor kept its value");
  check(tree.validate(), "valid after deletions");
  console.log("after deletions:", tree.keys().join(", "));

  console.log("\n--- sorted insertion is a linked list ---");
  const skewed = new BinarySearchTree<number, number>((left, right) => left - right);
  for (let key = 1; key <= 100; key += 1) {
    skewed.set(key, key);
  }
  console.log("height after ascending inserts:", skewed.height());
  check(skewed.height() === 100, "worst-case shape is a chain");
  check(skewed.validate(), "still a valid BST");

  console.log("\n--- string keys ---");
  const words = new BinarySearchTree<string, number>((left, right) => left.localeCompare(right));
  for (const word of ["pear", "apple", "orange", "banana", "kiwi"]) {
    words.set(word, word.length);
  }
  console.log("sorted words:", words.keys().join(", "));
  check(words.keys().join(",") === "apple,banana,kiwi,orange,pear", "locale ordering");
  check(words.range("b", "oz").join(",") === "6,4,6", "range over strings");
  check(words.validate(), "string tree valid");

  console.log("\n--- against a map ---");
  let state = 77;
  const next = (): number => {
    state = (state * 1103515245 + 12345) % 2147483648;
    return state;
  };

  const randomTree = new BinarySearchTree<number, string>((left, right) => left - right);
  const reference = new Map<number, string>();

  for (let step = 0; step < 2_000; step += 1) {
    const key = next() % 200;
    if (next() % 3 === 0) {
      const expected = reference.delete(key);
      check(randomTree.delete(key) === expected, `delete mismatch at step ${step}`);
    } else {
      const value = `v${step}`;
      const expected = !reference.has(key);
      check(randomTree.set(key, value) === expected, `insert mismatch at step ${step}`);
      reference.set(key, value);
    }

    check(randomTree.size === reference.size, `size at step ${step}`);
    check(randomTree.validate(), `invariant at step ${step}`);

    if (step % 100 === 0) {
      const expectedKeys = [...reference.keys()].sort((left, right) => left - right);
      check(randomTree.keys().join(",") === expectedKeys.join(","), `keys at step ${step}`);
      for (const [key, value] of reference) {
        check(randomTree.get(key) === value, `value for ${key} at step ${step}`);
      }
    }
  }
  console.log("2000 random operations match a Map");

  console.log("\nAll checks passed.");
}

main();
