# Password Manager

[Polska wersja](README.pl.md)

Local educational password manager CLI planned as the next portfolio project
after `studytracker`.

This repository currently contains documentation, architectural decisions and an
empty project structure. It intentionally contains no application code yet.

## Project Goal

Build a local password vault that stores credentials in an encrypted file on the
user's machine. The project is meant to demonstrate careful design, testability
and practical security engineering, not to compete with audited production
password managers.

## Planned V1 Scope

- Initialize a local encrypted vault.
- Unlock the vault with a master password.
- Add, list, show and delete credential entries.
- Generate random passwords.
- Store one encrypted vault file locally.
- Keep the CLI testable and separated from domain, crypto and persistence code.

## Non-Goals for V1

- Browser extension.
- Cloud sync.
- GUI.
- Shared/team vaults.
- Clipboard integration.
- Password breach checks.
- Real production security claims or audit claims.

## Selected Technology

- Java 21 LTS.
- Maven.
- picocli for command parsing.
- Jackson for JSON serialization.
- Bouncy Castle for Argon2id.
- Java Cryptography Architecture for AES-GCM and `SecureRandom`.
- JUnit 5 for tests.
- GitHub Actions for CI once implementation starts.

See:

- [Research](docs/research.md)
- [Project Scope](docs/scope.md)
- [Architecture](docs/architecture.md)
- [Security Design](docs/security-design.md)
- [Vault Format](docs/vault-format.md)
- [Testing Strategy](docs/testing-strategy.md)
- [Roadmap](docs/roadmap.md)

## Planned CLI Shape

```text
passwordmanager init
passwordmanager add <name>
passwordmanager list
passwordmanager show <name>
passwordmanager remove <name>
passwordmanager generate
passwordmanager change-master
```

The exact command names can still change during implementation.

## Current Status

```text
Status: planning / skeleton only
Code: not implemented yet
```

The next step is to implement the first thin vertical slice:

1. `passwordmanager init`
2. vault file creation
3. master password prompt
4. encrypted empty vault persisted to disk
5. tests for vault initialization
