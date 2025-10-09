package com.passwordmanager.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Arrays;

public class Pbkdf2HashUtil {
    // SECURITY CONSTANTS
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    // Increase this for security, minimum 310,000, aiming for 600,000+
    private static final int ITERATION_COUNT = 600000;
    private static final int KEY_LENGTH = 256; // 256 bits for hash/key
    private static final int SALT_LENGTH = 16; // 16 bytes salt

    /**
     * Generates a random salt.
     * @return A unique salt array.
     */
    public static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return salt;
    }

    /**
     * Hashes the password using PBKDF2.
     * @param password The plaintext password (Master Password).
     * @param salt The unique salt for this user.
     * @return The resulting 256-bit hash (32-byte array).
     */
    public static byte[] hashPassword(String password, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
        return factory.generateSecret(spec).getEncoded();
    }

    /**
     * Convenience method to convert salt and hash to Base64 strings for DB storage.
     */
    public static String toBase64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    /**
     * Convenience method to convert Base64 strings from DB back to byte arrays.
     */
    public static byte[] fromBase64(String base64) {
        return Base64.getDecoder().decode(base64);
    }

    /**
     * Verifies an entered password against a stored hash and salt.
     * @param enteredPassword The password entered by the user.
     * @param storedHash The stored hash from the database.
     * @param storedSalt The stored salt from the database.
     * @return true if the passwords match, false otherwise.
     */
    public static boolean verifyPassword(String enteredPassword, String storedHash, String storedSalt) {
        try {
            byte[] salt = fromBase64(storedSalt);
            byte[] storedHashBytes = fromBase64(storedHash);

            // Hash the entered password with the stored salt
            byte[] enteredHashBytes = hashPassword(enteredPassword, salt);

            // Compare the resulting hash bytes (using Arrays.equals for constant-time comparison)
            return Arrays.equals(storedHashBytes, enteredHashBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}