# Installation and PATH Setup

This project is educational software. It is not professionally audited and is not a replacement for mature production password managers. Use it at your own risk.

## Release archive (Java 21 or newer)

Download `passwordmanager-1.0.0.zip` and `passwordmanager-1.0.0.zip.sha256` from the [GitHub release](https://github.com/wikt0r2115/passwordmanager/releases/tag/v1.0.0). On Linux, verify with `sha256sum -c passwordmanager-1.0.0.zip.sha256`; on macOS, use `shasum -a 256 -c passwordmanager-1.0.0.zip.sha256`. On Windows PowerShell compare `(Get-FileHash .\passwordmanager-1.0.0.zip -Algorithm SHA256).Hash.ToLower()` with the hash in the `.sha256` file. Extract the ZIP and run the launcher beside the JAR, from any working directory. The release archive includes the JAR, both launchers, license and READMEs. It does not need Maven.

Linux/macOS: `unzip passwordmanager-1.0.0.zip` then `./passwordmanager-1.0.0/passwordmanager.sh --help`. Windows: `Expand-Archive .\passwordmanager-1.0.0.zip`, then `.\passwordmanager-1.0.0\passwordmanager.bat --help`. Run `init` from a real terminal to create the vault and set a master password. The program cannot read a password prompt from a non-interactive pipe.

## Build from source

From the repository root:

```bash
./mvnw clean package
```

On Windows PowerShell:

```powershell
.\mvnw.cmd clean package
```

The launcher scripts use:

```text
  passwordmanager-1.0.0.jar (release), or target/passwordmanager-1.0.0.jar (source checkout)
```

Rebuild the project after code changes so the launcher runs the latest version.

## Linux

Create a user-local bin directory:

```bash
mkdir -p ~/.local/bin
```

Create a `passwordmanager` symlink that points to the repository launcher:

```bash
ln -sf "$(pwd)/passwordmanager.sh" ~/.local/bin/passwordmanager
```

Make sure the script is executable:

```bash
chmod +x passwordmanager.sh
```

If `~/.local/bin` is not already in `PATH`, add this line to `~/.bashrc` or `~/.zshrc`:

```bash
export PATH="$HOME/.local/bin:$PATH"
```

Reload the shell:

```bash
source ~/.bashrc
```

For zsh users:

```bash
source ~/.zshrc
```

Verify:

```bash
passwordmanager --help
```

## macOS

Make sure the script is executable:

```bash
chmod +x passwordmanager.sh
```

Create a symlink in `/usr/local/bin`:

```bash
sudo ln -sf "$(pwd)/passwordmanager.sh" /usr/local/bin/passwordmanager
```

If `/usr/local/bin` is not in `PATH`, add this line to `~/.zshrc`:

```bash
export PATH="/usr/local/bin:$PATH"
```

Reload the shell:

```bash
source ~/.zshrc
```

Verify:

```bash
passwordmanager --help
```

## Windows

Use the included launcher:

```text
passwordmanager.bat
```

The easiest option is to add the project directory to the user `Path`.

PowerShell from the repository root:

```powershell
$project = (Get-Location).Path
[Environment]::SetEnvironmentVariable(
  "Path",
  [Environment]::GetEnvironmentVariable("Path", "User") + ";$project",
  "User"
)
```

Open a new PowerShell or Command Prompt window and verify:

```powershell
passwordmanager --help
```

Manual Windows setup:

1. Open Start Menu and search for `Environment Variables`.
2. Open `Edit the system environment variables`.
3. Click `Environment Variables`.
4. Under user variables, select `Path` and click `Edit`.
5. Add the full path to the project directory.
6. Open a new terminal.
7. Run `passwordmanager --help`.

Windows resolves `passwordmanager` to `passwordmanager.bat` through the default `PATHEXT` setting.

## Notes

- The launcher first looks for the release JAR beside itself, then for the source-build JAR in `target/`.
- Moving the archive or repository requires recreating a symlink or updating `Path` if you set one up.
- The vault itself is stored separately at `~/.passwordmanager/vault.json`.
- Successful writes keep the prior encrypted vault at `~/.passwordmanager/vault.json.bak`. Stop the CLI and copy the backup over `vault.json` to recover; keep the master password. See the README for exact commands.
- A second concurrent writer exits with code 1. Do not delete `vault.json.lock` while commands may run.
- Exported JSON files are plaintext and contain passwords. Treat them as sensitive data.
