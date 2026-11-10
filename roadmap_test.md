# riftPlots — roadmap testów i odbioru wydania

> Plan dla riftPlots 1.0.0, P1–P2 oraz konfiguracji `lang.yml` / `config.yml` / `gui.yml`. Przygotowany 10 listopada 2026 r. Puste checkboxy oznaczają testy do wykonania i udokumentowania w danym wydaniu.

[README](README.md) · [NOWOŚCI](NOWOSCI.md) · [Komendy](docs/commands.md) · [Konfiguracja GUI](docs/wiki/Konfiguracja-GUI.md) · [Scenariusz filmu](prezentacja.md)

## 1. Cel i sposób korzystania

Celem jest sprawdzenie zachowania na docelowym zestawie pluginów, rzeczywistych kontach graczy i kopii miasta. Plan zawiera **240 identyfikowanych punktów kontrolnych**. Odbiór obejmuje pieniądze, własność, dostęp, świat, historię i odzyskiwanie danych. Sam komunikat „sukces” na czacie nie wystarcza do zaliczenia operacji finansowej.

Najpierw wykonaj przygotowanie i test dymny, następnie funkcje graczy, konfigurację i trwałość. Awarie, migracje oraz obciążenie uruchamiaj na oddzielnych kopiach środowiska. Po naprawie błędu powtórz jego scenariusz, powiązane operacje i krótki test dymny.

**Ten dokument jest planem odbioru, a nie raportem potwierdzającym wykonanie wszystkich poniższych testów.** Nie zaznaczaj przypadku na podstawie tego, że podobny test automatyczny już istnieje.

### Ostatnia zapisana weryfikacja

| Kontrola | Stan udokumentowany przed przygotowaniem tego planu |
| :--- | :--- |
| Build i testy Java | 119 testów, bez błędów i pominięć przy uruchomionym rzeczywistym MySQL. |
| Backup | 6 testów narzędzia Python. |
| Paper | 54 sprawdzenia w fazach 24 + 11 + 19: transakcje, restart, reset, kopia oraz GUI/język. |
| Inspekcje IDE | 190 plików Java, bez błędów poziomu `error`. |
| Konfiguracja | Audyt 166 plików produkcyjnych; katalog 777 szablonów i etykiet. |

To punkt odniesienia dla wcześniejszego artefaktu. W nowym przebiegu zapisz własny wynik, hash JAR-a i datę. Próba Paper korzystała z zastępczych obiektów graczy i kontrolowanej ekonomii; potrzebne są również prawdziwy klient Minecraft oraz docelowy provider Vault.

## 2. Priorytety, statusy i dowody

| Priorytet | Znaczenie | Przykłady |
| :--- | :--- | :--- |
| P0 | Blokuje wydanie. | Duplikacja pieniędzy, utrata własności, nieuprawniona zmiana świata, uszkodzenie danych lub brak możliwości odtworzenia kopii. |
| P1 | Wymaga naprawy albo formalnie opisanej decyzji o wyłączeniu funkcji. | Niedziałający najem, błędny podatek, niemożliwa migracja używanego backendu. |
| P2 | Problem obsługi lub prezentacji. | Nieczytelne lore, literówka, niewłaściwa ikona bez zmiany celu operacji. |

Status przypadku: `NIEURUCHOMIONY`, `PASS`, `FAIL`, `BLOCKED`, `N/D`. `BLOCKED` i pominięcie testu nie oznaczają sukcesu. `N/D` wymaga przyczyny, np. MySQL nie jest wspieranym backendem danego wdrożenia i nie jest reklamowany w wydaniu.

W każdej tabeli checkbox zaznacz dopiero po `PASS`. Dla finansów zachowaj salda przed/po, UUID, ID działki, ID operacji i wynik odczytu po restarcie. Dla ochrony potrzebne są ujęcia z konta bez OP. Log powinien obejmować czas przed zdarzeniem i po nim, a nie sam wyjątek.

### Metryka przebiegu

```text
Wydanie / commit:
SHA-256 JAR-a:
Data, osoba testująca:
Paper / Java / system:
WorldEdit / WorldGuard / Vault:
Provider ekonomii i jego wersja:
Plugin uprawnień i pozostałe pluginy:
Backend i wersja bazy:
Liczba działek / graczy / rozmiar świata:
Zmiany konfiguracji względem domyślnej:
Identyfikator kopii bazowej:
Katalog logów i nagrań:
PASS / FAIL / BLOCKED / N/D:
Decyzja o wydaniu i otwarte zgłoszenia:
```

## 3. Środowisko i dane testowe

### 3.1. Przygotowanie środowiska

- [ ] **ENV-01 · P0** — użyj osobnego świata, katalogu serwera, portu i bazy. Ekonomia testowa nie może zapisywać do produkcyjnych portfeli.
- [ ] **ENV-02 · P1** — zapisz wersje wszystkich integracji. Wcześniejszy zestaw sprawdzony w repo to Paper 26.3 build 246, Java 25, WorldEdit 7.4.6, WorldGuard 7.0.21 i Vault 1.7.3.
- [ ] **ENV-03 · P0** — przygotuj spójną kopię świata, `plugins/riftPlots`, schematów, ekonomii i SQL, jeśli jest używany.
- [ ] **ENV-04 · P1** — zaloguj raz wszystkie konta, sprawdź UUID, salda i uprawnienia.
- [ ] **ENV-05 · P0** — konta testujące ochronę nie mają OP, wildcarda, bypassu riftPlots ani administracyjnego obejścia WorldGuard.
- [ ] **ENV-06 · P1** — nagrywaj konsolę/logi i miej sposób odczytu danych ekonomii niezależny od wiadomości riftPlots.
- [ ] **ENV-07 · P1** — zapisz hash artefaktu; ten sam JAR ma przejść automatykę, test ręczny i końcowy odbiór.

```powershell
Get-FileHash build/libs/riftPlots-1.0.0.jar -Algorithm SHA256
```

### 3.2. Konta

| Konto | Przeznaczenie | Uprawnienia / stan |
| :--- | :--- | :--- |
| `QAAdmin` | Tworzenie danych, konfiguracja, diagnostyka. | Administracja; nie używać do udowadniania ochrony graczy. |
| `Alicja` | Właścicielka i sprzedająca. | Zwykły gracz; początkowo 100 000 jednostek. |
| `Bartek` | Kupujący i najemca. | Zwykły gracz; początkowo 100 000 jednostek. |
| `Kuba` | Obcy gracz, drugi licytujący. | Zwykły gracz; początkowo 100 000 jednostek. |
| `Biedny` | Brak środków, dokładna granica salda. | Zwykły gracz; saldo dobierane do przypadku. |

Uprawnienia i salda ustaw narzędziami faktycznie zainstalowanych pluginów. riftPlots nie dostarcza uniwersalnego `/eco` ani komend menedżera uprawnień.

### 3.3. Nieruchomości

Przygotuj kilka osobnych lokali. Nie wykorzystuj aktywnie wynajętej działki do testu zwykłej sprzedaży lub aukcji.

| ID | Przeznaczenie | Stan początkowy |
| :--- | :--- | :--- |
| `qa_dom` | Zakup, podatek, członkowie, teleport. | Dostępny; cena 10 000, `FIXED`, podatek 200, okres 7 dni. |
| `qa_sklep` | Transfer gracz–gracz. | Własność Alicji; brak najmu i aukcji. |
| `qa_najem` | Najem miejski. | Bez właściciela; czynsz 250 `DAY`, kaucja 500. |
| `qa_najem_gracza` | Czynsz dla właściciela. | Własność Alicji; najem dozwolony. |
| `qa_aukcja` | Aukcja miejska. | Bez właściciela; brak oferty najmu. |
| `qa_aukcja_gracza` | Prowizja i wypłata sprzedającemu. | Własność Alicji. |
| `qa_parter` / `qa_pietro` | Ochrona pionowa. | Te same X/Z, rozłączne Y, np. 64–68 i 70–74. |
| `qa_reset` | Snapshot i odtworzenie. | Zapamiętany wzorzec, znane bloki kontrolne i wyposażenie. |
| `qa_typ` | Dziedziczenie i dzielnice. | Dostępny; bez ręcznych nadpisań w pierwszym przebiegu. |
| `qa_minus` | Ujemne współrzędne i chunki. | Obszar po obu stronach granicy chunka w ujemnych X/Z. |

Przykład przygotowania `qa_dom` po zaznaczeniu bryły WorldEdit:

```text
/plots admin create qa_dom Dom testowy
/plots admin setprice qa_dom 10000
/plots admin settax qa_dom 200
/plots admin settaxmode qa_dom FIXED
/plots admin setperiod qa_dom 7
/plots admin setgrace qa_dom 2
/plots admin info qa_dom
/plots admin health reset qa_dom
```

Zaznaczenie WorldEdit wykonaj w świecie i wymiarach danego lokalu. `create` zapisuje jego wzorzec, więc budynek musi być gotowy przed rejestracją. `snapshot` zastępuje ten wzorzec aktualnym stanem.

### 3.4. Przyspieszony czas na osobnej kopii

Do najmu można przed podpisaniem nowych umów ustawić `rental-period-seconds.HOUR: 60`. Nowa oferta w jednostce `HOUR` potrwa wtedy 60 sekund na okres. Zachowaj normalne długości w głównym przebiegu zgodności i osobno sprawdź przywrócenie konfiguracji.

