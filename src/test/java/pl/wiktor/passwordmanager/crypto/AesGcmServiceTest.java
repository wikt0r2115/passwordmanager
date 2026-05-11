package pl.wiktor.passwordmanager.crypto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import pl.wiktor.passwordmanager.error.CryptoException;

public class AesGcmServiceTest {
    @Test
    void roundtrip() throws Exception {
        AesGcmService aesGcmService = new AesGcmService();
        SecureRandomBytes secureRandomBytes = new SecureRandomBytes();
        byte[] key = secureRandomBytes.generateBytes(32);
        byte[] nonce = secureRandomBytes.generateBytes(12);
        byte[] plaintext = "secret payload".getBytes(StandardCharsets.UTF_8);
        byte[] aad = "vault-header".getBytes(StandardCharsets.UTF_8);
        byte[] result;
        result = aesGcmService.encrypt(key, nonce, plaintext, aad);
        assertArrayEquals(plaintext, aesGcmService.decrypt(key, nonce, result, aad));
    }

    @Test
    void differentAADInDecryption() throws Exception {
        AesGcmService aesGcmService = new AesGcmService();
        SecureRandomBytes secureRandomBytes = new SecureRandomBytes();
        byte[] key = secureRandomBytes.generateBytes(32);
        byte[] nonce = secureRandomBytes.generateBytes(12);
        byte[] plaintext = "secret payload".getBytes(StandardCharsets.UTF_8);
        byte[] aad = "vault-header".getBytes(StandardCharsets.UTF_8);
        byte[] aad2 = "vault-footer".getBytes(StandardCharsets.UTF_8);
        byte[] result;
        result = aesGcmService.encrypt(key, nonce, plaintext, aad);
        assertThrows(CryptoException.class, () -> aesGcmService.decrypt(key, nonce, result, aad2));
    }

    @Test
    void differentNONCEInDecryption() throws Exception {
        AesGcmService aesGcmService = new AesGcmService();
        SecureRandomBytes secureRandomBytes = new SecureRandomBytes();
        byte[] key = secureRandomBytes.generateBytes(32);
        byte[] nonce = secureRandomBytes.generateBytes(12);
        byte[] nonce2 = secureRandomBytes.generateBytes(12);
        byte[] plaintext = "secret payload".getBytes(StandardCharsets.UTF_8);
        byte[] aad = "vault-header".getBytes(StandardCharsets.UTF_8);
        byte[] result;
        result = aesGcmService.encrypt(key, nonce, plaintext, aad);
        assertThrows(CryptoException.class, () -> aesGcmService.decrypt(key, nonce2, result, aad));
    }

    @Test
    void differentCipherText() throws Exception {
        AesGcmService aesGcmService = new AesGcmService();
        SecureRandomBytes secureRandomBytes = new SecureRandomBytes();
        byte[] key = secureRandomBytes.generateBytes(32);
        byte[] nonce = secureRandomBytes.generateBytes(12);
        byte[] plaintext = "secret payload".getBytes(StandardCharsets.UTF_8);
        byte[] aad = "vault-header".getBytes(StandardCharsets.UTF_8);
        byte[] result;
        result = aesGcmService.encrypt(key, nonce, plaintext, aad);
        result[0] ^= 1;
        assertThrows(CryptoException.class, () -> aesGcmService.decrypt(key, nonce, result, aad));
    }

    @Test
    void nullKey() throws Exception {
        AesGcmService aesGcmService = new AesGcmService();
        SecureRandomBytes secureRandomBytes = new SecureRandomBytes();
        byte[] key = null;
        byte[] nonce = secureRandomBytes.generateBytes(12);
        byte[] plaintext = "secret payload".getBytes(StandardCharsets.UTF_8);
        byte[] aad = "vault-header".getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> aesGcmService.encrypt(key, nonce, plaintext, aad));
        assertThrows(IllegalArgumentException.class, () -> aesGcmService.decrypt(key, nonce, plaintext, aad));
    }

    @Test
    void wrongKeyLength() throws Exception {
        AesGcmService aesGcmService = new AesGcmService();
        SecureRandomBytes secureRandomBytes = new SecureRandomBytes();
        byte[] key = secureRandomBytes.generateBytes(33);
        byte[] nonce = secureRandomBytes.generateBytes(12);
        byte[] plaintext = "secret payload".getBytes(StandardCharsets.UTF_8);
        byte[] aad = "vault-header".getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> aesGcmService.encrypt(key, nonce, plaintext, aad));
        assertThrows(IllegalArgumentException.class, () -> aesGcmService.decrypt(key, nonce, plaintext, aad));
    }

    @Test
    void nullNONCE() throws Exception {
        AesGcmService aesGcmService = new AesGcmService();
        SecureRandomBytes secureRandomBytes = new SecureRandomBytes();
        byte[] key = secureRandomBytes.generateBytes(32);
        byte[] nonce = null;
        byte[] plaintext = "secret payload".getBytes(StandardCharsets.UTF_8);
        byte[] aad = "vault-header".getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> aesGcmService.encrypt(key, nonce, plaintext, aad));
        assertThrows(IllegalArgumentException.class, () -> aesGcmService.decrypt(key, nonce, plaintext, aad));
    }

    @Test
    void wrongNONCELength() throws Exception {
        AesGcmService aesGcmService = new AesGcmService();
        SecureRandomBytes secureRandomBytes = new SecureRandomBytes();
        byte[] key = secureRandomBytes.generateBytes(32);
        byte[] nonce = secureRandomBytes.generateBytes(14);
        byte[] plaintext = "secret payload".getBytes(StandardCharsets.UTF_8);
        byte[] aad = "vault-header".getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> aesGcmService.encrypt(key, nonce, plaintext, aad));
        assertThrows(IllegalArgumentException.class, () -> aesGcmService.decrypt(key, nonce, plaintext, aad));
    }

    @Test
    void nullPlainText() throws Exception {
        AesGcmService aesGcmService = new AesGcmService();
        SecureRandomBytes secureRandomBytes = new SecureRandomBytes();
        byte[] key = secureRandomBytes.generateBytes(32);
        byte[] nonce = secureRandomBytes.generateBytes(12);
        byte[] plaintext = null;
        byte[] aad = "vault-header".getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> aesGcmService.encrypt(key, nonce, plaintext, aad));
    }

    @Test
    void nullCipherText() throws Exception {
        AesGcmService aesGcmService = new AesGcmService();
        SecureRandomBytes secureRandomBytes = new SecureRandomBytes();
        byte[] key = secureRandomBytes.generateBytes(32);
        byte[] nonce = secureRandomBytes.generateBytes(12);
        byte[] cipherTextWithTag = null;
        byte[] aad = "vault-header".getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> aesGcmService.decrypt(key, nonce, cipherTextWithTag, aad));
    }

    @Test
    void nullAAD() throws Exception {
        AesGcmService aesGcmService = new AesGcmService();
        SecureRandomBytes secureRandomBytes = new SecureRandomBytes();
        byte[] key = secureRandomBytes.generateBytes(32);
        byte[] nonce = secureRandomBytes.generateBytes(12);
        byte[] plaintext = "secret payload".getBytes(StandardCharsets.UTF_8);
        byte[] aad = null;
        assertThrows(IllegalArgumentException.class, () -> aesGcmService.encrypt(key, nonce, plaintext, aad));
        assertThrows(IllegalArgumentException.class, () -> aesGcmService.decrypt(key, nonce, plaintext, aad));
    }
}
