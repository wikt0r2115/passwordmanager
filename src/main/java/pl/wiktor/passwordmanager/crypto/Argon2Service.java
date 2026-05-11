package pl.wiktor.passwordmanager.crypto;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

import pl.wiktor.passwordmanager.model.KdfParams;

public class Argon2Service {
    private static final int KEY_LENGTH = 32;

    public byte[] generateKey(char[] password, byte[] salt, KdfParams params) {
        Argon2Parameters.Builder builder = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withIterations(params.iterations())
                .withMemoryAsKB(params.memoryKiB())
                .withParallelism(params.parallelism())
                .withSalt(salt);

        Argon2BytesGenerator generate = new Argon2BytesGenerator();
        generate.init(builder.build());
        byte[] result = new byte[KEY_LENGTH];

        generate.generateBytes(password, result, 0, result.length);
        return result;
    }

}
