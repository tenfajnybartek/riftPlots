# Najczęstsze pytania

[← Strona główna](Home.md) · [Rozwiązywanie problemów](Rozwiazywanie-problemow.md)

## Czy riftPlots generuje świat działek?

Nie. Administrator przygotowuje budynki i wydziela lokale WorldEdit. Plugin rejestruje nieruchomości i zarządza ich używaniem.

## Czy mogę tworzyć mieszkania na różnych piętrach?

Tak, granice są 3D. Zaznaczenia nie mogą zawierać tych samych bloków i nie mogą kolidować z istniejącymi regionami. Zwróć uwagę na wspólne podłogi, sufity i ściany.

## Czy potrzebny jest zewnętrzny serwer bazy?

Nie dla domyślnego SQLite ani YAML. MySQL jest opcją zewnętrzną. [Magazyny danych](Magazyny-danych.md).

## Czy sam Vault wystarczy do ekonomii?

Nie. Vault musi mieć zarejestrowanego providera ekonomii. Bez niego riftPlots przerywa uruchamianie.

## Czy `trust` pozwala budować znajomym?

Domyślnie nie. Nadaje interakcje, drzwi i pojemniki. `TENANT` ma dodatkowe prawa budowania i niszczenia, a model/API pozwala zarządzać pojedynczymi uprawnieniami. Nie ma jeszcze komendowego edytora wszystkich tych praw.

## Czy wynajmujący zostaje właścicielem?

Najemca nie zostaje właścicielem. Jego prawa wynikają z umowy. Czynsz, kaucja i podatek właściciela to różne elementy. [Wynajem](Wynajem.md).

## Czy najem odnawia się automatycznie?

Komenda `/plots rent` włącza odnowienie o jeden okres. Nie ma obecnie komendy gracza wyłączającej tę opcję; administracja może zakończyć najem przez `admin rent cancel`.

## Czy `MONTH` oznacza miesiąc kalendarzowy?

Nie. Oznacza 30 dni.

## Czy cena typu jest od razu ceną działki?

Nie. Przypisanie typu nie kopiuje ceny ani pozostałych domyślnych stawek. Cenę ustalaj `setprice`, a rozliczenia odpowiednimi komendami. [Typy i dzielnice](Dzielnice-i-typy.md).

## Czy `CUSTOM` pomija mnożnik podatku dzielnicy?

Nie. W obecnej implementacji jest liczony tak samo jak `FIXED`, z mnożnikiem dzielnicy.

## Czy restart daje dodatkową karencję?

Nie. Karencja wynika z zapisanego terminu. Zaległość może zostać wykryta od razu po uruchomieniu schedulera.

## Czy `restore` zachowuje właściciela?

Nie. Korzysta z mechanizmu resetu i usuwa własność. [Schematy i reset](Schematy-i-reset.md).

## Czy koniec najmu resetuje budynek?

Nie sam w sobie. Kończy umowę i odbiera dostęp po rozliczeniu; przywracanie schematu jest osobną operacją.

## Czy aukcja rezerwuje pieniądze?

Nie. Saldo jest sprawdzane przy ofercie, ale pobranie następuje przy rozliczeniu. Nieudana płatność anuluje aukcję bez automatycznego wyboru drugiego licytującego.

## Czy budżet miasta to konto bankowe Vault?

Nie. To wewnętrzna księga pluginu. `budget add` i `withdraw` nie są przelewami na konto wykonującego komendę.

## Czy mogę po prostu zmienić SQLite na MySQL?

Zmiana `storage.type` wybiera magazyn po restarcie, ale nie kopiuje danych. Wykonaj [procedurę migracji](Migracja-i-kopie-zapasowe.md).

## Czy MySQL zapewnia wspólne działki na kilku serwerach?

Nie jako gotowy mechanizm. Brakuje synchronizacji cache'ów i rozproszonych blokad. [Stan funkcji](Stan-funkcji.md).

## Czy są screenshoty interfejsu?

Grafiki tej wiki są ilustracjami projektu i schematami działania. Nie udają zrzutów ekranu z gry. Rzeczywisty wygląd GUI zależy m.in. od klienta i konfiguracji serwera.
