# Szybki start — pierwsza kamienica

[← Instalacja](Instalacja.md) · [Pełny poradnik administratora →](Poradnik-administratora.md)

**Cel:** utworzyć lokal `rynek_1`, przypisać go do centrum, ustalić rozliczenia i udostępnić zakup graczowi.

## 1. Zbuduj gotowy lokal

Przygotuj budynek w stanie, do którego ma wracać po resecie. Usuń z niego przedmioty i modyfikacje, które nie powinny należeć do wzorca. Snapshot obejmuje zapisane bloki i ich dane, więc zawartość przygotowanego lokalu ma znaczenie.

## 2. Zaznacz objętość WorldEdit

```text
//sel cuboid
//wand
```

Zaznacz dwa przeciwległe narożniki. Uwzględnij całą wysokość lokalu, podłogę, sufit i ściany wymagające ochrony. Granice są wliczane do działki: współrzędne `100..109` to dziesięć bloków, nie dziewięć.

Przy osobnych lokalach na piętrach sprawdź wspólną podłogę/sufit. Ten sam blok nie może jednocześnie należeć do dwóch działek.

## 3. Utwórz nieruchomość

```text
/plots admin create rynek_1 Kamienica przy rynku
```

Plugin zapisuje schemat i rejestruje region WorldGuard. Identyfikator musi mieć od 2 do 32 znaków: litery, cyfry, podkreślenia lub myślniki. Nazwa może być czytelnym, wielowyrazowym opisem.

Jeśli zaznaczenie koliduje z działką albo innym regionem WorldGuard, popraw je przed ponowieniem. Nie usuwaj istniejącego regionu tylko po to, by pominąć komunikat kolizji.

## 4. Ustal zasady

```text
/plots admin type set rynek_1 COMMERCIAL
/plots admin district set rynek_1 centrum
/plots admin setprice rynek_1 45000
/plots admin settax rynek_1 3000
/plots admin settaxmode rynek_1 FIXED
/plots admin setperiod rynek_1 7
/plots admin setgrace rynek_1 6
```

| Parametr | Wynik przykładu |
| :--- | :--- |
| Cena zakupu | 45 000 jednostek waluty serwera. |
| Podatek bazowy | 3 000 za okres. |
| Dzielnica | `centrum`, mnożnik podatku `1.25`. |
| Podatek wynikowy | **3 750 co 7 dni**. |
| Karencja | 6 godzin od terminu podatku. |

> Nowe działki dziedziczą parametry typu. Ten przykład ustawia ręczne nadpisania, więc `setprice` jest końcową ceną bez dodatkowego mnożenia przez dzielnicę. Cena dziedziczona uwzględnia mnożnik cen dzielnicy. Dla starszych rekordów `/plots admin type apply-defaults rynek_1` pokazuje zmiany przed potwierdzeniem.

## 5. Dodaj tabliczkę

Spójrz na tabliczkę w odległości do pięciu bloków:

```text
/plots admin sign rynek_1 FOR_SALE
```

Gracz może kupić lokal prawym przyciskiem myszy. Lewy przycisk pokazuje informacje. Jeśli chcesz wyłącznie tabliczkę informacyjną, wybierz `STATUS`.

## 6. Przetestuj jako gracz

Na koncie z odpowiednim saldem:

```text
/plots info rynek_1
/plots buy rynek_1
/plots sethome rynek_1
/plots home rynek_1
```

Sprawdź, czy zmienił się właściciel, pojawił termin podatku i zaktualizowała tabliczka. Następnie sprawdź budowanie na koncie właściciela oraz brak możliwości budowania na obcym koncie bez bypassu.

## Co dalej?

- [Wynajem](Wynajem.md) — przygotuj lokal wynajmowany z kaucją.
- [Podatki](Podatki.md) — wybierz sposób naliczania stawek.
- [Schematy i reset](Schematy-i-reset.md) — sprawdź powrót do stanu pierwotnego.
- [Poradnik gracza](Poradnik-gracza.md) — przekaż mieszkańcom zestaw podstawowych zasad.
