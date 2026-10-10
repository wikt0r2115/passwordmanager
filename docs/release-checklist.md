# v1.0 verification

## Behavior and failures

| Scenario | Verification | Result |
| --- | --- | --- |
| Init/add/list/show/update/remove | CLI command tests and release smoke | Pass locally |
| Export/import, duplicate IDs | `ExportCommandTest`, `ImportCommandTest` | Pass locally |
| Wrong master password, tampered ciphertext/AAD, invalid format/KDF | `VaultUnlockServiceTest`, CLI command tests | Rejected before save |
| Invalid import JSON or invalid later entry | `ImportCommandTest` | No partial write |
| No write permission, backup creation failure, atomic replacement failure | `VaultStorageTest` | Existing vault retained; backup from prior state recoverable |
| Second concurrent writer | `ConcurrentWriteTest` | Exit code 1, vault unchanged |
| Missing file or unavailable console | CLI command tests | Predictable nonzero exit |

The write lock covers the complete read/modify/write cycle of `init`, `add`, `update`, `remove` and `import`. It coordinates this CLI's writers on a local filesystem. The lock file can remain after a run. Atomic replacement means readers see a complete old or new vault. `.bak` is overwritten on each successful update and should not be the only long-term backup.

## Build and artifact

Run `bash scripts/package-release.sh` in a clean checkout with Java 21+, Bash, ZIP and `sha256sum`. This uses Maven Wrapper, runs the tests, builds the fat JAR and writes `dist/passwordmanager-1.0.0.zip` plus its `.sha256` file. Run `python3 scripts/smoke-release.py dist/passwordmanager-1.0.0.zip` to unpack into a fresh directory containing spaces and exercise `--help`, `--version`, the full entry flow, export/import and backup recovery against a separate synthetic vault. Linux was tested locally with JDK 25 compiling for Java 21. CI builds on JDK 21 and tests launchers on Linux, Windows and macOS; use the green tag workflow run as evidence for those hosted systems.

## Recovery check

After a successful update on a test vault, stop the CLI, preserve `vault.json` and `vault.json.bak`, copy `.bak` over `vault.json`, then use the same master password with `list`. The prior entry state should return. Do not rely on `.bak` after two or more writes if you need an older state.

## Portfolio description

Educational Java 21 password manager CLI with AES-256-GCM authenticated encryption, Argon2id key derivation, atomic vault updates, a prior-version backup, concurrent-writer locking and integration tests. Local-only and not professionally audited.
