package pl.wiktor.passwordmanager.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

import picocli.CommandLine;
import pl.wiktor.passwordmanager.io.ObjectMapperFactory;
import pl.wiktor.passwordmanager.io.VaultStorage;
import pl.wiktor.passwordmanager.model.VaultEntry;
import pl.wiktor.passwordmanager.model.VaultEnvelope;
import pl.wiktor.passwordmanager.model.VaultPayload;
import pl.wiktor.passwordmanager.vault.VaultInitializationService;
import pl.wiktor.passwordmanager.vault.VaultSaveService;
import pl.wiktor.passwordmanager.vault.VaultUnlockService;

class UpdateCommandTest {
    private final ObjectMapper mapper = ObjectMapperFactory.create();

    @TempDir
    Path tempDir;

    @Test
    void shouldUpdateEntryFields() throws Exception {
        Path vaultPath = initializedVaultPath();
        addEntry(vaultPath, "github", "user", "secret");

        int exitCode = executeQuietly(new UpdateCommand(
                mapper,
                vaultPath,
                prompt -> "test-master-password".toCharArray()),
                "--name", "github",
                "--new-name", "github-new",
                "--username", "user-new",
                "--password", "secret-new",
                "--url", "https://new.com",
                "--notes", "new notes");

        VaultEnvelope savedEnvelope = new VaultStorage(mapper).read(vaultPath);
        VaultPayload payload = new VaultUnlockService()
                .unlock(savedEnvelope, "test-master-password".toCharArray(), mapper);
        VaultEntry entry = payload.entries().getFirst();

        assertEquals(0, exitCode);
        assertEquals(1, payload.entries().size());
        assertEquals("github-new", entry.name());
        assertEquals("user-new", entry.username());
        assertEquals("secret-new", entry.password());
        assertEquals("https://new.com", entry.url());
        assertEquals("new notes", entry.notes());
        assertNotEquals(entry.createdAt(), entry.updatedAt());
    }

    @Test
    void shouldGenerateNewPasswordDuringUpdate() throws Exception {
        Path vaultPath = initializedVaultPath();
        addEntry(vaultPath, "github", "user", "secret");

        int exitCode = executeQuietly(new UpdateCommand(
                mapper,
                vaultPath,
                prompt -> "test-master-password".toCharArray()),
                "--name", "github",
                "--generate",
                "--length", "32");

        VaultEnvelope savedEnvelope = new VaultStorage(mapper).read(vaultPath);
        VaultPayload payload = new VaultUnlockService()
                .unlock(savedEnvelope, "test-master-password".toCharArray(), mapper);
        VaultEntry entry = payload.entries().getFirst();

        assertEquals(0, exitCode);
        assertEquals(32, entry.password().length());
        assertNotEquals("secret", entry.password());
    }

    @Test
    void shouldPromptForNewPasswordDuringUpdate() throws Exception {
        Path vaultPath = initializedVaultPath();
        addEntry(vaultPath, "github", "user", "secret");

        int exitCode = executeQuietly(new UpdateCommand(
                mapper,
                vaultPath,
                prompt -> {
                    if (prompt.contains("new entry password")) {
                        return "prompted-secret".toCharArray();
                    }
                    return "test-master-password".toCharArray();
                }),
                "--name", "github",
                "--prompt-password");

        VaultEnvelope savedEnvelope = new VaultStorage(mapper).read(vaultPath);
        VaultPayload payload = new VaultUnlockService()
                .unlock(savedEnvelope, "test-master-password".toCharArray(), mapper);
        VaultEntry entry = payload.entries().getFirst();

        assertEquals(0, exitCode);
        assertEquals("prompted-secret", entry.password());
    }

    @Test
    void shouldRejectMultiplePasswordSources() throws Exception {
        Path vaultPath = initializedVaultPath();

        int exitCode = executeQuietly(new UpdateCommand(
                mapper,
                vaultPath,
                prompt -> "test-master-password".toCharArray()),
                "--name", "github",
                "--password", "new-secret",
                "--prompt-password");

        assertEquals(1, exitCode);
    }

