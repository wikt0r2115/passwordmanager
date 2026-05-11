package pl.wiktor.passwordmanager.crypto;

import java.security.SecureRandom;

public class SecureRandomBytes {
    private final SecureRandom secureRandom = new SecureRandom();

    public byte[] generateBytes(int length) {
        if(length <= 0 )
            throw new IllegalArgumentException("Length must be greater than zero");
        byte[] salt = new byte[length];
        secureRandom.nextBytes(salt);

        return salt;
    }
}
