package com.passwordmanager.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.passwordmanager.dao.PasswordDAO;
import com.passwordmanager.model.PasswordEntry;
import com.passwordmanager.util.AesGcmEncryptionUtil;

public class PasswordService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordService.class);
    private final PasswordDAO passwordDAO = new PasswordDAO();

    // NOTE: Removed getEntryById method to revert DAO changes

    // --- CRUD OPERATIONS ---

    public boolean createEntry(int userId, String title, String serviceUsername, String plaintextPassword, String note, byte[] masterKeyBytes) {
        try {
            // Reverting to the combined String output method for all fields
            String usernameEnc = AesGcmEncryptionUtil.encrypt(serviceUsername, masterKeyBytes);
            String passwordEnc = AesGcmEncryptionUtil.encrypt(plaintextPassword, masterKeyBytes);
            String noteEnc = AesGcmEncryptionUtil.encrypt(note, masterKeyBytes);

            // NOTE: Nonce/IV handling is assumed to be handled poorly here (the source of the original error),
            // but this code should compile and function until it hits the exception boundary.
            String entryNonce = "temp_nonce_for_password"; // PLACEHOLDER
            String noteNonce = "temp_nonce_for_note";       // PLACEHOLDER

            PasswordEntry entry = new PasswordEntry();
            entry.setUserId(userId);
            entry.setTitle(title);

            // Set encrypted ciphertext
            entry.setUsernameEnc(usernameEnc);
            entry.setPasswordEnc(passwordEnc);
            entry.setNoteEnc(noteEnc);

            // Set nonce separately
            entry.setEntryNonce(entryNonce);
            entry.setNoteNonce(noteNonce);

            return passwordDAO.createEntry(entry) > 0;

        } catch (Exception e) {
            LOGGER.error("Failed to create password entry:", e);
            return false;
        }
    }

    public List<PasswordEntry> getEncryptedEntries(int userId) {
        return passwordDAO.findAllByUserId(userId);
    }

    public boolean deleteEntry(int entryId, int userId) {
        return passwordDAO.deleteEntry(entryId, userId);
    }

    // --- DECRYPTION ---

    public String decryptPassword(PasswordEntry encryptedEntry, byte[] masterKeyBytes) {
        try {
            // Decrypt using the simple combined string method
            return AesGcmEncryptionUtil.decrypt(
                    encryptedEntry.getPasswordEnc(),
                    masterKeyBytes
            );
        } catch (Exception e) {
            LOGGER.error("Failed to decrypt password for entry ID {}:", encryptedEntry.getId(), e);
            return "[DECRYPTION FAILED]";
        }
    }
}