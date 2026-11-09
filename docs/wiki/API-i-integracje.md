# API i integracje

[← Strona główna](Home.md) · [Rozwój i testy](Rozwoj-i-testy.md) · [Stan funkcji](Stan-funkcji.md)

## Integracje wymagane przez plugin

| Integracja | Odpowiedzialność |
| :--- | :--- |
| `VaultHook` | Dostęp do sald, weryfikacja środków, pobieranie i wypłaty providera. |
| `WorldEditHook` | Zaznaczenia gracza, kopiowanie i odczyt schematów. |
| `WorldGuardHook` | Tworzenie regionu, synchronizacja właścicieli/członków i kontrola kolizji. |

HikariCP i sterowniki SQL są bibliotekami wynikowego JAR-a, nie oddzielnymi pluginami serwera. Dodatkowych integracji takich jak PlaceholderAPI czy mapa internetowa nie należy zakładać bez osobnej implementacji.

## Zdarzenia Bukkit

Zdarzenia znajdują się w pakiecie `pl.riftplots.api`.

| Zdarzenie | Moment | Dostępna kontrola |
| :--- | :--- | :--- |
| `PlotBuyEvent` | Przed pobraniem ceny bezpośredniego zakupu przez `PaymentService`. | Anulowanie i zmiana ceny. |
| `PlotTaxPayEvent` | Przed pobraniem podatku. | Anulowanie i zmiana kwoty. |
| `PlotExpireEvent` | Przed obsługą utraty własności za zaległość. | Anulowanie wygaśnięcia. |
| `PlotResetStartEvent` | Przed odebraniem własności w resecie. | Anulowanie rozpoczęcia. |
| `PlotStateChangeEvent` | Podczas kończenia resetu. | Zmiana stanu docelowego; anulowanie pozostawia działkę wyłączoną. |
| `PlotResetCompleteEvent` | Po zapisaniu wyniku resetu. | Odczyt wyniku; zdarzenie nie jest anulowalne. |

> `PlotStateChangeEvent` nie jest globalnym strumieniem wszystkich zmian stanu. Transfer i wygrana aukcja nie emitują `PlotBuyEvent`. Nie buduj pełnej historii własności wyłącznie na założeniu, że te zdarzenia obejmują wszystkie ścieżki.

## Przykład nasłuchiwania

```java
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import pl.riftplots.api.PlotBuyEvent;

public final class PropertyListener implements Listener {
    @EventHandler
    public void onDirectPurchase(PlotBuyEvent event) {
        if (event.getPlot().getId().equals("ratusz_1")) {
            event.setCancelled(true);
        }
    }
}
```

Dodatek musi skompilować się z odpowiednim API pluginu, zarejestrować listener i zapewnić właściwą kolejność ładowania, np. przez zależność od `riftPlots`. Nazwa w zależności pluginu to `riftPlots`, nie pakiet Java.

Cena lub kwota zmieniana w zdarzeniu płatności musi być nieujemna i różna od `null`. Zdarzenie bezpośredniego zakupu nie jest miejscem do obsługi rozliczenia każdej aukcji.

## Model i serwisy

`Plot` przechowuje dane domeny i indywidualne uprawnienia członków. `PlotManager` zapewnia cache, odczyt działek, walidację nabycia i ścieżkę zapisu aktualizującą profile, tabliczki oraz WorldGuard.

Omijanie serwisów przez samodzielne ustawienie `ownerUuid` i zapis SQL nie wykonuje całej operacji biznesowej: może pominąć ekonomię, członków, terminy i powiadomienia. Przed rozszerzeniem konkretnej ścieżki przeczytaj jej implementację.

Operacje wykorzystujące Bukkit i świat wykonuj na wątku serwera. Użycie `ConcurrentHashMap` wewnątrz części usług nie nadaje wszystkim metodom bezpieczeństwa pracy z dowolnego wątku.

## Nawigacja po źródłach

- [Zdarzenia](https://github.com/tenfajnybartek/riftPlots/tree/master/src/main/java/pl/riftplots/api)
- [Integracje](https://github.com/tenfajnybartek/riftPlots/tree/master/src/main/java/pl/riftplots/integration)
- [Model nieruchomości](https://github.com/tenfajnybartek/riftPlots/tree/master/src/main/java/pl/riftplots/domain)
- [Usługi](https://github.com/tenfajnybartek/riftPlots/tree/master/src/main/java/pl/riftplots/service)

Nie ma tu deklaracji stabilnego wersjonowanego SDK ani opublikowanego artefaktu API w zewnętrznym repozytorium Maven. Dopasuj rozszerzenie do wersji pluginu i sprawdź integrację na serwerze testowym.
