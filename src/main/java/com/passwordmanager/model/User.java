package com.passwordmanager.model;

public class User {
    private int id;
    private String username;
    private String masterHash;
    private String masterSalt;
    private String totpSecretEnc; // Encrypted Base32 Secret
    // NOTE: Your schema stores the TOTP Nonce combined with the secret,
    // or relies on the secret being stored after decryption.
    // For now, we use one field: totpSecretEnc, which will store the combined result (Nonce|Ciphertext).

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getMasterHash() { return masterHash; }
    public void setMasterHash(String masterHash) { this.masterHash = masterHash; }
    public String getMasterSalt() { return masterSalt; }
    public void setMasterSalt(String masterSalt) { this.masterSalt = masterSalt; }
    public String getTotpSecretEnc() { return totpSecretEnc; }
    public void setTotpSecretEnc(String totpSecretEnc) { this.totpSecretEnc = totpSecretEnc; }
}