Do szybkiego zakończenia aukcji użyj czasu 1 minuta i `auction.anti-snipe-extension-seconds: 0`. Anti-snipe sprawdź oddzielnie z dodatnim oknem. Scheduler ma własny interwał, więc zmiana statusu może nastąpić przy najbliższym przebiegu, a nie dokładnie w sekundzie końca.

Okres podatkowy komendy `setperiod` ma minimum jeden dzień. Nie ma komendy „przesuń termin podatku o minutę”. Test karencji wymaga odczekania lub przygotowanego przez programistę rekordu z kontrolowaną datą. Ewentualną edycję danych wykonuj przy zatrzymanym serwerze, zgodnie ze schematem używanego backendu; po starcie potwierdź odczyt przez `admin info`. Nie zmieniaj zegara systemowego podczas pracy serwera.

## 4. Kolejność i budżet czasu

| Etap | Zakres | Szacunkowo | Warunek przejścia |
| :--- | :--- | :--- | :--- |
| A | Środowisko, kopia, build, podstawowy start. | 30–60 min | Prawidłowy artefakt i działające integracje. |
| B | Zakup, dostęp, GUI, podatek, teleport i tabliczki. | 1–2 h | Brak P0 w podstawowej obsłudze. |
| C | Najem, transfer, aukcje, księga i historia. | 2–3 h | Zgodne salda i własność. |
| D | Konfiguracja, restart, reset i backendy. | 1–3 h | Dane i ochrona zachowane. |
| E | Awarie, zwroty i migracja. | 2–4 h | Możliwe uzgodnienie oraz odzyskanie danych. |
| F | Obciążenie i próba długotrwała. | 2–4 h + 12–24 h obserwacji | Zapisane pomiary i brak narastającej degradacji. |
| G | Końcowy odbiór kopii i decyzja o wydaniu. | 30–60 min | Wypełnione kryteria z rozdziału 24. |

To szacunki organizacyjne, nie deklaracja wykonanych prac ani gwarantowanego czasu testów.

## 5. Automatyka i powtarzalny build

### 5.1. Komendy lokalne

```powershell
.\gradlew.bat test build integrationProbeJar --console=plain
python scripts/check_configuration.py
python scripts/wiki.py check
python -m unittest discover -s scripts/tests -v
.\gradlew.bat benchmarkPlots --console=plain
```

Na Linux/macOS użyj `./gradlew` i odpowiedniego polecenia Python 3. Raport JUnit/HTML: `build/reports/tests/test/index.html`; benchmark: `build/reports/benchmarks/spatial-index.json`; audyt konfiguracji: `build/reports/configuration/`.

- [ ] **AUTO-01 · P1** — build ze świeżego klona na JDK 25 nie korzysta z ręcznie dopisanych lokalnych bibliotek.
- [ ] **AUTO-02 · P0** — regresje finansów, ochrony, resetu, awarii zapisu i trwałości przechodzą dla wydawanego JAR-a.
- [ ] **AUTO-03 · P1** — test natywnego SQLite otwiera bazę z faktycznie spakowanego artefaktu.
- [ ] **AUTO-04 · P1** — brak pominiętych testów MySQL w przebiegu deklarującym jego zgodność.
- [ ] **AUTO-05 · P1** — raport kontroli konfiguracji nie zgłasza nowych naruszeń; ręcznie sprawdzono też teksty widoczne w grze.
- [ ] **AUTO-06 · P1** — produkcyjny JAR zawiera `gui.yml`, `lang.yml`, `config.yml` i pozostałe wymagane zasoby, a nie zawiera klas `integrationprobe`.
- [ ] **AUTO-07 · P1** — przygotowany workflow rzeczywiście wykonano na GitHubie dla wydawanego commita; zapisano link do wyniku.

Test MySQL wymaga osobnej instancji i konta z uprawnieniami potrzebnymi testowi. Przykład ustawienia środowiska przed Gradle:

```powershell
$env:RIFTPLOTS_MYSQL_HOST = '127.0.0.1'
$env:RIFTPLOTS_MYSQL_PORT = '33077'
$env:RIFTPLOTS_MYSQL_USER = 'riftplots_test'
$env:RIFTPLOTS_MYSQL_PASSWORD = 'UZUPELNIJ_LOKALNIE'
.\gradlew.bat test build --console=plain
```

Użyj faktycznych danych izolowanej bazy. Obecność `BUILD SUCCESSFUL` nie zastępuje odczytania liczby `skipped` w raportach.

### 5.2. Izolowany Paper z repozytorium

Po zapoznaniu się z Minecraft EULA i jej zaakceptowaniu przez operatora środowiska:

```powershell
.\gradlew.bat build integrationProbeJar --console=plain
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/integration-server.ps1 -ServerDirectory build/qa-paper -PrepareOnly
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/integration-server.ps1 -ServerDirectory build/qa-paper -AcceptEula
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/integration-server.ps1 -ServerDirectory build/qa-paper
```

Wybierz nowy katalog dla nowej kampanii. Pierwsze uruchomienie wykonuje fazę utworzenia i transakcji, drugie restart oraz reset. Kolejną fazę wykonaj na odtworzonej kopii zgodnie z rozdziałem 20. Sonda sama zatrzymuje serwer. Weryfikuj `plugins/RiftPlotsProbe/report.yml`: `passed`, `phase`, `checks`, aktualność pliku oraz log `RIFTPLOTS_INTEGRATION_PASS`.

Po prawidłowej drugiej fazie i zatrzymaniu procesu możesz wykonać poniższą sekwencję. Launcher z repozytorium wymaga katalogu serwera wewnątrz projektowego `build/`, dlatego kopię sondy odtwarzamy właśnie tam:

```powershell
python scripts/backup.py create --server-dir build/qa-paper --output build/qa-paper-backup.zip --server-stopped
python scripts/backup.py verify build/qa-paper-backup.zip
python scripts/backup.py restore build/qa-paper-backup.zip --target build/qa-paper-restored
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/integration-server.ps1 -ServerDirectory build/qa-paper-restored -AcceptEula
```

Są to dane kontrolowanej ekonomii sondy. Test odtwarzania własnego providera wykonaj oddzielnie według rozdziału 20, wraz z jego rzeczywistymi plikami lub dumpem SQL.

- [ ] **AUTO-08 · P1** — faza pierwsza przeszła na świeżym katalogu; nie użyto starego raportu jako dowodu.
- [ ] **AUTO-09 · P0** — faza druga potwierdziła trwałość i faktyczne odtworzenie bloków.
- [ ] **AUTO-10 · P0** — faza po odtworzeniu kopii potwierdziła zgodne dane, salda testowe i GUI.

Ta sonda zapewnia własną ekonomię testową. Do ręcznej gry uruchom osobny serwer z normalnym providerem Vault; automatyczna sonda nie służy do nagrywania filmu.

## 6. Start, zależności i podstawowa obsługa

| PASS | ID / priorytet | Czynność | Oczekiwany wynik |
| :--- | :--- | :--- | :--- |
| [ ] | START-01 / P1 | Pierwszy start na pustych danych. | Plugin włączony, pliki konfiguracji utworzone, brak nieobsłużonego błędu. |
| [ ] | START-02 / P1 | Pełny restart bez zmian danych. | Brak duplikacji regionów, schematów i rekordów. |
| [ ] | START-03 / P1 | Osobne próby bez Vault, WorldEdit lub WorldGuard. | Czytelna odmowa uruchomienia; brak działających płatności w połowicznie zainicjalizowanym pluginie. |
| [ ] | START-04 / P1 | Vault obecny, provider ekonomii nieobecny. | Brak normalnego działania finansów i czytelna diagnoza. |
| [ ] | START-05 / P1 | `/plots`, `/plot`, `/dzialka`, `/plots help`, `/plots admin help`. | Właściwe GUI/pomoc i rozdzielenie gracza od administratora. |
| [ ] | START-06 / P1 | Konsola wywołuje komendę wymagającą gracza. | Komunikat zamiast wyjątku lub nieistniejącej lokalizacji. |
| [ ] | START-07 / P1 | `/plots admin health`. | Aktywny backend, integracje i problemy są czytelne; diagnostyka nie zmienia danych. |
| [ ] | START-08 / P1 | Zapis nazw z polskimi znakami i restart. | Poprawne UTF-8 w GUI, YAML, SQL i logach. |

## 7. Rejestracja, granice, typy i dzielnice

