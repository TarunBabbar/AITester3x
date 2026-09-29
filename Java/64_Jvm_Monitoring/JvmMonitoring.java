import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.List;

/**
 * 64 - JVM monitoring: the platform MXBeans for memory, threads, class loading
 * and garbage collection, read from inside the running program.
 *
 * Compile and run:
 *   javac JvmMonitoring.java
 *   java JvmMonitoring
 */
public class JvmMonitoring {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static String mb(long bytes) {
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    public static void main(String[] args) {
        // ---- Runtime: the simple view ----------------------------------------
        Runtime runtime = Runtime.getRuntime();
        check(runtime.maxMemory() > 0, "the JVM reports a maximum heap");
        check(runtime.availableProcessors() >= 1, "at least one processor is visible");
        check(runtime.version().feature() >= 21, "running a modern JDK: " + runtime.version());
        System.out.println("jvm         : " + System.getProperty("java.vm.name")
                + " " + runtime.version());
        System.out.println("processors  : " + runtime.availableProcessors()
                + ", max heap " + mb(runtime.maxMemory()));

        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        check(runtimeBean.getUptime() >= 0, "uptime is a number of milliseconds");
        check(!runtimeBean.getClassPath().isEmpty(), "the classpath is visible");
        System.out.println("uptime      : " + runtimeBean.getUptime() + " ms, "
                + runtimeBean.getInputArguments().size() + " JVM argument(s)");

        // ---- memory ----------------------------------------------------------
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = memory.getHeapMemoryUsage();
        MemoryUsage nonHeap = memory.getNonHeapMemoryUsage();
        check(heap.getCommitted() > 0, "some heap is committed");
        check(heap.getMax() > 0, "a maximum heap is defined");
        check(nonHeap.getUsed() >= 0, "non-heap usage is reported");
        System.out.printf("heap        : used %s of %s (committed %s)%n",
                mb(heap.getUsed()), mb(heap.getMax()), mb(heap.getCommitted()));

        // Hold a block of memory so the delta below is real, then report it.
        long budget = Math.max(8L * 1024 * 1024,
                Math.min(64L * 1024 * 1024, runtime.maxMemory() / 16));
        int blockSize = 1024 * 1024;
        int blockCount = (int) (budget / blockSize);
        long before = memory.getHeapMemoryUsage().getUsed();
        byte[][] blocks = new byte[blockCount][];
        for (int i = 0; i < blockCount; i++) {
            blocks[i] = new byte[blockSize];
        }
        long after = memory.getHeapMemoryUsage().getUsed();
        check(blocks.length == blockCount && blocks[0].length == blockSize, "the blocks were allocated");
        System.out.printf("held %s    : used %s (delta %s)%n",
                mb(budget), mb(after), mb(after - before));
        // The delta is advisory - a GC can run at any moment - so it is printed,
        // not asserted. The allocations themselves are what we check.
        check(blocks.length > 0, "the reference is still held");

        // ---- threads ---------------------------------------------------------
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        check(threads.getThreadCount() >= 1, "at least one live thread");
        check(threads.getPeakThreadCount() >= threads.getThreadCount(),
                "the peak is at least the current count");
        check(threads.getDaemonThreadCount() >= 0, "the daemon count is reported");
        System.out.println("threads     : " + threads.getThreadCount() + " live, peak "
                + threads.getPeakThreadCount() + ", " + threads.getDaemonThreadCount() + " daemon");

        if (threads.isThreadCpuTimeSupported() && threads.isThreadCpuTimeEnabled()) {
            System.out.println("this thread : " + threads.getCurrentThreadCpuTime() / 1_000_000 + " ms of CPU");
        } else {
            System.out.println("this thread : CPU time is not available on this JVM");
        }

        // ---- class loading ---------------------------------------------------
        ClassLoadingMXBean classes = ManagementFactory.getClassLoadingMXBean();
        check(classes.getLoadedClassCount() > 0, "some classes are loaded");
        check(classes.getTotalLoadedClassCount() >= classes.getLoadedClassCount(),
                "the total is cumulative, so it cannot be smaller");
        check(classes.getUnloadedClassCount() >= 0, "the unloaded count is reported");
        System.out.println("classes     : " + classes.getLoadedClassCount() + " loaded, "
                + classes.getTotalLoadedClassCount() + " ever loaded, "
                + classes.getUnloadedClassCount() + " unloaded");

        // ---- garbage collectors ---------------------------------------------
        List<GarbageCollectorMXBean> collectors = ManagementFactory.getGarbageCollectorMXBeans();
        check(!collectors.isEmpty(), "at least one garbage collector is reported");
        for (GarbageCollectorMXBean collector : collectors) {
            System.out.printf("gc          : %-30s collections=%-4d time=%d ms%n",
                    collector.getName(), collector.getCollectionCount(), collector.getCollectionTime());
        }

        // ---- operating system ------------------------------------------------
        java.lang.management.OperatingSystemMXBean base = ManagementFactory.getOperatingSystemMXBean();
        check(base.getAvailableProcessors() >= 1, "the OS bean agrees on the processor count");
        check(base.getName() != null && !base.getName().isEmpty(), "the OS name is available");
        check(base.getSystemLoadAverage() >= -1.0, "-1 means the load average is unavailable here");
        System.out.println("os          : " + base.getName() + " " + base.getArch()
                + ", load " + base.getSystemLoadAverage());

        if (base instanceof OperatingSystemMXBean extended) {
            check(extended.getProcessCpuTime() >= 0, "the process CPU time is available");
            double load = extended.getProcessCpuLoad();
            System.out.println("process cpu : "
                    + (load < 0 ? "not available" : String.format("%.1f%%", load * 100)));
        }

        // ---- a few system properties -----------------------------------------
        for (String key : new String[] {"java.version", "java.vendor", "os.name", "user.dir", "file.separator"}) {
            check(System.getProperty(key) != null, key + " is set");
        }
        System.out.println("All checks passed.");
    }
}
