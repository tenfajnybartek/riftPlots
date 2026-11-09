# Przegląd repozytorium riftPlots

[← README](../README.md) · [Architektura](architecture.md)

Przegląd wykonano 9–10 listopada 2026 r. Objął kod produkcyjny we wszystkich pakietach, istniejące testy, sześć plików zasobów, konfigurację Gradle i wcześniejsze README. Po zmianach projekt zawiera 142 pliki Java kodu produkcyjnego oraz 12 klas testowych. Przegląd źródeł i przejście testów nie stanowią dowodu braku wszystkich możliwych błędów wykonania.

## Najważniejsze naprawy

| Obszar | Stwierdzony problem | Zmiana |
| :--- | :--- | :--- |
| `AuctionService` | Zdublowany fragment finalizacji, nieprawidłowe klamry i brak importu `RoundingMode` uniemożliwiały kompilację. | Usunięto duplikat i poprawiono strukturę klasy. |
| Rejestracja komend | Trzy wywołania konstruktorów nie przekazywały `MessageConfiguration`. | Dopasowano tworzenie komend najmu, transferu i akceptacji. |
| `TeleportService` | Settery odwoływały się do nieistniejących pól. | Usunięto nieużywane settery; źródłem ustawień jest konfiguracja. |
| Pakowanie JAR-a | Relokacja `org.sqlite` powodowała rzeczywisty `NoClassDefFoundError: org/sqlite/core/NativeDB`. | SQLite zachowuje nazwę pakietu; dodano próbę natywnego połączenia w osobnej JVM. |
| Rozliczenie aukcji | Ignorowano wynik wypłaty/pobrania środków, brakowało nowego terminu podatku i oczyszczenia dawnych członków. | Sprawdzane są wyniki operacji Vault; poprawiono stan własności, terminy i obsługę nieudanej wypłaty. |
| Dostępność aukcji | Można było uruchamiać aukcje w nieodpowiednim stanie oraz kupić działkę zwykłą komendą poza aukcją. | Walidacja parametrów i stanów, blokada bezpośredniego zakupu, kontrola sprzedawcy. |
| Cena kup teraz | Pole ceny nie kończyło aukcji. | Osiągnięcie ceny uruchamia natychmiastową finalizację. |
| Najem po restarcie | YAML odrzucał umowy, których czas minął podczas wyłączenia serwera, mimo statusu `ACTIVE`. | Umowy są odczytywane według statusu i później rozliczane. Przywracane są powiązania najmu i rola najemcy. |
| Odnowienie najmu | Ignorowano nieudane pobranie; pobrany czynsz nie trafiał do właściciela ani miasta. | Odnowienie wymaga udanej płatności i rozlicza odbiorcę czynszu. |
| Transfer | Nie weryfikowano ponownie bieżącego stanu działki, kupujący nie mógł wykonać `deny`, zachowywano dostęp dawnych członków. | Powtórna walidacja przed zapłatą, odrzucenie przez kupującego, czyszczenie dostępu i starych ofert. |
| Prowizje | Konfiguracja dopuszczała wartości powyżej 100%. | Prowizje ograniczono do zakresu 0–100%. |
| GUI | Kliknięcia w dolnym ekwipunku trafiały do obsługi przycisków; brakowało ochrony przeciągania i ponownego sprawdzania właściciela. | Obsługiwane są wyłącznie sloty górnego menu; dodano kontrolę uprawnień, własności i drag events. |
| Porzucanie | GUI oddawało działkę bez potwierdzenia; stan nie zapewniał ochrony przed ponownym zwrotem. | GUI uruchamia potwierdzenie komendą; serwis blokuje kolidujące stany i rejestruje nieudany zwrot. |
| Współużytkownicy | Można było usunąć aktywnego najemcę jak zwykłego członka; limit typu nie był sprawdzany przez `trust`. | Dodano rolę `TENANT`, kontrolę usuwania i limit dodawania członków. |
| Profil i tabliczki | Zmiany właścicieli w różnych usługach nie aktualizowały spójnie profili i tabliczek. | Synchronizację przeniesiono do ścieżki zapisu `PlotManager`. |
| Przedpłaty | Zapisany licznik nie malał z upływem okresów. | Dostępność limitu wynika z pozostałego opłaconego czasu. |
| Karencja | Po restarcie zaległa działka dostawała nową karencję liczoną od momentu sprawdzenia. | Karencja jest liczona od zapisanego terminu podatku. |
| Reset | Brak schematu uznawano za sukces, a deklarowane porcjowanie nie było używane. | Brak schematu wyłącza działkę; bloki są przywracane porcjami z kontrolą wymiarów. |
| Schematy | Nieudany zapis mógł uszkodzić poprzedni wzorzec. | Zapis przez plik tymczasowy i zastąpienie po sukcesie. Tworzenie działki wymaga zapisanego schematu. |
| Teleportacja | Wyniki pobrania opłaty i teleportacji były ignorowane; szukanie pozycji mogło wyjść ponad lokal. | Sprawdzany jest wynik, anulowany teleport zwraca koszt; poszukiwanie pozostaje w granicach działki i odrzuca niebezpieczne bloki. |
| Ochrona | Część ochrony encji działała też poza działkami; tłoki nie sprawdzały wszystkich źródeł i miejsc docelowych. | Zawężono ochronę encji do działek i uzupełniono kontrolę granic tłoków. |
| Tabliczki | Możliwa była podwójna obsługa obu rąk i duplikaty powiązań w jednym miejscu. | Tylko główna ręka, pojedyncze powiązanie oraz sprawdzanie uprawnień akcji. |
| Przeładowanie | Nowe obiekty konfiguracji nie trafiały do usług trzymających stare referencje. | Wczytywanie do istniejących obiektów, przeładowanie typów i dzielnic. |
| Ekonomia przy starcie | Plugin kontynuował pracę bez dostępnego providera Vault. | Uruchomienie jest przerywane przy braku wymaganej ekonomii. |
| Budżet i audyt | Saldo początkowe było zaszyte w kodzie, historia YAML nie była przywracana po restarcie. | Używana jest konfiguracja salda; odczyt historii i jednoznaczna kolejność operacji budżetu. |
| Migracja | Pomijano profile i dane usług, limitowano historię do 10 000 rekordów i nie zawsze zamykano połączenia. | Dodano konwersję danych usług, pełny odczyt transakcji, profile, zamykanie w `finally` i odmowę nadpisania niepustego celu. |
| Błędy magazynu | Repozytoria przechwytywały błędy i kontynuowały jak po sukcesie; uszkodzony YAML mógł wyglądać na pusty. | Błędy są zgłaszane do wywołującego, a pliki danych YAML są czytane rygorystycznie. |
| Modele i geometria | Konstruktor pustego zestawu uprawnień mógł rzucić wyjątek; awaryjne daty odwoływały się do parametru `null`; sąsiedztwo ignorowało odległość na drugiej osi. | Poprawiono konstrukcję modeli i sprawdzanie odległości w trzech osiach. |

