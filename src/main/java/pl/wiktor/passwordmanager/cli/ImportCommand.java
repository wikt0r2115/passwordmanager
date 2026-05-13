package pl.wiktor.passwordmanager.cli;

import java.io.Console;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.Callable;

import com.fasterxml.jackson.databind.ObjectMapper;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import pl.wiktor.passwordmanager.error.CryptoException;
import pl.wiktor.passwordmanager.io.ObjectMapperFactory;
import pl.wiktor.passwordmanager.io.VaultPathResolver;
import pl.wiktor.passwordmanager.io.VaultStorage;
import pl.wiktor.passwordmanager.model.VaultEnvelope;
import pl.wiktor.passwordmanager.model.VaultPayload;
import pl.wiktor.passwordmanager.vault.VaultEntryService;
import pl.wiktor.passwordmanager.vault.VaultSaveService;
import pl.wiktor.passwordmanager.vault.VaultUnlockService;

@Command(name = "import", description = "Imports entries from a plaintext JSON file")
public class ImportCommand implements Callable<Integer> {
    interface PasswordReader {
        char[] readPassword(String prompt);
    }

    @Option(names = "--input", required = true, description = "Path to the source JSON file")
    private Path inputPath;

    private final ObjectMapper mapper;
    private final Path vaultPath;
    private final PasswordReader passwordReader;

    public ImportCommand() {
        this(ObjectMapperFactory.create(), VaultPathResolver.getDefaultVaultPath(), null);
    }

    @SuppressWarnings("unused")
    ImportCommand(ObjectMapper mapper, Path vaultPath, PasswordReader passwordReader) {
        this.mapper = mapper;
        this.vaultPath = vaultPath;
        this.passwordReader = passwordReader;
    }

    @Override
    public Integer call() {
        if (!Files.exists(inputPath)) {
            System.err.println("Input file does not exist: " + inputPath);
            return 1;
        }

        if (!Files.exists(vaultPath)) {
            System.err.println("Vault does not exist: " + vaultPath);
            return 2;
        }

        VaultPayload importedPayload;
        try {
            importedPayload = mapper.readValue(inputPath.toFile(), VaultPayload.class);
        } catch (IOException e) {
            System.err.println("Failed to read input file: " + e.getMessage());
            return 1;
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
            VaultPayload currentPayload;

            try {
                currentPayload = vaultUnlockService.unlock(
                        envelope,
                        Arrays.copyOf(masterPassword, masterPassword.length),
                        mapper);
            } catch (CryptoException e) {
                System.err.println("Vault unlock failed.");
                return 1;
            } catch (IOException e) {
                System.err.println("Vault payload could not be read: " + e.getMessage());
                return 1;
            }

            VaultPayload mergedPayload = new VaultEntryService().merge(currentPayload, importedPayload);

            VaultEnvelope newEnvelope = new VaultSaveService()
                    .save(envelope, mergedPayload, masterPassword, mapper);

            new VaultStorage(mapper).writeReplace(vaultPath, newEnvelope);
            System.out.println("Import successful. Imported " + importedPayload.entries().size() + " entries.");
            return 0;
        } catch (IOException e) {
            System.err.println("Vault could not be written: " + e.getMessage());
            return 1;
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
