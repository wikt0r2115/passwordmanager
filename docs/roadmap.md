# Roadmap

## Fit After `studytracker`

`studytracker` covered a simpler Java CLI domain: Maven, tests, storage, CSV, README, CI and basic layered design.

This project is the next step because it adds:

- security-sensitive design,
- encryption at rest,
- KDF parameter storage,
- command framework usage,
- stronger test coverage,
- richer documentation and threat-model discussion.

## Milestone Status

### Milestone 0: Planning

Status: done.

- Research.
- Documentation.
- Technology choices.
- Security boundaries.

### Milestone 1: Vault Initialization

Status: done.

- `init` command.
- master password prompt.
- encrypted empty vault file.
- tests for init and storage behavior.

### Milestone 2: Entries

Status: done.

- `add`.
- `list`.
- `show`.
- `update`.
- `remove`.
- vault round-trip tests.

### Milestone 3: Password Generator

Status: done.

- `generate`.
- length option.
- character-class options.
- generated password tests.

### Milestone 4: Hardening

Status: done for v1.0.

- wrong password and tampering tests.
- vault envelope validation.
- safer password prompt flows.
- clearer error messages.
- explicit security limitations.

Remaining future hardening:

- POSIX owner-only file permissions for vault and backups.
- dependency vulnerability scan.
- static analysis.
- deeper filesystem fault injection and crash recovery validation.

### Milestone 5: Portfolio Polish

Status: done for v1.0.

- README.
- Polish README.
- architecture docs.
- security design docs.
- CI workflow.
- Maven Wrapper.
- runnable fat jar and launcher scripts.
- versioned release archive, checksums and platform smoke workflow.

## Explicit Stop Point

The v1.0 feature scope is closed. See [release checks](release-checklist.md) for verification. Further hardening is optional and belongs to a later version.

Do not expand into GUI, sync or browser extension in this project version.
