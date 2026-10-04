import java.util.ArrayList;
import java.util.List;

/**
 * 97 - An AVL tree: a binary search tree that rotates itself back into balance
 * after every insert and delete, so the height stays logarithmic even when the
 * keys arrive in sorted order.
 *
 * Compile and run:
 *   javac AvlTree.java
 *   java AvlTree
 */
public class AvlTree {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static final class Node {
        int key;
        int height = 1;
        Node left;
        Node right;

        Node(int key) {
            this.key = key;
        }
    }

    static final class Avl {
        private Node root;
        private int size;

        int size() {
            return size;
        }

        int height() {
            return height(root);
        }

        private static int height(Node node) {
            return node == null ? 0 : node.height;
        }

        private static void updateHeight(Node node) {
            node.height = Math.max(height(node.left), height(node.right)) + 1;
        }

        private static int balanceFactor(Node node) {
            return height(node.left) - height(node.right);
        }

        void add(int key) {
            if (contains(key)) {
                return;                       // duplicates are ignored
            }
            root = insert(root, key);
            size++;
        }

        boolean contains(int key) {
            Node node = root;
            while (node != null) {
                if (key < node.key) {
                    node = node.left;
                } else if (key > node.key) {
                    node = node.right;
                } else {
                    return true;
                }
            }
            return false;
        }

        void remove(int key) {
            if (!contains(key)) {
                return;
            }
            root = delete(root, key);
            size--;
        }

        private Node insert(Node node, int key) {
            if (node == null) {
                return new Node(key);
            }
            if (key < node.key) {
                node.left = insert(node.left, key);
            } else {
                node.right = insert(node.right, key);
            }
            return rebalance(node);
        }

        private Node delete(Node node, int key) {
            if (key < node.key) {
                node.left = delete(node.left, key);
            } else if (key > node.key) {
                node.right = delete(node.right, key);
            } else if (node.left == null) {
                return node.right;
            } else if (node.right == null) {
                return node.left;
            } else {
                // Replace with the smallest key in the right subtree, then remove it.
                Node successor = node.right;
                while (successor.left != null) {
                    successor = successor.left;
                }
                node.key = successor.key;
                node.right = delete(node.right, successor.key);
            }
            return rebalance(node);
        }

        /** One of the four rotation cases, chosen from the balance factors. */
        private Node rebalance(Node node) {
            updateHeight(node);
            int balance = balanceFactor(node);

            if (balance > 1) {
                if (balanceFactor(node.left) < 0) {
                    node.left = rotateLeft(node.left);     // left-right case
                }
                return rotateRight(node);
            }
            if (balance < -1) {
                if (balanceFactor(node.right) > 0) {
                    node.right = rotateRight(node.right);  // right-left case
                }
                return rotateLeft(node);
            }
            return node;
        }

        private Node rotateRight(Node node) {
            Node pivot = node.left;
            node.left = pivot.right;
            pivot.right = node;
            updateHeight(node);
            updateHeight(pivot);
            return pivot;
        }

        private Node rotateLeft(Node node) {
            Node pivot = node.right;
            node.right = pivot.left;
            pivot.left = node;
            updateHeight(node);
            updateHeight(pivot);
            return pivot;
        }

        /** Every node's two subtrees differ in height by at most one. */
        boolean isBalanced() {
            return balanced(root);
        }

        private boolean balanced(Node node) {
            if (node == null) {
                return true;
            }
            if (Math.abs(balanceFactor(node)) > 1) {
                return false;
            }
            return balanced(node.left) && balanced(node.right);
        }

        /** The heights and balance factors recorded on the nodes are honest. */
        boolean heightsAreCorrect() {
            return heightsAgree(root);
        }

        private boolean heightsAgree(Node node) {
            if (node == null) {
                return true;
            }
            if (node.height != Math.max(height(node.left), height(node.right)) + 1) {
                return false;
            }
            return heightsAgree(node.left) && heightsAgree(node.right);
        }

        int rootKey() {
            return root == null ? Integer.MIN_VALUE : root.key;
        }

        List<Integer> inOrder() {
            List<Integer> keys = new ArrayList<>();
            walk(root, keys);
            return keys;
        }

        private void walk(Node node, List<Integer> keys) {
            if (node == null) {
                return;
            }
            walk(node.left, keys);
            keys.add(node.key);
            walk(node.right, keys);
        }

        boolean isSearchTree() {
            List<Integer> keys = inOrder();
            for (int i = 1; i < keys.size(); i++) {
                if (keys.get(i - 1) >= keys.get(i)) {
                    return false;
                }
            }
            return true;
        }
    }

    /** A deliberately naive search tree, to show what the rotations prevent. */
    static final class PlainTree {
        private Node root;

        void add(int key) {
            root = insert(root, key);
        }

        private Node insert(Node node, int key) {
            if (node == null) {
                return new Node(key);
            }
            if (key < node.key) {
                node.left = insert(node.left, key);
            } else {
                node.right = insert(node.right, key);
            }
            return node;
        }

        int height() {
            return height(root);
        }

        private static int height(Node node) {
            return node == null ? 0 : Math.max(height(node.left), height(node.right)) + 1;
        }
    }

