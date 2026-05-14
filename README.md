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

## Quick Start

Requirements:

- Java 21 or newer
- Maven is optional because the repository includes Maven Wrapper

Build and run from the repository root:

```bash
./mvnw clean package
./passwordmanager.sh --help
```

### Add the Launcher to PATH

The launchers expect the project to be built first with `./mvnw clean package`.

Linux:

```bash
mkdir -p ~/.local/bin
ln -sf "$(pwd)/passwordmanager.sh" ~/.local/bin/passwordmanager
```

If `~/.local/bin` is not in your `PATH`, add this to `~/.bashrc` or `~/.zshrc`:

```bash
export PATH="$HOME/.local/bin:$PATH"
```

macOS:

```bash
sudo ln -sf "$(pwd)/passwordmanager.sh" /usr/local/bin/passwordmanager
```

Windows PowerShell:

```powershell
$project = (Get-Location).Path
[Environment]::SetEnvironmentVariable(
  "Path",
  [Environment]::GetEnvironmentVariable("Path", "User") + ";$project",
  "User"
)
```

After opening a new terminal, Windows can run `passwordmanager` through `passwordmanager.bat`.

Full installation notes are in [docs/installation.md](docs/installation.md).

## Demo

![Password Manager CLI terminal demo](docs/assets/passwordmanager-demo.gif)

Copy-paste demo flow:

```bash
./mvnw clean package
./passwordmanager.sh init
./passwordmanager.sh add --name github --username user@example.com --url https://github.com
./passwordmanager.sh list
./passwordmanager.sh show --name github
./passwordmanager.sh update --name github --prompt-password
./passwordmanager.sh export --output backup.json
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
- A local CLI cannot protect against malware, keyloggers, screen capture or a privileged attacker on the same machine.
- This project is educational software used at your own risk.

## Development

Run tests:

```bash
./mvnw test
```

Build the runnable jar:

```bash
./mvnw clean package
java -jar target/passwordmanager-0.1.0-SNAPSHOT.jar --help
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

## License

MIT. See [LICENSE](LICENSE).
