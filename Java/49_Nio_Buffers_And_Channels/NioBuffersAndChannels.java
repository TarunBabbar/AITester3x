import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;

/**
 * 49 - NIO part two: ByteBuffer mechanics (the flip trap), FileChannel reads and
 * writes, memory-mapped files and transferTo.
 *
 * Compile and run:
 *   javac NioBuffersAndChannels.java
 *   java NioBuffersAndChannels
 */
public class NioBuffersAndChannels {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static void deleteTree(Path root) throws Exception {
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("java-nio-");
        try {
            // ---- ByteBuffer mechanics ------------------------------------------
            ByteBuffer buffer = ByteBuffer.allocate(32);
            check(buffer.position() == 0 && buffer.limit() == 32 && buffer.capacity() == 32,
                    "a fresh buffer is empty but full of room");
            check(buffer.order() == ByteOrder.BIG_ENDIAN, "buffers default to big-endian");

            buffer.putInt(42);
            buffer.putDouble(3.5);
            buffer.put("hi".getBytes(StandardCharsets.UTF_8));
            check(buffer.position() == 4 + 8 + 2, "position advanced by what we wrote");
            check(buffer.remaining() == 32 - 14, "remaining shrinks as we write");

            buffer.flip();   // the step everyone forgets: write mode -> read mode
            check(buffer.position() == 0 && buffer.limit() == 14, "flip prepares the buffer for reading");
            check(buffer.getInt() == 42, "the int comes back");
            check(buffer.getDouble() == 3.5, "the double comes back");
            byte[] tail = new byte[buffer.remaining()];
            buffer.get(tail);
            check(new String(tail, StandardCharsets.UTF_8).equals("hi"), "the text comes back");
            check(!buffer.hasRemaining(), "drained");

            buffer.rewind();
            check(buffer.position() == 0 && buffer.limit() == 14, "rewind replays from the start");
            buffer.clear();
            check(buffer.position() == 0 && buffer.limit() == 32, "clear resets for writing again");

            ByteBuffer direct = ByteBuffer.allocateDirect(4);
            direct.putInt(7).flip();
            check(direct.getInt() == 7, "a direct buffer behaves the same");
            check(direct.isDirect(), "and reports itself as direct");

            ByteBuffer wrapped = ByteBuffer.wrap("hello".getBytes(StandardCharsets.UTF_8));
            check(StandardCharsets.UTF_8.decode(wrapped).toString().equals("hello"),
                    "decoding a buffer with a charset");

            // ---- FileChannel ---------------------------------------------------
            Path data = dir.resolve("data.bin");
            try (FileChannel channel = FileChannel.open(data,
                    StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE)) {

                ByteBuffer toWrite = ByteBuffer.allocate(12);
                toWrite.putInt(7).putInt(11).putInt(13);
                toWrite.flip();
                check(channel.write(toWrite) == 12, "wrote twelve bytes");
                check(channel.size() == 12, "the channel reports the size");

                ByteBuffer toRead = ByteBuffer.allocate(12);
                channel.position(0);
                check(channel.read(toRead) == 12, "read twelve bytes");
                toRead.flip();
                check(toRead.getInt() == 7 && toRead.getInt() == 11 && toRead.getInt() == 13,
                        "the values round trip");

                channel.position(channel.size());
                channel.write(ByteBuffer.wrap("tail".getBytes(StandardCharsets.UTF_8)));
                check(channel.size() == 16, "appending grew the file");
                channel.truncate(12);
                check(channel.size() == 12, "truncate cut the tail off again");
            }

            // ---- memory mapping -------------------------------------------------
            Path mappedFile = dir.resolve("mapped.bin");
            byte[] seed = "ABCDEFGHIJ".getBytes(StandardCharsets.UTF_8);
            Files.write(mappedFile, seed);

            try (FileChannel channel = FileChannel.open(mappedFile,
                    StandardOpenOption.READ, StandardOpenOption.WRITE)) {
                MappedByteBuffer mapping = channel.map(FileChannel.MapMode.READ_WRITE, 0, seed.length);
                check(mapping.get(0) == 'A', "the mapping sees the file's first byte");
                mapping.put(0, (byte) 'Z');   // absolute put: position is untouched
                check(mapping.position() == 0, "an absolute put does not move the position");
                mapping.force();              // push the change to disk
            }
            check(Files.readString(mappedFile).equals("ZBCDEFGHIJ"), "the edit reached the file");

            // ---- transferTo: the kernel copies, not your loop --------------------
            Path copy = dir.resolve("copy.bin");
            try (FileChannel source = FileChannel.open(mappedFile, StandardOpenOption.READ);
                 FileChannel target = FileChannel.open(copy,
                         StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
                long moved = source.transferTo(0, source.size(), target);
                check(moved == seed.length, "every byte was transferred");
            }
            check(Files.readString(copy).equals("ZBCDEFGHIJ"), "the copy matches the original");

            System.out.println("buffer flip : 14 bytes written, read back as int, double and text");
            System.out.println("file channel: " + Files.size(data) + " bytes");
            System.out.println("mapping     : " + Files.readString(mappedFile));
        } finally {
            deleteTree(dir);
        }
        System.out.println("All checks passed (temp tree cleaned up).");
    }
}
