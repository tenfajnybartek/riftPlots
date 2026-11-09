# Konfiguracja

[← Strona główna](Home.md) · [Magazyny danych](Magazyny-danych.md) · [Typy i dzielnice](Dzielnice-i-typy.md)

## Pliki

| Plik w `plugins/riftPlots/` | Zastosowanie |
| :--- | :--- |
| `config.yml` | Ustawienia główne, ekonomia, ochrona, reset i interfejs. |
| `storage.yml` | Wybór backendu, połączenie i pula. |
| `lang.yml` | Szablony wiadomości i ich placeholdery. |
| `plot-types.yml` | Definicje typów, dozwolone operacje i limity. |
| `districts.yml` | Dzielnice, mnożnik podatku i reguły nabycia. |

Pełne pliki dostarczane z pluginem znajdują się w [zasobach repozytorium](https://github.com/tenfajnybartek/riftPlots/tree/master/src/main/resources). Nie edytuj pliku wewnątrz JAR-a zamiast konfiguracji w katalogu serwera.

## Przeładowanie a zapisane dane

```text
/plots admin reload
```

Odświeża ustawienia, wiadomości, typy, dzielnice i tabliczki. Wybór backendu nadal wymaga restartu. Zapisana cena, podatek, okres i polityka resetu konkretnej działki nie są masowo zastępowane nowymi globalnymi wartościami domyślnymi.

Stosuj poprawne wcięcia YAML i nie dubluj tych samych sekcji. Fragmenty na tej stronie są przykładami wybranych ustawień; nie zawsze zastępują cały plik.

## Wygląd i waluta

| Klucz | Domyślna wartość / znaczenie |
| :--- | :--- |
| `general.prefix` | Gradientowy prefiks `riftPlots` w MiniMessage. |
| `general.currency-symbol` | `$`, symbol prezentacji kwot. |
| `general.currency-symbol-suffix` | `true`: symbol po liczbie. |

Zmiana symbolu nie zmienia providera ekonomii ani kursu waluty.

```yaml
general:
  prefix: "<gradient:#00c6ff:#0072ff><b>riftPlots</b></gradient> <gray>»</gray> "
  currency-symbol: "zł"
  currency-symbol-suffix: true
```

## Ekonomia i terminy

| Klucz pod `economy` | Domyślnie | Znaczenie |
| :--- | :--- | :--- |
| `default-price` | `25000.0` | Cena nowo tworzonej działki. |
| `default-base-tax` | `1500.0` | Podstawa podatku. |
| `default-tax-mode` | `FIXED` | Tryb kalkulacji. |
| `default-tax-multiplier` | `1.0` | Mnożnik dla modelu działki. |
| `default-billing-period-days` | `7` | Dni okresu rozliczeniowego. |
| `default-grace-period-hours` | `6` | Godziny karencji. |
| `max-advance-periods` | `4` | Dodatkowe okresy opłacane z góry. |
| `default-abandon-refund-percent` | `50.0` | Procent zapisanej ceny zwracany przy oddaniu. |

Obliczenia i progi opisuje [rozdział o podatkach](Podatki.md). Procent zwrotu dotyczy wartości działki, nie sumy materiałów gracza.

| Klucz pod `scheduler` | Domyślnie |
| :--- | :--- |
| `check-interval-seconds` | `60`; kod przyjmuje minimum 10 sekund. |
| `tax-reminder-hours` | `[48, 24, 12, 2, 1]` |

Scheduler obsługuje również wygasanie najmu i kończenie aukcji. Zwiększenie interwału wpływa więc także na te sprawdzenia; kup teraz wywołuje finalizację bez oczekiwania na cykl.

## Ochrona i reset

Wszystkie przełączniki `protection` są domyślnie włączone: `prevent-piston-border-crossing`, `prevent-fluid-border-crossing`, `prevent-explosions`, `prevent-fire-spread`, `protect-armor-stands`, `protect-item-frames`. Opis zakresu: [ochrona terenu](Ochrona-terenu.md).

| Klucz pod `reset` | Domyślnie | Znaczenie |
| :--- | ---: | :--- |
| `auto-reset-on-expire` | `true` | Polityka nadawana nowym działkom. |
| `block-batch-size` | `2500` | Maksimum bloków w porcji; kod przyjmuje minimum 100. |
| `tick-delay` | `1` | Ticks między porcjami; minimum 1. |

Porcjowanie ogranicza pracę pojedynczej porcji, ale nie jest gwarancją określonego TPS dla dowolnego rozmiaru budynku. [Schematy i reset](Schematy-i-reset.md).

## Rynek i aukcje

| Klucz | Domyślnie |
| :--- | ---: |
| `market.transfer-fee-percent` | `5.0` |
| `market.transfer-offer-duration-seconds` | `300` |
| `auction.city-fee-percent` | `5.0` |
| `auction.anti-snipe-extension-seconds` | `120` |
| `auction.default-min-bid-increment` | `100.0` |
| `auction.default-duration-minutes` | `30` |

Prowizje są ograniczone do 0–100%. Komenda rozpoczynająca aukcję nadal wymaga jawnego podania przebicia i czasu — same pola wartości domyślnych nie czynią tych argumentów opcjonalnymi.

## Teleportacja i budżet

```yaml
teleport:
  enabled: true
  cost: 50.0
  cooldown-seconds: 10

city-budget:
  initial-balance: 100000.0
```

`initial-balance` służy do inicjalizacji nowej księgi. Zapisane saldo nie jest resetowane przy każdym starcie. [Teleportacja](Teleportacja.md) i [budżet](Budzet-i-audyt.md).

## Tabliczki

`signs.enabled` oraz `signs.auto-update` mają domyślnie wartość `true`. Pod `signs.formats` znajdują się klucze `header`, `renew-header`, `for-sale-action`, `sold-action`, `renew-action`, `resetting-action`, `disabled-action`. Są to szablony MiniMessage używane przez [tabliczki](Tabliczki.md).

## Wiadomości

`lang.yml` korzysta z szablonów MiniMessage. Zachowuj placeholdery potrzebne danej wiadomości, np. `{prefix}`, `{plot_id}`, `{name}`, `{price}`, `{amount}`, `{time_left}`, `{offer_id}`. Nie każdy placeholder jest dostępny we wszystkich wiadomościach.

```yaml
general:
  no-permission: "{prefix}<red>Brak uprawnień do tej czynności.</red>"
```

Część tekstów komend i GUI jest nadal zapisana w kodzie. Edycja `lang.yml` nie przetłumaczy jeszcze całego interfejsu. Dla danej wiadomości korzystaj z placeholderów widocznych w jej dostarczonym szablonie.
