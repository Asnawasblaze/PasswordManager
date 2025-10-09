package com.passwordmanager.util;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PasswordGenerator {

    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String SYMBOLS = "!@#$%^&*()-_+=<>?";

    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generatePassword(int length, boolean useUpper, boolean useLower, boolean useDigits, boolean useSymbols) {

        StringBuilder charPool = new StringBuilder();
        if (useUpper) charPool.append(UPPERCASE);
        if (useLower) charPool.append(LOWERCASE);
        if (useDigits) charPool.append(DIGITS);
        if (useSymbols) charPool.append(SYMBOLS);

        if (charPool.length() == 0) {
            throw new IllegalArgumentException("At least one character set must be selected.");
        }

        // 1. Ensure at least one character from each selected set is included
        StringBuilder password = new StringBuilder();
        if (useUpper) password.append(getRandomChar(UPPERCASE));
        if (useLower) password.append(getRandomChar(LOWERCASE));
        if (useDigits) password.append(getRandomChar(DIGITS));
        if (useSymbols) password.append(getRandomChar(SYMBOLS));

        // 2. Fill the remaining length with random characters from the full pool
        int remainingLength = length - password.length();
        for (int i = 0; i < remainingLength; i++) {
            password.append(getRandomChar(charPool.toString()));
        }

        // 3. Shuffle the characters to randomize placement
        List<Character> charList = password.chars()
                .mapToObj(e -> (char)e)
                .collect(Collectors.toList());
        Collections.shuffle(charList, RANDOM);

        // 4. Convert back to string and return
        return charList.stream().map(String::valueOf).collect(Collectors.joining());
    }

    private static char getRandomChar(String pool) {
        return pool.charAt(RANDOM.nextInt(pool.length()));
    }
}