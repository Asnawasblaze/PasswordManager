package com.passwordmanager.util;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;

public class TotpUtil {

    private static final GoogleAuthenticator gAuth = new GoogleAuthenticator();

    /**
     * Generates a new TOTP secret key and QR Code URI.
     * @param userName The username to embed in the QR code (for display on the authenticator app).
     * @param issuer The name of the application (Password Manager).
     * @return A TotpSetupInfo object containing the secret and the URI.
     */
    public static TotpSetupInfo generateNewSecret(String userName, String issuer) {
        // Generates a random, base32-encoded secret key
        GoogleAuthenticatorKey key = gAuth.createCredentials();
        String secret = key.getKey();

        // Generates the standard TOTP URI for QR code generation
        String uri = String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s",
                issuer, userName, secret, issuer);

        return new TotpSetupInfo(secret, uri);
    }

    /**
     * Verifies the 6-digit code provided by the user against the secret.
     * @param secret The base32-encoded TOTP secret stored in the database (after decryption).
     * @param code The 6-digit code entered by the user.
     * @return true if the code is valid within the time window, false otherwise.
     */
    public static boolean verifyCode(String secret, int code) {
        // The library handles time drift (window of 3 codes) automatically
        return gAuth.authorize(secret, code);
    }

    /**
     * Simple inner class to hold setup information.
     */
    public static class TotpSetupInfo {
        private final String secret;
        private final String qrCodeUri;

        public TotpSetupInfo(String secret, String qrCodeUri) {
            this.secret = secret;
            this.qrCodeUri = qrCodeUri;
        }

        public String getSecret() {
            return secret;
        }

        public String getQrCodeUri() {
            return qrCodeUri;
        }
    }
}