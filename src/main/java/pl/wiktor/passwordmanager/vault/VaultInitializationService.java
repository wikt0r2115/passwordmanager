package pl.wiktor.passwordmanager.vault;

import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import pl.wiktor.passwordmanager.crypto.AesGcmService;
import pl.wiktor.passwordmanager.crypto.Argon2Service;
import pl.wiktor.passwordmanager.crypto.SecureRandomBytes;
import pl.wiktor.passwordmanager.model.CipherParams;
import pl.wiktor.passwordmanager.model.KdfParams;
import pl.wiktor.passwordmanager.model.VaultEnvelope;
import pl.wiktor.passwordmanager.model.VaultPayload;

public class VaultInitializationService {

    public VaultEnvelope init(char[] masterPassword, ObjectMapper mapper) {
        byte[] key = null;
        try {
            SecureRandomBytes secureRandomBytes = new SecureRandomBytes();
            byte[] saltBytes = secureRandomBytes.generateBytes(16);
            String saltBase64 = Base64.getEncoder().encodeToString(saltBytes);

            KdfParams kdf = new KdfParams("argon2id", 19456, 2, 1, saltBase64);
            Argon2Service argon2Service = new Argon2Service();
            key = argon2Service.generateKey(masterPassword, saltBytes, kdf);

            VaultPayload vaultPayload = new VaultPayload(List.of());
            byte[] payloadBytes;
            try {
                payloadBytes = mapper.writeValueAsBytes(vaultPayload);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Parsing to JSON failed", e);
            }

            byte[] nonceBytes = secureRandomBytes.generateBytes(12);
            String nonceBase64 = Base64.getEncoder().encodeToString(nonceBytes);

            CipherParams cipher = new CipherParams("AES-256-GCM", nonceBase64, 128);

            byte[] aadBytes = VaultAadBuilder.build("passwordmanager-vault", 1, kdf, cipher);

            AesGcmService aesGcmService = new AesGcmService();
            byte[] ciphertextWithTag = aesGcmService.encrypt(key, nonceBytes, payloadBytes, aadBytes);
            String payloadBase64 = Base64.getEncoder().encodeToString(ciphertextWithTag);
            Instant now = Instant.now();

            return new VaultEnvelope(
                    "passwordmanager-vault",
                    1,
                    now,
                    now,
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
        }
    }
}
