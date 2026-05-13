#!/bin/bash

# Professional launcher for Password Manager CLI
# Works from any directory and resolves symlinks

# Get the absolute path of the script, resolving symlinks
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do
  DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
done
SCRIPT_DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

JAR_PATH="$SCRIPT_DIR/target/passwordmanager-0.1.0-SNAPSHOT.jar"

if [ ! -f "$JAR_PATH" ]; then
    echo "Error: Jar file not found at $JAR_PATH"
    echo "Please run 'mvn clean package' in $SCRIPT_DIR first."
    exit 1
fi

java -jar "$JAR_PATH" "$@"
