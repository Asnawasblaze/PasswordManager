package com.passwordmanager.service;

import com.passwordmanager.dao.UserDAO;
import com.passwordmanager.model.User;
import com.passwordmanager.util.AesGcmEncryptionUtil;
import com.passwordmanager.util.AesGcmEncryptionUtil.EncryptedResult; // <-- Crucial Import for encryption results
import com.passwordmanager.util.Pbkdf2HashUtil;
import com.passwordmanager.util.TotpUtil;
import com.passwordmanager.util.TotpUtil.TotpSetupInfo;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);
    private final UserDAO userDAO = new UserDAO();

    // --- REGISTRATION ---

    /**
     * Registers a new user, hashes the master password, and encrypts the TOTP secret.
     */
    public TotpSetupInfo registerUser(String username, String masterPassword) {
        try {
            // 1. Generate Salt and Hash Master Password (PBKDF2)
            byte[] salt = Pbkdf2HashUtil.generateSalt();
            byte[] masterKeyBytes = Pbkdf2HashUtil.hashPassword(masterPassword, salt);

            // 2. Generate TOTP Secret
            TotpSetupInfo totpInfo = TotpUtil.generateNewSecret(username, "PasswordManager");

            // 3. Encrypt the raw TOTP secret using the Master Key (AES-GCM)
            EncryptedResult totpEncryptedResult = AesGcmEncryptionUtil.encrypt(
                    totpInfo.getSecret(), masterKeyBytes
            );
            // We only store the ciphertext part for the single totp_secret column in the DB
            String encryptedTotpSecret = totpEncryptedResult.getCipherTextBase64();

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
            if (userDAO.createUser(user) > 0) {
                return totpInfo;
            }

        } catch (Exception e) {
            LOGGER.error("Registration failed for user {}:", username, e);
        }
        return null;
    }

    // --- LOGIN ---

    /**
     * Authenticates master password.
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
     * Authenticates the TOTP code as the final step of login (derives key internally).
     */
    public boolean authenticateTotp(User user, int totpCode, String masterPassword) {
        try {
            // 1. Re-derive the Master Key from the password and stored salt
            byte[] salt = Pbkdf2HashUtil.fromBase64(user.getMasterSalt());
            byte[] masterKeyBytes = Pbkdf2HashUtil.hashPassword(masterPassword, salt);

            // 2. Decrypt the TOTP secret using the Master Key
            String encryptedSecret = user.getTotpSecretEnc();

            // FIX: Pass empty Nonce ("") for decryption of the combined TOTP secret (due to schema structure)
            String decryptedSecret = AesGcmEncryptionUtil.decrypt(
                    encryptedSecret,
                    "", // Nonce is stored implicitly/combined in this field for TOTP
                    masterKeyBytes
            );

            // 3. Verify the 6-digit code
            return TotpUtil.verifyCode(decryptedSecret, totpCode);

        } catch (Exception e) {
            LOGGER.error("TOTP authentication failed for user {}:", user.getUsername(), e);
            return false;
        }
    }

    /**
     * Overload to authenticate TOTP using a pre-derived master key (for session handling).
     */
    public boolean authenticateTotp(User user, int totpCode, byte[] masterKeyBytes) {
        try {
            String encryptedSecret = user.getTotpSecretEnc();

            // FIX: Correct the signature here as well.
            String decryptedSecret = AesGcmEncryptionUtil.decrypt(
                    encryptedSecret,
                    "", // Nonce is stored implicitly/combined in this field for TOTP
                    masterKeyBytes
            );

            return TotpUtil.verifyCode(decryptedSecret, totpCode);
        } catch (Exception e) {
            LOGGER.error("TOTP authentication (with key) failed for user {}:", user.getUsername(), e);
            return false;
        }
    }
}