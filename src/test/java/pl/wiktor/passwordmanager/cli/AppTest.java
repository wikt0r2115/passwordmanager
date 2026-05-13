package pl.wiktor.passwordmanager.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.Test;

import picocli.CommandLine;

class AppTest {
    @Test
    void shouldExposeOnlySupportedCommands() {
        CommandResult result = executeQuietly("--help");

        assertEquals(0, result.exitCode());
        assertTrue(result.stdout().contains("init"));
        assertTrue(result.stdout().contains("add"));
        assertTrue(result.stdout().contains("list"));
        assertTrue(result.stdout().contains("show"));
        assertTrue(result.stdout().contains("remove"));
        assertFalse(result.stdout().contains("unlock"));
    }

    private CommandResult executeQuietly(String... args) {
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(stdout));
            System.setErr(new PrintStream(stderr));
            int exitCode = new CommandLine(new App()).execute(args);
            return new CommandResult(exitCode, stdout.toString(), stderr.toString());
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

    private record CommandResult(int exitCode, String stdout, String stderr) {
    }
}
