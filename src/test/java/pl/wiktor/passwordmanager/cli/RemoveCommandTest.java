package pl.wiktor.passwordmanager.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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

class RemoveCommandTest {
    private final ObjectMapper mapper = ObjectMapperFactory.create();

    @TempDir
    Path tempDir;

    @Test
    void shouldRemoveEntryByName() throws Exception {
        Path vaultPath = vaultWithEntries(List.of(testEntry("entry-1", "github")));

        CommandResult result = executeQuietly(new RemoveCommand(
                mapper,
                vaultPath,
                prompt -> "test-master-password".toCharArray()),
                "--name", "github");

        VaultEnvelope savedEnvelope = new VaultStorage(mapper).read(vaultPath);
        VaultPayload payload = new VaultUnlockService()
                .unlock(savedEnvelope, "test-master-password".toCharArray(), mapper);

        assertEquals(0, result.exitCode());
        assertTrue(result.stdout().contains("Entry removed: github"));
        assertEquals(List.of(), payload.entries());
    }

    @Test
    void shouldReturnTwoWhenEntryIsNotFoundWithoutChangingVault() throws Exception {
        VaultEntry entry = testEntry("entry-1", "github");
        Path vaultPath = vaultWithEntries(List.of(entry));
        VaultEnvelope initialEnvelope = new VaultStorage(mapper).read(vaultPath);

        CommandResult result = executeQuietly(new RemoveCommand(
                mapper,
                vaultPath,
                prompt -> "test-master-password".toCharArray()),
                "--name", "email");

        VaultEnvelope envelopeAfterFailedRemove = new VaultStorage(mapper).read(vaultPath);
        VaultPayload payload = new VaultUnlockService()
                .unlock(envelopeAfterFailedRemove, "test-master-password".toCharArray(), mapper);

        assertEquals(2, result.exitCode());
        assertTrue(result.stderr().contains("Entry not found: email"));
        assertEquals(initialEnvelope, envelopeAfterFailedRemove);
        assertEquals(List.of(entry), payload.entries());
    }

    @Test
    void shouldRejectWrongMasterPasswordWithoutChangingVault() throws Exception {
        VaultEntry entry = testEntry("entry-1", "github");
        Path vaultPath = vaultWithEntries(List.of(entry));
        VaultEnvelope initialEnvelope = new VaultStorage(mapper).read(vaultPath);

        CommandResult result = executeQuietly(new RemoveCommand(
                mapper,
                vaultPath,
                prompt -> "wrong-password".toCharArray()),
                "--name", "github");

        VaultEnvelope envelopeAfterFailedRemove = new VaultStorage(mapper).read(vaultPath);
        VaultPayload payload = new VaultUnlockService()
                .unlock(envelopeAfterFailedRemove, "test-master-password".toCharArray(), mapper);

        assertEquals(1, result.exitCode());
        assertTrue(result.stderr().contains("Vault unlock failed."));
        assertEquals(initialEnvelope, envelopeAfterFailedRemove);
        assertEquals(List.of(entry), payload.entries());
    }

    @Test
    void shouldReturnTwoWhenVaultDoesNotExist() {
        AtomicBoolean passwordPrompted = new AtomicBoolean(false);
        Path vaultPath = tempDir.resolve("missing-vault.json");

        CommandResult result = executeQuietly(new RemoveCommand(
                mapper,
                vaultPath,
                prompt -> {
                    passwordPrompted.set(true);
                    return "test-master-password".toCharArray();
                }),
                "--name", "github");

        assertEquals(2, result.exitCode());
        assertFalse(passwordPrompted.get());
    }

    @Test
    void shouldReturnOneWhenMasterPasswordIsNotProvided() throws Exception {
        VaultEntry entry = testEntry("entry-1", "github");
        Path vaultPath = vaultWithEntries(List.of(entry));
        VaultEnvelope initialEnvelope = new VaultStorage(mapper).read(vaultPath);

        CommandResult result = executeQuietly(new RemoveCommand(
                mapper,
                vaultPath,
                prompt -> null),
                "--name", "github");

        VaultEnvelope envelopeAfterFailedRemove = new VaultStorage(mapper).read(vaultPath);

        assertEquals(1, result.exitCode());
        assertTrue(result.stderr().contains("Master password was not provided."));
        assertEquals(initialEnvelope, envelopeAfterFailedRemove);
    }

    @Test
    void shouldReturnTwoWhenVaultHasNoEntries() throws Exception {
        Path vaultPath = initializedVaultPath();

        CommandResult result = executeQuietly(new RemoveCommand(
                mapper,
                vaultPath,
                prompt -> "test-master-password".toCharArray()),
                "--name", "github");

        VaultEnvelope savedEnvelope = new VaultStorage(mapper).read(vaultPath);
        VaultPayload payload = new VaultUnlockService()
                .unlock(savedEnvelope, "test-master-password".toCharArray(), mapper);

        assertEquals(2, result.exitCode());
        assertTrue(result.stderr().contains("Entry not found: github"));
        assertEquals(List.of(), payload.entries());
    }

    private Path initializedVaultPath() throws Exception {
        Path vaultPath = tempDir.resolve("vault.json");
        VaultEnvelope envelope = new VaultInitializationService()
                .init("test-master-password".toCharArray(), mapper);
        new VaultStorage(mapper).writeNew(vaultPath, envelope);
        return vaultPath;
    }

    private Path vaultWithEntries(List<VaultEntry> entries) throws Exception {
        Path vaultPath = tempDir.resolve("vault.json");
        VaultEnvelope initialEnvelope = new VaultInitializationService()
                .init("test-master-password".toCharArray(), mapper);
        VaultEnvelope savedEnvelope = new VaultSaveService()
                .save(initialEnvelope, new VaultPayload(entries), "test-master-password".toCharArray(), mapper);
        new VaultStorage(mapper).writeNew(vaultPath, savedEnvelope);
        return vaultPath;
    }

    private VaultEntry testEntry(String id, String name) {
        Instant now = Instant.parse("2026-05-13T12:00:00Z");
        return new VaultEntry(
                id,
                name,
                "user@example.com",
                "secret-password",
                "https://github.com",
                "primary account",
                now,
                now);
    }

    private CommandResult executeQuietly(RemoveCommand command, String... args) {
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(stdout));
            System.setErr(new PrintStream(stderr));
            int exitCode = new CommandLine(command).execute(args);
            return new CommandResult(exitCode, stdout.toString(), stderr.toString());
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

    private record CommandResult(int exitCode, String stdout, String stderr) {
    }
}
