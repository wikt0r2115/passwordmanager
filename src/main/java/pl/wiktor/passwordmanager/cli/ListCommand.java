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
import pl.wiktor.passwordmanager.model.VaultEntry;
import pl.wiktor.passwordmanager.model.VaultEnvelope;
import pl.wiktor.passwordmanager.model.VaultPayload;
import pl.wiktor.passwordmanager.vault.VaultUnlockService;

@Command(name = "list", description = "lists all")
public class ListCommand implements Callable<Integer> {
    @Override
    public Integer call() {
        ObjectMapper mapper = ObjectMapperFactory.create();
        Path defaultVaultPath = VaultPathResolver.getDefaultVaultPath();
        if (!Files.exists(defaultVaultPath)) {
            System.err.println("Vault does not exist: " + defaultVaultPath);
            return 2;
        }

        Console console = System.console();
        if (console == null) {
            System.err.println("Console is unavailable. Run this command from a terminal.");
            return 1;
        }

        VaultEnvelope envelope;
        try {
            VaultStorage vaultStorage = new VaultStorage(mapper);
            envelope = vaultStorage.read(defaultVaultPath);
        } catch (IOException e) {
            System.err.println("Vault could not be read: " + e.getMessage());
            return 1;
        }

        char[] masterPassword = console.readPassword("Type master password: ");
        VaultUnlockService vaultUnlockService = new VaultUnlockService();
        VaultPayload vaultPayload;
        try {
            vaultPayload = vaultUnlockService.unlock(envelope, masterPassword, mapper);
        } catch (CryptoException e) {
            System.err.println("Vault unlock failed.");
            return 1;
        } catch (IOException e) {
            System.err.println("Vault payload could not be read: " + e.getMessage());
            return 1;
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
}
