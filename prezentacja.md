# riftPlots — scenariusz prezentacji na YouTube

> Materiał roboczy do nagrania prezentacji riftPlots 1.0.0 z P1–P2 i edytowalną konfiguracją. Scenariusz przygotowany 10 listopada 2026 r. Pokazuje dostępne funkcje; nie zastępuje odbioru produkcyjnego.

[README](README.md) · [NOWOŚCI](NOWOSCI.md) · [Plan testów](roadmap_test.md) · [Komendy](docs/commands.md) · [Konfiguracja GUI](docs/wiki/Konfiguracja-GUI.md)

## 1. Pomysł na film

**Główna historia:** administrator przygotowuje lokal, gracz ogląda jego granice i kupuje go, zaprasza drugą osobę, a później pokazujemy inne nieruchomości przeznaczone na najem, sprzedaż oraz aukcję. Na końcu administrator odtwarza wzorzec budynku i zmienia wygląd GUI bez kompilowania pluginu.

Najpierw pokaż efekt w grze, dopiero później pliki i narzędzia administracyjne. Film powinien odpowiadać na trzy pytania widza: co może zrobić gracz, jak przygotowuje to administrator i jak dopasować system do własnego miasta.

Proponowany format główny: **około 26 minut**. Z tych samych nagrań można zmontować wersję 8–10 minut i zwiastun 45–60 sekund.

### Wiadomości, które widz powinien zapamiętać

1. Nieruchomość jest bryłą 3D, więc można wydzielać mieszkania na różnych piętrach.
2. Gracze mają zakup, najem, rynek wtórny, aukcje i panel dostępu.
3. Administrator ustala typy, dzielnice, ceny, podatki i wzorzec do resetu.
4. Wiadomości, parametry oraz wygląd siedmiu ekranów można dopasować przez YAML.
5. Historia, dziennik operacji, zwroty i backup pomagają obsługiwać serwer oraz rozpoznawać problemy.

## 2. Jak uczciwie opisać stan pluginu

Bezpieczna zapowiedź do filmu:

> „To riftPlots: system miejskich nieruchomości z zakupem, wynajmem, aukcjami i ochroną lokali na różnych piętrach. Pokażę działanie gracza, przygotowanie miasta oraz konfigurację interfejsu.”

Jeżeli nagrywasz przed końcowym odbiorem serwera, dodaj:

> „Pokazuję aktualną wersję testową. Przed wdrożeniem na własny serwer trzeba sprawdzić ją z używaną ekonomią i pozostałymi pluginami.”

| Można pokazać/powiedzieć | Co doprecyzować |
| :--- | :--- |
| „Działki i lokale mają granice 3D”. | Same budynki przygotowuje administrator; plugin nie generuje miasta. |
| „Wygląd menu i wiadomości można edytować”. | Akcje, identyfikatory techniczne i format danych mają stałe znaczenie. |
| „Są trwałe operacje finansowe i zwroty”. | Vault i baza nie tworzą jednej transakcji; wynik niepewny wymaga uzgodnienia. |
| „Obsługiwane są YAML, SQLite i MySQL”. | Wspólna baza nie synchronizuje kilku równocześnie zapisujących serwerów. |
| „Mamy testy automatyczne i próbę Paper”. | Podawaj wyniki dotyczące nagrywanego artefaktu. Wcześniejszy zapis to 119 testów Java, 6 testów backupu i 54 sprawdzenia Paper. |
| „Paginacja i indeks pomagają przy większym mieście”. | Wynik benchmarku wyszukiwania nie jest obietnicą konkretnego TPS lub liczby graczy. |

Nie pokazuj jako gotowych funkcji punktu P3: placeholderów zewnętrznych, integracji mapy i eksportu statystyk miasta. Nie używaj obietnic „zero bugów”, „pieniądze nigdy nie zginą” lub „każdy serwer bez lagów”.

## 3. Przygotowanie przed nagrywaniem

### 3.1. Oddzielne środowisko nagraniowe

- [ ] Kopia demonstracyjnego świata z poprawnie działającymi Paper, WorldEdit, WorldGuard, Vault i faktycznym providerem ekonomii.
- [ ] Sprawdzony JAR, zapisany numer wersji i hash.
- [ ] Osobny serwer do nagrania; bez produkcyjnej ekonomii i przypadkowych graczy.
- [ ] Gotowa kopia stanu przed pierwszym ujęciem i punkt kontrolny przed resetem.
- [ ] Brak pluginu `RiftPlotsProbe` w środowisku ręcznego nagrania — sonda automatyczna wykonuje własne operacje i zatrzymuje serwer.
- [ ] Zaliczone najważniejsze przypadki z [roadmap_test.md](roadmap_test.md): zakup, dostęp, najem, transfer, aukcja, reset i edycja GUI.

### 3.2. Konta i perspektywy

| Konto | Rola w nagraniu |
| :--- | :--- |
| `DemoAdmin` | Tworzy nieruchomości, zmienia ustawienia, uruchamia aukcję i reset. |
| `Alicja` | Główna perspektywa gracza: zakup, członkowie, podatek, sprzedaż. |
| `Bartek` | Najemca, kupujący w transferze i drugi licytujący. |
| `Kuba` | Obcy gracz do pokazania ochrony bez uprawnień. |

Alicja, Bartek i Kuba mają zwykłe konta, bez OP i bypassów. Ochrona pokazana z konta operatora nie jest wiarygodnym przykładem doświadczenia zwykłego gracza.

