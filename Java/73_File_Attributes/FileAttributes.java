import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileVisitResult;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 73 - File attributes: basic timestamps and sizes, the platform specific views
 * (DOS on Windows, POSIX elsewhere), the file store, and walking a tree.
 *
 * Compile and run:
 *   javac FileAttributes.java
 *   java FileAttributes
 */
public class FileAttributes {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("java-attrs-");
        try {
            String contents = "hello attributes\n";
            Path file = dir.resolve("note.txt");
            Files.writeString(file, contents);

            // ---- basic attributes --------------------------------------------
            BasicFileAttributes basic = Files.readAttributes(file, BasicFileAttributes.class);
            check(basic.isRegularFile() && !basic.isDirectory(), "it is a regular file");
            check(basic.size() == contents.length(), "the size matches what we wrote");
            check(!basic.isSymbolicLink(), "not a link");
            check(basic.lastModifiedTime().toMillis() > 0, "the last modified time is set");
            check(basic.creationTime().toInstant().isBefore(Instant.now().plusSeconds(5)),
                    "the creation time is not in the future");
            check(Files.isReadable(file) && Files.isWritable(file), "readable and writable");
            check(Files.isDirectory(dir), "the temp entry really is a directory");
            check(Files.exists(file) && !Files.notExists(file), "exists and notExists disagree, as they should");
            System.out.println("basic       : size=" + basic.size() + " created=" + basic.creationTime());

            // ---- which views does this filesystem support? -------------------
            FileStore store = Files.getFileStore(file);
            boolean dosSupported = store.supportsFileAttributeView("dos");
            boolean posixSupported = store.supportsFileAttributeView("posix");
            System.out.println("views       : dos=" + dosSupported + " posix=" + posixSupported);
            check(dosSupported || posixSupported,
                    "a real filesystem offers at least one of these views");

            if (dosSupported) {
                DosFileAttributes dos = Files.readAttributes(file, DosFileAttributes.class);
                check(!dos.isDirectory(), "the DOS view agrees it is a file");
                boolean originalReadOnly = dos.isReadOnly();
                Files.setAttribute(file, "dos:readonly", true);
                check(Files.readAttributes(file, DosFileAttributes.class).isReadOnly(),
                        "the read-only flag can be set");
                Files.setAttribute(file, "dos:readonly", originalReadOnly);
                check(Files.readAttributes(file, DosFileAttributes.class).isReadOnly() == originalReadOnly,
                        "and restored");
                System.out.println("dos         : readOnly=" + originalReadOnly
                        + " hidden=" + dos.isHidden() + " archive=" + dos.isArchive());
            }

            if (posixSupported) {
                PosixFileAttributes posix = Files.readAttributes(file, PosixFileAttributes.class);
                check(!posix.permissions().isEmpty(), "POSIX permissions are present");
                System.out.println("posix       : " + PosixFilePermissions.toString(posix.permissions())
                        + " owner=" + posix.owner().getName());
            } else {
                System.out.println("posix       : unsupported on this filesystem, so the view is skipped");
            }

            // ---- setting a timestamp ----------------------------------------
            Instant fixed = Instant.parse("2026-01-05T09:00:00Z");
            Files.setLastModifiedTime(file, FileTime.from(fixed));
            check(Files.getLastModifiedTime(file).toInstant().equals(fixed),
                    "the modified time survives a round trip");
            System.out.println("mtime       : set to " + Files.getLastModifiedTime(file));

            // ---- the file store ----------------------------------------------
            check(store.getTotalSpace() > 0, "the store reports a total size");
            check(store.getUsableSpace() >= 0 && store.getUsableSpace() <= store.getTotalSpace(),
                    "usable space is inside the total");
            check(!store.name().isEmpty() && !store.type().isEmpty(), "the store has a name and a type");
            check(!store.isReadOnly() || store.getTotalSpace() > 0, "read-only stores still report a size");
            System.out.printf("store       : %s (%s) total %.1f GB, usable %.1f GB%n",
                    store.name(), store.type(),
                    store.getTotalSpace() / 1_073_741_824.0, store.getUsableSpace() / 1_073_741_824.0);

            // ---- walking the tree --------------------------------------------
            Files.createDirectories(dir.resolve("sub/deeper"));
            Files.writeString(dir.resolve("sub/a.txt"), "a");
            Files.writeString(dir.resolve("sub/deeper/b.txt"), "b");
            Files.writeString(dir.resolve("sub/deeper/c.log"), "c");

            AtomicInteger files = new AtomicInteger();
            AtomicInteger directories = new AtomicInteger();
            AtomicLong totalBytes = new AtomicLong();

            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attrs) {
                    directories.incrementAndGet();
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                    files.incrementAndGet();
                    totalBytes.addAndGet(attrs.size());
                    return FileVisitResult.CONTINUE;
                }
            });

            check(files.get() == 4, "four files: note.txt plus three under sub/");
            check(directories.get() == 3, "three directories: the root, sub and sub/deeper");
            check(totalBytes.get() == contents.length() + 3, "the byte counts add up");
            System.out.println("walk        : " + files.get() + " files in " + directories.get()
                    + " directories, " + totalBytes.get() + " bytes");

            // ---- symlinks, where the platform allows them --------------------
            try {
                Path link = dir.resolve("link.txt");
                Files.createSymbolicLink(link, file.getFileName());
                check(Files.isSymbolicLink(link), "the link is recognised");
                check(Files.readAttributes(link, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS)
                        .isSymbolicLink(), "reading without following shows a link");
                check(Files.readAttributes(link, BasicFileAttributes.class).isRegularFile(),
                        "reading with following shows the target");
                System.out.println("symlink     : created, followed and detected");
            } catch (IOException | UnsupportedOperationException denied) {
                System.out.println("symlink     : not permitted here ("
                        + denied.getClass().getSimpleName() + "), skipping that part");
            }
        } finally {
            try (var paths = Files.walk(dir)) {
                // Delete files before the directories that hold them.
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    try {
                        Files.deleteIfExists(path);
                    } catch (FileSystemException ignored) {
                        // a read-only entry left behind by the DOS test is harmless
                    }
                }
            }
        }
        System.out.println("All checks passed.");
    }
}
