# Audyt konfiguracji i stałych — 10.11.2026

Przejrzano całą produkcyjną część Java: **166 plików**. Wiadomości, opisy komend, walidacja, diagnostyka, prezentacja historii i etykiety korzystają z `lang.yml`. Wygląd siedmiu menu pochodzi z nowego `gui.yml`. Parametry działania i wartości domyślne znajdują się w `config.yml`, `storage.yml`, `plot-types.yml` i `districts.yml`.

## Gdzie edytować

| Zakres | Plik / sekcja |
| :--- | :--- |
| Czat, pomoc, użycie komend, odmowy, błędy usług i logi pluginu | `lang.yml`: dotychczasowe klucze oraz `text.<klasa>.<wiadomość>` |
| Nazwy statusów, ról, praw, jednostek najmu, sortowania i kategorii historii | `lang.yml`: `labels` |
| Format kwot i czasu względnego, brak wartości | `lang.yml`: `format`, `labels.none` |
| Tytuły, rozmiary, sloty, materiały, nazwy, lore i wypełnienie menu | `gui.yml`: `menus` |
| Liczba pozycji na stronie GUI | `gui.yml`: `content-slots` |
| Poświata, liczba przedmiotów i model zasobu | `gui.yml`: `glow`, `amount`, `custom-model-data` przy przedmiocie |
| Precyzja kwot, zaokrąglenie, limit transakcji | `config.yml`: `economy` |
| Progi powierzchni i mnożniki podatku | `config.yml`: `tax` |
| Domyślne opłaty i parametry nowej działki | `config.yml`: `economy`, `plots`; profile w `plot-types.yml` |
| Domyślne okresy opłat i najmu, auto-odnowienie, długość jednostek | `config.yml`: `billing`, `rental`, `rental-period-seconds` |
| Strony historii, audytu, wiadomości i kolejek finansowych | `config.yml`: `paging` |
| Cztery wiersze i format tabliczek | `config.yml`: `signs.layouts`, `signs.formats` |
| Zasięg wskazania tabliczki, priorytet regionu WG | `config.yml`: `interaction`, `worldguard` |
| Cząsteczki granic, kolor, rozmiar, czas i częstotliwość | `config.yml`: `visualization` |
| Nazwy dźwięków, głośność, wysokość i czas tytułów | `config.yml`: `sounds`, `notifications` |
| Opóźnienia ponawiania zwrotów | `config.yml`: `refunds` |
| Opóźnienie startu schedulera, indeks przestrzenny, zamykanie I/O | `config.yml`: `performance` |
| Format dat, strefa, lokalizacja i grupowanie kwot | `config.yml`: `formatting` |
| Początkowe prawa członków i najemców | `config.yml`: `members.default-permissions`, `rental.default-tenant-permissions` |
| Podpowiedzi liczbowe komend | `config.yml`: `tab-completion` |
| Początkowa zakładka i sortowanie rynku | `gui.yml`: `menus.market.default-tab`, `menus.market.default-sort` |
| Wartości domyślne własnych profili dzielnic | `config.yml`: `district-defaults` |

Katalog języka zawiera **777 szablonów i etykiet**. Brakujące klucze są uzupełniane w pamięci z zasobów JAR-a, bez nadpisywania plików administratora. Pusty tekst pozostaje pusty. Nowy `gui.yml` powstaje przy uruchomieniu. Zmiany stosuje `/plots admin reload`; już otwarte ekwipunki należy otworzyć ponownie.

## Kontrola źródeł

```powershell
python scripts/check_configuration.py
```

Wymaga JDK 25 i Pythona 3.10+. Nie instaluje dodatkowych pakietów i nie zmienia kodu. Parser kompilatora Java zbiera literały, konkatenacje i wywołania także wtedy, gdy tekst jest budowany z wielu fragmentów. Kontrola działa również w przygotowanym workflow CI.

W `build/reports/configuration/` powstają:

