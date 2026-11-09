# Rozwiązywanie problemów

[← Strona główna](Home.md) · [FAQ](FAQ.md) · [Stan funkcji](Stan-funkcji.md)

## Zacznij od faktów

Przy zgłoszeniu ustal: co wykonano, jaki był oczekiwany wynik, ID działki, konto gracza i czas zdarzenia. Sprawdź konsolę od pierwszego błędu z tego czasu. Kolejny komunikat może być tylko skutkiem wcześniejszego nieudanego odczytu lub połączenia.

## Plugin nie uruchamia się

| Sprawdź | Dlaczego |
| :--- | :--- |
| Java 25 i docelowe API Paper | To środowisko zapisane w projekcie. |
| WorldEdit i WorldGuard | Są wymaganymi integracjami. |
| Vault oraz provider ekonomii | Sam Vault nie udostępnia sald bez providera. |
| Dane połączenia i istnienie MySQL | Plugin tworzy tabele, nie samą bazę. |
| Odczyt plików danych | Błędny YAML ma zgłaszać awarię, nie pusty magazyn. |

Nie usuwaj katalogu danych jako pierwszego kroku. Zachowaj kopię i znajdź przyczynę.

## „Nie mogę kupić dostępnej działki”

Sprawdź `admin info`, aktywną aukcję, ofertę najmu i umowę. Sam status `AVAILABLE` nie oznacza bezwarunkowej możliwości zakupu. Sprawdź również `sales-allowed`, `sales-enabled`, dozwolony typ oraz limit własności w dzielnicy.

Jeśli przeszkodą jest ekonomia, porównaj saldo i wynik pobrania u providera. Widoczna kwota nie gwarantuje, że provider zaakceptuje transakcję.

## „Po trust nie można budować”

To domyślny zakres `MEMBER`: interakcje, drzwi i pojemniki. Nie obejmuje stawiania i niszczenia bloków. Najemca otrzymuje szerszy zestaw na czas umowy. [Role i komendy](Komendy-gracza.md).

## „Ochrona nie działa na moim koncie”

Sprawdź `riftplots.admin.bypass`, uprawnienia i bypass innych pluginów. Testuj także obcym kontem bez operatora. Potwierdź, że czynność dotyczy bloków wewnątrz pełnych granic X/Y/Z.

## „Typ lub dzielnica nie zmieniły ceny”

Typ nie przepisuje automatycznie domyślnych stawek, a mnożnik cen dzielnicy nie wpływa na ścieżkę bezpośredniego zakupu. Użyj `setprice`, `settax`, `settaxmode`, `setperiod`. Podatek dzielnicy jest mnożony przy obliczeniu. [Szczegóły](Dzielnice-i-typy.md).

## „Wyliczony podatek jest za duży”

Porównaj podstawę, tryb i granice. W `PER_BLOCK` podstawa jest mnożona przez całą powierzchnię poziomą, a w `PER_CHUNK` przez każdą przecinaną kolumnę chunku. Dodawany jest też mnożnik dzielnicy; `CUSTOM` go nie pomija.

## „Działka jest wyłączona po resecie”

Sprawdź `schematics/<id>.schem`, jego niepusty rozmiar i wymiary, dostępność świata oraz log WorldEdit. Odtwórz poprawny plik z kopii i ponów `restore`. Nie zapisuj wzorca z przypadkowo zniszczonego budynku.

[Schematy i reset](Schematy-i-reset.md) wyjaśniają różnicę między brakiem schematu przed rozpoczęciem a błędem wklejania po odebraniu własności.

## „Teleport nie znalazł miejsca”

Sprawdź zapisany dom, wysokość granic, podłoże i wolne miejsce na nogi/głowę. Zapasowe wyszukiwanie dotyczy pionu nad środkiem działki, nie całego mieszkania. Ustaw bezpieczny dom jawnie. [Teleportacja](Teleportacja.md).

## „Najem nie kończy się przy błędzie kaucji”

Nieudana wypłata kaucji może zachować umowę do ponowienia zwrotu. Sprawdź ekonomię i konto odbiorcy; nie usuwaj umowy lub członka ręcznie. Taka zmiana może zgubić zobowiązanie do wypłaty.

## „Wygrałem aukcję, ale nie dostałem lokalu”

Środki nie są rezerwowane przy licytowaniu. Jeśli przy finalizacji zwycięzca nie ma pieniędzy lub pobranie zostało odrzucone, aukcja jest anulowana. Nie wybiera automatycznie drugiej osoby. Sprawdź też aktualną własność i reguły nabycia. [Aukcje](Aukcje.md).

## „Oferta transferu zniknęła”

Mogła wygasnąć, zostać zastąpiona nową ofertą tej samej działki albo zostać usunięta przy restarcie. Oferty oczekujące są w pamięci. Wystaw nową i użyj jej dokładnego ID.

## „Zmiana backendu nie działa po reload”

`reload` nie wymienia aktywnego magazynu. Potrzebny jest restart, a dla istniejących danych również [migracja](Migracja-i-kopie-zapasowe.md). Jeśli komenda migracji mówi o niepustym celu, nie pomijaj tego zabezpieczenia przypadkowymi usunięciami.

## „Build przechodzi, a IntelliJ podkreśla testy”

Odśwież model Gradle przez **Reload All Gradle Projects** i sprawdź JDK projektu. Po zmianie zależności IDE może nie widzieć jeszcze `Mockito` lub `MockedStatic`, mimo że Gradle już je rozwiązuje. Potem ponów inspekcję pliku i uruchom testy przez Gradle.

## Co dołączyć do zgłoszenia?

```text
Wersja riftPlots:
Wersja Paper i Java:
WorldEdit / WorldGuard / Vault / provider ekonomii:
Backend:
ID działki i czas zdarzenia:
Wykonana komenda / czynność:
Oczekiwany wynik:
Rzeczywisty wynik:
Pierwszy pełny błąd z konsoli:
```

Dołącz odpowiedni fragment konfiguracji po usunięciu haseł i innych danych dostępowych. Dla płatności podaj kwotę i zmianę salda. Zgłoszenia projektu: [GitHub Issues](https://github.com/tenfajnybartek/riftPlots/issues).
