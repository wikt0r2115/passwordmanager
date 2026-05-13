package pl.wiktor.passwordmanager.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.Test;

import picocli.CommandLine;

public class GenerateCommandTest {

    @Test
    void shouldGenerateDefaultPassword() {
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));

        int exitCode = new CommandLine(new GenerateCommand()).execute();

        assertEquals(0, exitCode);
        String output = outContent.toString().trim();
        assertEquals(16, output.length());
        
        // Verify default categories presence (statistically very likely with length 16 and guarantee 2)
        assertTrue(countMatches(output, "abcdefghijklmnopqrstuvwxyz") >= 2);
        assertTrue(countMatches(output, "ABCDEFGHIJKLMNOPQRSTUVWXYZ") >= 2);
        assertTrue(countMatches(output, "0123456789") >= 2);
        assertTrue(countMatches(output, "!@#$%^&*()-_=+[]{}|;:,.<>?") >= 2);
    }

    @Test
    void shouldGeneratePasswordWithSpecifiedLength() {
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));

        int exitCode = new CommandLine(new GenerateCommand()).execute("-l", "20");

        assertEquals(0, exitCode);
        String output = outContent.toString().trim();
        assertEquals(20, output.length());
    }

    @Test
    void shouldGenerateSimplePassword() {
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));

        int exitCode = new CommandLine(new GenerateCommand()).execute(
            "--no-uppercase", "--no-digits", "--no-symbols"
        );

        assertEquals(0, exitCode);
        String output = outContent.toString().trim();
        assertEquals(16, output.length());
        assertTrue(countMatches(output, "abcdefghijklmnopqrstuvwxyz") == 16);
    }

    @Test
    void shouldReturnErrorForShortLength() {
        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errContent));

        int exitCode = new CommandLine(new GenerateCommand()).execute("-l", "5");

        assertEquals(1, exitCode);
        assertTrue(errContent.toString().contains("Password length must be at least 8"));
    }

    private int countMatches(String password, String charset) {
        int count = 0;
        for (char c : password.toCharArray()) {
            if (charset.indexOf(c) != -1) {
                count++;
            }
        }
        return count;
    }
}
