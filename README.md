# Password Manager CLI

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Educational local encrypted password vault built with Java 21. The project is a portfolio-grade CLI application that demonstrates layered design, authenticated encryption, key derivation, atomic file writes and testable command flows.

> This project is educational, not professionally audited, and not a replacement for mature production password managers. Use it at your own risk.

## Features

- Encrypted local JSON vault stored at `~/.passwordmanager/vault.json`.
- AES-256-GCM authenticated encryption for the vault payload.
- Argon2id key derivation from the master password.
- Add, list, show, update and remove password entries.
- Standalone password generator with configurable length and character classes.
- Plaintext JSON export/import for migration and backup workflows.
- Atomic vault writes with `.bak` backup creation before updates.
- Maven build, JUnit test suite, GitHub Actions workflow and runnable fat jar.
- Local Docker Compose demo with a persistent encrypted vault volume.

## Install and first run

Install Java 21 or newer. Download `passwordmanager-1.0.0.zip` and its `.sha256` file from the [v1.0.0 release](https://github.com/wikt0r2115/passwordmanager/releases/tag/v1.0.0). Verify the checksum and extract the archive. No Maven installation is needed for the release package.

Linux:

```bash
sha256sum -c passwordmanager-1.0.0.zip.sha256
unzip passwordmanager-1.0.0.zip
./passwordmanager-1.0.0/passwordmanager.sh --help
./passwordmanager-1.0.0/passwordmanager.sh init
```

macOS: use `shasum -a 256 -c passwordmanager-1.0.0.zip.sha256`, then the same `unzip` and launcher commands. Windows PowerShell: compare `(Get-FileHash .\passwordmanager-1.0.0.zip -Algorithm SHA256).Hash.ToLower()` with the hash in `.sha256`, run `Expand-Archive .\passwordmanager-1.0.0.zip`, then `.\passwordmanager-1.0.0\passwordmanager.bat --help` and `init`.

The CLI prompts for the master password in a real terminal. For source builds, run `./mvnw clean package` (Windows: `.\mvnw.cmd clean package`) and use the launcher in the repository root. See [installation details](docs/installation.md).

## Local Docker demo

Requires Docker Engine with Compose v2, or Docker Desktop running Linux containers.
Clone this repository and run the following commands from its root in a real terminal
(PowerShell, Terminal or a Linux shell). Java and Maven are included in the build image.

```bash
docker compose build
docker compose run --rm passwordmanager --help
docker compose run --rm passwordmanager init
docker compose run --rm passwordmanager add --name github --username user@example.com --url https://github.com
docker compose run --rm passwordmanager list
docker compose run --rm passwordmanager update --name github --prompt-password
docker compose run --rm passwordmanager show --name github
docker compose run --rm passwordmanager remove --name github
```

The master and entry passwords are prompted without echo; do not pipe them into
the command or disable the TTY with `-T`. `show` deliberately displays the stored
entry password. Each command runs in a fresh, non-root container without network
access. The encrypted vault and `.bak` backup stay in the Compose `vault` named
volume at `/data/.passwordmanager`, so removing the container with `--rm` does not
remove the vault. This volume is separate from your host's `~/.passwordmanager`.
Keep using the same checkout directory (or the same Compose project name) to open
the same volume. Run `init` only once for that volume. **`docker compose down -v`
deletes the demo vault and its backup.**

The image is built from source with Java 21 and Maven 3.9.9, and the build runs the
JUnit suite. The `Docker demo` CI workflow also tests the real interactive commands,
data persistence between containers, backup creation and an incorrect master password.
On Linux, the same integration check can be run after the build with
`python3 scripts/smoke-docker.py`; it uses an isolated test volume and removes it afterward.

## CLI demo

![Password Manager CLI terminal demo](docs/assets/passwordmanager-demo.gif)

Demo flow from the extracted release directory (with a separate test user account):

```bash
./passwordmanager.sh init
./passwordmanager.sh add --name github --username user@example.com --url https://github.com
./passwordmanager.sh list
./passwordmanager.sh update --name github --prompt-password
./passwordmanager.sh show --name github
```

Initialize a vault:

```bash
./passwordmanager.sh init
```

Add an entry. If `--password` is omitted, the CLI prompts for the entry password without echo:

```bash
./passwordmanager.sh add --name github --username user@example.com --url https://github.com
```

Generate and store a new password:

```bash
./passwordmanager.sh add --name email --username user@example.com --generate --length 24
```

List and show entries:

```bash
./passwordmanager.sh list
./passwordmanager.sh show --name github
```

Update an entry and prompt for a new password:

```bash
./passwordmanager.sh update --name github --prompt-password
```

Export/import plaintext JSON:

```bash
./passwordmanager.sh export --output backup.json
./passwordmanager.sh import --input backup.json
```

Exported files are plaintext and contain passwords. Handle them as sensitive data.

## Commands

| Command | Description |
| :--- | :--- |
| `init` | Initialize a new encrypted vault. |
| `add` | Add a new entry. Prompts for the entry password unless `--password` or `--generate` is used. |
| `list` | List entry names, usernames and URLs without showing passwords. |
| `show` | Show full details, including password, for one entry. |
| `update` | Update fields or rotate a password with `--generate` or `--prompt-password`. |
| `remove` | Remove an entry by name. |
| `export` | Export entries to plaintext JSON. |
| `import` | Merge entries from plaintext JSON, skipping duplicate IDs. |
| `generate` | Generate a standalone random password. |

## Security Design

- Master passwords are read as `char[]` and cleared after use where Java APIs allow it.
- Entry passwords can be prompted without echo. Passing an entry password with `--password` is supported for scripting, but it can expose the password through shell history or process listings.
- Vault envelopes are validated before key derivation to reject unsupported versions, invalid Base64, invalid nonce/salt lengths and unreasonable KDF parameters.
- AES-GCM AAD authenticates selected header fields: format, version, KDF name, cipher name and tag length.
- KDF parameters, salt and nonce are stored in the plaintext envelope because they are required for decryption. Credential data is stored only inside the encrypted payload.
- Writers take an exclusive lock on `vault.json.lock` from read through save. A second writer exits with code 1. The lock file can remain after exit; do not delete it while commands may be running. Readers can run during a write and see either the old or new complete vault.
- A successful update first creates `vault.json.bak` from the previous encrypted vault, then atomically replaces the current file. If backup creation or replacement fails, the previous vault remains. Keep separate backups too: `.bak` stores only the state before the most recent successful write.
- A local CLI cannot protect against malware, keyloggers, screen capture or a privileged attacker on the same machine.
- This project is educational software used at your own risk.

## Recovery

The encrypted vault is at `~/.passwordmanager/vault.json` (Windows: `%USERPROFILE%\.passwordmanager\vault.json`). After a successful update, the previous encrypted version is at `vault.json.bak` in the same directory. Keep your master password; the backup requires it too. Stop all passwordmanager commands, copy both files elsewhere, then replace the broken vault with the backup and try `list`. On Linux/macOS:

```bash
cp ~/.passwordmanager/vault.json ~/.passwordmanager/vault.json.failed
cp ~/.passwordmanager/vault.json.bak ~/.passwordmanager/vault.json
./passwordmanager.sh list
```

On Windows PowerShell use `Copy-Item "$HOME\.passwordmanager\vault.json" "$HOME\.passwordmanager\vault.json.failed"` and `Copy-Item "$HOME\.passwordmanager\vault.json.bak" "$HOME\.passwordmanager\vault.json"`, then run `passwordmanager.bat list`. A wrong master password, malformed vault or failed import does not trigger a save. The `.bak` file does not help if both files are lost or the master password is forgotten. Export is **plaintext** and contains every entry password; only create it when needed and store or delete it securely.

## Development

Run tests:

```bash
./mvnw test
```

Build the runnable jar:

```bash
./mvnw clean package
java -jar target/passwordmanager-1.0.0.jar --help
```

## Tech Stack

- Java 21
- Maven
- Maven Wrapper
- picocli
- Jackson
- Bouncy Castle Argon2id
- Java Cryptography Architecture AES-GCM
- JUnit 5
- Docker / Docker Compose

## License

MIT. See [LICENSE](LICENSE).
