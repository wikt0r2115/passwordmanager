# Vault Format

## Format Strategy

The vault file is a JSON envelope with encrypted payload.

Only metadata required for decryption is stored in plaintext. Credential entries
are inside the encrypted payload.

## Plaintext Envelope

Planned shape:

```json
{
  "format": "passwordmanager-vault",
  "version": 1,
  "createdAt": "2026-05-06T00:00:00Z",
  "updatedAt": "2026-05-06T00:00:00Z",
  "kdf": {
    "name": "argon2id",
    "memoryKiB": 19456,
    "iterations": 2,
    "parallelism": 1,
    "salt": "base64"
  },
  "cipher": {
    "name": "AES-256-GCM",
    "nonce": "base64",
    "tagLengthBits": 128
  },
  "payload": "base64(ciphertext-and-tag)"
}
```

## Encrypted Payload

Planned plaintext before encryption:

```json
{
  "entries": [
    {
      "id": "uuid",
      "name": "github",
      "username": "user@example.com",
      "password": "secret",
      "url": "https://github.com",
      "notes": null,
      "createdAt": "2026-05-06T00:00:00Z",
      "updatedAt": "2026-05-06T00:00:00Z"
    }
  ]
}
```

## AAD Plan

AES-GCM should authenticate selected plaintext header fields as Additional
Authenticated Data. This means tampering with the header causes decryption to
fail.

Initial AAD fields:

```text
format
version
kdf.name
kdf.memoryKiB
kdf.iterations
kdf.parallelism
kdf.salt
cipher.name
cipher.nonce
cipher.tagLengthBits
```

Before implementation, define a canonical serialization for AAD so tests can
verify it deterministically.

## Versioning

`version` starts at `1`.

Future migrations must be explicit. The loader should reject unknown future
versions with a clear message rather than guessing.

## Corruption Handling

The application should distinguish:

- File missing.
- Invalid JSON envelope.
- Unsupported version.
- Missing required fields.
- Wrong master password or tampered encrypted data.

User-facing messages should avoid leaking low-level crypto details.
