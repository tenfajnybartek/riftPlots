# Punkt domowy i teleportacja

[← Strona główna](Home.md) · [Poradnik gracza](Poradnik-gracza.md) · [Ochrona](Ochrona-terenu.md)

## Zapisz miejsce powrotu

Stań w bezpiecznym miejscu na własnej nieruchomości:

```text
/plots sethome rynek_1
```

Pozycja musi znajdować się wewnątrz granic działki. Zapisywane są współrzędne i kierunek patrzenia. Ustawienie punktu jest dostępne właścicielowi lub administratorowi z odpowiednim bypassem; samo członkostwo nie pozwala nadpisywać domu właściciela.

## Wróć na działkę

```text
/plots home rynek_1
```

Wrócić może właściciel, członek z `TELEPORT` lub uprawniony administrator. Dostęp najemcy wymaga aktualnej umowy. Nowi członkowie mają `TELEPORT`, ale dawne zapisane listy praw pozostają bez zmian; właściciel może dodać je w edytorze. Bez ID komenda wybiera jedną z posiadanych działek; przy wielu nieruchomościach podawaj ID.

| Ustawienie domyślne | Wartość |
| :--- | ---: |
| Koszt udanego powrotu | 50 |
| Czas odnowienia | 10 sekund |
| Funkcja włączona | Tak |

## Jak wybierany jest bezpieczny cel?

Plugin sprawdza zapisany dom w świecie działki. Punkt musi mieścić się w granicach, zapewniać przestrzeń na nogi i głowę oraz stabilne podłoże. Sprawdzane są też płyny i wybrane niebezpieczne bloki, np. ogień, magma, kaktusy czy ogniska.

Jeżeli dom nie nadaje się do użycia, serwis szuka bezpiecznej pozycji w pionie nad środkiem działki, wewnątrz jej granic wysokości. Nie jest to przeszukanie całej powierzchni lokalu. W mieszkaniu ze złożonym układem najlepiej jawnie ustawić bezpieczny punkt.

## Koszt i niepowodzenie

Przed wykonaniem pobierana jest skonfigurowana opłata. Odrzucone pobranie blokuje teleport. Jeśli sama teleportacja zwróci niepowodzenie, serwis próbuje zwrócić koszt; udany powrót ustawia czas odnowienia.

Inny plugin może anulować teleportację. Jeśli zgłaszasz brak powrotu i problem z opłatą, podaj administracji czas operacji, ID działki i zmianę salda.

## Konfiguracja

```yaml
teleport:
  enabled: true
  cost: 50.0
  cooldown-seconds: 10
```

Koszt `0.0` oznacza darmową teleportację. `riftplots.admin.bypass` omija koszt, czas odnowienia i wyłączenie funkcji, ale nie pozwala tej ścieżce teleportować do działki w stanie resetu lub wyłączenia.

## Zmiana właściciela

Transfer, wygrana aukcja i reset czyszczą zapisany dom poprzedniej własności. Nowy właściciel powinien ustawić punkt ponownie.

Jeżeli pojawia się informacja o braku bezpiecznego miejsca, sprawdź wysokość zaznaczenia, wolne miejsce nad graczem i dostępność świata. [Pomoc w diagnozie →](Rozwiazywanie-problemow.md)