    public static void main(String[] args) {
        // ---- the four rotation shapes ----------------------------------------
        Avl rightRotation = new Avl();
        rightRotation.add(3);
        rightRotation.add(2);
        rightRotation.add(1);
        check(rightRotation.rootKey() == 2, "descending keys rotate the root into the middle");
        check(rightRotation.inOrder().equals(List.of(1, 2, 3)), "and the order is intact");

        Avl leftRotation = new Avl();
        leftRotation.add(1);
        leftRotation.add(2);
        leftRotation.add(3);
        check(leftRotation.rootKey() == 2, "ascending keys rotate the other way");

        Avl leftRight = new Avl();
        leftRight.add(3);
        leftRight.add(1);
        leftRight.add(2);
        check(leftRight.rootKey() == 2, "a left-right shape needs two rotations");

        Avl rightLeft = new Avl();
        rightLeft.add(1);
        rightLeft.add(3);
        rightLeft.add(2);
        check(rightLeft.rootKey() == 2, "and so does a right-left shape");
        System.out.println("rotations    : all four cases end with 2 at the root");

        // ---- sorted input is the worst case for a plain tree ------------------
        Avl balanced = new Avl();
        PlainTree plain = new PlainTree();
        for (int key = 1; key <= 100; key++) {
            balanced.add(key);
            plain.add(key);
        }
        check(plain.height() == 100, "one hundred sorted keys make a chain of height 100");
        check(balanced.height() <= 8, "the AVL tree stays within 8: " + balanced.height());
        check(balanced.size() == 100, "with every key present");
        check(balanced.isBalanced(), "and balanced everywhere");
        check(balanced.heightsAreCorrect(), "with correct height bookkeeping");
        check(balanced.isSearchTree(), "and it is still a search tree");
        System.out.println("sorted input : plain height " + plain.height()
                + ", AVL height " + balanced.height());

        // ---- the classic bound: height <= 1.44 log2(n + 2) --------------------
        for (int count : new int[] {1, 2, 3, 10, 100, 1_000}) {
            Avl tree = new Avl();
            for (int key = 1; key <= count; key++) {
                tree.add(key);
            }
            double bound = 1.44 * (Math.log(count + 2) / Math.log(2));
            check(tree.height() <= Math.ceil(bound),
                    "n=" + count + " has height " + tree.height() + " within " + Math.ceil(bound));
            check(tree.isBalanced(), "and is balanced at n=" + count);
        }
        System.out.println("height bound : holds for sizes up to 1000");

        // ---- lookup and duplicates -------------------------------------------
        check(balanced.contains(1) && balanced.contains(100) && balanced.contains(50), "lookups work");
        check(!balanced.contains(0) && !balanced.contains(101) && !balanced.contains(-5), "misses work");
        balanced.add(50);
        check(balanced.size() == 100, "a duplicate is ignored");
        System.out.println("duplicates   : ignored, size stays " + balanced.size());

        // ---- deletion keeps it balanced --------------------------------------
        for (int key = 1; key <= 100; key += 2) {
            balanced.remove(key);
        }
        check(balanced.size() == 50, "half the keys are gone");
        check(balanced.isBalanced(), "and the tree is still balanced after 50 deletions");
        check(balanced.heightsAreCorrect(), "with the heights still right");
        check(balanced.isSearchTree(), "and the order preserved");
        check(!balanced.contains(1) && balanced.contains(2), "the evens remain");
        System.out.println("after deletes: size " + balanced.size() + ", height " + balanced.height());

        balanced.remove(999);
        check(balanced.size() == 50, "removing a missing key changes nothing");

        // ---- delete the root, repeatedly -------------------------------------
        Avl shrinking = new Avl();
        for (int key = 1; key <= 20; key++) {
            shrinking.add(key);
        }
        while (shrinking.size() > 0) {
            int rootKey = shrinking.rootKey();
            shrinking.remove(rootKey);
            check(shrinking.isBalanced(), "balanced after removing the root " + rootKey);
            check(shrinking.isSearchTree(), "and still ordered");
        }
        check(shrinking.height() == 0, "an empty tree has height zero");
        System.out.println("empty        : removing every root kept the tree balanced throughout");

        // ---- a deterministic pseudo-random workload --------------------------
        Avl workload = new Avl();
        java.util.Random random = new java.util.Random(42);
        java.util.Set<Integer> expected = new java.util.TreeSet<>();
        for (int i = 0; i < 2_000; i++) {
            int key = random.nextInt(1_000);
            workload.add(key);
            expected.add(key);
        }
        check(workload.size() == expected.size(), "the size matches a reference set");
        check(workload.inOrder().equals(new ArrayList<>(expected)), "and so does the contents");
        check(workload.isBalanced() && workload.heightsAreCorrect(),
                "after 2000 random inserts the tree is still sound");
        for (int key : expected) {
            if (!workload.contains(key)) {
                throw new AssertionError("lost key " + key);
            }
        }
        System.out.println("workload     : 2000 inserts into " + workload.size()
                + " keys, height " + workload.height());
        System.out.println("All checks passed.");
    }
}
