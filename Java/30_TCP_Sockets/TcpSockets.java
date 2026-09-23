import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 30 - TCP sockets: a line-based echo server on localhost and a client that
 * talks to it, plus an accept timeout for when nobody connects.
 *
 * Compile and run:
 *   javac TcpSockets.java
 *   java TcpSockets
 */
public class TcpSockets {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) throws Exception {
        try (ServerSocket server = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))) {
            int port = server.getLocalPort();   // port 0 = the OS picks a free one
            System.out.println("server listening on 127.0.0.1:" + port);

            ExecutorService pool = Executors.newSingleThreadExecutor();
            Future<Integer> handled = pool.submit(() -> {
                int messages = 0;
                try (Socket connection = server.accept();
                     BufferedReader in = new BufferedReader(
                             new InputStreamReader(connection.getInputStream()));
                     PrintWriter out = new PrintWriter(connection.getOutputStream(), true)) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        if (line.equals("bye")) {
                            out.println("closing");
                            break;
                        }
                        out.println("echo: " + line);
                        messages++;
                    }
                }
                return messages;
            });

            try (Socket client = new Socket("127.0.0.1", port);
                 PrintWriter toServer = new PrintWriter(client.getOutputStream(), true);
                 BufferedReader fromServer = new BufferedReader(
                         new InputStreamReader(client.getInputStream()))) {

                toServer.println("hello");
                check(fromServer.readLine().equals("echo: hello"), "first reply");
                System.out.println("client sent hello  -> " + "echo: hello");

                toServer.println("world");
                check(fromServer.readLine().equals("echo: world"), "second reply");

                toServer.println("bye");
                check(fromServer.readLine().equals("closing"), "shutdown reply");
            }

            check(handled.get(5, TimeUnit.SECONDS) == 2, "the server echoed two messages");
            pool.shutdown();
        }

        // A short accept timeout stops a server hanging around forever.
        try (ServerSocket idle = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            idle.setSoTimeout(250);
            try {
                idle.accept();
                throw new AssertionError("accept should have timed out");
            } catch (SocketTimeoutException expected) {
                System.out.println("accept timed out after 250ms on port " + idle.getLocalPort());
            }
        }

        System.out.println("Server stopped. All checks passed.");
    }
}
