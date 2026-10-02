import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 87 - HMAC signed tokens, in the shape of a JWT: three base64url parts where
 * the third is an HMAC-SHA256 over the first two. Shows why the signature must
 * be compared in constant time and why the algorithm must be checked.
 *
 * The JSON handling is deliberately minimal: flat objects of strings, numbers
 * and booleans, which is all a claim set needs here.
 *
 * Compile and run:
 *   javac SignedTokens.java
 *   java SignedTokens
 */
public class SignedTokens {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static final String HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

    // ------------------------------------------------------- minimal JSON
    static String toJson(Map<String, ?> values) {
        StringBuilder out = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            if (!first) {
                out.append(',');
            }
            first = false;
            out.append('"').append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof Number || value instanceof Boolean) {
                out.append(value);
            } else {
                out.append('"').append(String.valueOf(value).replace("\"", "\\\"")).append('"');
            }
        }
        return out.append('}').toString();
    }

    static Map<String, Object> fromJson(String text) {
        Map<String, Object> values = new LinkedHashMap<>();
        int index = text.indexOf('{');
        if (index < 0) {
            throw new IllegalArgumentException("not a JSON object: " + text);
        }
        index++;

        while (true) {
            while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
                index++;
            }
            if (index >= text.length() || text.charAt(index) == '}') {
                return values;
            }
            if (text.charAt(index) != '"') {
                throw new IllegalArgumentException("expected a quoted key at " + index);
            }
            int keyEnd = text.indexOf('"', index + 1);
            String key = text.substring(index + 1, keyEnd);
            index = text.indexOf(':', keyEnd) + 1;
            while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
                index++;
            }

            Object value;
            if (text.charAt(index) == '"') {
                int valueEnd = text.indexOf('"', index + 1);
                value = text.substring(index + 1, valueEnd);
                index = valueEnd + 1;
            } else {
                int valueEnd = index;
                while (valueEnd < text.length() && ",}".indexOf(text.charAt(valueEnd)) < 0) {
                    valueEnd++;
                }
                String raw = text.substring(index, valueEnd).trim();
                value = switch (raw) {
                    case "true" -> Boolean.TRUE;
                    case "false" -> Boolean.FALSE;
                    default -> Long.parseLong(raw);
                };
                index = valueEnd;
            }
            values.put(key, value);

            while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
                index++;
            }
            if (index < text.length() && text.charAt(index) == ',') {
                index++;
            }
        }
    }

    // ------------------------------------------------------------ base64url
    static String encodeBase64Url(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    static byte[] decodeBase64Url(String text) {
        return Base64.getUrlDecoder().decode(text);
    }

    static byte[] sign(String signingInput, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
    }

    static String issue(Map<String, ?> claims, String secret) throws Exception {
        String header = encodeBase64Url(HEADER_JSON.getBytes(StandardCharsets.UTF_8));
        String payload = encodeBase64Url(toJson(claims).getBytes(StandardCharsets.UTF_8));
        String signingInput = header + "." + payload;
        return signingInput + "." + encodeBase64Url(sign(signingInput, secret));
    }

    record Verified(Map<String, Object> claims, Map<String, Object> header) {
    }

    /**
     * Checks the algorithm, then the signature, then the expiry. The signature
     * comparison is constant time, because a byte-by-byte comparison that stops
     * early leaks how much of a forged signature was correct.
     */
    static Verified verify(String token, String secret, long nowSeconds) throws Exception {
        String[] parts = token.split("\\.", -1);
        if (parts.length != 3) {
            throw new IllegalArgumentException("a token has exactly three parts, got " + parts.length);
        }

        Map<String, Object> header = fromJson(new String(decodeBase64Url(parts[0]), StandardCharsets.UTF_8));
        if (!"HS256".equals(header.get("alg"))) {
            throw new IllegalArgumentException("unsupported algorithm: " + header.get("alg"));
        }

        byte[] expected = sign(parts[0] + "." + parts[1], secret);
        byte[] presented;
        try {
            presented = decodeBase64Url(parts[2]);
        } catch (IllegalArgumentException malformed) {
            throw new IllegalArgumentException("the signature is not valid base64url");
        }
        if (!MessageDigest.isEqual(expected, presented)) {
            throw new IllegalArgumentException("the signature does not match");
        }

        Map<String, Object> claims = fromJson(new String(decodeBase64Url(parts[1]), StandardCharsets.UTF_8));
        Object expiry = claims.get("exp");
        if (expiry instanceof Number moment && nowSeconds >= moment.longValue()) {
            throw new IllegalArgumentException("the token expired at " + moment);
        }
        return new Verified(claims, header);
    }

    public static void main(String[] args) throws Exception {
        String secret = "correct horse battery staple";

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", "user-42");
        claims.put("role", "tester");
        claims.put("exp", 2_000_000_000L);

        String token = issue(claims, secret);
        String[] parts = token.split("\\.");
        check(parts.length == 3, "a token is three parts");
        check(parts[0].equals(encodeBase64Url(HEADER_JSON.getBytes(StandardCharsets.UTF_8))),
                "the header is the base64url of the header JSON");
        check(!token.contains("="), "base64url without padding has no equals signs");
        System.out.println("token        : " + token.substring(0, 40) + "...");

        Map<String, Object> header = fromJson(new String(decodeBase64Url(parts[0]), StandardCharsets.UTF_8));
        check("HS256".equals(header.get("alg")), "the header says which algorithm was used");
        check("JWT".equals(header.get("typ")), "and the type");

        // ---- a good token verifies -------------------------------------------
        Verified verified = verify(token, secret, 1_000_000_000L);
        check(verified.claims().get("sub").equals("user-42"), "the subject comes back");
        check(verified.claims().get("role").equals("tester"), "and the role");
        check(verified.claims().get("exp").equals(2_000_000_000L), "and the expiry");
        System.out.println("verified     : sub=" + verified.claims().get("sub")
                + " role=" + verified.claims().get("role"));

        // ---- determinism ------------------------------------------------------
        check(issue(claims, secret).equals(token), "the same claims and secret give the same token");

        // ---- tampering --------------------------------------------------------
        String tamperedPayload = parts[0] + "."
                + encodeBase64Url(toJson(Map.of("sub", "admin", "role", "admin", "exp", 2_000_000_000L))
                        .getBytes(StandardCharsets.UTF_8))
                + "." + parts[2];
        try {
            verify(tamperedPayload, secret, 1_000_000_000L);
            throw new AssertionError("a changed payload must not verify");
        } catch (IllegalArgumentException expected) {
            check(expected.getMessage().contains("signature"), "the payload change breaks the signature");
        }

        byte[] signature = decodeBase64Url(parts[2]);
        signature[0] ^= 0x01;
        String brokenSignature = parts[0] + "." + parts[1] + "." + encodeBase64Url(signature);
        try {
            verify(brokenSignature, secret, 1_000_000_000L);
            throw new AssertionError("a changed signature must not verify");
        } catch (IllegalArgumentException expected) {
            check(expected.getMessage().contains("signature"), "one flipped bit is enough");
        }

        try {
            verify(token, "a different secret", 1_000_000_000L);
            throw new AssertionError("the wrong secret must not verify");
        } catch (IllegalArgumentException expected) {
            check(expected.getMessage().contains("signature"), "the secret is what makes it unforgeable");
        }
        System.out.println("tampering    : payload, signature and wrong secret are all rejected");

        // ---- the alg none attack ---------------------------------------------
        String noneHeader = encodeBase64Url("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String nonePayload = encodeBase64Url(toJson(Map.of("sub", "admin")).getBytes(StandardCharsets.UTF_8));
        try {
            verify(noneHeader + "." + nonePayload + ".", secret, 0);
            throw new AssertionError("alg none must be refused");
        } catch (IllegalArgumentException expected) {
            check(expected.getMessage().contains("unsupported algorithm"),
                    "the algorithm is checked before the signature");
            System.out.println("alg none     : " + expected.getMessage());
        }

        // ---- malformed tokens --------------------------------------------------
        for (String malformed : new String[] {"", "one", "two.parts", "a.b.c.d", "..", "a.b.!!!"}) {
            try {
                verify(malformed, secret, 0);
                throw new AssertionError("should have been rejected: " + malformed);
            } catch (IllegalArgumentException expected) {
                // expected
            }
        }
        System.out.println("malformed    : six broken tokens all rejected");

        // ---- expiry ------------------------------------------------------------
        check(verify(token, secret, 1_999_999_999L).claims().get("exp").equals(2_000_000_000L),
                "one second before the expiry is fine");
        try {
            verify(token, secret, 2_000_000_000L);
            throw new AssertionError("the moment of expiry must fail");
        } catch (IllegalArgumentException expected) {
            check(expected.getMessage().contains("expired"), "expiry is inclusive");
            System.out.println("expiry       : " + expected.getMessage());
        }

        // a token with no expiry claim never expires
        String forever = issue(Map.of("sub", "service"), secret);
        check(verify(forever, secret, 9_999_999_999L).claims().get("sub").equals("service"),
                "no exp claim means no expiry check");
        System.out.println("no expiry    : a token without exp is still accepted");
        System.out.println("All checks passed.");
    }
}
