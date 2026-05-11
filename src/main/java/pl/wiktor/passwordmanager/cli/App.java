package pl.wiktor.passwordmanager.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine;

@Command(name = "passwordmanager", mixinStandardHelpOptions = true, version = "0.1.0", subcommands = {
        InitCommand.class }, description = "Educational local password manager")
public class App implements Runnable {
    @Override
    public void run() {

    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new App()).execute(args);
        System.exit(exitCode);

    }
}
