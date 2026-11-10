# Stan funkcji i ograniczenia

[← Strona główna](Home.md) · [FAQ](FAQ.md) · [Rozwój i testy](Rozwoj-i-testy.md)

Dokumentacja opisuje lokalne wdrożenie P1–P2 dla riftPlots 1.0.0 z 10 listopada 2026 r. [Przewodnik zmian](Nowosci-P1-P2.md) zawiera komendy, ustawienia i zasady aktualizacji.

## Zaimplementowane ścieżki

| Obszar | Zakres |
| :--- | :--- |
| Rejestracja | Zaznaczenie WorldEdit, granice 3D, schemat i region WorldGuard. |
| Zakup | Komenda, GUI i tabliczka; kontrola dostępności i reguł nabycia. |
| Podatki | Sześć modeli, terminy, karencja i przedpłaty. |
| Najem | Czynsz, trwały zwrot kaucji, wybór odnowienia, zakończenie przez najemcę i opcjonalne oferty właściciela. |
| Rynek wtórny | Oferta do wskazanego gracza i akceptacja lub odrzucenie. |
| Aukcje | Trwałe przebicia, panel, kup teraz, anulowanie i opcjonalny escrow. |
| Reset | Wzorzec bloków, porcjowanie, kontrola wymiarów i ponowienie po restarcie. |
| Obsługa | Wewnętrzny budżet, transakcje, audyt i trzy backendy. |
| Migracja | Kolejka I/O, blokada zmian do restartu, schemat v2 i historia przebić. |
| Interfejs | Strony, sortowanie i filtry rynku, edytor roli i praw, obrys 3D. |
| Rozliczenia | Trwały dziennik z ID, blokada niepewnych operacji, kolejka zobowiązań i narzędzia uzgodnienia. |

## Dziedziczenie i aktualizacja

Nowe działki dziedziczą parametry typu; starsze rekordy i ręczne nadpisania zachowują wartości. Cena dziedziczona uwzględnia mnożnik dzielnicy. Aktywne umowy zachowują warunki. Ikona działa w rynku, a flagi stanu typu przy rejestracji i `apply-defaults`.

Działające reguły opisuje [rozdział o dzielnicach](Dzielnice-i-typy.md). `apply-defaults` pokazuje różnice i wymaga potwierdzenia.

## Finanse nie są jedną transakcją atomową

Dziennik utrwala zamiar i odpowiedź Vault oraz zachowuje operację po restarcie. Niepewnego wyniku nie powtarza; administrator sprawdza portfele i stan, a następnie uzgadnia wynik. Provider ekonomii i baza pozostają osobnymi systemami.

## Trwałość danych

Oferty transferu, wiadomości, operacje i zwroty mają lokalne pliki. Historia aukcji jest zapisywana w backendzie. Potwierdzenie wiadomości oznacza wysłanie przez serwer, nie przeczytanie; przerwanie przed zapisem potwierdzenia może spowodować ponowne wysłanie.

## MySQL nie oznacza synchronizacji sieci

Każdy proces utrzymuje własny cache. Brakuje rozproszonych blokad i uzgadniania zmian. Wspólna baza sama w sobie nie jest obsługą kilku serwerów zarządzających tymi samymi nieruchomościami.

## Interfejs i operacje

- Teksty pluginu pochodzą z `lang.yml`, a wygląd siedmiu ekranów z [gui.yml](Konfiguracja-GUI.md). Identyfikatory komend, akcji, uprawnień i danych pozostają stałe.
- Wystawianie aukcji pozostaje komendą administracyjną.
- Zdarzenia API nie pokrywają każdej zmiany statusu i własności.
- Zwykłe płatności i ich trwałe zapisy pozostają synchroniczne. Migracja działa w kolejce; większe miasta wymagają pomiaru obciążenia.

## Zakres sprawdzenia

Build i **119 testów Java** przeszły, w tym rzeczywisty MySQL. Dodatkowo przeszło 6 testów backupu i 54 sprawdzenia Paper z WorldEdit, WorldGuard, Vault i testową ekonomią: zakup, restart, reset, odtworzona kopia oraz zmiana GUI i języka. Zastępcze obiekty graczy nie zastępują próby z klientem i testu wszystkich mechanik fizyki.

[Przewodnik testowania](Rozwoj-i-testy.md) wskazuje, jak uzupełnić sprawdzenie przed uruchomieniem miasta.
