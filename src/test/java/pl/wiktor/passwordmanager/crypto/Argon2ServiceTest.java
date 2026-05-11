package pl.wiktor.passwordmanager.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import pl.wiktor.passwordmanager.model.KdfParams;

class Argon2ServiceTest {
    private final Argon2Service argon2Service = new Argon2Service();

    @Test
    void shouldGenerateThirtyTwoByteKey() {
        char[] password = "test-master-password".toCharArray();
        byte[] salt = new byte[] {
                1, 2, 3, 4,
                5, 6, 7, 8,
                9, 10, 11, 12,
                13, 14, 15, 16
        };
        KdfParams params = new KdfParams("argon2id", 1024, 1, 1, "unused-in-this-test");

        byte[] key = argon2Service.generateKey(password, salt, params);

        assertEquals(32, key.length);
    }

    @Test
    void shouldGenerateSameKey() {
        char[] password = "test-master-password".toCharArray();
        byte[] salt = new byte[] {
                1, 2, 3, 4,
                5, 6, 7, 8,
                9, 10, 11, 12,
                13, 14, 15, 16
        };
        KdfParams params = new KdfParams("argon2id", 1024, 1, 1, "unused-in-this-test");

        byte[] key1 = argon2Service.generateKey(password, salt, params);
        byte[] key2 = argon2Service.generateKey(password, salt, params);
        assertArrayEquals(key1, key2);
    }

    @Test
    void shouldGenerateDifferentKeyOnDifferentSaltUsingSamePassword() {
        char[] password = "test-master-password".toCharArray();
        byte[] salt1 = new byte[] {
                1, 2, 3, 4,
                5, 6, 7, 8,
                9, 10, 11, 12,
                13, 14, 15, 16
        };
        byte[] salt2 = new byte[] {
                16, 15, 14, 13,
                12, 11, 10, 9,
                8, 7, 6, 5,
                4, 3, 2, 1
        };

        KdfParams params = new KdfParams("argon2id", 1024, 1, 1, "unused-in-this-test");

        byte[] key1 = argon2Service.generateKey(password, salt1, params);
        byte[] key2 = argon2Service.generateKey(password, salt2, params);
        assertFalse(Arrays.equals(key1,key2));
    }

    @Test
    void shouldGenerateDifferentKeyOnDifferentPassword() {
        char[] password1 = "test-master-password".toCharArray();
        char[] password2 = "test-password-master".toCharArray();
        byte[] salt = new byte[] {
                1, 2, 3, 4,
                5, 6, 7, 8,
                9, 10, 11, 12,
                13, 14, 15, 16
        };
        KdfParams params = new KdfParams("argon2id", 1024, 1, 1, "unused-in-this-test");

        byte[] key1 = argon2Service.generateKey(password1, salt, params);
        byte[] key2 = argon2Service.generateKey(password2, salt, params);
        assertFalse(Arrays.equals(key1,key2));
    }
}
