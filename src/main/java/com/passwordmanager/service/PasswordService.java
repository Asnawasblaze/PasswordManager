package com.passwordmanager.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.passwordmanager.dao.PasswordDAO;
import com.passwordmanager.model.PasswordEntry;
import com.passwordmanager.util.AesGcmEncryptionUtil;
import com.passwordmanager.util.AesGcmEncryptionUtil.EncryptedResult;

public class PasswordService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordService.class);
    private final PasswordDAO passwordDAO = new PasswordDAO();

    // --- CRUD OPERATIONS ---

    /**
     * Encrypts a new password entry and saves it to the database.
     * @param userId The ID of the owning user.
     * @param title The entry title.
     * @param serviceUsername The plaintext service username/email.
     * @param plaintextPassword The plaintext password.
     * @param note The plaintext note.
     * @param masterKeyBytes The 32-byte master key derived from the master password.
     * @return true if the entry was successfully created.
     */
    public boolean createEntry(int userId, String title, String serviceUsername, String plaintextPassword, String note, byte[] masterKeyBytes) {
        try {
            // FIX: Use the imported EncryptedResult and the correct 'encrypt' method name
            EncryptedResult usernameEncResult = AesGcmEncryptionUtil.encrypt(serviceUsername, masterKeyBytes);
            EncryptedResult passwordEncResult = AesGcmEncryptionUtil.encrypt(plaintextPassword, masterKeyBytes);
            EncryptedResult noteEncResult = AesGcmEncryptionUtil.encrypt(note, masterKeyBytes);

            PasswordEntry entry = new PasswordEntry();
            entry.setUserId(userId);
            entry.setTitle(title);

            // Set encrypted ciphertext
            entry.setUsernameEnc(usernameEncResult.getCipherTextBase64());
            entry.setPasswordEnc(passwordEncResult.getCipherTextBase64());
            entry.setNoteEnc(noteEncResult.getCipherTextBase64());

            // Set nonce separately
            entry.setEntryNonce(passwordEncResult.getNonceBase64()); // Use password nonce for entryNonce
            entry.setNoteNonce(noteEncResult.getNonceBase64());

            return passwordDAO.createEntry(entry) > 0;

        } catch (Exception e) {
            LOGGER.error("Failed to create password entry:", e);
            return false;
        }
    }

    /**
     * Retrieves all password entries (encrypted) for display in the UI list.
     * @param userId The ID of the user.
     * @return List of encrypted PasswordEntry objects.
     */
    public List<PasswordEntry> getEncryptedEntries(int userId) {
        return passwordDAO.findAllByUserId(userId);
    }

    /**
     * Deletes a password entry.
     */
    public boolean deleteEntry(int entryId, int userId) {
        return passwordDAO.deleteEntry(entryId, userId);
    }

    // --- DECRYPTION ---

    /**
     * Decrypts the password for a single entry.
     * @param encryptedEntry The entry model containing ciphertext and nonce.
     * @param masterKeyBytes The master key.
     * @return The plaintext password.
     */
    public String decryptPassword(PasswordEntry encryptedEntry, byte[] masterKeyBytes) {
        try {
            // NOTE: The utility needs the ciphertext and the separate nonce!
            return AesGcmEncryptionUtil.decrypt(
                    encryptedEntry.getPasswordEnc(),  // Ciphertext
                    encryptedEntry.getEntryNonce(), // Nonce
                    masterKeyBytes
            );
        } catch (Exception e) {
            LOGGER.error("Failed to decrypt password for entry ID {}:", encryptedEntry.getId(), e);
            return "[DECRYPTION FAILED]";
        }
    }
}