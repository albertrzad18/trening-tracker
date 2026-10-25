# Handoff: redesign apki trening-tracker (Android, Jetpack Compose + Material 3)

## Overview
Pełny redesign wyglądu i układu istniejącej, działającej apki **trening-tracker** (treningi z zegarka Huawei Watch GT5: Basen, Rower, Trenażer; dane z Health Connect, lokalnie w Room). **Zakres funkcji i ekranów się nie zmienia** — zmienia się wygląd i układ. Interfejs wyłącznie po polsku, zero emoji.

## About the Design Files
Pliki w `source/` to **makiety w HTML** (prototypy pokazujące wygląd i zachowanie), a nie kod produkcyjny do skopiowania. Zadanie: **odtworzyć je w istniejącym projekcie Android (Jetpack Compose + Material 3)** istniejącymi wzorcami i bibliotekami (Vico dla słupków/linii, własny `Canvas` dla heatmapy/paska udziału/stref, osmdroid dla mapy). Otwórz `source/Trening Tracker Final.dc.html` w przeglądarce, żeby zobaczyć wszystkie ekrany (zakładki: prototyp, ekrany główne, pozostałe, stany, tokeny). Źródła treści: `brief/BRIEF.md`, `brief/SCREENS.md`, `brief/CONTENT.md`.

## Fidelity
**Hi-fi.** Kolory, krój, rozmiary, odstępy i ruch są finalne. Odtwórz co do wartości z tabel poniżej i z `TtTheme.kt`.

## Wybrane warianty (decyzje zamknięte)
- Kierunek wizualny: **B „Panel”**. Zamiennik emoji: **E1 (ikony)**.
- Układy: Pulpit **1**, Treningi **1**, Szczegóły treningu **1**, Statystyki **2**, Podsumowania **1**, Rekordy **1**, Cele **1**, Więcej **2**, Ustawienia **2**, Diagnostyka **2**, ekrany powitalne (brak uprawnień / brak Health Connect / ładowanie) **1**.
- **Dynamic color (Material You) wyłączony** — stała paleta jasna i ciemna, motyw: jak w systemie / jasny / ciemny.

## Design Tokens
Pełne wartości w `TtTheme.kt` (gotowy plik Compose: kolory, typografia, kształty, odstępy, ruch).

Kolory (jasny / ciemny):
| token | jasny | ciemny | użycie |
|---|---|---|---|
| bg | #EEF0F3 | #0D0F12 | tło ekranu |
| surface | #FFFFFF | #171A1F | panele, arkusze, pasek nawigacji |
| track | #E3E6EB | #22262D | tory pasków, puste kratki, szkielety, tło segmentów |
| line | #D9DDE3 | #2A2F37 | linie 1 dp, obrysy chipów |
| ink | #0F1216 | #E8EBEF | tekst |
| ink2 | #59616E | #97A0AD | jedyny drugi poziom tekstu |
| accent | #B8326A | #F07AA6 | przycisk główny, wybrany chip/segment, linia tętna |
| onAccent | #FFFFFF | #1A0A11 | tekst na akcencie |
| pool (Basen) | #2F6BFF | #6E9BFF | |
| road (Rower) | #1E9E5A | #4CC985 | |
| trainer (Trenażer) | #E5730B | #FF9E4A | |
| error | #C4372B | #FF7A6B | tylko błąd synchronizacji, „Usuń cel” |
- Kontener dyscypliny = kolor dyscypliny 14% (jasny) / 22% (ciemny) na bg.
- Strefy tętna 1–5 = accent 20/40/60/80/100% na bg.
- Zmiana plus/minus: **strzałka + liczba, nigdy zielony/czerwony** (zielony = Rower).

