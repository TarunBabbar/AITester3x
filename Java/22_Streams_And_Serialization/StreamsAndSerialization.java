import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

/**
 * 22 - Streams and serialization: binary data with DataOutputStream, text with
 * BufferedReader/Writer, and saving objects with ObjectOutputStream.
 *
 * Compile and run:
 *   javac StreamsAndSerialization.java
 *   java StreamsAndSerialization
 */
public class StreamsAndSerialization {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static class Employee implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String name;
        private final int salary;
        private transient String cache;   // transient fields are NOT serialized

        Employee(String name, int salary) {
            this.name = name;
            this.salary = salary;
        }

        void setCache(String cache) {
            this.cache = cache;
        }

        @Override
        public String toString() {
            return name + ":" + salary + (cache == null ? " (no cache)" : " (cache=" + cache + ")");
        }
    }

    static void deleteTree(Path root) throws IOException {
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Path dir = Files.createTempDirectory("java-streams-");
        try {
            // ---- binary primitives ---------------------------------------
            Path binary = dir.resolve("numbers.bin");
            try (DataOutputStream out = new DataOutputStream(new FileOutputStream(binary.toFile()))) {
                out.writeInt(42);
                out.writeDouble(3.5);
                out.writeUTF("hello");
                out.writeBoolean(true);
            }
            try (DataInputStream in = new DataInputStream(new FileInputStream(binary.toFile()))) {
                check(in.readInt() == 42, "int round trip");
                check(Math.abs(in.readDouble() - 3.5) < 0.001, "double round trip");
                check(in.readUTF().equals("hello"), "UTF string round trip");
                check(in.readBoolean(), "boolean round trip");
            }
            store(binary);

            // ---- text streams --------------------------------------------
            Path text = dir.resolve("lines.txt");
            try (BufferedWriter writer = new BufferedWriter(new java.io.FileWriter(text.toFile()))) {
                writer.write("alpha");
                writer.newLine();
                writer.write("beta");
                writer.newLine();
            }
            List<String> lines;
            try (BufferedReader reader = new BufferedReader(new java.io.FileReader(text.toFile()))) {
                lines = reader.lines().toList();
            }
            check(lines.equals(List.of("alpha", "beta")), "text lines round trip");
            store(text);

            // ---- object serialization ------------------------------------
            Path objects = dir.resolve("employees.ser");
            Employee original = new Employee("Ada", 90_000);
            original.setCache("not saved");
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(objects.toFile()))) {
                out.writeObject(original);
                out.writeObject(List.of(new Employee("Bob", 70_000), new Employee("Cid", 80_000)));
            }
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(objects.toFile()))) {
                Employee restored = (Employee) in.readObject();
                @SuppressWarnings("unchecked")
                List<Employee> team = (List<Employee>) in.readObject();

                check(restored.toString().equals("Ada:90000 (no cache)"),
                        "the transient field came back empty");
                check(team.size() == 2 && team.get(1).toString().equals("Cid:80000 (no cache)"),
                        "a whole list survives the round trip");
                System.out.println("deserialized : " + restored);
                System.out.println("team         : " + team);
            }
            store(objects);

            System.out.println("\ntemp dir     : " + dir);
        } finally {
            deleteTree(dir);
        }
        System.out.println("All checks passed (temp tree cleaned up).");
    }

    /** Print a file's name and size, to show the streams really wrote bytes. */
    static void store(Path path) throws IOException {
        check(Files.size(path) > 0, path.getFileName() + " should not be empty");
        System.out.printf("%-14s %4d bytes%n", path.getFileName(), Files.size(path));
    }
}
