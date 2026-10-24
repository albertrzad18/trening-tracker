# trening-tracker

An Android app that shows workouts recorded by a Huawei Watch GT5: pool swimming, outdoor cycling
and indoor trainer rides. Tables, workout details, charts, summaries, goals, personal records and route maps.

All data stays on the phone. There is no server and no accounts. The app UI is in Polish.

> Work in progress. Feature plan (in Polish): [`docs/PLAN.md`](docs/PLAN.md).

## Where the data comes from

```
Watch → Huawei Health → Health Sync → Health Connect → trening-tracker
```

The app does not use Huawei Health Kit. It reads workouts from **Health Connect** and keeps a local
copy in a Room database, so the history is not lost.

## Setup

1. Install **Huawei Health** on your phone and pair the watch.
2. Install **Health Sync** (Google Play) and set up syncing from Huawei Health to Health Connect for
   workouts (swimming, cycling, indoor cycling), heart rate, distance and calories.
3. Make sure **Health Connect** is installed (it is part of the system since Android 14).
4. Open trening-tracker and grant it read access to Health Connect.

By default Health Connect exposes data from the 30 days before you grant permissions. To read older
history the app asks for the additional "history access" permission.

## Building

Requirements: JDK 17+ and the Android SDK (platform 37).

```
./gradlew assembleDebug   # APK in app/build/outputs/apk/debug/
./gradlew test            # unit tests
```

## License

MIT, see [`LICENSE`](LICENSE).
