package com.passwordmanager.model;

public class PasswordEntry {
    private int id;
    private int userId;
    private String title;

    // Encrypted fields (Ciphertext + Nonce)
    private String usernameEnc; // Encrypted service username/email
    private String passwordEnc; // Encrypted actual password
    private String noteEnc;     // Encrypted note

    // Nonce fields (separate storage in your schema)
    private String entryNonce;  // Maps to 'nonce' (for password/service username)
    private String noteNonce;   // Maps to 'note_nonce' (for note)

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getUsernameEnc() { return usernameEnc; }
    public void setUsernameEnc(String usernameEnc) { this.usernameEnc = usernameEnc; }
    public String getPasswordEnc() { return passwordEnc; }
    public void setPasswordEnc(String passwordEnc) { this.passwordEnc = passwordEnc; }
    public String getNoteEnc() { return noteEnc; }
    public void setNoteEnc(String noteEnc) { this.noteEnc = noteEnc; }
    public String getEntryNonce() { return entryNonce; }
    public void setEntryNonce(String entryNonce) { this.entryNonce = entryNonce; }
    public String getNoteNonce() { return noteNonce; }
    public void setNoteNonce(String noteNonce) { this.noteNonce = noteNonce; }
}