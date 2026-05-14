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
import pl.wiktor.passwordmanager.vault.VaultUnlockService;

class ImportCommandTest {
    private final ObjectMapper mapper = ObjectMapperFactory.create();

    @TempDir
    Path tempDir;

    @Test
    void shouldImportEntriesFromPlaintextJson() throws Exception {
        Path vaultPath = initializedVaultPath();
        Path importPath = tempDir.resolve("import.json");
        
        Instant now = Instant.now();
        VaultEntry entry = new VaultEntry("id-123", "imported", "user", "pass", null, null, now, now);
        VaultPayload importPayload = new VaultPayload(List.of(entry));
        mapper.writeValue(importPath.toFile(), importPayload);

        CommandResult result = executeQuietly(new ImportCommand(
                mapper,
                vaultPath,
                prompt -> "test-master-password".toCharArray()),
                "--input", importPath.toString());

        assertEquals(0, result.exitCode());
        assertTrue(result.stdout().contains("Import successful. Added 1 entries."));
        
        VaultEnvelope savedEnvelope = new VaultStorage(mapper).read(vaultPath);
        VaultPayload payload = new VaultUnlockService()
                .unlock(savedEnvelope, "test-master-password".toCharArray(), mapper);
        
        assertEquals(1, payload.entries().size());
        assertEquals("imported", payload.entries().getFirst().name());
        
        // Verify backup creation
        assertTrue(Files.exists(vaultPath.resolveSibling("vault.json.bak")));
    }

    @Test
    void shouldSkipDuplicateIdsAndReportOnlyAddedEntries() throws Exception {
        Instant now = Instant.now();
        VaultEntry existingEntry = new VaultEntry("id-123", "existing", "user", "pass", null, null, now, now);
        Path vaultPath = vaultWithEntries(List.of(existingEntry));
        Path importPath = tempDir.resolve("import.json");
        VaultEntry duplicateEntry = new VaultEntry("id-123", "duplicate", "user", "pass", null, null, now, now);
        VaultEntry newEntry = new VaultEntry("id-456", "new", "user", "pass", null, null, now, now);
        mapper.writeValue(importPath.toFile(), new VaultPayload(List.of(duplicateEntry, newEntry)));

        CommandResult result = executeQuietly(new ImportCommand(
                mapper,
                vaultPath,
                prompt -> "test-master-password".toCharArray()),
                "--input", importPath.toString());

        VaultEnvelope savedEnvelope = new VaultStorage(mapper).read(vaultPath);
        VaultPayload payload = new VaultUnlockService()
                .unlock(savedEnvelope, "test-master-password".toCharArray(), mapper);

        assertEquals(0, result.exitCode());
        assertTrue(result.stdout().contains("Import successful. Added 1 entries."));
        assertEquals(2, payload.entries().size());
        assertEquals("existing", payload.entries().get(0).name());
        assertEquals("new", payload.entries().get(1).name());
    }

    @Test
    void shouldRejectInputPayloadWithoutEntriesBeforePromptingForPassword() throws Exception {
        Path vaultPath = initializedVaultPath();
        VaultEnvelope initialEnvelope = new VaultStorage(mapper).read(vaultPath);
        Path importPath = tempDir.resolve("import.json");
        mapper.writeValue(importPath.toFile(), new VaultPayload(null));
        AtomicBoolean passwordPrompted = new AtomicBoolean(false);

        CommandResult result = executeQuietly(new ImportCommand(
                mapper,
                vaultPath,
                prompt -> {
                    passwordPrompted.set(true);
                    return "test-master-password".toCharArray();
                }),
                "--input", importPath.toString());

        VaultEnvelope envelopeAfterFailedImport = new VaultStorage(mapper).read(vaultPath);

        assertEquals(1, result.exitCode());
        assertTrue(result.stderr().contains("payload entries are missing"));
        assertFalse(passwordPrompted.get());
        assertEquals(initialEnvelope, envelopeAfterFailedImport);
    }

    @Test
    void shouldRejectWrongMasterPasswordWithoutChangingVault() throws Exception {
        Path vaultPath = initializedVaultPath();
        VaultEnvelope initialEnvelope = new VaultStorage(mapper).read(vaultPath);
        Path importPath = tempDir.resolve("import.json");
        Instant now = Instant.now();
        VaultEntry entry = new VaultEntry("id-123", "imported", "user", "pass", null, null, now, now);
        mapper.writeValue(importPath.toFile(), new VaultPayload(List.of(entry)));

        CommandResult result = executeQuietly(new ImportCommand(
                mapper,
                vaultPath,
                prompt -> "wrong-password".toCharArray()),
                "--input", importPath.toString());

        VaultEnvelope envelopeAfterFailedImport = new VaultStorage(mapper).read(vaultPath);

        assertEquals(1, result.exitCode());
        assertTrue(result.stderr().contains("Vault unlock failed."));
        assertEquals(initialEnvelope, envelopeAfterFailedImport);
    }

    @Test
    void shouldReturnOneWhenInputFileDoesNotExistWithoutPromptingForPassword() throws Exception {
        Path vaultPath = initializedVaultPath();
        AtomicBoolean passwordPrompted = new AtomicBoolean(false);

        CommandResult result = executeQuietly(new ImportCommand(
                mapper,
                vaultPath,
                prompt -> {
                    passwordPrompted.set(true);
                    return "test-master-password".toCharArray();
                }),
                "--input", tempDir.resolve("missing.json").toString());

        assertEquals(1, result.exitCode());
        assertFalse(passwordPrompted.get());
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
        VaultEnvelope savedEnvelope = new pl.wiktor.passwordmanager.vault.VaultSaveService()
                .save(initialEnvelope, new VaultPayload(entries), "test-master-password".toCharArray(), mapper);
        new VaultStorage(mapper).writeNew(vaultPath, savedEnvelope);
        return vaultPath;
    }

    private CommandResult executeQuietly(ImportCommand command, String... args) {
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
