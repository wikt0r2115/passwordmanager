package pl.wiktor.passwordmanager.model;

import java.time.Instant;

public record VaultEnvelope(
        String format,
        int version,
        Instant createdAt,
        Instant updatedAt,
        KdfParams kdf,
        CipherParams cipher,
        String payload // Base64(ciphertext + tag)
) {
}
