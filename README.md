# riftPlots — Advanced City & Real Estate System dla Minecraft Paper 26.3

**riftPlots** to zaawansowany, produkcyjny system miejskich nieruchomości, gospodarki miejskiej i rynku działek stworzony specjalnie dla serwerów **Minecraft Paper 26.3** z wykorzystaniem **Javy 25**.

Plugin realizuje profesjonalną koncepcję **gotowych nieruchomości miejskich**. Administratorzy i budowniczowie wznoszą tętniące życiem miasta z kamienicami, sklepami, wieżowcami, fabrykami i willami, a gracze nabywają, wynajmują, handlują lub licytują gotowe lokale, uiszczając cykliczne podatki do wirtualnego budżetu miejskiego. W razie niewypłacalności lub porzucenia, nieruchomość automatycznie powraca do stanu pierwotnego ze schematu.

---

## Spis treści

1. [Wymagania techniczne](#1-wymagania-techniczne)
2. [Główne filary systemu](#2-główne-filary-systemu)
3. [Instalacja i szybki start](#3-instalacja-i-szybki-start)
4. [Typy nieruchomości i strefy ekonomiczne (Dzielnice)](#4-typy-nieruchomości-i-strefy-ekonomiczne-dzielnice)
   - [Domyślne typy nieruchomości](#domyślne-typy-nieruchomości)
   - [Dzielnice miejskie](#dzielnice-miejskie)
   - [Hierarchia dziedziczenia parametrów](#hierarchia-dziedziczenia-parametrów)
5. [System wynajmu lokali (Rental Engine)](#5-system-wynajmu-lokali-rental-engine)
6. [Rynek wtórny i przekazywanie nieruchomości (Transfer Engine)](#6-rynek-wtórny-i-przekazywanie-nieruchomości-transfer-engine)
7. [System aukcji i licytacji (Auction Engine)](#7-system-aukcji-i-licytacji-auction-engine)
8. [Urząd miasta i wirtualny budżet miejski (City Budget)](#8-urząd-miasta-i-wirtualny-budżet-miejski-city-budget)
9. [Punkty domowe i bezpieczna teleportacja](#9-punkty-domowe-i-bezpieczna-teleportacja)
10. [Tutorial Administratora — Zarządzanie miastem krok po kroku](#10-tutorial-administratora--zarządzanie-miastem-krok-po-kroku)
11. [Tutorial Gracza — Życie i biznes w mieście krok po kroku](#11-tutorial-gracza--życie-i-biznes-w-mieście-krok-po-kroku)
12. [Modele podatkowe i kalkulacje finansowe](#12-modele-podatkowe-i-kalkulacje-finansowe)
13. [Wielowarstwowa ochrona terenu i mechanizm resetu](#13-wielowarstwowa-ochrona-terenu-i-mechanizm-resetu)
14. [Migracje baz danych (Data Migration Engine)](#14-migracje-baz-danych-data-migration-engine)
15. [Pełny wykaz komend i uprawnień](#15-pełny-wykaz-komend-i-uprawnień)
16. [Opis plików konfiguracyjnych](#16-opis-plików-konfiguracyjnych)

---

## 1. Wymagania techniczne

* **Silnik serwera:** Paper 26.3 (lub nowszy zgodny z Paper API 26.3).
* **Wersja Javy:** Java 25 LTS (OpenJDK, Temurin, GraalVM lub Oracle JDK).
* **Zależności (Plugins):**
  * **Vault** + dowolny provider ekonomiczny (np. EssentialsX, TheEconomy).
  * **WorldEdit** (7.3.10+).
  * **WorldGuard** (7.0.12+).
* **Formatowanie tekstu:** Adventure API z pełnym wsparciem MiniMessage (kolory HEX, gradienty, hover, click events).

---

## 2. Główne filary systemu

* **Architektura gotowych lokali:** Każda parcela to zaplanowany obiekt miejski. Przy utworzeniu zapisywany jest wierny snapshot bloków, block states i zawartości kontenerów (WorldEdit Sponge schematic).
* **Odporny na restarty reset nieruchomości:** Transakcyjne przywracanie terenu po utracie własności bez ryzyka naruszenia parceli sąsiadów.
* **Klasyfikacja i urbanistyka:** Dzielnice miejskie z indywidualnymi mnożnikami, ograniczeniami typów parcel oraz limitami na gracza.
* **System wynajmu (Rental):** Pokoje, mieszkania, lokale handlowe z czynszem godzinowym, dziennym, tygodniowym lub miesięcznym, kaucjami zwrotnymi i auto-odnawianiem.
* **Handel gracz-gracz:** Bezpieczne oferty sprzedaży z potwierdzeniem, prowizją miejską i weryfikacją zadłużenia.
* **Publiczne licytacje:** Aukcje nieruchomości z ochroną przed snajpowaniem (anti-snipe) i automatycznym rozliczeniem wygranych.
* **Skarb Miasta (City Budget):** Centralne wirtualne konto miejskie kumulujące podatki i finansujące zwroty oraz inwestycje.
* **Trzy silniki magazynowania:** YAML, SQLite oraz MySQL/MariaDB z pulą HikariCP i narzędziem migracji w czasie rzeczywistym.

---

## 3. Instalacja i szybki start

1. Wgraj `riftPlots-1.0.0.jar` oraz wymagane wtyczki (Vault, WorldEdit, WorldGuard, plugin ekonomii) do folderu `plugins/`.
2. Uruchom serwer na środowisku Java 25.
3. W katalogu `plugins/riftPlots/` wygenerują się automatycznie:
   * `config.yml` — ustawienia ogólne, mechanika ochrony, interwały.
   * `storage.yml` — wybór silnika danych (SQLITE, YAML, MYSQL).
   * `lang.yml` — komunikaty MiniMessage.
   * `districts.yml` — definicje dzielnic miejskich.
   * `plot-types.yml` — typy nieruchomości i ich domyślne parametry.
4. Plugin jest od razu gotowy do pracy!

---

## 4. Typy nieruchomości i strefy ekonomiczne (Dzielnice)

### Domyślne typy nieruchomości

Zdefiniowane w `plot-types.yml`:

| Typ | Nazwa | Domyślna cena | Podatek bazowy | Tryb | Limit członków | Wynajem | Sprzedaż |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **`RESIDENTIAL`** | Działka Mieszkalna | 25 000 $ | 1 500 $ / 7 dni | FIXED | 5 | Tak | Tak |
| **`COMMERCIAL`** | Lokal Usługowy | 45 000 $ | 3 000 $ / 7 dni | FIXED | 10 | Tak | Tak |
| **`INDUSTRIAL`** | Obiekt Przemysłowy | 60 000 $ | 4 500 $ / 14 dni | PER_CHUNK | 15 | Nie | Tak |
| **`PREMIUM`** | Posiadłość Premium | 100 000 $ | 2 000 $ / 30 dni | FIXED | 20 | Tak | Tak |
| **`RENTAL`** | Lokal na Wynajem | 30 000 $ | 1 000 $ / 7 dni | FIXED | 3 | Tak | Nie |
| **`CUSTOM`** | Obiekt Specjalny | 50 000 $ | 2 500 $ / 7 dni | CUSTOM | 25 | Tak | Tak |

Administrator może dowolnie dodawać nowe typy w pliku `plot-types.yml` lub przypisywać je działkom komendą:
```text
/plots admin type set <działka> <TYP>
```

### Dzielnice miejskie

Zdefiniowane w `districts.yml`. Każda dzielnica kontroluje lokalną strefę ekonomiczną:

* **Centrum Miasta (`centrum`):** Mnożnik cen 1.5x, podatków 1.25x. Dozwolone: COMMERCIAL, PREMIUM, RESIDENTIAL. Limit: 2 działki/gracza.
* **Stare Miasto (`stare_miasto`):** Mnożnik cen 1.2x, podatków 1.1x. Dozwolone: RESIDENTIAL, COMMERCIAL. Limit: 2 działki/gracza.
* **Przedmieścia (`przedmiescia`):** Mnożnik cen 0.8x, podatków 0.75x. Dozwolone: RESIDENTIAL. Limit: 4 działki/gracza.
* **Dzielnica Handlowa (`handlowa`):** Mnożnik cen 1.3x, podatków 1.3x. Dozwolone: COMMERCIAL, RENTAL. Limit: 3 działki/gracza.
* **Strefa Przemysłowa (`przemyslowa`):** Mnożnik cen 0.9x, podatków 0.9x. Dozwolone: INDUSTRIAL. Wynajem wyłączony. Limit: 5 działek/gracza.

Przypisywanie działki do dzielnicy:
```text
/plots admin district set <działka> <dzielnica>
```

### Hierarchia dziedziczenia parametrów

System stosuje przejrzystą regułę pierwszeństwa:
1. **Wartość wpisana bezpośrednio dla działki** (np. indywidualna cena z komendy `/plots admin setprice`).
2. **Mnożnik dzielnicy $\times$ stawka typu nieruchomości** (np. `COMMERCIAL` w `centrum` = stawka bazowa typu $\times 1.25$).
3. **Domyślne wartości z `config.yml`**.

---

## 5. System wynajmu lokali (Rental Engine)

Właściciel nieruchomości lub administrator może przeznaczyć działkę na wynajem.

### Udostępnienie nieruchomości do wynajmu:
```text
/plots admin rent offer <działka> <cena> <HOUR|DAY|WEEK|MONTH> [kaucja]
```
*Przykład:* `/plots admin rent offer apartament_4 250 DAY 500` (Czynsz 250 $ dziennie, kaucja zwrotna 500 $).

### Wynajem przez gracza:
```text
/plots rent <działka> [liczba_okresów]
```
*Przykład:* `/plots rent apartament_4 7` (Wynajem na 7 dni).

**Prawa i obowiązki najemcy:**
* Z konta najemcy pobierany jest czynsz (trafia do właściciela lub budżetu miasta) oraz kaucja.
* Najemca otrzymuje uprawnienia do budowania, niszczenia, otwierania pojemników i drzwi.
* Najemca **NIE staje się właścicielem** — nie może zmieniać parametrów działki, sprzedać jej ani zresetować.
* **Automatyczne odnawianie:** Jeśli włączone, scheduler automatycznie pobiera czynsz na kolejny okres.
* **Zakończenie najmu:** Kaucja zostaje zwrócona, uprawnienia odebrane, a działka zwolniona dla kolejnych chętnych.

---

## 6. Rynek wtórny i przekazywanie nieruchomości (Transfer Engine)

Właściciel może bezpiecznie sprzedać lub przekazać swoją działkę innemu graczowi bez ryzyka oszustwa:

1. **Wysłanie oferty:**
   ```text
   /plots transfer <gracz> <cena> [działka]
   ```
   *Przykład:* `/plots transfer Janek 40000 rynek_1` (Oferta sprzedaży za 40 000 $ ważna przez 5 minut).
2. **Akceptacja przez kupującego:**
   ```text
   /plots accept [id_oferty]
   ```
3. **Odrzucenie oferty:**
   ```text
   /plots deny <id_oferty>
   ```

**Zasady bezpieczeństwa:**
* Działka nie może mieć zaległości podatkowych (`PAYMENT_DUE` lub `GRACE_PERIOD`).
* Kupujący musi posiadać wymaganą kwotę w Vault.
* Z transakcji pobierana jest konfigurowalna prowizja miejska (np. 5%), która zasila Skarb Miasta.
* Zmiana właściciela następuje natychmiastowo, a region WorldGuard zostaje zaktualizowany.

---

## 7. System aukcji i licytacji (Auction Engine)

Administrator lub uprawniony gracz może wystawić działkę na publiczną licytację miejską:

### Rozpoczęcie aukcji:
```text
/plots admin auction start <działka> <cena_startowa> <przebicie> <czas_w_minutach> [kup_teraz]
```
*Przykład:* `/plots admin auction start willa_1 50000 2500 30 150000`

### Udział w licytacji:
* Podgląd aktywnych licytacji: `/plots auction list`
* Złożenie oferty: `/plots auction bid <działka> <kwota>`

**Mechanika Anti-Snipe:** Jeśli gracz złoży ofertę w ostatnich 2 minutach trwania aukcji, czas zakończenia zostaje automatycznie przedłużony o 2 minuty, zapobiegając botom i podbijaniu w ostatniej sekundzie.

**Finalizacja aukcji:** Scheduler automatycznie weryfikuje zakończenie czasu, pobiera środki od zwycięzcy, przepisuje własność działki i odprowadza prowizję do budżetu miasta.

---

## 8. Urząd miasta i wirtualny budżet miejski (City Budget)

System posiada niezależne wirtualne konto Skarbu Miasta, rejestrujące wszystkie przepływy kapitałowe:

* **Przychody miasta:**
  * Podatki od nieruchomości opłacane przez graczy.
  * Pieniądze z zakupu działek miejskich.
  * Czynsze z wynajmu nieruchomości komunalnych.
  * Prowizje z aukcji i rynku wtórnego.
* **Wydatki miasta:**
  * Zwroty środków przy porzuceniu działek (`/plots abandon`).
  * Rekompensaty i dotacje miejskie.

### Komendy zarządzania budżetem:
* Sprawdzenie salda miasta: `/plots admin budget balance`
* Historia operacji finansowych: `/plots admin budget history`
* Wpłata do budżetu: `/plots admin budget add <kwota> [powód]`
* Wypłata z budżetu: `/plots admin budget withdraw <kwota> [powód]`

Każda operacja zostaje trwale odnotowana w dzienniku audytowym z identyfikatorem i datą.

---

## 9. Punkty domowe i bezpieczna teleportacja

Gracz może wygodnie wracać na swoją nieruchomość:
* Ustawienie punktu domowego: `/plots sethome [działka]` (punkt musi znajdować się wewnątrz parceli).
* Teleportacja: `/plots home [działka]`

**Algorytm bezpiecznej teleportacji:**
* Weryfikuje blok pod stopami (musi być stabilny, brak lawy, ognia, bloków magmy).
* Weryfikuje przestrzeń na wysokości nóg i głowy (musi być wolna).
* W razie braku ustalonego punktu domowego wyznacza bezpieczną lokalizację na najwyższym bloku w centrum działki.
* Blokuje teleportację do nieruchomości w trakcie resetowania (`RESETTING`) lub wyłączonych (`DISABLED`).

---

## 10. Tutorial Administratora — Zarządzanie miastem krok po kroku

### Krok 1: Projekt i zaznaczenie parceli
Zbuduj budynek, weź drewnianą siekierkę (`//wand`) i zaznacz dwa przeciwległe narożniki prostopadłościanu.

### Krok 2: Rejestracja działki
```text
/plots admin create rynek_1 Kamienica pod Złotym Lwem
```
Działka zostaje zarejestrowana, powstaje region WorldGuard oraz snapshot schematu.

### Krok 3: Przypisanie dzielnicy i typu
```text
/plots admin district set rynek_1 centrum
/plots admin type set rynek_1 COMMERCIAL
```
Działka natychmiast dziedziczy mnożniki cen i podatków dla lokalu handlowego w centrum.

### Krok 4: Tabliczki sprzedaży
Spójrz na tabliczkę na ścianie i wpisz:
```text
/plots admin sign rynek_1 FOR_SALE
```

### Krok 5: Wystawienie na wynajem (opcjonalnie)
Jeśli zamiast sprzedaży wolisz wynajem:
```text
/plots admin rent offer rynek_1 500 DAY 1000
/plots admin sign rynek_1 RENEW
```

---

## 11. Tutorial Gracza — Życie i biznes w mieście krok po kroku

1. **Wyszukaj ofertę:** Użyj `/plots list`, `/plots gui` lub znajdź tabliczkę `[DZIAŁKA]` z zielonym napisem `KUP TERAZ`.
2. **Kup działkę:** Kliknij tabliczkę prawym przyciskiem myszy lub wpisz `/plots buy rynek_1`.
3. **Urządź się:** Wpisz `/plots sethome` w salonie swojego nowego domu.
4. **Zaproś znajomych:** Wpisz `/plots trust Michal rynek_1`, aby dać koledze dostęp do skrzyń i budowania.
5. **Płać podatki na czas:** Sprawdzaj `/plots info`. Płać podatek komendą `/plots pay` (lub opłać kilka tygodni z góry: `/plots pay rynek_1 4`).
6. **Sprzedaj z zyskiem:** Gdy postanowisz zmienić lokal, zaoferuj go innemu graczowi za pomocą `/plots transfer Janek 60000 rynek_1`.

---

## 12. Modele podatkowe i kalkulacje finansowe

* **`FIXED`:** $\text{Podatek} = \text{Stawka bazowa} \times \text{Mnożnik dzielnicy}$.
* **`PER_BLOCK`:** $\text{Podatek} = \text{Stawka za blok} \times \text{Powierzchnia } (X \times Z) \times \text{Mnożnik dzielnicy}$.
* **`PER_CHUNK`:** $\text{Podatek} = \text{Stawka za chunk} \times \text{Liczba objętych chunków} \times \text{Mnożnik dzielnicy}$.
* **`SIZE_TIERS`:** Progi dynamiczne ($\le 250\text{ m}^2: 1.0\times$, $\le 1000\text{ m}^2: 1.5\times$, $\le 2500\text{ m}^2: 2.5\times$, $>2500\text{ m}^2: 4.0\times$).
* **`MULTIPLIER`:** $\text{Podatek} = \text{Stawka bazowa} \times \text{Mnożnik działki} \times \text{Mnożnik dzielnicy}$.
* **`CUSTOM`:** Indywidualna stawka bez ingerencji automatycznych mnożników.

---

## 13. Wielowarstwowa ochrona terenu i mechanizm resetu

* **Ochrona granic przed mechanikami gry:**
  * Tłoki nie mogą wypychać ani wciągać bloków przez granicę parceli.
  * Płyny (woda/lawa) nie rozlewają się poza działkę ani do jej wnętrza.
  * Eksplozje (TNT, creepery, wither) nie niszczą klocków na chronionych działkach.
  * Ochrona przed ogniem, stojaków na zbroje, ramek na przedmioty i wylewania wiader.
* **Transakcyjny reset:**
  * W momencie wygaśnięcia lub porzucenia działka otrzymuje status `RESETTING`.
  * Wszelki dostęp i zakupy zostają zablokowane.
  * Odtwarzanie ze schematu WorldEdit odbywa się partiami z poszanowaniem ticków Paper.
  * Po sukcesie status zmienia się na `AVAILABLE`, a tabliczki aktualizują treść.

---

## 14. Migracje baz danych (Data Migration Engine)

Możesz w dowolnym momencie przenieść wszystkie dane (działki, transakcje, profile, tabliczki) do innego silnika bez utraty informacji:

```text
/plots admin migrate <źródło> <cel>
```
*Dostępne opcje:* `YAML`, `SQLITE`, `MYSQL`.

*Przykład:* `/plots admin migrate SQLITE MYSQL`

Migracja jest wykonywana w tle (asynchronicznie) i generuje pełny raport w konsoli i na czacie. Po zakończeniu wystarczy zmienić parametr `storage.type` w `storage.yml` i zrestartować serwer.

---

## 15. Pełny wykaz komend i uprawnień

### Komendy gracza (`/plots`, aliasy: `/plot`, `/dzialka`)

| Komenda | Uprawnienie | Opis |
| :--- | :--- | :--- |
| `/plots` | `riftplots.user` | Otwiera menu główne GUI. |
| `/plots help` | `riftplots.user.help` | Lista pomocy. |
| `/plots list` | `riftplots.user.list` | Lista Twoich nieruchomości i wolnych lokali. |
| `/plots info [id]` | `riftplots.user.info` | Szczegółowe parametry działki. |
| `/plots buy [id]` | `riftplots.user.buy` | Zakup nieruchomości. |
| `/plots pay [id] [okresy]` | `riftplots.user.pay` | Opłacenie podatku miejskiego. |
| `/plots extend [id] [okresy]` | `riftplots.user.extend` | Przedłużenie ważności z góry. |
| `/plots rent <id> [okresy]` | `riftplots.rent` | Wynajem nieruchomości. |
| `/plots transfer <gracz> <cena> [id]` | `riftplots.sell` | Wysłanie oferty sprzedaży innemu graczowi. |
| `/plots accept [id_oferty]` | `riftplots.user` | Akceptacja oferty zakupu. |
| `/plots deny [id_oferty]` | `riftplots.user` | Odrzucenie oferty zakupu. |
| `/plots auction [list\|bid <id> <kwota>]` | `riftplots.auction` | Udział w licytacjach nieruchomości. |
| `/plots home [id]` | `riftplots.home` | Teleportacja do punktu domowego. |
| `/plots sethome [id]` | `riftplots.sethome` | Ustawienie punktu domowego na działce. |
| `/plots trust <gracz> [id]` | `riftplots.user.trust` | Dodanie współużytkownika. |
| `/plots untrust <gracz> [id]` | `riftplots.user.untrust` | Usunięcie współużytkownika. |
| `/plots members [id]` | `riftplots.user.members` | Lista członków parceli. |
| `/plots abandon [id] [confirm]` | `riftplots.user.abandon` | Porzucenie nieruchomości i zwrot środków. |
| `/plots history [id]` | `riftplots.user.history` | Podgląd historii finansowej. |
| `/plots gui` | `riftplots.user.gui` | Panel graficzny. |

### Komendy administratora (`/plots admin ...`)

| Komenda | Uprawnienie | Opis |
| :--- | :--- | :--- |
| `/plots admin create <id> [nazwa]` | `riftplots.admin.create` | Tworzy działkę z WorldEdit i zapisuje schemat. |
| `/plots admin delete <id>` | `riftplots.admin.delete` | Usuwa działkę, region i tabliczki. |
| `/plots admin select <id>` | `riftplots.admin.select` | Zaznacza obszar działki w WorldEdit. |
| `/plots admin info <id>` | `riftplots.admin.info` | Panel techniczny działki. |
| `/plots admin setprice <id> <cena>` | `riftplots.admin.setprice` | Zmienia cenę zakupu. |
| `/plots admin settax <id> <podatek>` | `riftplots.admin.settax` | Zmienia stawkę podatku bazowego. |
| `/plots admin settaxmode <id> <tryb>` | `riftplots.admin.settaxmode` | Zmienia tryb kalkulacji podatku. |
| `/plots admin setperiod <id> <dni>` | `riftplots.admin.setperiod` | Zmienia okres rozliczeniowy. |
| `/plots admin setgrace <id> <godziny>` | `riftplots.admin.setgrace` | Zmienia czas trwania karencji. |
| `/plots admin setowner <id> <gracz\|none>` | `riftplots.admin.setowner` | Ustawia/usuwa właściciela. |
| `/plots admin district <list\|set\|info>` | `riftplots.admin.district` | Zarządzanie dzielnicami i przypisaniem. |
| `/plots admin type <list\|set\|info>` | `riftplots.admin.type` | Zarządzanie typami nieruchomości. |
| `/plots admin budget <balance\|add\|withdraw\|history>` | `riftplots.admin.budget` | Zarządzanie Skarbem Miasta. |
| `/plots admin rent <offer\|cancel> <id>` | `riftplots.admin` | Konfiguracja wynajmu lokali. |
| `/plots admin auction start <id> ...` | `riftplots.admin` | Wystawienie nieruchomości na licytację. |
| `/plots admin migrate <źródło> <cel>` | `riftplots.admin.migrate` | Migracja bazy danych. |
| `/plots admin reset <id>` | `riftplots.admin.reset` | Wymuszenie resetu klocków ze schematu. |
| `/plots admin snapshot <id>` | `riftplots.admin.snapshot` | Zapisanie nowego schematu ze świata. |
| `/plots admin restore <id>` | `riftplots.admin.restore` | Odtworzenie stanu ze schematu. |
| `/plots admin sign <id> [typ]` | `riftplots.admin.sign` | Powiązanie tabliczki handlowej. |
| `/plots admin unsign` | `riftplots.admin.unsign` | Odpięcie tabliczki handlowej. |
| `/plots admin audit <id>` | `riftplots.admin.audit` | Dziennik audytowy operacji na działce. |
| `/plots admin disable <id>` | `riftplots.admin.disable` | Wyłączenie działki z użytku. |
| `/plots admin enable <id>` | `riftplots.admin.enable` | Włączenie działki do użytku. |
| `/plots admin reload` | `riftplots.admin.reload` | Przeładowanie plików konfiguracyjnych. |
| *Uprawnienie specjalne* | `riftplots.admin.bypass` | Omijanie wszelkich ograniczeń i blokad terenu. |

---

## 16. Opis plików konfiguracyjnych

* **`config.yml`:** Ustawienia prefiksów, symboli waluty, domyślnych stawek, ochrony fizyki (tłoki, płyny, wybuchy, ogień), parametrów porcjowania resetu.
* **`districts.yml`:** Definicje dzielnic miejskich, mnożników cenowych i podatkowych oraz dozwolonych typów nieruchomości.
* **`plot-types.yml`:** Konfiguracja gotowych typów parcel (mieszkalne, komercyjne, przemysłowe, premium itp.).
* **`storage.yml`:** Wybór backendu (YAML, SQLITE, MYSQL) oraz zaawansowane parametry puli połączeń **HikariCP**.
* **`lang.yml`:** Wszystkie komunikaty dla graczy i administratorów w formacie Adventure MiniMessage z obsługą placeholderów `{plot_id}`, `{price}`, `{tax}`, `{player}`, `{time_left}`.

---

**riftPlots** — kompletny, niezawodny silnik gospodarki nieruchomościami miejskimi na Twoim serwerze Minecraft!
