package com.passwordmanager.util;

public class PasswordStrengthChecker {

    // Entropy thresholds (adjust these based on desired security level)
    private static final double WEAK_THRESHOLD = 40; // Below this is Weak
    private static final double MODERATE_THRESHOLD = 60; // Between 40 and 60 is Moderate

    public static String checkStrength(String password) {
        if (password == null || password.isEmpty()) {
            return "Weak";
        }

        // 1. Calculate Keyspace Size
        int keyspaceSize = 0;

        // Lowercase letters (26)
        if (password.matches(".*[a-z].*")) keyspaceSize += 26;

        // Uppercase letters (26)
        if (password.matches(".*[A-Z].*")) keyspaceSize += 26;

        // Digits (10)
        if (password.matches(".*[0-9].*")) keyspaceSize += 10;

        // Symbols (~32 common symbols)
        // A simple check for anything that isn't a letter or digit
        if (password.matches(".*[^a-zA-Z0-9].*")) keyspaceSize += 32;

        if (keyspaceSize == 0) {
            // Should not happen for a non-empty string, but as a safeguard
            return "Weak";
        }

        // 2. Calculate Entropy (H = L * log2(R))
        // L = length, R = keyspace size
        double length = password.length();
        double entropy = length * (Math.log(keyspaceSize) / Math.log(2));

        // 3. Classify based on Entropy
        if (entropy < WEAK_THRESHOLD) {
            return "Weak";
        } else if (entropy < MODERATE_THRESHOLD) {
            return "Moderate";
        } else {
            return "Strong";
        }
    }
}