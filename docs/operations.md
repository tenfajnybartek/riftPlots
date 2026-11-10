# Konfiguracja i obsługa

[← README](../README.md) · [Komendy](commands.md) · [Architektura](architecture.md)

## Domyślne ustawienia

Nowe działki dziedziczą stawki z `plot-types.yml`. Pola `economy.default-*` pozostają w konfiguracji, ale przy rejestracji nie zastępują ustawień przypisanego typu. Polityka resetu i karencja są zapisywane przy tworzeniu. Starsze rekordy zachowują stawki jako jawne nadpisania.

| Klucz | Wartość domyślna | Znaczenie |
| :--- | :--- | :--- |
| `economy.default-price` | `25000.0` | Wartość startowa przed zastosowaniem ceny typu. |
| `economy.default-base-tax` | `1500.0` | Wartość startowa przed zastosowaniem podatku typu. |
| `economy.default-tax-mode` | `FIXED` | Tryb startowy; nowa działka dziedziczy tryb typu. |
| `economy.default-billing-period-days` | `7` | Okres startowy; nowa działka dziedziczy okres typu. |
| `economy.default-grace-period-hours` | `6` | Czas na uregulowanie zaległości. |
| `economy.max-advance-periods` | `4` | Limit dodatkowych okresów opłaconych z góry. |
| `economy.default-abandon-refund-percent` | `50.0` | Procent startowy przed zastosowaniem `refund-percent` typu. |
| `scheduler.check-interval-seconds` | `60` | Sprawdzanie podatków, najmu i aukcji. |
| `scheduler.tax-reminder-hours` | `[48, 24, 12, 2, 1]` | Okna przypomnień o podatku. |
| `reset.auto-reset-on-expire` | `true` | Domyślna polityka resetu nowych działek. |
| `reset.block-batch-size` | `2500` | Maksymalna liczba bloków w porcji. |
| `reset.tick-delay` | `1` | Odstęp pomiędzy porcjami resetu. |
| `market.transfer-fee-percent` | `5.0` | Prowizja z transferu gracz–gracz. |
| `market.transfer-offer-duration-seconds` | `300` | Ważność oferty transferu. |
| `auction.city-fee-percent` | `5.0` | Prowizja od aukcji nieruchomości gracza. |
| `auction.anti-snipe-extension-seconds` | `120` | Minimalne pozostałe okno po późnej ofercie. |
| `auction.escrow-enabled` | `false` | Rezerwacja środków podczas licytacji w nowych aukcjach. |
| `rental.max-periods` | `365` | Limit okresów przy wynajmie. |
| `privacy.public-history` | `false` | Prywatna historia lokalu dla właściciela i administracji. |
| `notifications.retention-days` | `30` | Retencja wysłanych wiadomości przy zapisie. |
| `notifications.max-history-per-player` | `200` | Limit wysłanych wpisów; niewysłane pozostają w kolejce. |
| `teleport.enabled` | `true` | Dostępność `/plots home`. |
| `teleport.cost` | `50.0` | Opłata za udaną teleportację. |
| `teleport.cooldown-seconds` | `10` | Czas odnowienia teleportacji. |
| `city-budget.initial-balance` | `100000.0` | Saldo nowej księgi miasta. |

Symbol `$` jest formatowaniem z `general.currency-symbol`; rozliczenia korzystają z waluty providera Vault. Budżet miasta jest wewnętrzną księgą pluginu, a nie osobnym kontem bankowym Vault. Korekty budżetu nie zasilają konta wykonującego komendę.

`auction.default-min-bid-increment` i `auction.default-duration-minutes` są wczytywane, ale komenda rozpoczynająca aukcję wymaga podania obu parametrów jawnie.

Granice 3D mają osobną sekcję `visualization`: domyślnie 10 sekund, 10 sekund cooldownu i 120 punktów. Auto-odnowienie najmu wybiera gracz, domyślnie jest wyłączone. Oferty właścicieli wymagają jawnego nadania `riftplots.rent.offer`; nie ma osobnego przełącznika konfiguracji dla tej funkcji.

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

Dziedziczenie i nadpisania:

- Dostępne działki aktualizują dziedziczone stawki po zmianie typu lub reloadzie. Indywidualne nadpisania pozostają zachowane.
- Cena dziedziczona to cena typu razy mnożnik cen dzielnicy. `setprice` ustala końcową cenę ręczną; reload nie mnoży jej ponownie.
- Podatek korzysta z `Plot.baseTax`, wybranego trybu i mnożnika dzielnicy. Dotyczy to również `CUSTOM`.
- `icon-material` określa ikonę rynku. Flagi stanu typu (`allow`/`deny`) są stosowane przy rejestracji oraz jawnym `type apply-defaults`.
- Administracyjne przypisanie typu lub dzielnicy nie przebudowuje wcześniejszych umów. Używaj go świadomie przy aktywnych nieruchomościach.

