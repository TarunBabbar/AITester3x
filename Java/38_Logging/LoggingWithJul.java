import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

/**
 * 38 - Logging: java.util.logging levels, handlers, formatters and how a handler
 * filters independently of its logger.
 *
 * Compile and run:
 *   javac LoggingWithJul.java
 *   java LoggingWithJul
 */
public class LoggingWithJul {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /** A handler that just remembers what it was given. */
    static final class Collector extends Handler {
        final List<LogRecord> records = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            // A custom handler filters for itself: the logger only checks its OWN level.
            if (!isLoggable(record)) {
                return;
            }
            records.add(record);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }

    public static void main(String[] args) {
        Logger logger = Logger.getLogger("qa.runner");
        logger.setUseParentHandlers(false);   // do not also spray the root console logger
        logger.setLevel(Level.ALL);

        // ---- handler 1: formatted text into a buffer ------------------------
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        Formatter compact = new Formatter() {
            @Override
            public String format(LogRecord record) {
                return record.getLevel() + ": " + record.getMessage() + System.lineSeparator();
            }
        };
        StreamHandler text = new StreamHandler(sink, compact);
        text.setLevel(Level.INFO);            // this handler ignores anything below INFO
        logger.addHandler(text);

        logger.fine("this is below the handler level and should be dropped");
        logger.info("starting run");
        logger.warning("flaky retry");
        logger.log(Level.SEVERE, "run failed", new IllegalStateException("boom"));
        text.flush();

        String output = sink.toString(StandardCharsets.UTF_8);
        check(output.contains("INFO: starting run"), "INFO reached the handler");
        check(output.contains("WARNING: flaky retry"), "WARNING reached the handler");
        check(output.contains("SEVERE: run failed"), "SEVERE reached the handler");
        check(!output.contains("should be dropped"), "FINE was filtered out by the handler level");
        System.out.println("--- captured log ---" + System.lineSeparator() + output + "--------------------");

        // ---- handler 2: only warnings and above, into a list ----------------
        Collector collector = new Collector();
        collector.setLevel(Level.WARNING);
        logger.addHandler(collector);

        logger.info("the collector ignores this");
        logger.warning("counted once");
        logger.log(Level.SEVERE, "counted twice", new IllegalStateException("boom"));

        check(collector.records.size() == 2, "the collector saw only WARNING and SEVERE");
        check(collector.records.get(0).getMessage().equals("counted once"), "first collected record");
        check(collector.records.get(0).getLevel() == Level.WARNING, "record level");
        check(collector.records.get(1).getLevel() == Level.SEVERE, "second record level");
        check(collector.records.get(0).getLoggerName().equals("qa.runner"), "the record knows its logger");
        check(collector.records.get(1).getThrown() != null, "the thrown object is kept on the record");

        // ---- level arithmetic ----------------------------------------------
        check(Level.SEVERE.intValue() > Level.WARNING.intValue(), "SEVERE is above WARNING");
        check(Level.WARNING.intValue() > Level.INFO.intValue(), "WARNING is above INFO");
        check(Level.parse("INFO") == Level.INFO, "levels can be parsed by name");
        check(Level.WARNING.intValue() >= Level.WARNING.intValue(), "isLoggable mirrors this comparison");

        System.out.println("collected    : " + collector.records.size() + " records");
        System.out.println("isLoggable   : FINE=" + logger.isLoggable(Level.FINE)
                + ", WARNING=" + logger.isLoggable(Level.WARNING));

        logger.removeHandler(text);
        logger.removeHandler(collector);   // leave the logger as we found it
        System.out.println("All checks passed.");
    }
}
