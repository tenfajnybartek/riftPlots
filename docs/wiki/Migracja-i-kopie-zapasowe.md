# Migracja i kopie zapasowe

[← Strona główna](Home.md) · [Magazyny danych](Magazyny-danych.md) · [Pomoc](Rozwiazywanie-problemow.md)

## Najpierw spójna kopia

Miasto składa się ze świata, zapisanych własności i wzorców budynków. Sama kopia JAR-a nie zabezpiecza tych danych. Najprostszą spójną kopię wykonujesz przy zatrzymanym serwerze.

| Element | Co zachować |
| :--- | :--- |
| `plugins/riftPlots/` | Konfigurację, `data/` i `schematics/`. |
| Światy | Rzeczywisty stan budynków i otoczenia. |
| Baza MySQL | Eksport bazy używanej przez plugin, jeśli jest aktywna. |
| Ekonomia | Dane providera Vault zgodnie z jego sposobem tworzenia kopii. |

Zapis własności i saldo ekonomii mogą wymagać wspólnego punktu odtworzenia. Przywrócenie tylko jednej strony po transakcji może stworzyć rozbieżność.

## Co przenosi migracja?

Migracja obejmuje zapisane działki i ich członków, profile, transakcje finansowe, tabliczki, umowy najmu, aktualny stan aukcji, budżet i audyt. Schematy pozostają we wspólnym katalogu `schematics/` i nie wymagają konwersji backendu.

Nie są przenoszone ulotne oferty transferu i kolejka powiadomień z pamięci. Migracja nie tworzy synchronizacji dwóch baz do dalszej wspólnej pracy.

## Warunki uruchomienia

- Źródłem komendy musi być aktywny magazyn.
- Źródło i cel muszą być różne.
- Cel musi być pusty; narzędzie odmawia nadpisania istniejących danych.
- Dla MySQL baza docelowa musi istnieć i być osiągalna.

Komenda przyjmuje nazwy `YAML`, `SQLITE`, `MYSQL`:

```text
/plots admin migrate SQLITE MYSQL
```

## Procedura krok po kroku

### 1. Przygotuj przerwę techniczną

Zablokuj nowe transakcje graczy w ramach swojej obsługi serwera i wykonaj kopię przy zatrzymanym procesie. Zachowaj pierwotny backend jako źródło do ewentualnego odtworzenia.

### 2. Skonfiguruj cel

Uzupełnij dane połączenia docelowego backendu w `storage.yml`. **Nie przełączaj jeszcze `storage.type` na cel.** Gdy trzeba wczytać zmienione ustawienia, odśwież konfigurację lub uruchom serwer ze źródłem nadal wskazanym jako aktywne.

### 3. Uruchom kopiowanie

```text
/plots admin migrate SQLITE MYSQL
```

Operacja działa synchronicznie na wątku serwera, dzięki czemu lokalne komendy i scheduler nie zmieniają danych podczas kopii. Duża baza może w tym czasie zatrzymać obsługę ticków. To zadanie na okno serwisowe.

### 4. Sprawdź wynik

Przeczytaj komunikat końcowy i liczby działek, transakcji oraz tabliczek. Sprawdź reprezentatywne profile i dane usług w celu. Brak zgłoszonego sukcesu oznacza, że nie należy jeszcze przełączać backendu.

Przy niepowodzeniu częściowe dane mogą pozostać w celu. Nie ponawiaj kopiowania przez ręczne nadpisywanie przypadkowych rekordów. Ustal przyczynę i przygotuj ponownie właściwy pusty cel po zabezpieczeniu danych.

### 5. Przełącz i uruchom

Zatrzymaj serwer, zmień `storage.type` na docelowy backend i uruchom serwer ponownie. Zmiany wykonane na źródle po zakończeniu kopiowania nie pojawią się w celu, dlatego nie otwieraj pośrednio serwera dla nowych transakcji.

### 6. Weryfikuj przed otwarciem

| Kontrola | Oczekiwany wynik |
| :--- | :--- |
| Lista działek | Właściwa liczba i identyfikatory. |
| Własności | Odpowiednie UUID i członkowie. |
| Podatki | Zachowane terminy, stawki i dzielnice. |
| Najem i aukcje | Odczyt zapisanych umów i bieżących ofert aukcyjnych. |
| Księga miasta | Właściwe saldo i historia. |
| Tabliczki i schematy | Istniejące powiązania i dostępne pliki wzorców. |

## Przywracanie po awarii

Zatrzymaj serwer przed odtwarzaniem plików. Przywróć spójny zestaw świata, danych pluginu, schematów i — jeśli wymaga tego sytuacja — ekonomii. Następnie uruchom serwer w trybie testowym i sprawdź nieruchomości z czasu awarii.

Walidacja migracji w testach obejmuje SQLite → YAML → SQLite z profilami i danymi usług. Nie zastępuje to próby migracji z docelowym MySQL ani testowego odtworzenia Twojej rzeczywistej kopii.