Typografia: IBM Plex Sans (400/500/600) + cyfry tabelaryczne (`tnum`) wszędzie. heroXL 64/64 600 (−2%); hero 44/48 600; headline 24/30 600; value 20/26 600; title 16/22 600; body 15/22; bodyS 13/18; label 12/16 500 WERSALIKI +6%; caption 12/14 500. Jednostka przy dużej liczbie ≈ 40% rozmiaru liczby, kolor ink2, odstęp 4 dp.
Odstępy (skala 4): margines ekranu 16, odstęp między panelami 12, wypełnienie panelu 16, min. cel dotyku 48, min. wiersz listy 56.
Promienie: panel 20, mapa 16, arkusz 28 (górne rogi), pole 12, przyciski/chipy/segmenty/paski/słupki pełne.
Cienie: **brak**. Gradienty: **brak**. Elewacja = surface na bg.
Ruch: dotyk 150 ms (nakładka ink 8%), przejścia ekranów i rozwijanie 250 ms (fade), wykresy 450 ms (słupki rosną od osi ze staggerem 20 ms, linie odsłaniane od lewej), pierścień/pasek 600 ms. Easing `FastOutSlowInEasing`. **Bez odbić i sprężyn.** Szkielety: opacity 0,5↔1 w 1,2 s. Respektuj systemowe „usuń animacje”.
Ikony: Material Symbols Rounded, 24 dp (nawigacja: `home`, `format_list_bulleted`, `bar_chart`, `more_horiz`; Więcej: `calendar_view_week`, `trophy`, `flag`, `tune`, `troubleshoot`; inne: `sync`, `add`, `check_circle`, `arrow_upward/downward`, `chevron_right`, `expand_more/less`, `content_copy`).

## Formaty (obowiązkowe, polskie)
`1 234,5 km` (spacja niełamiąca jako tysiące, przecinek) · czas treningu `1:23:45` · suma czasu `19 h 20 min` / `51 min` · data `24 paź 2026` · z godziną `22 paź 2026, 18:54` · tempo `2:15 /100 m` · prędkość `26,8 km/h` · kalorie `1 793 kcal` · tętno `138 bpm` · zmiana ze znakiem `+55%`, `-37%` · udział `31%` · przewyższenie `326 m`. Referencja implementacji: funkcje `fmt*` w `source/tt-data.js`.
**Brak danych = brak pola** (trenażer bez dystansu/prędkości/trasy nie pokazuje „0 km”; sekcje bez danych nie istnieją). Jedyny wyjątek zamówiony wprost: kolumna dyscypliny na Pulpicie pokazuje `0 km` dla Roweru.

## Wspólne komponenty (nazwy pod Compose)
TtNavBar (M3 NavigationBar, 4 pozycje, wskaźnik accent 18%) · TtTopBar (strzałka + headline, bez tła) · Panel (surface, r=20, padding 16) · SectionHeader · MetricGrid (2 kol., linia 1 dp między wierszami, etykieta label ink2, wartość value + jednostka) · FilterChip dyscypliny (wybrany: kontener dyscypliny + obrys w kolorze dyscypliny) · PeriodMenuChip („Wszystko ▾” → DropdownMenu) · SegmentedToggle (tor track r=full, wybrany = accent/onAccent, wysokość 34–36) · SortSheet / AddGoalSheet (ModalBottomSheet, scrim ink 32%) · DeltaBadge (pigułka na track: strzałka + pogrubiony % + opis) · GoalRow · GoalRing · PrimaryButton (wysokość 52, accent, r=full) · SecondaryButton (obrys line, 48) · SkeletonBlock · ZoneBar · ShareBar · ActivityHeatmap · RouteMap · ElevationProfile.

## Screens / Views

