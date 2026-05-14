# Installation and PATH Setup

This project is educational software. It is not professionally audited and is not a replacement for mature production password managers. Use it at your own risk.

## Build First

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
target/passwordmanager-0.1.0-SNAPSHOT.jar
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

- The launcher points to the jar inside the repository `target/` directory.
- Moving the repository requires recreating the symlink or updating `Path`.
- The vault itself is stored separately at `~/.passwordmanager/vault.json`.
- Exported JSON files are plaintext and contain passwords. Treat them as sensitive data.
