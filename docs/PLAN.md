# Plan: aplikacja do treningów (pływanie, rower, trenażer) z danymi z Huawei Health

## Kontekst
Albert ma zegarek Huawei Watch GT5 i chce mieć własną apkę na Androida, w której przejrzyście
zobaczy swoje treningi: tabele, szczegóły, dużo wykresów, podsumowania tygodnia i miesiąca, postęp.
Interesują go tylko trzy dyscypliny: **pływanie na basenie, rower na dworze i trenażer**. Najważniejsze
dane to czas, dystans, kalorie, rozkład treningów w czasie i postęp.
Apka jest dla niego i ewentualnie znajomych, a repo będzie publiczne na GitHubie.

**Ustalone decyzje**
- Źródło danych: Huawei Health → Health Sync → **Health Connect** → apka. Bez kluczy API w repo.
- Bez serwera i bez kont. Każdy użytkownik widzi tylko swoje dane, przechowywane lokalnie.
- Dodatki: **cele**, **rekordy osobiste**, **mapa tras rowerowych**.
- Wygląd: Material 3, jasny i ciemny motyw zgodnie z telefonem, kolory z tapety (Material You).
- Język interfejsu: polski.

---

## 1. Dane: co dostaniemy i jak to pokażemy

| Dana | Basen | Rower (dwór) | Trenażer |
|---|---|---|---|
| Czas trwania | ✅ | ✅ | ✅ |
| Dystans | ✅ | ✅ | ⚠️ zależy od zegarka |
| Kalorie | ✅ | ✅ | ✅ |
| Tętno (średnie, max, wykres) | ✅ | ✅ | ✅ |
| Tempo / prędkość | tempo /100 m | km/h | km/h, jeśli jest dystans |
| Przewyższenie | – | ✅ jeśli jest | – |
| Kadencja / moc | – | ⚠️ jeśli są czujniki | ⚠️ jeśli są czujniki |
| Długości / odcinki, styl | ⚠️ jeśli są przesłane | – | – |
| Trasa GPS | – | ✅ | – |

⚠️ **Do sprawdzenia na starcie:** nie wszystkie dane z Huawei Health przechodzą przez Health Sync do
Health Connect. SWOLF prawdopodobnie nie przejdzie, a długości basenu i styl pływania są niepewne. Etap 0
odpowiada na to pytanie, zanim zaprojektujemy szczegóły ekranów. Apka pokazuje tylko te dane, które
faktycznie są, i ukrywa puste pola.

⚠️ Health Connect domyślnie daje dostęp do danych z **30 dni przed nadaniem uprawnień**. Do starszej
historii potrzebne jest dodatkowe uprawnienie „dostęp do historii” (nowsze wersje Androida / Health Connect).
Apka o nie poprosi i zapisze wszystko w lokalnej bazie, więc historia nie zniknie.

---

## 2. Ekrany (dolna nawigacja: Pulpit · Treningi · Statystyki · Więcej)

### 2.1 Pulpit
To, co widać po otwarciu apki:
- **Ten tydzień:** łączny czas, dystans i kalorie, z porównaniem do poprzedniego tygodnia (np. „+12%”).
- **Kafelki dyscyplin:** basen / rower / trenażer, każda ze swoim kolorem, liczbą treningów i dystansem w tygodniu.
- **Postęp celów:** paski postępu (np. „Rower: 64 / 100 km”).
- **Mini-wykres 7 dni:** słupki minut treningu na każdy dzień, kolorowane dyscypliną.
- **Ostatnie 3 treningi:** karty; kliknięcie otwiera szczegóły.
- Nowe rekordy z ostatniego tygodnia, jeśli są (np. „🏆 Nowy rekord: najdłuższa jazda 82 km”).

