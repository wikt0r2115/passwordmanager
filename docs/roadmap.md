# Roadmap

## Fit After `studytracker`

`studytracker` proved the basics:

- Java CLI.
- Maven.
- tests,
- storage,
- CSV,
- README,
- CI,
- simple layered design.

This project is the next step because it keeps the CLI shape familiar but adds:

- security-sensitive design,
- encryption at rest,
- KDF parameter storage,
- stronger test strategy,
- CLI framework usage,
- richer documentation.

## Milestone 0: Planning

Status: current.

- Research.
- Documentation.
- Empty project structure.
- Technology choices.
- Security boundaries.

## Milestone 1: Vault Initialization

- Maven wrapper.
- CI workflow.
- `init` command.
- master password prompt.
- encrypted empty vault file.
- tests for init and storage failure.

## Milestone 2: Basic Entries

- `add`.
- `list`.
- `show`.
- `remove`.
- domain validation.
- vault round-trip tests.

## Milestone 3: Password Generator

- `generate`.
- length option.
- symbols option.
- clear defaults.
- tests for generated length and character policy.

## Milestone 4: Hardening

- wrong password and tampering tests.
- file permission best effort.
- clearer error messages.
- README with threat model and disclaimer.

## Milestone 5: Portfolio Polish

- badges.
- examples.
- command screenshots/asciinema.
- architecture section.
- security decisions section.

## Explicit Stop Point

Stop after Milestone 5 unless there is a strong reason to continue.

Do not expand into GUI, sync or browser extension in this project version.
