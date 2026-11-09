# Konfiguracja i obsługa

[← README](../README.md) · [Komendy](commands.md) · [Architektura](architecture.md)

## Domyślne ustawienia

Nowe działki pobierają ustawienia z `config.yml` w momencie tworzenia. Późniejsza edycja globalnych stawek nie przepisuje zapisanych cen ani terminów istniejących działek.

| Klucz | Wartość domyślna | Znaczenie |
| :--- | :--- | :--- |
| `economy.default-price` | `25000.0` | Cena nowej działki. |
| `economy.default-base-tax` | `1500.0` | Podstawa podatku. |
| `economy.default-tax-mode` | `FIXED` | Sposób obliczania podatku. |
| `economy.default-billing-period-days` | `7` | Długość okresu podatkowego. |
| `economy.default-grace-period-hours` | `6` | Czas na uregulowanie zaległości. |
| `economy.max-advance-periods` | `4` | Limit dodatkowych okresów opłaconych z góry. |
| `economy.default-abandon-refund-percent` | `50.0` | Zwrot liczony od zapisanej ceny działki. |
| `scheduler.check-interval-seconds` | `60` | Sprawdzanie podatków, najmu i aukcji. |
| `scheduler.tax-reminder-hours` | `[48, 24, 12, 2, 1]` | Okna przypomnień o podatku. |
| `reset.auto-reset-on-expire` | `true` | Domyślna polityka resetu nowych działek. |
| `reset.block-batch-size` | `2500` | Maksymalna liczba bloków w porcji. |
| `reset.tick-delay` | `1` | Odstęp pomiędzy porcjami resetu. |
| `market.transfer-fee-percent` | `5.0` | Prowizja z transferu gracz–gracz. |
| `market.transfer-offer-duration-seconds` | `300` | Ważność oferty transferu. |
| `auction.city-fee-percent` | `5.0` | Prowizja od aukcji nieruchomości gracza. |
| `auction.anti-snipe-extension-seconds` | `120` | Minimalne pozostałe okno po późnej ofercie. |
| `teleport.enabled` | `true` | Dostępność `/plots home`. |
| `teleport.cost` | `50.0` | Opłata za udaną teleportację. |
| `teleport.cooldown-seconds` | `10` | Czas odnowienia teleportacji. |
| `city-budget.initial-balance` | `100000.0` | Saldo nowej księgi miasta. |

Symbol `$` jest formatowaniem z `general.currency-symbol`; rozliczenia korzystają z waluty providera Vault. Budżet miasta jest wewnętrzną księgą pluginu, a nie osobnym kontem bankowym Vault. Korekty budżetu nie zasilają konta wykonującego komendę.

`auction.default-min-bid-increment` i `auction.default-duration-minutes` są wczytywane, ale komenda rozpoczynająca aukcję wymaga podania obu parametrów jawnie.

## Podatki

Powierzchnia to `(maxX − minX + 1) × (maxZ − minZ + 1)`. Wysokość nie zwiększa podatku od bloku. Liczba chunków oznacza wszystkie kolumny chunków przecinające prostokąt, także częściowo.

Niech `B` oznacza stawkę bazową, `A` powierzchnię, `C` liczbę chunków, `M` mnożnik działki, a `D` mnożnik podatku dzielnicy lub `1`, gdy działka nie ma dzielnicy.

| Tryb | Podatek za okres |
| :--- | :--- |
| `FIXED` | `B × D` |
| `CUSTOM` | `B × D` — w obecnym kodzie działa tak samo jak `FIXED`. |
| `PER_BLOCK` | `B × A × D` |
| `PER_CHUNK` | `B × C × D` |
| `MULTIPLIER` | `B × max(0.01, M) × D` |
| `SIZE_TIERS` | `B × próg(A) × D` |

| Powierzchnia w `SIZE_TIERS` | Współczynnik |
| :--- | ---: |
| Do 250 | 1.0 |
| 251–1 000 | 1.5 |
| 1 001–2 500 | 2.5 |
| 2 501–5 000 | 4.0 |
| Powyżej 5 000 | 6.0 |

Wynik jest zaokrąglany do dwóch miejsc przez `HALF_UP`. Podatek wyłączony albo nieodatnia podstawa dają `0`.

**Przykład:** działka 10 × 10, `PER_BLOCK`, stawka `2.50`, dzielnica `centrum` z mnożnikiem `1.25` daje `2.50 × 100 × 1.25 = 312.50` za okres.

