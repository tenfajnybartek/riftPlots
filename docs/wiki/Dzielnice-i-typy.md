# Dzielnice i typy nieruchomości

[← Strona główna](Home.md) · [Konfiguracja](Konfiguracja.md) · [Podatki](Podatki.md)

## Dwa różne poziomy organizacji

**Typ** opisuje przeznaczenie pojedynczej nieruchomości, np. mieszkanie lub obiekt przemysłowy. **Dzielnica** grupuje nieruchomości logicznie i określa zasady strefy. Dzielnica nie tworzy dodatkowego regionu ani automatycznych granic w świecie.

<p align="center"><img src="assets/city-guide.png" alt="Różne rodzaje zabudowy na wspólnym planie miasta" width="100%"></p>

## Typy dostarczane w konfiguracji

Poniższe wartości pochodzą z `plot-types.yml`.

| Typ | Domyślna cena w definicji | Podstawa podatku | Tryb / dni | Członkowie | Najem / sprzedaż |
| :--- | ---: | ---: | :--- | ---: | :--- |
| `RESIDENTIAL` | 25 000 | 1 500 | `FIXED` / 7 | 5 | Tak / tak |
| `COMMERCIAL` | 45 000 | 3 000 | `FIXED` / 7 | 10 | Tak / tak |
| `INDUSTRIAL` | 60 000 | 4 500 | `PER_CHUNK` / 14 | 15 | Nie / tak |
| `PREMIUM` | 100 000 | 2 000 | `FIXED` / 30 | 20 | Tak / tak |
| `RENTAL` | 30 000 | 1 000 | `FIXED` / 7 | 3 | Tak / nie |
| `CUSTOM` | 50 000 | 2 500 | `CUSTOM` / 7 | 25 | Tak / tak |

> To wartości definicji typu, a nie automatyczny cennik wszystkich działek. `type set` zapisuje typ, ale nie przepisuje na działkę ceny, podatku, trybu, okresu ani procentu zwrotu.

## Jak przypisać typ?

```text
/plots admin type list
/plots admin type info COMMERCIAL
/plots admin type set rynek_1 COMMERCIAL
```

Po przypisaniu ustal parametry konkretnej działki:

```text
/plots admin setprice rynek_1 45000
/plots admin settax rynek_1 3000
/plots admin settaxmode rynek_1 FIXED
/plots admin setperiod rynek_1 7
```

Pola `rental-allowed`, `sales-allowed` i limit dodawania współużytkowników mają zastosowanie w odpowiednich ścieżkach usług. Samo pole `flags` nie nadaje automatycznie flag regionowi WorldGuard, a `icon-material` nie zmienia automatycznie ikony GUI.

## Dzielnice domyślne

| Identyfikator | Podatek | Limit własności na gracza | Dozwolone typy |
| :--- | ---: | ---: | :--- |
| `centrum` | ×1.25 | 2 | `COMMERCIAL`, `PREMIUM`, `RESIDENTIAL` |
| `stare_miasto` | ×1.10 | 2 | `RESIDENTIAL`, `COMMERCIAL` |
| `przedmiescia` | ×0.75 | 4 | `RESIDENTIAL` |
| `handlowa` | ×1.30 | 3 | `COMMERCIAL`, `RENTAL` |
| `przemyslowa` | ×0.90 | 5 | `INDUSTRIAL`; najem wyłączony |

```text
/plots admin district list
/plots admin district info centrum
/plots admin district set rynek_1 centrum
/plots admin district set rynek_1 none
```

Limit jest sprawdzany przy zakupie, transferze i rozliczeniu aukcji. `max-plots-per-player: 0` oznacza brak limitu własności. Pusta lista `allowed-types` oznacza brak ograniczenia typów w tej definicji.

## Co dokładnie jest przeliczane?

| Ustawienie | Aktualne zachowanie |
| :--- | :--- |
| `tax-multiplier` dzielnicy | Mnoży wynik podatku, również dla `CUSTOM`. |
| `price-multiplier` dzielnicy | Jest w definicji, ale nie zmienia bezpośredniej ceny zakupu `Plot.price`. |
| `default-*` typu | Nie są automatycznie kopiowane przez komendę przypisania typu. |
| `max-members` typu | Kontroluje dodawanie przez `trust`; automatyczny najem jest osobną operacją. |
| `sales-enabled` i `rental-enabled` | Są sprawdzane przy odpowiednich operacjach nabycia lub najmu. |

## Własny typ lub dzielnica

Skopiuj istniejącą sekcję odpowiednio pod `types:` lub `districts:` i użyj nowego identyfikatora. Zachowaj składnię YAML oraz poprawne nazwy materiałów i trybów. Następnie wykonaj `/plots admin reload`, sprawdź `type info` lub `district info` i przetestuj lokal.

Zmiana definicji nie przebudowuje wcześniejszych umów ani całej historii własności. Zasady nowego nabycia mogą zacząć obowiązywać po przeładowaniu, a zapisane ceny i terminy pozostają danymi konkretnej działki.
