import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

/**
 * 69 - Asymmetric cryptography with RSA: encrypt to a public key, decrypt with
 * the private key, sign with the private key and verify with the public one.
 *
 * Compile and run:
 *   javac RsaCrypto.java
 *   java RsaCrypto
 */
public class RsaCrypto {

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static final String TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";

    static byte[] encrypt(PublicKey key, String message) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(message.getBytes(StandardCharsets.UTF_8));
    }

    static String decrypt(PrivateKey key, byte[] sealed) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, key);
        return new String(cipher.doFinal(sealed), StandardCharsets.UTF_8);
    }

    static byte[] sign(PrivateKey key, String message) throws Exception {
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(key);
        signer.update(message.getBytes(StandardCharsets.UTF_8));
        return signer.sign();
    }

    static boolean verify(PublicKey key, String message, byte[] signature) throws Exception {
        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(key);
        verifier.update(message.getBytes(StandardCharsets.UTF_8));
        try {
            return verifier.verify(signature);
        } catch (SignatureException malformed) {
            return false;   // a mangled signature is simply "not verified"
        }
    }

    static byte[] tamper(byte[] signature) {
        byte[] broken = signature.clone();
        broken[broken.length - 1] ^= 0x01;
        return broken;
    }

    public static void main(String[] args) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        PublicKey publicKey = pair.getPublic();
        PrivateKey privateKey = pair.getPrivate();

        check(publicKey.getAlgorithm().equals("RSA"), "the algorithm is RSA");
        check(((RSAPublicKey) publicKey).getModulus().bitLength() == 2048, "a 2048-bit modulus");
        check(publicKey.getEncoded().length > 0, "the public key can be exported as bytes");

        // ---- encrypt with the public key, decrypt with the private ----------
        String message = "card=4111-1111-1111-1234; amount=17.27";
        byte[] sealed = encrypt(publicKey, message);
        check(sealed.length == 256, "an RSA-2048 ciphertext is exactly 256 bytes");
        check(decrypt(privateKey, sealed).equals(message), "the message round trips");

        byte[] sealedAgain = encrypt(publicKey, message);
        check(!Arrays.equals(sealed, sealedAgain),
                "OAEP padding is randomised, so the same message encrypts differently");
        check(decrypt(privateKey, sealedAgain).equals(message), "and both decrypt");
        System.out.println("ciphertext  : " + sealed.length + " bytes, "
                + Base64.getEncoder().encodeToString(sealed).substring(0, 24) + "...");

        // ---- one block is small: this is why real systems use hybrid crypto --
        try {
            encrypt(publicKey, "x".repeat(512));
            throw new AssertionError("512 bytes should not fit in one RSA block");
        } catch (IllegalBlockSizeException expected) {
            System.out.println("too big     : " + expected.getMessage());
        }
        // In practice you encrypt a random AES key with RSA and the payload with
        // that key - which is exactly the pattern from the AES program.

        // ---- signatures ------------------------------------------------------
        byte[] signature = sign(privateKey, message);
        check(signature.length == 256, "the signature is 256 bytes for RSA-2048");
        check(verify(publicKey, message, signature), "a good signature verifies");
        check(!verify(publicKey, message + "!", signature), "a changed message fails");
        check(!verify(publicKey, message, tamper(signature)), "a changed signature fails");

        KeyPair stranger = generator.generateKeyPair();
        check(!verify(stranger.getPublic(), message, signature),
                "the wrong public key fails, which is the whole point");
        System.out.println("signature   : verifies, and rejects a one-bit change");

        // ---- PKCS#1 v1.5 signatures are deterministic ------------------------
        check(Arrays.equals(signature, sign(privateKey, message)),
                "the same key and message give the same signature");
        check(!Arrays.equals(sign(stranger.getPrivate(), message), signature),
                "a different key gives a different signature");

        // ---- rebuild the public key from its bytes ---------------------------
        byte[] encoded = publicKey.getEncoded();
        PublicKey rebuilt = KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(encoded));
        check(verify(rebuilt, message, signature), "a key rebuilt from bytes still verifies");
        check(Arrays.equals(rebuilt.getEncoded(), encoded), "and re-encodes to the same bytes");
        System.out.println("public key  : " + encoded.length + " bytes, "
                + Base64.getEncoder().encodeToString(encoded).substring(0, 24) + "...");
        System.out.println("All checks passed.");
    }
}
