# Stan funkcji i ograniczenia

[← Strona główna](Home.md) · [FAQ](FAQ.md) · [Rozwój i testy](Rozwoj-i-testy.md)

Dokumentacja opisuje implementację riftPlots 1.0.0 po przeglądzie z 9–10 listopada 2026 r. Poniższe rozróżnienie pomaga planować konfigurację bez zakładania funkcji, które są jedynie polami modelu lub pomysłem na dalszy rozwój.

## Zaimplementowane ścieżki

| Obszar | Zakres |
| :--- | :--- |
| Rejestracja | Zaznaczenie WorldEdit, granice 3D, schemat i region WorldGuard. |
| Zakup | Komenda, GUI i tabliczka; kontrola dostępności i reguł nabycia. |
| Podatki | Sześć modeli, terminy, karencja i przedpłaty. |
| Najem | Oferta administracyjna, czynsz, kaucja i auto-odnowienie. |
| Rynek wtórny | Oferta do wskazanego gracza i akceptacja lub odrzucenie. |
| Aukcje | Przebicia, końcowe okno odpowiedzi i próg kup teraz. |
| Reset | Wzorzec bloków, porcjowanie, kontrola wymiarów i ponowienie po restarcie. |
| Obsługa | Wewnętrzny budżet, transakcje, audyt i trzy backendy. |
| Migracja | Kopia danych repozytoriów i zapisanych usług pomiędzy backendami. |

## Dziedziczenie parametrów jest częściowe

Typy mają domyślne ceny, podatki i inne metadane, ale `type set` nie kopiuje ich na działkę. Mnożnik cen dzielnicy nie jest używany w bezpośrednim zakupie. Pola flag i ikon typu nie sterują automatycznie regionem i GUI.

Działające reguły sprzedaży, najmu, dozwolonych typów i limitów są opisane w [rozdziale o dzielnicach](Dzielnice-i-typy.md). Ceny i rozliczenia działki konfiguruj jawnie.

## Finanse nie są jedną transakcją atomową

Odpowiedzi Vault są sprawdzane i istnieją ścieżki refundacji, ale provider ekonomii, zapis własności i logi nie uczestniczą w jednej wspólnej transakcji. Awaria bazy lub procesu pomiędzy etapami może wymagać ręcznego uzgodnienia. Nie ma trwałego dziennika pozwalającego automatycznie odtworzyć każdą przerwaną operację.

## Co znika po restarcie?

Oczekujące oferty transferu i kolejka powiadomień offline są w pamięci. Aukcje zachowują aktualną kwotę i licytującego, lecz nie pełną historię wcześniejszych przebić z modelu. Te różnice nie powinny być opisywane jako trwała historia wszystkich wydarzeń.

## MySQL nie oznacza synchronizacji sieci

Każdy proces utrzymuje własny cache. Brakuje rozproszonych blokad i uzgadniania zmian. Wspólna baza sama w sobie nie jest obsługą kilku serwerów zarządzających tymi samymi nieruchomościami.

## Interfejs i operacje

- GUI ma ograniczoną liczbę widocznych pozycji i nie posiada jeszcze paginacji.
- Nie wszystkie teksty pochodzą z `lang.yml`.
- Gracz nie ma komendy wyłączenia auto-odnowienia najmu ani samodzielnego zakończenia umowy.
- Komendy aukcji nie udostępniają wystawiania przez gracza i osobnego anulowania.
- Zdarzenia API nie pokrywają każdej zmiany statusu i własności.
- Większość I/O oraz migracja są synchroniczne; duże miasta wymagają pomiaru obciążenia.

## Zakres sprawdzenia

Kompilacja, budowa JAR-a i 53 testy przechodziły podczas wskazanej weryfikacji. YAML i SQLite były sprawdzane rzeczywistymi operacjami. Testy nie obejmowały pełnego serwera Minecraft ani zewnętrznego MySQL. Ochrona fizyki, wklejanie w docelowym świecie i wydajność wymagają próby integracyjnej.

[Przewodnik testowania](Rozwoj-i-testy.md) wskazuje, jak uzupełnić sprawdzenie przed uruchomieniem miasta.