`/plots admin type apply-defaults <id>` pokazuje różnice; dopiero dodanie `confirm` usuwa ręczne nadpisania. Aktywny najem, aukcja lub reset blokuje to działanie. [Pełny opis wdrożenia P1–P2](wiki/Nowosci-P1-P2.md).

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

Migracja przenosi działki, członków, profile, transakcje, tabliczki, umowy, aukcje z historią przebić, budżet i audyt. Schematy oraz lokalne pliki operacji, zwrotów, transferów i wiadomości pozostają we wspólnym katalogu pluginu i nie wymagają konwersji backendu.

1. Wykonaj spójną kopię całego `plugins/riftPlots/` przy zatrzymanym serwerze; dla MySQL dodaj eksport bazy. Zachowaj również kopię świata.
2. Przygotuj pusty backend docelowy i dane połączenia. Nie zmieniaj jeszcze `storage.type`.
3. Uruchom migrację w oknie serwisowym, bez nowych transakcji graczy:

   ```text
   /plots admin migrate SQLITE MYSQL
   ```

4. Sprawdź wynik i liczby przeniesionych rekordów. Przy błędzie nie przełączaj magazynu. Częściowa kopia może pozostać w celu i wymaga osobnej weryfikacji przed ponowieniem.
5. Po sukcesie plugin zapisuje backend docelowy. Zrestartuj serwer, zachowując blokadę nowych zapisów do przełączenia.
6. Zweryfikuj właścicieli, terminy, najem, aukcje i saldo miasta przed wpuszczeniem graczy.

Komenda dopuszcza jako źródło aktywny backend i odmawia nadpisania niepustego celu. Kopiowanie wykonuje uporządkowana kolejka I/O przy wstrzymanych zmianach i nowych logowaniach. Sukces zapisuje docelowy backend w `storage.yml`; blokada trwa do restartu. `admin migrate resume confirm` wraca do źródła i czyni docelową kopię nieaktualną.

## Ochrona i reset

Sześć przełączników `protection.*` kontroluje dodatkową ochronę tłoków, płynów, wybuchów, ognia, stojaków na zbroje i ramek. Osobne listenery sprawdzają budowanie, niszczenie, pojemniki, drzwi, wiadra i wybrane interakcje z encjami. WorldGuard zapewnia region z właścicielem i członkami.

Reset odbiera własność, kończy najem z próbą zwrotu kaucji, anuluje aukcję i przywraca zapisane bloki partiami. Wczytanie schematu odbywa się poza wątkiem serwera; zmiana świata — na wątku serwera. Wymiary schematu muszą odpowiadać działce. Encje i biomy nie są kopiowane.

Brak schematu powoduje `DISABLED` z zachowaniem dotychczasowej własności. Błędny odczyt lub wymiary odrzucają przygotowanie przed oddaniem własności. Nieudane wklejanie pozostawia działkę wyłączoną już po oddaniu własności. Napraw przyczynę i sprawdź podgląd `/plots admin restore <id>`, następnie potwierdź przez `confirm`.

`/plots admin snapshot <id>` zastępuje wzorzec aktualnym stanem budynku. Nie zapisuj w ten sposób przypadkowo zniszczonego lokalu. `restore` i `reset` usuwają własność — nie są tylko kosmetycznym odświeżeniem bloków.

## Diagnoza problemów

| Objaw | Co sprawdzić |
| :--- | :--- |
| Plugin nie uruchamia się | Java 25, docelowe API, WorldEdit, WorldGuard, Vault i provider ekonomii; pełny błąd w konsoli. |
| Zakup odrzucony mimo wolnej działki | Ofertę najmu, aktywną aukcję, zakaz sprzedaży typu/dzielnicy i limit własności. |
| Nie można budować po `trust` | To domyślny zakres `MEMBER`: interakcje, drzwi i pojemniki. |
| Typ nie zmienił ceny ani podatku | Sprawdź ręczne nadpisania przez `admin info`. `type apply-defaults <id>` pokazuje zmiany przed potwierdzeniem. |
| Działka jest wyłączona po resecie | Istnienie i wymiary `schematics/<id>.schem`, dostępność świata i log błędu WorldEdit. |
| Zmiana bazy nie działa po `reload` | Backend jest wybierany podczas uruchomienia; potrzebny jest restart. |
| Kaucja nie została zwrócona | Najem kończy się niezależnie od zwrotu. Sprawdź `admin transactions refunds` i uzgodnij niepewne wypłaty bez usuwania zobowiązania. |
| Zwrot po porzuceniu nie dotarł | Sprawdź rekord `REFUND` ze statusem `FAILED` i konto rzeczywistego właściciela. Nieruchomość mogła już zostać oddana; potrzebna jest ręczna korekta. |

Nie edytuj bezpośrednio aktywnej bazy ani plików danych: plugin utrzymuje część rekordów w pamięci. Do edycji plikowej zatrzymaj serwer i zachowaj kopię.
