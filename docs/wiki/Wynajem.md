# Wynajem lokali

[← Strona główna](Home.md) · [Podatki](Podatki.md) · [Komendy administracji](Komendy-administratora.md)

## Własność pozostaje oddzielna

Umowa najmu pozwala korzystać z przygotowanej nieruchomości bez zakupu jej własności. Właścicielem może być gracz; lokal bez właściciela traktowany jest jako miejski. Czynsz trafia odpowiednio do właściciela albo do budżetu miasta.

Najemca otrzymuje rolę `TENANT` i prawa `BUILD`, `DESTROY`, `CONTAINERS`, `DOORS`, `INTERACT`. Nie uzyskuje przez to możliwości sprzedaży, porzucenia ani zarządzania własnością.

## Wystawienie lokalu

Administrator wpisuje:

```text
/plots admin rent offer apartament_4 250 DAY 500
```

| Argument | Znaczenie |
| :--- | :--- |
| `apartament_4` | Identyfikator nieruchomości. |
| `250` | Czynsz za jeden okres najmu. |
| `DAY` | Jednostka okresu. |
| `500` | Kaucja; można pominąć, wtedy wynosi zero. |

Kwoty muszą być nieujemne. Lokal musi być w odpowiednim stanie, bez aktywnego najmu i aukcji. Typ i dzielnica muszą dopuszczać wynajem. Zwykła sprzedaż działki z ofertą najmu jest blokowana.

| Jednostka | Długość |
| :--- | :--- |
| `HOUR` | 1 godzina |
| `DAY` | 24 godziny |
| `WEEK` | 7 dni |
| `MONTH` | 30 dni, nie miesiąc kalendarzowy |

## Podpisanie umowy przez gracza

```text
/plots rent apartament_4 7
```

Liczba `7` oznacza siedem okresów jednostki ustawionej w ofercie. Jeśli jej nie podasz, wynajmujesz na jeden okres.

Pierwsza należność:

```text
czynsz × liczba okresów + kaucja
250 × 7 + 500 = 2 250
```

Kaucja nie jest czynszem właściciela. Jest przechowywana jako zobowiązanie umowy i zwracana przy jej zakończeniu, gdy operacja ekonomii zakończy się powodzeniem.

## Automatyczne odnowienie

Komenda gracza włącza automatyczne odnowienie. Po końcu opłaconego czasu scheduler próbuje pobrać czynsz za **jeden kolejny okres**. Nie pobiera ponownie kaucji i nie odnawia automatycznie na pierwotne siedem okresów.

```mermaid
flowchart TD
    A["Koniec opłaconego czasu"] --> B{"Auto-odnowienie i aktualne warunki?"}
    B -->|Tak| C{"Pobranie i rozliczenie czynszu"}
    C -->|Sukces| D["Kolejny okres najmu"]
    C -->|Niepowodzenie| E["Zakończenie umowy"]
    B -->|Nie| E
    E --> F["Próba zwrotu kaucji i odebranie dostępu"]
```

Odnowienie wymaga zgodności bieżącego właściciela i stanu nieruchomości z umową. Nieudana płatność nie powinna dopisywać bezpłatnego okresu.

## Zakończenie

Administrator może zakończyć najem i wyłączyć ofertę:

```text
/plots admin rent cancel apartament_4
```

Po skutecznym rozliczeniu umowa jest zamykana, odwołanie do najmu czyszczone, a najemca usuwany z członków. Sam koniec najmu nie wkleja automatycznie schematu lokalu. Reset nieruchomości jest osobną operacją.

Jeżeli provider odrzuci zwrot kaucji, mechanizm zachowuje umowę do ponowienia zwrotu. Sprawdź ekonomię i zgłoszenie zamiast usuwać rekord umowy ręcznie. Reset nieruchomości również próbuje rozliczyć aktywny najem.

## Aktualne granice funkcji

- Ofertę najmu wystawia administrator, nie właściciel osobną komendą gracza.
- Nie ma jeszcze komendy gracza wyłączającej auto-odnowienie ani samodzielnego rozwiązania umowy.
- `untrust` i usunięcie członka w GUI nie służą do odebrania dostępu aktywnemu najemcy.
- Umowa najmu i podatek właściciela to oddzielne mechanizmy. Najem nie zamienia czynszu w podatek i nie przejmuje zobowiązań właściciela.
