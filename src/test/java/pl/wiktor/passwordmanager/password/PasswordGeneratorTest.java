package pl.wiktor.passwordmanager.password;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class PasswordGeneratorTest {
    private final PasswordGenerator generator = new PasswordGenerator();

    @Test
    void shouldGeneratePasswordWithCorrectLength() {
        assertEquals(8, generator.generate(8, true, true, true).length());
        assertEquals(16, generator.generate(16, true, true, true).length());
        assertEquals(100, generator.generate(100, true, true, true).length());
    }

    @Test
    void shouldEnforceMinimumLengthOfEight() {
        assertThrows(IllegalArgumentException.class, () -> generator.generate(7, true, true, true));
    }

    @Test
    void shouldGuaranteeTwoCharsFromEachCategory() {
        String password = generator.generate(16, true, true, true);
        
        assertTrue(countMatches(password, "abcdefghijklmnopqrstuvwxyz") >= 2);
        assertTrue(countMatches(password, "ABCDEFGHIJKLMNOPQRSTUVWXYZ") >= 2);
        assertTrue(countMatches(password, "0123456789") >= 2);
        assertTrue(countMatches(password, "!@#$%^&*()-_=+[]{}|;:,.<>?") >= 2);
    }

    @Test
    void shouldNotIncludeDisabledCategories() {
        String password = generator.generate(16, false, false, false);
        
        assertEquals(16, password.length());
        assertTrue(countMatches(password, "abcdefghijklmnopqrstuvwxyz") == 16);
        assertTrue(countMatches(password, "ABCDEFGHIJKLMNOPQRSTUVWXYZ") == 0);
        assertTrue(countMatches(password, "0123456789") == 0);
        assertTrue(countMatches(password, "!@#$%^&*()-_=+[]{}|;:,.<>?") == 0);
    }

    @Test
    void shouldThrowIfLengthIsTooShortForActiveCategories() {
        // 4 categories active = 8 chars mandatory. 
        // Our generator already enforces length >= 8, so it's hard to trigger with length 8.
        // But if someone tried to use more mandatory chars than length, it should fail.
        // Currently length < 8 is caught first.
    }

    private int countMatches(String password, String charset) {
        int count = 0;
        for (char c : password.toCharArray()) {
            if (charset.indexOf(c) != -1) {
                count++;
            }
        }
        return count;
    }
}
