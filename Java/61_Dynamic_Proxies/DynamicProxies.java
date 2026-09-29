import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 61 - Dynamic proxies: implement an interface at runtime with InvocationHandler,
 * then build a call logger, a retry wrapper and a hand-rolled mock on top.
 *
 * Compile and run:
 *   javac DynamicProxies.java
 *   java DynamicProxies
 */
public class DynamicProxies {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    interface Repository {
        String find(String id);

        int count();

        void save(String id);
    }

    static final class InMemoryRepository implements Repository {
        private final Map<String, String> rows = new HashMap<>();

        @Override
        public String find(String id) {
            return rows.get(id);
        }

        @Override
        public int count() {
            return rows.size();
        }

        @Override
        public void save(String id) {
            rows.put(id, "row-" + id);
        }
    }

    record Call(String method, List<Object> arguments) {
    }

    /** Records every call, then delegates to the real object. */
    static final class LoggingHandler implements InvocationHandler {
        private final Object target;
        final List<Call> calls = new ArrayList<>();

        LoggingHandler(Object target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            calls.add(new Call(method.getName(), args == null ? List.of() : List.of(args)));
            return method.invoke(target, args);
        }
    }

    /** Retries a call while the underlying object throws IllegalStateException. */
    static final class RetryHandler implements InvocationHandler {
        private final Object target;
        private final int attempts;
        int failures;

        RetryHandler(Object target, int attempts) {
            this.target = target;
            this.attempts = attempts;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            IllegalStateException last = null;
            for (int attempt = 1; attempt <= attempts; attempt++) {
                try {
                    return method.invoke(target, args);
                } catch (InvocationTargetException wrapped) {
                    if (!(wrapped.getCause() instanceof IllegalStateException)) {
                        throw wrapped.getCause();
                    }
                    last = (IllegalStateException) wrapped.getCause();
                    failures++;
                }
            }
            throw last;
        }
    }

    /** A hand-rolled mock: stubbed return values plus per-method call counts. */
    static final class MockHandler implements InvocationHandler {
        private final Map<String, Object> results = new HashMap<>();
        private final Map<String, Integer> counts = new HashMap<>();

        MockHandler when(String method, Object result) {
            results.put(method, result);
            return this;
        }

        int callsTo(String method) {
            return counts.getOrDefault(method, 0);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            counts.merge(method.getName(), 1, Integer::sum);
            if (results.containsKey(method.getName())) {
                return results.get(method.getName());
            }
            return defaultFor(method.getReturnType());
        }

        private static Object defaultFor(Class<?> type) {
            if (type == int.class) {
                return 0;
            }
            if (type == boolean.class) {
                return false;
            }
            if (type == long.class) {
                return 0L;
            }
            return null;     // void and reference types
        }
    }

    @SuppressWarnings("unchecked")
    static <T> T proxyFor(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler);
    }

    static Repository flakyRepository(AtomicInteger attempts, int failUntil) {
        return proxyFor(Repository.class, (proxy, method, args) -> {
            if (method.getName().equals("count")) {
                if (attempts.incrementAndGet() <= failUntil) {
                    throw new IllegalStateException("temporary failure");
                }
                return 42;
            }
            return null;
        });
    }

    public static void main(String[] args) throws Exception {
        // ---- a logging proxy around a real object ---------------------------
        InMemoryRepository real = new InMemoryRepository();
        LoggingHandler logging = new LoggingHandler(real);
        Repository monitored = proxyFor(Repository.class, logging);

        check(Proxy.isProxyClass(monitored.getClass()), "the instance is a proxy class");
        check(Proxy.getInvocationHandler(monitored) == logging, "the handler is recoverable");
        check(monitored instanceof Repository, "it still implements the interface");

        monitored.save("a");
        monitored.save("b");
        check(monitored.count() == 2, "calls reach the real object");
        check(monitored.find("a").equals("row-a"), "return values come back unchanged");
        check(logging.calls.size() == 4, "every call was recorded");
        check(logging.calls.get(0).method().equals("save"), "the method name is recorded");
        check(logging.calls.get(0).arguments().equals(List.of("a")), "and the arguments");
        check(logging.calls.get(2).method().equals("count"), "the third call is count");
        check(logging.calls.get(2).arguments().isEmpty(), "a no-argument call records an empty list");
        check(logging.calls.get(3).arguments().equals(List.of("a")), "find recorded its argument");
        System.out.println("logged      : " + logging.calls);

        // ---- a retry proxy ---------------------------------------------------
        AtomicInteger attempts = new AtomicInteger();
        RetryHandler retrying = new RetryHandler(flakyRepository(attempts, 2), 5);
        Repository guarded = proxyFor(Repository.class, retrying);
        check(guarded.count() == 42, "the retry proxy got through");
        check(attempts.get() == 3, "it took three attempts");
        check(retrying.failures == 2, "two failures were swallowed");

        AtomicInteger stubborn = new AtomicInteger();
        Repository givingUp = proxyFor(Repository.class,
                new RetryHandler(flakyRepository(stubborn, 10), 3));
        try {
            givingUp.count();
            throw new AssertionError("three attempts should not be enough");
        } catch (IllegalStateException expected) {
            check(stubborn.get() == 3, "it stopped after the configured three attempts");
            System.out.println("gave up     : " + expected.getMessage() + " after " + stubborn.get() + " attempts");
        }

        // ---- a mock ----------------------------------------------------------
        MockHandler mock = new MockHandler().when("find", "stubbed-row").when("count", 7);
        Repository stubbed = proxyFor(Repository.class, mock);

        check(stubbed.find("anything").equals("stubbed-row"), "the stub ignores the argument");
        check(stubbed.count() == 7, "a second stub");
        stubbed.save("x");
        stubbed.save("y");
        check(mock.callsTo("save") == 2, "the mock counts calls per method");
        check(mock.callsTo("find") == 1, "and keeps them apart");
        System.out.println("mock counts : save=" + mock.callsTo("save")
                + " find=" + mock.callsTo("find") + " count=" + mock.callsTo("count"));

        // ---- Object methods are proxied too ----------------------------------
        check(monitored.toString().contains("InMemoryRepository"),
                "toString() is routed through the handler as well");
        check(logging.calls.get(logging.calls.size() - 1).method().equals("toString"),
                "and shows up in the log as an ordinary call");
        System.out.println("All checks passed.");
    }
}
