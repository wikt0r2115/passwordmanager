# Project Scope

## Product

Working name: `Password Manager CLI`.

This is an educational local encrypted password vault for learning and portfolio presentation. It is designed for a developer or technical user who is comfortable with terminal commands and local files. It is not audited security software and is used at the user's own risk.

## V1 Goals

- Create a new encrypted vault.
- Unlock an existing vault internally with a master password for each command.
- Add credential entries.
- List entry names, usernames and URLs without exposing passwords.
- Show a selected entry after unlocking.
- Update entry fields and rotate passwords.
- Remove entries.
- Generate strong random passwords.
- Export and import plaintext JSON for backup/migration workflows.
- Keep all vault data local.
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
- No production-security or third-party-audit claim.

## Implemented Commands

```text
passwordmanager init
passwordmanager add --name <name> --username <username>
passwordmanager list
passwordmanager show --name <name>
passwordmanager update --name <name>
passwordmanager remove --name <name>
passwordmanager export --output <path>
passwordmanager import --input <path>
passwordmanager generate
```

There is no public `unlock` command. Unlocking is an internal step because the application does not maintain a long-lived unlocked session.

## Data Location

Default local vault path:

```text
~/.passwordmanager/vault.json
```

The file name ends in `.json` because the vault envelope is JSON. The sensitive payload inside that JSON is encrypted.

## Success Criteria for V1

- Running tests pass in CI.
- README explains setup, commands, limitations and threat model.
- Vault file does not reveal stored usernames/passwords without the master password.
- Wrong master password fails cleanly without corrupting the vault.
- Unsupported/corrupt vault envelopes fail before expensive KDF work where possible.
- Interrupted commands do not partially overwrite existing vault data.
- Storage writes are atomic enough for a small local CLI.
