import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 48 - Running other processes: ProcessBuilder for exit codes, stdout and stderr,
 * a working directory, environment variables and a timeout.
 *
 * Compile and run:
 *   javac ProcessBuilderDemo.java
 *   java ProcessBuilderDemo
 */
public class ProcessBuilderDemo {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static final boolean WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");
    static final String PRINT_CWD = WINDOWS ? "cd" : "pwd";
    static final String PRINT_VAR = WINDOWS ? "echo %QA_RUN%" : "echo $QA_RUN";
    static final String LONG_RUNNING = WINDOWS ? "ping -n 20 127.0.0.1" : "sleep 20";

    record Outcome(int exitCode, String stdout, String stderr) {
    }

    static final ExecutorService READERS = Executors.newCachedThreadPool();

    /**
     * Run a command through the platform shell. Both output streams are drained
     * on separate threads: filling one pipe while the other is unread deadlocks.
     */
    static Outcome run(ProcessBuilder builder) throws Exception {
        Process process = builder.start();
        Future<String> stdout = READERS.submit(
                () -> new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
        Future<String> stderr = READERS.submit(
                () -> new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8));
        int exitCode = process.waitFor();
        return new Outcome(exitCode, stdout.get(), stderr.get());
    }

    static List<String> shell(String commandLine) {
        List<String> command = new ArrayList<>();
        if (WINDOWS) {
            command.add("cmd");
            command.add("/c");
        } else {
            command.add("sh");
            command.add("-c");
        }
        command.add(commandLine);
        return command;
    }

    static Outcome run(String commandLine) throws Exception {
        return run(new ProcessBuilder(shell(commandLine)));
    }

    public static void main(String[] args) throws Exception {
        Path working = Files.createTempDirectory("java-process-");
        try {
            // ---- a command that succeeds ---------------------------------------
            Outcome echoed = run("echo hello from a child process");
            check(echoed.exitCode() == 0, "echo exits 0");
            check(echoed.stdout().strip().equals("hello from a child process"), "stdout is captured");
            check(echoed.stderr().isEmpty(), "nothing on stderr");
            System.out.println("echo        : " + echoed.stdout().strip());

            // ---- a non-zero exit code is not an exception ------------------------
            Outcome failed = run("exit 3");
            check(failed.exitCode() == 3, "the exit code comes back as a number");
            System.out.println("exit 3      : exitCode=" + failed.exitCode());

            // ---- stdout and stderr are separate pipes ---------------------------
            Outcome mixed = run("echo to-stdout & echo to-stderr 1>&2");
            check(mixed.stdout().contains("to-stdout"), "stdout captured separately");
            check(mixed.stderr().contains("to-stderr"), "stderr captured separately");
            check(!mixed.stdout().contains("to-stderr"), "the streams are not merged");
            System.out.println("streams     : stdout=" + mixed.stdout().strip()
                    + " stderr=" + mixed.stderr().strip());

            // ---- a command that does not exist ----------------------------------
            Outcome missing = run("this-command-does-not-exist-anywhere");
            check(missing.exitCode() != 0, "a missing command exits non-zero");
            check(!missing.stderr().isEmpty(), "the shell explains the problem on stderr");
            System.out.println("missing cmd : exitCode=" + missing.exitCode());

            // ---- working directory ----------------------------------------------
            ProcessBuilder inDirectory = new ProcessBuilder(shell(PRINT_CWD));
            inDirectory.directory(working.toFile());
            Outcome where = run(inDirectory);
            check(where.stdout().toLowerCase().contains(working.getFileName().toString().toLowerCase()),
                    "the child ran in the directory we set");
            System.out.println("cwd         : " + where.stdout().strip());

            // ---- environment variables ------------------------------------------
            ProcessBuilder withEnv = new ProcessBuilder(shell(PRINT_VAR));
            withEnv.environment().put("QA_RUN", "42");
            Outcome env = run(withEnv);
            check(env.stdout().strip().equals("42"), "the child saw the variable we added");
            System.out.println("env QA_RUN  : " + env.stdout().strip());

            // ---- never wait forever ---------------------------------------------
            Process slow = new ProcessBuilder(shell(LONG_RUNNING)).start();
            boolean finishedInTime = slow.waitFor(700, TimeUnit.MILLISECONDS);
            check(!finishedInTime, "the long command is still running");
            slow.destroy();
            check(slow.waitFor(5, TimeUnit.SECONDS), "destroy() stopped it");
            System.out.println("timeout     : still running after 700ms, then destroyed");
        } finally {
            READERS.shutdown();
            try (var paths = Files.walk(working)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
        System.out.println("All checks passed (temp tree cleaned up).");
    }
}
