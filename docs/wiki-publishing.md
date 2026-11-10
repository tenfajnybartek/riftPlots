# Publikacja GitHub Wiki

Źródła wiki znajdują się w [`docs/wiki/`](wiki/Home.md). Zawierają 27 rozdziałów, w tym wdrożenie P1–P2 i konfigurację GUI, menu `_Sidebar.md`, stopkę `_Footer.md`, 3 grafiki i 6 diagramów Mermaid. Pliki źródłowe można czytać bezpośrednio w głównym repozytorium; używają względnych odnośników `.md`.

## Dlaczego osobny eksport?

GitHub Wiki jest osobnym repozytorium `riftPlots.wiki.git`. Samo dodanie katalogu `docs/wiki` do głównego repozytorium nie publikuje zakładki Wiki.

Eksporter zamienia odnośniki do stron na adresy `https://github.com/tenfajnybartek/riftPlots/wiki/<strona>`, a obrazy na adresy plików w repozytorium Wiki. Dzięki temu nie zależą od gałęzi głównego projektu ani katalogów roboczych komputera.

## Weryfikacja i eksport

Potrzebne są Python 3.10+ oraz Git. Skrypty używają biblioteki standardowej Pythona; nie wymagają instalacji pakietów.

```powershell
python scripts/wiki.py check
python scripts/wiki.py export
```

Gotowy pakiet znajdziesz w `build/wiki-export/`. Zawiera wyłącznie strony i obrazy przeznaczone do repozytorium Wiki. Walidacja sprawdza lokalne pliki, odnośniki, kotwice, nawigację, bloki kodu i sygnatury PNG. Nie udaje testu zdalnego renderera GitHub ani działającego serwera Minecraft.

Możesz też użyć:

```powershell
.\scripts\publish-wiki.ps1
```

Bez dodatkowych argumentów skrypt tylko przygotowuje pakiet — nie wykonuje publikacji.

Jeśli PowerShell blokuje uruchamianie lokalnych skryptów, możesz użyć osobnego procesu bez zmiany zasad dla kolejnych sesji:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\publish-wiki.ps1
```

## Pierwsze uruchomienie Wiki na GitHubie

1. Otwórz repozytorium [riftPlots](https://github.com/tenfajnybartek/riftPlots).
2. Jeżeli nie ma zakładki Wiki, włącz ją w **Settings → General → Features → Wikis**.
3. Otwórz [Wiki](https://github.com/tenfajnybartek/riftPlots/wiki), utwórz stronę `Home` i zapisz ją. Może zawierać krótki tekst inicjalny, który zostanie zastąpiony przygotowaną stroną.

GitHub udostępnia klonowanie wiki po zapisaniu pierwszej strony. Obsługę repozytorium oraz nazw `_Sidebar.md` i `_Footer.md` opisuje oficjalna dokumentacja: [edycja stron](https://docs.github.com/en/communities/documenting-your-project-with-wikis/adding-or-editing-wiki-pages), [menu i stopka](https://docs.github.com/en/communities/documenting-your-project-with-wikis/creating-a-footer-or-sidebar-for-your-wiki), [obrazy i diagramy](https://docs.github.com/en/communities/documenting-your-project-with-wikis/editing-wiki-content).

## Publikacja

Po inicjalizacji i przy skonfigurowanym uwierzytelnieniu Git:

```powershell
.\scripts\publish-wiki.ps1 -Publish
```

W wariancie z osobnym procesem dopisz `-Publish` na końcu polecenia z `powershell`.

Skrypt weryfikuje źródła, przygotowuje pakiet, klonuje wiki do `build/riftPlots.wiki`, ustala jej domyślną gałąź i kopiuje przygotowane pliki. Przy istniejącym checkoutcie wymaga czystego stanu i aktualizuje go przez `pull --ff-only`. Następnie tworzy commit i wykonuje zwykły push, bez wymuszania historii.

Publikowane są przygotowane pliki o tych samych nazwach. Dodatkowe strony istniejącej wiki nie są usuwane. Skrypt odmówi pracy na checkoutcie z lokalnymi zmianami, obcym zdalnym adresem lub inną gałęzią. Jeżeli commit/push się nie powiedzie, checkout pozostaje dostępny do sprawdzenia w `build/riftPlots.wiki`.

Nie zapisuj tokenów w skryptach. Jeśli Git nie ma dostępu, skonfiguruj zwykłe uwierzytelnienie dla swojego konta poza treścią dokumentacji.

## Aktualizacja treści

Zmieniaj źródła w `docs/wiki/`, uruchom walidację i obejrzyj wynik eksportu. Po weryfikacji ponów publikację. Zmiana tylko w zdalnym edytorze Wiki nie aktualizuje automatycznie kopii źródeł w głównym repozytorium.

Wdrożona wiki będzie dostępna pod adresem **https://github.com/tenfajnybartek/riftPlots/wiki**. Prawidłowy eksport lokalny nie jest potwierdzeniem publikacji — o niej świadczy dopiero udany push i odczyt stron ze zdalnego repozytorium.
