# trening-tracker

Apka na Androida, która pokazuje treningi z zegarka Huawei Watch GT5: pływanie na basenie,
rower na dworze i trenażer. Tabele, szczegóły, wykresy, podsumowania, cele, rekordy i mapa tras.

Dane zostają na telefonie. Apka nie ma serwera ani kont.

> Projekt w budowie. Plan funkcji: [`docs/PLAN.md`](docs/PLAN.md).

## Skąd apka bierze dane

```
Zegarek → Huawei Health → Health Sync → Health Connect → trening-tracker
```

Apka nie korzysta z Huawei Health Kit. Czyta treningi z **Health Connect**, a lokalnie
zapisuje je w bazie Room, żeby historia nie znikała.

## Konfiguracja

1. Na telefonie zainstaluj **Huawei Health** i sparuj zegarek.
2. Zainstaluj **Health Sync** (Google Play) i ustaw synchronizację Huawei Health → Health Connect
   dla treningów (pływanie, rower, trenażer), tętna, dystansu i kalorii.
3. Upewnij się, że **Health Connect** jest zainstalowany (od Androida 14 jest częścią systemu).
4. Uruchom trening-tracker i nadaj uprawnienia do odczytu danych z Health Connect.

Health Connect domyślnie udostępnia dane z 30 dni przed nadaniem uprawnień. Starszą historię
odblokowuje dodatkowe uprawnienie „dostęp do historii”, o które apka poprosi.

## Budowanie

Wymagania: JDK 17+ i Android SDK (platforma 37).

```
./gradlew assembleDebug   # APK w app/build/outputs/apk/debug/
./gradlew test            # testy jednostkowe
```

## Licencja

MIT, zobacz [`LICENSE`](LICENSE).
