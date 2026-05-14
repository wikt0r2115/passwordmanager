package pl.wiktor.passwordmanager.vault;

import java.util.Base64;

import pl.wiktor.passwordmanager.error.VaultValidationException;
import pl.wiktor.passwordmanager.model.CipherParams;
import pl.wiktor.passwordmanager.model.KdfParams;
import pl.wiktor.passwordmanager.model.VaultEnvelope;

public final class VaultEnvelopeValidator {
    private static final String FORMAT = "passwordmanager-vault";
    private static final int VERSION = 1;
    private static final String KDF_NAME = "argon2id";
    private static final int SALT_LENGTH = 16;
    private static final int MIN_MEMORY_KIB = 8 * 1024;
    private static final int MAX_MEMORY_KIB = 256 * 1024;
    private static final int MIN_ITERATIONS = 1;
    private static final int MAX_ITERATIONS = 10;
    private static final int MIN_PARALLELISM = 1;
    private static final int MAX_PARALLELISM = 4;
    private static final String CIPHER_NAME = "AES-256-GCM";
    private static final int NONCE_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final int MIN_CIPHERTEXT_WITH_TAG_LENGTH = 16;

    private VaultEnvelopeValidator() {
    }

    public static void validate(VaultEnvelope envelope) {
        if (envelope == null) {
            throw new VaultValidationException("Vault envelope is missing.");
        }
        if (!FORMAT.equals(envelope.format())) {
            throw new VaultValidationException("Unsupported vault format: " + envelope.format());
        }
        if (envelope.version() != VERSION) {
            throw new VaultValidationException("Unsupported vault version: " + envelope.version());
        }
        validateKdf(envelope.kdf());
        validateCipher(envelope.cipher());
        byte[] payload = decodeBase64(envelope.payload(), "payload");
        if (payload.length < MIN_CIPHERTEXT_WITH_TAG_LENGTH) {
            throw new VaultValidationException("Vault payload is too short.");
        }
    }

    private static void validateKdf(KdfParams kdf) {
        if (kdf == null) {
            throw new VaultValidationException("Vault KDF parameters are missing.");
        }
        if (!KDF_NAME.equals(kdf.name())) {
            throw new VaultValidationException("Unsupported KDF: " + kdf.name());
        }
        if (kdf.memoryKiB() < MIN_MEMORY_KIB || kdf.memoryKiB() > MAX_MEMORY_KIB) {
            throw new VaultValidationException("Vault KDF memory parameter is outside supported limits.");
        }
        if (kdf.iterations() < MIN_ITERATIONS || kdf.iterations() > MAX_ITERATIONS) {
            throw new VaultValidationException("Vault KDF iteration parameter is outside supported limits.");
        }
        if (kdf.parallelism() < MIN_PARALLELISM || kdf.parallelism() > MAX_PARALLELISM) {
            throw new VaultValidationException("Vault KDF parallelism parameter is outside supported limits.");
        }
        byte[] salt = decodeBase64(kdf.salt(), "KDF salt");
        if (salt.length != SALT_LENGTH) {
            throw new VaultValidationException("Vault KDF salt has invalid length.");
        }
    }

    private static void validateCipher(CipherParams cipher) {
        if (cipher == null) {
            throw new VaultValidationException("Vault cipher parameters are missing.");
        }
        if (!CIPHER_NAME.equals(cipher.name())) {
            throw new VaultValidationException("Unsupported cipher: " + cipher.name());
        }
        if (cipher.tagLengthBits() != TAG_LENGTH_BITS) {
            throw new VaultValidationException("Unsupported authentication tag length: " + cipher.tagLengthBits());
        }
        byte[] nonce = decodeBase64(cipher.nonce(), "cipher nonce");
        if (nonce.length != NONCE_LENGTH) {
            throw new VaultValidationException("Vault cipher nonce has invalid length.");
        }
    }

    private static byte[] decodeBase64(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new VaultValidationException("Vault " + fieldName + " is missing.");
        }
        try {
            return Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException e) {
            throw new VaultValidationException("Vault " + fieldName + " is not valid Base64.");
        }
    }
}
