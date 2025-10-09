package com.passwordmanager.model;

/**
 * Helper to pass structured encryption results (ciphertext and nonce)
 * for separate storage in the database.
 */
public class EncryptedResult {
    private final String cipherTextBase64;
    private final String nonceBase64;

    public EncryptedResult(String cipherTextBase64, String nonceBase64) {
        this.cipherTextBase64 = cipherTextBase64;
        this.nonceBase64 = nonceBase64;
    }

    public String getCipherTextBase64() { return cipherTextBase64; }
    public String getNonceBase64() { return nonceBase64; }
}