Przygotuj salda narzędziem używanego pluginu ekonomii, np. po 100 000 dla Alicji i Bartka. Zanotuj salda na początku każdej sceny. riftPlots nie definiuje uniwersalnej komendy `/eco`; nie dopisuj jej do poradnika instalacji jako własnej funkcji pluginu.

### 3.3. Miasto do pokazania

Przygotuj mały, estetyczny fragment miasta: rynek, kamienicę, sklep, mieszkanie na piętrze i nieco większą posesję do aukcji. Widoczne wejścia, chodniki i opisane lokale pomagają zrozumieć, co dokładnie kupuje gracz.

| ID | Zastosowanie | Przygotowanie |
| :--- | :--- | :--- |
| `film_dom` | Zakup, panel właściciela, prawa, podatek i home. | Dostępny, 10 000, `FIXED`, podatek 200 co 7 dni. |
| `film_parter` / `film_pietro` | Oddzielne nieruchomości nad sobą. | Te same X/Z, rozłączne Y; przygotowana własność na dwóch kontach. |
| `film_najem` | Najem miejski. | Bez właściciela; czynsz 250 `DAY`, kaucja 500. |
| `film_transfer` | Sprzedaż Alicja → Bartek. | Własność Alicji; brak najmu i aukcji, cena transakcji 12 000. |
| `film_aukcja` | Licytacja i kup teraz. | Miejski lokal bez właściciela; start 1 000, krok 100, kup teraz 1 500. |
| `film_reset` | Odtworzenie budynku ze wzorca. | Osobny lokal, gotowy schemat, własność Alicji i zaplanowane zmiany do cofnięcia. |
| `film_typ` | Typ i dzielnica. | Dostępny `RESIDENTIAL`, bez ręcznej ceny, do przypisania `centrum`. |
| `film_gui` | Wygląd potwierdzenia zakupu. | Dostępny lokal do oglądania; nie kupuj go przed sceną zmiany GUI. |

Oddzielne ID pozwalają nagrać sceny niezależnie. Nie próbuj sprzedać lokalu w trakcie aktywnego najmu ani rozpoczynać aukcji na nieruchomości zajętej inną operacją. Nie przypisuj wszystkich lokali do `centrum`: jego domyślny limit dwóch własności na gracza może zatrzymać pokaz.

### 3.4. Ustawienia obrazu i dźwięku — propozycja

- Nagrywaj w czytelnym 16:9, np. 1920 × 1080, ze stabilnym klatkażem odpowiednim dla komputera.
- Ustaw taką skalę GUI, aby lore i cena były czytelne w podglądzie gotowego filmu.
- Zostaw interfejs przy czynnościach gracza; do przebitek miasta nagraj osobne ujęcia bez HUD.
- Muzyka powinna być wyraźnie cichsza od głosu. Pozostaw słyszalny krótki dźwięk potwierdzenia transakcji.
- Wyłącz zbędne powiadomienia i ukryj prywatne IP, dane połączenia oraz hasła w ujęciach konfiguracji.
- Ustal pogodę, porę dnia i teksturę świata na całą sesję. Zmiana światła między ujęciami utrudnia montaż „przed i po”.

## 4. Plan rozdziałów filmu głównego

Poniższe czasy są planem montażowym. Przed publikacją dopasuj timestampy do faktycznej długości materiału.

| Czas | Segment | Najważniejszy obraz |
| :--- | :--- | :--- |
| 00:00–00:30 | Efekt na start | Granice 3D → zakup → aukcja → reset „przed/po”. |
| 00:30–01:30 | Czym jest riftPlots | Miasto i krótka lista zastosowań. |
| 01:30–04:00 | Przygotowanie nieruchomości | Zaznaczenie WorldEdit, rejestracja, cena i typ/dzielnica. |
| 04:00–05:30 | Rynek i granice 3D | Filtry, cena, obrys parteru i piętra. |
| 05:30–07:30 | Zakup i ochrona | Potwierdzenie ceny, nowy właściciel, próba obcego gracza. |
| 07:30–09:30 | Członkowie i role | Dodanie Bartka i zmiana jednego prawa. |
| 09:30–11:30 | Podatek i teleport | Kwota, termin, wpłata i powrót do domu. |
| 11:30–14:30 | Wynajem | Czynsz + kaucja, umowa, odnowienie i zakończenie. |
| 14:30–16:30 | Sprzedaż graczowi | Oferta, akceptacja i prowizja. |
| 16:30–19:30 | Aukcje | Przebicia dwóch osób, historia i kup teraz. |
| 19:30–21:30 | Reset budynku | Wzorzec, zmiany gracza, podgląd i odtworzenie. |
| 21:30–24:00 | Własny wygląd | Edycja `gui.yml` i `lang.yml`, reload, rezultat. |
| 24:00–25:30 | Narzędzia administracji | Historia, audyt, health, operacje i kopie. |
| 25:30–26:00 | Zakończenie | Link do repozytorium/Wiki i najważniejsze zastosowania. |

## 5. Scenariusz ujęcie po ujęciu

### Scena 1 — pierwsze 30 sekund

**Cel:** pokazać działający efekt, zanim pojawi się dłuższe wyjaśnienie.

Przygotuj cztery ujęcia po około 4–6 sekund:

1. Kamera obchodzi budynek, widać obrys 3D lokalu.
2. Gracz otwiera potwierdzenie zakupu z ceną i podatkiem.
3. Na aukcji pojawia się nowe przebicie i historia ofert.
4. Zmieniony lokal wraca do zapisanego wyglądu po resecie.

Propozycja narracji:

