package pl.wiktor.passwordmanager.cli;

import java.io.Console;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;

import com.fasterxml.jackson.databind.ObjectMapper;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import pl.wiktor.passwordmanager.error.CryptoException;
import pl.wiktor.passwordmanager.error.VaultValidationException;
import pl.wiktor.passwordmanager.io.ObjectMapperFactory;
import pl.wiktor.passwordmanager.io.VaultPathResolver;
import pl.wiktor.passwordmanager.io.VaultStorage;
import pl.wiktor.passwordmanager.model.VaultEntry;
import pl.wiktor.passwordmanager.model.VaultEnvelope;
import pl.wiktor.passwordmanager.model.VaultPayload;
import pl.wiktor.passwordmanager.password.PasswordGenerator;
import pl.wiktor.passwordmanager.vault.VaultEntryService;
import pl.wiktor.passwordmanager.vault.VaultSaveService;
import pl.wiktor.passwordmanager.vault.VaultUnlockService;

@Command(name = "update", description = "Updates an existing password entry")
public class UpdateCommand implements Callable<Integer> {
    interface PasswordReader {
        char[] readPassword(String prompt);
    }

    @Option(names = "--name", required = true, description = "Name of the entry to update")
    private String name;

    @Option(names = "--new-name", description = "New name for the entry")
    private String newName;
    @Option(names = "--username", description = "New username")
    private String username;
    @Option(names = "--password", description = "New password (visible in shell history; prefer --prompt-password)")
    private String password;
    @Option(names = "--prompt-password", description = "Prompt for the new password without echo")
    private boolean promptPassword;
    @Option(names = "--url", description = "New URL")
    private String url;
    @Option(names = "--notes", description = "New notes")
    private String notes;

    @Option(names = "--generate", description = "Generate a new random password")
    private boolean generate;
    @Option(names = {"-l", "--length"}, description = "Generated password length", defaultValue = "16")
    private int length;
    @Option(names = "--no-uppercase", description = "Do not use uppercase characters", negatable = true)
    private boolean useUppercase = true;
    @Option(names = "--no-digits", description = "Do not use digits", negatable = true)
    private boolean useDigits = true;
    @Option(names = "--no-symbols", description = "Do not use symbols", negatable = true)
    private boolean useSymbols = true;

    private final ObjectMapper mapper;
    private final Path vaultPath;
    private final PasswordReader passwordReader;

    public UpdateCommand() {
        this(ObjectMapperFactory.create(), VaultPathResolver.getDefaultVaultPath(), null);
    }

    UpdateCommand(ObjectMapper mapper, Path vaultPath, PasswordReader passwordReader) {
        this.mapper = mapper;
        this.vaultPath = vaultPath;
        this.passwordReader = passwordReader;
    }

    @Override
    public Integer call() {
        if (newName == null && username == null && password == null && url == null && notes == null && !generate
                && !promptPassword) {
            System.err.println("Error: Nothing to update. Provide at least one field to change, --generate or --prompt-password.");
            return 1;
        }

        int passwordSources = 0;
        if (password != null) {
            passwordSources++;
        }
        if (generate) {
            passwordSources++;
        }
        if (promptPassword) {
            passwordSources++;
        }
        if (passwordSources > 1) {
            System.err.println("Error: Please provide only one password source: --password, --generate or --prompt-password.");
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
            } catch (VaultValidationException e) {
                System.err.println("Invalid vault file: " + e.getMessage());
                return 1;
            } catch (IOException e) {
                System.err.println("Vault payload could not be read: " + e.getMessage());
                return 1;
            }

            VaultEntry existingEntry = null;
            for (VaultEntry e : vaultPayload.entries()) {
                if (name.equals(e.name())) {
                    existingEntry = e;
                    break;
                }
            }

            if (existingEntry == null) {
                System.err.println("Entry not found: " + name);
                return 2;
            }

            String effectivePassword = password != null ? password : existingEntry.password();
            if (generate) {
                try {
                    effectivePassword = new PasswordGenerator().generate(length, useUppercase, useDigits, useSymbols);
                    System.out.println("Generated new password: " + effectivePassword);
                } catch (IllegalArgumentException e) {
                    System.err.println("Password generation failed: " + e.getMessage());
                    return 1;
                }
            }
            if (promptPassword) {
                char[] entryPassword = null;
                try {
                    entryPassword = readEntryPassword();
                } catch (IllegalStateException e) {
                    System.err.println(e.getMessage());
                    return 1;
                }
                if (entryPassword == null) {
                    System.err.println("Entry password was not provided.");
                    return 1;
                }
                try {
                    effectivePassword = new String(entryPassword);
                } finally {
                    Arrays.fill(entryPassword, '\0');
                }
            }

            Instant now = Instant.now();
            VaultEntry updatedEntry = new VaultEntry(
                    existingEntry.id(),
                    newName != null ? newName : existingEntry.name(),
                    username != null ? username : existingEntry.username(),
                    effectivePassword,
                    url != null ? url : existingEntry.url(),
                    notes != null ? notes : existingEntry.notes(),
                    existingEntry.createdAt(),
                    now);

            VaultPayload updatedPayload = new VaultEntryService().updateEntry(vaultPayload, name, updatedEntry);

            VaultEnvelope newEnvelope = new VaultSaveService()
                    .save(envelope, updatedPayload, masterPassword, mapper);

            new VaultStorage(mapper).writeReplace(vaultPath, newEnvelope);
            System.out.println("Entry updated: " + name + (newName != null ? " -> " + newName : ""));
            return 0;
        } catch (IOException e) {
            System.err.println("Vault could not be written: " + e.getMessage());
            return 1;
        } catch (NoSuchElementException e) {
            System.err.println("Entry not found: " + name);
            return 2;
        } catch (RuntimeException e) {
            System.err.println("Entry could not be updated: " + e.getMessage());
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

    private char[] readEntryPassword() {
        if (passwordReader != null) {
            return passwordReader.readPassword("Type new entry password: ");
        }

        Console console = System.console();
        if (console == null) {
            throw new IllegalStateException("Console is unavailable. Run this command from a terminal.");
        }
        return console.readPassword("Type new entry password: ");
    }
}