### Pulpit (układ 1)
Przewijana lista paneli na tle bg (margines 16, odstęp 12). Kolejność:
1. **Kafelek „Ostatni tydzień”** (jeden panel): label `OSTATNI TYDZIEŃ · 19–25 PAŹ` → „Przepalone” (22/500, ink2) → **`1 027` heroXL + `kcal`** → wiersze dyscyplin (kropka 8 dp · nazwa · czas · dystans · kcal; siatka `8dp 1fr auto auto auto`, wysokość ≈44; Basen: `1 h 11 min`, `3,8 km`, `568 kcal`; Trenażer: `51 min`, `459 kcal`; dyscyplina bez treningów nie ma wiersza) → DeltaBadge `↓ 37% mniej niż poprzedni tydzień` → linia → dwa paski 10 dp r=full: „Ten tydzień 2 h 2 min” (szerokość proporcjonalna do poprzedniego, segmenty kolorami dyscyplin) i „Poprzedni tydzień 3 h 14 min” (te same kolory, alpha 40%) → linia → trzy kolumny rozdzielone linią pionową: pasek 3 dp u góry w kolorze dyscypliny (Rower bez treningów: kolor `line`), nazwa (label wersaliki), liczba treningów (value; `0` w ink2), pod spodem Basen `3,8 km`, Rower `0 km`, Trenażer `51 min`.
2. **Ostatnie treningi** (panel): 3 wiersze (min. 60 dp): kropka dyscypliny, nazwa + `22 paź 2026, 18:54` (bodyS ink2), po prawej czas `0:38:00` i pod nim dystans (Basen) lub kalorie (Trenażer, np. `459 kcal`). Na dole link „Zobacz podsumowania →”. Tap w wiersz → Szczegóły.
3. **Cele** (panel): do 2 GoalRow + link „Wszystkie cele →”.
Stany: ładowanie = szkielety tych samych bloków; pusto = „Brak treningów” (headline) + zdanie z CONTENT.md + PrimaryButton „Synchronizuj teraz” + 3 kroki (Health Sync → Health Connect → trening-tracker).
Usunięte z wcześniejszych wersji: wykres „Ostatnie 7 dni” i sekcja „Nowe rekordy” (nie implementować na Pulpicie).

### Treningi (układ 1)
FilterBar: chipy Basen/Rower/Trenażer (wielokrotny wybór) + „Wszystko ▾” (menu: Tydzień/Miesiąc/Rok/Wszystko). Pod spodem chip sortowania `Data ↓` (otwiera SortSheet: Data, Dyscyplina, Czas, Dystans, Kalorie, Tempo/prędkość, Tętno + Rosnąco/Malejąco) i `299 treningów` po prawej.
Lista **bez przewijania w poziomie**, grupowana po miesiącach — **nagłówek miesiąca jest przyklejony (sticky) i jest akordeonem** (tap zwija/rozwija wiersze miesiąca; strzałka expand_less/more; zawiera nazwę `październik 2026`, liczbę treningów, druga linia sumy `10 h 13 min · 13,5 km · 5 553 kcal`).
Wiersz: siatka `3dp(pasek dyscypliny) 64dp 1fr 1fr 1,2fr`, gap 12, padding 12×16: [`22 paź` + `czw 18:54` caption] [czas `0:38:00`] [dystans `2,0 km`] [tempo `1:52 /100 m` lub `26,8 km/h`]; druga linia bodyS ink2 `304 kcal · 128 bpm` (pomija brakujące). Brak wartości = pusta komórka.
Przyklejony na dole pasek sumy nad nawigacją: `SUMA (299)` + `353 h 20 min · 5 141,4 km · 211 715 kcal` + `śr. 1:10:54`. Przy aktywnym filtrze suma i licznik liczone dla widocznych wierszy.
Pusto: „Brak treningów dla wybranych filtrów.” + przycisk „Wyczyść filtry”.