> „Mieszkania na osobnych piętrach, zakup z panelu, wynajem z kaucją i własny rynek aukcyjny. Tak wygląda riftPlots. Za chwilę pokażę cały przepływ — od przygotowania budynku do obsługi gracza.”

Na ekranie wystarczą krótkie napisy: **LOKALE 3D**, **ZAKUP I NAJEM**, **AUKCJE**, **RESET ZE WZORCA**. Nie zasłaniaj nimi kwoty i przycisku potwierdzenia.

### Scena 2 — czym jest system

**Perspektywa:** spokojny spacer po rynku i dwa krótkie wejścia do lokali.

Narracja:

> „Administrator przygotowuje budynki i ustala zasady, a gracze wybierają nieruchomości z rynku. Każdy lokal ma swoje granice, cenę i rozliczenia. Można przygotować dom, sklep, magazyn albo osobne mieszkanie w kamienicy.”

Krótko podaj wymagania: Paper, WorldEdit, WorldGuard, Vault i działający plugin ekonomii. Szczegóły wersji skieruj do Wiki; ekran nie musi być slajdem z całą listą zależności.

**Ujęcie dodatkowe:** wejście na piętro i spojrzenie w dół na drugi lokal. To naturalne przejście do granic 3D.

### Scena 3 — administrator tworzy lokal

**Perspektywa:** `DemoAdmin`. Wcześniej gotowy budynek; nie buduj go od zera podczas prezentacji.

1. Zaznacz siekierką WorldEdit dwa narożniki bryły, razem z wysokością.
2. Pokaż rejestrację i podstawowe ustawienia:

```text
/plots admin create film_dom Dom przy rynku
/plots admin setprice film_dom 10000
/plots admin settax film_dom 200
/plots admin settaxmode film_dom FIXED
/plots admin setperiod film_dom 7
/plots admin info film_dom
```

Jeśli `film_dom` przygotowano wcześniej, tę scenę nagraj na kopii punktu startowego albo z osobnym nowym ID. Nie próbuj ponownie tworzyć już zarejestrowanego lokalu.

Narracja:

> „Zaznaczam pełną bryłę nieruchomości. Podczas rejestracji zapisywany jest jej wzorzec, a potem ustawiam cenę i podatek. Tutaj zakup kosztuje dziesięć tysięcy, a podatek dwieście za siedem dni.”

**Krótki dodatek o typach i dzielnicach** — użyj oddzielnego `film_typ`, bez ręcznego `setprice`:

```text
/plots admin type info RESIDENTIAL
/plots admin district info centrum
/plots admin district set film_typ centrum
/plots admin info film_typ
```

Narracja:

> „Typ daje domyślne parametry, a dzielnica może zmieniać końcową cenę i podatek. Przy domyślnej działce mieszkalnej za dwadzieścia pięć tysięcy i mnożniku centrum 1,5 cena dziedziczona wynosi trzydzieści siedem i pół tysiąca. Ręczna cena pozostaje osobnym nadpisaniem.”

**Przebitka:** ikony dwóch typów w rynku. Nie rozwijaj wszystkich sześciu modeli podatku w tym segmencie; zostaw to do osobnego poradnika.

### Scena 4 — rynek i podgląd 3D

**Perspektywa:** Alicja, bez uprawnień administracyjnych.

```text
/plots
/plots border film_parter
/plots border film_pietro
```

W menu przejdź z domyślnej zakładki „Moje” do „Dostępne”. Pokaż „Wynajem” i „Aukcje”, sortowanie oraz jeden filtr. Do ujęcia paginacji przygotuj więcej wpisów albo wcześniej zmniejsz liczbę `content-slots`; nie pozostawiaj nieaktywnej strzałki jako przykładu działającej drugiej strony.

Narracja:

> „Gracz przegląda nieruchomości w panelu, filtruje je i sprawdza koszt. Obrys pokazuje także wysokość — tutaj parter i mieszkanie nad nim mają osobne granice.”

Nagraj obrysy kolejno z odpowiedniej pozycji kamery. Są widoczne dla proszącego gracza, więc konto stojące obok nie musi widzieć jego cząsteczek.

**Koniec sceny:** kamera wraca pod wejście do `film_dom`.

### Scena 5 — zakup i ochrona

**Perspektywa:** Alicja; przełączenie na Kubę do próby dostępu.

1. Otwórz `film_dom` przez rynek.
2. Zatrzymaj kursor na potwierdzeniu na 2–3 sekundy, aby było widać cenę, podatek i okres.
3. Potwierdź zakup.
4. Pokaż `/plots info film_dom` i saldo z faktycznego providera ekonomii.
5. Kuba próbuje postawić blok i otworzyć skrzynię.
6. Alicja wykonuje dozwoloną czynność we własnym lokalu.

Narracja:

> „Przed zakupem widzę cenę i cykliczny podatek. Po potwierdzeniu lokal trafia do moich nieruchomości. Obca osoba nie może swobodnie przebudować wnętrza ani korzystać z pojemników.”

Przy saldzie początkowym 100 000 i cenie 10 000 pokaż zmianę do 90 000, jeśli w tej kopii nie wykonywano wcześniej innych obciążeń.

**Ważna uwaga do prowadzącego:** `/plots buy film_dom` wykonuje zakup od razu, bez osobnego argumentu `confirm`. Do tego ujęcia użyj GUI. Nie uruchamiaj komendy kupna przed nagraniem okna potwierdzenia.

### Scena 6 — współużytkownicy i prawa

