package pl.wiktor.passwordmanager.model;

import java.time.Instant;

public record VaultEntry(
    String id,
    String name,
    String username,
    String password,
    String url,
    String notes,
    Instant createdAt,
    Instant updatedAt
) {

}