Przy zakupie pierwszy termin przypada po jednym okresie. Wpłata przesuwa przyszły termin o opłacone okresy; po zaległości punktem wyjścia jest czas wpłaty. Dostępny limit przedpłat maleje wraz z upływem czasu. Karencja jest liczona od zapisanego terminu podatku, więc restart nie zapewnia nowego pełnego okresu karencji.

## Typy i dzielnice

Plik `plot-types.yml` zawiera sześć typów:

| Typ | Najem | Sprzedaż | Limit członków |
| :--- | :---: | :---: | ---: |
| `RESIDENTIAL` | Tak | Tak | 5 |
| `COMMERCIAL` | Tak | Tak | 10 |
| `INDUSTRIAL` | Nie | Tak | 15 |
| `PREMIUM` | Tak | Tak | 20 |
| `RENTAL` | Tak | Nie | 3 |
| `CUSTOM` | Tak | Tak | 25 |

Sprzedaż i wynajem weryfikują zasady typu oraz dzielnicy. Zakup i nabycie przez transfer lub aukcję sprawdzają limit posiadanych działek w dzielnicy. `max-plots-per-player: 0` oznacza brak limitu własności; limit członków `0` nie pozwala dodawać współużytkowników przez `trust`.

| Dzielnica | Mnożnik podatku | Limit własności | Dozwolone typy |
| :--- | ---: | ---: | :--- |
| `centrum` | 1.25 | 2 | `COMMERCIAL`, `PREMIUM`, `RESIDENTIAL` |
| `stare_miasto` | 1.10 | 2 | `RESIDENTIAL`, `COMMERCIAL` |
| `przedmiescia` | 0.75 | 4 | `RESIDENTIAL` |
| `handlowa` | 1.30 | 3 | `COMMERCIAL`, `RENTAL` |
| `przemyslowa` | 0.90 | 5 | `INDUSTRIAL`; najem wyłączony |

Aktualne granice działania konfiguracji:

- `type set` zapisuje typ. Domyślne ceny, podatki, tryby, okresy i zwroty z definicji typu nie są automatycznie kopiowane na działkę.
- Cena zakupu pochodzi z `Plot.price`. Mnożnik cen dzielnicy nie jest podłączony do tej ścieżki. Ustaw konkretną cenę przez `setprice`.
- Podatek korzysta z `Plot.baseTax`, wybranego trybu i mnożnika dzielnicy. Dotyczy to również `CUSTOM`.
- `flags` i `icon-material` w definicji typu nie są automatycznie stosowane w regionie WorldGuard ani w GUI.
- Administracyjne przypisanie typu lub dzielnicy nie przebudowuje wcześniejszych umów. Używaj go świadomie przy aktywnych nieruchomościach.

## Magazyny danych

W `storage.yml` ustaw `storage.type` na `YAML`, `SQLITE` lub `MYSQL`.

| Backend | Lokalizacja / połączenie | Uwagi |
| :--- | :--- | :--- |
| YAML | `data/plots/`, `data/players/`, `data/transactions/`, `data/signs.yml` oraz pliki usług | Rekordy możliwe do odczytania jako tekst; operacje plikowe są synchroniczne. |
| SQLite | Domyślnie `data/riftplots.db` | Jeden lokalny plik i pojedyncze połączenie w puli. |
| MySQL | Zewnętrzna baza ze `storage.mysql` | Baza musi istnieć przed uruchomieniem; tabele tworzy plugin. |

Przykładowe ustawienia połączenia — pozostałe sekcje `storage.yml` zachowaj:

```yaml
storage:
  type: "MYSQL"
  mysql:
    host: "127.0.0.1"
    port: 3306
    database: "riftplots"
    username: "riftplots"
    password: "UZUPELNIJ_LOKALNIE"
    use-ssl: false
```

Parametry puli `storage.pool` dotyczą połączenia MySQL. SQLite ma w kodzie pulę ograniczoną do jednego połączenia. Samo użycie MySQL nie zapewnia synchronizacji kilku serwerów — każdy proces utrzymuje własny cache działek i usług.

## Migracja i kopie zapasowe