**Perspektywa:** Alicja jako właścicielka i Bartek jako członek.

```text
/plots trust Bartek film_dom
/plots members film_dom
```

Właścicielka otwiera panel nieruchomości, przechodzi do członków i edytora Bartka. Przy domyślnych prawach Bartek otwiera drzwi lub pojemnik, ale nie buduje. Włącz mu `BUILD`, pokaż skuteczne postawienie bloku, po czym odbierz to prawo.

Narracja:

> „Dodanie współużytkownika nie musi oznaczać pełnego dostępu. Domyślny członek ma podstawowe interakcje, a właściciel może osobno nadać budowanie, niszczenie i pozostałe prawa. Rola managera daje szerszy zakres.”

Managera pokaż krótko tylko wtedy, gdy przed nagraniem sprawdzono jego zachowanie. Podkreśl, że aktywny najemca jest obsługiwany przez umowę, a nie usuwany zwykłym kliknięciem listy członków.

Opcjonalne zamknięcie sceny:

```text
/plots untrust Bartek film_dom
```

Pokaż odebranie dostępu. Jeśli Bartek ma pozostać członkiem w dalszej części, pomiń tę komendę i zachowaj spójność stanu.

### Scena 7 — podatek, historia i powrót do domu

**Perspektywa:** Alicja.

```text
/plots info film_dom
/plots pay film_dom 1
/plots history film_dom
/plots sethome film_dom
/plots home film_dom
```

`sethome` wykonaj wewnątrz nieruchomości, następnie odejdź i dopiero użyj `home`. Pokaż datę przed wpłatą i po niej oraz wiersz z kwotą podatku.

Narracja:

> „Podatek jest liczony za okres nieruchomości. Mogę opłacić kolejny okres i zobaczyć nowy termin oraz wpis w historii. Właściciel ustawia też punkt powrotu do swojego lokalu.”

W tej konfiguracji wpłata to 200 za jeden okres. Teleport domyślnie kosztuje dodatkowo 50 i ma cooldown — pokaż koszt albo wcześniej świadomie ustaw zero na serwerze demonstracyjnym i powiedz, że to konfiguracja filmu.

Nie czekaj w głównym nagraniu siedmiu dni na karencję. Jeżeli chcesz pokazać zaległość i automatyczny reset, przygotuj osobny klip testowy z kontrolowaną datą i wyraźnie podpisz przyspieszenie. Nie przedstawiaj nieistniejącej komendy ustawiania podatku „za minutę”.

### Scena 8 — wynajem z kaucją

**Perspektywa:** krótko administrator, potem Bartek.

Przygotowanie oferty miejskiej:

```text
/plots admin rent offer film_najem 250 DAY 500
```

Bartek otwiera zakładkę „Wynajem” i okno potwierdzenia. Pokaż czynsz, kaucję, łączny koszt i przełącznik auto-odnowienia. Dla głównego pokazu wynajmij z GUI na domyślną liczbę okresów **albo** użyj osobnego ujęcia komendy na siedem okresów:

```text
/plots rent film_najem 7 off
/plots rent info film_najem
/plots rent autorenew film_najem on
/plots rent autorenew film_najem off
```

To alternatywne ścieżki zawarcia jednej umowy. Nie potwierdzaj najpierw najmu w GUI, a potem drugi raz nie próbuj wynająć tego samego lokalu komendą.

Narracja dla wariantu siedmiu okresów:

> „W tym lokalu dzień kosztuje dwieście pięćdziesiąt, a kaucja pięćset. Siedem dni to tysiąc siedemset pięćdziesiąt czynszu i pięćset kaucji, razem dwa tysiące dwieście pięćdziesiąt. Najemca może sprawdzić umowę i świadomie włączyć odnowienie.”

Dla wariantu GUI z domyślnym jednym okresem powiedz odpowiednio **250 + 500 = 750**. Liczba w narracji ma odpowiadać faktycznie nagranemu potwierdzeniu.

Pokaż dostęp Bartka do wnętrza, a następnie zasady rozwiązania umowy:

```text
/plots rent cancel film_najem
/plots rent cancel film_najem confirm
```

Narracja:

> „Przed zakończeniem widzę zasady. Po potwierdzeniu tracę dostęp. Kaucja jest rozliczana osobno, a opłacony czynsz nie jest zwracany.”

Jeżeli wypłata jest w kolejce, pokaż jej stan i dograj zaksięgowany zwrot później. Nie montuj komunikatu o oczekującym zwrocie jako dowodu wykonanej wypłaty.

**Opcjonalna przebitka:** właściciel z osobnym `riftplots.rent.offer` wystawia swój lokal. Pokaż to na innym ID i powiedz o wymaganym uprawnieniu.

### Scena 9 — sprzedaż między graczami

**Perspektywa:** Alicja wystawia, Bartek akceptuje. Użyj `film_transfer` będącego własnością Alicji.

```text
# Alicja
/plots transfer Bartek 12000 film_transfer
# Bartek — ID z otrzymanej wiadomości
/plots accept <id_oferty>
```

1. Zatrzymaj ujęcie na kwocie i identyfikatorze oferty.
2. Przełącz na odbiorcę i zaakceptuj właściwą ofertę.
3. Pokaż nowego właściciela oraz różnice sald.
4. Na chwilę wyświetl księgę miasta lub audyt.

Narracja:

> „Właściciel może wysłać ofertę do konkretnego gracza. Przy cenie dwanaście tysięcy i prowizji pięć procent kupujący płaci dwanaście tysięcy, sprzedający otrzymuje jedenaście tysięcy czterysta, a sześćset trafia do księgi miasta.”

