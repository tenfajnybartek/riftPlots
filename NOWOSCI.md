# NOWOŚCI — poprawki i plan rozwoju riftPlots

[← README](README.md) · [Wiki](docs/wiki/Home.md) · [Poprzedni przegląd](docs/review.md)

Stan: **10 listopada 2026 r.**, riftPlots **1.0.0**, wdrożenie **P1–P2**. Punkty 1–12 zrealizowano lokalnie. Pierwotny plan poniżej zachowano jako zapis historyczny. Aktualne komendy i zasady opisuje [przewodnik P1–P2](docs/wiki/Nowosci-P1-P2.md).

## Wdrożenie P1–P2

Zakres obejmuje punkty 1–12 oraz uwagi P1/P2. Punkt 13 (P3) pozostaje pomysłem na przyszłość. Kod i zaktualizowane źródła Wiki są lokalnie; nie wdrażano ich na produkcję. Licencję projektu nadal wybiera właściciel.

| Obszar | Dostarczone |
| :--- | :--- |
| 1. Dziennik operacji | Trwałe etapy Vault, ID zamiaru i prób, blokada kolizji, restart i `admin transactions`. |
| 2. Zwroty | Trwałe zobowiązania i ponawianie odmów; niepewna wypłata wymaga uzgodnienia. Wygaśnięcie odbiera dostęp niezależnie od kaucji. |
| 3. Rynek | Strony po 45 pozycji, cztery zakładki, sortowanie, filtry, ID w PDC i ponowne potwierdzenie zmienionej ceny. |
| 4. Najem gracza | Informacje, świadome odnowienie, potwierdzane zakończenie i opcjonalne oferty właściciela. |
| 5. Aukcje | Trwałe przebicia YAML/SQL i migracja, panel, kup teraz, anulowanie z powodem i opcjonalny escrow. |
| 6. Dostęp | Edytor roli i praw, ochrona najmu, synchronizacja WorldGuard oraz audyt zmian. |
| 7. Dziedziczenie | Maska nadpisań, cena typu × dzielnica, zachowanie starszych danych, podgląd i potwierdzenie `apply-defaults`, ikony i flagi stanu. |
| 8. Granice 3D | Cząsteczki dla jednego gracza, limity, czas i cooldown; podgląd z okna zakupu. |
| 9. Wiadomości i historia | Trwała kolejka i retencja, potwierdzenia wysłania przez serwer, prywatna historia, strony SQL i audyt operatora oraz stanu. |
| 10. Diagnostyka i reset | Read-only `health`, aktywny backend, walidacja schematu przed zwolnieniem własności i sprzątanie blokad. |
| 11. Wydajność | Indeks świata/chunka, benchmark 100/1 000/10 000 działek, uporządkowana kolejka migracji i blokada zmian do restartu. |
| 12. Utrzymanie | Przypięte API i wrapper, workflow CI z MySQL, schemat v2, backup SHA-256 i próba odtworzenia. |

Poprawiono także ścisłe argumenty i kwoty, rabat eventu przed kontrolą salda, izolację błędów umów/aukcji, wygaszanie przypomnień i przywracanie kopii obiektów po błędzie zapisu.

### Weryfikacja P1–P2

- **102 testów Java, 0 niepowodzeń, 0 błędów, 0 pominiętych** przy włączonym teście rzeczywistego MySQL 8.4.10; `test build` zakończone powodzeniem.
- **6 testów Python** narzędzia backupu, w tym uszkodzona suma, ścieżka poza archiwum i odmowa nadpisania.
- **183 pliki Java** sprawdzone przez inspekcje IDE bez błędów na poziomie `error`.
- Rzeczywisty Paper 26.3 build 246 z WorldEdit 7.4.6, WorldGuard 7.0.21 i Vault 1.7.3: **45 sprawdzeń w trzech fazach (24 + 11 + 10)** — zakup, piętra, najem, aukcja, GUI, restart, reset i odtworzona kopia. Końcowy JAR przeszedł próbę odtworzenia, tworzenia/anulowania aukcji oraz rzeczywistego GUI z ID aukcji.
- Raporty lokalne: `build/reports/tests/test/index.html`, `build/reports/benchmarks/spatial-index.json`, `build/integration-release/report-phase1.yml`, `build/integration-release/report-phase2.yml` i `build/integration-release-restored/plugins/RiftPlotsProbe/report.yml`. Backup ma 253 pliki zweryfikowane SHA-256. Serwery testowe i MySQL zostały zatrzymane.

