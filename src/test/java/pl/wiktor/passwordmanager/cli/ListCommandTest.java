package pl.wiktor.passwordmanager.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
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

class ListCommandTest {
    private final ObjectMapper mapper = ObjectMapperFactory.create();

    @TempDir
    Path tempDir;

    @Test
    void shouldListEntriesWithoutShowingPasswords() throws Exception {
        Path vaultPath = vaultWithEntries(List.of(
                testEntry("entry-1", "github", "github-password", "https://github.com"),
                testEntry("entry-2", "email", "email-password", "https://mail.example.com")));

        CommandResult result = executeQuietly(new ListCommand(
                mapper,
                vaultPath,
                prompt -> "test-master-password".toCharArray()));

        assertEquals(0, result.exitCode());
        assertTrue(result.stdout().contains("github\tuser@example.com\thttps://github.com"));
        assertTrue(result.stdout().contains("email\tuser@example.com\thttps://mail.example.com"));
        assertFalse(result.stdout().contains("github-password"));
        assertFalse(result.stdout().contains("email-password"));
    }

    @Test
    void shouldReturnZeroWhenVaultHasNoEntries() throws Exception {
        Path vaultPath = initializedVaultPath();

        CommandResult result = executeQuietly(new ListCommand(
                mapper,
                vaultPath,
                prompt -> "test-master-password".toCharArray()));

        assertEquals(0, result.exitCode());
        assertTrue(result.stdout().contains("No entries found."));
    }

    @Test
    void shouldRejectWrongMasterPassword() throws Exception {
        Path vaultPath = vaultWithEntries(List.of(
                testEntry("entry-1", "github", "github-password", "https://github.com")));

        CommandResult result = executeQuietly(new ListCommand(
                mapper,
                vaultPath,
                prompt -> "wrong-password".toCharArray()));

        assertEquals(1, result.exitCode());
        assertTrue(result.stderr().contains("Vault unlock failed."));
    }

    @Test
    void shouldReturnTwoWhenVaultDoesNotExist() {
        AtomicBoolean passwordPrompted = new AtomicBoolean(false);
        Path vaultPath = tempDir.resolve("missing-vault.json");

        CommandResult result = executeQuietly(new ListCommand(
                mapper,
                vaultPath,
                prompt -> {
                    passwordPrompted.set(true);
                    return "test-master-password".toCharArray();
                }));

        assertEquals(2, result.exitCode());
        assertFalse(passwordPrompted.get());
    }

    @Test
    void shouldReturnOneWhenMasterPasswordIsNotProvided() throws Exception {
        Path vaultPath = vaultWithEntries(List.of(
                testEntry("entry-1", "github", "github-password", "https://github.com")));

        CommandResult result = executeQuietly(new ListCommand(
                mapper,
                vaultPath,
                prompt -> null));

        assertEquals(1, result.exitCode());
        assertTrue(result.stderr().contains("Master password was not provided."));
    }

    @Test
    void shouldReturnOneWhenVaultEnvelopeIsInvalid() throws Exception {
        Path vaultPath = initializedVaultPath();
        VaultEnvelope envelope = new VaultStorage(mapper).read(vaultPath);
        VaultEnvelope invalidEnvelope = new VaultEnvelope(
                envelope.format(),
                99,
                envelope.createdAt(),
                envelope.updatedAt(),
                envelope.kdf(),
                envelope.cipher(),
                envelope.payload());
        Files.delete(vaultPath);
        new VaultStorage(mapper).writeNew(vaultPath, invalidEnvelope);

        CommandResult result = executeQuietly(new ListCommand(
                mapper,
                vaultPath,
                prompt -> "test-master-password".toCharArray()));

        assertEquals(1, result.exitCode());
        assertTrue(result.stderr().contains("Invalid vault file: Unsupported vault version: 99"));
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

    private VaultEntry testEntry(String id, String name, String password, String url) {
        Instant now = Instant.parse("2026-05-13T12:00:00Z");
        return new VaultEntry(
                id,
                name,
                "user@example.com",
                password,
                url,
                "primary account",
                now,
                now);
    }

    private CommandResult executeQuietly(ListCommand command, String... args) {
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
