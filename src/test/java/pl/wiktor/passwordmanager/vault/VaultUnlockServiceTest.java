package pl.wiktor.passwordmanager.vault;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import pl.wiktor.passwordmanager.error.CryptoException;
import pl.wiktor.passwordmanager.error.VaultValidationException;
import pl.wiktor.passwordmanager.io.ObjectMapperFactory;
import pl.wiktor.passwordmanager.model.CipherParams;
import pl.wiktor.passwordmanager.model.KdfParams;
import pl.wiktor.passwordmanager.model.VaultEnvelope;
import pl.wiktor.passwordmanager.model.VaultPayload;

class VaultUnlockServiceTest {
    private final ObjectMapper mapper = ObjectMapperFactory.create();

    @Test
    void shouldUnlockVaultWithCorrectPassword() throws Exception {
        VaultEnvelope envelope = createEnvelope();
        char[] masterPassword = "test-master-password".toCharArray();

        VaultPayload payload = new VaultUnlockService().unlock(envelope, masterPassword, mapper);

        assertTrue(payload.entries().isEmpty());
        assertArrayEquals(new char[masterPassword.length], masterPassword);
    }

    @Test
    void shouldRejectWrongPassword() {
        VaultEnvelope envelope = createEnvelope();
        char[] wrongPassword = "wrong-password".toCharArray();

        assertThrows(CryptoException.class,
                () -> new VaultUnlockService().unlock(envelope, wrongPassword, mapper));
        assertArrayEquals(new char[wrongPassword.length], wrongPassword);
    }

    @Test
    void shouldRejectTamperedPayload() {
        VaultEnvelope envelope = createEnvelope();
        VaultEnvelope tamperedEnvelope = withPayload(envelope, tamperBase64(envelope.payload()));
        char[] masterPassword = "test-master-password".toCharArray();

        assertThrows(CryptoException.class,
                () -> new VaultUnlockService().unlock(tamperedEnvelope, masterPassword, mapper));
        assertArrayEquals(new char[masterPassword.length], masterPassword);
    }

    @Test
    void shouldRejectTamperedAadProtectedHeader() {
        VaultEnvelope envelope = createEnvelope();
        VaultEnvelope tamperedEnvelope = new VaultEnvelope(
                envelope.format(),
                2,
                envelope.createdAt(),
                envelope.updatedAt(),
                envelope.kdf(),
                envelope.cipher(),
                envelope.payload());
        char[] masterPassword = "test-master-password".toCharArray();

        assertThrows(VaultValidationException.class,
                () -> new VaultUnlockService().unlock(tamperedEnvelope, masterPassword, mapper));
        assertArrayEquals(new char[masterPassword.length], masterPassword);
    }

    @Test
    void shouldRejectInvalidBase64SaltBeforeDerivingKey() {
        VaultEnvelope envelope = createEnvelope();
        KdfParams invalidKdf = new KdfParams(
                envelope.kdf().name(),
                envelope.kdf().memoryKiB(),
                envelope.kdf().iterations(),
                envelope.kdf().parallelism(),
                "not-base64!");
        VaultEnvelope invalidEnvelope = withKdf(envelope, invalidKdf);
        char[] masterPassword = "test-master-password".toCharArray();

        assertThrows(VaultValidationException.class,
                () -> new VaultUnlockService().unlock(invalidEnvelope, masterPassword, mapper));
        assertArrayEquals(new char[masterPassword.length], masterPassword);
    }

    @Test
    void shouldRejectKdfMemoryOutsideSupportedLimits() {
        VaultEnvelope envelope = createEnvelope();
        KdfParams invalidKdf = new KdfParams(
                envelope.kdf().name(),
                999_999_999,
                envelope.kdf().iterations(),
                envelope.kdf().parallelism(),
                envelope.kdf().salt());
        VaultEnvelope invalidEnvelope = withKdf(envelope, invalidKdf);
        char[] masterPassword = "test-master-password".toCharArray();

        assertThrows(VaultValidationException.class,
                () -> new VaultUnlockService().unlock(invalidEnvelope, masterPassword, mapper));
        assertArrayEquals(new char[masterPassword.length], masterPassword);
    }

    @Test
    void shouldRejectInvalidNonceLength() {
        VaultEnvelope envelope = createEnvelope();
        CipherParams invalidCipher = new CipherParams(
                envelope.cipher().name(),
                Base64.getEncoder().encodeToString(new byte[3]),
                envelope.cipher().tagLengthBits());
        VaultEnvelope invalidEnvelope = withCipher(envelope, invalidCipher);
        char[] masterPassword = "test-master-password".toCharArray();

        assertThrows(VaultValidationException.class,
                () -> new VaultUnlockService().unlock(invalidEnvelope, masterPassword, mapper));
        assertArrayEquals(new char[masterPassword.length], masterPassword);
    }

    private VaultEnvelope createEnvelope() {
        return new VaultInitializationService().init("test-master-password".toCharArray(), mapper);
    }

    private VaultEnvelope withPayload(VaultEnvelope envelope, String payload) {
        return new VaultEnvelope(
                envelope.format(),
                envelope.version(),
                envelope.createdAt(),
                envelope.updatedAt(),
                envelope.kdf(),
                envelope.cipher(),
                payload);
    }

    private VaultEnvelope withKdf(VaultEnvelope envelope, KdfParams kdf) {
        return new VaultEnvelope(
                envelope.format(),
                envelope.version(),
                envelope.createdAt(),
                envelope.updatedAt(),
                kdf,
                envelope.cipher(),
                envelope.payload());
    }

    private VaultEnvelope withCipher(VaultEnvelope envelope, CipherParams cipher) {
        return new VaultEnvelope(
                envelope.format(),
                envelope.version(),
                envelope.createdAt(),
                envelope.updatedAt(),
                envelope.kdf(),
                cipher,
                envelope.payload());
    }

    private String tamperBase64(String value) {
        byte[] bytes = Base64.getDecoder().decode(value);
        bytes[0] ^= 1;
        return Base64.getEncoder().encodeToString(bytes);
    }
}
