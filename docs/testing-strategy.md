# Testing Strategy

## Test Levels

### Unit Tests

- Domain validation.
- Password generation policy.
- Vault envelope serialization.
- KDF parameter validation.
- Error mapping.

### Crypto Tests

- Encrypt/decrypt round trip.
- Wrong password fails.
- Tampered ciphertext fails.
- Tampered AAD fails.
- Random salt/nonce changes between saves.

### Repository Tests

- Missing vault file.
- Invalid JSON.
- Atomic write behavior.
- Storage failure keeps previous vault unchanged.

### CLI Tests

- `init` happy path.
- `add`, `list`, `show`, `remove` flows.
- EOF/interrupted input.
- Wrong master password.
- Validation errors to stderr.
- Non-zero exit codes for serious failures.

## Test Design Rule

Production randomness must be injectable in tests.

Do not make tests depend on fixed global `SecureRandom` output. Wrap randomness
behind a small interface, then use deterministic test implementations.

## CI Goal

First CI workflow after implementation starts:

```bash
./mvnw test
```

Later:

```bash
./mvnw verify
```

Potential additions:

- dependency vulnerability scan,
- static analysis,
- formatting check.