### 2.2 Treningi (tabela)
- Tabela z kolumnami: data, dyscyplina (ikona i kolor), czas, dystans, kalorie, śr. tempo/prędkość, śr. tętno.
- **Sortowanie** po kliknięciu w nagłówek kolumny.
- **Filtry:** dyscyplina (wiele naraz), zakres dat (tydzień / miesiąc / rok / własny).
- Wiersze zgrupowane po miesiącach, z sumą miesiąca w nagłówku grupy.
- Wiersz podsumowania na dole: suma i średnia dla przefiltrowanych treningów.
- Kliknięcie w wiersz otwiera szczegóły.

### 2.3 Szczegóły treningu
- Nagłówek: dyscyplina, data, godzina, nazwa.
- Siatka głównych metryk: czas, dystans, kalorie, śr./max tętno, śr./max prędkość lub tempo.
- **Wykres tętna** w czasie i **czas w strefach tętna** (słupki stref 1–5).
- **Rower na dworze:** mapa trasy (OpenStreetMap, bez klucza API), wykres prędkości i wykres wysokości.
- **Trenażer:** wykresy prędkości, mocy i kadencji, jeśli są dane.
- **Basen:** tempo /100 m, tabela odcinków / długości, jeśli są dane.
- Porównanie z Twoją średnią dla tej dyscypliny (np. „dystans +15% vs średnia”).

### 2.4 Statystyki (wykresy)
Na górze przełącznik okresu (**Tydzień / Miesiąc / Rok / Wszystko**) i filtr dyscypliny. Pod nim przewijana lista wykresów:
1. **Objętość treningowa:** słupki czasu (albo dystansu lub kalorii, do przełączenia) na tydzień/miesiąc, ułożone warstwami według dyscyplin.
2. **Kalendarz aktywności (heatmapa):** jak na GitHubie, kolor dnia według czasu treningu. Pokazuje rozkład treningów w czasie.
3. **Trend prędkości / tempa:** linia ze średnią kroczącą osobno dla roweru, trenażera i basenu. Pokazuje postęp.
4. **Dystans narastająco w roku:** linia bieżącego roku na tle poprzedniego.
5. **Udział dyscyplin:** wykres kołowy / donut z podziałem czasu.
6. **Kiedy trenuję:** słupki według dnia tygodnia i według pory dnia.
7. **Tętno vs prędkość w czasie:** czy przy tej samej prędkości tętno spada (forma rośnie).
8. **Kalorie w czasie:** słupki tygodniowe / miesięczne.

### 2.5 Podsumowania (z menu „Więcej” i z karty na Pulpicie)
- Lista okresów: tygodnie / miesiące / lata, przełączana zakładkami.
- Karta okresu zawiera:
  - liczbę treningów i aktywne dni,
  - łączny czas, dystans i kalorie,
  - zmianę w procentach względem poprzedniego okresu,
  - podział na dyscypliny,
  - najlepszy trening okresu,
  - najdłuższą serię dni z treningiem,
  - pobite rekordy i zrealizowane cele.

### 2.6 Rekordy osobiste
Osobno dla każdej dyscypliny. Każdy rekord pokazuje datę i link do treningu:
- **Rower / trenażer:** najdłuższy dystans, najdłuższy czas, najwyższa średnia prędkość (dla treningów powyżej 20 km), najwięcej kalorii, największe przewyższenie.
- **Basen:** najdłuższy dystans, najlepsze średnie tempo /100 m (dla treningów od 1000 m), najdłuższy czas.
- Rekordy tygodnia i miesiąca: największy dystans i czas w jednym tygodniu lub miesiącu.
- Historia rekordu: wykres, jak rekord się poprawiał.

### 2.7 Cele
- Cel tygodniowy, miesięczny lub roczny dla dyscypliny albo dla wszystkich łącznie. Mierzy dystans, czas, kalorie albo liczbę treningów.
- Pasek postępu z prognozą (np. „w tym tempie zrobisz 92 km z 100”).
- Historia: które tygodnie i miesiące cel był zrealizowany, z serią kolejnych udanych okresów.

