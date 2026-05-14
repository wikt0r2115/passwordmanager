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
import pl.wiktor.passwordmanager.vault.VaultUnlockService;

class ExportCommandTest {
    private final ObjectMapper mapper = ObjectMapperFactory.create();

    @TempDir
    Path tempDir;

    @Test
    void shouldExportVaultToPlaintextJson() throws Exception {
        Path vaultPath = initializedVaultPath();
        addEntry(vaultPath, "github", "user", "secret");
        Path exportPath = tempDir.resolve("export.json");

        CommandResult result = executeQuietly(new ExportCommand(
                mapper,
                vaultPath,
                prompt -> "test-master-password".toCharArray()),
                "--output", exportPath.toString());

        assertEquals(0, result.exitCode());
        assertTrue(result.stdout().contains("WARNING: The exported file is plaintext"));
        assertTrue(Files.exists(exportPath));
        
        VaultPayload exportedPayload = mapper.readValue(exportPath.toFile(), VaultPayload.class);
        assertEquals(1, exportedPayload.entries().size());
        assertEquals("github", exportedPayload.entries().getFirst().name());
    }

    @Test
    void shouldReturnTwoWhenVaultDoesNotExistWithoutPromptingForPassword() {
        AtomicBoolean passwordPrompted = new AtomicBoolean(false);
        Path vaultPath = tempDir.resolve("missing-vault.json");
        Path exportPath = tempDir.resolve("export.json");

        CommandResult result = executeQuietly(new ExportCommand(
                mapper,
                vaultPath,
                prompt -> {
                    passwordPrompted.set(true);
                    return "test-master-password".toCharArray();
                }),
                "--output", exportPath.toString());

        assertEquals(2, result.exitCode());
        assertFalse(passwordPrompted.get());
        assertFalse(Files.exists(exportPath));
    }

    @Test
    void shouldRejectWrongMasterPasswordWithoutWritingExportFile() throws Exception {
        Path vaultPath = initializedVaultPath();
        addEntry(vaultPath, "github", "user", "secret");
        Path exportPath = tempDir.resolve("export.json");

        CommandResult result = executeQuietly(new ExportCommand(
                mapper,
                vaultPath,
                prompt -> "wrong-password".toCharArray()),
                "--output", exportPath.toString());

        assertEquals(1, result.exitCode());
        assertTrue(result.stderr().contains("Vault unlock failed."));
        assertFalse(Files.exists(exportPath));
    }

    @Test
    void shouldReturnOneWhenMasterPasswordIsNotProvided() throws Exception {
        Path vaultPath = initializedVaultPath();
        Path exportPath = tempDir.resolve("export.json");

        CommandResult result = executeQuietly(new ExportCommand(
                mapper,
                vaultPath,
                prompt -> null),
                "--output", exportPath.toString());

        assertEquals(1, result.exitCode());
        assertTrue(result.stderr().contains("Master password was not provided."));
        assertFalse(Files.exists(exportPath));
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

    private CommandResult executeQuietly(ExportCommand command, String... args) {
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