### Szczegóły treningu (układ 1)
TopBar (tylko strzałka). Nagłówek: kropka + nazwa dyscypliny (headline), data z godziną. Dwie liczby hero obok siebie (czas, dystans; pod każdą DeltaBadge `↑ 55% powyżej średniej` / `59%` — zastępuje dawną kartę „Na tle Twojej średniej”). MetricGrid (tylko istniejące): Kalorie, Śr. tętno, Maks. tętno, Śr. prędkość / Tempo, Maks. prędkość, Przewyższenie. Potem (panele): **Tętno w czasie** (linia accent 2,5 dp + wypełnienie 12%, kropka na końcu, bez siatki, nad wykresem `138 bpm śr.`), **Strefy tętna** (5 poziomych pasków 8 dp na wspólnej skali, numer strefy, czas po prawej w jednej linii, strefa 0 min = sam tor; opis „Granice liczymy z tętna maksymalnego z ustawień.”), **Prędkość** (linia w kolorze dyscypliny + pozioma linia średniej z etykietą), **Trasa** (mapa osmdroid r=16 w panelu, ślad 3 dp w kolorze roweru, znaczniki Start/Meta) + **Profil wysokości** (linia + obszar 12%, oś X w km). Trenażer: tylko czas, kalorie, tętno + strefy (brak dystansu, prędkości, trasy). Basen: Tempo zamiast prędkości, brak mapy.

### Statystyki (układ 2)
FilterBar jak w Treningach. Pod nim poziomo przewijany rząd zakładek tekstowych **Objętość · Kalendarz · Udział · Rytm · Postęp** (aktywna: ink 600 + podkreślenie 2 dp). Treść zakładki w panelach (tytuł, jedno zdanie, kluczowa liczba, wykres):
- **Objętość**: SegmentedToggle Czas/Dystans/Kalorie, kluczowa liczba `180 h 1 min`, słupki warstwowe po miesiącach (Basen, Rower, Trenażer od dołu; szer. 40% slotu, pełne zaokrąglenie, przerwa 1 dp; etykieta wartości nad najwyższym słupkiem; bez siatki).
- **Kalendarz**: heatmapa 26 tygodni, kółka 9 dp gap 3, 4 poziomy nasycenia koloru dyscypliny (30/55/80/100%), puste = track, etykiety pn/śr/pt/nd i miesięcy, legenda „dłuższy trening = ciemniejszy”; kluczowa liczba = dni z treningiem.
- **Udział**: poziomy pasek 16 dp z trzema odcinkami + legenda `31% · 56 h 29 min` (suma `180 h 1 min` nad paskiem).
- **Rytm**: Dni tygodnia (pn–nd) i Pora dnia (`0–4`…`20–24`) słupki ink, jeden pod drugim.
- **Postęp**: Dystans narastająco (2026 gruba linia ink, 2025 cienka ink2, etykiety końców `2026 · 2 600`, `2025 · 2 480`), Trend (SegmentedToggle Basen/Rower/Trenażer; cienkie punkty = treningi, gruba linia w kolorze dyscypliny = średnia z 5; Basen: tempo /100 m, niżej = lepiej; Trenażer przy <2 treningach: „Potrzebne są co najmniej dwa treningi z tymi danymi.”), Forma Rower (km/h na 100 uderzeń tętna).
Stany: ładowanie = szkielety; pusto = „Brak treningów do pokazania. Zsynchronizuj dane w zakładce Więcej.”

### Podsumowania (układ 1)
SegmentedToggle Tygodnie/Miesiące/Lata. Lista okresów (panele): **najnowszy rozwinięty**: zakres (label), hero `2 h 2 min`, `3,8 km · 1 027 kcal`, trzy DeltaBadge (Czas ↓37%, Dystans ↑109%, Kalorie ↓48%), pasek udziału dyscyplin 10 dp, wiersze dyscyplin (`1 h 11 min · 3,8 km · 568 kcal`), fakty `3 treningi · 3 dni z treningiem · seria 2 dni`, klikalny wiersz „Najlepszy trening: Trenażer, 20 paź 2026 · 0:51:00” z chevronem. **Starsze zwinięte**: zakres, czas, mini-pasek 48 dp, zmiana czasu ze strzałką, expand_more; tap rozwija (250 ms, jeden otwarty naraz). Pusty okres: „Brak treningów w tym okresie.”

