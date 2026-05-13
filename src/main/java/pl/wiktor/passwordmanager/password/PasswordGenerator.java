package pl.wiktor.passwordmanager.password;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PasswordGenerator {
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String SYMBOLS = "!@#$%^&*()-_=+[]{}|;:,.<>?";

    private final SecureRandom random;

    public PasswordGenerator() {
        this(new SecureRandom());
    }

    PasswordGenerator(SecureRandom random) {
        this.random = random;
    }

    public String generate(int length, boolean useUpper, boolean useDigits, boolean useSymbols) {
        if (length < 8) {
            throw new IllegalArgumentException("Password length must be at least 8");
        }

        List<String> activeCategories = new ArrayList<>();
        activeCategories.add(LOWER);
        if (useUpper) activeCategories.add(UPPER);
        if (useDigits) activeCategories.add(DIGITS);
        if (useSymbols) activeCategories.add(SYMBOLS);

        int mandatoryCount = activeCategories.size() * 2;
        if (length < mandatoryCount) {
            throw new IllegalArgumentException("Length " + length + " is too short for " + activeCategories.size() + " active categories (minimum " + mandatoryCount + " required for 2 chars each)");
        }

        List<Character> passwordChars = new ArrayList<>();

        for (String category : activeCategories) {
            passwordChars.add(category.charAt(random.nextInt(category.length())));
            passwordChars.add(category.charAt(random.nextInt(category.length())));
        }

        StringBuilder allCharsBuilder = new StringBuilder();
        for (String category : activeCategories) {
            allCharsBuilder.append(category);
        }
        String allChars = allCharsBuilder.toString();

        while (passwordChars.size() < length) {
            passwordChars.add(allChars.charAt(random.nextInt(allChars.length())));
        }

        Collections.shuffle(passwordChars, random);

        StringBuilder result = new StringBuilder();
        for (char c : passwordChars) {
            result.append(c);
        }
        return result.toString();
    }
}
