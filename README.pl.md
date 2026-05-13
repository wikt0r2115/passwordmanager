# Password Manager CLI 🛡️

Lokalny, bezpieczny menedżer haseł CLI zbudowany w Javie 21. Stworzony dla użytkowników, którzy chcą mieć pełną kontrolę nad swoimi danymi bez polegania na usługach w chmurze.

> **Uwaga:** Jest to projekt edukacyjny. Mimo zastosowania najlepszych praktyk bezpieczeństwa, kod nie przeszedł profesjonalnego audytu.

## ✨ Funkcje

- **Bezpieczne Przechowywanie:** Wszystkie wpisy są zapisane w lokalnie zaszyfrowanym pliku JSON.
- **Silne Szyfrowanie:** Wykorzystuje AES-256-GCM do szyfrowania danych i Argon2id do wyprowadzania klucza.
- **Hasło Master:** Twoje hasło główne nigdy nie jest zapisywane – służy wyłącznie do generowania klucza szyfrującego.
- **Automatyczne Backupy:** Tworzy plik `.bak` automatycznie przed każdą zmianą w sejfie.
- **Generator Haseł:** Tworzy silne, losowe hasła z możliwością konfiguracji (symbole, cyfry itp.).
- **Operacje CRUD:** Łatwe dodawanie, listowanie, wyświetlanie, aktualizowanie i usuwanie wpisów.
- **Eksport/Import:** Backup wpisów do jawnego pliku JSON lub migracja z innych narzędzi.

## 🚀 Szybki Start

### Wymagania
- Java 21 lub nowsza
- Maven

### Instalacja
1. Sklonuj repozytorium:
   ```bash
   git clone https://github.com/TWOJ_USERNAME/passwordmanager.git
   cd passwordmanager
   ```
2. Zbuduj projekt:
   ```bash
   mvn clean package
   ```
3. Uruchom aplikację:
   ```bash
   ./passwordmanager.sh --help
   ```

## 🛠️ Komendy

| Komenda | Opis |
| :--- | :--- |
| `init` | Inicjalizacja nowego zaszyfrowanego sejfu. |
| `add` | Dodanie nowego wpisu (obsługuje `--generate`). |
| `list` | Listowanie nazw i użytkowników wszystkich wpisów. |
| `show` | Wyświetlenie pełnych danych (w tym hasła) konkretnego wpisu. |
| `update` | Aktualizacja wpisu (zmiana pól lub rotacja hasła). |
| `remove` | Stałe usunięcie wpisu. |
| `export` | Eksport wpisów do jawnego pliku JSON. |
| `import` | Scalanie wpisów z pliku JSON do aktualnego sejfu. |
| `generate`| Generowanie silnego losowego hasła. |

## 🔒 Bezpieczeństwo

Projekt został stworzony z myślą o bezpieczeństwie danych:

- **Wyprowadzanie Klucza:** Argon2id (Bouncy Castle) chroni przed atakami brute-force i słownikowymi.
- **Szyfrowanie:** AES-256 w trybie GCM zapewnia poufność oraz autentyczność danych (AEAD).
- **AAD (Additional Authenticated Data):** Metadane sejfu (wersja, format) są chronione tagiem autentyczności GCM przed manipulacją.
- **Bezpieczeństwo Pamięci:** Wrażliwe dane (`char[]`) są jawnie zerowane w pamięci (`Arrays.fill`) po użyciu.
- **Zapis Atomiczny:** Zmiany są zapisywane przez plik tymczasowy i operację `move`, co zapobiega uszkodzeniu sejfu przy awarii.

## 📦 Stack Technologiczny

- **Język:** Java 21 (LTS)
- **Framework CLI:** [picocli](https://picocli.info/)
- **Kryptografia:** JCA (AES-GCM), Bouncy Castle (Argon2id)
- **Serializacja JSON:** Jackson
- **Testy:** JUnit 5

## 📄 Licencja

Projekt jest dostępny na licencji MIT – szczegóły w pliku [LICENSE](LICENSE).