    @Test
    void shouldRejectWrongMasterPasswordWithoutChangingVault() throws Exception {
        Path vaultPath = initializedVaultPath();
        addEntry(vaultPath, "github", "user", "secret");
        VaultEnvelope initialEnvelope = new VaultStorage(mapper).read(vaultPath);

        int exitCode = executeQuietly(new UpdateCommand(
                mapper,
                vaultPath,
                prompt -> "wrong-password".toCharArray()),
                "--name", "github",
                "--password", "new-secret");

        VaultEnvelope envelopeAfterFailedUpdate = new VaultStorage(mapper).read(vaultPath);
        VaultPayload payload = new VaultUnlockService()
                .unlock(envelopeAfterFailedUpdate, "test-master-password".toCharArray(), mapper);

        assertEquals(1, exitCode);
        assertEquals(initialEnvelope, envelopeAfterFailedUpdate);
        assertEquals("secret", payload.entries().getFirst().password());
    }

    @Test
    void shouldFailWhenPromptedPasswordIsNotProvided() throws Exception {
        Path vaultPath = initializedVaultPath();
        addEntry(vaultPath, "github", "user", "secret");
        VaultEnvelope initialEnvelope = new VaultStorage(mapper).read(vaultPath);

        int exitCode = executeQuietly(new UpdateCommand(
                mapper,
                vaultPath,
                prompt -> prompt.contains("new entry password")
                        ? null
                        : "test-master-password".toCharArray()),
                "--name", "github",
                "--prompt-password");

        VaultEnvelope envelopeAfterFailedUpdate = new VaultStorage(mapper).read(vaultPath);

        assertEquals(1, exitCode);
        assertEquals(initialEnvelope, envelopeAfterFailedUpdate);
    }

    @Test
    void shouldFailWhenEntryNotFound() throws Exception {
        Path vaultPath = initializedVaultPath();

        int exitCode = executeQuietly(new UpdateCommand(
                mapper,
                vaultPath,
                prompt -> "test-master-password".toCharArray()),
                "--name", "missing",
                "--password", "new-secret");

        assertEquals(2, exitCode);
    }

    @Test
    void shouldFailWhenNothingToUpdate() throws Exception {
        Path vaultPath = initializedVaultPath();

        int exitCode = executeQuietly(new UpdateCommand(
                mapper,
                vaultPath,
                prompt -> "test-master-password".toCharArray()),
                "--name", "github");

        assertEquals(1, exitCode);
    }

    private Path initializedVaultPath() throws Exception {
        Path vaultPath = tempDir.resolve("vault.json");
        VaultEnvelope envelope = new VaultInitializationService()
                .init("test-master-password".toCharArray(), mapper);
        new VaultStorage(mapper).writeNew(vaultPath, envelope);
        return vaultPath;
    }

    private void addEntry(Path vaultPath, String name, String username, String password) throws Exception {
        VaultStorage storage = new VaultStorage(mapper);
        VaultEnvelope envelope = storage.read(vaultPath);
        VaultUnlockService unlockService = new VaultUnlockService();
        VaultPayload payload = unlockService.unlock(envelope, "test-master-password".toCharArray(), mapper);
        
        Instant now = Instant.now();
        VaultEntry entry = new VaultEntry("id", name, username, password, null, null, now, now);
        VaultPayload updatedPayload = new VaultPayload(List.of(entry));
        
        VaultEnvelope newEnvelope = new VaultSaveService().save(envelope, updatedPayload, "test-master-password".toCharArray(), mapper);
        storage.writeReplace(vaultPath, newEnvelope);
    }

    private int executeQuietly(UpdateCommand command, String... args) {
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        try {
            System.setOut(new PrintStream(new ByteArrayOutputStream()));
            System.setErr(new PrintStream(new ByteArrayOutputStream()));
            return new CommandLine(command).execute(args);
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }
}
