# Research

Date: 2026-05-06

This document summarizes the security and technology research used to plan the
local password manager project.

## Security Sources

### OWASP Password Storage Cheat Sheet

Relevant conclusions:

- Prefer Argon2id for password-based derivation/hashing.
- OWASP lists Argon2id minimum parameters such as 19 MiB memory, 2 iterations
  and 1 degree of parallelism.
- If Argon2id is not available, scrypt is the next good option.
- PBKDF2-HMAC-SHA256 is acceptable in FIPS-oriented environments, with high
  iteration counts.
- Work factors must be benchmarked and stored with the resulting protected data,
  so they can be upgraded later.

Source:
https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html

### OWASP Cryptographic Storage Cheat Sheet

Relevant conclusions:

- Do not invent custom cryptographic algorithms.
- For symmetric encryption, prefer AES with secure key sizes and secure modes.
- Use authenticated encryption modes where available, especially GCM or CCM.
- Do not hard-code keys or commit keys to version control.
- Key management should be treated as a first-class design problem.

Source:
https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html

### NIST SP 800-38D

Relevant conclusion:

- AES-GCM is an authenticated encryption mode that provides confidentiality and
  authentication/integrity for encrypted data.

Source:
https://csrc.nist.gov/pubs/sp/800/38/d/final

## Java Platform Sources

### Java `GCMParameterSpec`

Relevant conclusion:

- Java's GCM API requires an IV and authentication tag length. AAD, key,
  plaintext/ciphertext and authentication tag are handled through `Cipher`.

Source:
https://docs.oracle.com/en/java/javase/17/docs/api/java.base/javax/crypto/spec/GCMParameterSpec.html

### Java `SecureRandom`

Relevant conclusion:

- `SecureRandom` is the JDK API intended for cryptographically strong random
  bytes. It should be used for salts, nonces and generated passwords.

Source:
https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/security/SecureRandom.html

### Java `Console.readPassword`

Relevant conclusion:

- Password input should use `readPassword()` where possible, and returned
  `char[]` values should be manually cleared after use.

Source:
https://docs.oracle.com/en/java/javase/26/docs/api/java.base/java/io/Console.html

## Library Sources

### picocli

Relevant conclusions:

- picocli is a mature Java CLI framework.
- It supports subcommands, typed options and interactive password input.
- Interactive password options can use `char[]`, which matches the memory
  hygiene direction from the Java Console API.

Source:
https://picocli.info/

### Bouncy Castle

Relevant conclusion:

- Bouncy Castle provides Java cryptographic APIs and current `bcprov-jdk18on`
  artifacts. It gives us an Argon2id implementation without writing a KDF.

Source:
https://www.bouncycastle.org/documentation/documentation-java/

Current Maven artifact checked:
https://repo.maven.apache.org/maven2/org/bouncycastle/bcprov-jdk18on/1.84/

### Jackson

Relevant conclusion:

- Jackson remains a standard JSON library for Java and is suitable for
  serializing the vault envelope and encrypted payload model.

Current Maven artifact checked:
https://repo.maven.apache.org/maven2/com/fasterxml/jackson/core/jackson-databind/

## Decision Summary

Use Java 21, Maven, picocli, Jackson, Bouncy Castle Argon2id, JDK AES-GCM and
JDK `SecureRandom`.

The project should avoid marketing itself as production-ready security software.
The correct positioning is:

```text
Educational local password vault demonstrating secure design practices.
Not audited. Not recommended as a replacement for a mature password manager.
```
