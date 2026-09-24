import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 36 - Symmetric encryption with AES-GCM: generate a key, encrypt a message with
 * a fresh IV, decrypt it back, and watch tampering get rejected.
 *
 * Compile and run:
 *   javac EncryptionAes.java
 *   java EncryptionAes
 */
public class EncryptionAes {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static final int TAG_BITS = 128;   // authentication tag length
    static final int IV_BYTES = 12;    // 96 bits is the recommended GCM nonce size

    record Sealed(byte[] iv, byte[] ciphertext) {
    }

    static SecretKey newKey() throws Exception {
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(256);
        return generator.generateKey();
    }

    static Sealed encrypt(SecretKey key, String plaintext) throws Exception {
        byte[] iv = new byte[IV_BYTES];
        new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
        byte[] sealed = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        return new Sealed(iv, sealed);
    }

    static String decrypt(SecretKey key, byte[] iv, byte[] ciphertext) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
        byte[] plain = cipher.doFinal(ciphertext);
        return new String(plain, StandardCharsets.UTF_8);
    }

    public static void main(String[] args) throws Exception {
        String message = "card=4111-1111-1111-1111; amount=17.27";

        SecretKey key = newKey();
        check(key.getAlgorithm().equals("AES"), "key algorithm");
        check(key.getEncoded().length == 32, "a 256-bit key is 32 bytes");
        System.out.println("key          : " + key.getEncoded().length * 8 + "-bit AES");

        Sealed first = encrypt(key, message);
        Sealed second = encrypt(key, message);
        check(first.ciphertext().length > message.length(),
                "the ciphertext carries the 16-byte authentication tag");
        check(!Arrays.equals(first.iv(), second.iv()), "each encryption uses a fresh IV");
        check(!Arrays.equals(first.ciphertext(), second.ciphertext()),
                "the same plaintext encrypts differently every time");

        check(decrypt(key, first.iv(), first.ciphertext()).equals(message), "round trip");
        check(decrypt(key, second.iv(), second.ciphertext()).equals(message),
                "the second ciphertext also decrypts");
        System.out.println("ciphertext   : " + Base64.getEncoder().encodeToString(first.ciphertext()));
        System.out.println("iv           : " + Base64.getEncoder().encodeToString(first.iv()));

        // A wrong key must not decrypt.
        try {
            decrypt(newKey(), first.iv(), first.ciphertext());
            throw new AssertionError("a different key should not decrypt");
        } catch (AEADBadTagException expected) {
            System.out.println("wrong key    : rejected (" + expected.getClass().getSimpleName() + ")");
        }

        // GCM authenticates the data, so a single flipped bit is caught.
        byte[] tampered = first.ciphertext().clone();
        tampered[tampered.length - 1] ^= 0x01;
        try {
            decrypt(key, first.iv(), tampered);
            throw new AssertionError("tampered ciphertext should not decrypt");
        } catch (AEADBadTagException expected) {
            System.out.println("tampering    : detected, decryption refused");
        }

        // A reused key with the same plaintext is still safe because the IV changes.
        Sealed third = encrypt(key, "second message");
        check(decrypt(key, third.iv(), third.ciphertext()).equals("second message"),
                "a different message round trips");
        check(!Arrays.equals(third.iv(), first.iv()), "the IV moved on again");

        // Keys can be reconstructed from raw bytes, which is what a keystore would store.
        SecretKey restored = new SecretKeySpec(key.getEncoded(), "AES");
        check(restored.getEncoded().length == 32, "a key rebuilt from bytes is the same size");
        check(decrypt(restored, first.iv(), first.ciphertext()).equals(message),
                "the rebuilt key decrypts the original message");
        System.out.println("All checks passed.");
    }
}
