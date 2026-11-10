# Budżet miasta i audyt

[← Strona główna](Home.md) · [Podatki](Podatki.md) · [Transfery](Transfery.md)

## Dwie warstwy historii

Plugin zapisuje transakcje graczy związane z nieruchomościami oraz prowadzi własną księgę budżetu miasta. Osobno istnieją logi audytowe operacji usług. Te źródła mają różne zastosowania i nie są jedną listą obejmującą każdy możliwy ruch w ekonomii serwera.

## Budżet nie jest kontem Vault

Saldo miasta jest wartością wewnątrz riftPlots. Nie jest automatycznie osobnym kontem bankowym providera ekonomii. Komenda dodania środków do księgi nie pobiera pieniędzy od administratora, a wypłata z księgi nie przelewa ich na jego konto.

Domyślne saldo początkowe nowej księgi wynosi **100 000**, zgodnie z `city-budget.initial-balance`. Zmiana tego klucza nie zastępuje już zapisanego salda.

## Co wpływa do księgi?

| Operacja | Przychód miasta |
| :--- | :--- |
| Zakup dostępnej nieruchomości miejskiej | Cena zakupu. |
| Podatek | Opłacona kwota. |
| Najem lokalu bez właściciela | Czynsz; kaucja jest osobną kwotą umowy. |
| Transfer gracz–gracz | Prowizja transferu. |
| Aukcja nieruchomości gracza | Prowizja aukcyjna. |
| Aukcja nieruchomości miejskiej | Pełna zwycięska kwota. |

Zwrot przy dobrowolnym oddaniu może zostać zapisany jako wydatek miasta. Nie należy traktować salda księgi jako pełnego mechanizmu wymuszania wypłacalności wszystkich operacji Vault.

## Komendy

```text
/plots admin budget balance
/plots admin budget history
/plots admin budget add 5000 Dotacja na otwarcie dzielnicy
/plots admin budget withdraw 1000 Korekta księgowa
```

`add` i `withdraw` wymagają dodatniej kwoty. Kierunek operacji wybierany jest nazwą komendy. Opcjonalny powód ułatwia późniejsze rozpoznanie korekty. Historia prezentuje m.in. datę, typ, kwotę i saldo po operacji.

## Historia konkretnego lokalu

```text
/plots history rynek_1
/plots admin audit rynek_1
```

`history [id|all] [strona]` pokazuje dziennik operacji i zwroty. Opcja `legacy` otwiera starsze transakcje. Historia lokalu jest domyślnie prywatna. `admin audit <id> [strona]` pokazuje logi operacyjne z operatorem i zmianami stanu oraz transakcje.

Nowe rozliczenia zakupu, podatku, najmu, transferów, aukcji i zwrotów mają ID we wspólnym dzienniku. Starsze rekordy pozostają dostępne jako historia `legacy`.

## Zapis i restart

Przy YAML budżet znajduje się w `data/city_budget.yml`, a audyt w `data/audit_logs.yml`. SQL używa odpowiednich tabel. Historia księgi YAML i zapisany audyt są dostępne po restarcie.

Wiadomości mają trwałe ID, datę i potwierdzenie wysłania przez serwer. `/plots notifications [strona]` pokazuje ich historię. Retencja dotyczy wysłanych wpisów; przerwanie między wysłaniem a potwierdzeniem może spowodować ponowne wysłanie.

## Wyjaśnianie rozbieżności

1. Ustal ID działki, konto, kwotę i przybliżony czas.
2. Sprawdź saldo providera ekonomii oraz zapisane transakcje lokalu.
3. Porównaj prowizję, zwrot lub czynsz z odpowiednią historią budżetu.
4. Przeczytaj błędy Vault i magazynu z tego samego czasu.
5. Korektę wykonuj dopiero po ustaleniu, które etapy zostały wykonane.

Vault i backend nie tworzą jednej transakcji atomowej. Przy awarii procesu lub bazy pomiędzy etapami może być potrzebne ręczne uzgodnienie zapisów. [Zakres gwarancji](Stan-funkcji.md) i [pomoc techniczna](Rozwiazywanie-problemow.md) opisują to szerzej.