| Raport | Zawartość |
| :--- | :--- |
| `java-expressions.jsonl` | Pełne wyrażenia z plikiem, wierszem, metodą i kontekstem AST. |
| `literal-inventory.csv` | Wszystkie pozostałe literały tekstowe i liczbowe wraz z klasyfikacją do przeglądu. |
| `message-migration.json` | Kandydaci na teksty do przeniesienia; po refaktoryzacji pusta lista. |
| `summary.json` | Liczba plików, szablonów i naruszeń. |

Stan po zmianach: **2 979 literałów tekstowych, 948 liczbowych, 0 kandydatów tekstów do migracji, 0 naruszeń reguł kontroli**. Są to wystąpienia w AST, a nie liczba wiadomości ani nieunikalnych wartości. Klucze `Texts.raw("...")` również liczą się jako literały. Klasyfikacja CSV jest pomocniczą heurystyką; wynik automatu uzupełniają przegląd kodu i testy zachowania.

`scripts/extract_messages.py` jest narzędziem migracyjnym. Bez `--apply` wyłącznie przygotowuje plan. Przed użyciem `--apply` należy wygenerować aktualny AST i przejrzeć plan; skrypt sprawdza zgodność pozycji źródła przed zapisaniem plików.

## Stałe pozostawione w kodzie

| Rodzaj | Przykłady i powód |
| :--- | :--- |
| Tożsamość i protokół | Nazwy komend, uprawnienia, identyfikatory akcji GUI, klucze PDC, enumy i ID regionów. Zmiana oznacza zmianę API lub migrację danych. Widoczne etykiety enumów edytuje się oddzielnie. |
| Format przechowywania | SQL, nazwy tabel/kolumn, pozycje parametrów JDBC, wersje schematu, nazwy pól zapisanych umów. Nie są szablonami do wykonania z konfiguracji. |
| Zgodność starych danych | Domyślne wartości odczytu brakujących pól w starych rekordach, w tym `RESIDENTIAL`, `DAY`, dawne okresy i zwrot 50%. Chronią warunki istniejących nieruchomości przed zmianą przez nową konfigurację. |
| Matematyka i geometria | 20 ticków/s, 60 sekund/min, 100 w przeliczaniu procentów, chunki 16×16, środek bloku `0.5`, indeks pierwszej strony `0`, 12 krawędzi prostopadłościanu. |
| Ograniczenia API | Ekwipunek 9–54 slotów w rzędach po 9, cztery wiersze tabliczki, SQLite z jednym połączeniem, zera jako brak właściciela/nieaktywne rozliczenie. |
| Ograniczenia bezpieczeństwa | Kwoty do `999999999999.99` i maksymalnie 8 miejsc dziesiętnych, strony do 100 wpisów, dodatnie okresy, maksymalnie 3650 okresów najmu, granice 24–240 punktów/3–30 s i cooldown 5–120 s, limit wykładnika ponawiania zwrotu. Konfiguracja wybiera wartości wewnątrz tych zakresów. |

Komunikaty generowane przez Paper, WorldEdit, WorldGuard, Vault, bibliotekę JDBC lub JVM pochodzą z tych zależności. `lang.yml` obejmuje teksty tworzone przez riftPlots. Zapisane wcześniej powiadomienia i opisy transakcji zachowują treść z chwili utworzenia; reload nie przepisuje historii.

## Weryfikacja

- **119 testów Java**: brak błędów i pominięć, rzeczywiste MySQL 8.4.10, YAML i SQLite oraz natywny SQLite z gotowego JAR-a.
- Testy ustawień obejmują przesunięte przyciski, zmianę materiału, brak znacznika akcji, kolizje slotów, niewłaściwe typy YAML, domyślne klucze, placeholdery, znaki polskie, własne progi podatku, długość najmu, precyzję kwot i układ tabliczek.
- Scenariusz izolowanego Paper sprawdza menu przy rzeczywistym API Bukkit, restart, reset, kopię i odtworzenie danych. Wyniki scenariusza i dokumentacji opisuje [Wiki](wiki/Rozwoj-i-testy.md).

Praktyczne przykłady: [konfiguracja ogólna](wiki/Konfiguracja.md) i [konfiguracja GUI](wiki/Konfiguracja-GUI.md).
