package pl.wiktor.passwordmanager.vault;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import pl.wiktor.passwordmanager.model.VaultEntry;
import pl.wiktor.passwordmanager.model.VaultPayload;

public class VaultEntryService {
    public VaultPayload addEntry(VaultPayload payload, VaultEntry entry) {
        if (payload == null) {
            throw new IllegalArgumentException("Vault payload cannot be null");
        }
        if (payload.entries() == null) {
            throw new IllegalArgumentException("Vault entries cannot be null");
        }
        if (entry == null) {
            throw new IllegalArgumentException("Vault entry cannot be null");
        }
    
        List<VaultEntry> entries = new ArrayList<>(payload.entries());
        entries.add(entry);
        return new VaultPayload(List.copyOf(entries));
    }

    public VaultPayload removeByName(VaultPayload payload, String name) {
        if (payload == null) {
            throw new IllegalArgumentException("Vault payload cannot be null");
        }
        if (payload.entries() == null) {
            throw new IllegalArgumentException("Vault entries cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Entry name cannot be null or blank");
        }
        
        List<VaultEntry> entries = new ArrayList<>(payload.entries());
        for (int i = 0; i < entries.size(); i++) {
            VaultEntry entry = entries.get(i);
            if (name.equals(entry.name())) {
                entries.remove(i);
                return new VaultPayload(List.copyOf(entries));
            }
        }

        throw new NoSuchElementException("Entry not found: " + name);
    }

    public VaultPayload updateEntry(VaultPayload payload, String oldName, VaultEntry updatedEntry) {
        if (payload == null || payload.entries() == null) {
            throw new IllegalArgumentException("Vault payload and entries cannot be null");
        }
        if (oldName == null || oldName.isBlank()) {
            throw new IllegalArgumentException("Old entry name cannot be null or blank");
        }
        if (updatedEntry == null) {
            throw new IllegalArgumentException("Updated entry cannot be null");
        }

        List<VaultEntry> entries = new ArrayList<>(payload.entries());
        for (int i = 0; i < entries.size(); i++) {
            if (oldName.equals(entries.get(i).name())) {
                entries.set(i, updatedEntry);
                return new VaultPayload(List.copyOf(entries));
            }
        }

        throw new NoSuchElementException("Entry not found: " + oldName);
    }

    public VaultPayload merge(VaultPayload target, VaultPayload source) {
        if (target == null || source == null) {
            throw new IllegalArgumentException("Target and source payloads cannot be null");
        }

        List<VaultEntry> mergedEntries = new ArrayList<>(target.entries());
        for (VaultEntry sourceEntry : source.entries()) {
            boolean exists = mergedEntries.stream()
                    .anyMatch(e -> e.id().equals(sourceEntry.id()));
            if (!exists) {
                mergedEntries.add(sourceEntry);
            }
        }
        return new VaultPayload(List.copyOf(mergedEntries));
    }
}
