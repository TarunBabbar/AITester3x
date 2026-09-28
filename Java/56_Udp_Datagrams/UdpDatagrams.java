import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 56 - UDP datagrams: connectionless send and receive on localhost, the receive
 * timeout that stands in for "no connection", and silent truncation.
 *
 * Compile and run:
 *   javac UdpDatagrams.java
 *   java UdpDatagrams
 */
public class UdpDatagrams {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static final InetAddress LOOPBACK;

    static {
        try {
            LOOPBACK = InetAddress.getByName("127.0.0.1");
        } catch (Exception impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
    }

    static String text(DatagramPacket packet) {
        return new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
    }

    public static void main(String[] args) throws Exception {
        ExecutorService workers = Executors.newCachedThreadPool();
        try {
            // ---- a request and its reply ------------------------------------
            try (DatagramSocket server = new DatagramSocket(0, LOOPBACK)) {
                int port = server.getLocalPort();
                System.out.println("udp server on " + LOOPBACK.getHostAddress() + ":" + port);

                Future<String> served = workers.submit(() -> {
                    byte[] buffer = new byte[1024];
                    DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);
                    server.receive(incoming);          // waits for any packet
                    String request = text(incoming);
                    byte[] reply = ("echo: " + request).getBytes(StandardCharsets.UTF_8);
                    // UDP carries the sender's address and port on each packet:
                    // there is no connection to remember them.
                    server.send(new DatagramPacket(reply, reply.length,
                            incoming.getAddress(), incoming.getPort()));
                    return request;
                });

                try (DatagramSocket client = new DatagramSocket()) {
                    client.setSoTimeout(3000);         // else receive() blocks forever
                    byte[] payload = "hello udp".getBytes(StandardCharsets.UTF_8);
                    client.send(new DatagramPacket(payload, payload.length, LOOPBACK, port));

                    byte[] buffer = new byte[1024];
                    DatagramPacket response = new DatagramPacket(buffer, buffer.length);
                    client.receive(response);
                    check(text(response).equals("echo: hello udp"), "the reply arrived");
                    check(response.getPort() == port, "it came from the server's port");
                    check(response.getAddress().equals(LOOPBACK), "and from the loopback address");
                    System.out.println("client got  : " + text(response)
                            + " (" + response.getLength() + " bytes)");
                }
                check(served.get(5, TimeUnit.SECONDS).equals("hello udp"), "the server saw the request");
            }

            // ---- receive without a connection times out ---------------------
            try (DatagramSocket idle = new DatagramSocket(0, LOOPBACK)) {
                idle.setSoTimeout(300);
                DatagramPacket nothing = new DatagramPacket(new byte[32], 32);
                long start = System.nanoTime();
                try {
                    idle.receive(nothing);
                    throw new AssertionError("receive should have timed out");
                } catch (SocketTimeoutException expected) {
                    long waited = (System.nanoTime() - start) / 1_000_000;
                    check(waited >= 250, "it really waited for the timeout");
                    System.out.println("timeout     : receive gave up after " + waited + " ms");
                }
            }

            // ---- an oversized datagram is silently truncated ---------------
            try (DatagramSocket small = new DatagramSocket(0, LOOPBACK)) {
                int port = small.getLocalPort();
                Future<Integer> received = workers.submit(() -> {
                    DatagramPacket tiny = new DatagramPacket(new byte[8], 8);
                    small.receive(tiny);
                    return tiny.getLength();
                });

                try (DatagramSocket client = new DatagramSocket()) {
                    byte[] large = "0123456789ABCDEF".getBytes(StandardCharsets.UTF_8);   // 16 bytes
                    client.send(new DatagramPacket(large, large.length, LOOPBACK, port));
                }
                check(received.get(5, TimeUnit.SECONDS) == 8,
                        "only what fits in the buffer survives, with no error");
                System.out.println("truncation  : a 16-byte datagram arrived as 8 bytes");
            }
        } finally {
            workers.shutdown();
        }
        System.out.println("All checks passed.");
    }
}
