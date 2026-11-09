# Komendy gracza

[← Strona główna](Home.md) · [Poradnik gracza](Poradnik-gracza.md) · [Komendy administratora](Komendy-administratora.md)

Główna komenda: `/plots`. Aliasy: `/plot`, `/dzialka`. `<argument>` jest wymagany, `[argument]` opcjonalny. Gdy ID można pominąć, najczęściej wybierana jest działka pod graczem; `/plots home` wybiera jedną z posiadanych. Podawaj ID, aby uniknąć niejednoznaczności.

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
| `/plots history [id]` | `riftplots.user.history` | Historia finansowa działki; bez dopasowania działki — własna historia gracza. |
| `/plots trust <gracz> [id]` | `riftplots.user.trust` | Dodaje współużytkownika. |
| `/plots untrust <gracz> [id]` | `riftplots.user.untrust` | Usuwa współużytkownika; nie usuwa aktywnego najemcy. |
| `/plots members [id]` | `riftplots.user.members` | Lista współużytkowników. |
| `/plots abandon [id] [confirm]` | `riftplots.user.abandon` | Prosi o potwierdzenie; z `confirm` oddaje nieruchomość. |
| `/plots rent <id> [okresy]` | `riftplots.rent` | Wynajmuje lokal, domyślnie na jeden okres, z automatycznym odnowieniem. |
| `/plots transfer <gracz> <cena> [id]` | `riftplots.sell` | Wysyła ofertę sprzedaży online; cena `0` oznacza przekazanie. |
| `/plots accept [id_oferty]` | `riftplots.user` | Akceptuje ofertę; bez ID wybiera jedną z otrzymanych ofert. |
| `/plots deny <id_oferty>` | `riftplots.user` | Kupujący odrzuca ofertę, a sprzedający może ją anulować. |
| `/plots auction [list]` | `riftplots.auction` | Lista aktywnych aukcji. |
| `/plots auction bid <id> <kwota>` | `riftplots.auction` | Składa ofertę w aukcji. |
| `/plots home [id]` | `riftplots.home` | Teleportuje właściciela lub współużytkownika na działkę. |
| `/plots sethome [id]` | `riftplots.sethome` | Właściciel zapisuje punkt domowy wewnątrz granic działki. |

### Dostęp do nieruchomości

| Rola | Domyślny zakres |
| :--- | :--- |
| Właściciel | Wszystkie uprawnienia działki. |
| `MEMBER` dodany przez `trust` | `INTERACT`, `DOORS`, `CONTAINERS`. |
| `TENANT` utworzony przy wynajmie | `BUILD`, `DESTROY`, `CONTAINERS`, `DOORS`, `INTERACT`. |
| `MANAGER` w modelu/API | Wszystkie uprawnienia działki. Komenda `trust` nie nadaje tej roli. |

Komendy nie udostępniają edytora pojedynczych uprawnień. Zakres członka można zmieniać przez model/API. Najem kończy się przez mechanizm umów, a nie usunięcie główki najemcy w GUI.
