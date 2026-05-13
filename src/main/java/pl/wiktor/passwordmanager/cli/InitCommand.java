package pl.wiktor.passwordmanager.cli;

import java.io.Console;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.Callable;

import com.fasterxml.jackson.databind.ObjectMapper;

import picocli.CommandLine.Command;
import pl.wiktor.passwordmanager.io.ObjectMapperFactory;
import pl.wiktor.passwordmanager.io.VaultPathResolver;
import pl.wiktor.passwordmanager.io.VaultStorage;
import pl.wiktor.passwordmanager.model.VaultEnvelope;
import pl.wiktor.passwordmanager.vault.VaultInitializationService;

@Command(name = "init", description = "Initializes a new password vault")
public class InitCommand implements Callable<Integer> {
    interface PasswordReader {
        char[] readPassword(String prompt);
    }

    private final ObjectMapper mapper;
    private final Path vaultPath;
    private final PasswordReader passwordReader;

    public InitCommand() {
        this(ObjectMapperFactory.create(), VaultPathResolver.getDefaultVaultPath(), null);
    }

    InitCommand(ObjectMapper mapper, Path vaultPath, PasswordReader passwordReader) {
        this.mapper = mapper;
        this.vaultPath = vaultPath;
        this.passwordReader = passwordReader;
    }

    @Override
    public Integer call() {
        if (Files.exists(vaultPath)) {
            System.err.println("Vault already exists: " + vaultPath);
            return 2;
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

        VaultInitializationService vaultInitializationService = new VaultInitializationService();

        try {
            VaultEnvelope envelope = vaultInitializationService.init(masterPassword, mapper);
            VaultStorage vaultStorage = new VaultStorage(mapper);

            System.out.println("Initializing vault...");
            vaultStorage.writeNew(vaultPath, envelope);
            System.out.println("Vault created: " + vaultPath);
            return 0;
        } catch (FileAlreadyExistsException e) {
            System.err.println("Vault already exists: " + vaultPath);
            return 2;
        } catch (IOException e) {
            System.err.println("Vault could not be written: " + e.getMessage());
            return 1;
        } catch (RuntimeException e) {
            System.err.println("Vault initialization failed: " + e.getMessage());
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
