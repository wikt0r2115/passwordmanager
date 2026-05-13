package pl.wiktor.passwordmanager.cli;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

import picocli.CommandLine;
import pl.wiktor.passwordmanager.io.ObjectMapperFactory;
import pl.wiktor.passwordmanager.io.VaultStorage;
import pl.wiktor.passwordmanager.model.VaultEnvelope;
import pl.wiktor.passwordmanager.model.VaultPayload;
import pl.wiktor.passwordmanager.vault.VaultInitializationService;
import pl.wiktor.passwordmanager.vault.VaultUnlockService;

class InitCommandTest {
    private final ObjectMapper mapper = ObjectMapperFactory.create();

    @TempDir
    Path tempDir;

    @Test
    void shouldCreateVault() throws Exception {
        Path vaultPath = tempDir.resolve("vault.json");
        AtomicReference<char[]> masterPassword = new AtomicReference<>("test-master-password".toCharArray());

        CommandResult result = executeQuietly(new InitCommand(
                mapper,
                vaultPath,
                prompt -> masterPassword.get()));

        VaultEnvelope envelope = new VaultStorage(mapper).read(vaultPath);
        VaultPayload payload = new VaultUnlockService()
                .unlock(envelope, "test-master-password".toCharArray(), mapper);

        assertEquals(0, result.exitCode());
        assertTrue(result.stdout().contains("Initializing vault..."));
        assertTrue(result.stdout().contains("Vault created: " + vaultPath));
        assertTrue(Files.exists(vaultPath));
        assertEquals(List.of(), payload.entries());
        assertArrayEquals(new char[masterPassword.get().length], masterPassword.get());
    }

    @Test
    void shouldCreateParentDirectories() {
        Path vaultPath = tempDir.resolve("config").resolve("passwordmanager").resolve("vault.json");

        CommandResult result = executeQuietly(new InitCommand(
                mapper,
                vaultPath,
                prompt -> "test-master-password".toCharArray()));

        assertEquals(0, result.exitCode());
        assertTrue(Files.exists(vaultPath));
    }

    @Test
    void shouldReturnTwoWhenVaultAlreadyExistsWithoutPrompting() throws Exception {
        AtomicBoolean passwordPrompted = new AtomicBoolean(false);
        Path vaultPath = initializedVaultPath();
        VaultEnvelope initialEnvelope = new VaultStorage(mapper).read(vaultPath);

        CommandResult result = executeQuietly(new InitCommand(
                mapper,
                vaultPath,
                prompt -> {
                    passwordPrompted.set(true);
                    return "test-master-password".toCharArray();
                }));

        VaultEnvelope envelopeAfterFailedInit = new VaultStorage(mapper).read(vaultPath);

        assertEquals(2, result.exitCode());
        assertTrue(result.stderr().contains("Vault already exists: " + vaultPath));
        assertFalse(passwordPrompted.get());
        assertEquals(initialEnvelope, envelopeAfterFailedInit);
    }

    @Test
    void shouldReturnOneWhenMasterPasswordIsNotProvided() {
        Path vaultPath = tempDir.resolve("vault.json");

        CommandResult result = executeQuietly(new InitCommand(
                mapper,
                vaultPath,
                prompt -> null));

        assertEquals(1, result.exitCode());
        assertTrue(result.stderr().contains("Master password was not provided."));
        assertTrue(Files.notExists(vaultPath));
    }

    private Path initializedVaultPath() throws Exception {
        Path vaultPath = tempDir.resolve("vault.json");
        VaultEnvelope envelope = new VaultInitializationService()
                .init("test-master-password".toCharArray(), mapper);
        new VaultStorage(mapper).writeNew(vaultPath, envelope);
        return vaultPath;
    }

    private CommandResult executeQuietly(InitCommand command, String... args) {
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
