# Komendy i uprawnienia

[← README](../README.md) · [Konfiguracja i obsługa](operations.md)

Główna komenda to `/plots`. Aliasy: `/plot`, `/dzialka`.

`<argument>` jest wymagany, `[argument]` jest opcjonalny. Pominięty identyfikator zwykle oznacza działkę, na której stoisz; `/plots home` wybiera jedną z posiadanych nieruchomości. Dla jednoznacznego działania podawaj identyfikator.

## Gracze

Każda komenda wymaga `riftplots.user` oraz odpowiedniego uprawnienia z tabeli. `riftplots.user` jest domyślnie dostępne i ma poniższe uprawnienia jako dzieci.

| Komenda | Uprawnienie | Działanie |
| :--- | :--- | :--- |
| `/plots` | `riftplots.user.gui` | Otwiera główne GUI. |
| `/plots help` | `riftplots.user.help` | Wyświetla pomoc. |
| `/plots list` | `riftplots.user.list` | Gracz: GUI własnych działek. Konsola: lista wszystkich. |
| `/plots gui` | `riftplots.user.gui` | Otwiera GUI własnych działek. |
| `/plots info [id]` | `riftplots.user.info` | Informacje o nieruchomości. |
| `/plots buy [id]` | `riftplots.user.buy` | Kupuje dostępną działkę. |
| `/plots pay [id] [okresy]` | `riftplots.user.pay` | Opłaca jeden lub kilka okresów podatkowych. |
| `/plots extend [id] [okresy]` | `riftplots.user.extend` | Wywołuje ten sam mechanizm opłacania podatku. |
| `/plots history [id\|all] [strona] [legacy]` | `riftplots.user.history` | Prywatna historia operacji i stan zwrotów; `legacy` pokazuje starsze transakcje. |
| `/plots notifications [strona]` | `riftplots.user.history` | Zapisane wiadomości i potwierdzenia wysłania przez serwer. |
| `/plots border [id]` | `riftplots.border` | Tymczasowy obrys 3D widoczny tylko dla gracza. |
| `/plots trust <gracz> [id]` | `riftplots.user.trust` | Dodaje współużytkownika. |
| `/plots untrust <gracz> [id]` | `riftplots.user.untrust` | Usuwa współużytkownika; nie usuwa aktywnego najemcy. |
| `/plots members [id]` | `riftplots.user.members` | Lista współużytkowników. |
| `/plots abandon [id] [confirm]` | `riftplots.user.abandon` | Prosi o potwierdzenie; z `confirm` oddaje nieruchomość. |
| `/plots rent <id> [okresy] [on\|off]` | `riftplots.rent` | Wynajmuje lokal; domyślnie jeden okres i wyłączone odnowienie. |
| `/plots rent info <id>` | `riftplots.rent` | Czynsz, kaucja, koniec i stan odnowienia umowy. |
| `/plots rent autorenew <id> on\|off` | `riftplots.rent` | Zmienia automatyczne odnowienie bez skracania opłaconego czasu. |
| `/plots rent cancel <id> [confirm]` | `riftplots.rent` | Pokazuje zasady, a po potwierdzeniu kończy własny najem. |
| `/plots rent offer <id> <czynsz> <jednostka> <kaucja> confirm` | `riftplots.rent.offer` | Oferta własnego lokalu; uprawnienie trzeba nadać jawnie. |
| `/plots transfer <gracz> <cena> [id]` | `riftplots.sell` | Wysyła ofertę sprzedaży online; cena `0` oznacza przekazanie. |
| `/plots accept [id_oferty]` | `riftplots.user` | Akceptuje ofertę; bez ID wybiera jedną z otrzymanych ofert. |
| `/plots deny <id_oferty>` | `riftplots.user` | Kupujący odrzuca ofertę, a sprzedający może ją anulować. |
| `/plots auction [list]` | `riftplots.auction` | Lista aktywnych aukcji. |
| `/plots auction bid <id> <kwota>` | `riftplots.auction` | Składa ofertę w aukcji. |
| `/plots auction history <id> [strona]` | `riftplots.auction` | Trwała historia przebić, także po zakończeniu. |
| `/plots auction buy <id> confirm` | `riftplots.auction` | Kup teraz w aktywnej aukcji. |
| `/plots home [id]` | `riftplots.home` | Teleportuje właściciela lub współużytkownika na działkę. |
| `/plots sethome [id]` | `riftplots.sethome` | Właściciel zapisuje punkt domowy wewnątrz granic działki. |

