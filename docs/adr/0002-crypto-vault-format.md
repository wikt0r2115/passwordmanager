# ADR 0002: Crypto and Vault Format

## Status

Accepted for initial implementation, pending benchmark during coding.

## Context

The application must recover stored passwords, so credential entries need
reversible encryption at rest. The master password itself must not be stored.

## Decision

Use:

- Argon2id as the KDF through Bouncy Castle.
- AES-256-GCM for authenticated encryption through Java Cryptography
  Architecture.
- `SecureRandom` for salts, nonces and generated passwords.
- JSON envelope with encrypted payload.

## Initial Parameters

```text
Argon2id:
  memoryKiB: 19456
  iterations: 2
  parallelism: 1
  salt: 16 random bytes
  output: 32 bytes

AES-GCM:
  key: 256 bits
  nonce: 12 random bytes
  tag: 128 bits
```

## Rationale

OWASP recommends Argon2id as the preferred modern password hashing/KDF family.

OWASP recommends authenticated encryption modes where available.

NIST SP 800-38D specifies GCM as an authenticated encryption mode.

Java has built-in AES-GCM support, and Bouncy Castle provides Argon2id support.

## Consequences

- KDF parameters must be stored in the vault envelope.
- Every save must generate a fresh nonce.
- Tests must cover tampering and wrong-password failures.
- The project must document that it is not audited production security software.
