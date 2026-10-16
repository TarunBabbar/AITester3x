class TreeNode<T> {
  left: TreeNode<T> | null = null;
  right: TreeNode<T> | null = null;

  constructor(public value: T) {}
}

class BinarySearchTree<T> {
  root: TreeNode<T> | null = null;

  constructor(private compare: (a: T, b: T) => number) {}

  insert(value: T): void {
    const node = new TreeNode(value);
    if (!this.root) {
      this.root = node;
      return;
    }
    let current = this.root;
    while (true) {
      const cmp = this.compare(value, current.value);
      if (cmp < 0) {
        if (!current.left) {
          current.left = node;
          return;
        }
        current = current.left;
      } else {
        if (!current.right) {
          current.right = node;
          return;
        }
        current = current.right;
      }
    }
  }

  contains(value: T): boolean {
    let current = this.root;
    while (current) {
      const cmp = this.compare(value, current.value);
      if (cmp === 0) {
        return true;
      }
      current = cmp < 0 ? current.left : current.right;
    }
    return false;
  }

  inOrder(): T[] {
    const result: T[] = [];
    const walk = (node: TreeNode<T> | null): void => {
      if (!node) {
        return;
      }
      walk(node.left);
      result.push(node.value);
      walk(node.right);
    };
    walk(this.root);
    return result;
  }

  height(): number {
    const measure = (node: TreeNode<T> | null): number =>
      node ? 1 + Math.max(measure(node.left), measure(node.right)) : 0;
    return measure(this.root);
  }
}

const tree = new BinarySearchTree<number>((a, b) => a - b);

for (const value of [8, 3, 10, 1, 6, 14, 4, 7, 13]) {
  tree.insert(value);
}

console.log("in order:", tree.inOrder().join(", "));
console.log("contains 7:", tree.contains(7));
console.log("contains 99:", tree.contains(99));
console.log("height:", tree.height());

const words = new BinarySearchTree<string>((a, b) => a.localeCompare(b));
for (const word of ["banana", "apple", "cherry", "date"]) {
  words.insert(word);
}
console.log("sorted words:", words.inOrder().join(", "));