### Granice gwarancji

Vault i baza nie tworzą wspólnej transakcji: niepewny wynik jest blokowany do uzgodnienia. Zwykłe płatności i trwały zapis dziennika pozostają synchroniczne; kolejka obsługuje migrację. Benchmark mierzy wyszukiwanie, nie TPS. Test Paper używa zastępczych obiektów graczy; pełna rozgrywka i wszystkie mechaniki fizyki wymagają osobnej próby. MySQL nie synchronizuje kilku procesów. Workflow jest przygotowany, ale nie był uruchamiany na GitHub w ramach tej pracy.

## Czy plugin ma błędy?

**Tak — ponowny przegląd wykrył błędy logiczne mimo poprawnego builda i przechodzących wcześniejszych 53 testów.** Nowe testy odtworzyły problemy z błędami zapisu, usuwaniem działek, karencją, rozliczeniem prowizji oraz nieprawidłowymi kwotami Vault. Te przypadki zostały poprawione.

P1–P2 dodało trwałe operacje, zaległe zwroty i narzędzia odzyskiwania danych. Nie oznacza to gwarancji braku wszelkich błędów ani atomowości zewnętrznego Vault.

## Poprawione w tym przeglądzie

| Obszar | Problem i skutek | Wprowadzona poprawka |
| :--- | :--- | :--- |
| Usuwanie działki | `PlotManager` usuwał działkę z cache i jej region przed zakończeniem usuwania rekordu. Błąd bazy mógł pozostawić nieruchomość bez ochrony pluginu do restartu. | Najpierw usuwany jest trwały rekord. Nieudana operacja zachowuje działkę w cache i region. Powiązania tabliczek usuwane są po usunięciu działki. |
| Usuwanie YAML | Wynik `File.delete()` był ignorowany, więc nieudane usunięcie pliku wyglądało na sukces. | Nieudane usunięcie zgłasza wyjątek do wywołującego. |
| Zapis nowej działki | Błąd zapisu nie powstrzymywał dodania nowego obiektu do cache. | Nowa działka trafia do cache dopiero po udanym zapisie repozytorium. |
| Budżet miasta | Błąd zapisu YAML lub SQL pozostawiał zmienione saldo w pamięci. Kolejna operacja mogła uwzględnić niezapisaną transakcję. | Saldo i historia w pamięci zatwierdzane są po udanym zapisie. |
| Pliki YAML | Część danych była zapisywana bezpośrednio do istniejącego pliku. Przerwany zapis mógł uszkodzić poprzednią zawartość. | Repozytoria, najem, aukcje, budżet, audyt i eksport danych usług zapisują kompletny plik tymczasowy przed zastąpieniem poprzedniego. |
| Aukcje i transfery | Dla kwot z ułamkiem grosza zaokrąglenie mogło wypłacić więcej niż pobrano albo naliczyć prowizję większą od ceny. | Prowizja jest liczona w `BigDecimal`, ograniczona do ceny, a wypłata stanowi dokładną różnicę. |
| Vault | Kwoty ujemne i `null` były traktowane jak darmowa operacja. Bardzo duże lub małe liczby mogły zamienić się odpowiednio w `Infinity` lub zero przy konwersji do `double`. | Nieprawidłowe kwoty są odrzucane przed wywołaniem providera. Kwota zero pozostaje prawidłowa. |
| Karencja | Zapisany stan zaległości bez daty końca karencji otrzymywał nową karencję od chwili sprawdzenia. | Jeżeli istnieje termin podatku, od niego wyliczana jest karencja. |
| Scheduler | Wyjątek jednej działki zatrzymywał pozostałą część przebiegu, w tym sprawdzanie najmu i aukcji. | Błędy działek są izolowane i logowane. Awaria sprawdzania najmu nie blokuje wywołania obsługi aukcji. |
| Ogłoszenia o wygaśnięciu | Informacja o utracie własności pojawiała się przed sprawdzeniem, czy reset rzeczywiście został przyjęty. | Ogłoszenie następuje po przyjęciu resetu albo zapisaniu zwolnienia działki bez resetu. |
| Pamięć | Audyt przechowywał nieużywaną listę wpisów, a budżet SQL dodatkową historię, mimo odczytywania jej z bazy. | Usunięto zbędne przechowywanie tych wpisów w pamięci. |

