# Project Scope

## Product Name

Working name: `Password Manager`.

Possible polished name later: `LocalPass CLI`.

## Target User

A developer who wants a local command-line vault for learning and demonstration.
The user is comfortable with terminal commands and local files.

## V1 Goals

- Create a new encrypted vault.
- Unlock an existing vault with a master password.
- Add credential entries.
- List entry names without exposing passwords.
- Show a selected entry after unlocking.
- Remove entries.
- Generate strong random passwords.
- Keep all production data local.
- Keep code split into small, testable layers.

## V1 Non-Goals

- No browser extension.
- No cloud sync.
- No GUI.
- No sharing or multi-user support.
- No clipboard integration.
- No automatic form filling.
- No password breach API integration.
- No hardware security module integration.
- No claim of third-party security audit.

## Planned Commands

```text
passwordmanager init
passwordmanager add <name>
passwordmanager list
passwordmanager show <name>
passwordmanager remove <name>
passwordmanager generate
passwordmanager change-master
```

## Data Location

Default local vault path:

```text
~/.passwordmanager/vault.json
```

The file name ends in `.json` because the vault envelope is JSON. The sensitive
payload inside that JSON is encrypted.

## Success Criteria for V1

- Running tests pass in CI.
- README explains setup, commands, limitations and threat model.
- Vault file cannot reveal stored usernames/passwords without the master
  password.
- Wrong master password fails cleanly without corrupting the vault.
- Interrupted commands do not partially overwrite existing vault data.
- Storage writes are atomic enough for a small local CLI.
