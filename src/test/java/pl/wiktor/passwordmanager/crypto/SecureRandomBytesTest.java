package pl.wiktor.passwordmanager.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

class SecureRandomBytesTest {
    @Test
    void shouldGenerateArrayWithLengthInArgument() {
        SecureRandomBytes random = new SecureRandomBytes();
        int length = 16;
        assertEquals(length, random.generateBytes(length).length);
    }

    @Test
    void shouldGenerateDifferentKeyAlmostEveryTime() {
        SecureRandomBytes random = new SecureRandomBytes();
        int length = 16;
        assertFalse(Arrays.equals(random.generateBytes(length), random.generateBytes(length)));
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWithLengthSmallerOrEqualZero() {
        SecureRandomBytes random = new SecureRandomBytes();
        int length = 0;
        int length2 = -10;
        assertThrows(IllegalArgumentException.class, () -> random.generateBytes(length));
        assertThrows(IllegalArgumentException.class, () -> random.generateBytes(length2));
    }
}
