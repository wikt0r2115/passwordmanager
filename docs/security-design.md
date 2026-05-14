# Security Design

## Positioning

This is an educational local password vault. It demonstrates careful security-aware engineering, but it is not audited and should not be advertised as a production replacement for mature password managers. Use it at your own risk.

## Threat Model

### In Scope

- Attacker obtains a copy of the vault file.
- Attacker edits or corrupts the vault file.
- Attacker guesses weak master passwords offline.
- Accidental partial writes or interrupted saves.
- Accidental logging of secrets.

### Out of Scope for V1

- Malware running as the current user.
- Keyloggers.
- Screen capture.
- Shell history or process listings that already contain secrets.
- Memory scraping by a privileged attacker.
- Multi-user machine hardening.
- Hardware-backed key storage.

## Core Security Decisions

### Master Password

The master password is never stored.

It is used only as input to Argon2id. The KDF output becomes the encryption key for the vault payload.

The CLI reads master passwords as `char[]` and clears them after use where Java APIs allow it.

### KDF

Use Argon2id through Bouncy Castle.

Current parameters:

```text
memory:      19456 KiB
iterations:  2
parallelism: 1
salt:        16 random bytes
key length:  32 bytes
```

These parameters are stored in the vault envelope so future versions can migrate them.

### Encryption

Use AES-256-GCM through Java Cryptography Architecture.

Current parameters:

```text
key size:        256 bits
nonce/IV:        12 random bytes per encryption
tag length:      128 bits
```

The nonce must never repeat for the same key. Each save encrypts the full payload with a new random nonce.

### AAD

AES-GCM authenticates selected plaintext header fields as Additional Authenticated Data:

```text
format
version
kdf.name
cipher.name
cipher.tagLengthBits
```

KDF parameters, salt and nonce are also validated before decrypt. Tampering with them prevents successful decryption because they change key derivation or AES-GCM inputs.

### Randomness

Use `java.security.SecureRandom` for:

- KDF salts.
- AES-GCM nonces.
- Generated passwords.
- Any future secret tokens.

Never use `java.util.Random` for security-relevant values.

### Sensitive Input

Entry passwords can be prompted without echo. The `--password` option remains available for scripting, but it is documented as less safe because shells can keep command history or expose process arguments.

Entry passwords are represented as strings in the decrypted payload model because the vault has to serialize and recover them. This is a Java/application limitation and should not be overstated as full memory safety.

### Logging

V1 should not log secrets.

Do not log:

- Master password.
- Derived keys.
- Decrypted vault payload.
- Entry passwords.
- Plaintext export contents.

### File Writes

Vault writes go through a temporary file in the vault directory and then replace the target file with an atomic move where supported.

Before replacing an existing vault, the application creates a `.bak` copy next to it. Backup failure is reported as a warning and does not block the main save attempt.

### File Permissions

POSIX file permission hardening is not enforced in V1. It remains a future hardening item.

## Implemented Hardening

- Wrong master password fails without changing the vault.
- Tampered ciphertext fails through AES-GCM authentication.
- Unsupported/corrupt envelope metadata fails through `VaultEnvelopeValidator`.
- Re-saving vault changes nonce.
- Two vaults initialized with the same master password have different salts.
- Mutating commands keep the previous vault intact on wrong password and common validation failures.
- Public CLI has no long-lived `unlock` command because there is no unlocked session.

## Explicit Limitations

- Java cannot guarantee immediate removal of all sensitive data from memory.
- A local CLI cannot protect against malware running as the same user.
- Clipboard support is intentionally excluded from V1.
- Export files are plaintext and must be handled as sensitive data.
- This project will not claim formal cryptographic audit.