Mockito zaktualizowano z `5.16.0` do `5.20.0`, ponieważ wcześniejszy Byte Buddy odrzucał instrumentowanie klas Javy 25. Zmianę zależności potwierdza [wydanie Mockito 5.20.0](https://github.com/mockito/mockito/releases/tag/v5.20.0); testy zostały uruchomione na JDK 25 bez flagi eksperymentalnej Byte Buddy.

## Zakres weryfikacji

Wynik końcowej weryfikacji automatycznej: **53 testy, 0 błędów i 0 niepowodzeń**, kompilacja kodu i budowa Shadow JAR-a zakończone powodzeniem.

| Sprawdzenie | Co potwierdza |
| :--- | :--- |
| `gradlew.bat test build` | Kompilację wszystkich źródeł i przejście zestawu testów. |
| Testy usług z atrapami Bukkit/Vault | Rozliczenie aukcji, kup teraz, odmowę pobrania, zwrot po nieudanej wypłacie, odrzucanie ofert i odnowienia najmu. |
| Testy plików i SQLite | Odczyt/zapis, historię budżetu po restarcie, zgłaszanie błędów oraz migrację SQLite → YAML → SQLite z danymi usług. |
| Testy ochrony | Odrzucenie kliknięć we własnym ekwipunku, drag events, starą sesję GUI po zmianie właściciela, ochronę resetu i refundację anulowanego teleportu. |
| Osobna JVM z JAR-em | Natywne połączenie SQLite z rzeczywiście spakowanego sterownika. |

Nie uruchamiano pełnego serwera Minecraft ani zewnętrznego MySQL. Wklejanie schematów w prawdziwym świecie, fizyka tłoków/płynów, integracja regionów i wydajność dużych miast wymagają próby na docelowym serwerze. Testy błędnych płatności używają atrap odpowiedzi Vault, a nie konkretnego pluginu ekonomii.

## Pozostałe ograniczenia i dalsza praca

1. **Dziedziczenie parametrów nieruchomości jest niepełne.** Polecenie przypisania typu nie stosuje jego domyślnych stawek, okresu, zwrotu ani flag. `price-multiplier` dzielnicy nie wpływa na zakup. README opisuje rzeczywiste zachowanie; pełne dziedziczenie wymaga ustalenia modelu indywidualnych nadpisań wartości.
2. **Vault i zapis pluginu nie są jedną transakcją atomową.** Sprawdzane są odpowiedzi płatności i wykonywane zwroty przy nieudanej wypłacie, ale awaria procesu lub bazy pomiędzy etapami nadal wymaga weryfikacji i ręcznego uzgodnienia danych. Nie dodano trwałego dziennika operacji pozwalającego odtworzyć każdą przerwaną transakcję.
3. **Pamięć nie jest magazynem trwałym.** Oferty transferu i kolejka powiadomień offline znikają po restarcie. Zapisana aukcja zachowuje aktualnego licytującego i kwotę, ale lista wszystkich wcześniejszych przebić z modelu nie jest utrwalana.
4. **MySQL nie zapewnia pracy sieciowej kilku serwerów.** Brakuje uzgadniania cache'ów, rozproszonych blokad i testów wyścigów między procesami. Wspólnej bazy nie należy traktować jako gotowego mechanizmu współdzielenia świata.
5. **Operacje I/O mogą zatrzymywać ticki.** Repozytoria oraz migracja są synchroniczne, a zapisy list usług YAML rosną z historią. Porcjowanie resetu ogranicza pracę jednego ticka, ale nie jest testem wydajności WorldEdit ani odczytu dużego schematu.
6. **Dokumentacja API musi uwzględniać miejsca emisji.** `PlotStateChangeEvent` nie obejmuje wszystkich zmian stanu, a zakup aukcyjny i transfer nie wysyłają `PlotBuyEvent`.
7. **Interfejs i lokalizacja są częściowe.** Menu pokazują ograniczoną liczbę pozycji bez paginacji; nie wszystkie teksty pochodzą z `lang.yml`. Brakuje osobnego polecenia gracza do wyłączenia auto-odnowienia najmu.
8. **Budowa wymaga dalszego ustabilizowania zależności.** Paper API używa dynamicznego `26.3.build.+`; repozytorium nie ma jeszcze pliku licencji ani konfiguracji CI. Nie dodawano fikcyjnych odznak statusu CI ani deklaracji zgodności niewynikających z testów.

Przed wdrożeniem zalecany scenariusz obejmuje utworzenie dwóch sąsiadujących lokali i lokalu na innym piętrze, zakup, kontrolę dostępu, najem z odnowieniem i końcem umowy, transfer, aukcję, brak środków oraz reset z restartem w jego trakcie. Dla MySQL należy osobno sprawdzić zapis, restart, utratę połączenia i migrację. Zgłoszone wyżej ograniczenia nie zostały zamaskowane deklaracją „gotowy produkcyjnie”.
