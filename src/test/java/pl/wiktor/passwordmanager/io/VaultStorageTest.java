package pl.wiktor.passwordmanager.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

import pl.wiktor.passwordmanager.model.CipherParams;
import pl.wiktor.passwordmanager.model.KdfParams;
import pl.wiktor.passwordmanager.model.VaultEnvelope;

class VaultStorageTest {
    private final ObjectMapper mapper = ObjectMapperFactory.create();

    @TempDir
    Path tempDir;

    @Test
    void shouldWriteReadableVaultFile() throws Exception {
        VaultStorage storage = new VaultStorage(mapper);
        Path vaultPath = tempDir.resolve("vault.json");
        VaultEnvelope envelope = testEnvelope();

        storage.writeNew(vaultPath, envelope);

        assertTrue(Files.exists(vaultPath));
        VaultEnvelope writtenEnvelope = mapper.readValue(vaultPath.toFile(), VaultEnvelope.class);
        assertEquals(envelope.format(), writtenEnvelope.format());
        assertEquals(envelope.version(), writtenEnvelope.version());
        assertEquals(envelope.kdf(), writtenEnvelope.kdf());
        assertEquals(envelope.cipher(), writtenEnvelope.cipher());
        assertEquals(envelope.payload(), writtenEnvelope.payload());
    }

    @Test
    void shouldNotOverwriteExistingVaultFile() throws Exception {
        VaultStorage storage = new VaultStorage(mapper);
        Path vaultPath = tempDir.resolve("vault.json");
        Files.writeString(vaultPath, "existing vault", StandardCharsets.UTF_8);

        assertThrows(FileAlreadyExistsException.class, () -> storage.writeNew(vaultPath, testEnvelope()));
        assertEquals("existing vault", Files.readString(vaultPath, StandardCharsets.UTF_8));
    }

    @Test
    void shouldReadVaultEnvelope() throws Exception {
        VaultStorage storage = new VaultStorage(mapper);
        Path vaultPath = tempDir.resolve("vault.json");
        VaultEnvelope envelope = testEnvelope();

        storage.writeNew(vaultPath, envelope);

        VaultEnvelope readEnvelope = storage.read(vaultPath);

        assertEquals(envelope, readEnvelope);
    }

    private VaultEnvelope testEnvelope() {
        Instant now = Instant.parse("2026-05-11T12:00:00Z");
        return new VaultEnvelope(
                "passwordmanager-vault",
                1,
                now,
                now,
                new KdfParams("argon2id", 19456, 2, 1, "salt-base64"),
                new CipherParams("AES-256-GCM", "nonce-base64", 128),
                "payload-base64");
    }
}
