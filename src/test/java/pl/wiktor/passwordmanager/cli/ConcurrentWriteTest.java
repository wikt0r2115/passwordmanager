package pl.wiktor.passwordmanager.cli;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

import picocli.CommandLine;
import pl.wiktor.passwordmanager.io.ObjectMapperFactory;
import pl.wiktor.passwordmanager.io.VaultStorage;
import pl.wiktor.passwordmanager.vault.VaultUnlockService;
import pl.wiktor.passwordmanager.vault.VaultInitializationService;

class ConcurrentWriteTest {
    @TempDir Path tempDir;

    @Test
    void secondWriterFailsWithoutChangingVault() throws Exception {
        ObjectMapper mapper = ObjectMapperFactory.create();
        Path vault = tempDir.resolve("vault.json");
        new VaultStorage(mapper).writeNew(vault,
                new VaultInitializationService().init("synthetic-master".toCharArray(), mapper));
        byte[] before = Files.readAllBytes(vault);
        CountDownLatch firstPrompt = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CompletableFuture<Integer> first = CompletableFuture.supplyAsync(() ->
                new CommandLine(new AddCommand(mapper, vault, prompt -> {
                    firstPrompt.countDown();
                    try {
                        if (!releaseFirst.await(10, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("Timed out waiting for second writer");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(e);
                    }
                    return "synthetic-master".toCharArray();
                })).execute("--name", "first", "--username", "user", "--password", "synthetic-secret"));
        assertTrue(firstPrompt.await(5, TimeUnit.SECONDS));
        PrintStream previous = System.err;
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        try {
            System.setErr(new PrintStream(errors));
            int code = new CommandLine(new AddCommand(mapper, vault,
                    prompt -> "synthetic-master".toCharArray()))
                    .execute("--name", "second", "--username", "user", "--password", "synthetic-secret");
            assertEquals(1, code);
            assertArrayEquals(before, Files.readAllBytes(vault));
        } finally {
            System.setErr(previous);
            releaseFirst.countDown();
        }
        assertEquals(0, first.get(10, TimeUnit.SECONDS));
        assertTrue(errors.toString().contains("Vault could not be locked"));
        var payload = new VaultUnlockService().unlock(new VaultStorage(mapper).read(vault),
                "synthetic-master".toCharArray(), mapper);
        assertEquals(1, payload.entries().size());
        assertEquals("first", payload.entries().getFirst().name());
    }
}
