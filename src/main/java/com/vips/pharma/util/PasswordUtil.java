package com.vips.pharma.util;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Lightweight password hashing using SHA-256 + random salt.
 *
 * Format stored in DB:  BASE64(salt) + "$" + BASE64(SHA256(salt + password))
 *
 * NOTE: For production use, replace with BCrypt (add dependency
 *       org.mindrot:jbcrypt). This implementation avoids adding a new
 *       Maven dependency while still being far safer than plain text.
 */
public final class PasswordUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {}

    /** Hash a plain-text password for storage. */
    public static String hash(String plainPassword) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        byte[] hash = sha256(salt, plainPassword);
        return Base64.getEncoder().encodeToString(salt)
                + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    /** Verify a plain-text password against a stored hash. */
    public static boolean verify(String plainPassword, String stored) {
        try {
            String[] parts = stored.split("\\$", 2);
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] expected = Base64.getDecoder().decode(parts[1]);
            byte[] actual   = sha256(salt, plainPassword);
            // Constant-time comparison
            if (expected.length != actual.length) return false;
            int diff = 0;
            for (int i = 0; i < expected.length; i++) diff |= expected[i] ^ actual[i];
            return diff == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] sha256(byte[] salt, String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            md.update(password.getBytes("UTF-8"));
            return md.digest();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