Główne zmiany: [PlotManager](src/main/java/pl/riftplots/service/plot/PlotManager.java), [YamlDataFile](src/main/java/pl/riftplots/storage/yaml/YamlDataFile.java), [CityBudgetService](src/main/java/pl/riftplots/service/city/CityBudgetService.java), [AuctionService](src/main/java/pl/riftplots/service/auction/AuctionService.java), [PlotTransferService](src/main/java/pl/riftplots/service/transfer/PlotTransferService.java), [VaultHook](src/main/java/pl/riftplots/integration/vault/VaultHook.java) i [PlotTaxScheduler](src/main/java/pl/riftplots/scheduler/PlotTaxScheduler.java).

W aukcjach zaokrąglana jest teraz najpierw prowizja. Przykładowo cena `0,10` i prowizja `5%` dają `0,01` dla miasta oraz `0,09` dla sprzedawcy. Suma wypłaty i prowizji odpowiada pobranej cenie.

Zapis YAML próbuje atomowego zastąpienia pliku. Jeżeli system plików tego nie obsługuje, zastępuje go gotowym plikiem przez zwykłe przeniesienie. Nie jest to jedna atomowa transakcja obejmująca wszystkie pliki, SQL i Vault.

## Uwagi przed wdrożeniem — zapis historyczny

Poniższa tabela dokumentuje stan wyjściowy, rozwiązany przez wdrożenie opisane powyżej. Priorytety: **P1** — integralność danych i rozliczeń; **P2** — obsługa, wydajność i utrzymanie; **P3** — dodatkowe możliwości.

| Priorytet | Obserwacja w obecnym kodzie | Następny krok |
| :--- | :--- | :--- |
| P1 | Płatność Vault i zapis własności nie są wspólną transakcją. Awaria po wypłacie sprzedawcy, przed zapisaniem własności, może pozostawić niespójne dane. | Trwały dziennik operacji, blokada powtórzeń i procedura uzgodnienia niepewnych płatności. |
| P1 | `RentalService.terminateRental()` zwraca `false` po odmowie zwrotu kaucji, zachowując aktywną umowę i dostęp najemcy. Dotyczy to również wygasłej umowy. | Oddzielić zakończenie prawa dostępu od zobowiązania zwrotu pieniędzy. |
| P1 | Obiekty `Plot`, `RentalAgreement` i `PlotAuction` są mutowalne. Nie wszystkie zmiany w pamięci są wycofywane po nieudanym zapisie istniejącego obiektu. | Przygotowywać zmiany na kopii stanu lub w operacji z kontrolowanym zatwierdzeniem. Poprawka nowej działki nie rozwiązuje wszystkich tych ścieżek. |
| P1 | `PlotResetService` zwalnia `activeResets` w wybranych ścieżkach; wyjątek przed uruchomieniem zadania może pozostawić blokadę do restartu. Wymiary i zawartość schematu sprawdzane są po odebraniu własności. | Wstępna walidacja resetu i bezwarunkowe sprzątanie blokady przy nieprzyjętej operacji. |
| P1 | Przegląd nie zastępuje uruchomienia rzeczywistego Paper z WorldEdit, WorldGuard, Vault i MySQL. | Powtarzalny scenariusz integracyjny przed wydaniem. |
| P2 | Nieudane rozliczenie jednej umowy lub aukcji może przerwać sprawdzanie pozostałych elementów wewnątrz danej usługi. Scheduler izoluje grupy usług, ale nie każdą umowę i aukcję. | Izolacja błędów pojedynczych operacji wraz z ich trwałym statusem. |
| P2 | Synchroniczne SQL/YAML i migracja mogą zatrzymywać ticki. Historia YAML wymaga odczytu lub zapisu rosnących plików. | Pomiary, indeks działek, paginowane zapytania i kolejka I/O z zachowaniem kolejności zapisów. |
| P2 | `PlotTaxScheduler.sentReminders` zachowuje klucze dawnych terminów do zatrzymania schedulera. | Wygaszanie nieaktualnych wpisów i ograniczenie częstotliwości ostrzeżeń. |
| P2 | `HistorySubCommand` pozwala odczytać historię wskazanej działki bez sprawdzania własności. | Ustalić, czy historia ma być publiczna; dla prywatnej historii dodać kontrolę i osobne uprawnienie administracyjne. |
| P2 | Błędna liczba okresów w `pay`, `extend` lub `rent` może zostać zastąpiona jednym okresem. | Odrzucać błędne argumenty przed uruchomieniem płatności i pokazywać składnię. |
| P2 | Event zakupu/podatku jest emitowany po wstępnym sprawdzeniu salda. Rabat nadany przez dodatek może nie pomóc graczowi, którego nie stać na pierwotną kwotę. | Ustalić końcową cenę przez event, następnie sprawdzić saldo i wykonać płatność. |
| P2 | Po restarcie znikają oferty transferu i kolejka powiadomień; zapisywana jest tylko aktualna oferta aukcji. | Utrwalanie ofert, wiadomości i pełnej historii licytacji. |
| P2 | Metadane typów nie są w pełni stosowane do działek, a mnożnik ceny dzielnicy nie wpływa na zakup. | Jawny model dziedziczenia i indywidualnych nadpisań. |
| P2 | Zależność Paper ma dynamiczną wersję; wrapper Gradle jest obecny lokalnie, ale jego skrypty i JAR nie są śledzone przez Git w sprawdzonym stanie repozytorium. | Zapewnić build ze świeżego klona i CI oraz przypiąć sprawdzony build API. |

