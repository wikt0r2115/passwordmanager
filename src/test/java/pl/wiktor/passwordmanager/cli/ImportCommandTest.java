package pl.wiktor.passwordmanager.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
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

        int exitCode = executeQuietly(new ImportCommand(
                mapper,
                vaultPath,
                prompt -> "test-master-password".toCharArray()),
                "--input", importPath.toString());

        assertEquals(0, exitCode);
        
        VaultEnvelope savedEnvelope = new VaultStorage(mapper).read(vaultPath);
        VaultPayload payload = new VaultUnlockService()
                .unlock(savedEnvelope, "test-master-password".toCharArray(), mapper);
        
        assertEquals(1, payload.entries().size());
        assertEquals("imported", payload.entries().getFirst().name());
        
        // Verify backup creation
        assertTrue(Files.exists(vaultPath.resolveSibling("vault.json.bak")));
    }

    private Path initializedVaultPath() throws Exception {
        Path vaultPath = tempDir.resolve("vault.json");
        VaultEnvelope envelope = new VaultInitializationService()
                .init("test-master-password".toCharArray(), mapper);
        new VaultStorage(mapper).writeNew(vaultPath, envelope);
        return vaultPath;
    }

    private int executeQuietly(ImportCommand command, String... args) {
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
