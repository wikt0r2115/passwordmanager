package pl.wiktor.passwordmanager.io;

import java.nio.file.Path;

public class VaultPathResolver {
    private static Path home = Path.of(System.getProperty("user.home"));
    private static Path defaultVaultPath = home.resolve(".passwordmanager").resolve("vault.json");
    
    public static Path getDefaultVaultPath(){
        return defaultVaultPath;
    }
    
}