### Rekordy (układ 1)
Panel na dyscyplinę (kropka + tytuł); RecordRow min. 60 dp: nazwa (bodyS ink2) po lewej, wartość (value + jednostka) po prawej, data caption pod nią; cały wiersz klikalny → trening. Rekord z ostatnich 7 dni: pod nazwą ikona `trophy` (FILL) w kolorze dyscypliny + „Nowy rekord”. Na końcu panel „Najlepsze okresy”. Pusto: „Rekordy pojawią się, gdy w bazie będą treningi.”

### Cele (układ 1)
Lista paneli GoalRow: kropka + nazwa `Rower · Dystans (km) · Miesięczny` (label), `0,0 km` (34 px) + `z 300,0 km`, pasek 6 dp r=full (track + kolor dyscypliny; „Wszystko” = ink), prognoza `W tym tempie wyjdzie 93,0 km`, `Zostało 7 dni`, **historia = 6 kwadratów 10 dp r=2** (zrobiony = wypełniony kolorem dyscypliny, niezrobiony = tylko obrys line) + `Seria: 6 okresów pod rząd`. Cel osiągnięty: ikona `check_circle` (FILL) + „Cel osiągnięty” w kolorze dyscypliny. ExtendedFAB „Dodaj cel” (ikona `add`, accent, prawy dół, sticky). AddGoalSheet: chipy Wszystko/Basen/Rower/Trenażer, Tygodniowy/Miesięczny/Roczny, Dystans (km)/Czas (godziny)/Kalorie/Liczba treningów, pole „Wartość celu” z jednostką, PrimaryButton „Zapisz cel”. Pusto: „Nie masz jeszcze żadnego celu. Dodaj pierwszy przyciskiem poniżej.”

### Więcej (układ 2)
Pierwszy wiersz panelu: `Ostatnia synchronizacja:` + `24 paź 2026, 21:14` + mały SecondaryButton „Synchronizuj” po prawej (błąd: linia `Ostatni błąd: …` w kolorze error; brak synchronizacji: „Jeszcze nie synchronizowano”). Dwie grupy list: **Przegląd** (Podsumowania „Ten tydzień: 2 h 2 min”, Rekordy osobiste „11 rekordów”, Cele „1 aktywny”) i **Aplikacja** (Ustawienia „Tętno maks. 190 · basen 25 m”, Diagnostyka danych z Health Connect). Wiersz min. 64 dp: ikona, tytuł + podtytuł z realną informacją, chevron, linia 1 dp. Ekrany podrzędne bez dolnej nawigacji, ze strzałką powrotu.

### Ustawienia (układ 2)
Płaska lista SettingRow (nazwa po lewej, bieżąca wartość po prawej: `190 bpm`, `25 m`, `Jak w systemie`); tap otwiera arkusz edycji (tętno maksymalne: pole liczbowe z `bpm` + wyjaśnienie „Potrzebne do stref tętna w szczegółach treningu.”; długość basenu: segmenty 25 m / 50 m; motyw: Jak w systemie / Jasny / Ciemny; przycisk „Gotowe”). Ostatni wiersz „Jak ustawić Health Sync” rozwija inline 5 kroków (teksty z oryginalnej apki).

### Diagnostyka (układ 2)
Opis jednym zdaniem; tabela: nazwa typu (mono) + liczba po prawej + cienki pasek proporcjonalny (skala logarytmiczna); wiersz `Okres: 1 kwi 2025 – 24 paź 2026`; SecondaryButton „Kopiuj raport” (`content_copy`); panel „Tylko wersja debug” z przyciskami „Wczytaj dane demo” / „Usuń dane demo” (tylko w wariancie debug).

### Ekrany powitalne (układ 1) — brak uprawnień / brak Health Connect / ładowanie
Pełny ekran bg: wordmark `trening-tracker`, headline 32/37 „Twoje treningi z zegarka, na telefonie.”, pionowy łańcuch kroków (Zegarek Huawei → Huawei Health → Health Sync → Health Connect → trening-tracker; ukończone: ikona check ink2; bieżący: wypełnione koło ink + tekst ink 600; łącznik 1,5 dp line), na dole zdanie z CONTENT.md i PrimaryButton („Nadaj uprawnienia” / „Zainstaluj Health Connect”). Ładowanie: zamiast przycisku pasek postępu 2 dp (indeterminate) + „Wczytuję treningi…”. Bieżący krok: brak uprawnień i ładowanie = `trening-tracker`, brak HC = `Health Connect`.

