package com.passwordmanager.util;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Arrays;

public class AesGcmEncryptionUtil {

    // --- Constants ---
    private static final int GCM_NONCE_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    private static final String ALGORITHM = "AES";
    private static final String CIPHER_MODE = "AES/GCM/NoPadding";

    /**
     * Helper class to hold separate nonce and ciphertext Base64 strings.
     */
    public static class EncryptedResult { // Renamed/consolidated to EncryptedResult
        private final String nonceBase64;
        private final String cipherTextBase64;

        public EncryptedResult(String cipherTextBase64, String nonceBase64) {
            this.cipherTextBase64 = cipherTextBase64;
            this.nonceBase64 = nonceBase64;
        }

        public String getCipherTextBase64() { return cipherTextBase64; }
        public String getNonceBase64() { return nonceBase64; }
    }


    // --- Encryption Method ---

    /**
     * Encrypts the plaintext and returns separate Base64 encoded nonce and ciphertext+tag.
     * This is used because the database stores Nonce/IV and Ciphertext separately.
     * @param plaintext The data to encrypt.
     * @param keyBytes The 32-byte encryption key (AES-256).
     * @return EncryptedResult containing Base64 encoded nonce and ciphertext+tag.
     */
    public static EncryptedResult encrypt(String plaintext, byte[] keyBytes) throws Exception {
        byte[] plainBytes = plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        // 1. Generate a secure, unique Nonce (IV)
        byte[] nonce = new byte[GCM_NONCE_LENGTH];
        new SecureRandom().nextBytes(nonce);

        // 2. Setup Cipher
        Cipher cipher = Cipher.getInstance(CIPHER_MODE);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, ALGORITHM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, nonce);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

        // 3. Encrypt (Output contains Ciphertext + GCM Tag)
        byte[] cipherTextWithTag = cipher.doFinal(plainBytes);

        // 4. Return separate Base64 encoded components
        String base64Ciphertext = Base64.getEncoder().encodeToString(cipherTextWithTag);
        String base64Nonce = Base64.getEncoder().encodeToString(nonce);

        return new EncryptedResult(base64Ciphertext, base64Nonce);
    }

    // --- Decryption Method ---

    /**
     * Decrypts the Base64 encoded ciphertext using AES-256 GCM, requiring the Nonce separately.
     * This method is critical for decrypting entries saved by the new encrypt method.
     * @param base64Ciphertext The Base64 encoded Ciphertext + Tag.
     * @param base64Nonce The Base64 encoded Nonce/IV used during encryption.
     * @param keyBytes The 32-byte (256-bit) encryption key.
     * @return The original plaintext string.
     */
    public static String decrypt(String base64Ciphertext, String base64Nonce, byte[] keyBytes) throws Exception {
        // 1. Decode separate components
        byte[] cipherTextWithTag = Base64.getDecoder().decode(base64Ciphertext);
        byte[] nonce = Base64.getDecoder().decode(base64Nonce);

        // Input validation (optional, but good practice)
        if (nonce.length != GCM_NONCE_LENGTH) {
            throw new IllegalArgumentException("Invalid Nonce length during decryption.");
        }

        // 2. Setup Cipher
        Cipher cipher = Cipher.getInstance(CIPHER_MODE);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, ALGORITHM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, nonce);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

        // 3. Decrypt
        byte[] decryptedBytes = cipher.doFinal(cipherTextWithTag);

        return new String(decryptedBytes, java.nio.charset.StandardCharsets.UTF_8);
    }
}