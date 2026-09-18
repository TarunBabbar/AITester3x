import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;

/**
 * 10 - File I/O with java.nio.file: write, read, append, create directories,
 * walk a tree and clean up afterwards.
 *
 * Compile and run:
 *   javac FileIoNio.java
 *   java FileIoNio
 */
public class FileIoNio {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static void deleteTree(Path root) throws IOException {
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        Path dir = Files.createTempDirectory("java-fileio-");
        try {
            Path note = dir.resolve("note.txt");
            Files.writeString(note, "line one\nline two\nline three\n");
            check(Files.exists(note), "the file exists after writing");
            check(Files.readString(note).lines().count() == 3, "the file has three lines");

            List<String> lines = Files.readAllLines(note);
            check(lines.get(1).equals("line two"), "the second line is correct");

            Files.writeString(note, "appended\n", StandardOpenOption.APPEND);
            check(Files.readAllLines(note).size() == 4, "appending adds a line");

            Path inner = dir.resolve("logs/inner");
            Files.createDirectories(inner);
            Files.writeString(inner.resolve("a.log"), "hello\n");
            Files.writeString(inner.resolve("b.log"), "world\n");

            try (var stream = Files.walk(dir)) {
                check(stream.filter(Files::isRegularFile).count() == 3, "three regular files");
            }

            try (var stream = Files.list(dir)) {
                List<String> names = stream.map(path -> path.getFileName().toString()).sorted().toList();
                check(names.contains("note.txt") && names.contains("logs"), "listing has entries");
            }

            long totalBytes = 0;
            try (var stream = Files.walk(dir)) {
                for (Path file : stream.filter(Files::isRegularFile).toList()) {
                    totalBytes += Files.size(file);
                }
            }
            check(totalBytes > 0, "some bytes were written");

            System.out.println("temp dir    : " + dir);
            System.out.println("note.txt    :");
            Files.readAllLines(note).forEach(line -> System.out.println("  " + line));
            System.out.println("total bytes : " + totalBytes);
        } finally {
            deleteTree(dir);
        }
        System.out.println("All checks passed (temp tree cleaned up).");
    }
}
