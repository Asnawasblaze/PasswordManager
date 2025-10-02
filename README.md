Secure Password Manager (Java Swing)

Overview
- A desktop password manager built with Java Swing.
- Uses AES-256-GCM to encrypt all sensitive data with a master key derived from the user’s master password via PBKDF2 (600k iterations).
- Enforces 2‑Factor Authentication (2FA) at login using Time-based One-Time Passwords (TOTP).
- Stores data in a PostgreSQL database (designed for Supabase, but any PostgreSQL works).

Key Features
- Master Password + PBKDF2 (PBKDF2WithHmacSHA256, 600,000 iterations).
- AES‑256‑GCM encryption (nonce + ciphertext + tag are stored as one Base64 string).
- TOTP 2FA via googleauth library; QR codes generated via ZXing.
- Modern FlatLaf UI; login/registration dialogs; main vault with list and add/delete entries; generator and strength meter.

Architecture At a Glance
- UI (package com.passwordmanager.ui)
  - LoginDialog: Username + Master Password; on success derives session master key and opens 2FA.
  - RegistrationDialog: Creates user and shows TOTP setup (QR and secret key).
  - TotpSetupDialog: Renders QR (ZXing) and shows manual key as fallback.
  - TotpVerificationDialog: Verifies TOTP, then opens MainFrame.
  - AddPasswordDialog: Creates encrypted entries via PasswordService; shows strength meter and generator.
  - PasswordGeneratorDialog: Generates strong passwords with adjustable settings.
  - MainFrame: Main window showing password list and actions (decrypt, copy, add, delete). Includes clipboard auto‑clear and inactivity auto‑lock timers.
- Services (package com.passwordmanager.service)
  - AuthService: Registration (PBKDF2 + TOTP secret generation + AES‑GCM encrypt secret) and login (verify master password + TOTP).
  - PasswordService: Encrypts/decrypts entry fields and calls DAO CRUD methods.
- DAO (package com.passwordmanager.dao)
  - DatabaseManager: Singleton for JDBC connection to Supabase PostgreSQL (pooler). Contains current hardcoded credentials.
  - UserDAO: Creates and fetches users (stores master hash, salt, and encrypted TOTP secret).
  - PasswordDAO: CRUD for password entries. Expects encrypted fields; see schema note below.
- Utilities (package com.passwordmanager.util)
  - Pbkdf2HashUtil: Salt generation, hashing, verify, Base64 convenience.
  - AesGcmEncryptionUtil: AES‑GCM encrypt/decrypt; returns Base64(12‑byte nonce + ciphertext + tag).
  - TotpUtil: Generate new TOTP secret and otpauth:// URI; verify codes.
  - QrCodeUtil: Render QR code images for the otpauth URI.
  - PasswordGenerator: Generate random passwords from selected classes.
  - PasswordStrengthChecker: Estimate password strength from entropy.
- Models (package com.passwordmanager.model)
  - User: id, username, masterHash, masterSalt, totpSecretEnc (encrypted TOTP secret, Base64 of nonce|ct|tag).
  - PasswordEntry: id, userId, title, usernameEnc, passwordEnc, noteEnc, entryNonce, noteNonce.
- Entry point
  - com.passwordmanager.AppLauncher: Sets FlatDarkLaf and shows LoginDialog.

Data Flow
1) Registration
   - User enters username and master password.
   - PBKDF2 derives 32‑byte master key; hash and salt are stored (Base64 strings).
   - A new TOTP secret is generated (googleauth). The raw secret is AES‑GCM encrypted with the master key and stored as Base64(nonce|ct|tag).
   - UI shows QR and manual key to set up authenticator app.

2) Login
   - AuthenticateMasterPassword: PBKDF2 re‑derives hash from entered password + stored salt; compare to stored hash.
   - Derive session master key once and pass it through to subsequent dialogs/windows.
   - TOTP verification decrypts the stored TOTP secret using the session key and verifies the entered 6‑digit code.

3) Managing Passwords
   - Add entry: PasswordService encrypts fields with session master key (AES‑GCM) and stores them via PasswordDAO.
   - List entries: PasswordDAO loads encrypted values for the logged‑in user.
   - Decrypt/copy: AesGcmEncryptionUtil decrypts Base64(nonce|ct|tag) using the session key. MainFrame supports copying with auto‑clear timer and inactivity auto‑lock.

Build and Run
Prerequisites
- JDK 24 (pom is configured with source/target 24). Install a matching JDK and ensure mvn uses it.
- Maven 3.9+
- PostgreSQL database (Supabase recommended) reachable from the machine.

Steps
1) Configure database credentials
   - Edit src/main/java/com/passwordmanager/dao/DatabaseManager.java and set DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD to your environment.
   - Note: Credentials are currently hardcoded for simplicity. See project.txt for a future plan to load from a secure config file.
2) Create schema
   - Open PasswordManagerDatabaseSchema.sql in your PostgreSQL environment (e.g., Supabase SQL editor) and run it.
3) Build
   - mvn clean package
   - This produces target/PasswordManager-1.0.jar (a shaded fat JAR) and target/original-PasswordManager-1.0.jar.
4) Run
   - java -jar target/PasswordManager-1.0.jar

Database Schema Note (Important)
- The included SQL schema defines columns users(username, master_password_hash, salt, totp_secret) and passwords(title, username, encrypted_password, url, notes, ...).
- The current PasswordDAO and PasswordEntry expect encrypted_note, note_nonce, and nonce columns for finer‑grained storage, and PasswordService contains placeholders for nonces.
- In practice, AesGcmEncryptionUtil already stores nonce|ct|tag in a single Base64 string. You have two options:
  1) Simplify: Store the combined Base64 in encrypted_password (and similarly for the other sensitive fields), remove separate nonce columns, and adjust DAO/model to match the schema fully.
  2) Split: Refactor AesGcmEncryptionUtil to return nonce and ciphertext separately and store them in distinct columns (requires altering the schema to include nonce fields).
- Until this is unified, ensure your database columns match what the DAO queries, or update the DAO/schema together.

Security Considerations
- Master key is never stored; only PBKDF2 hash and salt are stored.
- AES‑GCM provides integrity via authentication tag.
- Clipboard handling: MainFrame includes an auto‑clear timer after copying a password. Inactivity auto‑lock is also present.
- Consider introducing a secure local configuration for DB credentials and enabling PostgreSQL RLS (see schema comments).

Typical User Flows
- Register → show TOTP QR/secret → return to login → login with master password → enter TOTP → main vault.
- Add entry → title, username/email, password (or generate), optional note → save → entry appears in main table.
- Select an entry → decrypt/copy password to clipboard → clipboard auto‑clears after a short period.

Troubleshooting
- Database connection errors: Use DatabaseManager.main() to run a quick connectivity test; verify DB_URL/USER/PASSWORD and that SSL is enabled if required.
- ClassNotFoundException for PostgreSQL driver: Ensure Maven build completed and you’re running the shaded JAR.
- TOTP code invalid: Check device time sync; re‑scan QR; ensure you’re entering a current 6‑digit code.

Known Gaps / TODOs
- Unify database schema with DAO/Model code for password entry fields (nonce vs combined storage; notes vs encrypted_note).
- Move DB credentials from source to a secure local config file (see project.txt Phase 1.1).
- Strengthen validation in PasswordService (title required, optional minimum generated strength) and improve error messages.
- Add unit/integration tests for crypto, DAO, and services.

Project Coordinates
- Group: com.passwordmanager
- Artifact: PasswordManager
- Main class: com.passwordmanager.AppLauncher

License
- Add your license here if applicable.
