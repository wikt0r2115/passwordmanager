package pl.wiktor.passwordmanager.cli;

import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import pl.wiktor.passwordmanager.password.PasswordGenerator;

@Command(name = "generate", description = "Generates a strong random password")
public class GenerateCommand implements Callable<Integer> {

    @Option(names = {"-l", "--length"}, description = "Password length (minimum 8)", defaultValue = "16")
    private int length;

    @Option(names = "--no-uppercase", description = "Do not use uppercase characters", negatable = true)
    private boolean useUppercase = true;

    @Option(names = "--no-digits", description = "Do not use digits", negatable = true)
    private boolean useDigits = true;

    @Option(names = "--no-symbols", description = "Do not use symbols", negatable = true)
    private boolean useSymbols = true;

    @Override
    public Integer call() {
        if (length < 8) {
            System.err.println("Password length must be at least 8.");
            return 1;
        }

        PasswordGenerator generator = new PasswordGenerator();
        try {
            String password = generator.generate(length, useUppercase, useDigits, useSymbols);
            System.out.println(password);
            return 0;
        } catch (IllegalArgumentException e) {
            System.err.println("Password generation failed: " + e.getMessage());
            return 1;
        }
    }
}
