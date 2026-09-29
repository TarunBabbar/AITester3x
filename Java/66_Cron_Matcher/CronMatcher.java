import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 66 - A cron expression matcher: parse the five fields, decide whether a given
 * minute matches, and compute the next runs.
 *
 * Fields are minute hour day-of-month month day-of-week, with * , - and / steps.
 * Day-of-week is 0-7 where both 0 and 7 mean Sunday. When BOTH day fields are
 * restricted, classic cron fires when EITHER matches; that rule is implemented
 * and tested below.
 *
 * Compile and run:
 *   javac CronMatcher.java
 *   java CronMatcher
 */
public class CronMatcher {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static final class CronSchedule {
        private final boolean[] minutes = new boolean[60];
        private final boolean[] hours = new boolean[24];
        private final boolean[] days = new boolean[32];      // 1..31
        private final boolean[] months = new boolean[13];    // 1..12
        private final boolean[] weekdays = new boolean[8];   // 0..6 plus 7 for Sunday
        private final boolean dayWildcard;
        private final boolean weekdayWildcard;

        private CronSchedule(String minute, String hour, String day, String month, String weekday) {
            fill(minutes, minute, 0, 59);
            fill(hours, hour, 0, 23);
            fill(days, day, 1, 31);
            fill(months, month, 1, 12);
            fill(weekdays, weekday, 0, 7);
            if (weekdays[7]) {
                weekdays[0] = true;      // 7 is an alias for Sunday
            }
            this.dayWildcard = day.trim().equals("*");
            this.weekdayWildcard = weekday.trim().equals("*");
        }

        static CronSchedule parse(String expression) {
            String[] fields = expression.trim().split("\\s+");
            if (fields.length != 5) {
                throw new IllegalArgumentException("expected 5 fields, got " + fields.length);
            }
            return new CronSchedule(fields[0], fields[1], fields[2], fields[3], fields[4]);
        }

        /** Handles a wildcard, a single value, a range, a step, and comma separated lists. */
        private static void fill(boolean[] target, String field, int minimum, int maximum) {
            for (String part : field.split(",")) {
                String range = part;
                int step = 1;
                int slash = part.indexOf('/');
                if (slash >= 0) {
                    range = part.substring(0, slash);
                    step = Integer.parseInt(part.substring(slash + 1));
                    if (step < 1) {
                        throw new IllegalArgumentException("step must be at least 1: " + part);
                    }
                }

                int from = minimum;
                int to = maximum;
                if (!range.equals("*")) {
                    int dash = range.indexOf('-');
                    if (dash >= 0) {
                        from = Integer.parseInt(range.substring(0, dash));
                        to = Integer.parseInt(range.substring(dash + 1));
                    } else {
                        from = Integer.parseInt(range);
                        to = slash >= 0 ? maximum : from;
                    }
                }
                if (from < minimum || to > maximum || from > to) {
                    throw new IllegalArgumentException(
                            "out of range in field: " + part + " (allowed " + minimum + "-" + maximum + ")");
                }
                for (int value = from; value <= to; value += step) {
                    target[value] = true;
                }
            }
        }

        boolean matches(LocalDateTime time) {
            if (!minutes[time.getMinute()] || !hours[time.getHour()] || !months[time.getMonthValue()]) {
                return false;
            }
            boolean onDay = days[time.getDayOfMonth()];
            boolean onWeekday = weekdays[time.getDayOfWeek().getValue() % 7];

            if (dayWildcard && weekdayWildcard) {
                return true;
            }
            if (dayWildcard) {
                return onWeekday;
            }
            if (weekdayWildcard) {
                return onDay;
            }
            return onDay || onWeekday;    // classic cron: either field may match
        }

        /** The next matches strictly after {@code from}, minute by minute. */
        List<LocalDateTime> next(LocalDateTime from, int count) {
            List<LocalDateTime> runs = new ArrayList<>();
            LocalDateTime candidate = from.withSecond(0).withNano(0).plusMinutes(1);
            int guard = 0;
            while (runs.size() < count) {
                if (matches(candidate)) {
                    runs.add(candidate);
                }
                candidate = candidate.plusMinutes(1);
                if (++guard > 6_000_000) {
                    throw new IllegalStateException("no match found within the search window");
                }
            }
            return runs;
        }
    }

