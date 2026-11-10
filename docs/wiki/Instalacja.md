# Instalacja

[← Strona główna](Home.md) · **Następny krok:** [Pierwsza działka](Szybki-start.md)

## Zanim rozpoczniesz

riftPlots korzysta z ekonomii serwera, regionów WorldGuard i schematów WorldEdit. Wszystkie te elementy muszą być dostępne, zanim zaczniesz udostępniać nieruchomości.

| Element | Wymaganie wynikające z projektu |
| :--- | :--- |
| Serwer | Paper 26.3 — wersja API zadeklarowana w `plugin.yml`. |
| Java | Java 25; do samodzielnego budowania potrzebujesz JDK 25. |
| WorldGuard | Wymagany plugin; kod kompilowany z `7.0.12`. |
| WorldEdit | Wymagany plugin; kod kompilowany z `7.3.10`. |
| Vault | Wymagany plugin i zarejestrowany provider ekonomii. |
| Baza | Domyślnie lokalne SQLite; MySQL jest opcjonalny. |

Wersje kompilacyjne nie są obietnicą zgodności dowolnego zestawu pluginów. Sprawdź docelowy serwer testowy, zwłaszcza współpracę wersji WorldEdit i WorldGuard z wybranym Paper.

Próbę integracyjną P1–P2 wykonano na **Paper 26.3 build 246, WorldEdit 7.4.6, WorldGuard 7.0.21 i Vault 1.7.3**, z kontrolowaną ekonomią testową. API Paper jest przypięte do `26.3.build.246-stable`. [Zakres testu i narzędzia](Nowosci-P1-P2.md#migracja-testy-i-backup).

## 1. Przygotuj JAR

Jeżeli budujesz z kodu źródłowego, otwórz terminal w katalogu projektu:

```powershell
# Windows
.\gradlew.bat build
```

```bash
# Linux / macOS
./gradlew build
```

Artefakt do instalacji to `build/libs/riftPlots-1.0.0.jar`. Jest to Shadow JAR zawierający HikariCP i sterowniki baz. Nie trzeba instalować ich jako osobnych pluginów.

## 2. Umieść pluginy na serwerze

W katalogu `plugins/` powinny znaleźć się riftPlots, WorldEdit, WorldGuard, Vault oraz wybrany plugin dostarczający ekonomię. Sam Vault jest mostem integracyjnym — nie tworzy gospodarki ani sald graczy.

Uruchom serwer normalnie. Jeżeli wgrywasz nową wersję na działający serwer, wykonaj kontrolowany restart zamiast ładowania jej menedżerem pluginów w trakcie gry.

## 3. Sprawdź pierwszy start

W konsoli powinien pojawić się komunikat o włączeniu riftPlots i liczbie wczytanych działek. Przy pierwszej instalacji liczba może wynosić zero.

Po uruchomieniu powstaje katalog:

```text
plugins/riftPlots/
├── config.yml
├── storage.yml
├── lang.yml
├── plot-types.yml
├── districts.yml
├── data/
└── schematics/
```

Bez wymaganego WorldEdit, WorldGuard lub dostępnego providera ekonomii plugin nie powinien rozpocząć normalnej pracy. Diagnozę zaczynaj od pierwszego błędu w konsoli, a nie od kolejnych komunikatów zależnych.

## 4. Wybierz magazyn

Domyślnie w `storage.yml` znajduje się `storage.type: "SQLITE"`. To lokalny plik `data/riftplots.db`; nie wymaga przygotowania zewnętrznego serwera bazy.

Jeżeli chcesz użyć MySQL, najpierw utwórz bazę i skonfiguruj połączenie. **Zmiana `storage.type` nie kopiuje istniejących danych.** Dla działającego miasta zastosuj [procedurę migracji](Migracja-i-kopie-zapasowe.md).

## 5. Zweryfikuj uprawnienia

Gracze mają domyślnie podstawowy węzeł `riftplots.user` i jego dzieci. Administracja wymaga `riftplots.admin`, domyślnie dostępnego operatorom. Szczegółowy zakres opisują [komendy gracza](Komendy-gracza.md) i [komendy administracji](Komendy-administratora.md).

Przetestuj `/plots` na koncie gracza i `/plots admin` na koncie administratora. Następnie przejdź do [szybkiego startu](Szybki-start.md).

## Aktualizacja istniejącego serwera

Przed wymianą JAR-a zachowaj spójną kopię katalogu pluginu, schematów, danych oraz świata. Po aktualizacji sprawdź kilka własności, terminów podatkowych i umów. Kopie oraz odtwarzanie danych opisuje [osobny rozdział](Migracja-i-kopie-zapasowe.md).

Wdrożenie P1–P2 używa schematu danych **2**. Starsze działki zachowują dotychczasowe stawki jako nadpisania, a zapisane prawa członków pozostają bez zmian. Nawet przy MySQL zachowaj lokalne `data/operations`, `data/refunds`, `data/notifications` i `data/transfers.yml`. Powrót do starszego JAR-a wymaga zgodnej kopii danych.
