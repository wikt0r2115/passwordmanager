package pl.wiktor.passwordmanager.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import pl.wiktor.passwordmanager.model.VaultEntry;
import pl.wiktor.passwordmanager.model.VaultPayload;

class VaultEntryServiceTest {
    @Test
    void shouldAddEntryToEmptyPayload() {
        VaultEntry entry = testEntry("entry-1", "github");
        VaultPayload payload = new VaultPayload(List.of());

        VaultPayload updatedPayload = new VaultEntryService().addEntry(payload, entry);

        assertEquals(List.of(entry), updatedPayload.entries());
        assertEquals(List.of(), payload.entries());
    }

    @Test
    void shouldAppendEntryToExistingPayload() {
        VaultEntry existingEntry = testEntry("entry-1", "github");
        VaultEntry newEntry = testEntry("entry-2", "email");
        VaultPayload payload = new VaultPayload(List.of(existingEntry));

        VaultPayload updatedPayload = new VaultEntryService().addEntry(payload, newEntry);

        assertEquals(List.of(existingEntry, newEntry), updatedPayload.entries());
        assertEquals(List.of(existingEntry), payload.entries());
    }

    @Test
    void shouldRejectNullPayload() {
        VaultEntry entry = testEntry("entry-1", "github");

        assertThrows(IllegalArgumentException.class, () -> new VaultEntryService().addEntry(null, entry));
    }

    @Test
    void shouldRejectNullEntries() {
        VaultEntry entry = testEntry("entry-1", "github");
        VaultPayload payload = new VaultPayload(null);

        assertThrows(IllegalArgumentException.class, () -> new VaultEntryService().addEntry(payload, entry));
    }

    @Test
    void shouldRejectNullEntry() {
        VaultPayload payload = new VaultPayload(List.of());

        assertThrows(IllegalArgumentException.class, () -> new VaultEntryService().addEntry(payload, null));
    }

    @Test
    void shouldRemoveEntryByName() {
        VaultEntry githubEntry = testEntry("entry-1", "github");
        VaultEntry emailEntry = testEntry("entry-2", "email");
        VaultPayload payload = new VaultPayload(List.of(githubEntry, emailEntry));

        VaultPayload updatedPayload = new VaultEntryService().removeByName(payload, "github");

        assertEquals(List.of(emailEntry), updatedPayload.entries());
    }

    @Test
    void shouldNotMutateOriginalPayloadWhenRemovingEntry() {
        VaultEntry githubEntry = testEntry("entry-1", "github");
        VaultEntry emailEntry = testEntry("entry-2", "email");
        VaultPayload payload = new VaultPayload(List.of(githubEntry, emailEntry));

        new VaultEntryService().removeByName(payload, "github");

        assertEquals(List.of(githubEntry, emailEntry), payload.entries());
    }

    @Test
    void shouldRemoveOnlyFirstMatchingEntryByName() {
        VaultEntry firstGithubEntry = testEntry("entry-1", "github");
        VaultEntry secondGithubEntry = testEntry("entry-2", "github");
        VaultPayload payload = new VaultPayload(List.of(firstGithubEntry, secondGithubEntry));

        VaultPayload updatedPayload = new VaultEntryService().removeByName(payload, "github");

        assertEquals(List.of(secondGithubEntry), updatedPayload.entries());
    }

    @Test
    void shouldRejectMissingEntryWhenRemovingByName() {
        VaultPayload payload = new VaultPayload(List.of(testEntry("entry-1", "github")));

        assertThrows(NoSuchElementException.class, () -> new VaultEntryService().removeByName(payload, "email"));
    }

    @Test
    void shouldRejectNullPayloadWhenRemovingByName() {
        assertThrows(IllegalArgumentException.class, () -> new VaultEntryService().removeByName(null, "github"));
    }

    @Test
    void shouldRejectNullEntriesWhenRemovingByName() {
        VaultPayload payload = new VaultPayload(null);

        assertThrows(IllegalArgumentException.class, () -> new VaultEntryService().removeByName(payload, "github"));
    }

