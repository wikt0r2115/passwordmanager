package pl.wiktor.passwordmanager.vault;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import pl.wiktor.passwordmanager.model.CipherParams;
import pl.wiktor.passwordmanager.model.KdfParams;
import pl.wiktor.passwordmanager.model.VaultEnvelope;

class VaultAadBuilderTest {
    @Test
    void shouldBuildStableAadFromEnvelopeHeaderFields() {
        KdfParams kdf = new KdfParams("argon2id", 19456, 2, 1, "salt-base64");
        CipherParams cipher = new CipherParams("AES-256-GCM", "nonce-base64", 128);
        VaultEnvelope envelope = new VaultEnvelope(
                "passwordmanager-vault",
                1,
                Instant.parse("2026-05-11T12:00:00Z"),
                Instant.parse("2026-05-11T12:00:00Z"),
                kdf,
                cipher,
                "payload-base64");

        byte[] expected = "format=passwordmanager-vault;version=1;kdf=argon2id;cipher=AES-256-GCM;tag=128"
                .getBytes(StandardCharsets.UTF_8);

        assertArrayEquals(expected, VaultAadBuilder.build(envelope));
        assertArrayEquals(expected, VaultAadBuilder.build(envelope.format(), envelope.version(), kdf, cipher));
    }
}
