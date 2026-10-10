# Changelog

## v1.0.0 — 2026-10-10

- Release of the educational local password manager CLI with `init`, `add`, `list`, `show`, `update`, `remove`, `generate`, `export` and `import`.
- AES-256-GCM vault encryption with Argon2id key derivation, envelope validation and authenticated header fields.
- Atomic vault replacement, prior-version `.bak` backup and exclusive lock for concurrent writers. A backup failure now aborts the write.
- Versioned ZIP with runnable fat JAR, Linux/macOS and Windows launchers, license and bilingual READMEs. SHA-256 checksum accompanies the archive.
- Maven Wrapper build, behavior tests and release smoke checks in CI on Linux, Windows and macOS.

Requirements: Java 21 or newer. The release ZIP does not require Maven. Master passwords require a terminal prompt. The program is for education and has not undergone a professional security audit. Exports are plaintext; `.bak` holds only the previous encrypted vault. Local malware and privileged users are outside the protection boundary.
