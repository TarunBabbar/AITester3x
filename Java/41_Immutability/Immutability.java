import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 41 - Immutability: defensive copies in records, the difference between a copy
 * and an unmodifiable view, and why arrays and mutable fields need extra care.
 *
 * Compile and run:
 *   javac Immutability.java
 *   java Immutability
 */
public class Immutability {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /** The compact constructor copies on the way in, so callers cannot mutate us. */
    record Team(String name, List<String> members) {
        Team {
            members = List.copyOf(members);
        }

        Team add(String member) {
            List<String> bigger = new ArrayList<>(members);
            bigger.add(member);
            return new Team(name, bigger);   // a new value, not a mutation
        }
    }

    /** Arrays are mutable, so they must be copied in AND out. */
    record Bounds(int[] range) {
        Bounds {
            range = range.clone();
        }

        @Override
        public int[] range() {
            return range.clone();
        }
    }

    public static void main(String[] args) {
        // ---- copying in ----------------------------------------------------
        List<String> mutable = new ArrayList<>(List.of("Alice", "Bob"));
        Team team = new Team("Eng", mutable);
        mutable.add("Mallory");
        check(team.members().size() == 2, "changing the caller's list does not change the record");
        System.out.println("team        : " + team);

        // ---- copying out ---------------------------------------------------
        try {
            team.members().add("Eve");
            throw new AssertionError("the record's list should be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            System.out.println("list is immutable: UnsupportedOperationException on add");
        }
        check(team.add("Carol").members().equals(List.of("Alice", "Bob", "Carol")),
                "an 'add' returns a new record and leaves the original alone");
        check(team.members().equals(List.of("Alice", "Bob")), "the original is unchanged");

        // ---- copy versus view ----------------------------------------------
        List<String> backing = new ArrayList<>(List.of("a", "b"));
        List<String> view = Collections.unmodifiableList(backing);
        List<String> copy = List.copyOf(backing);
        backing.add("c");
        check(view.size() == 3, "an unmodifiable VIEW reflects later changes to its backing list");
        check(copy.size() == 2, "a COPY does not");
        try {
            view.add("d");
            throw new AssertionError("the view is read-only");
        } catch (UnsupportedOperationException expected) {
            check(backing.size() == 3, "the backing list was not touched");
        }
        System.out.println("backing=" + backing + " view=" + view + " copy=" + copy);

        Map<String, Integer> scores = new HashMap<>(Map.of("a", 1));
        Map<String, Integer> scoresView = Collections.unmodifiableMap(scores);
        Map<String, Integer> scoresCopy = Map.copyOf(scores);
        scores.put("b", 2);
        check(scoresView.size() == 2 && scoresCopy.size() == 1, "same rule for maps");

        // ---- arrays need cloning on both sides -----------------------------
        int[] source = {1, 2, 3};
        Bounds bounds = new Bounds(source);
        source[0] = 99;
        check(Arrays.equals(bounds.range(), new int[] {1, 2, 3}), "the array was copied in");

        int[] handedOut = bounds.range();
        handedOut[0] = 42;
        check(Arrays.equals(bounds.range(), new int[] {1, 2, 3}), "the array was copied out");

        // A record's generated equals() is shallow: array components compare by identity.
        check(!new Bounds(new int[] {1, 2}).equals(new Bounds(new int[] {1, 2})),
                "record equals does not compare array contents");
        check(Arrays.equals(new Bounds(new int[] {1, 2}).range(), new Bounds(new int[] {1, 2}).range()),
                "compare array components with Arrays.equals instead");

        // ---- the standard library's own immutable factories -----------------
        List<String> fixed = List.of("x", "y");
        try {
            fixed.add("z");
            throw new AssertionError("List.of is immutable");
        } catch (UnsupportedOperationException expected) {
            System.out.println("List.of     : immutable as expected");
        }

        // ---- strings are immutable; builders are not ------------------------
        String text = "abc";
        String upper = text.toUpperCase();
        check(text.equals("abc") && upper.equals("ABC"), "toUpperCase returns a new string");
        StringBuilder builder = new StringBuilder("abc");
        builder.append("d");
        check(builder.toString().equals("abcd"), "the builder itself changed");
        System.out.println("All checks passed.");
    }
}
