import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 94 - A dependency injection container: types are registered against
 * implementations, and constructor arguments are resolved by reflection, so the
 * classes themselves never mention the container.
 *
 * Compile and run:
 *   javac DependencyInjection.java
 *   java DependencyInjection
 */
public class DependencyInjection {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.CONSTRUCTOR)
    @interface Inject {
    }

    enum Scope {
        SINGLETON, PROTOTYPE
    }

    static class MissingBindingException extends RuntimeException {
        MissingBindingException(String message) {
            super(message);
        }
    }

    static class CircularDependencyException extends RuntimeException {
        CircularDependencyException(String message) {
            super(message);
        }
    }

    static class ConstructionException extends RuntimeException {
        ConstructionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    static final class Container {
        private final Map<Class<?>, Class<?>> bindings = new LinkedHashMap<>();
        private final Map<Class<?>, Scope> scopes = new LinkedHashMap<>();
        private final Map<Class<?>, Object> instances = new LinkedHashMap<>();
        /** The classes currently being built, so a cycle can be spotted. */
        private final Deque<Class<?>> building = new ArrayDeque<>();

        <T> Container bind(Class<T> type, Class<? extends T> implementation) {
            return bind(type, implementation, Scope.SINGLETON);
        }

        <T> Container bind(Class<T> type, Class<? extends T> implementation, Scope scope) {
            bindings.put(type, implementation);
            scopes.put(type, scope);
            instances.remove(type);
            return this;
        }

        /** Register a ready made object, returned as is. */
        <T> Container toInstance(Class<T> type, T instance) {
            bindings.put(type, instance.getClass());
            scopes.put(type, Scope.SINGLETON);
            instances.put(type, instance);
            return this;
        }

        <T> T get(Class<T> type) {
            Object ready = instances.get(type);
            if (ready != null) {
                return type.cast(ready);
            }
            Class<?> implementation = bindings.get(type);
            if (implementation == null) {
                throw new MissingBindingException("nothing is registered for " + type.getSimpleName());
            }
            if (building.contains(implementation)) {
                throw new CircularDependencyException("circular dependency: "
                        + describe(building) + " -> " + implementation.getSimpleName());
            }

            building.push(implementation);
            try {
                Object created = construct(implementation);
                if (scopes.getOrDefault(type, Scope.SINGLETON) == Scope.SINGLETON) {
                    instances.put(type, created);
                }
                return type.cast(created);
            } finally {
                building.pop();          // unwound even when construction throws
            }
        }

        private String describe(Deque<Class<?>> chain) {
            StringBuilder out = new StringBuilder();
            Iterator<Class<?>> oldestFirst = chain.descendingIterator();
            while (oldestFirst.hasNext()) {
                out.append(oldestFirst.next().getSimpleName());
                if (oldestFirst.hasNext()) {
                    out.append(" -> ");
                }
            }
            return out.toString();
        }

        private Object construct(Class<?> implementation) {
            Constructor<?> chosen = chooseConstructor(implementation);
            chosen.setAccessible(true);
            Class<?>[] parameterTypes = chosen.getParameterTypes();
            Object[] arguments = new Object[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                arguments[i] = get(parameterTypes[i]);
            }
            try {
                return chosen.newInstance(arguments);
            } catch (InvocationTargetException wrapped) {
                throw new ConstructionException("the constructor of "
                        + implementation.getSimpleName() + " failed", wrapped.getCause());
            } catch (ReflectiveOperationException other) {
                throw new IllegalStateException("could not build " + implementation.getSimpleName(), other);
            }
        }

        /** An @Inject constructor wins; otherwise the widest one is assumed to be the real one. */
        private Constructor<?> chooseConstructor(Class<?> implementation) {
            Constructor<?>[] constructors = implementation.getDeclaredConstructors();
            if (constructors.length == 0) {
                throw new IllegalStateException(implementation.getSimpleName() + " has no constructor");
            }
            for (Constructor<?> constructor : constructors) {
                if (constructor.isAnnotationPresent(Inject.class)) {
                    return constructor;
                }
            }
            Constructor<?> widest = constructors[0];
            for (Constructor<?> constructor : constructors) {
                if (constructor.getParameterCount() > widest.getParameterCount()) {
                    widest = constructor;
                }
            }
            return widest;
        }

        int registeredTypes() {
            return bindings.size();
        }
    }

    // ------------------------------------------------------- example types
    interface Logger {
        String log(String message);
    }

    static final class ConsoleLogger implements Logger {
        @Override
        public String log(String message) {
            return "LOG " + message;
        }
    }

    interface Repository {
        String find(String id);
    }

    static final class InMemoryRepository implements Repository {
        private final Logger logger;

        @Inject
        InMemoryRepository(Logger logger) {
            this.logger = logger;
        }

        @Override
        public String find(String id) {
            logger.log("finding " + id);
            return "row-" + id;
        }

        Logger logger() {
            return logger;
        }
    }

    static final class Service {
        private final Repository repository;
        private final Logger logger;

        Service(Repository repository, Logger logger) {
            this.repository = repository;
            this.logger = logger;
        }

        String run(String id) {
            return logger.log("serving") + " -> " + repository.find(id);
        }

        Repository repository() {
            return repository;
        }

        Logger logger() {
            return logger;
        }
    }

    static final class TwoConstructors {
        final String which;

        TwoConstructors() {
            which = "no arguments";
        }

        @Inject
        TwoConstructors(Logger logger) {
            which = "injected: " + logger.getClass().getSimpleName();
        }
    }

    static class Counter {
        static final AtomicInteger created = new AtomicInteger();

        Counter() {
            created.incrementAndGet();
        }
    }

    static final class CycleA {
        CycleA(CycleB other) {
        }
    }

    static final class CycleB {
        CycleB(CycleA other) {
        }
    }

    static final class SelfReferential {
        SelfReferential(SelfReferential itself) {
        }
    }

    static final class Exploding {
        Exploding() {
            throw new IllegalStateException("this constructor always fails");
        }
    }

    public static void main(String[] args) {
        // ---- singletons -------------------------------------------------------
        Container container = new Container()
                .bind(Logger.class, ConsoleLogger.class)
                .bind(Repository.class, InMemoryRepository.class)
                .bind(Service.class, Service.class);

        check(container.registeredTypes() == 3, "three bindings");
        Logger firstLogger = container.get(Logger.class);
        Logger secondLogger = container.get(Logger.class);
        check(firstLogger == secondLogger, "a singleton is handed out again and again");
        check(firstLogger.log("hello").equals("LOG hello"), "and it works");

        Repository repository = container.get(Repository.class);
        check(repository == container.get(Repository.class), "the repository is a singleton too");
        check(((InMemoryRepository) repository).logger() == firstLogger,
                "and it received the same logger the container holds");

        Service service = container.get(Service.class);
        check(service.logger() == firstLogger, "the service got the singleton logger");
        check(service.repository() == repository, "and the singleton repository");
        check(service.run("42").equals("LOG serving -> row-42"), "so the whole graph is wired up");
        System.out.println("singletons   : one logger, one repository, one service");

        // ---- prototypes -------------------------------------------------------
        Counter.created.set(0);
        Container prototypes = new Container()
                .bind(Counter.class, Counter.class, Scope.PROTOTYPE);
        Counter one = prototypes.get(Counter.class);
        Counter two = prototypes.get(Counter.class);
        check(one != two, "a prototype binding makes a new instance every time");
        check(Counter.created.get() == 2, "the constructor ran twice");
        System.out.println("prototypes   : two distinct instances from one binding");

        // a singleton by contrast is built once
        Counter.created.set(0);
        Container singletonCounter = new Container().bind(Counter.class, Counter.class);
        singletonCounter.get(Counter.class);
        singletonCounter.get(Counter.class);
        check(Counter.created.get() == 1, "a singleton constructor runs once, however often it is asked for");

        // ---- a pre-built instance ---------------------------------------------
        Logger fixed = message -> "FIXED " + message;
        Container withInstance = new Container().toInstance(Logger.class, fixed);
        check(withInstance.get(Logger.class) == fixed, "a registered instance is returned untouched");
        check(withInstance.get(Logger.class).log("x").equals("FIXED x"), "and is the one that was given");

        // ---- which constructor gets used --------------------------------------
        Container choosy = new Container()
                .bind(Logger.class, ConsoleLogger.class)
                .bind(TwoConstructors.class, TwoConstructors.class);
        check(choosy.get(TwoConstructors.class).which.equals("injected: ConsoleLogger"),
                "@Inject picks the annotated constructor, not the no argument one");
        System.out.println("injection    : " + choosy.get(TwoConstructors.class).which);

        // ---- missing bindings and cycles --------------------------------------
        try {
            container.get(Runnable.class);
            throw new AssertionError("nothing is registered for Runnable");
        } catch (MissingBindingException expected) {
            check(expected.getMessage().contains("Runnable"), "the message names the missing type");
            System.out.println("missing      : " + expected.getMessage());
        }

        Container cyclic = new Container()
                .bind(CycleA.class, CycleA.class)
                .bind(CycleB.class, CycleB.class);
        try {
            cyclic.get(CycleA.class);
            throw new AssertionError("a two class cycle should be reported");
        } catch (CircularDependencyException expected) {
            check(expected.getMessage().contains("CycleA") && expected.getMessage().contains("CycleB"),
                    "the message shows the path that closed the loop");
            System.out.println("cycle        : " + expected.getMessage());
        }

        Container selfCycle = new Container().bind(SelfReferential.class, SelfReferential.class);
        try {
            selfCycle.get(SelfReferential.class);
            throw new AssertionError("a self dependency should be reported");
        } catch (CircularDependencyException expected) {
            System.out.println("self cycle   : caught on the second visit");
        }

        // the container still works after a failed build
        cyclic.bind(Logger.class, ConsoleLogger.class);
        try {
            cyclic.get(CycleA.class);
        } catch (CircularDependencyException expected) {
            // ignored: the point is what happens next
        }
        check(cyclic.get(Logger.class) != null, "the container is usable again after a failed build");

        // ---- a constructor that throws ----------------------------------------
        Container exploding = new Container().bind(Exploding.class, Exploding.class);
        try {
            exploding.get(Exploding.class);
            throw new AssertionError("the failure should be wrapped");
        } catch (ConstructionException expected) {
            check(expected.getCause() instanceof IllegalStateException, "the original cause is kept");
            check(expected.getCause().getMessage().equals("this constructor always fails"),
                    "with its own message");
            System.out.println("build failure: " + expected.getMessage()
                    + " because " + expected.getCause().getMessage());
        }

        // ---- a deeper graph ---------------------------------------------------
        Container deep = new Container()
                .bind(Logger.class, ConsoleLogger.class, Scope.PROTOTYPE)
                .bind(Repository.class, InMemoryRepository.class)
                .bind(Service.class, Service.class);
        Service oneService = deep.get(Service.class);
        Service twoService = deep.get(Service.class);
        check(oneService == twoService, "the service is a singleton");
        check(oneService.logger() == twoService.logger(),
                "and it was built once, so its dependency did not change underneath it");
        System.out.println("mixed scopes : a singleton service over a prototype dependency");
        System.out.println("All checks passed.");
    }
}
