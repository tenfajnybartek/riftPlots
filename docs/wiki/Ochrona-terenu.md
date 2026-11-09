# Ochrona terenu

[← Strona główna](Home.md) · [Typy i dzielnice](Dzielnice-i-typy.md) · [Schematy i reset](Schematy-i-reset.md)

## Granice są trójwymiarowe

Ochrona dotyczy obszaru pomiędzy minimalnymi i maksymalnymi współrzędnymi X, Y i Z. Wszystkie graniczne bloki należą do działki. Zaznaczenie samej podłogi nie zapewnia ochrony całej wysokości domu.

```text
Przekrój budynku — przykład rozdzielenia lokali

Y = 79  ┌────────────────────────────┐
        │       mieszkanie_2         │
Y = 72  └────────────────────────────┘
Y = 71  ┌────────────────────────────┐
        │          sklep_1           │
Y = 64  └────────────────────────────┘

Ten sam blok nie należy do obu nieruchomości.
```

Przykład pokazuje zasady granic, nie automatyczny generator mieszkań. Administrator sam wydziela lokale zaznaczeniem WorldEdit.

## Kto ma dostęp?

| Osoba / stan | Domyślne zachowanie |
| :--- | :--- |
| Właściciel | Pełne uprawnienia działki. |
| Współużytkownik `MEMBER` | Interakcje, drzwi i pojemniki. |
| Najemca `TENANT` | Dodatkowo budowanie i niszczenie na czas umowy. |
| Osoba obca | Brak prawa modyfikacji chronionego lokalu. |
| Lokal `RESETTING` lub `DISABLED` | Dostęp zwykłych graczy jest blokowany. |
| Działka dostępna bez najmu | Nie można jej modyfikować jak własnego terenu. |
| Uprawniony administrator z bypass | Może omijać kontrolę ochrony terenu. |

Lokal miejski może nie mieć właściciela, a mimo to mieć aktywnego najemcę. Wtedy uprawnienia najemcy wynikają z umowy i nie są odrzucane tylko z powodu stanu `AVAILABLE`.

## Warstwy ochrony

WorldGuard przechowuje region oraz właściciela i członków. Listenery riftPlots uzupełniają ochronę o sprawdzanie praw do konkretnych czynności i wybrane mechaniki graniczne.

Obsługiwane ścieżki obejmują stawianie i niszczenie bloków, pojemniki, drzwi, interakcje, wiadra, wybrane pojazdy, stojaki na zbroje i ramki. Dodatkowe przełączniki:

```yaml
protection:
  prevent-piston-border-crossing: true
  prevent-fluid-border-crossing: true
  prevent-explosions: true
  prevent-fire-spread: true
  protect-armor-stands: true
  protect-item-frames: true
```

Tłoki sprawdzają źródło i miejsce docelowe przesuwanych bloków. Przepływ wody i lawy pomiędzy różnymi działkami lub działką a otoczeniem może być blokowany. Przy ochronie wybuchów bloki wewnątrz działek są usuwane z listy niszczonych bloków zdarzenia.

Te mechanizmy nie są deklaracją obsługi każdej nowej mechaniki Minecrafta ani wszystkich modyfikacji wprowadzanych przez inne pluginy.

## Jak testować ochronę?

1. Przygotuj dwa sąsiadujące lokale oraz dwa lokale na różnych wysokościach.
2. Użyj konta właściciela, konta współużytkownika i obcego konta.
3. Konto testujące blokadę nie może mieć `riftplots.admin.bypass`.
4. Sprawdź budowanie, niszczenie, drzwi, skrzynie oraz granice płynów i tłoków.
5. Oddzielnie sprawdź zachowanie w czasie resetu i po zmianie właściciela.

Przy konflikcie z WorldGuard sprawdź, czy nie działa dodatkowy nakładający się region albo bypass innego pluginu. Nie wyłączaj od razu całej ochrony — najpierw ustal dokładne współrzędne i oczekiwane prawo dostępu.
