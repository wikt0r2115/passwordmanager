# Vault Format

## Format Strategy

The vault file is a JSON envelope with an encrypted payload.

Only metadata required for decryption is stored in plaintext. Credential entries are inside the encrypted payload.

## Plaintext Envelope

Current V1 shape:

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

Plaintext before encryption:

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

## AAD

AES-GCM authenticates selected plaintext header fields as Additional Authenticated Data:

```text
format
version
kdf.name
cipher.name
cipher.tagLengthBits
```

Other envelope fields are still validated before decrypt. Changes to KDF parameters, salt or nonce also prevent successful decryption because they affect key derivation or AES-GCM input.

## Validation

The loader rejects:

- missing envelope,
- unsupported format,
- unsupported version,
- missing KDF/cipher parameters,
- unsupported KDF/cipher names,
- invalid Base64,
- invalid salt/nonce lengths,
- unsupported tag length,
- unreasonable Argon2 memory/iteration/parallelism values,
- too-short payload.

## Versioning

`version` starts at `1`.

Future migrations must be explicit. The loader rejects unknown future versions with a clear message rather than guessing.

## Corruption Handling

The application distinguishes:

- file missing,
- invalid JSON envelope,
- unsupported envelope metadata,
- wrong master password or tampered encrypted data.

User-facing messages avoid leaking low-level crypto details.
