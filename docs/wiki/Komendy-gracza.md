# Komendy gracza

[← Strona główna](Home.md) · [Poradnik gracza](Poradnik-gracza.md) · [Komendy administratora](Komendy-administratora.md)


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
