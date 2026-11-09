<p align="center">
  <img src="assets/riftplots-banner.png" alt="riftPlots — miasto z podświetlonymi granicami nieruchomości" width="100%">
</p>

<h1 align="center">Witaj w mieście riftPlots</h1>

<p align="center">
  <strong>Dokumentacja systemu miejskich nieruchomości</strong><br>
  Od pierwszej kamienicy po podatki, najem i rynek gracz–gracz.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/dokumentacja-PL-00c6ff?style=flat-square" alt="Dokumentacja po polsku">
  <img src="https://img.shields.io/badge/plugin-1.0.0-0072ff?style=flat-square" alt="Wersja pluginu 1.0.0">
  <img src="https://img.shields.io/badge/docelowe_API-Paper_26.3-334155?style=flat-square" alt="Docelowe API Paper 26.3">
</p>

---

## Wybierz swoją ścieżkę

| 🏡 Gram na serwerze | 🏙️ Zarządzam miastem | 🛠️ Rozwijam plugin |
| :--- | :--- | :--- |
| [Pierwsze kroki gracza](Poradnik-gracza.md) | [Instalacja](Instalacja.md) | [API i integracje](API-i-integracje.md) |
| [Kupowanie i komendy](Komendy-gracza.md) | [Pierwsza działka w kilka minut](Szybki-start.md) | [Budowanie i testy](Rozwoj-i-testy.md) |
| [Podatki](Podatki.md) i [wynajem](Wynajem.md) | [Poradnik administratora](Poradnik-administratora.md) | [Stan funkcji i ograniczenia](Stan-funkcji.md) |

## Czym jest riftPlots?

Administrator buduje gotowe lokale i wydziela je zaznaczeniem WorldEdit. Każdy lokal otrzymuje granice, cenę, zasady dostępu i zapisany schemat pierwotnego stanu. Gracze mogą go kupić, wynająć albo nabyć od innego gracza. Miasto rejestruje podatki, czynsze i prowizje w swojej wewnętrznej księdze.

Działki są **przestrzenią 3D**. Dom, sklep na parterze i mieszkanie na piętrze mogą być odrębnymi nieruchomościami, jeśli ich granice oraz regiony WorldGuard nie kolidują. Plugin nie generuje świata ani automatycznej siatki parcel.

<p align="center">
  <img src="assets/city-guide.png" alt="Ilustracja miasta z domami, mieszkaniami, sklepem i warsztatem oraz granicami lokali" width="100%">
  <sub>Ilustracja koncepcyjna. Nie jest zrzutem ekranu z serwera ani wizualizacją granic w grze.</sub>
</p>

## Co znajdziesz w wiki?

| Temat | Rozdziały |
| :--- | :--- |
| Rozpoczęcie pracy | [Instalacja](Instalacja.md), [szybki start](Szybki-start.md), [poradnik gracza](Poradnik-gracza.md), [poradnik administratora](Poradnik-administratora.md) |
| Nieruchomości i gospodarka | [Podatki](Podatki.md), [wynajem](Wynajem.md), [transfery](Transfery.md), [aukcje](Aukcje.md), [budżet i audyt](Budzet-i-audyt.md) |
| Organizacja miasta | [Typy i dzielnice](Dzielnice-i-typy.md), [ochrona](Ochrona-terenu.md), [schematy i reset](Schematy-i-reset.md), [tabliczki](Tabliczki.md), [teleportacja](Teleportacja.md) |
| Obsługa serwera | [Konfiguracja](Konfiguracja.md), [magazyny danych](Magazyny-danych.md), [migracja i kopie](Migracja-i-kopie-zapasowe.md) |
| Pomoc i rozwój | [Rozwiązywanie problemów](Rozwiazywanie-problemow.md), [FAQ](FAQ.md), [API](API-i-integracje.md), [testy](Rozwoj-i-testy.md) |

## Najważniejsze zasady

> **Własność i najem to różne rzeczy.** Najemca może korzystać z lokalu zgodnie z umową, ale nie staje się jego właścicielem.

> **`trust` nie nadaje domyślnie budowania.** Zwykły współużytkownik otrzymuje interakcje, drzwi i pojemniki. Więcej w [poradniku gracza](Poradnik-gracza.md).

> **Typ nie ustawia automatycznie ceny.** Po przypisaniu typu ustal cenę, podatek i okres osobnymi komendami. Aktualne zachowanie opisuje [rozdział o typach](Dzielnice-i-typy.md).

## Wersja dokumentacji

Wiki opisuje implementację **riftPlots 1.0.0** po przeglądzie kodu z 9–10 listopada 2026 r. Docelowe API projektu to Paper 26.3 i Java 25. Wersje zależności oraz zakres testów znajdują się w [instalacji](Instalacja.md) i [rozdziale dla programistów](Rozwoj-i-testy.md).

Zacznij od [szybkiego startu](Szybki-start.md), a jeśli coś nie działa — od [diagnozy problemów](Rozwiazywanie-problemow.md).