Kwoty są przykładem z konfiguracji; nie nazywaj wewnętrznego budżetu miasta kontem administratora w Vault.

Odrzucenie oferty pokaż najwyżej w krótkiej dodatkowej przebitce. Nie wysyłaj kilkunastu ofert jednocześnie — widz powinien rozumieć, którą akceptujesz.

### Scena 10 — aukcja i kup teraz

**Perspektywa:** administrator rozpoczyna, Alicja i Bartek licytują.

```text
# DemoAdmin
/plots admin auction start film_aukcja 1000 100 5 1500
# Alicja
/plots auction bid film_aukcja 1000
# Bartek
/plots auction bid film_aukcja 1100
# Alicja
/plots auction bid film_aukcja 1200
/plots auction history film_aukcja
# Bartek
/plots auction buy film_aukcja confirm
```

Pokaż aukcję także w GUI: minimalna oferta, termin, historia i oddzielny przycisk kup teraz. Możesz zastąpić końcową komendę kliknięciem GUI; nie wykonuj obu dróg jako dwóch zakupów.

Narracja:

> „Aukcja ma cenę startową, minimalne przebicie i opcjonalne kup teraz. W historii widać kolejne oferty, a po natychmiastowym zakupie lokal otrzymuje zwycięzca.”

Domyślnie escrow jest wyłączone: kwota zostaje pobrana przy rozliczeniu. Powiedz to zgodnie z faktyczną konfiguracją nagrania.

#### Opcjonalny dodatek: escrow

Nagraj oddzielną aukcję na innym ID, po włączeniu `auction.escrow-enabled: true` przed jej utworzeniem. Pokaż rezerwację pierwszej oferty, zwrot przebitemu graczowi i dopłatę różnicy przy podnoszeniu własnej oferty.

Propozycja narracji:

> „Opcjonalnie nowe aukcje mogą rezerwować środki już podczas licytowania. Przebity gracz otrzymuje zwrot, a lider dopłaca różnicę. Ten tryb jest zapisywany w konkretnej aukcji.”

Nie przełączaj opcji podczas trwającej aukcji z oczekiwaniem, że zmieni jej model rozliczenia. Anti-snipe najlepiej pokazać krótkim podpisanym klipem z widocznym terminem przed i po ofercie.

### Scena 11 — reset do zapisanego wzorca

**Perspektywa:** najpierw Alicja zmienia wnętrze `film_reset`, potem `DemoAdmin`.

Przygotuj porównanie z identycznej pozycji kamery: wzorzec → kilka widocznych zmian → odtworzenie. Użyj ograniczonej przebudowy, np. innej podłogi, dwóch zniszczonych ścian i dodanego wyposażenia.

```text
/plots admin health reset film_reset
/plots admin reset film_reset
/plots admin reset film_reset confirm
```

Narracja:

> „Wzorzec był zapisany przy przygotowaniu nieruchomości. Administrator najpierw sprawdza reset, widzi jego skutki i dopiero go potwierdza. Budynek wraca do wzorca i może trafić do ponownego użytku.”

Dodaj czytelny napis: **RESET ODBIERA WŁASNOŚĆ**. Pokaż wynik `/plots info film_reset`, aby widz nie uznał tego za kosmetyczną naprawę z zachowaniem właściciela.

Jeśli reset trwa dłużej, przyspiesz montaż z podpisem „nagranie przyspieszone”. Nie obiecuj natychmiastowego odtworzenia dowolnie dużego schematu.

Nie używaj `snapshot` już po zniszczeniu wnętrza, jeśli chcesz przywrócić wcześniejszy wzorzec — zastąpiłbyś go stanem uszkodzonym. `restore` ma tę samą zasadniczą ścieżkę resetu, a `delete` nie odtwarza budynku.

### Scena 12 — własne GUI i wiadomości

**Cel:** mocny wizualny efekt „przed i po” bez wielominutowego przewijania plików.

1. Nagraj domyślne potwierdzenie zakupu `film_gui`, bez kupowania.
2. Zrób kopię `gui.yml` i `lang.yml`.
3. W istniejącej sekcji `menus.buy` zmień wybrane pola, np.:

```yaml
menus:
  buy:
    title: '<gold><bold>Wybierz swój nowy dom</bold></gold>'
    filler:
      enabled: true
      material: BLACK_STAINED_GLASS_PANE
      name: ' '
    buttons:
      confirm:
        slot: 5
        material: DIAMOND
        glow: true
        name: '<green>Kup {plot_name}</green>'
        lore:
          - '<gray>Cena: <gold>{price}</gold></gray>'
          - '<gray>Podatek: {tax} co {period} dni</gray>'
          - '<yellow>Twoja nieruchomość w naszym mieście.</yellow>'
```

To fragment pokazujący zmieniane pola. Edytuj istniejącą sekcję pliku, zachowując pozostałe przyciski; nie dopisuj drugiego głównego `menus`. Slot 5 nie koliduje z domyślnym `cancel` na 15 i `border` na 22 przy rozmiarze 27.

4. W `lang.yml` zmień np. wiadomość zakupu, zachowując placeholdery:

```yaml
plot:
  buy-success: '{prefix}<green>Witaj w nowym domu: <gold>{name}</gold>! Cena: {price}.</green>'
```

5. Uruchom:

```text
/plots admin reload
```

