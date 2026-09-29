import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 62 - Cloning techniques: why a shallow clone is usually a bug, and the four
 * ways to get a real copy - copy constructor, deep clone, serialization, or
 * making the type immutable so copying is unnecessary.
 *
 * Compile and run:
 *   javac CloningTechniques.java
 *   java CloningTechniques
 */
public class CloningTechniques {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /** A mutable field is exactly what makes a shallow copy dangerous. */
    static class Team implements Cloneable {
        String name;
        List<String> members;

        Team(String name, List<String> members) {
            this.name = name;
            this.members = members;
        }

        /** Copy constructor: the usual recommendation. */
        Team(Team other) {
            this(other.name, new ArrayList<>(other.members));
        }

        @Override
        public Team clone() {
            try {
                return (Team) super.clone();     // SHALLOW: members is shared
            } catch (CloneNotSupportedException impossible) {
                throw new AssertionError(impossible);
            }
        }

        Team deepCopy() {
            return new Team(name, new ArrayList<>(members));
        }

        @Override
        public String toString() {
            return name + members;
        }
    }

    /** Calling super.clone() without implementing Cloneable is an error. */
    static class NotCloneable {
        @Override
        protected Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }

    static final class Config implements Serializable {
        private static final long serialVersionUID = 1L;

        String name;
        List<String> options;

        Config(String name, List<String> options) {
            this.name = name;
            this.options = options;
        }

        /** The blunt instrument: serialize and read back for a deep copy. */
        Config deepCopy() throws Exception {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
                out.writeObject(this);
            }
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
                return (Config) in.readObject();
            }
        }
    }

    /** Immutability sidesteps cloning entirely: sharing is safe. */
    record ImmutableTeam(String name, List<String> members) {
        ImmutableTeam {
            members = List.copyOf(members);   // copied in, and unmodifiable
        }
    }

    public static void main(String[] args) throws Exception {
        // ---- shallow clone shares the mutable field --------------------------
        Team original = new Team("A", new ArrayList<>(List.of("alice")));
        Team shallow = original.clone();
        check(shallow != original, "clone() does make a new object");
        check(shallow.name.equals(original.name), "and copies the fields");
        check(shallow.members == original.members, "but the list is THE SAME object");

        shallow.members.add("bob");
        check(original.members.size() == 2, "so editing the copy changed the original");
        System.out.println("shallow     : original=" + original + " copy=" + shallow);

        // ---- a deep copy breaks the link -------------------------------------
        Team deep = original.deepCopy();
        check(deep.members != original.members, "a deep copy has its own list");
        deep.members.add("carol");
        check(original.members.size() == 2, "editing the deep copy left the original alone");
        System.out.println("deep copy   : original=" + original + " copy=" + deep);

        // ---- copy constructor -------------------------------------------------
        Team copied = new Team(original);
        check(copied.members != original.members, "the copy constructor takes a fresh list");
        check(copied.members.equals(original.members), "with the same contents");
        copied.members.add("dave");
        check(original.members.size() == 2, "and no sharing either");
        System.out.println("copy ctor   : " + copied);

        // ---- clone() without Cloneable throws --------------------------------
        try {
            new NotCloneable().clone();
            throw new AssertionError("clone() without Cloneable should fail");
        } catch (CloneNotSupportedException expected) {
            System.out.println("no Cloneable: " + expected);
        }

        // ---- deep copy through serialization ---------------------------------
        Config config = new Config("report", new ArrayList<>(List.of("csv", "json")));
        Config configCopy = config.deepCopy();
        check(configCopy != config, "serialization produced a new object");
        check(configCopy.options != config.options, "and a genuinely separate list");
        check(configCopy.options.equals(List.of("csv", "json")), "with the same contents");
        configCopy.options.add("xml");
        check(config.options.size() == 2, "so the original is untouched");
        System.out.println("serialized  : " + configCopy.options);
        // Serialization is slow and needs every field to be Serializable, but it
        // handles arbitrarily deep graphs without anyone writing copy code.

        // ---- arrays -----------------------------------------------------------
        int[] numbers = {1, 2, 3};
        int[] numbersCopy = numbers.clone();
        numbersCopy[0] = 99;
        check(numbers[0] == 1, "an array clone is independent");
        check(numbersCopy.length == 3, "with the same length");

        int[][] grid = {{1, 2}, {3, 4}};
        int[][] gridCopy = grid.clone();
        check(gridCopy != grid, "the outer array is a new object");
        check(gridCopy[0] == grid[0], "but the row arrays are the SAME references");
        gridCopy[0][0] = 99;
        check(grid[0][0] == 99, "so a cell written through the copy shows up in the original");
        System.out.println("2d clone    : grid=" + Arrays.deepToString(grid));
        gridCopy[0] = new int[] {7, 8};
        check(grid[0][0] == 99, "replacing a whole row only rewrites the outer array");
        check(gridCopy[0][0] == 7, "and the copy now points at a different row");
        System.out.println("after replace: grid=" + Arrays.deepToString(grid)
                + " copy=" + Arrays.deepToString(gridCopy));

        // ---- immutability: no copy needed ------------------------------------
        List<String> source = new ArrayList<>(List.of("x", "y"));
        ImmutableTeam fixed = new ImmutableTeam("B", source);
        source.add("z");
        check(fixed.members().size() == 2, "the record copied the list in");
        try {
            fixed.members().add("nope");
            throw new AssertionError("the list should be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            System.out.println("immutable   : sharing is safe, the list cannot change");
        }
        System.out.println("All checks passed.");
    }
}
