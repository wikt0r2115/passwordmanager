package pl.wiktor.passwordmanager.io;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.fasterxml.jackson.databind.ObjectMapper;

import pl.wiktor.passwordmanager.model.VaultEnvelope;

public class VaultStorage {
    private final ObjectMapper mapper;

    public VaultStorage(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void writeNew(Path vaultPath, VaultEnvelope envelope) throws IOException {
        Path absoluteVaultPath = vaultPath.toAbsolutePath();
        Path vaultDirectory = absoluteVaultPath.getParent();

        Files.createDirectories(vaultDirectory);

        if (Files.exists(absoluteVaultPath)) {
            throw new FileAlreadyExistsException(absoluteVaultPath.toString());
        }

        Path tempFile = Files.createTempFile(vaultDirectory, absoluteVaultPath.getFileName().toString(), ".tmp");
        try {
            mapper.writeValue(tempFile.toFile(), envelope);
            Files.move(tempFile, absoluteVaultPath, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(tempFile);
            throw e;
        }
    }

    public VaultEnvelope read(Path vaultPath) throws IOException {
        Path absoluteVaultPath = vaultPath.toAbsolutePath();

        if (!Files.isReadable(absoluteVaultPath))
            throw new IOException("Vault file is not readable " + absoluteVaultPath);

        return mapper.readValue(absoluteVaultPath.toFile(), VaultEnvelope.class);
    }

    public void writeReplace(Path vaultPath, VaultEnvelope vaultEnvelope) throws IOException {
        Path absoluteVaultPath = vaultPath.toAbsolutePath();
        Path vaultDirectory = absoluteVaultPath.getParent();

        if(!Files.exists(absoluteVaultPath))
            throw new NoSuchFileException("Vault is not initialized at " + absoluteVaultPath.toString());

        Path tempFile = Files.createTempFile(vaultDirectory, absoluteVaultPath.getFileName().toString(), ".tmp");
        try {
            mapper.writeValue(tempFile.toFile(), vaultEnvelope);
            Path backupPath = absoluteVaultPath.resolveSibling(absoluteVaultPath.getFileName().toString() + ".bak");
            Path backupTemp = Files.createTempFile(vaultDirectory, absoluteVaultPath.getFileName().toString(), ".bak.tmp");
            try {
                Files.copy(absoluteVaultPath, backupTemp, StandardCopyOption.REPLACE_EXISTING);
                Files.move(backupTemp, backupPath, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } finally {
                Files.deleteIfExists(backupTemp);
            }
            moveReplacement(tempFile, absoluteVaultPath);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    protected void moveReplacement(Path source, Path destination) throws IOException {
        Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
