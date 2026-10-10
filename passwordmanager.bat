@echo off
REM Launcher for the release archive and for a local Maven build.

SET SCRIPT_DIR=%~dp0
SET "JAR_PATH=%SCRIPT_DIR%passwordmanager-1.0.0.jar"
IF NOT EXIST "%JAR_PATH%" SET "JAR_PATH=%SCRIPT_DIR%target\passwordmanager-1.0.0.jar"

IF NOT EXIST "%JAR_PATH%" (
    echo Error: Jar file not found at %JAR_PATH%
    echo Build with mvnw.cmd clean package or use the complete release archive.
    exit /b 1
)

java -jar "%JAR_PATH%" %*
