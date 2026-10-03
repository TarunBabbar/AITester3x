import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * 96 - A Merkle tree: each leaf hashes a block, each parent hashes its two
 * children, and the root summarises the whole set. A single leaf can be proved
 * to belong using only log(n) sibling hashes.
 *
 * Compile and run:
 *   javac MerkleTree.java
 *   java MerkleTree
 */
public class MerkleTree {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    record Proof(List<byte[]> siblings, List<Boolean> siblingOnLeft) {
    }

    static final class Tree {
        /** levels.get(0) is the leaves, the last level is the root. */
        private final List<List<byte[]>> levels = new ArrayList<>();

        Tree(List<String> blocks) {
            List<byte[]> leaves = new ArrayList<>();
            for (String block : blocks) {
                leaves.add(hash(block.getBytes(StandardCharsets.UTF_8)));
            }
            levels.add(leaves);

            List<byte[]> current = leaves;
            while (current.size() > 1) {
                List<byte[]> parents = new ArrayList<>();
                for (int i = 0; i < current.size(); i += 2) {
                    byte[] left = current.get(i);
                    // An odd node out is paired with itself, so the shape stays a tree.
                    byte[] right = i + 1 < current.size() ? current.get(i + 1) : current.get(i);
                    parents.add(combine(left, right));
                }
                levels.add(parents);
                current = parents;
            }
        }

        byte[] root() {
            List<byte[]> top = levels.get(levels.size() - 1);
            return top.isEmpty() ? hash(new byte[0]) : top.get(0);
        }

        int leafCount() {
            return levels.get(0).size();
        }

        int height() {
            return levels.size();
        }

        byte[] leaf(int index) {
            return levels.get(0).get(index);
        }

        /** The sibling hashes needed to walk from a leaf up to the root. */
        Proof proofFor(int index) {
            if (index < 0 || index >= leafCount()) {
                throw new IndexOutOfBoundsException("there is no leaf " + index);
            }
            List<byte[]> siblings = new ArrayList<>();
            List<Boolean> siblingOnLeft = new ArrayList<>();
            int position = index;

            for (int level = 0; level < levels.size() - 1; level++) {
                List<byte[]> nodes = levels.get(level);
                int sibling = position % 2 == 0 ? position + 1 : position - 1;
                if (sibling >= nodes.size()) {
                    sibling = position;      // an odd tail is its own sibling
                }
                siblings.add(nodes.get(sibling));
                siblingOnLeft.add(position % 2 == 1);
                position /= 2;
            }
            return new Proof(siblings, siblingOnLeft);
        }

        /** Recompute the root from one leaf hash and the proof, and compare. */
        static boolean verify(byte[] leafHash, Proof proof, byte[] root) {
            byte[] current = leafHash;
            for (int i = 0; i < proof.siblings().size(); i++) {
                byte[] sibling = proof.siblings().get(i);
                current = proof.siblingOnLeft().get(i)
                        ? combine(sibling, current)
                        : combine(current, sibling);
            }
            return MessageDigest.isEqual(current, root);
        }
    }

    static byte[] hash(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (Exception impossible) {
            throw new IllegalStateException("SHA-256 is always available", impossible);
        }
    }

    /** Hash the pair with a marker byte, so ordering cannot be ambiguous. */
    static byte[] combine(byte[] left, byte[] right) {
        byte[] joined = new byte[left.length + 1 + right.length];
        System.arraycopy(left, 0, joined, 0, left.length);
        joined[left.length] = 0x01;
        System.arraycopy(right, 0, joined, left.length + 1, right.length);
        return hash(joined);
    }

    static byte[] leafHash(String block) {
        return hash(block.getBytes(StandardCharsets.UTF_8));
    }

    static String hex(byte[] data) {
        return HexFormat.of().formatHex(data);
    }