6. Otwórz menu ponownie i pokaż różnicę: tytuł, obramowanie, diamentowy przycisk w nowym miejscu i nowe lore.
7. Jeśli chcesz pokazać nowy komunikat sukcesu, dopiero teraz kup `film_gui` zwykłym kontem i sprawdź jego saldo. Możesz też zakończyć scenę na samym podglądzie GUI.

Narracja:

> „Wygląd tego ekranu pochodzi z gui.yml. Mogę zmienić tytuł, materiał, opis i położenie przycisku. Jego akcja nadal jest zakupem, a plugin sprawdza aktualną wycenę. Wiadomości zmieniam osobno w lang.yml, a parametry działania w config.yml.”

Przez kilka sekund pokaż inne sekcje: dźwięki, cząsteczki, układ tabliczek i domyślne prawa członków. Nie deklaruj możliwości dopisania dowolnej nowej komendy przez samo stworzenie przycisku w YAML.

### Scena 13 — narzędzia administratora

**Perspektywa:** `DemoAdmin`. Skup się na tym, do czego służy narzędzie, zamiast czytać każdą kolumnę.

```text
/plots admin health
/plots admin audit film_dom
/plots admin budget balance
/plots admin budget history
/plots admin transactions pending
/plots admin transactions refunds
```

Narracja:

> „Administrator ma diagnostykę, audyt zmian i księgę miasta. Operacje finansowe mają identyfikatory, a nieudany zwrot może pozostać trwałym zobowiązaniem do rozliczenia. Dzięki temu problem da się sprawdzić zamiast zgadywać po samym czacie.”

Pusty widok `pending` podpisz „brak operacji wymagających uzgodnienia”. Nie twórz awarii prawdziwej ekonomii tylko po to, żeby zapełnić ekran filmu. Kontrolowany przykład odzyskiwania finansów lepiej nagrać jako osobny poradnik.

Pokaż także `/plots notifications` z konta gracza, jeżeli przygotowano prawdziwe powiadomienie offline. Nie nazywaj potwierdzenia wysłania dowodem przeczytania wiadomości przez klienta.

**Przebitka plików:** katalogi danych i schematów oraz wynik weryfikacji testowej kopii. Wyjaśnij, że backup ma obejmować świat i ekonomię, nie tylko bazę działek.

### Scena 14 — zakończenie

**Obraz:** powolne ujęcie miasta; opcjonalnie logo z repozytorium.

Propozycja narracji:

> „Tak wygląda riftPlots w praktyce: osobne lokale, zakup i najem, handel między graczami, aukcje oraz panel administracyjny. Link do kodu i dokumentacji zostawiam w opisie. W kolejnym materiale mogę przejść przez pełną konfigurację miasta albo testy wdrożenia.”

Jeśli projekt nie ma jeszcze publicznie gotowego wydania, podaj link do źródeł zamiast obiecywać gotowy instalator lub opublikowany release. Źródła Wiki w repo i zdalna zakładka Wiki mogą wymagać oddzielnej aktualizacji.

## 6. Skrócona lista komend dla prowadzącego

Tabela służy do wyszukiwania podczas nagrania. Nie wykonuj całej kolumny mechanicznie na jednym koncie i w jednym stanie świata.

| Fragment | Konto | Komenda / czynność |
| :--- | :--- | :--- |
| Nowy lokal | DemoAdmin | `/plots admin create film_dom Dom przy rynku` po zaznaczeniu WorldEdit. |
| Parametry | DemoAdmin | `/plots admin setprice film_dom 10000`, `/plots admin settax film_dom 200`, `/plots admin settaxmode film_dom FIXED`, `/plots admin setperiod film_dom 7`. |
| Rynek | Alicja | `/plots`, zakładka „Dostępne”, potwierdzenie GUI. |
| Bryła | Alicja | `/plots border film_parter`, następnie `film_pietro`. |
| Współużytkownik | Alicja | `/plots trust Bartek film_dom`, panel członków. |
| Podatek | Alicja | `/plots pay film_dom 1`. |
| Dom | Alicja | `/plots sethome film_dom`, odejście, `/plots home film_dom`. |
| Oferta najmu | DemoAdmin | `/plots admin rent offer film_najem 250 DAY 500`. |
| Najem | Bartek | GUI **albo** `/plots rent film_najem 7 off`. |
| Umowa | Bartek | `/plots rent info film_najem`, `autorenew … on/off`, `cancel … confirm`. |
| Sprzedaż | Alicja → Bartek | `/plots transfer Bartek 12000 film_transfer`, następnie `/plots accept <id_oferty>`. |
| Aukcja | DemoAdmin | `/plots admin auction start film_aukcja 1000 100 5 1500`. |
| Licytacja | Alicja / Bartek | `/plots auction bid film_aukcja <kwota>`. |
| Kup teraz | Bartek | GUI **albo** `/plots auction buy film_aukcja confirm`. |
| Reset | DemoAdmin | `/plots admin reset film_reset`, następnie z `confirm`. |
| Konfiguracja | DemoAdmin | `/plots admin reload`, ponowne otwarcie menu. |

Pełne sekwencje znajdują się w scenach powyżej. `<id_oferty>` i `<kwota>` zastąp rzeczywistymi wartościami; oznaczenie `on/off` opisuje wybór jednego argumentu, a nie dosłowny tekst komendy.

## 7. B-roll i montaż

Przydatne ujęcia dodatkowe, każde po około 5–10 sekund:

