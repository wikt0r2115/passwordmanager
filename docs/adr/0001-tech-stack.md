# ADR 0001: Technology Stack

## Status

Accepted for initial implementation.

## Context

The previous project used Java, Maven and hand-written CLI parsing. The next
project should build on that foundation while showing a higher level of
engineering maturity.

## Decision

Use:

- Java 21 LTS.
- Maven.
- picocli for CLI parsing.
- Jackson for JSON.
- JUnit 5 for tests.
- GitHub Actions for CI.

## Rationale

Java 21 is already available locally and is a current LTS version.

Maven keeps continuity with `studytracker`.

picocli gives structured commands, help output and password input support
without building another parser by hand.

Jackson is a standard JSON library and fits the vault envelope/payload format.

JUnit 5 keeps the test style familiar.

## Consequences

- The project can focus on vault design and security boundaries instead of CLI
  parsing mechanics.
- The dependency set is larger than `studytracker`, so dependency hygiene matters
  more.
- Maven wrapper should be added during the first implementation milestone.
