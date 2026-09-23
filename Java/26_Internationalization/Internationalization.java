import java.text.MessageFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ListResourceBundle;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * 26 - Internationalization: Locale, NumberFormat, DateTimeFormatter and
 * MessageFormat, plus a small class-based ResourceBundle.
 *
 * Compile and run:
 *   javac Internationalization.java
 *   java Internationalization
 */
public class Internationalization {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /** A bundle written in code, so this program needs no .properties file. */
    static class Greetings extends ListResourceBundle {
        @Override
        protected Object[][] getContents() {
            return new Object[][] {
                    {"hello", "Hello"},
                    {"bye", "Goodbye"},
            };
        }
    }

    public static void main(String[] args) {
        Locale us = Locale.US;
        Locale germany = Locale.GERMANY;
        Locale india = Locale.forLanguageTag("en-IN");

        check(us.getLanguage().equals("en") && us.getCountry().equals("US"), "US locale parts");
        check(us.getDisplayLanguage(Locale.US).equals("English"), "display language");
        check(Locale.forLanguageTag("en-IN").equals(india), "forLanguageTag round trip");

        NumberFormat usMoney = NumberFormat.getCurrencyInstance(us);
        NumberFormat deMoney = NumberFormat.getCurrencyInstance(germany);
        NumberFormat inMoney = NumberFormat.getCurrencyInstance(india);
        check(usMoney.format(1234.5).equals("$1,234.50"), "US currency uses a dot and a comma");
        check(deMoney.format(1234.5).contains(","), "German currency uses a decimal comma");
        System.out.println("money US / DE / IN : "
                + usMoney.format(1234.5) + "  |  " + deMoney.format(1234.5) + "  |  " + inMoney.format(1234.5));

        NumberFormat plain = NumberFormat.getNumberInstance(us);
        check(plain.format(1234567.891).equals("1,234,567.891"), "grouped number");
        NumberFormat percent = NumberFormat.getPercentInstance(us);
        check(percent.format(0.256).equals("26%"), "percent rounds to whole numbers");
        System.out.println("number / percent   : " + plain.format(1234567.891) + "  |  " + percent.format(0.256));

        try {
            check(usMoney.parse("$1,234.50").doubleValue() == 1234.5, "parsing a US amount");
        } catch (Exception wrong) {
            throw new AssertionError("parsing should have worked", wrong);
        }

        LocalDate release = LocalDate.of(2026, 3, 16);
        String english = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.US).format(release);
        String german = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.GERMANY).format(release);
        check(english.equals("16 March 2026"), "English month name");
        check(german.startsWith("16 ") && german.endsWith(" 2026"), "German month name shape");
        System.out.println("date US / DE       : " + english + "  |  " + german);

        String message = new MessageFormat("Hello {0}, you have {1,number,integer} messages.", Locale.US)
                .format(new Object[] {"Ada", 3});
        check(message.equals("Hello Ada, you have 3 messages."), "MessageFormat output");
        System.out.println("message            : " + message);

        ResourceBundle bundle = new Greetings();
        check(bundle.getString("hello").equals("Hello"), "bundle lookup");
        check(bundle.containsKey("bye"), "bundle contains bye");
        System.out.println("bundle             : " + bundle.getString("hello") + " / " + bundle.getString("bye"));
        System.out.println("All checks passed.");
    }
}
