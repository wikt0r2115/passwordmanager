package pl.wiktor.passwordmanager.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
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
import pl.wiktor.passwordmanager.vault.VaultUnlockService;

class AddCommandTest {
    private final ObjectMapper mapper = ObjectMapperFactory.create();

    @TempDir
    Path tempDir;

    @Test
    void shouldAddEntryToVault() throws Exception {
        Path vaultPath = initializedVaultPath();

        int exitCode = executeQuietly(new AddCommand(
                mapper,
                vaultPath,
                prompt -> "test-master-password".toCharArray()),
                "--name", "github",
                "--username", "user@example.com",
                "--password", "secret-password",
                "--url", "https://github.com",
                "--notes", "primary account");

        VaultEnvelope savedEnvelope = new VaultStorage(mapper).read(vaultPath);
        VaultPayload payload = new VaultUnlockService()
                .unlock(savedEnvelope, "test-master-password".toCharArray(), mapper);
        VaultEntry entry = payload.entries().getFirst();

        assertEquals(0, exitCode);
        assertEquals(1, payload.entries().size());
        assertEquals("github", entry.name());
        assertEquals("user@example.com", entry.username());
        assertEquals("secret-password", entry.password());
        assertEquals("https://github.com", entry.url());
        assertEquals("primary account", entry.notes());
    }

    @Test
    void shouldRejectWrongMasterPasswordWithoutChangingVault() throws Exception {
        Path vaultPath = initializedVaultPath();
        VaultEnvelope initialEnvelope = new VaultStorage(mapper).read(vaultPath);

        int exitCode = executeQuietly(new AddCommand(
                mapper,
                vaultPath,
                prompt -> "wrong-password".toCharArray()),
                "--name", "github",
                "--username", "user@example.com",
                "--password", "secret-password");

        VaultEnvelope envelopeAfterFailedAdd = new VaultStorage(mapper).read(vaultPath);
        VaultPayload payload = new VaultUnlockService()
                .unlock(envelopeAfterFailedAdd, "test-master-password".toCharArray(), mapper);

        assertEquals(1, exitCode);
        assertEquals(initialEnvelope, envelopeAfterFailedAdd);
        assertEquals(0, payload.entries().size());
    }

    @Test
    void shouldReturnTwoWhenVaultDoesNotExist() {
        AtomicBoolean passwordPrompted = new AtomicBoolean(false);
        Path vaultPath = tempDir.resolve("missing-vault.json");

        int exitCode = executeQuietly(new AddCommand(
                mapper,
                vaultPath,
                prompt -> {
                    passwordPrompted.set(true);
                    return "test-master-password".toCharArray();
                }),
                "--name", "github",
                "--username", "user@example.com",
                "--password", "secret-password");

        assertEquals(2, exitCode);
        assertFalse(passwordPrompted.get());
    }

    @Test
    void shouldReturnOneWhenMasterPasswordIsNotProvided() throws Exception {
        Path vaultPath = initializedVaultPath();
        VaultEnvelope initialEnvelope = new VaultStorage(mapper).read(vaultPath);

        int exitCode = executeQuietly(new AddCommand(
                mapper,
                vaultPath,
                prompt -> null),
                "--name", "github",
                "--username", "user@example.com",
                "--password", "secret-password");

        VaultEnvelope envelopeAfterFailedAdd = new VaultStorage(mapper).read(vaultPath);

        assertEquals(1, exitCode);
        assertEquals(initialEnvelope, envelopeAfterFailedAdd);
    }

    @Test
    void shouldAddEntryWithGeneratedPassword() throws Exception {
        Path vaultPath = initializedVaultPath();

        int exitCode = executeQuietly(new AddCommand(
                mapper,
                vaultPath,
                prompt -> "test-master-password".toCharArray()),
                "--name", "github",
                "--username", "user@example.com",
                "--generate",
                "--length", "24",
                "--no-symbols");

        VaultEnvelope savedEnvelope = new VaultStorage(mapper).read(vaultPath);
        VaultPayload payload = new VaultUnlockService()
                .unlock(savedEnvelope, "test-master-password".toCharArray(), mapper);
        VaultEntry entry = payload.entries().getFirst();

        assertEquals(0, exitCode);
        assertEquals(24, entry.password().length());
        
        String symbols = "!@#$%^&*()-_=+[]{}|;:,.<>?";
        for (char c : entry.password().toCharArray()) {
            assertFalse(symbols.indexOf(c) != -1, "Password should not contain symbols");
        }
    }

    @Test
    void shouldFailWhenNeitherPasswordNorGenerateProvided() throws Exception {
        Path vaultPath = initializedVaultPath();

        int exitCode = executeQuietly(new AddCommand(
                mapper,
                vaultPath,
                prompt -> "test-master-password".toCharArray()),
                "--name", "github",
                "--username", "user@example.com");

        assertEquals(1, exitCode);
    }

    @Test
    void shouldFailWhenBothPasswordAndGenerateProvided() throws Exception {
        Path vaultPath = initializedVaultPath();

        int exitCode = executeQuietly(new AddCommand(
                mapper,
                vaultPath,
                prompt -> "test-master-password".toCharArray()),
                "--name", "github",
                "--username", "user@example.com",
                "--password", "secret",
                "--generate");

        assertEquals(1, exitCode);
    }

    private Path initializedVaultPath() throws Exception {
        Path vaultPath = tempDir.resolve("vault.json");
        VaultEnvelope envelope = new VaultInitializationService()
                .init("test-master-password".toCharArray(), mapper);
        new VaultStorage(mapper).writeNew(vaultPath, envelope);
        return vaultPath;
    }

    private int executeQuietly(AddCommand command, String... args) {
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
