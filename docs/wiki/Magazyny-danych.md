# Magazyny danych

[← Strona główna](Home.md) · [Konfiguracja](Konfiguracja.md) · [Migracja i kopie](Migracja-i-kopie-zapasowe.md)

## Dostępne backendy

| Backend | Gdzie przechowuje dane | Co trzeba przygotować |
| :--- | :--- | :--- |
| `YAML` | Pliki tekstowe w `plugins/riftPlots/data/`. | Dostępny katalog danych. |
| `SQLITE` | Domyślnie `data/riftplots.db`. | Dostęp do lokalnego pliku; backend domyślny. |
| `MYSQL` | Zewnętrzna baza SQL. | Istniejącą bazę i poprawne dane połączenia. |

Wybór następuje podczas startu pluginu. `reload` nie wymienia działającego backendu. Przy istniejących danych stosuj [migrację](Migracja-i-kopie-zapasowe.md), a nie samą zmianę nazwy magazynu.

## YAML

```text
data/
├── plots/              Rekordy działek
├── players/            Profile graczy
├── transactions/       Transakcje finansowe
├── signs.yml           Powiązania tabliczek
├── rentals.yml         Umowy najmu
├── auctions.yml        Stan aukcji
├── operations/         Dziennik zamiarów i etapów płatności
├── refunds/            Zobowiązania zwrotów
├── transfers.yml       Oczekujące oferty transferu
├── notifications/      Wiadomości i potwierdzenia wysłania
├── city_budget.yml     Księga miasta
└── audit_logs.yml      Logi operacji
```

Pliki można odczytać tekstowo. Nie edytuj ich podczas pracy pluginu: część rekordów pozostaje w cache i może nadpisać ręczną zmianę. Uszkodzony składniowo plik danych ma zgłosić błąd, nie zostać potraktowany jak pusta baza.

Historia i listy usług rosną wraz z użyciem. W tym backendzie wiele operacji zapisuje całe pliki usług, co trzeba uwzględnić przy większej liczbie rekordów.

## SQLite

```yaml
storage:
  type: "SQLITE"
  sqlite:
    file: "riftplots.db"
```

Plik trafia do katalogu `data/`. Pula połączeń SQLite jest ograniczona w kodzie do jednego połączenia. Parametry ogólnej puli w konfiguracji nie zwiększają jej automatycznie.

Sterownik SQLite jest dołączony do JAR-a. Jego pakiet nie jest relokowany, ponieważ natywne JNI odwołuje się do oryginalnych nazw klas. Test gotowego artefaktu wykonuje rzeczywiste zapytanie SQLite.

## MySQL

Przykład wybranych sekcji; zachowaj pozostałe ustawienia pliku:

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
  pool:
    maximum-pool-size: 10
    minimum-idle: 2
    idle-timeout-ms: 600000
    connection-timeout-ms: 30000
    max-lifetime-ms: 1800000
```

Wypełnij dane rzeczywistej bazy i ustaw SSL zgodnie z jej konfiguracją. Plugin tworzy tabele, ale nie tworzy samej bazy z sekcji `database`. Hasło konfigurujesz lokalnie; nie dołączaj go do zgłoszeń ani publicznych kopii pliku.

Parser konfiguracji rozpoznaje także nazwę `MARIADB` jako wariant `MYSQL`, ale używany jest konektor MySQL. Zgodność konkretnego serwera i jego ustawień wymaga testu w docelowym środowisku.

## Co jest trwałe?

Zapis obejmuje działki i członków, profile, powiązania tabliczek, transakcje, umowy najmu, aktualne aukcje, budżet i audyt. Schematy pozostają oddzielnymi plikami niezależnie od backendu.

Oferty transferu, wiadomości, dziennik operacji i zwroty są trwałe w lokalnym `data/`, niezależnie od backendu. Aukcje zapisują pełną historię przebić; SQL używa tabeli `rift_auction_bids`. Schemat danych ma wersję 2. [Aktualizacja i backup](Nowosci-P1-P2.md#migracja-testy-i-backup).

## Jedna baza nie oznacza wielu zsynchronizowanych serwerów

Każdy uruchomiony plugin ma własny cache. MySQL nie dodaje mechanizmu uzgadniania zmian, rozproszonych blokad ani odświeżania cache między serwerami. Nie traktuj współdzielenia bazy jako gotowej obsługi sieci serwerów dla tych samych działek.

Weryfikacja objęła rzeczywiste YAML, SQLite oraz MySQL 8.4.10, w tym restart zapisu i migrację MySQL → YAML z historią aukcji. Awarie i obciążenie docelowej bazy nadal wymagają sprawdzenia w Twoim środowisku.
