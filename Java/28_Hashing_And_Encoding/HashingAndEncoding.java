import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 28 - Hashing and encoding: SHA-256 digests, Base64, an HMAC, salted password
 * hashing and constant-time comparison.
 *
 * Compile and run:
 *   javac HashingAndEncoding.java
 *   java HashingAndEncoding
 */
public class HashingAndEncoding {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static String sha256Hex(String text) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }

    static String hmacSha256(String key, String message) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
    }

    static byte[] saltedHash(byte[] salt, String password) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(salt);
        digest.update(password.getBytes(StandardCharsets.UTF_8));
        return digest.digest();
    }

    public static void main(String[] args) throws Exception {
        // ---- SHA-256: same input always gives the same 32 bytes -------------
        check(sha256Hex("hello").equals(
                "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"),
                "SHA-256 of hello");
        check(sha256Hex("").equals(
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"),
                "SHA-256 of the empty string");
        check(sha256Hex("hello").length() == 64, "hex form is 64 characters");
        check(!sha256Hex("hello").equals(sha256Hex("Hello")), "one letter changes everything");
        System.out.println("sha256(hello)   : " + sha256Hex("hello"));

        // ---- Base64: bytes as text -----------------------------------------
        String encoded = Base64.getEncoder()
                .encodeToString("hello".getBytes(StandardCharsets.UTF_8));
        check(encoded.equals("aGVsbG8="), "Base64 of hello");
        check(new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8)
                .equals("hello"), "Base64 round trip");
        byte[] awkward = {(byte) 0xFB, (byte) 0xFF};
        check(!Base64.getEncoder().encodeToString(awkward)
                .equals(Base64.getUrlEncoder().encodeToString(awkward)),
                "the URL-safe alphabet avoids + and /");
        System.out.println("base64(hello)   : " + encoded);

        // ---- HMAC: proves the message AND that the sender had the key ------
        String mac = hmacSha256("secret", "hello");
        check(mac.length() == 64, "HMAC-SHA256 is 32 bytes");
        check(mac.equals(hmacSha256("secret", "hello")), "HMAC is deterministic");
        check(!mac.equals(hmacSha256("other-key", "hello")), "a different key gives a different MAC");
        check(!mac.equals(hmacSha256("secret", "hellp")), "a changed message gives a different MAC");
        System.out.println("hmac(hello)     : " + mac.substring(0, 32) + "...");

        // ---- salted password hashing ---------------------------------------
        SecureRandom random = new SecureRandom();
        byte[] saltA = new byte[16];
        byte[] saltB = new byte[16];
        random.nextBytes(saltA);
        random.nextBytes(saltB);
        check(!Arrays.equals(saltA, saltB), "two random salts differ");
        check(Arrays.equals(saltedHash(saltA, "hunter2"), saltedHash(saltA, "hunter2")),
                "the same salt re-verifies a password");
        check(!Arrays.equals(saltedHash(saltA, "hunter2"), saltedHash(saltB, "hunter2")),
                "the same password under a different salt hashes differently");
        System.out.println("salt            : " + HexFormat.of().formatHex(saltA));

        // ---- constant-time comparison --------------------------------------
        check(MessageDigest.isEqual(saltA, saltA.clone()), "isEqual accepts identical bytes");
        check(!MessageDigest.isEqual(saltA, saltB), "isEqual rejects different bytes");
        check(!Arrays.equals(new byte[] {1, 2, 3}, new byte[] {1, 2, 4}), "Arrays.equals for content");
        System.out.println("All checks passed.");
    }
}