| PASS | ID / priorytet | Czynność | Oczekiwany wynik |
| :--- | :--- | :--- | :--- |
| [ ] | PLOT-01 / P0 | `create` z prawidłowym zaznaczeniem. | Jeden rekord, odpowiedni region i czytelny schemat zgodny z bryłą. |
| [ ] | PLOT-02 / P1 | Brak zaznaczenia, powtórzone ID, nieprawidłowy identyfikator. | Odmowa bez częściowej rejestracji i bez nadpisania istniejącego schematu. |
| [ ] | PLOT-03 / P0 | Rejestracja nachodzącego obszaru i kolizji z obcym regionem WG. | Czytelna informacja o kolizji; poprzedni region pozostaje chroniony. |
| [ ] | PLOT-04 / P0 | Parter i piętro z tym samym X/Z, różnym Y. | Dwa odrębne lokale; brak konfliktu przy rozłącznych granicach. |
| [ ] | PLOT-05 / P0 | Bloki na `min`/`max` i o jeden blok poza każdą ścianą. | Ochrona odpowiada dokładnie granicom, także podłodze i sufitowi. |
| [ ] | PLOT-06 / P1 | Ujemne współrzędne, przekroczenie granicy chunka i inny świat. | Prawidłowe wyszukiwanie, wybór działki i opodatkowanie. |
| [ ] | PLOT-07 / P1 | `type set` dostępnej i posiadanej działki. | Dziedziczone wartości aktualizują się zgodnie z zasadami; jawne nadpisania i warunki własności są chronione. |
| [ ] | PLOT-08 / P1 | `type apply-defaults` bez potwierdzenia, potem z `confirm`. | Podgląd nie zmienia danych; potwierdzenie stosuje pokazane wartości i maskę nadpisań. |
| [ ] | PLOT-09 / P0 | `apply-defaults` podczas najmu, aukcji lub resetu. | Odmowa kolidującej operacji. |
| [ ] | PLOT-10 / P1 | Cena typu × mnożnik ceny dzielnicy, kilka reloadów. | Wynik nie narasta przez kolejne mnożenie. |
| [ ] | PLOT-11 / P1 | Ręczny `setprice`, potem zmiana dzielnicy i reload. | Zachowanie ręcznej ceny zgodnie z modelem nadpisania. |
| [ ] | PLOT-12 / P1 | Niedozwolony typ, zakaz sprzedaży/najmu i limit działek w dzielnicy. | Nabycie jest odrzucone bez pobrania pieniędzy. |
| [ ] | PLOT-13 / P1 | Typ z własną ikoną i flagą stanu WG `pvp: deny`. | Ikona w rynku i flaga przy rejestracji/jawnym zastosowaniu profilu odpowiadają konfiguracji. |
| [ ] | PLOT-14 / P1 | Starszy rekord bez maski nadpisań. | Zachowane dawne wartości; brak cichej zmiany ceny przy aktualizacji. |
| [ ] | PLOT-15 / P0 | Błąd zapisu/usunięcia rekordu w kontrolowanej próbie. | Brak fałszywego sukcesu i utraty ochrony wcześniej istniejącej działki. |

### Kontrola liczb dla dziedziczenia

Na `qa_typ` bez nadpisań: domyślny `RESIDENTIAL` kosztuje 25 000, a dzielnica `centrum` ma mnożnik ceny 1,5. Zakup powinien wynosić **37 500**. Bazowy podatek 1 500 przy mnożniku podatku `centrum` 1,25 daje **1 875** w trybie `FIXED`.

Wykonaj `setprice qa_typ 40000`. Ręczna cena końcowa wynosi teraz **40 000**, a nie 60 000 po ponownym zastosowaniu mnożnika. Przywrócenie dziedziczenia wymaga świadomego `apply-defaults`.

## 8. Zakup, prowizje i walidacja kwot

**Zakup komendą `/plots buy qa_dom` wykonuje operację bez osobnego `confirm`.** Ścieżkę z oknem potwierdzenia sprawdź przez rynek GUI. Prawy klik tabliczki sprzedaży również może wykonać zakup.

| PASS | ID / priorytet | Czynność | Oczekiwany wynik |
| :--- | :--- | :--- | :--- |
| [ ] | BUY-01 / P0 | Zakup miejskiego `qa_dom` za 10 000 przy saldzie Alicji 100 000. | Saldo 90 000, Alicja właścicielką, jeden przychód miasta i zakończona operacja. |
| [ ] | BUY-02 / P0 | Dwa konta kupują ten sam lokal niemal jednocześnie. | Jeden właściciel i jedno skuteczne obciążenie; przegrany nie płaci. |
| [ ] | BUY-03 / P0 | Wielokrotne kliknięcie potwierdzenia i powtórzona komenda. | Brak drugiego zakupu i powielonego przychodu. |
| [ ] | BUY-04 / P0 | Saldo mniejsze o 0,01; następnie równe cenie. | Najpierw odmowa bez skutków, potem prawidłowy zakup. |
| [ ] | BUY-05 / P0 | Odmowa pobrania przez provider mimo dodatniego salda. | Brak przyznania własności; stan finansowy zgodny z rzeczywistą odpowiedzią. |
| [ ] | BUY-06 / P0 | Dostępny, posiadany, wynajęty, aukcyjny, resetowany i wyłączony lokal. | Zakup możliwy wyłącznie w dozwolonym stanie. |
| [ ] | BUY-07 / P0 | Stare GUI: zmiana ceny, właściciela lub stanu przed kliknięciem. | Ponowna weryfikacja; zmieniona oferta wymaga ponownego potwierdzenia. |
| [ ] | BUY-08 / P1 | Cena zero. | Bezpłatne nabycie, brak sztucznego minimalnego obciążenia. |
| [ ] | BUY-09 / P1 | Kwota ujemna, `NaN`, `Infinity`, tekst, przekroczony limit i nadmiar miejsc dziesiętnych. | Odrzucenie przed wywołaniem Vault; czytelna informacja. |
| [ ] | BUY-10 / P0 | Dodatek API odrzuca zakup lub obniża cenę do dostępnego salda. | Anulowanie bez skutków; rabat uwzględniony przed końcową kontrolą salda. |
| [ ] | BUY-11 / P0 | Błąd zapisu po pobraniu w harnessie. | Rozpoznawalna operacja do uzgodnienia, brak automatycznego podwójnego pobrania po restarcie. |

### Kontrola księgowa

Każdy przykład zacznij od osobnego punktu kontrolnego. Wewnętrzny budżet miasta nie jest kontem gracza w Vault.

| Operacja | Oczekiwane rozliczenie przy konfiguracji przykładu |
| :--- | :--- |
| Zakup miejskiego lokalu 10 000 | Kupujący −10 000, księga miasta +10 000. |
| Podatek 200 | Właściciel −200, księga miasta +200. |
| Transfer gracz–gracz 12 000, prowizja 5% | Kupujący −12 000, sprzedający +11 400, miasto +600. |
| Najem miejski 7 × 250, kaucja 500 | Najemca −2 250; czynsz 1 750 do miasta; 500 kaucji rozliczane osobno. |
| Najem lokalu gracza na tych warunkach | Najemca −2 250, właściciel +1 750; kaucja pozostaje zobowiązaniem. |
| Aukcja lokalu gracza, cena 1 500, prowizja 5% | Łącznie kupujący −1 500, sprzedający +1 425, miasto +75. |

Kwoty zwrotów oceniaj po zakończeniu przetwarzania kolejki albo jako trwałe, prawidłowe zobowiązanie. Zobowiązanie nie może znikać tylko dlatego, że provider chwilowo odmawia wypłaty.

## 9. Podatki, terminy i karencja

| PASS | ID / priorytet | Czynność | Oczekiwany wynik |
| :--- | :--- | :--- | :--- |
| [ ] | TAX-01 / P0 | `pay qa_dom`, `pay qa_dom 2`, `extend qa_dom 2`. | Zgodna kwota, przesunięcie terminu i historia; `extend` stosuje te same reguły. |
| [ ] | TAX-02 / P1 | Liczba okresów `0`, `-1`, tekst i powyżej limitu. | Odmowa; błędny argument nie staje się automatycznie jednym okresem. |
| [ ] | TAX-03 / P0 | Osiągnięcie limitu przedpłat i upływ jednego opłaconego okresu. | Limit działa i odzyskuje wolne miejsce po czasie. |
| [ ] | TAX-04 / P0 | Termin w przyszłości oraz termin zaległy. | Prawidłowy punkt początkowy przedłużenia; brak naliczania od przypadkowego restartu. |
| [ ] | TAX-05 / P0 | Przejście do karencji i wpłata przed końcem. | Przywrócenie poprawnego stanu bez odebrania własności. |
| [ ] | TAX-06 / P0 | Upływ karencji z włączonym resetem. | Odebranie i reset na właściwej działce; ogłoszenie po przyjęciu operacji. |
| [ ] | TAX-07 / P0 | Brak możliwości przygotowania schematu przy wygaśnięciu. | Kontrolowany błąd; brak pozornego udanego odtworzenia i osieroconej blokady. |
| [ ] | TAX-08 / P1 | Restart po upływie terminu, ale przed następnym sprawdzeniem. | Brak nowej pełnej karencji od czasu uruchomienia. |
| [ ] | TAX-09 / P1 | Przypomnienia i kolejne cykle podatkowe. | Wiadomości zgodnie z progami; brak spamowania starym terminem. |
| [ ] | TAX-10 / P1 | Wyłączone przypomnienia przez pustą listę. | Brak automatycznego przywrócenia domyślnej listy. |
| [ ] | TAX-11 / P1 | Błąd jednej działki podczas przebiegu schedulera. | Pozostałe działki, najmy i aukcje nadal sprawdzane. |
| [ ] | TAX-12 / P0 | Zmiana precyzji i zaokrąglenia kwot. | GUI, faktyczne obciążenie i historia nie rozchodzą się przez zaokrąglenie. |

Sprawdź sześć trybów na kontrolowanym obszarze 10 × 10, bez dzielnicy, ze stawką 2,50:

| Tryb | Wynik / metoda kontroli |
| :--- | :--- |
| `FIXED` | 2,50. |
| `CUSTOM` | 2,50; obecnie wzór jak `FIXED`. |
| `PER_BLOCK` | 250,00, ponieważ powierzchnia wynosi 100. |
| `PER_CHUNK` | 2,50 × rzeczywista liczba przecinanych chunków; sprawdź także częściowe chunki. |
| `MULTIPLIER` | 2,50 × mnożnik działki, z minimum z konfiguracji. |
| `SIZE_TIERS` | 2,50 × mnożnik właściwego progu; sprawdź próg i pierwszy blok powyżej niego. |