## Pierwotny zakres funkcji i kryteria odbioru

### 1. Odzyskiwanie rozliczeń po awarii

**Priorytet: P1 · Zakres: duży · Fundament pozostałych funkcji finansowych.**

Każdy zakup, transfer, zwrot, czynsz i rozliczenie aukcji otrzymuje trwałe ID operacji oraz zapis etapów. Administrator widzi operacje wymagające interwencji zamiast zgadywać na podstawie stanu działki.

Warunki ukończenia:

- Wielokrotne kliknięcie i ponowne uruchomienie obsługi tego samego ID nie tworzą drugiego zakupu.
- Restart pomiędzy etapami nie usuwa informacji o rozpoczętej operacji.
- Niepewny wynik zewnętrznej płatności otrzymuje status wymagający uzgodnienia. Zwykłe Vault nie daje podstaw do obiecywania bezpiecznego automatycznego ponawiania każdej wypłaty.
- Testy przerywają operację przed płatnością, po pobraniu, po wypłacie i przed zapisem własności.

Propozycja narzędzi: `/plots admin transactions pending` i `/plots admin transactions inspect <id>`.

### 2. Kaucje i zwroty oczekujące

**Priorytet: P1 · Zakres: średni/duży · Zależność: dziennik rozliczeń.**

Wygaśnięcie umowy odbiera dostęp w ustalonym terminie. Jeżeli ekonomia odrzuca zwrot, plugin zachowuje osobny dług wobec najemcy i pokazuje go administracji.

Warunki ukończenia:

- Wygasły najem nie pozostaje aktywny wyłącznie z powodu odmowy zwrotu.
- Zobowiązanie przetrwa restart i ma powiązanie z konkretną umową.
- Ponawianie oraz ręczne rozliczenie chronią przed podwójną wypłatą.
- Najemca widzi kwotę i stan zwrotu w historii.

### 3. Panel nieruchomości z paginacją

**Priorytet: P2 · Zakres: średni · Duża korzyść dla gracza.**

Rozbudować obecne GUI o strony, sortowanie po cenie i podatku, filtry dzielnic i typów oraz czytelne zakładki „Moje”, „Dostępne”, „Wynajem”, „Aukcje”.

Warunki ukończenia:

- Każda działka pozostaje osiągalna także po przekroczeniu pojemności pierwszej strony.
- Przyciski przechowują ID działki lub UUID członka w danych przedmiotu, zamiast odczytywać je z nazwy wyświetlanej.
- Kliknięcie ponownie sprawdza stan, cenę i uprawnienia.
- Potwierdzenie pokazuje koszt zakupu, podatek, okres i prowizję przed płatnością.

### 4. Kontrola umowy najmu przez gracza

**Priorytet: P2 · Zakres: średni · Zależność części finansowej: bezpieczne zwroty.**

Dodać informacje o umowie, świadomy wybór auto-odnowienia i zakończenie najmu na uzgodnionych zasadach.

Proponowane komendy: `/plots rent info <id>`, `/plots rent autorenew <id> on|off`, `/plots rent cancel <id> confirm`.

Warunki ukończenia:

- Najemca widzi kolejną płatność, czynsz, kaucję i status auto-odnowienia.
- Wyłączenie odnowienia nie odbiera opłaconego już czasu.
- Wcześniejsze zakończenie pokazuje zasady zwrotu przed potwierdzeniem.
- Możliwość wystawiania własnego lokalu przez gracza wymaga osobnego uprawnienia i walidacji.

### 5. Pełny rynek aukcyjny

**Priorytet: P2 · Zakres: duży · Zależność depozytu ofert: dziennik rozliczeń.**