- [ ] Powolny przejazd kamerą przez rynek i wejście do kamienicy.
- [ ] Parter i piętro z zaznaczeniem, że są odrębnymi lokalami.
- [ ] Obrys widziany z narożnika, aby pokazać długość, szerokość i wysokość.
- [ ] Kursor nad ceną i podatkiem w lore.
- [ ] Przejście pomiędzy czterema zakładkami rynku.
- [ ] Zmiana pojedynczego prawa członka, potem ta sama czynność w świecie.
- [ ] Umowa z widoczną kaucją i stanem auto-odnowienia.
- [ ] Historia przebić dwóch kont.
- [ ] Ten sam kadr wnętrza przed zmianami, po zmianach i po resecie.
- [ ] GUI domyślne i zmienione przez YAML, ujęcie z tej samej perspektywy.
- [ ] Trzy pliki `config.yml`, `lang.yml`, `gui.yml` pokazane obok krótkich etykiet ich zastosowania.
- [ ] Logo, tło miasta i odnośnik do dokumentacji.

Przy zmianie konta dodaj podpis roli: „administrator”, „właściciel”, „najemca”, „obcy gracz”. Przyspieszone lub dograne ujęcia podpisz, jeśli mogłyby sugerować inny czas działania funkcji.

Po wykonaniu ważnej operacji pozostaw na ekranie wynik przez kilka sekund. Ruch kamery, otwarcie GUI i szybkie przewinięcie czatu wykonane naraz utrudniają odczytanie ceny oraz komunikatu.

## 8. Wariant 8–10 minut

Jeżeli pełny materiał jest za długi, zachowaj taki układ:

| Czas orientacyjny | Treść |
| :--- | :--- |
| 00:00–00:20 | Efekt: granice, zakup, reset. |
| 00:20–01:00 | Zastosowania i zależności. |
| 01:00–02:00 | Przygotowanie jednego lokalu i granice pięter. |
| 02:00–03:30 | Zakup, ochrona i edytor jednego prawa. |
| 03:30–05:00 | Najem, kaucja i kontrola odnowienia. |
| 05:00–06:30 | Transfer i aukcja w krótkich przebitkach. |
| 06:30–07:30 | Reset z porównaniem. |
| 07:30–09:00 | Edycja GUI/języka oraz narzędzia administracyjne. |
| 09:00–09:30 | Repozytorium, Wiki i zakończenie. |

Pełne poradniki o podatkach, escrow, migracji i odzyskiwaniu po awarii przenieś do kolejnych materiałów. Wersja skrócona powinna nadal pokazać realny koszt i wynik operacji, nie sam montaż otwieranych menu.

## 9. Zwiastun 45–60 sekund

Propozycja kolejności:

1. **0–5 s:** miasto i tytuł „Twoje miasto. Własne nieruchomości.”
2. **5–12 s:** dwa lokale nad sobą i granice 3D.
3. **12–20 s:** rynek → cena → potwierdzenie zakupu.
4. **20–28 s:** wynajem z widoczną kaucją.
5. **28–36 s:** dwa przebicia aukcji i kup teraz.
6. **36–45 s:** reset w porównaniu przed/po.
7. **45–55 s:** edycja wyglądu GUI i efekt.
8. **55–60 s:** logo, repozytorium, pełna prezentacja.

Krótka narracja:

> „Przygotuj miasto, wydziel mieszkania i oddaj graczom rynek nieruchomości. Zakup, wynajem z kaucją, aukcje i dostęp dla znajomych. Do tego granice 3D, reset ze wzorca i interfejs dopasowany do serwera. To riftPlots.”

## 10. Tytuł i miniatura

Gotowa miniatura w stylu oprawy GitHuba: [JPG 3840 × 2160](docs/assets/youtube/riftplots-thumbnail-4k.jpg) · [PNG 3840 × 2160](docs/assets/youtube/riftplots-thumbnail-4k.png). [Opis plików, oryginał generacji i prompt](docs/assets/youtube/README.md).

Propozycje tytułu:

1. **riftPlots — działki 3D, wynajem i aukcje | Prezentacja pluginu**
2. **Mieszkania na piętrach w Minecraft? Pokazuję riftPlots**
3. **Miejski rynek nieruchomości — zakup, najem i GUI w riftPlots**

Pomysł na miniaturę:

- W tle jedna czytelna kamienica, nie cały rozległy serwer.
- Na pierwszym planie dwa wyraźnie obrysowane lokale na różnych piętrach.
- Logo riftPlots w rogu i krótki napis, np. **DZIAŁKI 3D** lub **WŁASNE MIASTO**.
- Opcjonalnie mały fragment GUI, o ile pozostaje czytelny przy małym rozmiarze.

Materiały z repozytorium: [logo](docs/assets/riftplots-logo.png), [banner](docs/assets/riftplots-banner.png), [grafika miasta](docs/wiki/assets/city-guide.png). Do demonstracji funkcji najlepiej użyć również rzeczywistego kadru z nagrania.

## 11. Opis filmu — szablon do uzupełnienia