Dodatkowo zastosuj mnożnik podatku dzielnicy. Wysokość bryły nie zwiększa powierzchni poziomej. Przed testem zapisz rzeczywiste granice, a nie same rozmiary budynku widoczne na ekranie.

## 10. Członkowie, role i ochrona świata

### 10.1. Dostęp graczy

| PASS | ID / priorytet | Czynność | Oczekiwany wynik |
| :--- | :--- | :--- | :--- |
| [ ] | ACL-01 / P0 | Obcy próbuje budować, niszczyć i otwierać pojemnik. | Brak dostępu wewnątrz cudzej działki. |
| [ ] | ACL-02 / P1 | Właściciel wykonuje te same czynności. | Dozwolone w granicach jego uprawnień oraz polityki serwera. |
| [ ] | ACL-03 / P0 | `trust Bartek qa_dom` przy domyślnych ustawieniach. | Interakcje, drzwi, pojemniki, teleport; bez domyślnego budowania i niszczenia. |
| [ ] | ACL-04 / P0 | Właściciel osobno włącza każde prawo w GUI. | Każde prawo steruje właściwą czynnością; sprawdź też ponowne odebranie. |
| [ ] | ACL-05 / P0 | Członek próbuje edytować własne prawa lub prawa innych. | Brak samodzielnego podniesienia uprawnień. |
| [ ] | ACL-06 / P0 | Zmiana `MEMBER` → `MANAGER` → `MEMBER`. | Pełny dostęp managera i właściwe ograniczenia po cofnięciu roli; zgodność WG. |
| [ ] | ACL-07 / P1 | Limit członków, ponowne dodanie i próba dodania siebie. | Brak duplikacji; limity i walidacja działają. |
| [ ] | ACL-08 / P0 | `untrust` zwykłego członka. | Utrata praw od razu i po restarcie. |
| [ ] | ACL-09 / P0 | Próba edycji/usunięcia aktywnego `TENANT` przez panel i `untrust`. | Umowy nie da się obejść przez zwykłą listę członków. |
| [ ] | ACL-10 / P0 | Stary otwarty edytor po zmianie właściciela. | Dawny właściciel nie zapisuje zmian dostępu. |
| [ ] | ACL-11 / P0 | Alicja właścicielką parteru, Bartek piętra. | Uprawnienia obu lokali pozostają odrębne także przy suficie i podłodze. |
| [ ] | ACL-12 / P1 | Zmiana `members.default-permissions`, dodanie nowej osoby. | Nowa osoba otrzymuje nowy zestaw; stare zapisane prawa nie są przepisywane. |

### 10.2. Mechaniki Minecrafta

Każdy przypadek wykonaj przy granicy poziomej i pionowej oraz jako obcy, członek z danym prawem i właściciel. Porównaj zachowanie z włączoną i wyłączoną odpowiednią opcją ochrony oraz z polityką WorldGuard.

- [ ] **PHY-01 · P0** — woda i lawa rozlewające się do/z lokalu; brak nieuprawnionej zmiany sąsiedniej nieruchomości.
- [ ] **PHY-02 · P0** — tłok zwykły i lepki, push/pull, bloki śluzu/miodu, ruch przez sufit i podłogę.
- [ ] **PHY-03 · P0** — eksplozja wewnątrz, na zewnątrz i przy ścianie; właściwy zestaw bloków chronionych.
- [ ] **PHY-04 · P0** — ogień, zapalenie oraz rozprzestrzenianie przez granicę.
- [ ] **PHY-05 · P1** — ramki, obracanie/usuwanie przedmiotów, stojaki na zbroję i ich wyposażenie.
- [ ] **PHY-06 · P1** — drzwi, furtki, klapy, przyciski i dźwignie; prawa nie otwierają obcej nieruchomości przez przypadek.
- [ ] **PHY-07 · P0** — skrzynia podwójna na granicy, lejki i wagoniki z pojemnikiem; brak nieuprawnionego dostępu do cudzych przedmiotów.
- [ ] **PHY-08 · P1** — wiadra, pojemniki i interakcje drugą ręką; brak podwójnego wykonania czynności.
- [ ] **PHY-09 · P1** — pojazdy i prawo `VEHICLES`, wejście/wyjście na granicy regionu.
- [ ] **PHY-10 · P1** — kolizja z nadrzędnym regionem WG i innym pluginem ochrony; zachowanie spójne z deklarowaną konfiguracją serwera.

To testy wymaganej odporności docelowego serwera, nie deklaracja obsługi każdego zdarzenia przez sam riftPlots. Jeżeli dana mechanika wymaga ochrony WorldGuard albo dodatkowego ograniczenia, zapisz zależność i skonfiguruj ją przed odbiorem. Przeciek pieniędzy lub cudzych przedmiotów pozostaje błędem P0.

## 11. Najem, odnowienie, zakończenie i kaucja

Przygotowanie oferty miejskiej:

```text
/plots admin rent offer qa_najem 250 DAY 500
```

Scenariusz Bartka:

```text
/plots rent qa_najem 7 off
/plots rent info qa_najem
/plots rent autorenew qa_najem on
/plots rent autorenew qa_najem off
/plots rent cancel qa_najem
/plots rent cancel qa_najem confirm
```

| PASS | ID / priorytet | Czynność | Oczekiwany wynik |
| :--- | :--- | :--- | :--- |
| [ ] | RENT-01 / P0 | Najem 7 × 250 + 500 kaucji. | Pobranie 2 250, poprawny najemca i termin, zgodny odbiorca czynszu. |
| [ ] | RENT-02 / P0 | Oferta własnego lokalu z/bez `riftplots.rent.offer`. | Bez prawa odmowa; z prawem oferta tylko własnego, dozwolonego lokalu. |
| [ ] | RENT-03 / P0 | Najem własnego lokalu, będąc już członkiem, drugi najemca lub niedozwolony typ/dzielnica. | Odmowa bez nowej płatności. |
| [ ] | RENT-04 / P0 | Brak środków na kaucję mimo środków na sam czynsz. | Nie powstaje częściowo opłacona umowa. |
| [ ] | RENT-05 / P1 | Domyślne odnowienie, świadome on/off i informacja o umowie. | GUI, komenda i zapis umowy pokazują zgodny wybór. |
| [ ] | RENT-06 / P0 | Odnowienie przy wystarczającym saldzie. | Jeden kolejny czynsz; brak ponownego pobrania kaucji. |
| [ ] | RENT-07 / P0 | Brak środków w chwili odnowienia. | Umowa nie zapewnia bezpłatnego dalszego dostępu; kaucja rozliczana właściwą ścieżką. |
| [ ] | RENT-08 / P0 | Wyłączenie odnowienia przed końcem. | Opłacony czas pozostaje dostępny do końca. |
| [ ] | RENT-09 / P0 | `cancel` bez `confirm`, potem z potwierdzeniem. | Najpierw opis zasad bez zmiany stanu; potem utrata dostępu, zwrot kaucji, brak zwrotu wykorzystanego/opłaconego czynszu. |
| [ ] | RENT-10 / P0 | Provider odmawia zwrotu kaucji podczas wygaśnięcia. | Najem wygasa, dostęp znika, dług 500 przetrwa restart. |
| [ ] | RENT-11 / P0 | Powtórne zakończenie i wielokrotne `retry` zwrotu. | Kaucja wypłacona najwyżej raz. |
| [ ] | RENT-12 / P0 | Restart z aktywną, następnie wygasłą umową. | Właściwy dostęp i daty; brak nowego pełnego okresu od restartu. |
| [ ] | RENT-13 / P0 | Zmiana oferty/czynszu/kaucji przed kliknięciem starego GUI. | Stare potwierdzenie nie obciąża innymi warunkami. |
| [ ] | RENT-14 / P1 | Jednostki `HOUR`, `DAY`, `WEEK`, `MONTH` i własne długości. | Zgodne terminy; domyślne `MONTH` to 30 dni, nie miesiąc kalendarzowy. |
| [ ] | RENT-15 / P0 | Zmiana profilu lub oferty podczas aktywnego najmu. | Brak cichego przepisania zapisanych warunków umowy. |
| [ ] | RENT-16 / P1 | Własne `rental.default-tenant-permissions` i restart. | Zestaw praw zgodny z polityką odtwarzania najemcy; brak przejścia do roli właściciela. |

## 12. Transfer i rynek wtórny

```text
# Alicja
/plots transfer Bartek 12000 qa_sklep
# Bartek — rzeczywiste ID skopiowane z otrzymanej oferty
/plots accept <id_oferty>
```

