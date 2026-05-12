package pl.wiktor.passwordmanager.model;

import java.util.List;

public record VaultPayload(
    List<VaultEntry> entries
) {

}
