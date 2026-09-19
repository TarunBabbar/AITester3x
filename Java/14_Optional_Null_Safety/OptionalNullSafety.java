import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 14 - Optional and null safety: creating, transforming and consuming Optional
 * values instead of returning null.
 *
 * Compile and run:
 *   javac OptionalNullSafety.java
 *   java OptionalNullSafety
 */
public class OptionalNullSafety {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    record Address(String city) {
    }

    record User(String name, Optional<Address> address) {
    }

    static Optional<String> findUser(Map<String, String> users, String id) {
        return Optional.ofNullable(users.get(id));
    }

    public static void main(String[] args) {
        Map<String, String> users = Map.of("u1", "Alice", "u2", "Bob");

        check(findUser(users, "u1").isPresent(), "u1 exists");
        check(findUser(users, "u9").isEmpty(), "u9 is missing");

        check(findUser(users, "u1").orElse("unknown").equals("Alice"), "orElse with a value");
        check(findUser(users, "u9").orElse("unknown").equals("unknown"), "orElse fallback");
        check(findUser(users, "u9").or(() -> Optional.of("Guest")).orElseThrow().equals("Guest"),
                "or() supplies another Optional");

        check(findUser(users, "u1").map(String::length).orElse(0) == 5, "map transforms");
        check(findUser(users, "u9").map(String::length).isEmpty(), "map keeps an empty empty");
        check(findUser(users, "u2").filter(name -> name.startsWith("A")).isEmpty(),
                "filter can empty a present value");

        check(Optional.of("x").map(text -> (String) null).isEmpty(), "map returning null empties");
        check(Optional.of("hello").flatMap(text -> Optional.of(text.length())).orElse(0) == 5,
                "flatMap avoids Optional<Optional<...>>");

        try {
            findUser(users, "u9").orElseThrow(() -> new IllegalStateException("user u9 is missing"));
            throw new AssertionError("orElseThrow should have thrown");
        } catch (IllegalStateException expected) {
            check(expected.getMessage().contains("missing"), "orElseThrow carries a message");
            System.out.println("Caught: " + expected.getMessage());
        }

        try {
            Optional.of(null);
            throw new AssertionError("Optional.of(null) should fail");
        } catch (NullPointerException expected) {
            System.out.println("Optional.of(null) -> NullPointerException");
        }

        StringBuilder log = new StringBuilder();
        findUser(users, "u1").ifPresentOrElse(
                name -> log.append("found ").append(name), () -> log.append("none"));
        findUser(users, "u9").ifPresentOrElse(
                name -> log.append("found"), () -> log.append(";none"));
        check(log.toString().equals("found Alice;none"), "ifPresentOrElse branches");

        List<String> present = List.of("u1", "u9", "u2").stream()
                .map(id -> findUser(users, id))
                .flatMap(Optional::stream)
                .toList();
        check(present.equals(List.of("Alice", "Bob")), "Optional.stream drops the empties");

        User alice = new User("Alice", Optional.of(new Address("London")));
        User bob = new User("Bob", Optional.empty());
        check(alice.address().map(Address::city).orElse("nowhere").equals("London"), "nested lookup");
        check(bob.address().map(Address::city).orElse("nowhere").equals("nowhere"), "nested fallback");

        System.out.println("present users : " + present);
        System.out.println("alice city    : " + alice.address().map(Address::city).orElse("nowhere"));
        System.out.println("bob city      : " + bob.address().map(Address::city).orElse("nowhere"));
        System.out.println("All checks passed.");
    }
}
