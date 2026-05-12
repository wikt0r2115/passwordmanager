package pl.wiktor.passwordmanager.vault;

import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import pl.wiktor.passwordmanager.crypto.AesGcmService;
import pl.wiktor.passwordmanager.crypto.Argon2Service;
import pl.wiktor.passwordmanager.crypto.SecureRandomBytes;
import pl.wiktor.passwordmanager.model.CipherParams;
import pl.wiktor.passwordmanager.model.KdfParams;
import pl.wiktor.passwordmanager.model.VaultEnvelope;
import pl.wiktor.passwordmanager.model.VaultPayload;

public class VaultSaveService {
    public VaultEnvelope save(VaultEnvelope oldEnvelope,
            VaultPayload vaultPayload,
            char[] masterPassword,
            ObjectMapper mapper) {
        byte[] key = null;
        byte[] payloadBytes = null;

        try {
            KdfParams kdf = oldEnvelope.kdf();

            byte[] saltBytes = Base64.getDecoder().decode(kdf.salt());
            Argon2Service argon2Service = new Argon2Service();
            key = argon2Service.generateKey(masterPassword, saltBytes, kdf);

            try {
                payloadBytes = mapper.writeValueAsBytes(vaultPayload);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Failed to serialize payload", e);
            }

            byte[] nonceBytes = new SecureRandomBytes().generateBytes(12);
            String nonceBase64 = Base64.getEncoder().encodeToString(nonceBytes);

            CipherParams cipher = new CipherParams("AES-256-GCM", nonceBase64, 128);
            byte[] aadBytes = VaultAadBuilder.build(
                    oldEnvelope.format(),
                    oldEnvelope.version(),
                    kdf,
                    cipher);

            byte[] ciphertextWithTag = new AesGcmService().encrypt(key, nonceBytes, payloadBytes, aadBytes);
            String payloadBase64 = Base64.getEncoder().encodeToString(ciphertextWithTag);
            return new VaultEnvelope(
                    oldEnvelope.format(),
                    oldEnvelope.version(),
                    oldEnvelope.createdAt(),
                    Instant.now(),
                    kdf,
                    cipher,
                    payloadBase64);
        } finally {
            if (masterPassword != null) {
                Arrays.fill(masterPassword, '\0');
            }
            if (key != null) {
                Arrays.fill(key, (byte) 0);
            }
            if (payloadBytes != null) {
                Arrays.fill(payloadBytes, (byte) 0);
            }
        }
    }
}
