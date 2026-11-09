# Poradnik administratora

[← Strona główna](Home.md) · [Szybki start](Szybki-start.md) · [Komendy administratora](Komendy-administratora.md)

## Zaprojektuj nieruchomości przed sprzedażą

Dobry lokal ma jednoznaczne granice, gotowy wzorzec, czytelną cenę i przewidywalne rozliczenia. Zacznij od małego fragmentu miasta: domu, sklepu i mieszkania na piętrze. Pozwoli to sprawdzić ochronę przed udostępnieniem większej dzielnicy.

| Decyzja | Co ustalić |
| :--- | :--- |
| Granice | Dokładna objętość X/Y/Z, wspólne ściany i przejścia. |
| Model korzystania | Sprzedaż, najem albo aukcja. |
| Rozliczenia | Cena, podatek, okres, karencja i procent zwrotu. |
| Dostęp | Typ nieruchomości, dzielnica i liczba współużytkowników. |
| Wzorzec | Stan budynku i zapisanych pojemników po resecie. |

## Utworzenie i kontrola

Zaznacz gotowy obszar WorldEdit, a następnie:

```text
/plots admin create sklep_1 Sklep przy fontannie
/plots admin type set sklep_1 COMMERCIAL
/plots admin district set sklep_1 handlowa
/plots admin setprice sklep_1 45000
/plots admin settax sklep_1 3000
/plots admin setperiod sklep_1 7
/plots admin info sklep_1
```

W przykładzie dzielnica `handlowa` zwiększa podatek przez mnożnik `1.30`, więc tryb `FIXED` daje 3 900 za okres. Ustaw tryb jawnie, jeśli wcześniej był inny.

`admin info` pokazuje m.in. region WorldGuard, wymiary, stawkę bazową, wynik podatku, właściciela i terminy. `admin select` ustawia zaznaczenie na granice istniejącego lokalu, ale wymaga pobytu w jego świecie.

## Wybierz sposób udostępnienia

| Sposób | Przygotowanie |
| :--- | :--- |
| Bezpośredni zakup | Pozostaw nieruchomość dostępną i dodaj `FOR_SALE`. |
| Najem | `/plots admin rent offer sklep_1 500 DAY 1000`. |
| Aukcja | `/plots admin auction start sklep_1 30000 1000 30 60000`. |

Nie prowadź równocześnie kilku modeli udostępnienia tej samej działki. Plugin blokuje zakup poza aktywną aukcją i najmem; zmiana przeznaczenia powinna obejmować zakończenie poprzedniej operacji.

## Codzienna obsługa

- Sprawdzaj `/plots admin list` i szczegóły zgłoszonego lokalu.
- Weryfikuj [budżet i transakcje](Budzet-i-audyt.md), jeśli zgłaszane jest błędne saldo.
- Zmieniaj konfigurację świadomie: `reload` odświeża ustawienia, ale nie przelicza automatycznie zapisanych cen wszystkich działek.
- Aktualizuj wzorzec przez `snapshot` dopiero po sprawdzeniu budynku.
- Wykonuj spójne [kopie danych i świata](Migracja-i-kopie-zapasowe.md).

## Działania wymagające uwagi

> **`reset` i `restore` usuwają własność.** Obie komendy uruchamiają powrót do wzorca. Nie służą do kosmetycznej naprawy domu z zachowaniem właściciela.

`setowner <id> none` usuwa własność bez odtwarzania bloków. `delete` usuwa rejestrację i region, lecz nie wyburza budynku. `snapshot` zastępuje dotychczasowy wzorzec aktualnym stanem świata.

Usunięcie, wyłączenie i zmiana właściciela są blokowane podczas kolidującego resetu, najmu lub aukcji. Najem zakończ przez `admin rent cancel`, aby rozliczyć kaucję i odebrać dostęp zgodnie z umową.

## Sprawdzenie przed wpuszczeniem graczy

1. Kup lokal zwykłym kontem i sprawdź pobraną kwotę, właściciela i termin.
2. Sprawdź obce konto bez uprawnienia `riftplots.admin.bypass`.
3. Przetestuj tabliczkę, najem i opłacenie podatku.
4. Na lokalu testowym sprawdź reset oraz zawartość wzorca.
5. Sprawdź zachowanie po restarcie, a dla MySQL także odtwarzanie połączenia w docelowym środowisku.

Listę aktualnych ograniczeń znajdziesz w [stanie funkcji](Stan-funkcji.md). Ułatwia ona odróżnienie błędnej konfiguracji od funkcji, która nie jest jeszcze zaimplementowana.
