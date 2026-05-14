# Testing Strategy

## Test Levels

### Unit Tests

- Domain validation.
- Password generation policy.
- Vault envelope serialization.
- KDF parameter validation.
- Exit-code and message mapping for CLI commands.

### Crypto Tests

- Encrypt/decrypt round trip.
- Wrong password fails.
- Tampered ciphertext fails.
- Tampered AAD fails.
- Random salt/nonce changes between saves.
- Invalid vault metadata fails before decrypt where possible.

### Storage Tests

- Missing vault file.
- Existing vault is not overwritten by `init`.
- Replace existing vault.
- Backup creation is covered through mutating CLI flows.

### CLI Tests

- `init`, `add`, `list`, `show`, `update`, `remove`, `export`, `import`, `generate`.
- Missing vault file without password prompt.
- Missing password input.
- Wrong master password without changing the vault.
- Validation errors to stderr.
- Non-zero exit codes for serious failures.
- No-argument invocation prints usage.

## Current Test Command

```bash
./mvnw test
```

## Potential Additions

- Dependency vulnerability scan.
- Static analysis.
- Formatting check.
- More filesystem failure simulation for backup/write edge cases.
