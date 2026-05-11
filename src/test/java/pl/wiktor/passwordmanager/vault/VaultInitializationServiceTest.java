package pl.wiktor.passwordmanager.vault;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import pl.wiktor.passwordmanager.crypto.AesGcmService;
import pl.wiktor.passwordmanager.crypto.Argon2Service;
import pl.wiktor.passwordmanager.io.ObjectMapperFactory;
import pl.wiktor.passwordmanager.model.VaultEnvelope;
import pl.wiktor.passwordmanager.model.VaultPayload;

class VaultInitializationServiceTest {
    private final ObjectMapper mapper = ObjectMapperFactory.create();

    @Test
    void shouldCreateDecryptableEmptyVaultEnvelope() throws Exception {
        VaultInitializationService service = new VaultInitializationService();
        char[] masterPassword = "test-master-password".toCharArray();

        VaultEnvelope envelope = service.init(masterPassword, mapper);

        assertEquals("passwordmanager-vault", envelope.format());
        assertEquals(1, envelope.version());
        assertNotNull(envelope.createdAt());
        assertNotNull(envelope.updatedAt());
        assertEquals("argon2id", envelope.kdf().name());
        assertEquals("AES-256-GCM", envelope.cipher().name());
        assertFalse(envelope.kdf().salt().isBlank());
        assertFalse(envelope.cipher().nonce().isBlank());
        assertFalse(envelope.payload().isBlank());
        assertFalse(envelope.payload().contains("entries"));
        assertFalse(envelope.payload().contains("placeholder"));
        assertArrayEquals(new char[masterPassword.length], masterPassword);

        byte[] salt = Base64.getDecoder().decode(envelope.kdf().salt());
        byte[] nonce = Base64.getDecoder().decode(envelope.cipher().nonce());
        byte[] ciphertextWithTag = Base64.getDecoder().decode(envelope.payload());
        byte[] key = new Argon2Service().generateKey("test-master-password".toCharArray(), salt, envelope.kdf());
        byte[] aad = VaultAadBuilder.build(envelope);

        byte[] plaintext = new AesGcmService().decrypt(key, nonce, ciphertextWithTag, aad);
        VaultPayload payload = mapper.readValue(plaintext, VaultPayload.class);

        assertTrue(payload.entries().isEmpty());
    }

    @Test
    void shouldUseFreshSaltNonceAndPayloadForEachVault() {
        VaultInitializationService service = new VaultInitializationService();

        VaultEnvelope first = service.init("test-master-password".toCharArray(), mapper);
        VaultEnvelope second = service.init("test-master-password".toCharArray(), mapper);

        assertNotEquals(first.kdf().salt(), second.kdf().salt());
        assertNotEquals(first.cipher().nonce(), second.cipher().nonce());
        assertNotEquals(first.payload(), second.payload());
    }
}