    @Test
    void shouldRejectNullNameWhenRemovingByName() {
        VaultPayload payload = new VaultPayload(List.of(testEntry("entry-1", "github")));

        assertThrows(IllegalArgumentException.class, () -> new VaultEntryService().removeByName(payload, null));
    }

    @Test
    void shouldRejectBlankNameWhenRemovingByName() {
        VaultPayload payload = new VaultPayload(List.of(testEntry("entry-1", "github")));

        assertThrows(IllegalArgumentException.class, () -> new VaultEntryService().removeByName(payload, " "));
    }

    @Test
    void shouldUpdateEntry() {
        VaultEntry githubEntry = testEntry("entry-1", "github");
        VaultEntry emailEntry = testEntry("entry-2", "email");
        VaultPayload payload = new VaultPayload(List.of(githubEntry, emailEntry));
        
        VaultEntry updatedGithub = testEntry("entry-1", "github-personal");
        VaultPayload updatedPayload = new VaultEntryService().updateEntry(payload, "github", updatedGithub);

        assertEquals(2, updatedPayload.entries().size());
        assertEquals(updatedGithub, updatedPayload.entries().get(0));
        assertEquals(emailEntry, updatedPayload.entries().get(1));
    }

    @Test
    void shouldNotMutateOriginalPayloadWhenUpdatingEntry() {
        VaultEntry githubEntry = testEntry("entry-1", "github");
        VaultPayload payload = new VaultPayload(List.of(githubEntry));
        
        VaultEntry updatedGithub = testEntry("entry-1", "github-personal");
        new VaultEntryService().updateEntry(payload, "github", updatedGithub);

        assertEquals(githubEntry, payload.entries().get(0));
    }

    @Test
    void shouldRejectMissingEntryWhenUpdating() {
        VaultPayload payload = new VaultPayload(List.of(testEntry("entry-1", "github")));
        VaultEntry updatedEntry = testEntry("entry-2", "email");

        assertThrows(NoSuchElementException.class, () -> new VaultEntryService().updateEntry(payload, "email", updatedEntry));
    }

    @Test
    void shouldRejectInvalidArgumentsWhenUpdating() {
        VaultPayload payload = new VaultPayload(List.of(testEntry("entry-1", "github")));
        VaultEntry updatedEntry = testEntry("entry-1", "github");

        assertThrows(IllegalArgumentException.class, () -> new VaultEntryService().updateEntry(null, "github", updatedEntry));
        assertThrows(IllegalArgumentException.class, () -> new VaultEntryService().updateEntry(payload, null, updatedEntry));
        assertThrows(IllegalArgumentException.class, () -> new VaultEntryService().updateEntry(payload, "github", null));
    }

    @Test
    void shouldMergePayloads() {
        VaultEntry targetEntry = testEntry("id-1", "github");
        VaultEntry sourceEntry = testEntry("id-2", "email");
        VaultPayload target = new VaultPayload(List.of(targetEntry));
        VaultPayload source = new VaultPayload(List.of(sourceEntry));

        VaultPayload merged = new VaultEntryService().merge(target, source);

        assertEquals(2, merged.entries().size());
        assertEquals(targetEntry, merged.entries().get(0));
        assertEquals(sourceEntry, merged.entries().get(1));
    }

    @Test
    void shouldSkipDuplicatesDuringMerge() {
        VaultEntry targetEntry = testEntry("id-1", "github");
        VaultEntry duplicateEntry = testEntry("id-1", "github-personal");
        VaultPayload target = new VaultPayload(List.of(targetEntry));
        VaultPayload source = new VaultPayload(List.of(duplicateEntry));

        VaultPayload merged = new VaultEntryService().merge(target, source);

        assertEquals(1, merged.entries().size());
        assertEquals(targetEntry, merged.entries().getFirst());
    }

    private VaultEntry testEntry(String id, String name) {
        Instant now = Instant.parse("2026-05-13T12:00:00Z");
        return new VaultEntry(
                id,
                name,
                "user@example.com",
                "secret-password",
                "https://example.com",
                "notes",
                now,
                now);
    }
}