Uzupełnić aukcje o panel, historię przebić, osobny przycisk kup teraz i komendę administracyjnego anulowania z powodem. Opcjonalny depozyt licytacji ograniczyłby sytuacje, w których zwycięzca wydaje pieniądze przed zakończeniem aukcji.

Warunki ukończenia:

- Historia przebić przetrwa restart i ma kolejność niezależną od czasu wyświetlenia.
- Anulowanie ma jednoznaczne reguły po pierwszej ofercie.
- Przy depozycie nowa oferta blokuje środki, a poprzednia otrzymuje trwałe rozliczenie zwrotu.
- Cena kup teraz i minimalne przebicie mają wyraźną relację; interfejs pokazuje kwotę, która zostanie pobrana.

### 6. Role i edytor dostępu

**Priorytet: P2 · Zakres: średni.**

Wykorzystać istniejące role i `PlotPermission` w panelu właściciela: budowanie, niszczenie, pojemniki, drzwi, pojazdy i interakcje. Obecne `trust` pozostaje prostą metodą dodania zwykłego współużytkownika.

Warunki ukończenia:

- Zwykły członek nie może nadać sobie dodatkowych praw.
- Edytor nie pozwala ominąć zasad aktywnego najmu ani limitów typu.
- Zmiana roli aktualizuje region i ochronę pluginu.
- Dostęp jest testowany również dla mieszkań położonych jedno nad drugim.

### 7. Przewidywalne ustawienia typów i dzielnic

**Priorytet: P2 · Zakres: średni/duży.**

Rozróżnić wartości dziedziczone i ręcznie ustawione. Administrator powinien wiedzieć, skąd wynika cena i które parametry zmieni przypisanie typu.

Warunki ukończenia:

- Podgląd pokazuje wartość bazową, mnożniki i końcową kwotę.
- Proponowane `type apply-defaults` najpierw wyświetla różnice i wymaga potwierdzenia dla istniejącej działki.
- Reload nie nakłada mnożnika ponownie na już przemnożoną cenę.
- Zmiana konfiguracji nie przepisuje po cichu warunków aktywnego najmu.

### 8. Podgląd granic 3D

**Priorytet: P2 · Zakres: średni · Efekt widoczny od razu w grze.**

Proponowane `/plots border [id]` rysuje przez krótki czas granice lokalu za pomocą cząsteczek widocznych dla danego gracza. Podgląd przed zakupem pokazuje także wysokość mieszkania.

Warunki ukończenia:

- Podgląd nie zmienia bloków i nie pozostaje po wylogowaniu.
- Duże nieruchomości mają limit liczby punktów i czas odnowienia.
- Obszar odpowiada dokładnie granicom `PlotBounds`, również na innych piętrach.

### 9. Trwałe powiadomienia i jedna historia operacji

**Priorytet: P2 · Zakres: średni.**

Zapisywać wiadomości offline i połączyć widok zakupu, najmu, kaucji, transferu oraz aukcji w jedną historię. Audyt administracyjny powinien pokazywać również zmiany uprawnień i konfiguracji działki.

Warunki ukończenia:

- Powiadomienia mają ID, datę, przyczynę i potwierdzenie dostarczenia.
- Restart nie usuwa wiadomości, a stare wpisy podlegają ustalonej retencji.
- Historia ma paginację i ustaloną politykę prywatności.
- Każdy wpis finansowy prowadzi do ID operacji oraz działki.

### 10. Diagnostyka i bezpieczny reset

**Priorytet: P1 dla kontroli resetu, P2 dla panelu · Zakres: średni.**

Proponowane `/plots admin health` sprawdza aktywny magazyn, integracje, brakujące schematy i zgodność regionów. Podgląd resetu weryfikuje świat, odczyt schematu i jego wymiary przed odebraniem własności.

Warunki ukończenia:

- Raport odróżnia aktywny backend od samej wartości w przeładowanym `storage.yml`.
- Diagnostyka niczego nie naprawia ani nie resetuje automatycznie.
- Odmowa uruchomienia resetu zawsze zwalnia blokadę operacji.
- Potwierdzenie pokazuje utratę własności, zakończenie najmu i zakres odtwarzanych bloków.

### 11. Wydajność dużego miasta

**Priorytet: P2 · Zakres: duży · Zacząć od pomiarów.**

Obecne wyszukiwanie działki przegląda kolekcję. Indeks według świata i chunka może ograniczyć liczbę testów granic przy zdarzeniach graczy, płynów i tłoków.

Warunki ukończenia:

