import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 31 - Data structures from scratch: a growable array and a singly linked list,
 * then the growable array reused as a stack.
 *
 * Compile and run:
 *   javac DataStructures.java
 *   java DataStructures
 */
public class DataStructures {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /** A generic array-backed list that doubles its capacity when it fills up. */
    static class DynamicArray<T> {
        private Object[] elements;
        private int size;

        DynamicArray() {
            this(2);
        }

        DynamicArray(int initialCapacity) {
            elements = new Object[Math.max(1, initialCapacity)];
        }

        int size() {
            return size;
        }

        int capacity() {
            return elements.length;
        }

        void add(T value) {
            if (size == elements.length) {
                grow();
            }
            elements[size++] = value;
        }

        @SuppressWarnings("unchecked")
        T get(int index) {
            checkIndex(index);
            return (T) elements[index];
        }

        void set(int index, T value) {
            checkIndex(index);
            elements[index] = value;
        }

        @SuppressWarnings("unchecked")
        T remove(int index) {
            checkIndex(index);
            T removed = (T) elements[index];
            System.arraycopy(elements, index + 1, elements, index, size - index - 1);
            elements[--size] = null;   // let the garbage collector take it
            return removed;
        }

        int indexOf(T value) {
            for (int i = 0; i < size; i++) {
                if (Objects.equals(elements[i], value)) {
                    return i;
                }
            }
            return -1;
        }

        private void grow() {
            Object[] bigger = new Object[elements.length * 2];
            System.arraycopy(elements, 0, bigger, 0, size);
            elements = bigger;
        }

        private void checkIndex(int index) {
            if (index < 0 || index >= size) {
                throw new IndexOutOfBoundsException("index " + index + ", size " + size);
            }
        }

        @Override
        public String toString() {
            return toArrayList().toString();
        }

        List<T> toArrayList() {
            List<T> copy = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                copy.add(get(i));
            }
            return copy;
        }
    }

    /** A singly linked list keeping head, tail and size in step. */
    static class LinkedList<T> {
        private static final class Node<T> {
            T value;
            Node<T> next;

            Node(T value) {
                this.value = value;
            }
        }

        private Node<T> head;
        private Node<T> tail;
        private int size;

        void addLast(T value) {
            Node<T> node = new Node<>(value);
            if (tail == null) {
                head = tail = node;
            } else {
                tail.next = node;
                tail = node;
            }
            size++;
        }

        void addFirst(T value) {
            Node<T> node = new Node<>(value);
            node.next = head;
            head = node;
            if (tail == null) {
                tail = node;
            }
            size++;
        }

        boolean remove(T value) {
            Node<T> previous = null;
            Node<T> current = head;
            while (current != null) {
                if (Objects.equals(current.value, value)) {
                    if (previous == null) {
                        head = current.next;
                    } else {
                        previous.next = current.next;
                    }
                    if (current == tail) {
                        tail = previous;
                    }
                    size--;
                    return true;
                }
                previous = current;
                current = current.next;
            }
            return false;
        }

        void reverse() {
            Node<T> previous = null;
            Node<T> current = head;
            tail = head;
            while (current != null) {
                Node<T> next = current.next;
                current.next = previous;
                previous = current;
                current = next;
            }
            head = previous;
        }

        int size() {
            return size;
        }

        List<T> toList() {
            List<T> values = new ArrayList<>();
            for (Node<T> node = head; node != null; node = node.next) {
                values.add(node.value);
            }
            return values;
        }
    }

    public static void main(String[] args) {
        DynamicArray<String> names = new DynamicArray<>();
        check(names.capacity() == 2, "starts with capacity 2");
        names.add("a");
        names.add("b");
        names.add("c");
        check(names.size() == 3 && names.capacity() == 4, "capacity doubled on the third add");
        check(names.get(1).equals("b"), "indexed access");
        names.set(1, "B");
        check(names.get(1).equals("B"), "replace");
        check(names.indexOf("c") == 2 && names.indexOf("zz") == -1, "indexOf");
        check(names.remove(0).equals("a"), "remove returns the removed value");
        check(names.toString().equals("[B, c]"), "after removal");
        try {
            names.get(5);
            throw new AssertionError("out of bounds should fail");
        } catch (IndexOutOfBoundsException expected) {
            System.out.println("bounds       : " + expected.getMessage());
        }

        DynamicArray<String> stack = new DynamicArray<>();
        stack.add("first");
        stack.add("second");
        stack.add("third");
        check(stack.remove(stack.size() - 1).equals("third"), "stack pops the last in");
        System.out.println("dynamic array: " + names + " (capacity " + names.capacity() + ")");
        System.out.println("stack after pop: " + stack);

        LinkedList<Integer> linked = new LinkedList<>();
        for (int value = 1; value <= 5; value++) {
            linked.addLast(value);
        }
        linked.addFirst(0);
        check(linked.toList().equals(List.of(0, 1, 2, 3, 4, 5)), "linked list built");
        check(linked.remove(3), "remove finds the value");
        check(!linked.remove(99), "removing a missing value returns false");
        check(linked.toList().equals(List.of(0, 1, 2, 4, 5)), "after removing 3");
        linked.reverse();
        check(linked.toList().equals(List.of(5, 4, 2, 1, 0)), "reversed");
        linked.addLast(9);
        check(linked.toList().equals(List.of(5, 4, 2, 1, 0, 9)), "tail still valid after a reverse");
        System.out.println("linked list  : " + linked.toList() + " (size " + linked.size() + ")");
        System.out.println("All checks passed.");
    }
}