| PASS | ID / priorytet | Czynność | Oczekiwany wynik |
| :--- | :--- | :--- | :--- |
| [ ] | TR-01 / P0 | Zwykła sprzedaż 12 000 przy prowizji 5%. | Bartek −12 000, Alicja +11 400, miasto +600; jeden nowy właściciel. |
| [ ] | TR-02 / P0 | Transfer za zero. | Bezpłatne przekazanie bez ujemnej prowizji lub błędnej walidacji. |
| [ ] | TR-03 / P0 | Nieobecny odbiorca, własny nick, cudza działka, niedozwolona dzielnica. | Brak skutecznej oferty lub płatności w niedozwolonym przypadku. |
| [ ] | TR-04 / P0 | Akceptacja przez nieuprawnione konto i podwójne `accept`. | Tylko wskazany odbiorca, rozliczenie najwyżej raz. |
| [ ] | TR-05 / P1 | Odrzucenie przez kupującego i anulowanie przez sprzedającego. | Oferta zamknięta; brak obciążenia i późniejszej akceptacji. |
| [ ] | TR-06 / P0 | Upływ ważności, restart w trakcie i po terminie. | ID zachowane, ważność nie przedłuża się od restartu. |
| [ ] | TR-07 / P0 | Zmiana właściciela/stanu po wystawieniu oferty. | Nieaktualna oferta nie sprzedaje cudzej lub zablokowanej nieruchomości. |
| [ ] | TR-08 / P0 | Provider odmawia wypłaty sprzedającemu. | Własność i faktyczne przepływy są możliwe do uzgodnienia; zwrot/dziennik nie znika. |
| [ ] | TR-09 / P0 | Cena 0,10 i prowizja 5%. | Przy 2 miejscach i `HALF_UP`: miasto 0,01, sprzedający 0,09; suma równa pobraniu. |
| [ ] | TR-10 / P0 | Transfer przy najmie, aukcji, zaległości lub resecie. | Odmowa konfliktu zgodnie z regułami stanu. |

## 13. Aukcje bez escrow i z escrow

Zacznij od miejskiej aukcji, potem powtórz na lokalu gracza. Nową aukcję utwórz dopiero po zamknięciu poprzedniej i uzgodnieniu jej operacji.

```text
/plots admin auction start qa_aukcja 1000 100 5 1500
/plots auction list
/plots auction bid qa_aukcja 1000
/plots auction history qa_aukcja
/plots auction buy qa_aukcja confirm
```

W przykładzie zwykłe przebicia rozdziel między Alicję i Bartka. Polecenie kup teraz wykonuje tylko wybrany docelowy kupujący.

| PASS | ID / priorytet | Czynność | Oczekiwany wynik |
| :--- | :--- | :--- | :--- |
| [ ] | AUC-01 / P1 | Prawidłowy start i parametry zerowe/ujemne/błędne. | Prawidłowa aukcja albo odmowa bez częściowego uruchomienia. |
| [ ] | AUC-02 / P0 | Sprzedający licytuje sam siebie; obcy z brakiem prawa lub niedozwolonym nabyciem. | Brak niedozwolonej oferty. |
| [ ] | AUC-03 / P0 | Oferta poniżej minimum, równa minimum i następne przebicie. | Jednoznaczna walidacja i dokładna kwota lidera. |
| [ ] | AUC-04 / P0 | Konkurencyjne oferty i wieloklik GUI. | Prawidłowy porządek i rozliczenie bez duplikacji. |
| [ ] | AUC-05 / P1 | Oferta w oknie anti-snipe. | Termin przedłużony zgodnie z ustawieniem; oferta poza oknem nie przedłuża aukcji. |
| [ ] | AUC-06 / P0 | Zakończenie bez ofert i ze zwycięzcą. | Brak właściciela-widma; zwycięzca otrzymuje lokal dopiero po skutecznym rozliczeniu. |
| [ ] | AUC-07 / P0 | Zwycięzca wydał środki przed końcem przy wyłączonym escrow. | Brak bezpłatnego nabycia i jednoznaczny zapis wyniku. |
| [ ] | AUC-08 / P0 | Kup teraz przez GUI i komendę. | Cena i prowizja zgodne; aukcja rozliczona dokładnie raz. |
| [ ] | AUC-09 / P0 | Następne zwykłe przebicie przekracza cenę kup teraz. | Dostępna poprawna ścieżka zakupu za cenę kup teraz. |
| [ ] | AUC-10 / P0 | Stare okno po zamknięciu i utworzeniu nowej aukcji tego samego lokalu. | Sprawdzane ID aukcji; stare potwierdzenie nie licytuje nowej. |
| [ ] | AUC-11 / P0 | Anulowanie z ofertami, z `confirm` i powodem. | Audyt powodu, zamknięcie aukcji i właściwy zwrot depozytu. |
| [ ] | AUC-12 / P1 | Historia, strony, restart i zamknięta aukcja. | Przebicia zachowane i uporządkowane. |
| [ ] | AUC-13 / P0 | Włącz escrow przed utworzeniem nowej aukcji. | Środki rezerwowane przy ofercie. |
| [ ] | AUC-14 / P0 | Lider przebija sam siebie. | Pobierana tylko różnica względem własnej rezerwacji. |
| [ ] | AUC-15 / P0 | Inny gracz przebija lidera. | Poprzedni lider otrzymuje jeden trwały zwrot; nowa rezerwacja ma właściwą kwotę. |
| [ ] | AUC-16 / P0 | Koniec aukcji z escrow i zakup przez aktualnego lidera. | Łączna zapłata równa cenie; brak drugiego pełnego pobrania. |
| [ ] | AUC-17 / P0 | Restart i zmiana ustawienia escrow podczas trwającej aukcji. | Tryb danej aukcji pozostaje zapisany; zmiana dotyczy nowych aukcji. |
| [ ] | AUC-18 / P0 | Odmowa zwrotu rezerwacji lub wypłaty sprzedającemu. | Środki nie znikają z ewidencji; trwałe zobowiązanie albo stan wymagający uzgodnienia. |

**Przykład kontroli escrow:** Alicja rezerwuje 1 000. Bartek przebija do 1 100, Alicja po rozliczeniu odzyskuje 1 000. Bartek podnosi własną ofertę do 1 300 i dopłaca 200. Kupuje za 1 500 i dopłaca kolejne 200. Jego łączny koszt wynosi **1 500**, a nie 3 900. Na aukcji miejskiej odbiorcą ceny jest miasto; przy sprzedaży gracza zastosuj prowizję i wypłatę właścicielowi.

## 14. GUI, wejścia klienta i podgląd granic

| PASS | ID / priorytet | Czynność | Oczekiwany wynik |
| :--- | :--- | :--- | :--- |
| [ ] | GUI-01 / P1 | Otwórz rynek, szczegóły, zakup, najem, aukcję, członków i edytor praw. | Wszystkie siedem ekranów czytelne; aktualne ceny, stany i role. |
| [ ] | GUI-02 / P1 | Więcej ofert/członków niż `content-slots`. | Każdy wpis osiągalny przez strony, bez duplikacji lub utraty ostatniego wpisu. |
| [ ] | GUI-03 / P1 | Filtry typu/dzielnicy i sortowanie ID/cena/podatek. | Trafne wyniki i poprawne przejścia między pustą i pełną stroną. |
| [ ] | GUI-04 / P0 | Shift-click, klawisze 1–9, swap ręki, double-click, wyrzucanie i przeciąganie. | Brak wyniesienia przedmiotów GUI i wywołania niezamierzonej operacji. |
| [ ] | GUI-05 / P0 | Kliknięcie własnego dolnego ekwipunku pod oknem. | Nie uruchamia zakupu, zapłaty ani zarządzania. |
| [ ] | GUI-06 / P0 | Przedmiot podobny nazwą/materiałem bez danych akcji. | Nie traktowany jako przycisk finansowy. |
| [ ] | GUI-07 / P0 | Przenieś przycisk kupna na slot 5, zmień materiał na `DIAMOND`. | Ta sama akcja i kontrola aktualnej wyceny; brak zależności od starego slotu/materiału. |
| [ ] | GUI-08 / P1 | Zmień tytuł, lore, filler, ilość, glow i model. | Zgodne renderowanie po ponownym otwarciu; legalny stos materiału. |
| [ ] | GUI-09 / P1 | Puste wyniki, puste lore, wyłączony przycisk. | Czytelny ekran; wyłączona akcja nie jest klikalna. |
| [ ] | GUI-10 / P0 | Kolizja slotów, slot poza zakresem, zły materiał lub typ YAML. | Odrzucenie nowej konfiguracji; ostatni poprawny układ pozostaje aktywny. |
| [ ] | GUI-11 / P1 | Zmiana rozmiaru i `content-slots` oraz domyślnej zakładki/sortu. | Rozmiar strony i pierwszy widok odpowiadają konfiguracji. |
| [ ] | GUI-12 / P1 | Długa nazwa, polskie znaki, nazwa podobna do tagu `<red>`. | Użyteczny wygląd; dane gracza nie zmieniają formatowania lub akcji. |
| [ ] | GUI-13 / P0 | Zamknięcie, wylogowanie i zmiana stanu podczas otwartego menu. | Brak zakupu na nieaktualnych prawach i osieroconej blokady. |
| [ ] | GUI-14 / P1 | `/plots border qa_parter` i `qa_pietro`. | Właściwe 12 krawędzi, wysokość i brak zmian bloków. |
| [ ] | GUI-15 / P1 | Obrys obserwuje drugie konto; spam, wylogowanie, zmiana świata. | Widoczność tylko dla proszącego, cooldown i zakończenie podglądu. |

## 15. Język, konfiguracja i przeładowanie

Przed testami zachowaj kopie wszystkich sześciu plików ustawień. Wprowadzaj jeden błąd naraz i po próbie przywracaj poprawny plik. Stare otwarte menu otwórz ponownie po reloadzie.

