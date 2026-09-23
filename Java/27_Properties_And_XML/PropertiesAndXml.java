import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * 27 - Configuration: java.util.Properties for key/value settings, and DOM
 * parsing for a small XML document.
 *
 * Compile and run:
 *   javac PropertiesAndXml.java
 *   java PropertiesAndXml
 */
public class PropertiesAndXml {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static final String XML = """
            <suite name="smoke" browser="chromium">
              <test id="TC-01" status="pass"><name>Login with valid user</name></test>
              <test id="TC-02" status="fail"><name>Checkout totals</name></test>
              <test id="TC-03" status="pass"><name>Search returns results</name></test>
            </suite>
            """;

    static void deleteTree(Path root) throws IOException {
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("java-config-");
        try {
            // ---- Properties: write, then read back -------------------------
            Properties config = new Properties();
            config.setProperty("browser", "chromium");
            config.setProperty("headless", "true");
            config.setProperty("timeout.ms", "30000");

            Path file = dir.resolve("config.properties");
            try (Writer writer = Files.newBufferedWriter(file)) {
                config.store(writer, "demo configuration");
            }

            Properties loaded = new Properties();
            try (Reader reader = Files.newBufferedReader(file)) {
                loaded.load(reader);
            }
            check(loaded.getProperty("browser").equals("chromium"), "loaded value");
            check(loaded.getProperty("headless").equals("true"), "boolean stored as text");
            check(loaded.getProperty("missing", "fallback").equals("fallback"), "default when absent");
            check(loaded.size() == 3, "three keys");
            System.out.println("properties    : " + loaded.getProperty("browser")
                    + ", headless=" + loaded.getProperty("headless")
                    + ", timeout=" + loaded.getProperty("timeout.ms"));

            // system properties come from the same class
            check(System.getProperty("java.version") != null, "java.version is set");
            check(!System.getProperty("java.version", "").isEmpty(), "java.version is not blank");
            System.out.println("java.version  : " + System.getProperty("java.version"));

            // ---- DOM parsing ----------------------------------------------
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(XML)));
            Element root = document.getDocumentElement();

            check(root.getTagName().equals("suite"), "root element is suite");
            check(root.getAttribute("name").equals("smoke"), "root attribute");
            check(root.getAttribute("browser").equals("chromium"), "second root attribute");

            NodeList tests = root.getElementsByTagName("test");
            check(tests.getLength() == 3, "three test elements");

            int passed = 0;
            for (int i = 0; i < tests.getLength(); i++) {
                Element test = (Element) tests.item(i);
                String id = test.getAttribute("id");
                String status = test.getAttribute("status");
                String name = test.getElementsByTagName("name").item(0).getTextContent();
                if (status.equals("pass")) {
                    passed++;
                }
                System.out.printf("  %-6s %-5s %s%n", id, status, name);
                if (id.equals("TC-02")) {
                    check(name.equals("Checkout totals"), "nested element text");
                    check(status.equals("fail"), "attribute value");
                }
            }
            check(passed == 2, "two tests passed");
            System.out.println("xml suite     : " + root.getAttribute("name")
                    + ", " + passed + "/" + tests.getLength() + " passed");
        } finally {
            deleteTree(dir);
        }
        System.out.println("All checks passed (temp tree cleaned up).");
    }
}