- Pomiar czasu wyszukiwania dla 100, 1 000 i 10 000 działek.
- Aktualizacja indeksu przy tworzeniu, usuwaniu i zmianie granic.
- Odczyt SQL/YAML może pracować w kolejce, ale zmiany świata i Bukkit pozostają na właściwym wątku.
- Kolejka zapewnia kolejność i zgłasza błędy zapisu; samo użycie `async` nie jest kryterium sukcesu.

### 12. Powtarzalne wydania, CI i kopie zapasowe

**Priorytet: P1/P2 · Zakres: średni.**

Zapewnić budowę ze świeżego klona na JDK 25, kontrolę spakowanego JAR-a i próby migracji. Dodać wersję schematu danych oraz procedurę wykonania i odtworzenia spójnej kopii.

Warunki ukończenia:

- Wrapper jest kompletny w repozytorium, a API Paper ma sprawdzoną, konkretną wersję.
- CI uruchamia testy, budowę i próbę natywnego SQLite z artefaktu.
- Osobne próby integracyjne obejmują docelowy MySQL i prawdziwy serwer.
- Backup obejmuje dane, schematy, świat i ekonomię; test odtwarzania potwierdza spójność zestawu.
- Licencję projektu wybiera właściciel repozytorium.

### 13. Integracje prezentacyjne

**Priorytet: P3 · Zakres: średni/duży.**

Opcjonalne placeholdery, mapa nieruchomości i eksport statystyk miasta mogą ułatwić prezentację serwera. Warstwa integracji powinna korzystać z odczytu danych i opublikowanych zdarzeń, bez omijania serwisów płatności.

Warunki ukończenia: opcjonalne zależności, brak wpływu awarii mapy na zakup, cache dla kosztownych odczytów i konfigurowalna widoczność danych właścicieli.

## Pierwotna propozycja kolejności wydań

| Etap | Zawartość | Kryterium zakończenia |
| :--- | :--- | :--- |
| Poprawki 1.0.x | Poprawki z tego przeglądu, sprawdzenie na serwerze, niezawodne uruchamianie resetu, poprawne argumenty komend. | Przechodzące regresje i scenariusz ręczny na kopii miasta. |
| Propozycja 1.1 | Dziennik rozliczeń, zwroty oczekujące, testy awarii i trwałe wiadomości. | Restart w każdym etapie płatności daje rozpoznawalny, możliwy do uzgodnienia stan. |
| Propozycja 1.2 | Paginacja GUI, kontrola najmu, role i podgląd granic. | Gracz wykonuje zwykłe operacje bez pomocy administracji. |
| Kolejny etap | Dziedziczenie ustawień, pełny rynek aukcyjny, pomiary wydajności i integracje. | Reguły cen i rozliczeń pozostają czytelne oraz pokryte testami. |

Największą wartość da najpierw domknięcie rozliczeń i zwrotów, a następnie wygodne GUI oraz kontrola najmu. Obsługę wspólnego miasta przez wiele serwerów warto potraktować jako osobny projekt: wspólna baza MySQL nie synchronizuje cache ani nie zapewnia blokad między procesami.

## Weryfikacja poprzedniego przeglądu

Dodano **17 testów regresyjnych**. Pierwsze uruchomienia nowych scenariuszy wykazały 10 niepowodzeń w dotychczasowej logice oraz 2 kolejne w walidacji Vault. Po poprawkach pełny zestaw zawiera **70 testów**.

Wynik końcowy: **70 testów, 0 niepowodzeń, 0 błędów, 0 pominiętych; build zakończony powodzeniem**. Dodatkowa inspekcja IDE dla 158 plików Java kodu i testów nie zgłosiła błędów na poziomie `error`.

```powershell
.\gradlew.bat test build --console=plain
```

Nowe testy: [StorageFailureTest](src/test/java/pl/riftplots/StorageFailureTest.java), [TaxSchedulerRegressionTest](src/test/java/pl/riftplots/TaxSchedulerRegressionTest.java), [VaultAmountValidationTest](src/test/java/pl/riftplots/VaultAmountValidationTest.java) oraz dodatkowe scenariusze w [MarketServiceTest](src/test/java/pl/riftplots/MarketServiceTest.java).

Raport po uruchomieniu: `build/reports/tests/test/index.html`. Testy sprawdzają rzeczywiste pliki i SQLite oraz logikę usług z atrapami Bukkit/Vault. Nie uruchamiają pełnego serwera Minecraft ani zewnętrznego MySQL.
