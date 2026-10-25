# Prompt do wklejenia w Claude Code

Skopiuj wszystko poniżej linii. Uruchom Claude Code w katalogu głównym repozytorium apki Android, po wcześniejszym skopiowaniu folderu `design_handoff_trening_tracker_redesign/` do repo (np. do `docs/redesign/`).

---

Wdrażasz redesign istniejącej apki **trening-tracker** (Android, Jetpack Compose + Material 3, Room, Health Connect, Vico, osmdroid). Apka w pełni działa; **zmieniasz tylko wygląd i układ, nie funkcje, dane ani logikę biznesową**.

**Źródło prawdy:** `docs/redesign/README.md` (czytaj w całości najpierw), `docs/redesign/TtTheme.kt` (gotowe tokeny), `docs/redesign/brief/*.md` (wymagania i teksty). Pliki w `docs/redesign/source/*.html` to makiety HTML do podglądu zachowania i wartości, **nie** kod do przeniesienia — odtwórz je natywnie w Compose, zgodnie z konwencjami tego repo. Wybrana wersja jest opisana w README w sekcji „Wybrane warianty” (kierunek B, ikony zamiast emoji, konkretne układy per ekran); pozostałych wariantów w plikach źródłowych nie implementuj.

**Zasady nie do ruszenia:**
1. Interfejs wyłącznie po polsku, formaty polskie (`1 234,5 km`, `1:23:45`, `24 paź 2026`, `2:15 /100 m`, `+12%`). **Zero emoji** i znaków ✔/✘ w UI — zamienniki są w README (ikony `trophy`, `check_circle`, kwadraty historii celu).
2. **Brakujące dane ukrywamy**, nie pokazujemy zer (trenażer bez dystansu/prędkości/trasy). Jedyny wyjątek: kolumna Rower na Pulpicie pokazuje `0 km`.
3. **Wyłącz dynamic color.** Stała paleta jasna/ciemna z `TtTheme.kt`; motyw: jak w systemie / jasny / ciemny (ustawienie już istnieje).
4. Kolory dyscyplin: Basen niebieski, Rower zielony, Trenażer pomarańczowy — z `TtColors`. Zmiana plus/minus = strzałka + liczba, nie zielony/czerwony.
5. Bez cieni, gradientów, szkła. Ruch 150–600 ms, `FastOutSlowInEasing`, bez sprężyn; respektuj systemowe usuwanie animacji.
6. Apka ma działać na 360 dp szerokości i jedną ręką. Cele dotyku ≥ 48 dp. Kontrast tekstu ≥ 4,5:1 w obu motywach.
7. Nie zmieniaj schematu Room, repozytoriów, synchronizacji z Health Connect ani nawigacji (4 zakładki + ekrany podrzędne). Jeśli dane potrzebne do nowego widoku nie istnieją (np. suma poprzedniego tygodnia z podziałem na dyscypliny, sumy miesiąca), dodaj je w warstwie ViewModel/UseCase, bez zmiany schematu.

**Plan pracy (rób po kolei, po każdym kroku buduj projekt i uruchom, commit per krok):**
1. Przeczytaj README i obecny kod UI; wypisz mi krótko, które pliki/composable zmienisz i czego brakuje w danych. Poczekaj na moje „ok” tylko jeśli coś jest niejasne lub sprzeczne z README — w przeciwnym razie idź dalej.
2. Motyw: dodaj `TtTheme.kt` (dopasuj pakiet), dodaj fonty IBM Plex Sans/Mono z latin-ext do `res/font` (polskie znaki muszą działać) i podepnij `tnum`; wyłącz dynamic color; przepnij `MaterialTheme`.
3. Komponenty wspólne: `Panel`, `SegmentedToggle`, `DeltaBadge`, `FilterChip` dyscypliny, `PrimaryButton`/`SecondaryButton`, `SectionHeader`, `MetricGrid`, `SkeletonBlock`, dolna nawigacja (M3 `NavigationBar`, wskaźnik accent 18%), `TtTopBar`.
4. Pulpit → Treningi (sticky nagłówki miesięcy jako akordeon, pasek sumy, arkusz sortowania, bez przewijania poziomego) → Szczegóły treningu.
5. Wykresy: najpierw wspólne ustawienia Vico (bez osi Y i siatki, pełne zaokrąglenie słupków, przerwa 1 dp między warstwami, animacja wejścia 450 ms); własny `Canvas`: heatmapa (kółka), pasek udziału, paski stref tętna; mapa osmdroid z nowym stylem śladu. Potem Statystyki (układ 2 z zakładkami).
6. Podsumowania, Rekordy, Cele (z ExtendedFAB i arkuszem dodawania), Więcej, Ustawienia (lista + arkusze edycji), Diagnostyka.
7. Ekrany powitalne: brak uprawnień, brak Health Connect, ładowanie.
8. Stany ładowania (szkielety) i puste (teksty z `brief/CONTENT.md`) na każdym ekranie.
9. Sprzątanie: usuń nieużywane stare komponenty i emoji z zasobów/stringów; sprawdź `grep` po emoji i znakach ✔✘; sprawdź oba motywy i szerokość 360 dp na emulatorze; dodaj `@Preview` jasny/ciemny dla głównych ekranów (ze stanami: dane, ładowanie, pusto).

**Definition of done:** każdy ekran z README wygląda i zachowuje się jak makieta w obu motywach; brak emoji; brak zer dla brakujących danych; testy istniejące przechodzą; podsumuj mi zmiany i listę rzeczy z sekcji „Do rozstrzygnięcia w kodzie”, które rozstrzygnąłeś, z uzasadnieniem.

Zacznij od kroku 1.
