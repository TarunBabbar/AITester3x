import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 68 - Compression: GZIP for a single stream, ZIP for a bundle of named entries,
 * and what happens when the data has nothing to compress.
 *
 * Compile and run:
 *   javac Compression.java
 *   java Compression
 */
public class Compression {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static byte[] gzip(String text) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(buffer)) {
            gzip.write(text.getBytes(StandardCharsets.UTF_8));
        }
        return buffer.toByteArray();
    }

    static String gunzip(byte[] compressed) throws IOException {
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            return new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    static byte[] toZip(Map<String, String> entries) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(buffer)) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return buffer.toByteArray();
    }

    static Map<String, String> fromZip(byte[] archive) throws IOException {
        Map<String, String> entries = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                entries.put(entry.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return entries;
    }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("java-compress-");
        try {
            // ---- GZIP round trip ------------------------------------------------
            String repetitive = "the quick brown fox jumps over the lazy dog. ".repeat(400);
            byte[] packed = gzip(repetitive);
            check((packed[0] & 0xFF) == 0x1F && (packed[1] & 0xFF) == 0x8B,
                    "a gzip stream starts with the magic bytes 1f 8b");
            check(gunzip(packed).equals(repetitive), "the text survives the round trip");
            check(packed.length < repetitive.length() / 4,
                    "repetitive text compresses to well under a quarter of its size");
            System.out.printf("gzip        : %d -> %d bytes (%.1f%%)%n",
                    repetitive.length(), packed.length,
                    100.0 * packed.length / repetitive.length());

            // ---- the same data compacted by deflate, not stored ----------------
            check(gunzip(gzip("")) .isEmpty(), "an empty stream round trips");
            check(gunzip(gzip("a")).equals("a"), "a single character round trips");
            check(gunzip(packed).length() == repetitive.length(), "the length is preserved");

            // ---- data with nothing to compress --------------------------------
            byte[] noise = new byte[4096];
            new Random(1).nextBytes(noise);
            ByteArrayOutputStream noiseBuffer = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(noiseBuffer)) {
                gzip.write(noise);
            }
            byte[] packedNoise = noiseBuffer.toByteArray();
            check(packedNoise.length > noise.length * 0.98,
                    "incompressible data does not shrink - gzip adds its own overhead");
            try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(packedNoise))) {
                check(java.util.Arrays.equals(gzip.readAllBytes(), noise), "and still round trips exactly");
            }
            System.out.printf("random      : %d -> %d bytes (no gain)%n", noise.length, packedNoise.length);

            // ---- ZIP: several named entries in one archive ---------------------
            Map<String, String> entries = new LinkedHashMap<>();
            entries.put("summary.txt", "all tests passed\n");
            entries.put("docs/plan.md", "# plan\nstep one\n");
            entries.put("data/nested/deep.txt", "nested entry\n");

            byte[] archive = toZip(entries);
            check(archive.length > 0, "the archive has bytes");
            Map<String, String> readBack = fromZip(archive);
            check(readBack.equals(entries), "every entry round trips, names included");
            check(readBack.keySet().toString().equals("[summary.txt, docs/plan.md, data/nested/deep.txt]"),
                    "the order is preserved by ZipOutputStream");
            System.out.println("zip entries : " + readBack.keySet());

            // ---- a ZIP on disk, read at random ---------------------------------
            Path bundle = dir.resolve("bundle.zip");
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(bundle))) {
                zip.putNextEntry(new ZipEntry("alpha.txt"));
                zip.write("alpha".getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
                zip.putNextEntry(new ZipEntry("beta.txt"));
                zip.write("beta".getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }

            try (ZipFile zip = new ZipFile(bundle.toFile())) {
                check(zip.size() == 2, "two entries on disk");
                check(zip.getEntry("beta.txt") != null, "entries can be found by name");
                check(zip.getEntry("missing.txt") == null, "and a missing name is simply null");
                try (InputStream in = zip.getInputStream(zip.getEntry("alpha.txt"))) {
                    check(new String(in.readAllBytes(), StandardCharsets.UTF_8).equals("alpha"),
                            "one entry can be read without touching the others");
                }
                Enumeration<? extends ZipEntry> names = zip.entries();
                int counted = 0;
                while (names.hasMoreElements()) {
                    names.nextElement();
                    counted++;
                }
                check(counted == 2, "iterating the entries agrees with size()");
            }
            System.out.printf("zip on disk : %s, %d bytes%n", bundle.getFileName(), Files.size(bundle));
        } finally {
            try (var paths = Files.walk(dir)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
        System.out.println("All checks passed.");
    }
}
