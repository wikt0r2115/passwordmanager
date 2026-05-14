package pl.wiktor.passwordmanager.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command(name = "passwordmanager", mixinStandardHelpOptions = true, version = "0.1.0", subcommands = {
        InitCommand.class,
        ListCommand.class,
        AddCommand.class,
        ShowCommand.class,
        RemoveCommand.class,
        UpdateCommand.class,
        ExportCommand.class,
        ImportCommand.class,
        GenerateCommand.class }, description = "Local password manager CLI")
public class App implements java.util.concurrent.Callable<Integer> {
    @Spec
    private CommandSpec spec;

    @Override
    public Integer call() {
        spec.commandLine().usage(System.out);
        return 1;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new App()).execute(args);
        System.exit(exitCode);

    }
}
