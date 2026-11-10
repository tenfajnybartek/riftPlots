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

Domyślnie nie. Nadaje interakcje, drzwi, pojemniki i teleportację. Właściciel może zmienić rolę oraz prawa w panelu członków z `riftplots.members.edit`. `TENANT` ma prawa wynikające z umowy.

## Czy wynajmujący zostaje właścicielem?

Najemca nie zostaje właścicielem. Jego prawa wynikają z umowy. Czynsz, kaucja i podatek właściciela to różne elementy. [Wynajem](Wynajem.md).

## Czy najem odnawia się automatycznie?

Domyślnie odnowienie jest wyłączone. Najemca wybiera je w GUI lub przez `rent autorenew <id> on|off`. Może też zakończyć umowę przez `rent cancel <id> confirm`.

## Czy `MONTH` oznacza miesiąc kalendarzowy?

Nie. Oznacza 30 dni.

## Czy cena typu jest od razu ceną działki?

Nowe działki dziedziczą cenę typu i mnożnik cen dzielnicy. `setprice` tworzy końcową cenę ręczną; starsze rekordy zachowują parametry. [Typy i dzielnice](Dzielnice-i-typy.md).

## Czy `CUSTOM` pomija mnożnik podatku dzielnicy?

Nie. W obecnej implementacji jest liczony tak samo jak `FIXED`, z mnożnikiem dzielnicy.

## Czy restart daje dodatkową karencję?

Nie. Karencja wynika z zapisanego terminu. Zaległość może zostać wykryta od razu po uruchomieniu schedulera.

## Czy `restore` zachowuje właściciela?

Nie. Korzysta z mechanizmu resetu i usuwa własność. [Schematy i reset](Schematy-i-reset.md).

## Czy koniec najmu resetuje budynek?

Nie sam w sobie. Kończy umowę i odbiera dostęp niezależnie od wypłaty kaucji; przywracanie schematu jest osobną operacją.

## Czy aukcja rezerwuje pieniądze?

Domyślnie nie. `auction.escrow-enabled: true` włącza rezerwację dla nowych aukcji. Bez niej pobranie następuje przy finalizacji; odmowa nie wybiera automatycznie drugiej osoby.

## Czy budżet miasta to konto bankowe Vault?

Nie. To wewnętrzna księga pluginu. `budget add` i `withdraw` nie są przelewami na konto wykonującego komendę.

## Czy mogę po prostu zmienić SQLite na MySQL?

Zmiana `storage.type` wybiera magazyn po restarcie, ale nie kopiuje danych. Wykonaj [procedurę migracji](Migracja-i-kopie-zapasowe.md).

## Czy MySQL zapewnia wspólne działki na kilku serwerach?

Nie jako gotowy mechanizm. Brakuje synchronizacji cache'ów i rozproszonych blokad. [Stan funkcji](Stan-funkcji.md).

## Czy są screenshoty interfejsu?

Grafiki tej wiki są ilustracjami projektu i schematami działania. Nie udają zrzutów ekranu z gry. Rzeczywisty wygląd GUI zależy m.in. od klienta i konfiguracji serwera.
