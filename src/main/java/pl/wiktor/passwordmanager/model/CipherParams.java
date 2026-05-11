package pl.wiktor.passwordmanager.model;

public record CipherParams(
        String name,
        String nonce, // Base64
        int tagLengthBits) {
}