```text
riftPlots to system miejskich nieruchomości dla Minecraft: lokale z granicami 3D,
zakup, wynajem z kaucją, sprzedaż między graczami, aukcje i panel dostępu.

W filmie pokazuję:
• przygotowanie lokalu przez administratora,
• rynek, zakup i ochronę,
• członków, podatki i teleportację,
• wynajem, transfer oraz aukcje,
• reset budynku i edycję wyglądu GUI.

Repozytorium: https://github.com/tenfajnybartek/riftPlots
Wiki: https://github.com/tenfajnybartek/riftPlots/wiki

Nagrywana wersja / commit: [UZUPEŁNIJ]
Paper / Java: [UZUPEŁNIJ]
WorldEdit / WorldGuard / Vault / ekonomia: [UZUPEŁNIJ]

Wymagane są WorldEdit, WorldGuard, Vault i działający provider ekonomii.
Przed wdrożeniem sprawdź zgodność ze swoim zestawem pluginów i wykonaj kopię danych.

Rozdziały — po montażu zastąp rzeczywistymi timestampami:
00:00 Prezentacja riftPlots
00:30 Co potrafi system
01:30 Przygotowanie nieruchomości
04:00 Rynek i granice 3D
05:30 Zakup i ochrona
07:30 Członkowie i prawa
09:30 Podatki i home
11:30 Wynajem z kaucją
14:30 Sprzedaż graczowi
16:30 Aukcje
19:30 Reset budynku
21:30 Własne GUI i wiadomości
24:00 Narzędzia administracji
25:30 Dokumentacja i zakończenie
```

Przed publikacją sprawdź, czy link do Wiki prowadzi do wersji odpowiadającej filmowi, a deklarowane wydanie jest faktycznie dostępne. Jeśli materiał jest wersją testową, dopisz to w opisie i na początku nagrania.

## 12. Co zostawić do oddzielnych poradników

| Odcinek | Zawartość |
| :--- | :--- |
| Instalacja od zera | Zależności, provider ekonomii, pierwszy start, konfiguracja SQLite/MySQL. |
| Budowa miasta | Bryły, piętra, typy, dzielnice, dziedziczenie i indywidualne ceny. |
| Ekonomia serwera | Tryby podatku, okresy, prowizje, najem i kasa miasta. |
| Aukcje i escrow | Rezerwacje, dopłata różnicy, przebicie i zwroty. |
| Własny styl | `lang.yml`, siedem menu `gui.yml`, tabliczki, dźwięki i cząsteczki. |
| Administracja po awarii | `health`, dziennik, uzgodnienia, backup i faktyczne odtworzenie. |
| Test przed wdrożeniem | Praktyczny przebieg wybranych przypadków z [roadmap_test.md](roadmap_test.md). |

## 13. Typowe problemy podczas nagrania

| Objaw | Najpierw sprawdź |
| :--- | :--- |
| Rynek jest pusty | Wybraną zakładkę, filtry i stan lokali; „Moje” nie pokazuje wszystkich dostępnych ofert. |
| Nie da się kupić | Saldo, stan najmu/aukcji, typ, dzielnicę, limit własności i oczekującą operację. |
| Właściciel może robić wszystko mimo odebrania prawa | Czy testujesz zwykłym kontem bez OP i bypassów. |
| Drugi gracz nie widzi obrysu | Podgląd cząsteczek dotyczy proszącego gracza. |
| Kwota najmu nie zgadza się z narracją | Liczbę okresów: GUI domyślnie 1, przykład komendy 7; dolicz kaucję. |
| Aukcja trwa dłużej niż zaplanowano | Anti-snipe; do pokazu natychmiastowego finału użyj kup teraz. |
| Nowe GUI się nie pojawiło | Wynik reloadu, błędy slotów/materiałów i ponowne otwarcie ekwipunku. |
| Reset odtworzył zmienione wnętrze | Czy snapshot nie został nadpisany już po przebudowie. |
| Kaucja jeszcze nie jest na koncie | Stan zwrotu i rzeczywiste zaksięgowanie; nie wymuszaj nieuzgodnionego powtórzenia wypłaty. |
| Kolejne podejście do sceny ma inny wynik | Stan po poprzednim ujęciu, salda, właściciela i aktywne oferty; wróć do kopii punktu startowego. |

## 14. Checklista przed i po montażu

### Przed nagrywaniem

- [ ] Jeden sprawdzony JAR i zapisane wersje zależności.
- [ ] Zwykłe konta przetestowane bez bypassu.
- [ ] Wszystkie lokale z tabeli mają prawidłowy, niekolidujący stan.
- [ ] Schemat `film_reset` zweryfikowany przed zmianami gracza.
- [ ] Salda i prowizje odpowiadają liczbom w narracji.
- [ ] Pełna kopia umożliwia powtórzenie ujęć.
- [ ] Wybrane warianty: najem GUI albo komendą; zwykła aukcja albo osobny dodatek escrow.
- [ ] Własny fragment GUI przygotowany i sprawdzony wcześniej.

### Przed publikacją

- [ ] Każda pokazywana funkcja rzeczywiście działała w nagraniu; brak makiet udających wynik transakcji.
- [ ] Zmiana perspektywy i przyspieszenie są czytelne.
- [ ] Ceny, prowizje, kaucja i salda nie przeczą sobie między ujęciami.
- [ ] Reset jest opisany jako operacja odbierająca własność.
- [ ] Wyniki testów mają właściwy zakres i nie są obietnicą bezbłędności.
- [ ] P3 nie występuje jako zrealizowana funkcja.
- [ ] Czytelny głos, GUI i kluczowe komunikaty.
- [ ] Prawdziwe timestampy zamiast orientacyjnego planu.
- [ ] Działające linki do właściwego repozytorium, dokumentacji i dostępnego wydania.
- [ ] Opis podaje, czy prezentowana wersja jest testowa czy po odbiorze.

### Po sesji

- [ ] Zachowaj surowe nagrania, konfigurację demo i hash użytego JAR-a.
- [ ] Przywróć ustawienia przyspieszonego czasu i inne parametry filmu przed dalszym użytkowaniem kopii.
- [ ] Zanotuj zauważone problemy jako zgłoszenia według planu testów.