## Interactions & Behavior
- Nawigacja: 4 zakładki na dole (Pulpit, Treningi, Statystyki, Więcej); Więcej → Podsumowania/Rekordy/Cele/Ustawienia/Diagnostyka (bez dolnego paska, strzałka wraca). Tap w trening z dowolnego miejsca → Szczegóły. Przejście między ekranami: fade 250 ms.
- Filtry (chipy dyscyplin, menu okresu) i sortowanie na liście Treningi zmieniają listę bez skoku układu; sortowanie po dacie grupuje po miesiącach, po innych polach lista jest płaska.
- Wykresy: dotyk słupka/punktu podmienia kluczową liczbę nad wykresem na wartość elementu + datę (bez dymka), zaznaczenie = pionowa linia ink + punkt 6 dp.
- Pulpit/Statystyki/Treningi: ładowanie = szkielety; puste = teksty z CONTENT.md.

## State Management
Jak w istniejącej apce (Room + Health Connect + ViewModel/Flow). Nowy stan UI: zwinięte miesiące (Set kluczy `YYYY-MM`) i klucz+kierunek sortowania w Treningach; wybrana zakładka w Statystykach; metryka (Czas/Dystans/Kalorie) w Objętości; wybrana dyscyplina w Trendzie; rozwinięty okres w Podsumowaniach; stan arkuszy (sortowanie, dodawanie celu, edycja ustawień).

## Do rozstrzygnięcia w kodzie
Wyłączenie `dynamicColor` i własny `ColorScheme` (mapowanie w `TtTheme.kt`) · IBM Plex Sans/Mono z latin-ext w `res/font` + `tnum` · Vico: ukryta oś Y i siatka, pełne zaokrąglenie słupków warstwowych (własny `ColumnProvider`), przerwa 1 dp między warstwami, etykieta wartości nad najwyższym słupkiem (własny `Decoration`), marker jako callback zamiast dymka · kolor kratki heatmapy przy dwóch dyscyplinach jednego dnia (proponowane: najdłuższy trening) · `stickyHeader` w `LazyColumn` + pasek sumy nad `NavigationBar` (wcięcia okna) · ślad trasy w osmdroid (cieńsza Polyline w kolorze dyscypliny, znaczniki Start/Meta) · animacje przez `animateFloatAsState`/clip, wyłączane przez systemowe „usuń animacje” · ukrywanie dyscypliny nigdy nietrenowanej (proponowane: po 90 dniach) · `0 km` dla Roweru w kolumnie Pulpitu to świadomy wyjątek od reguły „brak danych = brak pola”.

## Assets
Brak obrazów. Ikony: Material Symbols Rounded. Czcionki: IBM Plex Sans i IBM Plex Mono (Google Fonts, licencja OFL). Mapa: kafelki OpenStreetMap (osmdroid) — w makiecie zastąpione prążkowanym placeholderem.

## Files
- `TtTheme.kt` — gotowe tokeny i motyw Compose.
- `PROMPT_DLA_CLAUDE_CODE.md` — prompt do wklejenia.
- `source/Trening Tracker Final.dc.html` — podgląd wybranej wersji (otwórz w przeglądarce); `source/TT *.dc.html` — poszczególne ekrany (czytaj jako referencję układu i wartości); `source/tt-data.js` — dane demo i formatery; `source/tt-charts.js` — geometria wykresów.
- `brief/` — BRIEF.md, SCREENS.md, CONTENT.md (oryginalne wymagania i teksty).
Uwaga: w plikach źródłowych są też układy/kierunek niewybrane (A, układy 2 itd.) — ignoruj je; obowiązuje sekcja „Wybrane warianty”.
