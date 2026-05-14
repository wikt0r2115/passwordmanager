package pl.wiktor.passwordmanager.cli;

import java.io.Console;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.Callable;

import com.fasterxml.jackson.databind.ObjectMapper;

import picocli.CommandLine.Command;
import pl.wiktor.passwordmanager.error.CryptoException;
import pl.wiktor.passwordmanager.error.VaultValidationException;
import pl.wiktor.passwordmanager.io.ObjectMapperFactory;
import pl.wiktor.passwordmanager.io.VaultPathResolver;
import pl.wiktor.passwordmanager.io.VaultStorage;
import pl.wiktor.passwordmanager.model.VaultEntry;
import pl.wiktor.passwordmanager.model.VaultEnvelope;
import pl.wiktor.passwordmanager.model.VaultPayload;
import pl.wiktor.passwordmanager.vault.VaultUnlockService;

@Command(name = "list", description = "Lists password entries")
public class ListCommand implements Callable<Integer> {
    interface PasswordReader {
        char[] readPassword(String prompt);
    }

    private final ObjectMapper mapper;
    private final Path vaultPath;
    private final PasswordReader passwordReader;

    public ListCommand() {
        this(ObjectMapperFactory.create(), VaultPathResolver.getDefaultVaultPath(), null);
    }

    ListCommand(ObjectMapper mapper, Path vaultPath, PasswordReader passwordReader) {
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
        } finally {
            if (masterPassword != null) {
                Arrays.fill(masterPassword, '\0');
            }
        }

        if (vaultPayload.entries().isEmpty()) {
            System.out.println("No entries found.");
            return 0;
        }

        for (VaultEntry entry : vaultPayload.entries()) {
            System.out.println(entry.name() + "\t" + entry.username() + "\t" + entry.url());
        }

        return 0;
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
