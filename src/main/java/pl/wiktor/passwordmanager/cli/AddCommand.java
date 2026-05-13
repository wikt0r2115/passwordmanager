package pl.wiktor.passwordmanager.cli;

import java.io.Console;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.Callable;

import com.fasterxml.jackson.databind.ObjectMapper;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import pl.wiktor.passwordmanager.error.CryptoException;
import pl.wiktor.passwordmanager.io.ObjectMapperFactory;
import pl.wiktor.passwordmanager.io.VaultPathResolver;
import pl.wiktor.passwordmanager.io.VaultStorage;
import pl.wiktor.passwordmanager.model.VaultEntry;
import pl.wiktor.passwordmanager.model.VaultEnvelope;
import pl.wiktor.passwordmanager.model.VaultPayload;
import pl.wiktor.passwordmanager.vault.VaultEntryService;
import pl.wiktor.passwordmanager.vault.VaultSaveService;
import pl.wiktor.passwordmanager.vault.VaultUnlockService;

@Command(name = "add", description = "Adds a new password entry")
public class AddCommand implements Callable<Integer> {
    interface PasswordReader {
        char[] readPassword(String prompt);
    }

    @Option(names = "--name", required = true, description = "Entry name")
    private String name;
    @Option(names = "--username", required = true, description = "Entry username")
    private String username;
    @Option(names = "--password", description = "Entry password")
    private String password;
    @Option(names = "--url", description = "Entry URL")
    private String url;
    @Option(names = "--notes", description = "Entry notes")
    private String notes;

    @Option(names = "--generate", description = "Generate a random password instead of providing one")
    private boolean generate;
    @Option(names = {"-l", "--length"}, description = "Generated password length (minimum 8)", defaultValue = "16")
    private int length;
    @Option(names = "--no-uppercase", description = "Do not use uppercase characters in generated password", negatable = true)
    private boolean useUppercase = true;
    @Option(names = "--no-digits", description = "Do not use digits in generated password", negatable = true)
    private boolean useDigits = true;
    @Option(names = "--no-symbols", description = "Do not use symbols in generated password", negatable = true)
    private boolean useSymbols = true;

    private final ObjectMapper mapper;
    private final Path vaultPath;
    private final PasswordReader passwordReader;

    public AddCommand() {
        this(ObjectMapperFactory.create(), VaultPathResolver.getDefaultVaultPath(), null);
    }

    AddCommand(ObjectMapper mapper, Path vaultPath, PasswordReader passwordReader) {
        this.mapper = mapper;
        this.vaultPath = vaultPath;
        this.passwordReader = passwordReader;
    }

    @Override
    public Integer call() {
        if (password == null && !generate) {
            System.err.println("Error: Either --password or --generate must be provided.");
            return 1;
        }
        if (password != null && generate) {
            System.err.println("Error: Please provide either --password or --generate, not both.");
            return 1;
        }

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
                vaultPayload = vaultUnlockService.unlock(
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

            String effectivePassword = password;
            if (generate) {
                try {
                    effectivePassword = new pl.wiktor.passwordmanager.password.PasswordGenerator()
                            .generate(length, useUppercase, useDigits, useSymbols);
                    System.out.println("Generated password: " + effectivePassword);
                } catch (IllegalArgumentException e) {
                    System.err.println("Password generation failed: " + e.getMessage());
                    return 1;
                }
            }

            Instant now = Instant.now();
            VaultEntry entry = new VaultEntry(
                    UUID.randomUUID().toString(),
                    name,
                    username,
                    effectivePassword,
                    url,
                    notes,
                    now,
                    now);

            VaultPayload updatedPayload = new VaultEntryService().addEntry(vaultPayload, entry);

            VaultEnvelope newEnvelope = new VaultSaveService()
                    .save(envelope, updatedPayload, masterPassword, mapper);

            new VaultStorage(mapper).writeReplace(vaultPath, newEnvelope);
            System.out.println("Entry added: " + name);
            return 0;
        } catch (IOException e) {
            System.err.println("Vault could not be written: " + e.getMessage());
            return 1;
        } catch (RuntimeException e) {
            System.err.println("Entry could not be added: " + e.getMessage());
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
