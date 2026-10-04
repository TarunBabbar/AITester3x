import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 98 - LZW compression from scratch: build a dictionary of phrases as you read,
 * emit the code for the longest phrase you already know, and teach the decoder
 * the same dictionary so it can rebuild the text from the codes alone.
 *
 * Compile and run:
 *   javac LzwCompression.java
 *   java LzwCompression
 */
public class LzwCompression {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static final int FIRST_NEW_CODE = 256;

    /** Longest-match encoding: the codes are indices into a growing dictionary. */
    static List<Integer> encode(String text) {
        Map<String, Integer> dictionary = new HashMap<>();
        for (int code = 0; code < FIRST_NEW_CODE; code++) {
            dictionary.put(String.valueOf((char) code), code);
        }
        int nextCode = FIRST_NEW_CODE;

        List<Integer> codes = new ArrayList<>();
        String current = "";
        for (int i = 0; i < text.length(); i++) {
            String candidate = current + text.charAt(i);
            if (dictionary.containsKey(candidate)) {
                current = candidate;                   // keep extending the known phrase
            } else {
                codes.add(dictionary.get(current));    // emit what we had
                dictionary.put(candidate, nextCode++); // learn the longer phrase
                current = String.valueOf(text.charAt(i));
            }
        }
        if (!current.isEmpty()) {
            codes.add(dictionary.get(current));
        }
        return codes;
    }

    /**
     * The decoder rebuilds the dictionary from the codes, learning one entry per
     * code. The one wrinkle is a code equal to the next free index, which means
     * the encoder used a phrase it had only just created.
     */
    static String decode(List<Integer> codes) {
        Map<Integer, String> dictionary = new HashMap<>();
        for (int code = 0; code < FIRST_NEW_CODE; code++) {
            dictionary.put(code, String.valueOf((char) code));
        }
        int nextCode = FIRST_NEW_CODE;

        StringBuilder out = new StringBuilder();
        String previous = null;
        for (int code : codes) {
            String entry;
            if (dictionary.containsKey(code)) {
                entry = dictionary.get(code);
            } else if (previous != null && code == nextCode) {
                entry = previous + previous.charAt(0);   // the just-created phrase
            } else {
                throw new IllegalArgumentException("code " + code + " is not in the dictionary");
            }
            out.append(entry);
            if (previous != null) {
                dictionary.put(nextCode++, previous + entry.charAt(0));
            }
            previous = entry;
        }
        return out.toString();
    }

    /** How many bits each code needs once the dictionary has grown to this size. */
    static int bitsPerCode(int dictionarySize) {
        int bits = 0;
        while ((1 << bits) < dictionarySize) {
            bits++;
        }
        return Math.max(bits, 8);
    }

    static int packedBits(List<Integer> codes) {
        int largest = 0;
        for (int code : codes) {
            largest = Math.max(largest, code);
        }
        return codes.size() * bitsPerCode(largest + 1);
    }

    static String summarise(String text) {
        List<Integer> codes = encode(text);
        return text.length() + " chars -> " + codes.size() + " codes ("
                + packedBits(codes) + " bits, " + (text.length() * 8) + " raw)";
    }

    public static void main(String[] args) {
        // ---- a hand-checked example -----------------------------------------
        check(encode("ABA").equals(List.of(65, 66, 65)),
                "ABA encodes to the two single letters and then A again");
        check(encode("ABAABA").equals(List.of(65, 66, 65, 256, 65)),
                "the second AB is emitted as the new code 256");
        check(decode(List.of(65, 66, 65, 256, 65)).equals("ABAABA"),
                "and the decoder rebuilds it from the codes alone");
        System.out.println("hand checked : ABAABA -> " + encode("ABAABA"));

        // ---- the awkward case, where a code arrives before the decoder has it --
        List<Integer> repeated = encode("aaaa");
        check(repeated.equals(List.of(97, 256, 97)), "aaaa encodes to a, aa, a");
        check(decode(repeated).equals("aaaa"), "which only decodes if the special case is handled");
        check(encode("aaaaaaaaaa").size() < 6, "a longer run needs very few codes: "
                + encode("aaaaaaaaaa"));
        System.out.println("self referent: " + repeated + " decodes to \"aaaa\"");

        // ---- round trips ------------------------------------------------------
        List<String> samples = List.of(
                "",
                "a",
                "ab",
                "aaaaaaa",
                "ababababab",
                "the quick brown fox jumps over the lazy dog",
                "TOBEORNOTTOBEORTOBEORNOT",
                "mississippi",
                String.join("", java.util.Collections.nCopies(50, "repeat me ")));
        for (String sample : samples) {
            List<Integer> codes = encode(sample);
            check(decode(codes).equals(sample),
                    "round trip failed for " + sample.length() + " characters");
        }
        check(decode(encode("")).isEmpty(), "the empty string encodes to no codes at all");
        System.out.println("round trips  : " + samples.size() + " inputs, shortest to 500 characters");

        // ---- the dictionary grows past a byte --------------------------------
        String longText = String.join(" ", java.util.Collections.nCopies(400, "the same phrase again"));
        List<Integer> longCodes = encode(longText);
        int largest = longCodes.stream().mapToInt(Integer::intValue).max().orElse(0);
        check(largest >= FIRST_NEW_CODE, "codes above 255 appear once the dictionary grows: " + largest);
        check(decode(longCodes).equals(longText), "and the long text round trips");
        System.out.println("dictionary   : grew to " + (largest + 1) + " entries"
                + ", needing " + bitsPerCode(largest + 1) + " bits per code");

        // ---- and it compresses repetitive text --------------------------------
        int raw = longText.length() * 8;
        int packed = packedBits(longCodes);
        check(packed < raw / 2, "repetition compresses well: " + packed + " bits against " + raw);
        System.out.println("compression  : " + summarise(longText));

        // ---- incompressible text does not -------------------------------------
        StringBuilder scattered = new StringBuilder();
        java.util.Random random = new java.util.Random(7);
        for (int i = 0; i < 400; i++) {
            scattered.append((char) (33 + random.nextInt(90)));
        }
        List<Integer> scatteredCodes = encode(scattered.toString());
        check(decode(scatteredCodes).equals(scattered.toString()), "scattered text still round trips");
        check(packedBits(scatteredCodes) >= scattered.length() * 8 * 0.9,
                "but there is little to gain: " + summarise(scattered.toString()));
        System.out.println("incompressible: " + summarise(scattered.toString()));

        // ---- a natural paragraph ---------------------------------------------
        String paragraph = "It was the best of times, it was the worst of times, "
                + "it was the age of wisdom, it was the age of foolishness, "
                + "it was the epoch of belief, it was the epoch of incredulity.";
        check(decode(encode(paragraph)).equals(paragraph), "a paragraph round trips");
        System.out.println("paragraph    : " + summarise(paragraph));

        // ---- codes that cannot decode ----------------------------------------
        try {
            decode(List.of(65, 66, 9999));
            throw new AssertionError("a code past the dictionary should be rejected");
        } catch (IllegalArgumentException expected) {
            System.out.println("bad code     : " + expected.getMessage());
        }

        // ---- decoding is not idempotent, and that is fine ---------------------
        List<Integer> codes = encode("hello hello hello");
        check(decode(codes).equals("hello hello hello"), "the first decode works");
        check(decode(codes).equals("hello hello hello"), "and so does the second: decode keeps no state");
        System.out.println("stateless    : decoding twice gives the same text");
        System.out.println("All checks passed.");
    }
}