### Dostęp do nieruchomości

| Rola | Domyślny zakres |
| :--- | :--- |
| Właściciel | Wszystkie uprawnienia działki. |
| `MEMBER` dodany przez `trust` | `INTERACT`, `DOORS`, `CONTAINERS`, `TELEPORT`. |
| `TENANT` utworzony przy wynajmie | `BUILD`, `DESTROY`, `CONTAINERS`, `DOORS`, `INTERACT`, `TELEPORT`. |
| `MANAGER` w modelu/API | Wszystkie uprawnienia działki. Komenda `trust` nie nadaje tej roli. |

Panel członków udostępnia edytor roli `MEMBER`/`MANAGER` i pojedynczych praw dla właściciela lub bypass z `riftplots.members.edit`. Dawne listy praw pozostają bez zmian. Najem kończy się przez mechanizm umów; edytor nie zmienia praw `TENANT`.

## Administracja

Dispatcher wymaga `riftplots.admin` oraz uprawnienia konkretnej komendy. Węzeł `riftplots.admin` ma domyślną wartość `op` i zawiera administracyjne uprawnienia jako dzieci. Wejście przez główną komendę wymaga również `riftplots.user`.

| Komenda | Uprawnienie | Działanie |
| :--- | :--- | :--- |
| `/plots admin` lub `/plots admin help` | `riftplots.admin` | Pomoc administracyjna. |
| `/plots admin create <id> [nazwa]` | `riftplots.admin.create` | Zapisuje schemat i rejestruje działkę z zaznaczenia WorldEdit. |
| `/plots admin delete <id>` | `riftplots.admin.delete` | Usuwa rekord działki, region i powiązania tabliczek; nie przywraca bloków. |
| `/plots admin select <id>` | `riftplots.admin.select` | Ustawia zaznaczenie WorldEdit; gracz musi być w świecie działki. |
| `/plots admin info <id>` | `riftplots.admin.info` | Parametry techniczne i rozliczeniowe. |
| `/plots admin list` | `riftplots.admin.list` | Wszystkie działki i ich stany. |
| `/plots admin setprice <id> <cena>` | `riftplots.admin.setprice` | Cena bezpośredniego zakupu. |
| `/plots admin settax <id> <stawka>` | `riftplots.admin.settax` | Bazowa stawka podatku. |
| `/plots admin settaxmode <id> <tryb>` | `riftplots.admin.settaxmode` | Tryb kalkulacji podatku. |
| `/plots admin setperiod <id> <dni>` | `riftplots.admin.setperiod` | Długość okresu podatkowego, co najmniej jeden dzień. |
| `/plots admin setgrace <id> <godziny>` | `riftplots.admin.setgrace` | Czas karencji; `0` wyłącza dodatkowy czas. |
| `/plots admin setowner <id> <gracz\|none>` | `riftplots.admin.setowner` | Administracyjnie ustawia lub usuwa właściciela. |
| `/plots admin snapshot <id>` | `riftplots.admin.snapshot` | Zastępuje pierwotny schemat obecnym stanem budynku. |
| `/plots admin reset <id> [confirm]` | `riftplots.admin.reset` | Pokazuje podgląd, po potwierdzeniu oddaje własność i odtwarza bloki. |
| `/plots admin restore <id> [confirm]` | `riftplots.admin.restore` | Ten sam mechanizm podglądu i resetu. |
| `/plots admin sign <id> [FOR_SALE\|STATUS\|RENEW]` | `riftplots.admin.sign` | Wiąże tabliczkę wskazywaną wzrokiem, do pięciu bloków. |
| `/plots admin unsign` | `riftplots.admin.unsign` | Odpina wskazywaną tabliczkę. |
| `/plots admin audit <id> [strona]` | `riftplots.admin.audit` | Audyt operacyjny ze zmianami stanu oraz transakcje. |
| `/plots admin health [reset <id>]` | `riftplots.admin.health` | Diagnostyka bez zmiany danych i podgląd schematu. |
| `/plots admin transactions …` | `riftplots.admin.transactions` | Oczekujące operacje, zwroty i ręczne uzgodnienie; [pełna składnia](wiki/Nowosci-P1-P2.md). |
| `/plots admin disable <id>` | `riftplots.admin.disable` | Wyłącza działkę bez aktywnego resetu, najmu i aukcji. |
| `/plots admin enable <id>` | `riftplots.admin.enable` | Włącza wyłączoną działkę; nie przerywa resetu. |
| `/plots admin reload` | `riftplots.admin.reload` | Odświeża konfigurację, wiadomości, typy, dzielnice i tabliczki. |

