# Konfiguracja GUI

[← Strona główna](Home.md) · [Ustawienia i wiadomości](Konfiguracja.md) · [Poradnik gracza](Poradnik-gracza.md)

<p align="center"><img src="assets/riftplots-banner.png" alt="riftPlots — miejski rynek nieruchomości" width="100%"></p>

W `plugins/riftPlots/gui.yml` zmieniasz wygląd wszystkich siedmiu ekranów. Plugin tworzy plik przy pierwszym uruchomieniu. Źródłowy [gui.yml](https://github.com/tenfajnybartek/riftPlots/blob/master/src/main/resources/gui.yml) zawiera kompletny zestaw domyślny.

## Dostępne menu

| Klucz pod `menus` | Ekran | Przyciski |
| :--- | :--- | :--- |
| `market` | Rynek z filtrowaniem i stronami | `previous`, `owned`, `sale`, `rent`, `auction`, `sort`, `district`, `type`, `next` |
| `details` | Zarządzanie własną działką | `info`, `pay`, `members`, `abandon`, `back` |
| `buy` | Potwierdzenie zakupu | `confirm`, `cancel`, `border` |
| `rental` | Potwierdzenie najmu | `confirm`, `autorenew`, `cancel` |
| `auction` | Oferty, historia i kup teraz | `bid`, `history`, `buyout`, `back` |
| `members` | Lista współużytkowników | `previous`, `back`, `next` |
| `permissions` | Uprawnienia wskazanego członka | `member`, `manager`, `back` |

Identyfikatory menu i przycisków są stałymi akcjami pluginu. Edytuj ich właściwości, zachowując nazwy kluczy. Przedmiot otrzymuje znacznik akcji PDC; materiał ani nazwa nie decydują o wykonanej operacji. Własny dodatkowy klucz nie tworzy nowej komendy. Zakup, najem i aukcja ponownie sprawdzają warunki finansowe przy kliknięciu.

`menus.market.default-tab` wybiera początkową zakładkę: `OWNED`, `SALE`, `RENT` lub `AUCTION`. `menus.market.default-sort` wybiera sortowanie `ID`, `PRICE` lub `TAX`. Domyślnie są to `OWNED` i `ID`.

## Rozmiar, sloty i paginacja

`size` przyjmuje `9`, `18`, `27`, `36`, `45` lub `54`. Sloty liczymy od **0**: górny lewy to `0`, następny `1`, drugi rząd zaczyna się od `9`.

`content-slots` wskazuje kolejność pozycji na rynku i w liście członków. Liczba slotów wyznacza wielkość strony. W edytorze praw lista musi pomieścić wszystkie rodzaje uprawnień. Sloty zawartości i aktywnych przycisków nie mogą się powtarzać ani wychodzić poza rozmiar menu.

Przykład rynku na 18 ofert, z nawigacją w trzecim rzędzie:

```yaml
menus:
  market:
    size: 27
    content-slots: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17]
    buttons:
      previous: {slot: 18}
      owned: {slot: 19}
      sale: {slot: 20}
      rent: {slot: 21}
      auction: {slot: 22}
      sort: {slot: 23}
      district: {slot: 24}
      type: {slot: 25}
      next: {slot: 26}
```

Przykłady są fragmentami konfiguracji: brakujące materiały, nazwy i lore pochodzą z domyślnego pliku. Zachowaj pozostałe sekcje własnych ustawień.

## Właściwości przedmiotu

| Pole | Znaczenie |
| :--- | :--- |
| `slot` | Pozycja przycisku. Wpisy z list korzystają z `content-slots`. |
| `material` | Nazwa przedmiotu Bukkit, np. `DIAMOND`, `BOOK`, `LIME_WOOL`. |
| `name` | Wyświetlana nazwa, obsługuje MiniMessage. |
| `lore` | Lista wierszy opisu; `[]` usuwa opis, `''` dodaje pusty wiersz. |
| `enabled` | `false` wyłącza przycisk. Brak oznacza włączenie. |
| `amount` | Liczba sztuk, ograniczona do legalnego stosu materiału. Domyślnie 1. |
| `glow` | `true` lub `false` wymusza obecność/brak poświaty. |
| `custom-model-data` | Liczba dla modelu z paczki zasobów. Bez pola plugin jej nie ustawia. |

Przycisk zakupu po przeniesieniu i zmianie wyglądu:

```yaml
menus:
  buy:
    title: '<dark_blue><bold>Twoja nowa nieruchomość</bold></dark_blue>'
    buttons:
      confirm:
        slot: 5
        material: DIAMOND
        amount: 1
        glow: true
        custom-model-data: 1001
        name: '<green>Kup {plot_name}</green>'
        lore:
          - '<gray>Cena: <gold>{price}</gold></gray>'
          - '<gray>Podatek: {tax} co {period} dni</gray>'
          - ''
          - '<yellow>Kliknij, aby potwierdzić.</yellow>'
```

`entry` opisuje pojedynczą ofertę rynku, członka albo prawo. Rynek domyślnie pobiera `{type_icon}` z `plot-types.yml`; można zastąpić go stałym materiałem. Edytor praw używa `{permission_icon}`, wybieranego przez `allowed-material` i `denied-material`.

## Wypełnienie

Każde menu ma oddzielny `filler`. Jego przedmiot jest kopiowany do wszystkich slotów, po czym zastępują go oferty i przyciski. Wypełnienie nie wykonuje akcji.

```yaml
menus:
  buy:
    filler:
      enabled: true
      material: BLACK_STAINED_GLASS_PANE
      name: ' '
      lore: []
```

## Placeholdery

Placeholdery obowiązują w danym ekranie; wpisanie zmiennej z innego ekranu pozostawi jej nazwę w tekście. Nazwa działki i inne wartości użytkownika są wstawiane jako tekst, bez interpretowania ich tagów MiniMessage.

| Kontekst | Zmienne |
| :--- | :--- |
| Działka w szczegółach, zakupie, najmie, aukcji i wpisie rynku | `{plot_id}`, `{plot_name}`, `{status}`, `{owner}`, `{world}`, `{width}`, `{length}`, `{height}`, `{area}`, `{volume}`, `{price}`, `{tax}`, `{period}`, `{advance_paid}`, `{advance_max}`, `{member_count}`, `{purchase_date}`, `{next_tax}`, `{type}`, `{district}`, `{rent}`, `{deposit}`, `{rental_unit}` |
| Nagłówek i przyciski rynku | `{page}`, `{sort}`, `{district_filter}`, `{type_filter}` |
| Wpis rynku | Dodatkowo `{type_icon}` jako materiał. |
| Szczegóły działki | Dodatkowo `{pay_periods}`, `{refund}`. |
| Zakup | Dodatkowo `{buyer_fee}`. |
| Najem | Dodatkowo `{cost}`, `{autorenew}`, `{rental_periods}`. |
| Aukcja | Dodatkowo `{minimum}`, `{commission}`, `{end}`, `{charge}`, `{settlement}`, `{buyout}`, `{buyout_charge}`. |
| Wiersz historii aukcji `bid-line` | `{bidder}`, `{amount}`. |
| Lista członków | `{page}`, `{plot_id}` w tytule/przyciskach; `{member}`, `{role}` we wpisie. |
| Edytor dostępu | `{member}`, `{member_role}`, `{manager_role}` w tytule/przyciskach; `{permission}`, `{allowed}`, `{permission_icon}` we wpisie. |

`{entries}` jako osobny wiersz lore rozwija listę ostatnich ofert aukcji. Ich liczbą steruje `menus.auction.history-lines` (0–100), wyglądem `bid-line`. `settlement-reserved` opisuje rezerwację escrow, a `settlement-final` pobranie przy rozliczeniu. Przycisk kup teraz pojawia się, gdy aukcja ma taką cenę, a przyciski stron zależą od liczby wyników.

Etykiety statusów, jednostek, ról i sortowania znajdują się w `lang.yml` pod `labels`. Format pieniędzy i dat ustawia się w `config.yml` pod `formatting` i `economy`.

## Przeładowanie i starsze pliki

```text
/plots admin reload
```

Ponownie otwórz menu, aby zobaczyć nowy układ. Błędny rozmiar, kolizja slotów, niewłaściwy materiał lub typ pola YAML powodują odrzucenie nowego układu. Poprzednia poprawna konfiguracja menu pozostaje aktywna. Przy zmniejszaniu rozmiaru przenieś wszystkie aktywne przyciski lub ustaw im `enabled: false`.

Usunięcie domyślnego klucza przywraca wartość z JAR-a; aby ukryć przycisk, użyj `enabled: false`. Aby wyczyścić nazwę lub lore, ustaw odpowiednio `''` lub `[]`. Plugin nie nadpisuje istniejącego pliku administratora przy aktualizacji.

Układ GUI nie zmienia uprawnień, umowy najmu ani opłat. Domyślne okresy płatności i najmu wybierasz przez `billing.default-payment-periods` i `rental.default-periods`, a ceny — przez konfigurację działek i komendy administracyjne.