    public static void main(String[] args) {
        // ---- every quarter hour ---------------------------------------------
        CronSchedule quarterHourly = CronSchedule.parse("*/15 * * * *");
        List<LocalDateTime> quarter = quarterHourly.next(LocalDateTime.of(2026, 1, 5, 9, 0), 4);
        check(quarter.get(0).equals(LocalDateTime.of(2026, 1, 5, 9, 15)), "the next quarter hour");
        check(quarter.get(3).equals(LocalDateTime.of(2026, 1, 5, 10, 0)), "and the fourth");
        check(quarterHourly.matches(LocalDateTime.of(2026, 1, 5, 9, 45)), "45 matches");
        check(!quarterHourly.matches(LocalDateTime.of(2026, 1, 5, 9, 46)), "46 does not");
        System.out.println("*/15        : " + quarter);

        // ---- 09:00 on weekdays ----------------------------------------------
        CronSchedule weekdayNine = CronSchedule.parse("0 9 * * 1-5");
        List<LocalDateTime> mornings = weekdayNine.next(LocalDateTime.of(2026, 1, 5, 0, 0), 6);
        check(mornings.get(0).equals(LocalDateTime.of(2026, 1, 5, 9, 0)), "Monday morning");
        check(mornings.get(4).equals(LocalDateTime.of(2026, 1, 9, 9, 0)), "Friday morning");
        check(mornings.get(5).equals(LocalDateTime.of(2026, 1, 12, 9, 0)), "then it skips the weekend");
        System.out.println("0 9 * * 1-5 : " + mornings);

        // ---- a day-of-month schedule ----------------------------------------
        CronSchedule firstOfMonth = CronSchedule.parse("30 6 1 * *");
        check(firstOfMonth.next(LocalDateTime.of(2026, 1, 5, 0, 0), 1).get(0)
                .equals(LocalDateTime.of(2026, 2, 1, 6, 30)), "the 1st of February");
        check(firstOfMonth.next(LocalDateTime.of(2026, 1, 5, 0, 0), 2).get(1)
                .equals(LocalDateTime.of(2026, 3, 1, 6, 30)), "then the 1st of March");
        System.out.println("30 6 1 * *  : " + firstOfMonth.next(LocalDateTime.of(2026, 1, 5, 0, 0), 2));

        // ---- once a year -----------------------------------------------------
        CronSchedule newYear = CronSchedule.parse("0 0 1 1 *");
        check(newYear.next(LocalDateTime.of(2026, 1, 5, 0, 0), 1).get(0)
                .equals(LocalDateTime.of(2027, 1, 1, 0, 0)), "midnight on 1 January");
        System.out.println("0 0 1 1 *   : " + newYear.next(LocalDateTime.of(2026, 1, 5, 0, 0), 1));

        // ---- both day fields restricted: either may match --------------------
        CronSchedule mondayOrFirst = CronSchedule.parse("0 0 1 * 1");
        List<LocalDateTime> either = mondayOrFirst.next(LocalDateTime.of(2026, 1, 5, 0, 0), 5);
        check(either.get(0).equals(LocalDateTime.of(2026, 1, 12, 0, 0)), "Monday 12 January");
        check(either.get(3).equals(LocalDateTime.of(2026, 2, 1, 0, 0)), "then the 1st of February, a Sunday");
        check(either.get(4).equals(LocalDateTime.of(2026, 2, 2, 0, 0)), "then Monday 2 February");
        System.out.println("0 0 1 * 1   : " + either);
        check(!mondayOrFirst.matches(LocalDateTime.of(2026, 1, 13, 0, 0)),
                "Tuesday the 13th matches neither field");

        // ---- steps inside a range, and lists ---------------------------------
        CronSchedule custom = CronSchedule.parse("0,30 9-17/4 * * *");
        check(custom.matches(LocalDateTime.of(2026, 1, 5, 9, 0)), "hour 9 is in 9-17/4");
        check(custom.matches(LocalDateTime.of(2026, 1, 5, 13, 30)), "hour 13 is too");
        check(!custom.matches(LocalDateTime.of(2026, 1, 5, 10, 0)), "hour 10 is not");
        check(!custom.matches(LocalDateTime.of(2026, 1, 5, 21, 0)), "hour 21 is outside the range");
        List<LocalDateTime> customRuns = custom.next(LocalDateTime.of(2026, 1, 5, 8, 0), 3);
        check(customRuns.get(0).equals(LocalDateTime.of(2026, 1, 5, 9, 0)), "first custom run");
        check(customRuns.get(2).equals(LocalDateTime.of(2026, 1, 5, 13, 0)), "third custom run");
        System.out.println("0,30 9-17/4 : " + customRuns);

        // ---- Sunday is 0 or 7 -------------------------------------------------
        LocalDateTime sunday = LocalDateTime.of(2026, 1, 11, 0, 0);
        check(sunday.getDayOfWeek() == DayOfWeek.SUNDAY, "the test date really is a Sunday");
        check(CronSchedule.parse("0 0 * * 0").matches(sunday), "0 means Sunday");
        check(CronSchedule.parse("0 0 * * 7").matches(sunday), "7 means Sunday as well");
        check(!CronSchedule.parse("0 0 * * 1").matches(sunday), "1 is Monday, not Sunday");
        check(CronSchedule.parse("0 0 * * 5-7").matches(sunday), "a range ending at 7 includes Sunday");

        // ---- bad expressions fail loudly -------------------------------------
        for (String bad : List.of("* * * *", "60 * * * *", "* 24 * * *", "* * 0 * *",
                "* * * 13 *", "*/0 * * * *", "5-1 * * * *", "a b c d e")) {
            try {
                CronSchedule.parse(bad);
                throw new AssertionError("should have been rejected: " + bad);
            } catch (IllegalArgumentException expected) {
                System.out.printf("rejected    : %-12s -> %s%n", bad, expected.getMessage());
            }
        }
        System.out.println("All checks passed.");
    }
}
