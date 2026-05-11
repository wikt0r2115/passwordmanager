package pl.wiktor.passwordmanager.model;

public record KdfParams(
        String name,
        int memoryKiB,
        int iterations,
        int parallelism,
        String salt // Base64
) {
}
