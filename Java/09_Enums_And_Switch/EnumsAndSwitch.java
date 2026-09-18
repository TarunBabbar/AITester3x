import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

/**
 * 09 - Enums: constants with fields and methods, switch, values(), valueOf() and EnumMap.
 *
 * Compile and run:
 *   javac EnumsAndSwitch.java
 *   java EnumsAndSwitch
 */
public class EnumsAndSwitch {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    enum Planet {
        MERCURY(3.303e+23, 2.4397e6),
        EARTH(5.976e+24, 6.37814e6),
        MARS(6.421e+23, 3.3972e6);

        private final double mass;   // kilograms
        private final double radius; // metres

        Planet(double mass, double radius) {
            this.mass = mass;
            this.radius = radius;
        }

        double surfaceGravity() {
            return 6.67300E-11 * mass / (radius * radius);
        }
    }

    enum Status {
        OPEN, IN_PROGRESS, DONE;

        boolean isFinished() {
            return this == DONE;
        }
    }

    static String describe(Status status) {
        return switch (status) {
            case OPEN -> "not started";
            case IN_PROGRESS -> "being worked on";
            case DONE -> "finished";
        };
    }

    public static void main(String[] args) {
        check(Planet.values().length == 3, "there are three planets");
        check(Planet.valueOf("EARTH") == Planet.EARTH, "valueOf finds EARTH");
        check(Planet.EARTH.name().equals("EARTH"), "name() returns the constant name");
        check(Planet.EARTH.ordinal() == 1, "EARTH is at ordinal 1");
        check(Planet.EARTH.surfaceGravity() > 9.7 && Planet.EARTH.surfaceGravity() < 9.9,
                "Earth gravity is about 9.8 m/s^2");

        check(describe(Status.OPEN).equals("not started"), "describe OPEN");
        check(describe(Status.IN_PROGRESS).equals("being worked on"), "describe IN_PROGRESS");
        check(describe(Status.DONE).equals("finished"), "describe DONE");
        check(!Status.OPEN.isFinished() && Status.DONE.isFinished(), "isFinished works");

        Map<Status, Integer> counts = new EnumMap<>(Status.class);
        for (Status status : Status.values()) {
            counts.put(status, 0);
        }
        counts.merge(Status.DONE, 1, Integer::sum);
        counts.merge(Status.OPEN, 3, Integer::sum);
        check(counts.get(Status.DONE) == 1 && counts.get(Status.OPEN) == 3, "EnumMap counts");

        for (Planet planet : Planet.values()) {
            System.out.printf("%-8s gravity %.2f m/s^2%n", planet, planet.surfaceGravity());
        }
        System.out.println("statuses : " + Arrays.toString(Status.values()));
        System.out.println("counts   : " + counts);
        System.out.println("All checks passed.");
    }
}
