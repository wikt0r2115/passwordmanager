# Architecture

## High-Level Design

The project should stay layered:

```text
CLI -> Application Services -> Domain Model -> Vault Storage -> Crypto
```

The CLI layer parses commands and handles terminal input/output. It should not
know encryption details.

The service layer coordinates use cases such as adding an entry or changing the
master password.

The vault storage layer reads and writes the encrypted vault file.

The crypto layer derives keys, encrypts payloads and decrypts payloads. It must
not depend on CLI classes.

## Planned Package Structure

```text
pl.wiktor.passwordmanager
  cli       command classes and terminal adapters
  crypto    KDF, AES-GCM, random generation, sensitive value handling
  error     application exceptions and exit-code mapping
  io        filesystem paths, atomic writes, permissions checks
  model     domain objects such as VaultEntry
  password  generated password policies and generator
  vault     vault envelope, payload, serialization, repository
```

## Directory Structure

```text
passwordmanager/
  docs/
  src/main/java/pl/wiktor/passwordmanager/
  src/test/java/pl/wiktor/passwordmanager/
  src/test/resources/fixtures/
  examples/
  scripts/
```

The current repository skeleton keeps these directories with `.gitkeep` files
until actual implementation starts.

## Planned Main Components

### CLI

Responsibilities:

- Parse subcommands.
- Print user-facing messages.
- Read master password without echo.
- Map application errors to exit codes.

### Vault Service

Responsibilities:

- Validate use-case inputs.
- Coordinate load/decrypt/update/encrypt/save flows.
- Keep decrypted data lifetime short.

### Vault Repository

Responsibilities:

- Locate the vault file.
- Read JSON envelope.
- Write JSON envelope atomically.
- Avoid corrupting existing data on failed writes.

### Crypto Service

Responsibilities:

- Derive an encryption key from the master password and vault salt.
- Generate salts, nonces and passwords using `SecureRandom`.
- Encrypt and decrypt payloads with AES-GCM.
- Verify authentication failure cleanly for wrong passwords or tampered files.

### Password Generator

Responsibilities:

- Generate random passwords from a defined character policy.
- Use `SecureRandom`, never `Random`.
- Make defaults strong and explicit.

## Dependency Direction

Allowed:

```text
cli -> service -> vault -> crypto
service -> model
vault -> model
password -> crypto random source
```

Avoid:

```text
crypto -> cli
model -> storage
model -> crypto libraries
```
