package pl.wiktor.passwordmanager.io;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Serializes cooperating CLI writers across processes. Hold it from read through write. */
public final class VaultWriteLock implements AutoCloseable {
    private final FileChannel channel;
    private final FileLock lock;

    private VaultWriteLock(FileChannel channel, FileLock lock) {
        this.channel = channel;
        this.lock = lock;
    }

    public static VaultWriteLock acquire(Path vaultPath) throws IOException {
        Path absolute = vaultPath.toAbsolutePath();
        Files.createDirectories(absolute.getParent());
        Path lockPath = absolute.resolveSibling(absolute.getFileName() + ".lock");
        FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        try {
            FileLock lock = channel.tryLock();
            if (lock == null) {
                throw new IOException("Vault is in use by another writer: " + absolute);
            }
            return new VaultWriteLock(channel, lock);
        } catch (IOException | RuntimeException e) {
            channel.close();
            if (e instanceof OverlappingFileLockException) {
                throw new IOException("Vault is in use by another writer: " + absolute, e);
            }
            throw e;
        }
    }

    @Override
    public void close() throws IOException {
        try {
            lock.release();
        } finally {
            channel.close();
        }
    }
}
