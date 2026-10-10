package pl.wiktor.passwordmanager.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.util.Set;

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

    @Test
    void shouldReplaceExistingVaultFile() throws Exception {
        VaultStorage storage = new VaultStorage(mapper);
        Path vaultPath = tempDir.resolve("vault.json");
        VaultEnvelope initialEnvelope = testEnvelope();
        VaultEnvelope replacementEnvelope = replacementEnvelope();

        storage.writeNew(vaultPath, initialEnvelope);
        storage.writeReplace(vaultPath, replacementEnvelope);

        VaultEnvelope readEnvelope = storage.read(vaultPath);

        assertEquals(replacementEnvelope, readEnvelope);
    }

    @Test
    void shouldNotReplaceMissingVaultFile() {
        VaultStorage storage = new VaultStorage(mapper);
        Path vaultPath = tempDir.resolve("vault.json");

        assertThrows(NoSuchFileException.class, () -> storage.writeReplace(vaultPath, testEnvelope()));
        assertTrue(Files.notExists(vaultPath));
    }

    @Test
    void shouldKeepVaultWhenBackupCannotBeCreated() throws Exception {
        Path vaultPath = tempDir.resolve("vault.json");
        VaultStorage storage = new VaultStorage(mapper);
        storage.writeNew(vaultPath, testEnvelope());
        byte[] before = Files.readAllBytes(vaultPath);
        Files.createDirectory(tempDir.resolve("vault.json.bak"));

        assertThrows(IOException.class, () -> storage.writeReplace(vaultPath, replacementEnvelope()));
        assertTrue(java.util.Arrays.equals(before, Files.readAllBytes(vaultPath)));
    }

    @Test
    void shouldKeepVaultAndRecoverableBackupWhenReplacementFails() throws Exception {
        Path vaultPath = tempDir.resolve("vault.json");
        VaultStorage storage = new VaultStorage(mapper);
        storage.writeNew(vaultPath, testEnvelope());
        byte[] before = Files.readAllBytes(vaultPath);
        VaultStorage failingStorage = new VaultStorage(mapper) {
            @Override
            protected void moveReplacement(Path source, Path destination) throws IOException {
                throw new IOException("simulated atomic replacement failure");
            }
        };

        assertThrows(IOException.class, () -> failingStorage.writeReplace(vaultPath, replacementEnvelope()));
        assertTrue(java.util.Arrays.equals(before, Files.readAllBytes(vaultPath)));
        assertTrue(java.util.Arrays.equals(before, Files.readAllBytes(tempDir.resolve("vault.json.bak"))));
    }

    @Test
    void shouldKeepVaultWhenDirectoryIsNotWritable() throws Exception {
        assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"));
        Path directory = Files.createDirectory(tempDir.resolve("read-only"));
        Path vaultPath = directory.resolve("vault.json");
        VaultStorage storage = new VaultStorage(mapper);
        storage.writeNew(vaultPath, testEnvelope());
        byte[] before = Files.readAllBytes(vaultPath);
        Set<PosixFilePermission> original = Files.getPosixFilePermissions(directory);
        try {
            Files.setPosixFilePermissions(directory, Set.of(
                    PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_EXECUTE));
            assumeFalse(Files.isWritable(directory));
            assertThrows(IOException.class, () -> storage.writeReplace(vaultPath, replacementEnvelope()));
            assertTrue(java.util.Arrays.equals(before, Files.readAllBytes(vaultPath)));
        } finally {
            Files.setPosixFilePermissions(directory, original);
        }
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

    private VaultEnvelope replacementEnvelope() {
        Instant createdAt = Instant.parse("2026-05-11T12:00:00Z");
        Instant updatedAt = Instant.parse("2026-05-12T12:00:00Z");
        return new VaultEnvelope(
                "passwordmanager-vault",
                1,
                createdAt,
                updatedAt,
                new KdfParams("argon2id", 19456, 2, 1, "salt-base64"),
                new CipherParams("AES-256-GCM", "new-nonce-base64", 128),
                "new-payload-base64");
    }
}
