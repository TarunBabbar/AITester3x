type Comparator<K> = (left: K, right: K) => number;

class AvlNode<K, V> {
  key: K;
  value: V;
  height = 1;
  left: AvlNode<K, V> | null = null;
  right: AvlNode<K, V> | null = null;

  constructor(key: K, value: V) {
    this.key = key;
    this.value = value;
  }
}

class AvlTree<K, V> {
  private root: AvlNode<K, V> | null = null;
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

  rootKey(): K | undefined {
    return this.root?.key;
  }

  set(key: K, value: V): boolean {
    const [node, added] = this.insert(this.root, key, value);
    this.root = node;
    return added;
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
    const [node, removed] = this.remove(this.root, key);
    this.root = node;
    return removed;
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
    const visit = (node: AvlNode<K, V> | null): void => {
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

  /** Every node's stored height is right and no subtree differs by more than one. */
  isBalanced(): boolean {
    const verify = (node: AvlNode<K, V> | null): boolean => {
      if (node === null) {
        return true;
      }
      const leftHeight = this.heightOf(node.left);
      const rightHeight = this.heightOf(node.right);
      if (node.height !== 1 + Math.max(leftHeight, rightHeight)) {
        return false;
      }
      if (Math.abs(leftHeight - rightHeight) > 1) {
        return false;
      }
      return verify(node.left) && verify(node.right);
    };
    return verify(this.root);
  }

  private heightOf(node: AvlNode<K, V> | null): number {
    return node === null ? 0 : node.height;
  }

  private refresh(node: AvlNode<K, V>): void {
    node.height = 1 + Math.max(this.heightOf(node.left), this.heightOf(node.right));
  }

  private balanceFactor(node: AvlNode<K, V>): number {
    return this.heightOf(node.left) - this.heightOf(node.right);
  }

  private rotateRight(node: AvlNode<K, V>): AvlNode<K, V> {
    const pivot = node.left!;
    node.left = pivot.right;
    pivot.right = node;
    this.refresh(node);
    this.refresh(pivot);
    return pivot;
  }

  private rotateLeft(node: AvlNode<K, V>): AvlNode<K, V> {
    const pivot = node.right!;
    node.right = pivot.left;
    pivot.left = node;
    this.refresh(node);
    this.refresh(pivot);
    return pivot;
  }

  private rebalance(node: AvlNode<K, V>): AvlNode<K, V> {
    this.refresh(node);
    const factor = this.balanceFactor(node);

    if (factor > 1) {
      if (this.balanceFactor(node.left!) < 0) {
        node.left = this.rotateLeft(node.left!);
      }
      return this.rotateRight(node);
    }

    if (factor < -1) {
      if (this.balanceFactor(node.right!) > 0) {
        node.right = this.rotateRight(node.right!);
      }
      return this.rotateLeft(node);
    }

    return node;
  }

  private insert(
    node: AvlNode<K, V> | null,
    key: K,
    value: V,
  ): [AvlNode<K, V>, boolean] {
    if (node === null) {
      this.count += 1;
      return [new AvlNode(key, value), true];
    }

    const comparison = this.compare(key, node.key);
    if (comparison === 0) {
      node.value = value;
      return [node, false];
    }

    let added = false;
    if (comparison < 0) {
      const [child, inserted] = this.insert(node.left, key, value);
      node.left = child;
      added = inserted;
    } else {
      const [child, inserted] = this.insert(node.right, key, value);
      node.right = child;
      added = inserted;
    }

    return [this.rebalance(node), added];
  }

  private remove(
    node: AvlNode<K, V> | null,
    key: K,
  ): [AvlNode<K, V> | null, boolean] {
    if (node === null) {
      return [null, false];
    }

    const comparison = this.compare(key, node.key);

    if (comparison < 0) {
      const [child, removed] = this.remove(node.left, key);
      if (!removed) {
        return [node, false];
      }
      node.left = child;
      return [this.rebalance(node), true];
    }

    if (comparison > 0) {
      const [child, removed] = this.remove(node.right, key);
      if (!removed) {
        return [node, false];
      }
      node.right = child;
      return [this.rebalance(node), true];
    }

    if (node.left === null) {
      this.count -= 1;
      return [node.right, true];
    }
    if (node.right === null) {
      this.count -= 1;
      return [node.left, true];
    }

    const successor = this.minNode(node.right);
    node.key = successor.key;
    node.value = successor.value;
    const [child] = this.remove(node.right, successor.key);
    node.right = child;
    return [this.rebalance(node), true];
  }

  private minNode(node: AvlNode<K, V>): AvlNode<K, V> {
    let current = node;
    while (current.left !== null) {
      current = current.left;
    }
    return current;
  }
}

function check(condition: boolean, message: string): void {
  if (!condition) {
    throw new Error(message);
  }
}

function main(): void {
  console.log("--- rotations keep the tree short ---");
  const ascending = new AvlTree<number, number>((left, right) => left - right);
  for (let key = 1; key <= 1_000; key += 1) {
    ascending.set(key, key);
  }
  console.log(
    `1000 ascending keys: height ${ascending.height()}, balanced ${ascending.isBalanced()}`,
  );
  check(ascending.size === 1_000, "all keys inserted");
  check(ascending.height() <= 15, `height ${ascending.height()} should stay logarithmic`);
  check(ascending.isBalanced(), "the tree is balanced");
  check(ascending.keys().join(",") === Array.from({ length: 1_000 }, (_, index) => index + 1).join(","), "in-order keys");

  const shape = new AvlTree<number, string>((left, right) => left - right);
  shape.set(1, "one");
  shape.set(2, "two");
  shape.set(3, "three");
  console.log("after 1, 2, 3 the root is:", shape.rootKey());
  check(shape.rootKey() === 2, "a left rotation makes 2 the root");

  const mirrored = new AvlTree<number, number>((left, right) => left - right);
  mirrored.set(3, 3);
  mirrored.set(2, 2);
  mirrored.set(1, 1);
  console.log("after 3, 2, 1 the root is:", mirrored.rootKey());
  check(mirrored.rootKey() === 2, "a right rotation makes 2 the root");

  const zigzag = new AvlTree<number, number>((left, right) => left - right);
  zigzag.set(3, 3);
  zigzag.set(1, 1);
  zigzag.set(2, 2);
  console.log("after 3, 1, 2 the root is:", zigzag.rootKey());
  check(zigzag.rootKey() === 2, "a double rotation handles the zig-zag");

  console.log("\n--- basics ---");
  const tree = new AvlTree<string, number>((left, right) => left.localeCompare(right));
  for (const word of ["pear", "apple", "orange", "banana", "kiwi"]) {
    check(tree.set(word, word.length), `insert ${word}`);
  }
  console.log("keys:", tree.keys().join(", "), "| height:", tree.height());
  check(tree.keys().join(",") === "apple,banana,kiwi,orange,pear", "sorted keys");
  check(tree.get("kiwi") === 4, "lookup");
  check(tree.get("grape") === undefined, "missing key");
  check(tree.min() === "apple" && tree.max() === "pear", "extremes");
  check(!tree.set("kiwi", 99), "duplicate key is not new");
  check(tree.get("kiwi") === 99, "value replaced");

  console.log("\n--- deletion ---");
  check(tree.delete("banana"), "delete an interior key");
  check(!tree.delete("banana"), "deleting twice reports false");
  check(tree.keys().join(",") === "apple,kiwi,orange,pear", "key removed");
  check(tree.isBalanced(), "still balanced");
  check(tree.delete("pear") && tree.delete("apple"), "delete ends");
  check(tree.isBalanced(), "balanced after end deletions");
  console.log("remaining:", tree.keys().join(", "));
  tree.delete("kiwi");
  tree.delete("orange");
  check(tree.isEmpty, "all keys deleted");
  check(tree.height() === 0 && tree.rootKey() === undefined, "empty tree has no height or root");

  console.log("\n--- against a map ---");
  let state = 80;
  const next = (): number => {
    state = (state * 1103515245 + 12345) % 2147483648;
    return state;
  };

  const randomTree = new AvlTree<number, string>((left, right) => left - right);
  const reference = new Map<number, string>();

  for (let step = 0; step < 2_000; step += 1) {
    const key = next() % 300;
    if (next() % 3 === 0) {
      check(randomTree.delete(key) === reference.delete(key), `delete mismatch at step ${step}`);
    } else {
      const value = `v${step}`;
      const expected = !reference.has(key);
      check(randomTree.set(key, value) === expected, `insert mismatch at step ${step}`);
      reference.set(key, value);
    }

    check(randomTree.size === reference.size, `size at step ${step}`);
    check(randomTree.isBalanced(), `balance at step ${step}`);

    if (step % 200 === 0) {
      const expectedKeys = [...reference.keys()].sort((left, right) => left - right);
      check(randomTree.keys().join(",") === expectedKeys.join(","), `keys at step ${step}`);
      for (const [key, value] of reference) {
        check(randomTree.get(key) === value, `value for ${key} at step ${step}`);
      }
    }
  }
  console.log("2000 random operations match a Map and stay balanced");
  console.log("final height:", randomTree.height());

  console.log("\n--- worst-case height guarantee ---");
  for (const size of [10, 100, 1_000, 10_000]) {
    const stress = new AvlTree<number, number>((left, right) => left - right);
    for (let key = 0; key < size; key += 1) {
      stress.set(key, key);
    }
    const limit = Math.ceil(1.44 * Math.log2(size + 2));
    console.log(`  ${size} keys -> height ${stress.height()} (limit ${limit})`);
    check(stress.height() <= limit, `height bound for ${size}`);
    check(stress.isBalanced(), `balanced at ${size}`);
  }

  console.log("\nAll checks passed.");
}

main();