| PASS | ID / priorytet | Czynność | Oczekiwany wynik |
| :--- | :--- | :--- | :--- |
| [ ] | CFG-01 / P1 | Stara konfiguracja bez nowych kluczy i bez `gui.yml` przy starcie. | Nowy plik GUI utworzony; brakujące wartości uzupełnione z JAR-a bez nadpisania własnych ustawień. |
| [ ] | CFG-02 / P1 | Zmiana wiadomości sukcesu, błędu, pomocy, etykiety oraz diagnostyki. | Właściwy nowy tekst w odpowiadającej ścieżce; zanotuj każdy tekst nadal zaszyty w kodzie. |
| [ ] | CFG-03 / P1 | Pusta wiadomość `''`, puste lore `[]`, usunięty klucz. | Puste wartości zachowane; usunięty domyślny klucz korzysta z JAR-a. |
| [ ] | CFG-04 / P0 | Niepoprawny YAML, tekst zamiast sekcji lub lista zamiast wiadomości. | Czytelny błąd; brak częściowej podmiany głównych ustawień, języka i GUI w odrzucanym zestawie. |
| [ ] | CFG-05 / P1 | Zła konfiguracja typu/dzielnicy lub nieistniejący typ domyślny. | Diagnoza wskazuje problem; po poprawieniu reload działa, scheduler nadal pracuje. |
| [ ] | CFG-06 / P1 | MiniMessage, `{prefix}`, `{arg1}`, nazwane placeholdery i wartości z nawiasami. | Brak przypadkowego rozwinięcia drugiego placeholdera; poprawne polskie znaki. |
| [ ] | CFG-07 / P1 | Zmiana formatu daty, strefy, lokalizacji i grupowania kwot. | Spójna prezentacja; bez zmiany zapisanych dat i sald. |
| [ ] | CFG-08 / P0 | Precyzja 0/2/3/8 miejsc, limit kwoty i niepoprawne zaokrąglenie. | Akceptacja dozwolonych wartości; odmowa niebezpiecznych ustawień przed zastosowaniem. |
| [ ] | CFG-09 / P1 | Własne progi/mnożniki podatku, także błędna kolejność i różna liczba elementów. | Kalkulator używa poprawnych progów; błędna konfiguracja odrzucona. |
| [ ] | CFG-10 / P1 | Dźwięk, pusty dźwięk, głośność, pitch i czas tytułu. | Zgodny efekt, wyciszenie działa; brak wyjątku dla zwykłych komunikatów. |
| [ ] | CFG-11 / P1 | Kolor/typ/rozmiar/częstotliwość cząsteczek i duży obrys. | Efekt zgodny z konfiguracją i limitami; brak nieobsługiwanych danych cząsteczki. |
| [ ] | CFG-12 / P1 | Strony czatu, audytu, zwrotów i historii. | Właściwa liczba pozycji; zmiana jednej sekcji nie wpływa przypadkowo na inne. |
| [ ] | CFG-13 / P1 | Podpowiedzi `tab-completion`. | Nowe liczby na TAB; walidacja argumentów nadal odrzuca wartości spoza zakresu. |
| [ ] | CFG-14 / P0 | Reload w trakcie najmu i aukcji, następnie restart. | Aktywne umowy, rezerwacje i daty zachowane zgodnie z ich zapisanym stanem. |
| [ ] | CFG-15 / P1 | Zmiana `storage.type` bez restartu. | `health` rozróżnia ustawienie pliku i rzeczywiście aktywny backend; brak pozornej migracji. |

Retencja już utworzonego magazynu wiadomości, połączenia bazy i parametry indeksu wymagające inicjalizacji sprawdzaj po restarcie. W raporcie rozróżniaj reload plików od ponownego utworzenia usług.

## 16. Tabliczki i teleportacja

| PASS | ID / priorytet | Czynność | Oczekiwany wynik |
| :--- | :--- | :--- | :--- |
| [ ] | SIGN-01 / P1 | Powiąż wskazywaną tabliczkę jako `FOR_SALE`, `STATUS`, `RENEW`. | Właściwy lokal i cztery wiersze, także po restarcie. |
| [ ] | SIGN-02 / P0 | Lewy/prawy klik, obie ręce, szybkie kliknięcia. | Właściwa informacja albo pojedyncza płatność; bez podwójnego zakupu. |
| [ ] | SIGN-03 / P1 | Zmiana stanu, właściciela, ceny i podatek. | Tabliczka aktualizuje się zgodnie z polityką `signs.auto-update`. |
| [ ] | SIGN-04 / P1 | Zmień `signs.layouts`, format i liczbę okresów opłaty. | Cztery poprawne wiersze i oczekiwane obciążenie. Nieprawidłowa długość układu jest odrzucona. |
| [ ] | SIGN-05 / P0 | Obcy niszczy powiązaną tabliczkę; administrator odpina ją. | Ochrona przed nieuprawnionym usunięciem; brak osieroconego powiązania po legalnym odpięciu. |
| [ ] | SIGN-06 / P1 | Zmieniony zasięg wskazania, brak tabliczki i nieistniejący lokal. | Czytelna odmowa bez wyjątku i działania na innej tabliczce. |
| [ ] | TP-01 / P1 | Właściciel ustawia i używa `sethome`/`home`. | Właściwy świat, lokal i punkt wewnątrz bryły. |
| [ ] | TP-02 / P0 | Obcy oraz członek z/bez `TELEPORT`. | Kontrola praw również po zmianie roli i własności. |
| [ ] | TP-03 / P0 | Niebezpieczny/brakujący punkt: ściana, brak miejsca na głowę, niezaładowany świat. | Odmowa albo bezpieczne miejsce; brak utknięcia i nieuzasadnionej utraty pieniędzy. |
| [ ] | TP-04 / P1 | Koszt, dokładne saldo, cooldown i teleport wyłączony. | Zachowanie zgodne z konfiguracją; administrator testowany oddzielnie z bypassu. |

## 17. Reset, snapshot, porzucenie i operacje administracyjne

Pełny scenariusz `qa_reset`:

1. Zarejestruj gotowy budynek i zapisz znane pozycje bloków kontrolnych.
2. Kup go zwykłym kontem; zmień kilka bloków i dodaj skrzynię z kontrolnymi przedmiotami.
3. Wykonaj `/plots admin health reset qa_reset` i `/plots admin reset qa_reset` bez `confirm`.
4. Potwierdź, że podgląd nie odebrał właściciela i niczego nie wkleił.
5. Wykonaj `/plots admin reset qa_reset confirm`.
6. Obserwuj stan w trakcie, wynik bloków, własność, członków i dostęp po końcu.
7. Uruchom serwer ponownie i sprawdź te same dane.

| PASS | ID / priorytet | Czynność | Oczekiwany wynik |
| :--- | :--- | :--- | :--- |
| [ ] | RESET-01 / P0 | Prawidłowe odtworzenie znanego wzorca. | Zgodne bloki wewnątrz bryły; otoczenie i drugi lokal bez zmian. |
| [ ] | RESET-02 / P0 | Podgląd bez potwierdzenia. | Brak mutacji świata, własności i dostępu. |
| [ ] | RESET-03 / P0 | Brak/uszkodzony schemat, zły świat lub wymiary. | Odmowa przygotowania; brak cichego zwolnienia własności jako „sukcesu”. |
| [ ] | RESET-04 / P0 | Dwa resety jednocześnie i inne operacje podczas resetu. | Brak kolizji; blokady zwalniane po odmowie/błędzie. |
| [ ] | RESET-05 / P0 | Zatrzymanie i restart w trakcie porcjowanego resetu. | Wznowienie albo jawny stan wymagający naprawy; brak niechronionej, częściowo odtworzonej własności. |
| [ ] | RESET-06 / P1 | Różne `block-batch-size` i `tick-delay`, większy schemat. | Ukończenie bez utraty bloków; zmierz wpływ na ticki. |
| [ ] | RESET-07 / P0 | Skrzynie, block entities, wnętrze i granice schematu. | Wynik porównany ze wzorcem; resztki przedmiotów/encji opisane, brak możliwości duplikacji. |
| [ ] | RESET-08 / P0 | `snapshot` po zmianie budynku, następnie kolejna zmiana i reset. | Odtwarza nowy świadomie zapisany wzorzec. |
| [ ] | RESET-09 / P0 | `abandon` bez potwierdzenia i z potwierdzeniem. | Najpierw zasady; potem zwrot zgodny z parametrem działki i właściwa utrata własności. |
| [ ] | RESET-10 / P0 | Porzucenie podczas aktywnego najmu/aukcji lub przy błędzie zwrotu. | Brak obejścia blokad; zachowany ślad faktycznych rozliczeń. |
| [ ] | RESET-11 / P0 | `delete` na osobnym lokalu. | Usunięty rekord, region i powiązania; brak deklaracji odtworzenia budynku. |
| [ ] | RESET-12 / P1 | `disable`, `enable`, `setowner` oraz próby w stanie konfliktu. | Prawidłowe stany i odmowa kolidujących zmian. |

`reset` i `restore` korzystają z mechanizmu oddania nieruchomości do ponownego użytku. Obie ścieżki sprawdź jako operacje odbierające własność. Nie przedstawiaj `restore` jako naprawy budynku z zachowaniem właściciela. `delete` nie wkleja schematu.

## 18. Historia, audyt, wiadomości i budżet

