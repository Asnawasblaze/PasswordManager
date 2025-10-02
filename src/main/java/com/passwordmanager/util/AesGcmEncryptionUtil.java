package com.passwordmanager.util;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Arrays;

public class AesGcmEncryptionUtil {
    // 12 bytes for IV/Nonce is standard for GCM
    private static final int GCM_NONCE_LENGTH = 12;
    // 16 bytes for the GCM Authentication Tag
    private static final int GCM_TAG_LENGTH = 16;
    // 32 bytes for AES-256 Key (not strictly needed here, but kept for context)
    // private static final int AES_KEY_LENGTH = 32;
    private static final String ALGORITHM = "AES";
    private static final String CIPHER_MODE = "AES/GCM/NoPadding";

    /**
     * Encrypts the plaintext using AES-256 GCM.
     * @param plaintext The data to encrypt.
     * @param keyBytes The 32-byte encryption key.
     * @return Base64 encoded string: Nonce | Ciphertext | GCM Tag
     */
    public static String encrypt(String plaintext, byte[] keyBytes) throws Exception {
        byte[] plainBytes = plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        // 1. Generate a secure, unique Nonce (IV)
        byte[] nonce = new byte[GCM_NONCE_LENGTH];
        new SecureRandom().nextBytes(nonce);

        // 2. Setup Cipher
        Cipher cipher = Cipher.getInstance(CIPHER_MODE);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, ALGORITHM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, nonce); // Tag length in bits
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

        // 3. Encrypt (Output includes GCM Tag implicitly)
        byte[] cipherText = cipher.doFinal(plainBytes);

        // 4. Combine Nonce and Ciphertext + Tag
        byte[] combined = new byte[GCM_NONCE_LENGTH + cipherText.length];
        System.arraycopy(nonce, 0, combined, 0, GCM_NONCE_LENGTH);
        System.arraycopy(cipherText, 0, combined, GCM_NONCE_LENGTH, cipherText.length);

        // 5. Return Base64 encoded string for storage
        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * Decrypts the combined Base64 ciphertext (Nonce | Ciphertext | GCM Tag).
     * @param base64Ciphertext The combined Base64 string.
     * @param keyBytes The 32-byte encryption key.
     * @return The original plaintext string.
     */
    public static String decrypt(String base64Ciphertext, byte[] keyBytes) throws Exception {
        byte[] combined = Base64.getDecoder().decode(base64Ciphertext);

        // 1. Separate Nonce from Ciphertext + Tag
        if (combined.length < GCM_NONCE_LENGTH + GCM_TAG_LENGTH) {
            throw new IllegalArgumentException("Invalid combined ciphertext length.");
        }

        byte[] nonce = Arrays.copyOfRange(combined, 0, GCM_NONCE_LENGTH);
        byte[] cipherTextWithTag = Arrays.copyOfRange(combined, GCM_NONCE_LENGTH, combined.length);

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