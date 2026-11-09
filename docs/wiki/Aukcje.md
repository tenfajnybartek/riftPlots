# Aukcje nieruchomości

[← Strona główna](Home.md) · [Transfery](Transfery.md) · [Komendy administracji](Komendy-administratora.md)

## Model licytacji

Aukcję rozpoczyna administrator. Gracze widzą aktualną kwotę i mogą ją przebijać. Jeżeli lokal ma właściciela, to on jest sprzedawcą; dla lokalu bez właściciela odbiorcą ceny jest miasto.

## Uruchomienie

```text
/plots admin auction start willa_1 50000 2500 30 150000
```

| Argument | Wartość przykładu | Znaczenie |
| :--- | ---: | :--- |
| Cena wywoławcza | 50 000 | Minimalna pierwsza oferta. |
| Przebicie | 2 500 | Minimalny wzrost po pierwszej ofercie. |
| Czas | 30 | Minuty trwania aukcji. |
| Kup teraz | 150 000 | Opcjonalny próg natychmiastowego rozliczenia. |

Cena wywoławcza musi być nieujemna, przebicie i czas dodatnie, a kup teraz nie niższe od ceny wywoławczej. Lokal nie może być równocześnie oferowany na wynajem ani objęty aktywnym najmem czy inną aukcją. Stany resetu, wyłączenia i zaległości nie są właściwym stanem do rozpoczęcia.

## Przeglądanie i składanie ofert

```text
/plots auction list
/plots auction bid willa_1 50000
/plots auction bid willa_1 52500
```

Pierwsza oferta może być równa cenie wywoławczej. Każda następna musi osiągnąć aktualną ofertę powiększoną o minimalne przebicie. Sprzedający nie może licytować własnej nieruchomości. Sprawdzane są również reguły nabycia i limit własności w dzielnicy.

## Ochrona przed ofertą w ostatniej chwili

Domyślne okno wynosi 120 sekund. Gdy przyjęta oferta pada bliżej końca, termin jest ustawiany tak, aby pozostało około 120 sekund na odpowiedź.

Przykład: aukcja ma skończyć się o 18:30:00, a oferta wpada o 18:29:30. Nowy termin to około 18:31:30. Nie oznacza to bezwarunkowego dodawania dwóch minut do starego terminu po każdej ofercie.

## Kup teraz

Oferta równa lub wyższa od ustawionego progu kończy oczekiwanie i wywołuje finalizację od razu. Komenda nadal jest ofertą konkretnej kwoty — sprawdź wpisaną wartość przed wysłaniem.

## Kiedy pobierane są pieniądze?

**Środki nie są rezerwowane podczas licytacji.** Dostępne saldo sprawdzane jest przy składaniu oferty, a pobranie następuje przy rozliczeniu. Wydanie pieniędzy przed końcem może więc spowodować brak środków u zwycięzcy.

| Wynik | Zachowanie |
| :--- | :--- |
| Brak ofert | Aukcja kończy się bez zmiany właściciela. |
| Udana płatność | Własność przechodzi na zwycięzcę, dawne członkostwa i dom są czyszczone. |
| Brak środków lub odrzucone pobranie | Aukcja jest anulowana; nie wybiera automatycznie drugiego licytującego. |
| Nieudana wypłata sprzedawcy | Następuje próba zwrotu kupującemu, własność nie jest przekazywana. |
| Nieaktualny właściciel lub kolidujący stan | Aukcja nie jest finalizowana jako sprzedaż nowego właściciela. |

Przy aukcji nieruchomości gracza domyślna prowizja miasta wynosi 5%. Przy sprzedaży lokalu miejskiego do budżetu trafia cała cena. Jeżeli brak aktualnego przyszłego terminu podatku, wygrana tworzy nowy termin; przyszły zapisany termin jest zachowywany.

## Restart i dostępne narzędzia

Aktualny stan aukcji, kwota i licytujący są zapisywane w backendzie i odczytywane po restarcie. Pełna lista wszystkich wcześniejszych przebić z modelu nie jest utrwalana.

Komendy nie udostępniają jeszcze osobnego polecenia anulowania aukcji ani wystawiania aukcji przez gracza. Reset nieruchomości może anulować aukcję jako część porządkowania stanu. Nie używaj resetu jako zamiennika neutralnego anulowania: reset odbiera własność i przywraca budynek.
