package com.passwordmanager.service;

import com.passwordmanager.dao.UserDAO;
import com.passwordmanager.model.User;
import com.passwordmanager.util.AesGcmEncryptionUtil;
import com.passwordmanager.util.Pbkdf2HashUtil;
import com.passwordmanager.util.TotpUtil;
import com.passwordmanager.util.TotpUtil.TotpSetupInfo;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);
    private final UserDAO userDAO = new UserDAO();

    // NOTE: Master Key size for AES-256
    private static final int AES_KEY_SIZE_BYTES = 32;

    // --- REGISTRATION ---

    /**
     * Registers a new user with a hashed master password and initializes the TOTP secret.
     * @param username The new username.
     * @param masterPassword The plaintext master password.
     * @return TOTPSetupInfo object containing the secret and QR URI, or null on failure.
     */
    public TotpSetupInfo registerUser(String username, String masterPassword) {
        try {
            // 1. Generate Salt and Hash Master Password (PBKDF2)
            byte[] salt = Pbkdf2HashUtil.generateSalt();
            byte[] masterKeyBytes = Pbkdf2HashUtil.hashPassword(masterPassword, salt); // This is the 32-byte key

            // 2. Generate TOTP Secret
            TotpSetupInfo totpInfo = TotpUtil.generateNewSecret(username, "PasswordManager");

            // 3. Encrypt the raw TOTP secret using the Master Key (AES-GCM)
            // Note: Since the masterKeyBytes is 32 bytes (256 bits), it can be used directly.
            String encryptedTotpSecret = AesGcmEncryptionUtil.encrypt(
                    totpInfo.getSecret(), masterKeyBytes
            );

            // 4. Create User Model
            User user = new User();
            user.setUsername(username);
            user.setMasterHash(Pbkdf2HashUtil.toBase64(masterKeyBytes)); // Hash is stored
            user.setMasterSalt(Pbkdf2HashUtil.toBase64(salt));
            user.setTotpSecretEnc(encryptedTotpSecret); // Encrypted secret stored

            // 5. Save to Database
            if (userDAO.findUserByUsername(username).isPresent()) {
                LOGGER.warn("Registration failed: Username already exists.");
                return null;
            }
            // First try to store encrypted TOTP secret (preferred)
            if (userDAO.createUser(user, encryptedTotpSecret) > 0) {
                return totpInfo; // Return info so UI can display QR code
            }
            // Fallback: some existing databases use totp_secret VARCHAR(64), which may be too short for encrypted data.
            // Retry by storing the raw base32 secret (fits in 64 chars). This preserves functionality for now.
            LOGGER.warn("Encrypted TOTP secret didn't fit DB column; falling back to storing plain TOTP secret.");
            if (userDAO.createUser(user, totpInfo.getSecret()) > 0) {
                return totpInfo;
            }

        } catch (Exception e) {
            LOGGER.error("Registration failed for user {}:", username, e);
        }
        return null;
    }

    // --- LOGIN ---

    /**
     * Primary login method: authenticates master password and returns the User object
     * if the master password is correct.
     * @param username The username entered.
     * @param masterPassword The master password entered.
     * @return An Optional containing the User object if password is correct, otherwise empty.
     */
    public Optional<User> authenticateMasterPassword(String username, String masterPassword) {
        Optional<User> userOpt = userDAO.findUserByUsername(username);

        if (userOpt.isEmpty()) {
            return Optional.empty(); // User not found
        }

        User user = userOpt.get();

        if (Pbkdf2HashUtil.verifyPassword(masterPassword, user.getMasterHash(), user.getMasterSalt())) {
            return Optional.of(user); // Password match
        } else {
            return Optional.empty(); // Password mismatch
        }
    }

    /**
     * Authenticates the TOTP code as the final step of login.
     * @param user The authenticated user object.
     * @param totpCode The 6-digit code entered by the user.
     * @param masterPassword The master password (needed to decrypt the TOTP secret).
     * @return true if the TOTP code is valid, false otherwise.
     */
    public boolean authenticateTotp(User user, int totpCode, String masterPassword) {
        try {
            // 1. Re-derive the Master Key from the password and stored salt
            byte[] salt = Pbkdf2HashUtil.fromBase64(user.getMasterSalt());
            byte[] masterKeyBytes = Pbkdf2HashUtil.hashPassword(masterPassword, salt);

            // 2. Decrypt the TOTP secret using the Master Key
            String storedValue = user.getTotpSecretEnc();
            String secretForVerify;
            try {
                // Preferred path: stored value is encrypted (Base64 Nonce|CT|Tag)
                secretForVerify = AesGcmEncryptionUtil.decrypt(storedValue, masterKeyBytes);
            } catch (Exception decryptErr) {
                // Backward compatibility: some databases may store the plain Base32 secret (VARCHAR(64))
                LOGGER.warn("TOTP secret appears to be stored in plain text for user {}. Using as-is.", user.getUsername());
                secretForVerify = storedValue; // treat as plain Base32 secret
            }

            // 3. Verify the 6-digit code
            return TotpUtil.verifyCode(secretForVerify, totpCode);

        } catch (Exception e) {
            LOGGER.error("TOTP authentication failed for user {}:", user.getUsername(), e);
            return false;
        }
    }

    /**
     * Overload to authenticate TOTP using a pre-derived master key.
     * This avoids re-deriving the key and aligns with "derive once per session".
     */
    public boolean authenticateTotp(User user, int totpCode, byte[] masterKeyBytes) {
        try {
            String storedValue = user.getTotpSecretEnc();
            String secretForVerify;
            try {
                secretForVerify = AesGcmEncryptionUtil.decrypt(storedValue, masterKeyBytes);
            } catch (Exception decryptErr) {
                LOGGER.warn("TOTP secret appears to be stored in plain text for user {}. Using as-is.", user.getUsername());
                secretForVerify = storedValue;
            }
            return TotpUtil.verifyCode(secretForVerify, totpCode);
        } catch (Exception e) {
            LOGGER.error("TOTP authentication (with key) failed for user {}:", user.getUsername(), e);
            return false;
        }
    }
}