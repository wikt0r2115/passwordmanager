# Password Manager

[English version](README.md)

Lokalny edukacyjny password manager CLI planowany jako kolejny projekt
portfolio po `studytracker`.

Repozytorium zawiera obecnie dokumentację, decyzje architektoniczne i pustą
strukturę projektu. Nie ma jeszcze kodu aplikacji.

## Cel Projektu

Celem jest zbudowanie lokalnego vaulta na hasła, który zapisuje dane logowania w
zaszyfrowanym pliku na komputerze użytkownika. Projekt ma pokazać ostrożne
projektowanie, testowalność i praktyczne podejście do bezpieczeństwa, ale nie ma
konkurować z audytowanymi produkcyjnymi password managerami.

## Zakres V1

- Inicjalizacja lokalnego zaszyfrowanego vaulta.
- Odblokowanie vaulta master passwordem.
- Dodawanie, listowanie, podgląd i usuwanie wpisów.
- Generowanie losowych haseł.
- Zapis jednego lokalnego pliku vaulta.
- Oddzielenie CLI od domeny, kryptografii i persistence.

## Poza Zakresem V1

- Rozszerzenie przeglądarki.
- Synchronizacja z chmurą.
- GUI.
- Vault współdzielony albo zespołowy.
- Integracja ze schowkiem.
- Sprawdzanie wycieków haseł przez zewnętrzne API.
- Deklaracje produkcyjnego bezpieczeństwa albo audytu.

## Wybrane Technologie

- Java 21 LTS.
- Maven.
- picocli do parsowania komend CLI.
- Jackson do serializacji JSON.
- Bouncy Castle do Argon2id.
- Java Cryptography Architecture do AES-GCM i `SecureRandom`.
- JUnit 5 do testów.
- GitHub Actions do CI, gdy zacznie się implementacja.

Dokumenty projektowe:

- [Research](docs/research.md)
- [Zakres](docs/scope.md)
- [Architektura](docs/architecture.md)
- [Security Design](docs/security-design.md)
- [Format Vaulta](docs/vault-format.md)
- [Strategia Testów](docs/testing-strategy.md)
- [Roadmapa](docs/roadmap.md)

## Planowany Kształt CLI

```text
passwordmanager init
passwordmanager add <name>
passwordmanager list
passwordmanager show <name>
passwordmanager remove <name>
passwordmanager generate
passwordmanager change-master
```

Nazwy komend mogą się jeszcze zmienić podczas implementacji.

## Aktualny Status

```text
Status: planowanie / sam szkielet
Kod: jeszcze niezaimplementowany
```

Następny krok to pierwszy cienki pionowy wycinek:

1. `passwordmanager init`
2. utworzenie pliku vaulta
3. prompt o master password
4. zapis zaszyfrowanego pustego vaulta
5. testy inicjalizacji vaulta
