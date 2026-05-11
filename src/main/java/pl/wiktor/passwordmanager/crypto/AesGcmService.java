package pl.wiktor.passwordmanager.crypto;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import pl.wiktor.passwordmanager.error.CryptoException;

public class AesGcmService {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";
    private static final int TAG_LENGTH = 128;
    private static final int KEY_LENGTH = 32;
    private static final int NONCE_LENGTH = 12;

    public byte[] encrypt(byte[] key, byte[] nonce, byte[] plaintext, byte[] aad) {
        if (key == null)
            throw new IllegalArgumentException("Key cant be null");
        if (key.length != KEY_LENGTH)
            throw new IllegalArgumentException("Key must have length of " + KEY_LENGTH);
        if (nonce == null)
            throw new IllegalArgumentException("Nonce cant be null");
        if (nonce.length != NONCE_LENGTH)
            throw new IllegalArgumentException("Nonce must have length of " + NONCE_LENGTH);
        if (plaintext == null)
            throw new IllegalArgumentException("Text cant be null");
        if (aad == null)
            throw new IllegalArgumentException("AAD cant be null");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, KEY_ALGORITHM);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(TAG_LENGTH, nonce);
        try {
            Cipher cipher = javax.crypto.Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, gcmParameterSpec);
            cipher.updateAAD(aad);
            return cipher.doFinal(plaintext);
        } catch (Exception e) {
            throw new CryptoException("Encryption failed", e);
        }
    }

    public byte[] decrypt(byte[] key, byte[] nonce, byte[] cipherTextWithTag, byte[] aad) {
        if (key == null)
            throw new IllegalArgumentException("Key cant be null");
        if (key.length != KEY_LENGTH)
            throw new IllegalArgumentException("Key must have length of " + KEY_LENGTH);
        if (nonce == null)
            throw new IllegalArgumentException("Nonce cant be null");
        if (nonce.length != NONCE_LENGTH)
            throw new IllegalArgumentException("Nonce must have length of " + NONCE_LENGTH);
        if (cipherTextWithTag == null)
            throw new IllegalArgumentException("CipherText cant be null");
        if (aad == null)
            throw new IllegalArgumentException("AAD cant be null");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, KEY_ALGORITHM);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(TAG_LENGTH, nonce);
        try {
            Cipher cipher = javax.crypto.Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmParameterSpec);
            cipher.updateAAD(aad);
            return cipher.doFinal(cipherTextWithTag);
        } catch (Exception e) {
            throw new CryptoException("Decryption failed", e);
        }
    }

}
