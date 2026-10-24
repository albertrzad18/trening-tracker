# Start projektu: przekazanie kontekstu dla Claude

Przeczytaj ten plik i `docs/PLAN.md`, a potem wykonaj „Następne kroki”.

## O projekcie
**trening-tracker** to apka na Androida dla posiadacza zegarka Huawei Watch GT5. Pokazuje treningi
(pływanie na basenie, rower na dworze, trenażer) w tabelach, szczegółach, wykresach i podsumowaniach,
razem z celami, rekordami osobistymi i mapą tras. Pełny plan funkcji i ekranów jest w `docs/PLAN.md`.

## Ustalone decyzje
- Dane: Huawei Health → aplikacja Health Sync → **Health Connect** → trening-tracker. Bez Huawei Health Kit i bez kluczy API.
- Bez backendu i kont. Dane lokalnie w Room. Apka dla autora i ewentualnie znajomych (APK z GitHub Releases).
- Repo **publiczne** na GitHubie pod nazwą `trening-tracker`. W repo nie może być sekretów, keystore ani danych treningowych.
- Stack: Kotlin, Jetpack Compose, Material 3 (dynamiczne kolory, jasny i ciemny motyw), Room, WorkManager, Vico (wykresy), osmdroid/MapLibre + OSM (mapa).
- UI po polsku, polskie formaty liczb i dat.
- Model pracy: planowanie na Opus (high), implementacja zwykle na Sonnet (medium).

## Stan środowiska (24.10.2026)
- Git 2.43 ✅, JDK 21 (Temurin) ✅.
- GitHub CLI ✅, zalogowany jako `albertrzad18`. Jeśli `gh` nie jest w PATH, użyj `C:\Program Files\GitHub CLI\gh.exe`.
- Android Studio ✅ (`C:\Program Files\Android\Android Studio`). Wbudowane JDK jest w `jbr\`.
- Android SDK ✅ w `%LOCALAPPDATA%\Android\Sdk`: platforma `android-37.0`, build-tools, platform-tools, emulator.

## Następne kroki
1. Sprawdź środowisko: `gh auth status`, Android SDK, JDK.
2. Utwórz szkielet projektu Android w tym folderze: Gradle Kotlin DSL, version catalog, Compose,
   minSdk 28, jeden moduł `app`. Zapytaj użytkownika o package name (np. `pl.<nick>.treningtracker`).
3. Dodaj `.gitignore` (Android + `local.properties`, `*.jks`, `*.keystore`), `README.md` (po polsku, z opisem konfiguracji
   Health Sync i Health Connect) oraz `LICENSE` (MIT).
4. Sprawdź, czy projekt się buduje (`gradlew assembleDebug`). Uzupełnij sekcję „Komendy” w `CLAUDE.md` o faktyczną strukturę projektu.
5. Zrób `git init`, pierwszy commit i `gh repo create trening-tracker --public --source . --push`, po potwierdzeniu z użytkownikiem.
6. Przejdź do **Etapu 0** z planu: ekran debug, który pokazuje, jakie dane faktycznie przychodzą z Health Connect
   dla basenu, roweru i trenażera.
