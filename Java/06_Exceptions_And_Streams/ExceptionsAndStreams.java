import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 06 - Exceptions and streams: custom exceptions, try/catch and the Stream API.
 *
 * Compile and run:
 *   javac ExceptionsAndStreams.java
 *   java ExceptionsAndStreams
 */
public class ExceptionsAndStreams {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static class InsufficientFundsException extends Exception {
        InsufficientFundsException(String message) {
            super(message);
        }
    }

    record Account(String owner, double balance) {
        Account withdraw(double amount) throws InsufficientFundsException {
            if (amount <= 0) {
                throw new IllegalArgumentException("amount must be positive");
            }
            if (amount > balance) {
                throw new InsufficientFundsException(
                        "balance %.2f is too low for %.2f".formatted(balance, amount));
            }
            return new Account(owner, balance - amount);
        }
    }

    record Person(String name, int age) {
    }

    public static void main(String[] args) {
        Account account = new Account("Alice", 100.0);

        try {
            Account after = account.withdraw(30);
            check(after.balance() == 70.0, "balance after withdrawal is 70");
            System.out.printf("Withdrew 30, balance is now %.2f%n", after.balance());
        } catch (InsufficientFundsException impossible) {
            throw new AssertionError("this withdrawal should succeed", impossible);
        }

        try {
            account.withdraw(500);
            throw new AssertionError("the withdrawal should have failed");
        } catch (InsufficientFundsException expected) {
            System.out.println("Caught: " + expected.getMessage());
            check(expected.getMessage().contains("too low"), "message explains the problem");
        }

        try {
            account.withdraw(-5);
            throw new AssertionError("a negative amount should fail");
        } catch (IllegalArgumentException expected) {
            System.out.println("Caught: " + expected.getMessage());
        } catch (InsufficientFundsException unexpected) {
            throw new AssertionError("a negative amount should fail with IllegalArgumentException",
                    unexpected);
        }

        List<Person> people = List.of(
                new Person("Alice", 34),
                new Person("Bob", 17),
                new Person("Carol", 29),
                new Person("Dan", 12));

        List<String> adults = people.stream()
                .filter(person -> person.age() >= 18)
                .map(Person::name)
                .sorted()
                .toList();
        check(adults.equals(List.of("Alice", "Carol")), "adults are Alice and Carol");

        double averageAge = people.stream()
                .mapToInt(Person::age)
                .average()
                .orElse(0);
        check(Math.abs(averageAge - 23.0) < 0.001, "average age is 23");

        Map<Boolean, List<Person>> byAdulthood = people.stream()
                .collect(Collectors.partitioningBy(person -> person.age() >= 18));
        check(byAdulthood.get(true).size() == 2, "two adults are partitioned");

        Optional<Person> oldest = people.stream()
                .max(Comparator.comparingInt(Person::age));
        check(oldest.isPresent() && oldest.get().name().equals("Alice"), "oldest is Alice");

        long countOver20 = people.stream().filter(person -> person.age() > 20).count();
        check(countOver20 == 2, "two people are over 20");

        System.out.println("adults      : " + adults);
        System.out.printf("average age : %.1f%n", averageAge);
        System.out.println("oldest      : " + oldest.map(Person::name).orElse("none"));
        System.out.println("All checks passed.");
    }
}
