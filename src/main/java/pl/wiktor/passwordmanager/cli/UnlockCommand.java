package pl.wiktor.passwordmanager.cli;

import java.io.Console;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import com.fasterxml.jackson.databind.ObjectMapper;

import picocli.CommandLine.Command;
import pl.wiktor.passwordmanager.error.CryptoException;
import pl.wiktor.passwordmanager.io.ObjectMapperFactory;
import pl.wiktor.passwordmanager.io.VaultPathResolver;
import pl.wiktor.passwordmanager.io.VaultStorage;
import pl.wiktor.passwordmanager.model.VaultEnvelope;
import pl.wiktor.passwordmanager.model.VaultPayload;
import pl.wiktor.passwordmanager.vault.VaultUnlockService;

@Command(name = "unlock", description = "unlocks existing password vault")
public class UnlockCommand implements Callable<Integer> {
    @Override
    public Integer call() {
        ObjectMapper mapper = ObjectMapperFactory.create();
        Path defaultVaultPath = VaultPathResolver.getDefaultVaultPath();

        if (!Files.exists(defaultVaultPath)) {
            System.err.println("Vault does not exist: " + defaultVaultPath);
            return 2;
        }

        VaultEnvelope envelope;
        try {
            VaultStorage vaultStorage = new VaultStorage(mapper);
            envelope = vaultStorage.read(defaultVaultPath);
        } catch (IOException e) {
            System.err.println("Vault could not be read: " + e.getMessage());
            return 1;
        }

        Console console = System.console();
        if (console == null) {
            System.err.println("Console is unavailable. Run this command from a terminal.");
            return 1;
        }

        char[] masterPassword = console.readPassword("Type master password: ");
        VaultUnlockService vaultUnlockService = new VaultUnlockService();

        try {
            VaultPayload payload = vaultUnlockService.unlock(envelope, masterPassword, mapper);
            System.out.println("Vault unlocked: " + defaultVaultPath);
            System.out.println("Entries: " + payload.entries().size());
            return 0;
        } catch (CryptoException e) {
            System.err.println("Vault unlock failed.");
            return 1;
        } catch (IOException e) {
            System.err.println("Vault payload could not be read: " + e.getMessage());
            return 1;
        } catch (RuntimeException e) {
            System.err.println("Vault unlock failed: " + e.getMessage());
            return 1;
        }
    }
}
