package pl.wiktor.passwordmanager.vault;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import pl.wiktor.passwordmanager.io.ObjectMapperFactory;
import pl.wiktor.passwordmanager.model.VaultEntry;
import pl.wiktor.passwordmanager.model.VaultEnvelope;
import pl.wiktor.passwordmanager.model.VaultPayload;

class VaultSaveServiceTest {
    private final ObjectMapper mapper = ObjectMapperFactory.create();

    @Test
    void shouldSaveAndUnlockUpdatedPayload() throws Exception {
        VaultEnvelope initialEnvelope = new VaultInitializationService()
                .init("test-master-password".toCharArray(), mapper);
        VaultPayload initialPayload = new VaultUnlockService()
                .unlock(initialEnvelope, "test-master-password".toCharArray(), mapper);
        VaultEntry entry = testEntry();
        VaultPayload updatedPayload = new VaultPayload(List.of(entry));
        char[] masterPassword = "test-master-password".toCharArray();

        VaultEnvelope savedEnvelope = new VaultSaveService()
                .save(initialEnvelope, updatedPayload, masterPassword, mapper);
        VaultPayload unlockedPayload = new VaultUnlockService()
                .unlock(savedEnvelope, "test-master-password".toCharArray(), mapper);

        assertTrue(initialPayload.entries().isEmpty());
        assertEquals(List.of(entry), unlockedPayload.entries());
        assertArrayEquals(new char[masterPassword.length], masterPassword);
    }

    @Test
    void shouldPreserveKdfSaltAndRefreshNonce() throws Exception {
        VaultEnvelope initialEnvelope = new VaultInitializationService()
                .init("test-master-password".toCharArray(), mapper);
        char[] masterPassword = "test-master-password".toCharArray();

        VaultEnvelope savedEnvelope = new VaultSaveService()
                .save(initialEnvelope, new VaultPayload(List.of(testEntry())), masterPassword, mapper);

        assertEquals(initialEnvelope.kdf(), savedEnvelope.kdf());
        assertEquals(initialEnvelope.kdf().salt(), savedEnvelope.kdf().salt());
        assertNotEquals(initialEnvelope.cipher().nonce(), savedEnvelope.cipher().nonce());
        assertNotEquals(initialEnvelope.payload(), savedEnvelope.payload());
        assertEquals(initialEnvelope.createdAt(), savedEnvelope.createdAt());
    }

    @Test
    void shouldRefreshUpdatedAt() throws Exception {
        VaultEnvelope initialEnvelope = new VaultInitializationService()
                .init("test-master-password".toCharArray(), mapper);

        VaultEnvelope savedEnvelope = new VaultSaveService()
                .save(initialEnvelope, new VaultPayload(List.of(testEntry())),
                        "test-master-password".toCharArray(), mapper);

        assertTrue(savedEnvelope.updatedAt().isAfter(initialEnvelope.updatedAt())
                || savedEnvelope.updatedAt().equals(initialEnvelope.updatedAt()));
    }

    private VaultEntry testEntry() {
        Instant now = Instant.parse("2026-05-12T12:00:00Z");
        return new VaultEntry(
                "entry-1",
                "github",
                "user@example.com",
                "secret-password",
                "https://github.com",
                "primary account",
                now,
                now);
    }
}
