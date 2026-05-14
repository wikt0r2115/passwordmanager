package pl.wiktor.passwordmanager.error;

public class VaultValidationException extends RuntimeException {
    public VaultValidationException(String message) {
        super(message);
    }
}
