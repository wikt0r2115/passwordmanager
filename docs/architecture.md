# Architecture

## High-Level Design

The project uses a small layered structure:

```text
CLI -> Vault Services -> Domain Model -> Vault Storage -> Crypto
```

The CLI layer parses commands, handles terminal input/output and maps failures to exit codes. It does not implement encryption details directly.

The vault service layer coordinates use cases such as initialization, unlock, save, add, update, remove and import merge.

The storage layer reads and writes the encrypted vault envelope.

The crypto layer derives keys, encrypts payloads and decrypts payloads. It does not depend on CLI classes.

## Package Structure

```text
pl.wiktor.passwordmanager
  cli       picocli command classes and terminal password readers
  crypto    Argon2id, AES-GCM and secure random helpers
  error     application exceptions
  io        ObjectMapper setup, vault path resolution and vault storage
  model     immutable records for vault envelope, payload and entries
  password  generated password policy and generator
  vault     vault initialization, unlock, save, AAD, validation and entry service
```

## Main Components

### CLI

Responsibilities:

- Parse subcommands and options.
- Print user-facing messages.
- Read master password and prompted entry passwords without echo.
- Avoid prompting when a precondition already failed, such as missing vault file.
- Map application errors to stable exit codes.

### Vault Services

Responsibilities:

- Validate use-case inputs.
- Coordinate load/decrypt/update/encrypt/save flows.
- Keep decrypted data lifetime short.
- Validate vault envelope metadata before KDF/decrypt.

### Vault Storage

Responsibilities:

- Locate the vault file.
- Read JSON envelope.
- Write JSON envelope through a temporary file and atomic move.
- Create `.bak` before replacing an existing vault.
- Avoid corrupting existing data on failed writes.

### Crypto

Responsibilities:

- Derive a 256-bit encryption key from the master password and vault salt.
- Generate salts, nonces and passwords using `SecureRandom`.
- Encrypt and decrypt payloads with AES-GCM.
- Fail cleanly for wrong passwords or tampered ciphertext.

### Password Generator

Responsibilities:

- Generate random passwords from a defined character policy.
- Use `SecureRandom`, never `Random`.
- Make defaults strong and explicit.

## Dependency Direction

Allowed:

```text
cli -> vault/io/model/password/error
vault -> crypto/model/error
io -> model
password -> java.security.SecureRandom
crypto -> model/error
```

Avoid:

```text
crypto -> cli
model -> storage
model -> crypto libraries
```
