package pl.wiktor.passwordmanager.cli;

import java.io.Console;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import com.fasterxml.jackson.databind.ObjectMapper;

import picocli.CommandLine.Command;
import pl.wiktor.passwordmanager.io.ObjectMapperFactory;
import pl.wiktor.passwordmanager.io.VaultPathResolver;
import pl.wiktor.passwordmanager.io.VaultStorage;
import pl.wiktor.passwordmanager.model.VaultEnvelope;
import pl.wiktor.passwordmanager.vault.VaultInitializationService;

@Command(name = "init", description = "initializes new password vault")
public class InitCommand implements Callable<Integer> {
    @Override
    public Integer call() {
        ObjectMapper mapper = ObjectMapperFactory.create();

        Path defaultVaultPath = VaultPathResolver.getDefaultVaultPath();
        if (Files.exists(defaultVaultPath)) {
            System.err.println("Vault already exists: " + defaultVaultPath);
            return 2;
        }

        Console console = System.console();

        if (console == null) {
            System.err.println("Console is unavailable. Run this command from a terminal.");
            return 1;
        }

        char[] masterPassword = console.readPassword("Type master password: ");
        VaultInitializationService vaultInitializationService = new VaultInitializationService();

        try {
            VaultEnvelope envelope = vaultInitializationService.init(masterPassword, mapper);
            VaultStorage vaultStorage = new VaultStorage(mapper);

            System.out.println("Initializing vault...");
            vaultStorage.writeNew(defaultVaultPath, envelope);
            System.out.println("Vault created: " + defaultVaultPath);
            return 0;
        } catch (FileAlreadyExistsException e) {
            System.err.println("Vault already exists: " + defaultVaultPath);
            return 2;
        } catch (IOException e) {
            System.err.println("Vault could not be written: " + e.getMessage());
            return 1;
        } catch (RuntimeException e) {
            System.err.println("Vault initialization failed: " + e.getMessage());
            return 1;
        }
    }
}
