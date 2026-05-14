package pl.wiktor.passwordmanager.crypto;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

import pl.wiktor.passwordmanager.model.KdfParams;

public class Argon2Service {
    private static final int KEY_LENGTH = 32;
    private static final int SALT_LENGTH = 16;
    private static final int MIN_MEMORY_KIB = 1024;
    private static final int MAX_MEMORY_KIB = 256 * 1024;
    private static final int MIN_ITERATIONS = 1;
    private static final int MAX_ITERATIONS = 10;
    private static final int MIN_PARALLELISM = 1;
    private static final int MAX_PARALLELISM = 4;

    public byte[] generateKey(char[] password, byte[] salt, KdfParams params) {
        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
        if (salt == null || salt.length != SALT_LENGTH) {
            throw new IllegalArgumentException("Salt must have length of " + SALT_LENGTH);
        }
        if (params == null) {
            throw new IllegalArgumentException("KDF parameters cannot be null");
        }
        if (params.memoryKiB() < MIN_MEMORY_KIB || params.memoryKiB() > MAX_MEMORY_KIB) {
            throw new IllegalArgumentException("KDF memory parameter is outside supported limits");
        }
        if (params.iterations() < MIN_ITERATIONS || params.iterations() > MAX_ITERATIONS) {
            throw new IllegalArgumentException("KDF iteration parameter is outside supported limits");
        }
        if (params.parallelism() < MIN_PARALLELISM || params.parallelism() > MAX_PARALLELISM) {
            throw new IllegalArgumentException("KDF parallelism parameter is outside supported limits");
        }

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
