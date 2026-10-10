#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."
./mvnw -B clean package

version=1.0.0
archive="passwordmanager-${version}.zip"
mkdir -p dist
staging=$(mktemp -d)
trap 'rm -rf "$staging"' EXIT
mkdir -p "$staging/passwordmanager-${version}"
cp "target/passwordmanager-${version}.jar" passwordmanager.sh passwordmanager.bat LICENSE README.md README.pl.md \
    "$staging/passwordmanager-${version}/"
chmod +x "$staging/passwordmanager-${version}/passwordmanager.sh"
(
    cd "$staging"
    find "passwordmanager-${version}" -exec touch -t 202001010000 {} +
    zip -X -q -r "$OLDPWD/dist/$archive" "passwordmanager-${version}"
)
(
    cd dist
    sha256sum "$archive" > "$archive.sha256"
)
printf 'Created dist/%s and dist/%s.sha256\n' "$archive" "$archive"