`reset` i `restore` są operacjami oddania nieruchomości do ponownego użytku. Nie służą do naprawienia budynku z zachowaniem właściciela. Zmiana właściciela, usunięcie i wyłączenie są blokowane podczas kolidujących operacji najmu, aukcji lub resetu.

### Typy i dzielnice

| Komenda | Uprawnienie |
| :--- | :--- |
| `/plots admin type list` | `riftplots.admin.type` |
| `/plots admin type info <typ>` | `riftplots.admin.type` |
| `/plots admin type set <id> <typ>` | `riftplots.admin.type` |
| `/plots admin type apply-defaults <id> [confirm]` | `riftplots.admin.type` |
| `/plots admin district list` | `riftplots.admin.district` |
| `/plots admin district info <dzielnica>` | `riftplots.admin.district` |
| `/plots admin district set <id> <dzielnica\|none>` | `riftplots.admin.district` |

`type set` zmienia typ; dostępne działki aktualizują parametry dziedziczone. Indywidualne nadpisania i warunki posiadanych nieruchomości pozostają zachowane. `apply-defaults` pokazuje zmiany i wymaga potwierdzenia. Dzielnice są przypisaniem logicznym — nie tworzą dodatkowych regionów.

### Wynajem, aukcje i finanse miasta

| Komenda | Uprawnienie |
| :--- | :--- |
| `/plots admin rent offer <id> <czynsz> <HOUR\|DAY\|WEEK\|MONTH> [kaucja]` | `riftplots.admin` |
| `/plots admin rent cancel <id>` | `riftplots.admin` |
| `/plots admin auction start <id> <cena_startowa> <przebicie> <minuty> [kup_teraz]` | `riftplots.admin` |
| `/plots admin auction cancel <id> confirm <powód>` | `riftplots.admin` |
| `/plots admin budget balance` | `riftplots.admin.budget` |
| `/plots admin budget history` | `riftplots.admin.budget` |
| `/plots admin budget add <kwota> [powód]` | `riftplots.admin.budget` |
| `/plots admin budget withdraw <kwota> [powód]` | `riftplots.admin.budget` |
| `/plots admin migrate <YAML\|SQLITE\|MYSQL> <YAML\|SQLITE\|MYSQL>` | `riftplots.admin.migrate` |

Ofertę najmu wystawia administrator lub właściciel z `riftplots.rent.offer`. Aukcję rozpoczyna administrator. Przy aukcji nieruchomości gracza odbiorcą wypłaty jest jej obecny właściciel; nieruchomość bez właściciela sprzedaje miasto. `budget add` i `withdraw` są korektami wewnętrznej księgi miasta — nie przesyłają pieniędzy na konto administratora w Vault.

## Uprawnienie specjalne

`riftplots.admin.bypass` omija ochronę terenu, weryfikację własności w wybranych operacjach oraz koszt i czas odnowienia teleportacji. Nie jest ogólnym wyłączeniem zabezpieczeń płatności, stanów resetu czy wzajemnie wykluczających się ofert.

Definicje uprawnień i aliasów: [`plugin.yml`](../src/main/resources/plugin.yml). Składnia pochodzi z implementacji komend w [`command/`](../src/main/java/pl/riftplots/command).
