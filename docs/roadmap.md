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

Status: mostly done for V1.

- wrong password and tampering tests.
- vault envelope validation.
- safer password prompt flows.
- clearer error messages.
- explicit security limitations.

Remaining future hardening:

- POSIX owner-only file permissions for vault and backups.
- dependency vulnerability scan.
- static analysis.
- more filesystem failure simulation.

### Milestone 5: Portfolio Polish

Status: done for V1, with optional polish still possible.

- README.
- Polish README.
- architecture docs.
- security design docs.
- CI workflow.
- Maven Wrapper.
- runnable fat jar and launcher scripts.

## Explicit Stop Point

Stop after V1 polish unless there is a strong reason to continue.

Do not expand into GUI, sync or browser extension in this project version.
