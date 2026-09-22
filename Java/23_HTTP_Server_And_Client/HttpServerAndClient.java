import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 23 - HTTP server and client: start a small server from the JDK on a random
 * local port, then talk to it with java.net.http.HttpClient. No internet needed.
 *
 * Compile and run:
 *   javac HttpServerAndClient.java
 *   java HttpServerAndClient
 */
public class HttpServerAndClient {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static void respond(com.sun.net.httpserver.HttpExchange exchange, String body) throws java.io.IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (var out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    public static void main(String[] args) throws Exception {
        // Port 0 lets the OS pick a free port, so the demo never clashes.
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

        server.createContext("/hello", exchange -> respond(exchange, "hello from the server"));

        server.createContext("/echo", exchange -> {
            String method = exchange.getRequestMethod();
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            respond(exchange, method + " " + exchange.getRequestURI() + " body=" + body);
        });

        server.createContext("/boom", exchange -> {
            byte[] bytes = "gone wrong".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(500, bytes.length);
            try (var out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });

        server.start();
        String base = "http://127.0.0.1:" + server.getAddress().getPort();
        System.out.println("server started : " + base);

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> hello = client.send(
                    HttpRequest.newBuilder(URI.create(base + "/hello")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            check(hello.statusCode() == 200, "GET /hello returns 200");
            check(hello.body().equals("hello from the server"), "GET /hello body");
            System.out.println("GET  /hello  : " + hello.statusCode() + " " + hello.body());

            HttpResponse<String> getEcho = client.send(
                    HttpRequest.newBuilder(URI.create(base + "/echo?name=ada")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            check(getEcho.body().contains("GET /echo?name=ada"), "query string reached the server");
            System.out.println("GET  /echo   : " + getEcho.body());

            HttpResponse<String> postEcho = client.send(
                    HttpRequest.newBuilder(URI.create(base + "/echo"))
                            .header("Content-Type", "text/plain")
                            .POST(HttpRequest.BodyPublishers.ofString("ping"))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            check(postEcho.body().equals("POST /echo body=ping"), "POST body reached the server");
            System.out.println("POST /echo   : " + postEcho.body());

            HttpResponse<String> boom = client.send(
                    HttpRequest.newBuilder(URI.create(base + "/boom")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            check(boom.statusCode() == 500, "a failing route returns 500");
            check(boom.body().equals("gone wrong"), "error body is still readable");
            System.out.println("GET  /boom   : " + boom.statusCode() + " " + boom.body());

            HttpResponse<String> missing = client.send(
                    HttpRequest.newBuilder(URI.create(base + "/nope")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            check(missing.statusCode() == 404, "an unregistered path is 404");
            System.out.println("GET  /nope   : " + missing.statusCode() + " (no context registered)");
        } finally {
            server.stop(0);   // always shut the server down
        }

        System.out.println("Server stopped. All checks passed.");
    }
}