    static List<String> blocks(int count) {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            values.add("transaction-" + i);
        }
        return values;
    }

    public static void main(String[] args) {
        List<String> ledger = List.of("alice pays bob 10", "bob pays carol 4",
                "carol pays dan 7", "dan pays alice 2");
        Tree tree = new Tree(ledger);

        check(tree.leafCount() == 4, "four leaves");
        check(tree.height() == 3, "three levels: four leaves, two parents, one root");
        check(tree.root().length == 32, "SHA-256 gives a 32 byte root");
        System.out.println("root         : " + hex(tree.root()));

        // ---- every leaf can prove itself -------------------------------------
        for (int index = 0; index < tree.leafCount(); index++) {
            Proof proof = tree.proofFor(index);
            check(proof.siblings().size() == 2, "a proof for four leaves is two hashes");
            check(Tree.verify(tree.leaf(index), proof, tree.root()),
                    "leaf " + index + " verifies against the root");
        }
        System.out.println("proofs       : all four leaves verify with two hashes each");

        // ---- a proof reveals nothing else ------------------------------------
        Proof proof = tree.proofFor(1);
        check(proof.siblings().size() < tree.leafCount() - 1,
                "the proof is smaller than the rest of the data: "
                        + proof.siblings().size() + " hashes for " + tree.leafCount() + " leaves");

        // ---- the wrong leaf fails --------------------------------------------
        check(!Tree.verify(tree.leaf(0), tree.proofFor(1), tree.root()),
                "leaf 0's hash does not satisfy leaf 1's proof");
        check(!Tree.verify(leafHash("alice pays bob 11"), tree.proofFor(0), tree.root()),
                "a changed amount does not verify either");
        System.out.println("rejections   : the wrong leaf and a changed amount both fail");

        // ---- tampering changes the root --------------------------------------
        List<String> tampered = new ArrayList<>(ledger);
        tampered.set(2, "carol pays dan 7000");
        Tree afterTampering = new Tree(tampered);
        check(!MessageDigest.isEqual(afterTampering.root(), tree.root()),
                "editing one block changes the root");
        check(!Tree.verify(tree.leaf(0), tree.proofFor(0), afterTampering.root()),
                "so the old proof no longer fits the new root");
        System.out.println("tampering    : one edited block moves the root");

        // ---- order matters ----------------------------------------------------
        Tree swapped = new Tree(List.of("bob pays carol 4", "alice pays bob 10",
                "carol pays dan 7", "dan pays alice 2"));
        check(!MessageDigest.isEqual(swapped.root(), tree.root()),
                "swapping two blocks changes the root, so order is committed to");

        // ---- duplicate content still hashes distinctly -----------------------
        Tree repeated = new Tree(List.of("same", "same", "same", "same"));
        check(repeated.leafCount() == 4, "four identical leaves");
        check(MessageDigest.isEqual(repeated.leaf(0), repeated.leaf(1)),
                "identical blocks hash to identical leaves");
        check(Tree.verify(repeated.leaf(2), repeated.proofFor(2), repeated.root()),
                "and the proofs still hold");

        // ---- single block and empty ------------------------------------------
        Tree single = new Tree(List.of("only"));
        check(single.leafCount() == 1 && single.height() == 1, "one leaf, one level");
        check(MessageDigest.isEqual(single.root(), single.leaf(0)), "the root is the leaf");
        check(single.proofFor(0).siblings().isEmpty(), "and the proof is empty");
        check(Tree.verify(single.leaf(0), single.proofFor(0), single.root()),
                "which trivially verifies");

        Tree empty = new Tree(List.of());
        check(empty.leafCount() == 0, "no leaves");
        check(empty.root().length == 32, "an empty tree still has a root: the hash of nothing");
        check(MessageDigest.isEqual(empty.root(), hash(new byte[0])), "which is the empty hash");
        check(!MessageDigest.isEqual(empty.root(), single.root()), "and it differs from a one block tree");

        // ---- odd counts -------------------------------------------------------
        for (int count : new int[] {2, 3, 5, 7, 9, 33}) {
            Tree odd = new Tree(blocks(count));
            for (int index = 0; index < count; index++) {
                check(Tree.verify(odd.leaf(index), odd.proofFor(index), odd.root()),
                        "leaf " + index + " of " + count + " verifies");
            }
        }
        System.out.println("odd counts   : trees of 2, 3, 5, 7, 9 and 33 leaves all verify");

        // ---- the proof stays small -------------------------------------------
        Tree large = new Tree(blocks(1_000));
        check(large.leafCount() == 1_000, "a thousand blocks");
        check(large.proofFor(500).siblings().size() == 10,
                "and a proof is still only ten hashes");
        check(Tree.verify(large.leaf(500), large.proofFor(500), large.root()),
                "which verifies");
        System.out.println("scale        : 1,000 blocks, proofs of 10 hashes, height "
                + large.height());

        // ---- out of range -----------------------------------------------------
        try {
            tree.proofFor(4);
            throw new AssertionError("there is no leaf 4");
        } catch (IndexOutOfBoundsException expected) {
            System.out.println("bounds       : " + expected.getMessage());
        }
        System.out.println("All checks passed.");
    }
}
