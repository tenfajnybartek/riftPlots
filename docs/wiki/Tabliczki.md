# Interaktywne tabliczki

Układ każdego stanu ma cztery edytowalne wiersze w `config.yml` pod `signs.layouts`. Dostępne są `{plot_id}`, `{plot_name}`, `{owner}`, `{status}`, `{price}`, `{tax}`, `{market}` oraz komponenty `{header}`, `{renew_header}`, `{for_sale_action}`, `{sold_action}`, `{renew_action}`, `{resetting_action}`, `{disabled_action}` z `signs.formats`. Domyślną liczbę opłacanych okresów ustawia `billing.default-payment-periods`.

[← Strona główna](Home.md) · [Komendy administratora](Komendy-administratora.md) · [Podatki](Podatki.md)

## Trzy zastosowania

| Typ | Zastosowanie |
| :--- | :--- |
| `FOR_SALE` | Prezentacja ceny i bezpośredni zakup dostępnej nieruchomości. |
| `RENEW` | Opłacenie jednego okresu podatkowego. |
| `STATUS` | Informacje o nieruchomości i jej stanie. |

Tabliczka jest powiązana z identyfikatorem w bazie. Samodzielne wpisanie podobnego napisu na zwykłej tabliczce nie tworzy oferty riftPlots.

## Powiązanie

Postaw tabliczkę, spójrz na nią z odległości do pięciu bloków i wykonaj:

```text
/plots admin sign rynek_1 FOR_SALE
/plots admin sign rynek_1 RENEW
/plots admin sign rynek_1 STATUS
```

Wykonuj wybraną komendę dla konkretnej wskazanej tabliczki. Ponowne powiązanie tej samej pozycji zastępuje poprzedni wpis. Pominięcie typu wybiera `FOR_SALE`.

```text
/plots admin unsign
```

`unsign` usuwa powiązanie wskazywanej tabliczki. Nie usuwa nieruchomości ani jej regionu.

## Używanie przez gracza

LPM pokazuje informacje. PPM na `FOR_SALE` może kupić dostępny lokal, a na `RENEW` opłacić podatek. Tabliczka `STATUS` pozostaje informacyjna. Właściciel klikający tabliczkę sprzedażową własnego lokalu może również trafić do opłacenia podatku.

Weryfikowane są podstawowe uprawnienia i uprawnienie akcji, np. zakupu lub zapłaty. Nie można użyć tabliczki do obejścia aktywnej aukcji, najmu, blokady działki ani kontroli własności płatnika.

## Przykładowy układ

```text
[DZIAŁKA]
rynek_1
45 000.00 $
KUP TERAZ
```

To poglądowy zapis tekstu, nie zrzut ekranu. Dokładne separatory kwot zależą od formatowania środowiska, symbolu waluty i ustawień pluginu.

## Format i odświeżanie

```yaml
signs:
  enabled: true
  auto-update: true
  formats:
    header: "<gradient:#00c6ff:#0072ff><b>[DZIAŁKA]</b></gradient>"
    renew-header: "<gold><b>[OPŁAĆ PODATEK]</b></gold>"
    for-sale-action: "<green><b>KUP TERAZ</b></green>"
```

Fragment pokazuje wybrane klucze. W pliku zachowaj również pozostałe formaty akcji. Zmiana stanu, właściciela lub parametrów przez ścieżkę zapisu działki odświeża tabliczki, gdy włączone jest automatyczne odświeżanie. Przeładowanie konfiguracji aktualizuje wszystkie obsługiwane tabliczki.

Tabliczki są chronione przed zwykłym zniszczeniem bez uprawnienia administracyjnego. Przy problemie sprawdź fizyczną pozycję tabliczki, jej typ, powiązanie, stan działki i `signs.enabled`.
