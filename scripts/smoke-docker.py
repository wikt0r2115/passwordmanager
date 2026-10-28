#!/usr/bin/env python3
"""Exercise the Docker demo with a real PTY and an isolated Compose volume (Linux)."""

import errno
import os
import pty
import re
import select
import signal
import subprocess
import time
import uuid
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
# Reuse the built image; isolate the test containers and volume from the demo project.
PROJECT = "pm-smoke-" + uuid.uuid4().hex[:12]
COMPOSE = ["docker", "compose", "-p", PROJECT, "-f", str(ROOT / "compose.yaml")]


def terminal(command, answers=(), expected_code=0):
    master, slave = pty.openpty()
    process = subprocess.Popen(command, stdin=slave, stdout=slave, stderr=slave,
                               cwd=ROOT, start_new_session=True)
    os.close(slave)
    output = bytearray()
    answered = 0
    scanned = 0
    deadline = time.monotonic() + 60
    try:
        while time.monotonic() < deadline:
            readable, _, _ = select.select([master], [], [], 0.1)
            if readable:
                try:
                    chunk = os.read(master, 8192)
                except OSError as error:
                    if error.errno == errno.EIO:
                        break
                    raise
                if not chunk:
                    break
                output.extend(chunk)
                prompt = re.search(rb"Type (?:master|entry|new entry) password: ", output[scanned:])
                if prompt:
                    scanned += prompt.end()
                    if answered >= len(answers):
                        raise RuntimeError("Unexpected password prompt")
                    os.write(master, (answers[answered] + "\n").encode())
                    answered += 1
            elif process.poll() is not None:
                break
        else:
            raise TimeoutError("Docker CLI command timed out")
        code = process.wait(timeout=5)
    finally:
        if process.poll() is None:
            os.killpg(process.pid, signal.SIGKILL)
            process.wait(timeout=5)
        os.close(master)
    text = output.decode(errors="replace")
    if code != expected_code or answered != len(answers):
        # Test credentials are synthetic; still keep them out of failure logs.
        for answer in answers:
            text = text.replace(answer, "[test secret]")
        raise RuntimeError(f"Command failed (exit {code}, prompts {answered}/{len(answers)}):\n{text}")
    return text


def run(args, answers=(), expected_code=0):
    return terminal([*COMPOSE, "run", "--rm", "--no-deps", "passwordmanager", *args],
                    answers, expected_code)


def main():
    subprocess.run(["docker", "image", "inspect", "passwordmanager:local"],
                   check=True, stdout=subprocess.DEVNULL)
    master_password = "synthetic-docker-master"
    entry_password = "synthetic-docker-entry"
    try:
        assert "passwordmanager" in run(["--help"]).lower()
        assert "1.0.0" in run(["--version"])
        # Each call starts a fresh container. Only the named vault volume persists.
        run(["init"], [master_password])
        added = run(["add", "--name", "docker-example", "--username", "tester"],
                    [master_password, entry_password])
        assert master_password not in added and entry_password not in added, "Password prompt echoed input"
        assert "docker-example" in run(["list"], [master_password])
        shown = run(["show", "--name", "docker-example"], [master_password])
        assert "Username: tester" in shown and f"Password: {entry_password}" in shown
        run(["update", "--name", "docker-example", "--new-name", "docker-renamed"], [master_password])
        failed = run(["show", "--name", "docker-renamed"], ["incorrect-master"], expected_code=1)
        assert "Vault unlock failed" in failed and entry_password not in failed
        assert "docker-renamed" in run(["list"], [master_password])
        # Inspect as the same non-root user to check both ownership and backup persistence.
        check = subprocess.check_output([
            *COMPOSE, "run", "--rm", "--no-deps", "-T", "--entrypoint", "sh", "passwordmanager",
            "-c", "test $(id -u) -ne 0 && test -s /data/.passwordmanager/vault.json "
            "&& test -s /data/.passwordmanager/vault.json.bak && echo volume-and-backup-ok"
        ], cwd=ROOT, text=True)
        assert "volume-and-backup-ok" in check
        run(["remove", "--name", "docker-renamed"], [master_password])
        assert "docker-renamed" not in run(["list"], [master_password])
        print("Docker smoke passed: hidden prompts, persistent vault, CRUD, backup, wrong password, non-root user.")
    finally:
        # Only this UUID-named test project is removed, never the user's demo project.
        subprocess.run([*COMPOSE, "down", "--volumes", "--remove-orphans"], cwd=ROOT, check=True)


if __name__ == "__main__":
    main()