### 2.8 Ustawienia
- Uprawnienia Health Connect i status synchronizacji (ostatnia synchronizacja, przycisk „Synchronizuj teraz”).
- Krótka instrukcja konfiguracji Health Sync dla znajomych.
- Tętno maksymalne / strefy tętna, potrzebne do wykresu stref.
- Domyślna długość basenu: 25 lub 50 m.
- Motyw: system / jasny / ciemny.

---

## 3. Wygląd i czytelność
- Material 3 z dynamicznymi kolorami, w jasnej i ciemnej wersji.
- **Stały kolor dla każdej dyscypliny**, używany w tabelach, wykresach i kafelkach: basen niebieski, rower zielony, trenażer pomarańczowy.
- Duże liczby dla kluczowych metryk, jednostki mniejszą czcionką.
- Formaty polskie: `1 234,5 km`, `1:23:45`, `24 paź 2026`.
- Puste stany z instrukcją, gdy nie ma danych albo uprawnień.

---

## 4. Technika (w skrócie)
- **Kotlin + Jetpack Compose**, Material 3, minSdk 28 (Health Connect obsługuje Android 9+).
- **Health Connect** (`androidx.health.connect:connect-client`):
  - `ExerciseSessionRecord` z typami `SWIMMING_POOL`, `BIKING`, `BIKING_STATIONARY`,
  - `DistanceRecord`, `TotalCaloriesBurnedRecord`/`ActiveCaloriesBurnedRecord`, `HeartRateRecord`, `SpeedRecord`,
  - `PowerRecord`, `CyclingPedalingCadenceRecord`, `ElevationGainedRecord` oraz `ExerciseRoute`.
- **Room:** lokalna baza treningów (cache i historia), z której szybko liczą się agregaty do wykresów.
- **WorkManager:** synchronizacja w tle, przyrostowa przez Changes API w Health Connect.
- **Vico:** wykresy w Compose. **osmdroid albo MapLibre + OSM:** mapa bez klucza API.
- Architektura: MVVM, warstwa `data` (Health Connect + Room), `domain` (agregacje, rekordy, cele), `ui`.
- Repo: `.gitignore` (keystore, `local.properties`), README z instrukcją konfiguracji, licencja MIT. Żadnych danych treningowych w repo.

---

## 5. Etapy
**Start (pierwszy krok po zatwierdzeniu planu):** utworzyć folder `C:\Users\Albert\IdeaProjects\trening-tracker`
i zapisać w nim ten plan jako `docs/PLAN.md`. Dopiero potem: Android Studio, szkielet apki, `git init` i publiczne repo
`trening-tracker` na GitHubie. Do tego trzeba zainstalować `gh` i Android Studio.

0. **Rozpoznanie danych:** szkielet apki z ekranem debug, który wypisuje, co faktycznie przychodzi z Health Connect dla każdej z 3 dyscyplin. Wynik decyduje o szczegółach ekranu basenu i trenażera.
1. **MVP:** synchronizacja do Room, tabela treningów z filtrami i sortowaniem, podstawowe szczegóły treningu, Pulpit.
2. **Wykresy i podsumowania:** ekran Statystyki (wykresy 1–8), podsumowania tygodnia, miesiąca i roku.
3. **Dodatki:** rekordy osobiste, cele, mapa tras, wykresy w szczegółach (tętno, strefy, prędkość, wysokość).
4. **Dopracowanie:** puste stany, animacje, ikona, README ze screenshotami, wydanie APK na GitHub Releases dla znajomych.

## 6. Weryfikacja
- Testy jednostkowe agregacji: sumy tygodnia i miesiąca, % zmiany, rekordy, cele, serie dni. Przypadki brzegowe: trening przez północ, brak dystansu na trenażerze, strefy czasowe.
- Na prawdziwym telefonie z Twoimi danymi: porównanie kilku treningów (czas, dystans, kalorie) z apką Huawei Health.
- Test bez uprawnień i bez danych: puste stany.
- Test w jasnym i ciemnym motywie oraz na małym ekranie.