Migracja przenosi zapisane działki, członków działek, profile graczy, transakcje finansowe, tabliczki, umowy najmu, aktualny stan aukcji, budżet i audyt. Schematy pozostają we wspólnym katalogu `schematics/` i nie wymagają konwersji backendu. Oferty transferu i oczekujące powiadomienia są przechowywane w pamięci i nie są migrowane.

1. Wykonaj spójną kopię całego `plugins/riftPlots/` przy zatrzymanym serwerze; dla MySQL dodaj eksport bazy. Zachowaj również kopię świata.
2. Przygotuj pusty backend docelowy i dane połączenia. Nie zmieniaj jeszcze `storage.type`.
3. Uruchom migrację w oknie serwisowym, bez nowych transakcji graczy:

   ```text
   /plots admin migrate SQLITE MYSQL
   ```

4. Sprawdź wynik i liczby przeniesionych rekordów. Przy błędzie nie przełączaj magazynu. Częściowa kopia może pozostać w celu i wymaga osobnej weryfikacji przed ponowieniem.
5. Zatrzymaj serwer, zmień `storage.type` na backend docelowy i uruchom go ponownie.
6. Zweryfikuj właścicieli, terminy, najem, aukcje i saldo miasta przed wpuszczeniem graczy.

Komenda dopuszcza jako źródło aktywny backend i odmawia nadpisania niepustego celu. Kopiowanie odbywa się synchronicznie na wątku serwera, aby komendy i scheduler nie zmieniały danych w trakcie odczytu. Przy dużej liczbie rekordów oznacza to pauzę w obsłudze ticków. Zmiany wykonane po skopiowaniu nie pojawią się automatycznie w celu.

## Ochrona i reset

Sześć przełączników `protection.*` kontroluje dodatkową ochronę tłoków, płynów, wybuchów, ognia, stojaków na zbroje i ramek. Osobne listenery sprawdzają budowanie, niszczenie, pojemniki, drzwi, wiadra i wybrane interakcje z encjami. WorldGuard zapewnia region z właścicielem i członkami.

Reset odbiera własność, kończy najem z próbą zwrotu kaucji, anuluje aukcję i przywraca zapisane bloki partiami. Wczytanie schematu odbywa się poza wątkiem serwera; zmiana świata — na wątku serwera. Wymiary schematu muszą odpowiadać działce. Encje i biomy nie są kopiowane.

Brak schematu powoduje `DISABLED` z zachowaniem dotychczasowej własności. Nieudane wklejanie również pozostawia działkę wyłączoną; w tym przypadku własność została już oddana. Napraw schemat lub przyczynę błędu i ponów `/plots admin restore <id>`.

`/plots admin snapshot <id>` zastępuje wzorzec aktualnym stanem budynku. Nie zapisuj w ten sposób przypadkowo zniszczonego lokalu. `restore` i `reset` usuwają własność — nie są tylko kosmetycznym odświeżeniem bloków.

## Diagnoza problemów

| Objaw | Co sprawdzić |
| :--- | :--- |
| Plugin nie uruchamia się | Java 25, docelowe API, WorldEdit, WorldGuard, Vault i provider ekonomii; pełny błąd w konsoli. |
| Zakup odrzucony mimo wolnej działki | Ofertę najmu, aktywną aukcję, zakaz sprzedaży typu/dzielnicy i limit własności. |
| Nie można budować po `trust` | To domyślny zakres `MEMBER`: interakcje, drzwi i pojemniki. |
| Typ nie zmienił ceny ani podatku | Ustaw `setprice`, `settax`, `settaxmode` i `setperiod` osobno. |
| Działka jest wyłączona po resecie | Istnienie i wymiary `schematics/<id>.schem`, dostępność świata i log błędu WorldEdit. |
| Zmiana bazy nie działa po `reload` | Backend jest wybierany podczas uruchomienia; potrzebny jest restart. |
| Najem nie kończy się przy awarii wypłaty | Sprawdź provider Vault i wypłatę kaucji; mechanizm zachowuje umowę do ponowienia zwrotu. |
| Zwrot po porzuceniu nie dotarł | Sprawdź rekord `REFUND` ze statusem `FAILED` i konto rzeczywistego właściciela. Nieruchomość mogła już zostać oddana; potrzebna jest ręczna korekta. |

Nie edytuj bezpośrednio aktywnej bazy ani plików danych: plugin utrzymuje część rekordów w pamięci. Do edycji plikowej zatrzymaj serwer i zachowaj kopię.
