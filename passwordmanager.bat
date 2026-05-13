@echo off
REM Professional launcher for Password Manager CLI (Windows)
REM Works from any directory

SET SCRIPT_DIR=%~dp0
SET JAR_PATH=%SCRIPT_DIR%target\passwordmanager-0.1.0-SNAPSHOT.jar

IF NOT EXIST "%JAR_PATH%" (
    echo Error: Jar file not found at %JAR_PATH%
    echo Please run 'mvn clean package' first.
    exit /b 1
)

java -jar "%JAR_PATH%" %*
