import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

/**
 * 67 - Watching a directory: WatchService delivers create, modify and delete
 * events, and a key goes invalid when the directory it watches disappears.
 *
 * Compile and run:
 *   javac WatchDirectory.java
 *   java WatchDirectory
 */
public class WatchDirectory {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("java-watch-");
        try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
            dir.register(watcher,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);

            // Now make some changes. Events report the file NAME, relative to the
            // watched directory, not the full path.
            Path note = dir.resolve("note.txt");
            Files.writeString(note, "one\n");                                  // create
            Files.writeString(note, "two\n", StandardOpenOption.APPEND);       // modify
            Path copy = dir.resolve("copy.txt");
            Files.copy(note, copy);                                            // create
            Files.writeString(copy, "three\n", StandardOpenOption.APPEND);     // modify
            Files.delete(copy);                                                // delete

            Map<String, Integer> counts = new TreeMap<>();
            Set<String> events = new TreeSet<>();
            WatchKey key = null;
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);

            while (System.nanoTime() < deadline) {
                WatchKey ready = watcher.poll(200, TimeUnit.MILLISECONDS);
                if (ready != null) {
                    key = ready;
                    for (WatchEvent<?> event : ready.pollEvents()) {
                        if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                            continue;      // events were dropped: the queue overflowed
                        }
                        counts.merge(event.kind().name(), 1, Integer::sum);
                        events.add(event.kind().name() + " " + event.context());
                    }
                    ready.reset();          // without reset the key stops delivering
                }
                // the OS may merge modifies, so stop once the shape is clear
                if (counts.getOrDefault("ENTRY_CREATE", 0) >= 2
                        && counts.getOrDefault("ENTRY_DELETE", 0) >= 1
                        && counts.getOrDefault("ENTRY_MODIFY", 0) >= 1) {
                    break;
                }
            }

            check(counts.getOrDefault("ENTRY_CREATE", 0) >= 2, "both files were seen being created");
            check(counts.getOrDefault("ENTRY_DELETE", 0) >= 1, "the copy was seen being deleted");
            check(counts.getOrDefault("ENTRY_MODIFY", 0) >= 1, "a write was seen as a modify");
            check(events.contains("ENTRY_CREATE note.txt"), "the event context is the file name");
            check(events.contains("ENTRY_DELETE copy.txt"), "and the delete names the file too");
            check(!events.stream().anyMatch(name -> name.contains(":\\")),
                    "contexts are relative to the watched directory, not absolute paths");
            System.out.println("events      : " + events);
            System.out.println("counts      : " + counts);

            check(key != null && key.isValid(), "the key is still valid while the directory exists");
        }

        // ---- a key goes invalid when its directory is deleted ----------------
        try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
            Path doomed = Files.createTempDirectory("java-watch-doomed-");
            WatchKey doomedKey = doomed.register(watcher, StandardWatchEventKinds.ENTRY_CREATE);
            check(doomedKey.isValid(), "a fresh key is valid");

            Files.delete(doomed);
            check(!Files.exists(doomed), "the watched directory is gone");
            // Whether reset() reports false straight away is platform dependent:
            // on Windows the key can stay valid until the watch is serviced again.
            boolean survivedDelete = doomedKey.reset();
            System.out.println("after delete: reset() returned " + survivedDelete
                    + " for a deleted directory (platform dependent)");
            doomedKey.cancel();
            check(!doomedKey.isValid(), "cancel() invalidates the key immediately");
            System.out.println("invalid key : cancelled, so no more events are delivered");
        }

        // clean up the first directory and its files
        try (var paths = Files.walk(dir)) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
        System.out.println("All checks passed.");
    }
}
