import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * 75 - Layered configuration: defaults, then a properties file, then environment
 * variables, then command line arguments - each layer overriding the last.
 *
 * Compile and run:
 *   javac LayeredConfig.java
 *   java LayeredConfig --timeout.ms=5000
 */
public class LayeredConfig {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    record Config(String browser, int timeoutMs, String baseUrl, boolean headless) {
        @Override
        public String toString() {
            return "browser=" + browser + " timeout.ms=" + timeoutMs
                    + " base.url=" + baseUrl + " headless=" + headless;
        }
    }

    /** The lowest layer: sensible values that need no file at all. */
    static Map<String, String> defaults() {
        return new LinkedHashMap<>(Map.of(
                "browser", "chromium",
                "timeout.ms", "30000",
                "base.url", "http://localhost:8080"));
    }

    static Map<String, String> fromProperties(Path file) throws IOException {
        Map<String, String> values = new LinkedHashMap<>();
        if (!Files.exists(file)) {
            return values;
        }
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file)) {
            properties.load(reader);
        }
        for (String key : properties.stringPropertyNames()) {
            values.put(key.trim(), properties.getProperty(key).trim());
        }
        return values;
    }

    /** QA_BROWSER becomes browser; QA_TIMEOUT_MS becomes timeout.ms. */
    static Map<String, String> fromEnvironment(Map<String, String> environment, String prefix) {
        Map<String, String> values = new LinkedHashMap<>();
        new TreeMap<>(environment).forEach((name, value) -> {
            if (name.startsWith(prefix) && name.length() > prefix.length()) {
                values.put(name.substring(prefix.length()).toLowerCase().replace('_', '.'), value);
            }
        });
        return values;
    }

    static Map<String, String> fromArguments(String[] args) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String argument : args) {
            if (!argument.startsWith("--")) {
                throw new IllegalArgumentException("expected --key=value, got: " + argument);
            }
            int equals = argument.indexOf('=');
            if (equals < 0) {
                throw new IllegalArgumentException("no '=' in: " + argument);
            }
            values.put(argument.substring(2, equals).trim(), argument.substring(equals + 1).trim());
        }
        return values;
    }

    /** Later layers win, and the order here is the whole point. */
    @SafeVarargs
    static Map<String, String> merge(Map<String, String>... layers) {
        Map<String, String> merged = new LinkedHashMap<>();
        for (Map<String, String> layer : layers) {
            merged.putAll(layer);
        }
        return merged;
    }

    static Config toConfig(Map<String, String> values) {
        int timeout;
        try {
            timeout = Integer.parseInt(require(values, "timeout.ms"));
        } catch (NumberFormatException bad) {
            throw new IllegalArgumentException(
                    "timeout.ms must be a whole number, got: " + values.get("timeout.ms"), bad);
        }
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout.ms must not be negative: " + timeout);
        }
        boolean headless = switch (values.getOrDefault("headless", "false").toLowerCase()) {
            case "true", "yes", "1" -> true;
            case "false", "no", "0" -> false;
            default -> throw new IllegalArgumentException(
                    "headless must be true or false, got: " + values.get("headless"));
        };
        return new Config(require(values, "browser"), timeout, require(values, "base.url"), headless);
    }

    static String require(Map<String, String> values, String key) {
        String value = values.get(key);
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("missing required setting: " + key);
        }
        return value;
    }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("java-config-");
        try {
            Path file = dir.resolve("qa.properties");
            Files.writeString(file, "browser=firefox\ntimeout.ms=15000\n");

            Map<String, String> properties = fromProperties(file);
            check(properties.get("browser").equals("firefox"), "the file was read");
            check(properties.get("timeout.ms").equals("15000"), "and the second key too");
            check(fromProperties(dir.resolve("absent.properties")).isEmpty(),
                    "a missing file contributes nothing rather than failing");

            Map<String, String> environment = fromEnvironment(
                    Map.of("QA_BROWSER", "webkit",
                           "QA_HEADLESS", "true",
                           "PATH", "/usr/bin",
                           "QA_", "ignored-because-the-name-is-only-the-prefix"),
                    "QA_");
            check(environment.get("browser").equals("webkit"), "an environment variable became a key");
            check(environment.get("headless").equals("true"), "and so did the flag");
            check(environment.size() == 2, "PATH and the bare prefix are ignored");
            check(!environment.containsKey(""), "the bare prefix does not create an empty key");
            System.out.println("environment : " + environment);

            Map<String, String> arguments = fromArguments(new String[] {
                    "--timeout.ms=5000", "--base.url=https://staging.example"});
            check(arguments.get("timeout.ms").equals("5000"), "an argument was parsed");
            check(arguments.get("base.url").equals("https://staging.example"), "values may contain dots and colons");

            // ---- the layering -------------------------------------------------
            Config resolved = toConfig(merge(defaults(), properties, environment, arguments));
            check(resolved.browser().equals("webkit"),
                    "environment beats the file, which beat the default");
            check(resolved.timeoutMs() == 5000, "arguments beat every other layer");
            check(resolved.baseUrl().equals("https://staging.example"), "and supplied a new value entirely");
            check(resolved.headless(), "headless came from the environment alone");
            System.out.println("resolved    : " + resolved);

            // each layer on its own, to show the precedence explicitly
            check(toConfig(merge(defaults())).browser().equals("chromium"), "defaults alone");
            check(toConfig(merge(defaults(), properties)).browser().equals("firefox"), "file overrides defaults");
            check(toConfig(merge(defaults(), properties, environment)).browser().equals("webkit"),
                    "environment overrides the file");
            check(toConfig(merge(defaults(), properties, environment, arguments)).timeoutMs() == 5000,
                    "arguments win overall");

            Config withDefaults = toConfig(defaults());
            check(withDefaults.timeoutMs() == 30_000 && !withDefaults.headless(),
                    "an empty run falls back to the defaults");
            System.out.println("defaults    : " + withDefaults);

            // ---- rejected input ------------------------------------------------
            try {
                toConfig(merge(defaults(), fromArguments(new String[] {"--timeout.ms=soon"})));
                throw new AssertionError("a non-numeric timeout should fail");
            } catch (IllegalArgumentException expected) {
                System.out.println("bad timeout : " + expected.getMessage());
            }
            try {
                toConfig(merge(defaults(), Map.of("headless", "maybe")));
                throw new AssertionError("a non-boolean headless should fail");
            } catch (IllegalArgumentException expected) {
                System.out.println("bad boolean : " + expected.getMessage());
            }
            try {
                toConfig(Map.of("browser", "chromium", "timeout.ms", "1000"));
                throw new AssertionError("a missing base.url should fail");
            } catch (IllegalArgumentException expected) {
                System.out.println("missing key : " + expected.getMessage());
            }
            for (String bad : new String[] {"timeout.ms=1000", "--timeout.ms"}) {
                try {
                    fromArguments(new String[] {bad});
                    throw new AssertionError("should have been rejected: " + bad);
                } catch (IllegalArgumentException expected) {
                    System.out.println("bad argument: " + expected.getMessage());
                }
            }
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