| PASS | ID / priorytet | Czynność | Oczekiwany wynik |
| :--- | :--- | :--- | :--- |
| [ ] | HIST-01 / P1 | Zakup, podatek, najem, transfer, aukcja i zwrot. | Każda operacja osiągalna w odpowiednim widoku z ID i właściwą kwotą. |
| [ ] | HIST-02 / P0 | Obcy odczytuje prywatną historię działki. | Odmowa; właściciel i uprawniony administrator mają dostęp. |
| [ ] | HIST-03 / P1 | `privacy.public-history` false/true. | Zgodna polityka; widok `all` zwykłego gracza nadal dotyczy jego operacji. |
| [ ] | HIST-04 / P1 | Duża historia, kolejne strony i `legacy`. | Zgodna kolejność, brak pomijania wpisów o tym samym czasie. |
| [ ] | HIST-05 / P1 | Audyt edycji członków, roli, ustawień i statusu. | Operator, cel, zmiana i powiązanie finansowe są możliwe do ustalenia. |
| [ ] | HIST-06 / P1 | Wiadomość utworzona podczas nieobecności adresata i restart. | Kolejka przetrwa, wiadomość wysłana po wejściu, zapisane potwierdzenie wysłania. |
| [ ] | HIST-07 / P1 | Retencja wysłanych i niewysłanych powiadomień. | Limity usuwają właściwe wpisy; niewysłane nie giną przez zwykłe obcięcie historii. |
| [ ] | HIST-08 / P1 | Ponowne wejście po doręczeniu i przerwanie między wysłaniem a potwierdzeniem. | Normalnie bez powtórek; dopuszczalna powtórka po awarii jest opisana, nie nazwana potwierdzeniem przeczytania. |
| [ ] | HIST-09 / P0 | `budget add/withdraw`, błąd zapisu i restart. | Saldo księgi oraz historia zgodne; błąd nie zostawia niezatwierdzonego salda w pamięci. |
| [ ] | HIST-10 / P1 | Zmiana `initial-balance` na istniejącej księdze. | Nie zeruje ani nie inicjalizuje ponownie istniejącego salda. |

## 19. Awarie finansów i ręczne uzgodnienie

Te próby wymagają kontrolowanego providera/harnessu lub przygotowanych testów programistycznych. Plugin nie ma komend typu `/plots test crash` lub „odmów kolejnej wypłaty”. Zwykłe wyłączenie serwera w losowej chwili nie udowadnia testu konkretnego etapu płatności.

Wykonuj kampanię na odrębnej kopii. Po każdej próbie zapisz faktyczne salda i stan dziennika przed jakimkolwiek ręcznym uzgodnieniem.

| PASS | ID / priorytet | Punkt przerwania / błąd | Wymagany rezultat |
| :--- | :--- | :--- | :--- |
| [ ] | FAIL-01 / P0 | Zapis zamiaru przed wywołaniem Vault. | Trwały zamiar i rozpoznawalny stan po restarcie. |
| [ ] | FAIL-02 / P0 | Vault wykonał pobranie, odpowiedź nie została utrwalona. | Niepewny wynik do uzgodnienia; brak automatycznego ponownego pobrania. |
| [ ] | FAIL-03 / P0 | Pobrano kupującemu, nie wypłacono sprzedającemu. | Dziennik i zwrot pozwalają uzgodnić zobowiązanie bez utraty śladu. |
| [ ] | FAIL-04 / P0 | Wypłacono sprzedającemu, zapis właściciela zawiódł. | Brak automatycznej drugiej wypłaty; blokada kolejnych konfliktujących operacji. |
| [ ] | FAIL-05 / P0 | Własność zapisana, końcowy zapis statusu operacji zawiódł. | Uzgodnienie rozpoznaje rzeczywisty sukces bez powtórzenia finansów. |
| [ ] | FAIL-06 / P0 | Jawna odmowa zwrotu kaucji/rezerwacji. | Zobowiązanie przetrwa i zostaje ponowione zgodnie z opóźnieniem. |
| [ ] | FAIL-07 / P0 | Niepewny wynik wypłaty zwrotu. | Brak ślepego automatycznego ponawiania. |
| [ ] | FAIL-08 / P0 | Dysk pełny, odmowa zapisu lub utrata MySQL. | Błąd widoczny; brak fałszywego zakończenia i rozpoznawalny stan po przywróceniu usługi. |
| [ ] | FAIL-09 / P1 | Jedna uszkodzona umowa/aukcja podczas cyklu. | Inne operacje nadal przetwarzane, uszkodzony przypadek wskazany w logu. |
| [ ] | FAIL-10 / P0 | Równoczesne ponowienie tego samego ID. | Brak drugiego rozliczenia i duplikacji zwrotu. |

### Procedura sprawdzenia i uzgodnienia

```text
/plots admin transactions pending
/plots admin transactions inspect <id_operacji>
/plots admin transactions refunds
```

1. Zidentyfikuj aktorów, kwotę, ID działki i wszystkie kroki płatności.
2. Sprawdź niezależnie portfele, stan właściciela/umowy/aukcji, budżet i historię.
3. Doprowadź rzeczywiste dane i rozliczenie do zamierzonego stanu; zapisz dowody.
4. Dopiero wtedy oznacz ustalony wynik odpowiednią komendą.

```text
/plots admin transactions resolve <id_operacji> COMPLETED confirm <opis_uzgodnienia>
/plots admin transactions resolve <id_operacji> CANCELLED confirm <opis_uzgodnienia>
/plots admin transactions retry <id_zwrotu>
/plots admin transactions refund-resolve <id_zwrotu> PAID confirm <opis_uzgodnienia>
/plots admin transactions refund-resolve <id_zwrotu> PENDING confirm <opis_uzgodnienia>
/plots admin transactions refund-resolve <id_zwrotu> CANCELLED confirm <opis_uzgodnienia>
```

To alternatywne decyzje zależne od ustalonego wyniku, nie lista do wykonania kolejno na jednej operacji. `PAID` potwierdza wykonaną wypłatę, a `PENDING` dopuszcza następną próbę tylko po sprawdzeniu, że poprzednia nie nastąpiła. Samo uzgodnienie nie jest wypłatą ani naprawą własności; obsługa może następnie uruchomić przetwarzanie kwalifikujących się zwrotów. Nie usuwaj dziennika w celu odblokowania działki.

## 20. Backendy, migracja i kopie zapasowe

### 20.1. Macierz backendów

| Zestaw | Testy obowiązkowe |
| :--- | :--- |
| YAML | Zakup, zapis/odczyt członków, terminy, najem, aukcja, budżet, historia i restart. |
| SQLite | Ten sam zestaw oraz sterownik natywny w gotowym JAR-ze. |
| MySQL | Ten sam zestaw oraz utrata połączenia, powrót usługi, poprawne błędy i używana wersja serwera bazy. |
| Migracje | SQLite → YAML → SQLite i osobno obsługiwane przejścia do/z docelowego MySQL. |

Nie wymagaj zgodności mechanizmu wielu równocześnie zapisujących serwerów. riftPlots utrzymuje lokalny cache; wspólna baza MySQL nie stanowi synchronizacji sieci.

### 20.2. Migracja na kontrolowanej kopii

- [ ] **DATA-01 · P0** — źródło jest aktywnym backendem; cel jest pusty i oddzielony od produkcji.
- [ ] **DATA-02 · P1** — źródło = cel oraz niewłaściwe źródło są odrzucane.
- [ ] **DATA-03 · P0** — migracja zachowuje UUID, członków, prawa, nadpisania, ceny, terminy, tabliczki, umowy, aukcje, przebicia, budżet i audyt.
- [ ] **DATA-04 · P0** — podczas migracji zablokowane są kolidujące zapisy; diagnostyka i log nie sugerują ukończenia przed końcem.
- [ ] **DATA-05 · P0** — po sukcesie utrzymana blokada do restartu; po restarcie działa docelowy backend.
- [ ] **DATA-06 · P0** — błąd kopiowania nie powoduje przełączenia na niekompletny cel; źródło pozostaje dostępne do odzyskania.
- [ ] **DATA-07 · P1** — `admin migrate resume confirm` przywraca użycie źródła zgodnie z procedurą; kopia celu traktowana jako nieaktualna.
- [ ] **DATA-08 · P0** — lokalne `operations`, `refunds`, `notifications`, `transfers.yml` i schematy pozostają częścią danych niezależnie od backendu.

```text
/plots admin migrate SQLITE YAML
# Po sukcesie i restarcie — dopiero po przygotowaniu pustego celu:
/plots admin migrate YAML SQLITE
```

Źródło i cel w przykładzie muszą odpowiadać faktycznemu stanowi. Nie przełączaj `storage.type` na cel przed kopiowaniem. Szczegóły: [migracja i kopie](docs/wiki/Migracja-i-kopie-zapasowe.md).

### 20.3. Spójna kopia i realne odtworzenie

Po zatrzymaniu wszystkich zapisów serwera i ekonomii, z katalogu repozytorium:

```powershell
python scripts/backup.py create --server-dir C:/serwery/riftplots-qa --output C:/kopie/riftplots-qa.zip --server-stopped
python scripts/backup.py verify C:/kopie/riftplots-qa.zip
python scripts/backup.py restore C:/kopie/riftplots-qa.zip --target C:/serwery/riftplots-qa-odtworzony
```

Ścieżki są przykładami do zastąpienia. `--server-stopped` jest deklaracją operatora, nie poleceniem zatrzymania procesu. Dla zewnętrznego SQL dołącz odpowiednie spójne dumpy przez `--sql-dump <plik>`. Narzędzie przechowuje dumpy, ale nie importuje ich automatycznie do serwera SQL.

