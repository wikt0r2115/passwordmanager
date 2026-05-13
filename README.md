# Password Manager CLI 🛡️

[![Java CI with Maven](https://github.com/YOUR_GITHUB_USERNAME/passwordmanager/actions/workflows/maven.yml/badge.svg)](https://github.com/YOUR_GITHUB_USERNAME/passwordmanager/actions/workflows/maven.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

A cryptographically secure, local password manager CLI built with Java 21. Designed for users who want full control over their credentials without relying on cloud services.

> **Note:** This is an educational project. While it follows security best practices, it has not been professionally audited.

## ✨ Features

- **Secure Storage:** All entries are stored in a locally encrypted JSON vault.
- **Strong Encryption:** Uses AES-256-GCM for data encryption and Argon2id for key derivation.
- **Master Password:** Your master password is never stored and is used only to derive the encryption key.
- **Automatic Backups:** Creates a `.bak` file automatically before every vault update.
- **Password Generator:** Generate strong, random passwords with configurable requirements (symbols, digits, etc.).
- **CRUD Operations:** Easily add, list, show, update, and remove password entries.
- **Export/Import:** Backup your entries to plaintext JSON or migrate from other tools.

## 🚀 Quick Start

### Prerequisites
- Java 21 or higher
- Maven

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/YOUR_GITHUB_USERNAME/passwordmanager.git
   cd passwordmanager
   ```
2. Build the project:
   ```bash
   mvn clean package
   ```
3. Run the application:
   ```bash
   ./passwordmanager.sh --help
   ```

## 🛠️ Commands

| Command | Description |
| :--- | :--- |
| `init` | Initialize a new encrypted vault. |
| `add` | Add a new password entry (supports `--generate`). |
| `list` | List names and usernames of all entries. |
| `show` | Show full details (including password) of a specific entry. |
| `update` | Update an existing entry (supports field updates and password rotation). |
| `remove` | Permanently remove an entry. |
| `export` | Export entries to a plaintext JSON file. |
| `import` | Merge entries from a plaintext JSON file into the vault. |
| `generate`| Generate a standalone strong random password. |

## 🔒 Security Design

This project is built with a "security-first" mindset:

- **Key Derivation:** Argon2id (Bouncy Castle) is used to derive a 256-bit key from your master password, protecting against brute-force and dictionary attacks.
- **Encryption:** AES-256 in Galois/Counter Mode (GCM) provides both confidentiality and authenticity (AEAD).
- **AAD (Additional Authenticated Data):** Critical vault metadata (version, format) is included in the GCM authentication tag to prevent tampering.
- **Memory Safety:** Sensitive data (`char[]` for passwords) is explicitly wiped from memory (`Arrays.fill`) after use.
- **Atomic Writes:** Vault updates are performed using a temporary file and atomic move operation to prevent data corruption during crashes.

## 📦 Tech Stack

- **Language:** Java 21 (LTS)
- **CLI Framework:** [picocli](https://picocli.info/)
- **Cryptography:** JCA (AES-GCM), Bouncy Castle (Argon2id)
- **JSON Serialization:** Jackson
- **Testing:** JUnit 5

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
