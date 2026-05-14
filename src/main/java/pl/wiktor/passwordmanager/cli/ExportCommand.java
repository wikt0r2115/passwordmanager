package pl.wiktor.passwordmanager.cli;

import java.io.Console;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.Callable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import pl.wiktor.passwordmanager.error.CryptoException;
import pl.wiktor.passwordmanager.error.VaultValidationException;
import pl.wiktor.passwordmanager.io.ObjectMapperFactory;
import pl.wiktor.passwordmanager.io.VaultPathResolver;
import pl.wiktor.passwordmanager.io.VaultStorage;
import pl.wiktor.passwordmanager.model.VaultEnvelope;
import pl.wiktor.passwordmanager.model.VaultPayload;
import pl.wiktor.passwordmanager.vault.VaultUnlockService;

@Command(name = "export", description = "Exports all entries to a plaintext JSON file")
public class ExportCommand implements Callable<Integer> {
    interface PasswordReader {
        char[] readPassword(String prompt);
    }

    @Option(names = "--output", required = true, description = "Path to the destination JSON file")
    private Path outputPath;

    private final ObjectMapper mapper;
    private final Path vaultPath;
    private final PasswordReader passwordReader;

    public ExportCommand() {
        this(ObjectMapperFactory.create(), VaultPathResolver.getDefaultVaultPath(), null);
    }

    ExportCommand(ObjectMapper mapper, Path vaultPath, PasswordReader passwordReader) {
        this.mapper = mapper;
        this.vaultPath = vaultPath;
        this.passwordReader = passwordReader;
    }

    @Override
    public Integer call() {
        if (!Files.exists(vaultPath)) {
            System.err.println("Vault does not exist: " + vaultPath);
            return 2;
        }

        VaultEnvelope envelope;
        try {
            VaultStorage vaultStorage = new VaultStorage(mapper);
            envelope = vaultStorage.read(vaultPath);
        } catch (IOException e) {
            System.err.println("Vault could not be read: " + e.getMessage());
            return 1;
        }

        char[] masterPassword;
        try {
            masterPassword = readMasterPassword();
        } catch (IllegalStateException e) {
            System.err.println(e.getMessage());
            return 1;
        }

        if (masterPassword == null) {
            System.err.println("Master password was not provided.");
            return 1;
        }

        try {
            VaultUnlockService vaultUnlockService = new VaultUnlockService();
            VaultPayload vaultPayload;

            try {
                vaultPayload = vaultUnlockService.unlock(envelope, masterPassword, mapper);
            } catch (CryptoException e) {
                System.err.println("Vault unlock failed.");
                return 1;
            } catch (VaultValidationException e) {
                System.err.println("Invalid vault file: " + e.getMessage());
                return 1;
            } catch (IOException e) {
                System.err.println("Vault payload could not be read: " + e.getMessage());
                return 1;
            }

            try {
                // Use a copy of mapper with indentation for better readability of the export
                ObjectMapper exportMapper = mapper.copy().enable(SerializationFeature.INDENT_OUTPUT);
                exportMapper.writeValue(outputPath.toFile(), vaultPayload);
                System.out.println("Export successful: " + outputPath);
                System.out.println("WARNING: The exported file is plaintext and should be handled securely.");
                return 0;
            } catch (IOException e) {
                System.err.println("Failed to write export file: " + e.getMessage());
                return 1;
            }
        } finally {
            if (masterPassword != null) {
                Arrays.fill(masterPassword, '\0');
            }
        }
    }

    private char[] readMasterPassword() {
        if (passwordReader != null) {
            return passwordReader.readPassword("Type master password: ");
        }

        Console console = System.console();
        if (console == null) {
            throw new IllegalStateException("Console is unavailable. Run this command from a terminal.");
        }
        return console.readPassword("Type master password: ");
    }
}
