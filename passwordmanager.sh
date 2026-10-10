#!/bin/bash

# Launcher for the release archive and for a local Maven build.

# Get the absolute path of the script, resolving symlinks
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do
  DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
done
SCRIPT_DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

JAR_PATH="$SCRIPT_DIR/passwordmanager-1.0.0.jar"
if [ ! -f "$JAR_PATH" ]; then
    JAR_PATH="$SCRIPT_DIR/target/passwordmanager-1.0.0.jar"
fi

if [ ! -f "$JAR_PATH" ]; then
    echo "Error: Jar file not found at $JAR_PATH"
    echo "Build with './mvnw clean package' or use the complete release archive."
    exit 1
fi

exec java -jar "$JAR_PATH" "$@"
