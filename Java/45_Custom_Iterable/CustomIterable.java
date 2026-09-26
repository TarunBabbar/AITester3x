import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

/**
 * 45 - Custom iteration: implement Iterator and Iterable so your own class works
 * with for-each, with the forEachRemaining default method and with streams.
 *
 * Compile and run:
 *   javac CustomIterable.java
 *   java CustomIterable
 */
public class CustomIterable {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /** A run of consecutive integers, iterated by a hand-written Iterator. */
    static final class Range implements Iterable<Integer> {
        private final int start;
        private final int count;

        Range(int start, int count) {
            this.start = start;
            this.count = count;
        }

        @Override
        public Iterator<Integer> iterator() {
            return new Iterator<>() {
                private int index;

                @Override
                public boolean hasNext() {
                    return index < count;
                }

                @Override
                public Integer next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException("the range is exhausted");
                    }
                    return start + index++;
                }
            };
        }
    }

    /** The same thing, but the iterator is produced by a stream pipeline. */
    static final class Countdown implements Iterable<Integer> {
        private final int from;

        Countdown(int from) {
            this.from = from;
        }

        @Override
        public Iterator<Integer> iterator() {
            return IntStream.iterate(from, value -> value - 1)
                    .limit(from)
                    .boxed()
                    .iterator();
        }
    }

    static <T> List<T> collect(Iterable<T> source) {
        List<T> values = new ArrayList<>();
        for (T value : source) {        // for-each works because of Iterable
            values.add(value);
        }
        return values;
    }

    public static void main(String[] args) {
        Range range = new Range(10, 4);
        check(collect(range).equals(List.of(10, 11, 12, 13)), "for-each over Range");
        System.out.println("for-each    : " + collect(range));

        // Iterable can be iterated as many times as you like: each call is a new iterator.
        check(collect(range).equals(collect(range)), "a second pass gives the same values");
        check(range.iterator() != range.iterator(), "iterator() returns a fresh object each time");

        // the explicit protocol
        Iterator<Integer> iterator = range.iterator();
        check(iterator.hasNext(), "hasNext is true at the start");
        check(iterator.next() == 10 && iterator.next() == 11, "next advances");
        while (iterator.hasNext()) {
            iterator.next();
        }
        check(!iterator.hasNext(), "exhausted");
        try {
            iterator.next();
            throw new AssertionError("next() past the end should throw");
        } catch (NoSuchElementException expected) {
            System.out.println("exhausted   : " + expected.getMessage());
        }

        // remove() is optional: the default throws
        Iterator<Integer> removable = range.iterator();
        removable.next();
        try {
            removable.remove();
            throw new AssertionError("remove should not be supported");
        } catch (UnsupportedOperationException expected) {
            System.out.println("remove()    : UnsupportedOperationException (optional operation)");
        }

        // forEachRemaining is a default method built on hasNext/next
        List<Integer> remaining = new ArrayList<>();
        Iterator<Integer> partial = range.iterator();
        partial.next();
        partial.forEachRemaining(remaining::add);
        check(remaining.equals(List.of(11, 12, 13)), "forEachRemaining skips what was consumed");
        System.out.println("remaining   : " + remaining);

        // streams: Iterable.spliterator() comes for free
        int total = StreamSupport.stream(range.spliterator(), false)
                .mapToInt(Integer::intValue)
                .sum();
        check(total == 10 + 11 + 12 + 13, "streaming over the custom Iterable");

        // empty and single-element edge cases
        check(collect(new Range(5, 0)).isEmpty(), "an empty range");
        check(collect(new Range(7, 1)).equals(List.of(7)), "a one-element range");

        Countdown countdown = new Countdown(4);
        check(collect(countdown).equals(List.of(4, 3, 2, 1)), "the stream-backed iterable");
        System.out.println("countdown   : " + collect(countdown));
        System.out.println("All checks passed.");
    }
}
