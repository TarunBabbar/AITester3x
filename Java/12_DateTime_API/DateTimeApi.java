import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * 12 - Date and time API: LocalDate, LocalTime, Duration, Period, zones and formatting.
 *
 * Compile and run:
 *   javac DateTimeApi.java
 *   java DateTimeApi
 */
public class DateTimeApi {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) {
        LocalDate start = LocalDate.of(2026, 1, 5);
        LocalDate end = LocalDate.of(2026, 3, 16);

        check(start.getDayOfWeek() == DayOfWeek.MONDAY, "2026-01-05 is a Monday");
        check(ChronoUnit.DAYS.between(start, end) == 70, "70 days between the dates");
        check(Period.between(start, end).getMonths() == 2, "2 whole months between");
        check(Period.between(start, end).getDays() == 11, "plus 11 days");
        check(start.plusDays(7).equals(LocalDate.of(2026, 1, 12)), "plusDays(7)");
        check(end.minusWeeks(2).equals(LocalDate.of(2026, 3, 2)), "minusWeeks(2)");
        check(!end.isLeapYear(), "2026 is not a leap year");
        check(LocalDate.of(2024, 2, 29).plusDays(1).equals(LocalDate.of(2024, 3, 1)),
                "a leap day rolls into March");

        LocalTime begin = LocalTime.of(9, 30);
        LocalTime finish = LocalTime.of(12, 0);
        Duration duration = Duration.between(begin, finish);
        check(duration.toMinutes() == 150, "2h30m is 150 minutes");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        check(start.format(formatter).equals("05/01/2026"), "formatted date");
        check(LocalDate.parse("2026-03-16", DateTimeFormatter.ISO_LOCAL_DATE).equals(end),
                "parsing an ISO date");

        ZonedDateTime tokyo = ZonedDateTime.of(2026, 1, 5, 15, 0, 0, 0, ZoneId.of("Asia/Tokyo"));
        ZonedDateTime utc = tokyo.withZoneSameInstant(ZoneOffset.UTC);
        check(utc.getHour() == 6, "15:00 in Tokyo is 06:00 UTC");
        check(Instant.EPOCH.getEpochSecond() == 0, "the epoch is zero");

        System.out.println("start        : " + start + " (" + start.getDayOfWeek() + ")");
        System.out.println("end          : " + end);
        System.out.println("days between : " + ChronoUnit.DAYS.between(start, end));
        System.out.println("period       : " + Period.between(start, end));
        System.out.println("formatted    : " + start.format(formatter));
        System.out.println("duration     : " + duration.toMinutes() + " minutes");
        System.out.println("Tokyo 15:00  : " + tokyo);
        System.out.println("same instant : " + utc);
        System.out.println("All checks passed.");
    }
}
