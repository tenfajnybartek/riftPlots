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
/plots auction history willa_1 1
/plots auction buy willa_1 confirm
```

Pierwsza oferta może być równa cenie wywoławczej. Kolejna osiąga aktualną kwotę powiększoną o przebicie; jeśli przekroczyłoby to kup teraz, wystarczy próg kup teraz. Sprzedający nie może licytować własnej nieruchomości. Sprawdzane są też reguły nabycia i limit własności w dzielnicy.

## Ochrona przed ofertą w ostatniej chwili

Domyślne okno wynosi 120 sekund. Gdy przyjęta oferta pada bliżej końca, termin jest ustawiany tak, aby pozostało około 120 sekund na odpowiedź.

Przykład: aukcja ma skończyć się o 18:30:00, a oferta wpada o 18:29:30. Nowy termin to około 18:31:30. Nie oznacza to bezwarunkowego dodawania dwóch minut do starego terminu po każdej ofercie.

## Kup teraz

Oferta równa lub wyższa od ustawionego progu kończy oczekiwanie i wywołuje finalizację od razu. Komenda nadal jest ofertą konkretnej kwoty — sprawdź wpisaną wartość przed wysłaniem.

`/plots auction buy <id> confirm` składa ofertę dokładnie w cenie kup teraz. Panel ma osobny przycisk z tą ceną i kwotą pobrania. ID zapisane w przycisku chroni przed złożeniem oferty w nowej aukcji tego samego lokalu ze starego okna.

## Kiedy pobierane są pieniądze?

Domyślnie środki są pobierane przy rozliczeniu. Opcjonalne `auction.escrow-enabled: true` rezerwuje je w nowych aukcjach już przy licytacji, zwraca przebitą ofertę przez trwałą kolejkę i nie obciąża zwycięzcy drugi raz. [Zasady i komendy](Nowosci-P1-P2.md#aukcje-i-oferty-transferu).

| Wynik | Zachowanie |
| :--- | :--- |
| Brak ofert | Aukcja kończy się bez zmiany właściciela. |
| Udana płatność | Własność przechodzi na zwycięzcę, dawne członkostwa i dom są czyszczone. |
| Brak środków lub odrzucone pobranie | Aukcja jest anulowana; nie wybiera automatycznie drugiego licytującego. |
| Odrzucona wypłata sprzedawcy | Własność nie jest przekazywana; zwrot kupującemu otrzymuje trwałe zobowiązanie. Niepewna odpowiedź wymaga uzgodnienia dziennika. |
| Nieaktualny właściciel lub kolidujący stan | Aukcja nie jest finalizowana jako sprzedaż nowego właściciela. |

Przy aukcji nieruchomości gracza domyślna prowizja miasta wynosi 5%. Przy sprzedaży lokalu miejskiego do budżetu trafia cała cena. Jeżeli brak aktualnego przyszłego terminu podatku, wygrana tworzy nowy termin; przyszły zapisany termin jest zachowywany.

## Restart i dostępne narzędzia

Stan oraz pełna lista przebić są trwałe. `/plots auction history <id> [strona]` działa również po zakończeniu. Panel zawiera kup teraz, a `/plots admin auction cancel <id> confirm <powód>` zapisuje anulowanie i zwraca ewentualną rezerwację.

Wystawianie pozostaje komendą administratora. Do anulowania służy `/plots admin auction cancel <id> confirm <powód>`: powód trafia do audytu, a rezerwacja środków — do kolejki zwrotów. Nie wymaga to resetowania budynku ani odbierania własności.