- [ ] **BACKUP-01 · P0** — archiwum zawiera świat, dane pluginu, schematy, dane ekonomii i potrzebny SQL.
- [ ] **BACKUP-02 · P0** — manifest SHA-256 weryfikuje wszystkie zapisane pliki.
- [ ] **BACKUP-03 · P1** — uszkodzona kopia archiwum jest odrzucana; istniejący niepusty katalog docelowy nie jest nadpisywany.
- [ ] **BACKUP-04 · P0** — uruchomiono osobno odtworzony serwer, z odseparowanymi bazami i ekonomią.
- [ ] **BACKUP-05 · P0** — własność, salda, umowy, depozyty, historia, bloki i schematy odpowiadają wspólnemu punktowi odtworzenia.
- [ ] **BACKUP-06 · P0** — oczekujące operacje i zwroty zachowane; brak automatycznej wypłaty drugi raz.
- [ ] **BACKUP-07 · P1** — zmierzono czas wykonania i odtworzenia kopii oraz zapisano instrukcję dla administratora.

Odtworzenie samej bazy działek bez ekonomii i świata nie jest zaliczeniem testu spójnego backupu. Powrót do starszego JAR-a wymaga zgodnej kopii schematu; nie uruchamiaj go bezpośrednio na nowszych danych.

## 21. Wydajność i dłuższa obserwacja

Przed pomiarem zapisz sprzęt, RAM, parametry JVM, backend, liczbę działek, graczy, wielkość schematów i konfigurację pozostałych pluginów. Wykonaj porównanie do tego samego serwera bez aktywności riftPlots. Benchmark indeksu nie mierzy TPS całego serwera.

| Próba | Obciążenie | Co zapisać |
| :--- | :--- | :--- |
| Indeks | 100 / 1 000 / 10 000 działek, wyszukiwanie i zmiana granic. | Czas zapytań, zgodność wyniku, aktualizacja/usuwanie wpisów. |
| Ruch | Gracze przechodzą przez granice, piętra i różne chunki. | MSPT, TPS, użycie CPU i alokacje. |
| GUI | Równoczesne strony i filtrowanie dużej listy. | Opóźnienie otwarcia, poprawność wyników, skoki ticków. |
| Finanse | Serie zakupów/podatków na różnych lokalach. | Czas operacji, synchroniczne zapisy, wzrost historii i zgodność sald. |
| Scheduler | Wiele umów, podatków i aukcji kończących się w podobnym czasie. | Czas przebiegu, opóźnienie obsługi, błędy pojedynczych elementów. |
| Reset | Różne objętości i kilka zadań. | Czas ukończenia, p95/p99 MSPT, kolejka zadań i poprawność świata. |
| Długie działanie | 12–24 h z cyklicznymi zdarzeniami i logowaniami. | RAM po rozgrzaniu i GC, otwarte połączenia/pliki, rozmiar kolejek, liczba wyjątków. |

- [ ] **PERF-01 · P1** — przyjęto konkretne progi dla docelowego sprzętu przed testem, np. budżet p95 MSPT, czas otwarcia GUI i dopuszczalne opóźnienie schedulera.
- [ ] **PERF-02 · P1** — zapisano p50/p95/p99 oraz największe skoki; sama średnia nie ukrywa przycięć.
- [ ] **PERF-03 · P0** — brak utraty danych i błędnych finansów pod obciążeniem.
- [ ] **PERF-04 · P1** — kolejki, przypomnienia i zasoby nie rosną bez końca po zakończeniu aktywności.
- [ ] **PERF-05 · P1** — duża działka przekraczająca limit indeksowanych chunków nadal jest poprawnie chroniona i odnajdywana.
- [ ] **PERF-06 · P1** — zatrzymanie serwera z kolejką I/O kończy się w kontrolowany sposób; błędy niedokończonych zadań są widoczne.

Nie obiecuj konkretnej liczby graczy na podstawie samej liczby działek. Zwykłe płatności i trwałe zapisy pozostają synchroniczne; przy wolnym dysku lub SQL mogą wpływać na czas ticka.

## 22. Przypadki przekrojowe i wygoda gracza

- [ ] **UX-01 · P1** — nowy gracz kupuje i znajduje swój lokal bez instrukcji od administratora.
- [ ] **UX-02 · P1** — z samego GUI potrafi odczytać cenę, podatek, okres, czynsz, kaucję i stan odnowienia.
- [ ] **UX-03 · P1** — wszystkie nieodwracalne potwierdzenia przedstawiają rzeczywiste skutki; komendy działające od razu są opisane w pomocy i dokumentacji.
- [ ] **UX-04 · P1** — zwykły gracz rozumie odmowę zakupu/najmu i potrafi wskazać, co blokuje operację.
- [ ] **UX-05 · P1** — GUI działa w typowych ustawieniach skali klienta; długie lore nie zasłania najważniejszych danych.
- [ ] **UX-06 · P1** — polskie znaki, nazwy z odstępami, długie powody i komendy z niepoprawnymi argumentami nie wywołują wyjątków.
- [ ] **UX-07 · P0** — reconnect, opóźnione pakiety i szybkie zamykanie menu nie powielają operacji.
- [ ] **UX-08 · P1** — dokumentacja opisuje faktycznie dostępne komendy, role i ograniczenia, bez prezentowania P3 jako gotowej funkcji.

## 23. Zgłoszenie błędu i powtórka

```markdown
### BUG: krótki opis
- ID testu:
- Priorytet / status:
- Commit i SHA-256 JAR-a:
- Wersje środowiska i backend:
- Konto, UUID, uprawnienia:
- Działka / oferta / aukcja / ID operacji:
- Stan oraz salda przed:
- Kroki odtworzenia:
  1.
  2.
- Oczekiwany wynik:
- Faktyczny wynik oraz salda po:
- Zachowanie po restarcie:
- Log / screenshot / nagranie / kopia rekordu:
- Częstotliwość: zawsze / X na Y prób:
- Wersja poprawki i wynik powtórki:
```

Po naprawie finansów sprawdź również zwroty, powtórzenie operacji i restart. Po naprawie GUI sprawdź nieaktualne okno oraz manipulację ekwipunkiem. Po naprawie ochrony powtórz scenariusz na piętrze, granicy i koncie bez bypassu.

## 24. Kryteria decyzji o wydaniu

- [ ] **REL-01** — wszystkie P0 mają `PASS`; brak nierozpoznanego przepływu pieniędzy lub utraty dostępu.
- [ ] **REL-02** — P1 zakończone albo jawnie wyłączone z zakresu wydania wraz z uzasadnieniem i zgodną dokumentacją.
- [ ] **REL-03** — dołączone raporty dotyczą dokładnie tego samego JAR-a, który będzie wdrażany.
- [ ] **REL-04** — pełny przebieg z prawdziwymi graczami i docelową ekonomią wykonany na kopii serwera.
- [ ] **REL-05** — odtworzona spójna kopia została uruchomiona i sprawdzona, a nie tylko poprawnie rozpakowana.
- [ ] **REL-06** — wydajność zaakceptowana dla konkretnego sprzętu i spodziewanego obciążenia.
- [ ] **REL-07** — administrator umie znaleźć oraz uzgodnić operację wymagającą sprawdzenia i zna procedurę przywrócenia danych.
- [ ] **REL-08** — CI wykonane dla wydawanego commita; do publicznej dystrybucji ustalono licencję projektu.
- [ ] **REL-09** — README, Wiki i changelog opisują finalny stan; P3, brak synchronizacji sieci i granice Vault są jasno rozdzielone od wdrożonych funkcji.
- [ ] **REL-10** — przywrócono normalne długości okresów, anti-snipe i ustawienia demo; usunięto sondę integracyjną z wdrożenia.

## 25. Szybka checklista po każdej aktualizacji

1. Start i `health` bez nowych błędów.
2. Zakup jednego lokalu, kontrola salda i właściciela.
3. Obcy zablokowany, członek ma tylko przyznane prawa, piętro zachowuje odrębność.
4. Podatek oraz termin zgodne po wpłacie.
5. Najem, koniec dostępu i rozliczenie kaucji.
6. Transfer i aukcja — zgodna cena, prowizja i historia.
7. Zmieniony przycisk GUI nadal wykonuje właściwą akcję i sprawdza wycenę.
8. Reset próbnego lokalu przywraca wzorzec i nie narusza sąsiada.
9. Restart zachowuje stan, salda, terminy i historię.
10. Kopia i kontrolny odczyt/odtworzenie zgodnie z harmonogramem utrzymania.

## 26. Powiązanie z NOWOSCI.md

| Punkt planu | Główne rozdziały testów |
| :--- | :--- |
| 1. Dziennik operacji | 8, 12, 13, 19, 20. |
| 2. Zwroty i kaucje | 11, 13, 18, 19. |
| 3. Rynek z paginacją | 8, 14, 15, 22. |
| 4. Kontrola najmu | 11, 19. |
| 5. Aukcje | 13, 19, 20. |
| 6. Role i dostęp | 10, 14, 16. |
| 7. Dziedziczenie | 7, 9, 15. |
| 8. Granice 3D | 7, 14. |
| 9. Historia i wiadomości | 18, 19, 20. |
| 10. Diagnostyka i reset | 6, 17, 19. |
| 11. Wydajność | 20, 21. |
| 12. Wydania, CI i backup | 5, 20, 24. |
| Dodatkowo: konfiguracja i GUI | 14, 15, 16, 22. |
| 13. P3 | Poza obecnym zakresem odbioru; osobne kryteria po wdrożeniu integracji. |
