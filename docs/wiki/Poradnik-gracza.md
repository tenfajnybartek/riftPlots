# Poradnik gracza

[← Strona główna](Home.md) · [Wszystkie komendy gracza →](Komendy-gracza.md)

## Znajdź swój lokal

Wpisz `/plots`, aby otworzyć rynek z zakładkami **Moje**, **Dostępne**, **Wynajem** i **Aukcje**. Strony mieszczą po 45 lokali; dolny pasek zawiera sortowanie i filtry typu oraz dzielnicy. Możesz też spotkać tabliczkę w mieście lub otrzymać ID lokalu od administracji.

```text
/plots info rynek_1
```

Przed zakupem sprawdź cenę, podatek, długość okresu i status. Cena zakupu jest jednorazowa; podatek dotyczy kolejnych terminów korzystania z własności.

`/plots border rynek_1` pokazuje przez krótki czas granice 3D lokalu tylko Tobie. Podgląd można uruchomić również z okna potwierdzenia zakupu.

## Kup nieruchomość

```text
/plots buy rynek_1
```

Możesz użyć również GUI lub tabliczki `FOR_SALE`. Zakup wymaga wystarczających środków i dostępnej nieruchomości. Aktywna aukcja, oferta najmu, limit dzielnicy albo zakaz sprzedaży mogą blokować zakup mimo statusu `AVAILABLE`.

Po sukcesie otrzymujesz własność i termin następnego podatku. Jeśli provider ekonomii odrzuci pobranie pieniędzy, zakup nie powinien zostać przyznany.

## Ustaw dom

Stań w bezpiecznym miejscu wewnątrz lokalu:

```text
/plots sethome rynek_1
/plots home rynek_1
```

Domyślnie powrót kosztuje 50 jednostek waluty i ma 10 sekund odnowienia. Serwer może ustawić inne wartości. Teleportacja jest blokowana do lokalu wyłączonego albo resetowanego. [Jak działa bezpieczne miejsce?](Teleportacja.md)

## Zaproś znajomą osobę

```text
/plots trust Przyjaciel rynek_1
/plots members rynek_1
/plots untrust Przyjaciel rynek_1
```

**Domyślny współużytkownik może otwierać pojemniki, korzystać z drzwi i wykonywać interakcje. Nie dostaje prawa stawiania ani niszczenia bloków.** Nie zakładaj, że `trust` oznacza identyczne prawa jak właściciel.

Typ nieruchomości może ograniczać liczbę współużytkowników. Aktywnego najemcy nie usuwa się komendą `untrust` — jego dostęp wynika z umowy.

Właściciel może zmieniać rolę i pojedyncze prawa w panelu członków z `riftplots.members.edit`. Nowi członkowie mają też `TELEPORT`; starsze zapisane listy praw pozostają zachowane. Rola `MANAGER` daje pełne prawa działki, a uprawnienia `TENANT` wynikają z umowy i nie podlegają tej edycji.

## Pilnuj podatku

```text
/plots info rynek_1
/plots pay rynek_1
/plots pay rynek_1 2
```

Pierwszy przykład opłaca jeden okres, drugi dwa. `extend` korzysta z tego samego mechanizmu. Domyślnie można mieć opłacone do czterech dodatkowych okresów z góry.

Po terminie rozpoczyna się karencja. Po jej bezskutecznym upływie możesz stracić nieruchomość, a budynek może zostać przywrócony do wzorca. Nie licz na restart jako przedłużenie czasu. [Szczegóły i przykłady obliczeń](Podatki.md).

## Wynajem zamiast zakupu

```text
/plots rent apartament_4 7
```

Najemca nie staje się właścicielem. Otrzymuje na czas umowy również budowanie i niszczenie. Pierwsza opłata obejmuje czynsz za wskazaną liczbę okresów oraz kaucję. Auto-odnowienie jest domyślnie wyłączone; wybierasz je w GUI albo przez `rent autorenew <id> on|off`. `rent info <id>` pokazuje umowę, a `rent cancel <id>` wyświetla zasady wcześniejszego zakończenia przed potwierdzeniem. [Warunki najmu](Wynajem.md).

## Sprzedaż, aukcja lub oddanie

| Chcę… | Co zrobić |
| :--- | :--- |
| Sprzedać lokal konkretnej osobie | [Wyślij ofertę transferu](Transfery.md). |
| Kupić lokal w licytacji | [Sprawdź aktywne aukcje](Aukcje.md). |
| Oddać lokal miastu | Użyj `/plots abandon rynek_1`, przeczytaj kwotę zwrotu i potwierdź przez `/plots abandon rynek_1 confirm`. |

Oddanie może uruchomić reset i usunąć Twoje zmiany w budynku. Zwykłego porzucenia nie wykonasz przy aktywnym najmie lub aukcji. Zwrot jest liczony od zapisanej ceny działki i jej procentu zwrotu — nie od wartości wszystkich włożonych materiałów ani ceny zapłaconej na rynku wtórnym.

`/plots history all [strona]` pokazuje Twoje operacje i stan zwrotów. `/plots notifications [strona]` otwiera zapisane wiadomości. Historia cudzej działki jest domyślnie prywatna. Jeśli widzisz błąd ekonomii, zapisz ID operacji, ID lokalu i czas; administracja może sprawdzić trwały dziennik.
