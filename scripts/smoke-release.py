#!/usr/bin/env python3
"""Smoke test a release archive outside the checkout with an isolated test vault."""

import os
import pty
import select
import shutil
import subprocess
import sys
import tempfile
import time
from pathlib import Path


def run(launcher, home, cwd, args, answers=()):
    master, slave = pty.openpty()
    env = os.environ.copy()
    env["JAVA_TOOL_OPTIONS"] = f'-Duser.home="{home}"'
    process = subprocess.Popen([str(launcher), *args], stdin=slave, stdout=slave,
                               stderr=slave, cwd=cwd, env=env, start_new_session=True)
    os.close(slave)
    output = bytearray()
    answer_index = 0
    deadline = time.monotonic() + 30
    try:
        while time.monotonic() < deadline:
            readable, _, _ = select.select([master], [], [], 0.1)
            if readable:
                try:
                    chunk = os.read(master, 4096)
                except OSError:
                    break
                if not chunk:
                    break
                output.extend(chunk)
                if answer_index < len(answers) and b"password:" in output.lower().split(b"\n")[-1]:
                    os.write(master, (answers[answer_index] + "\n").encode())
                    answer_index += 1
            if process.poll() is not None and not readable:
                break
        else:
            process.kill()
            raise RuntimeError(f"Timed out: {args}")
    finally:
        os.close(master)
    code = process.wait(timeout=5)
    text = output.decode(errors="replace")
    if code != 0 or answer_index != len(answers):
        raise RuntimeError(f"Command {args} failed ({code}; prompts {answer_index}/{len(answers)}):\n{text}")
    return text


def main():
    archive = Path(sys.argv[1]).resolve()
    with tempfile.TemporaryDirectory(prefix="passwordmanager release ") as scratch:
        scratch = Path(scratch)
        subprocess.run(["unzip", "-q", str(archive), "-d", str(scratch)], check=True)
        launcher = scratch / "passwordmanager-1.0.0" / "passwordmanager.sh"
        home = scratch / "isolated home"
        home.mkdir()
        work = scratch / "outside checkout"
        work.mkdir()
        help_output = subprocess.run([str(launcher), "--help"], cwd=work, capture_output=True,
                                     text=True, check=True).stdout
        version_output = subprocess.run([str(launcher), "--version"], cwd=work, capture_output=True,
                                        text=True, check=True).stdout
        assert "passwordmanager" in help_output.lower(), repr(help_output)
        assert "1.0.0" in version_output, repr(version_output)
        master = "synthetic-master-for-smoke"
        run(launcher, home, work, ["init"], [master])
        run(launcher, home, work, ["add", "--name", "example", "--username", "tester"],
            [master, "synthetic-entry"])
        assert "example" in run(launcher, home, work, ["list"], [master])
        run(launcher, home, work, ["update", "--name", "example", "--new-name", "renamed"], [master])
        assert "renamed" in run(launcher, home, work, ["show", "--name", "renamed"], [master])
        vault = home / ".passwordmanager" / "vault.json"
        backup = vault.with_name("vault.json.bak")
        assert vault.is_file() and backup.is_file()
        current = work / "vault.after-update"
        shutil.copy2(vault, current)
        shutil.copy2(backup, vault)
        assert "example" in run(launcher, home, work, ["list"], [master])
        shutil.copy2(current, vault)
        exported = work / "plaintext-export.json"
        run(launcher, home, work, ["export", "--output", str(exported)], [master])
        assert "synthetic-entry" in exported.read_text()
        run(launcher, home, work, ["remove", "--name", "renamed"], [master])
        run(launcher, home, work, ["import", "--input", str(exported)], [master])
        assert "renamed" in run(launcher, home, work, ["list"], [master])
        print("Release smoke passed: help, version, full entry flow, export/import, backup recovery.")


if __name__ == "__main__":
    main()
