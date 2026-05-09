# Security Design

## Positioning

This is an educational local password vault. It should demonstrate careful
security design, but it is not audited and should not be advertised as a
production replacement for mature password managers.

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
- Shell history already containing secrets.
- Memory scraping by a privileged attacker.
- Multi-user machine hardening beyond basic file permissions.
- Hardware-backed key storage.

## Core Security Decisions

### Master Password

The master password is never stored.

It is used only as input to a KDF. The KDF output becomes the encryption key for
the vault payload.

### KDF

Use Argon2id through Bouncy Castle.

Initial planned parameters:

```text
memory:      19456 KiB
iterations:  2
parallelism: 1
salt:        16 random bytes
key length:  32 bytes
```

These parameters follow OWASP's listed Argon2id baseline. They must be stored in
the vault envelope so future versions can change them.

### Encryption

Use AES-256-GCM through Java Cryptography Architecture.

Planned parameters:

```text
key size:        256 bits
nonce/IV:        12 random bytes per encryption
tag length:      128 bits
AAD:             canonical vault header fields
```

The nonce must never repeat for the same key. For this project, each save
encrypts the full payload with a new random nonce.

### Randomness

Use `java.security.SecureRandom` for:

- KDF salts.
- AES-GCM nonces.
- Generated passwords.
- Any future secret tokens.

Never use `java.util.Random` for security-relevant values.

### Sensitive Input

Use `Console.readPassword()` or picocli interactive `char[]` options where
possible.

Clear `char[]` values after use with `Arrays.fill(...)`.

Avoid converting master passwords to `String`. If a library requires bytes,
perform conversion in one narrow adapter and clear temporary arrays immediately.

### Logging

V1 should not log secrets.

Do not log:

- Master password.
- Derived keys.
- Decrypted vault payload.
- Entry passwords.
- Full vault JSON if it includes ciphertext from real user data.

### File Writes

Write through a temporary file in the vault directory, then replace the target
file. This copies the safety pattern already proven in `studytracker`.

### File Permissions

On POSIX systems, attempt to set the vault directory to owner-only access.

If this is not portable on a platform, warn in documentation and tests rather
than pretending the guarantee exists everywhere.

## Security Tests to Add

- Wrong master password fails.
- Tampered ciphertext fails.
- Tampered header/AAD fails.
- Re-saving vault changes nonce.
- Two vaults initialized with same master password have different salts.
- Generated passwords use `SecureRandom` through an injectable random source.
- Storage failure does not corrupt previous vault.

## Explicit Limitations

- Java cannot guarantee immediate removal of all sensitive data from memory.
- A local CLI cannot protect against malware running as the same user.
- Clipboard support is intentionally excluded from V1.
- This project will not claim formal cryptographic audit.
