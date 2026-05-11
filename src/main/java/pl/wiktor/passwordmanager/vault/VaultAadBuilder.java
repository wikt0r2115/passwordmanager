package pl.wiktor.passwordmanager.vault;

import java.nio.charset.StandardCharsets;

import pl.wiktor.passwordmanager.model.CipherParams;
import pl.wiktor.passwordmanager.model.KdfParams;
import pl.wiktor.passwordmanager.model.VaultEnvelope;

public final class VaultAadBuilder {
    private VaultAadBuilder() {
    }

    public static byte[] build(VaultEnvelope envelope) {
        return build(envelope.format(), envelope.version(), envelope.kdf(), envelope.cipher());
    }

    public static byte[] build(String format, int version, KdfParams kdf, CipherParams cipher) {
        String aad = "format=" + format
                + ";version=" + version
                + ";kdf=" + kdf.name()
                + ";cipher=" + cipher.name()
                + ";tag=" + cipher.tagLengthBits();
        return aad.getBytes(StandardCharsets.UTF_8);
    }
}
