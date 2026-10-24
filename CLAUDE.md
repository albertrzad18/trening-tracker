# trening-tracker

Apka na Androida pokazująca treningi z zegarka Huawei Watch GT5: pływanie na basenie, rower na dworze, trenażer.
Plan funkcji i ekranów: `docs/PLAN.md`. Stan startowy i pierwsze kroki: `docs/START.md`.

## Architektura
- Dane: Huawei Health → Health Sync → **Health Connect** → apka. Nie używamy Huawei Health Kit.
- Bez backendu i kont. Health Connect jest źródłem, Room lokalnym cache i historią. Ekrany czytają tylko z Room.
- Stack: Kotlin, Jetpack Compose, Material 3, Room, WorkManager, Vico (wykresy), mapa na OSM bez klucza API.
- Warstwy: `data` (Health Connect, Room, sync), `domain` (agregacje, rekordy, cele: czysty Kotlin z testami), `ui` (Compose + ViewModel).
- Logika liczenia (sumy, % zmiany, rekordy, serie, cele) siedzi w `domain` i ma testy jednostkowe.

## Zasady
- Repo jest **publiczne**: żadnych sekretów, keystore, `local.properties` ani danych treningowych w commitach.
- Interfejs **po polsku**, formaty polskie: `1 234,5 km`, `1:23:45`, `24 paź 2026`. Teksty w `strings.xml`.
- Stałe kolory dyscyplin w motywie: basen niebieski, rower zielony, trenażer pomarańczowy. Używane wszędzie (tabele, wykresy, kafelki).
- Jasny i ciemny motyw. Każdy nowy ekran sprawdzić w obu.
- Brakujące dane (np. dystans na trenażerze) ukrywamy, nie pokazujemy zer.
- Commity małe, po angielsku, w trybie rozkazującym (np. `Add workout list screen`).

## Komendy
- Build: `./gradlew assembleDebug`
- Testy: `./gradlew test`

- Środowisko: Gradle 9.8.2 (wrapper), AGP 9.4.1 (wbudowany Kotlin, bez wtyczki `kotlin-android`), Kotlin 2.4.21, KSP, compileSdk/targetSdk 37, minSdk 28.
- Package: `io.github.albertrzad18.treningtracker`, jeden moduł `app`. Wersje bibliotek w `gradle/libs.versions.toml`.
- `local.properties` (poza gitem) ma `sdk.dir=C\:/Users/Albert/AppData/Local/Android/Sdk`.
- Pierwszy build trwa kilka minut (pobieranie zależności), kolejne są szybsze.

## Nauka (ważne)
Użytkownik uczy się Kotlina i Androida na tym projekcie. Zna podstawy programowania, ale nie zna Kotlina, Compose ani budowy apki na Androida.
- Przy każdym nowym pojęciu wyjaśnij krótko po polsku, co to jest i po co, na przykładzie z tego projektu. Pojęcia to m.in.:
  `val`/`var`, null-safety (`?`, `?.`, `?:`), `data class`, funkcje i extension functions, lambdy,
  coroutines i `suspend`, `Flow`, `@Composable`, `remember`/`State`, ViewModel, Room (`@Entity`, `@Dao`), Gradle, `AndroidManifest.xml`.
- Po większej zmianie dodaj krótkie „co tu się stało”: jakie pliki doszły, gdzie leżą w strukturze projektu i jak się ze sobą łączą.
  Pokaż też, jak samemu zrobić podobną rzecz, np. dodać nową metodę, nowy ekran, obsługę kliknięcia albo nowe pole w bazie.
- Wyjaśniaj tylko nowe rzeczy, bez powtarzania tego, co już było. Krótko, nie wykład.
- Każde nowe pojęcie dopisz do `docs/NAUKA.md` jako własną notatkę: nagłówek, 2–5 zdań i mały przykład z kodu projektu.
  Plik jest ściągawką do powtórki, uporządkowaną tematycznie.
- Gdy zadanie jest proste i dobre do ćwiczeń, zaproponuj, że użytkownik napisze je sam, i daj wskazówki zamiast gotowego kodu.

## Sposób pracy
- Każdy etap z `docs/PLAN.md` najpierw rozpisujemy w trybie plan, potem implementujemy.
- Przed zmianą modelu danych (encje Room, schemat sync) zapytaj użytkownika. Migracje Room muszą zachować dane.
