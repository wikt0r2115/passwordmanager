package pl.wiktor.passwordmanager.vault;

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;

import com.fasterxml.jackson.databind.ObjectMapper;

import pl.wiktor.passwordmanager.crypto.AesGcmService;
import pl.wiktor.passwordmanager.crypto.Argon2Service;
import pl.wiktor.passwordmanager.model.VaultEnvelope;
import pl.wiktor.passwordmanager.model.VaultPayload;

public class VaultUnlockService {
    public VaultPayload unlock(VaultEnvelope envelope, char[] masterPassword, ObjectMapper mapper) throws IOException {
        byte[] key = null;
        byte[] plaintext = null;

        try {
            if (masterPassword == null) {
                throw new IllegalArgumentException("Master password cant be null");
            }
            if (mapper == null) {
                throw new IllegalArgumentException("ObjectMapper cant be null");
            }
            VaultEnvelopeValidator.validate(envelope);

            byte[] salt = Base64.getDecoder().decode(envelope.kdf().salt());
            byte[] nonce = Base64.getDecoder().decode(envelope.cipher().nonce());
            byte[] ciphertextWithTag = Base64.getDecoder().decode(envelope.payload());

            Argon2Service argon2Service = new Argon2Service();
            key = argon2Service.generateKey(masterPassword, salt, envelope.kdf());

            byte[] aad = VaultAadBuilder.build(envelope);

            AesGcmService aesGcmService = new AesGcmService();
            plaintext = aesGcmService.decrypt(key, nonce, ciphertextWithTag, aad);

            VaultPayload payload = mapper.readValue(plaintext, VaultPayload.class);
            if (payload == null || payload.entries() == null) {
                throw new IOException("Vault payload entries are missing.");
            }
            return payload;
        } finally {
            if (masterPassword != null) {
                Arrays.fill(masterPassword, '\0');
            }
            if (key != null) {
                Arrays.fill(key, (byte) 0);
            }
            if (plaintext != null) {
                Arrays.fill(plaintext, (byte) 0);
            }
        }
    }
}
