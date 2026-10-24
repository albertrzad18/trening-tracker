# Ściągawka: Kotlin i Android

Notatki do powtórki, uporządkowane tematycznie. Każde nowe pojęcie z projektu trafia tutaj.

## Gradle

### Gradle i pliki `build.gradle.kts`
Gradle to narzędzie, które buduje apkę: pobiera biblioteki, kompiluje kod i pakuje APK.
Konfigurację piszemy w Kotlinie (`.kts` = Kotlin Script). W projekcie są dwa pliki build:
`build.gradle.kts` w głównym folderze (wtyczki wspólne dla całości) i `app/build.gradle.kts`
(ustawienia samej apki, np. `minSdk` i lista zależności).

```kotlin
// app/build.gradle.kts
android {
    namespace = "io.github.albertrzad18.treningtracker"
    defaultConfig { minSdk = 28 }
}
```

### Version catalog (`gradle/libs.versions.toml`)
Jedno miejsce z wersjami wszystkich bibliotek i wtyczek. Zamiast pisać wersję w kilku plikach,
odwołujemy się do niej po nazwie: `libs.androidx.room.runtime`. Zmiana wersji to edycja jednej linii.

```kotlin
implementation(libs.androidx.room.runtime)
```

### `gradlew` (wrapper)
Skrypt, który sam pobiera właściwą wersję Gradle zapisaną w `gradle/wrapper/gradle-wrapper.properties`.
Dzięki temu każdy buduje projekt tą samą wersją, bez instalowania Gradle.

### `local.properties`
Plik tylko na Twoim komputerze (jest w `.gitignore`), z ścieżką do Android SDK: `sdk.dir=...`.
Android Studio tworzy go samo. Gdy ścieżka jest błędna, build kończy się dziwnym błędem o niepoprawnej nazwie pliku.

## Android

### `AndroidManifest.xml`
Opis apki dla systemu: nazwa, ekrany (`<activity>`), uprawnienia. Ekran z filtrem `MAIN` + `LAUNCHER`
jest tym, który otwiera się po kliknięciu ikony.

### `strings.xml`
Wszystkie teksty interfejsu (u nas po polsku) w `res/values/strings.xml`. W kodzie czytamy je przez
`stringResource(R.string.hello)`, więc tekst nie siedzi na sztywno w kodzie.

## Kotlin

### `val` i `var`
`val` to wartość, której nie da się przypisać ponownie. `var` to zmienna. Domyślnie używamy `val`.

```kotlin
val PoolBlue = Color(0xFF1E88E5)   // kolor basenu, nie zmienia się
```

## Compose

### `@Composable`
Funkcja z tą adnotacją opisuje fragment interfejsu. Nie zwraca widoku, tylko go „rysuje”.
Można ją wywołać tylko z innej funkcji `@Composable`.

```kotlin
@Composable
fun HelloScreen() { Text("Cześć") }
```

### Motyw Material 3
`TreningTrackerTheme { ... }` ustawia kolory i czcionki dla wszystkiego w środku. Na Androidzie 12+
bierze kolory z tapety (dynamic color), a na starszych ma własny jasny i ciemny zestaw.

## Kotlin: dane i null

### `data class`
Klasa do przechowywania danych. Kotlin sam dodaje `equals`, `toString` i `copy`. U nas: `Workout` (model
treningu w `domain/Workout.kt`) i `WorkoutEntity` (wiersz w bazie).

```kotlin
data class Totals(val count: Int, val duration: Duration, val distanceM: Double?, val caloriesKcal: Double?)
```

### Null-safety: `?`, `?.`, `?:`
Typ z `?` (np. `Double?`) może być `null`, bez `?` nigdy. U nas `distanceM: Double?` jest `null` na trenażerze,
a UI wtedy pole ukrywa. `?.` wywołuje coś tylko gdy wartość nie jest `null` (`distanceM?.let { ... }`),
a `?:` podaje wartość zastępczą (`avgSpeedMps?.let { it * 3.6 } ?: policzZDystansu`).

### Extension functions
Funkcja „doklejona” do istniejącego typu. `fun List<Workout>.totals()` pozwala napisać `treningi.totals()`
(w `domain/Aggregations.kt`), mimo że `List` nie jest naszą klasą.

### Lambdy i funkcje na listach
`filter { ... }`, `map { ... }`, `sumOf { ... }` biorą mały kawałek kodu (lambdę) i stosują go do każdego elementu.
`it` to domyślna nazwa elementu. Przykład: `filter { it.discipline == discipline }`.

## Room (baza danych)

### `@Entity`, `@Dao`, `@Database`
Trzy elementy Rooma w `data/db/`: **`@Entity`** (`WorkoutEntity`) opisuje tabelę, **`@Dao`** (`WorkoutDao`) to lista
zapytań jako metody, a **`@Database`** (`AppDatabase`) spina wszystko w bazę. Room na podstawie adnotacji sam
generuje kod SQL (to robi KSP przy budowaniu).

```kotlin
@Query("SELECT * FROM workouts ORDER BY startMillis DESC")
fun observeAll(): Flow<List<WorkoutEntity>>
```

Dodanie nowego pola do bazy = dopisanie go w `WorkoutEntity`, **podbicie `version`** w `@Database` i napisanie migracji,
żeby dane na telefonie nie zniknęły. Schemat każdej wersji zapisuje się w `app/schemas/`.

### `suspend` i coroutines
`suspend fun` to funkcja, która może „poczekać” (np. na bazę) bez blokowania ekranu. Wywołuje się ją z coroutine,
np. `viewModelScope.launch { ... }`. U nas: `suspend fun upsertAll(...)` w `WorkoutDao`.

### `Flow`
Strumień wartości, który wysyła nową listę za każdym razem, gdy dane w bazie się zmienią. Ekran dzięki temu
odświeża się sam po synchronizacji. U nas `observeAll(): Flow<List<WorkoutEntity>>`.

### `@Upsert`
„Wstaw lub nadpisz”: jeśli trening o tym `id` już jest, zostanie zaktualizowany, jeśli nie, dodany.
Dzięki temu ponowna synchronizacja nie tworzy duplikatów.

## Architektura apki

### ViewModel i `StateFlow`
`ViewModel` trzyma stan ekranu i przeżywa obrót telefonu. Ekran nie czyta bazy sam, tylko pyta ViewModel.
U nas `DashboardViewModel` zamienia `Flow` z bazy na `StateFlow` (`stateIn`), który ekran obserwuje przez
`collectAsStateWithLifecycle()`. Gdy dane w Room się zmienią, ekran odświeża się sam.

```kotlin
val state: StateFlow<DashboardData?> = dao.observeAll().map { ... }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
```

### `@Composable` w praktyce: stan i `remember`
Compose rysuje ekran na nowo, gdy zmieni się stan, który czyta. `by` w `val data by vm.state.collectAsStateWithLifecycle()`
zamienia strumień w zwykłą wartość. `remember` zapamiętuje obiekt między przerysowaniami (np. `rememberScrollState()`).

### Nawigacja (`NavHost`)
`AppRoot.kt` definiuje ekrany jako trasy tekstowe (`"dashboard"`, `"workout/{id}"`). `nav.navigate("workout/abc")`
otwiera szczegóły, a `{id}` czyta `SavedStateHandle` w `WorkoutDetailViewModel`. Dolny pasek to `NavigationBar`.
Nowy ekran = nowy `composable("trasa") { ... }` w `NavHost` plus (jeśli ma być w pasku) pozycja w `topLevel`.

### WorkManager
Zadania w tle, które system uruchamia nawet gdy apka jest zamknięta. `SyncWorker` robi synchronizację,
`SyncScheduler` planuje ją co 6 godzin (`PeriodicWorkRequest`) albo od razu (`OneTimeWorkRequest`).

### Changes API (Health Connect)
Zamiast czytać wszystko za każdym razem, bierzemy token i pytamy tylko o zmiany od ostatniego razu
(`SyncRepository`). Token jest zapisany w `SharedPreferences` (`SyncPrefs`). Gdy wygaśnie, robimy pełny odczyt.

## Wykresy (Etap 2)

### Vico: `CartesianChartHost`
Vico rysuje wykresy słupkowe i liniowe. Trzy elementy: **model** (dane), **chart** (warstwy i osie), **host** (to, co widać).
Dane wkładamy do `CartesianChartModelProducer` w `runTransaction` (to funkcja `suspend`, więc wołamy ją w `LaunchedEffect`).

```kotlin
LaunchedEffect(bars) {
    producer.runTransaction { columnModel { bars.forEachIndexed { i, bar -> series(bar.values, key = i) } } }
}
CartesianChartHost(chart = rememberCartesianChart(rememberColumnCartesianLayer(), bottomAxis = ...), modelProducer = producer)
```

`MergeMode.Stacked` układa słupki jeden na drugim (u nas: dyscypliny w jednym słupku miesiąca).
`CartesianValueFormatter` zamienia liczbę z osi na tekst, dzięki czemu oś pokazuje `25 h` albo `paź`, a nie `1500`.

### `LaunchedEffect`
Uruchamia kod `suspend` wtedy, gdy ekran się pojawi albo gdy zmieni się klucz podany w nawiasie.
`LaunchedEffect(bars)` znaczy „przelicz wykres, gdy zmienią się dane”. Bez klucza kod poszedłby raz i nigdy więcej.

### Rysowanie samemu: `Canvas` i `drawRoundRect`
Heatmapa i pierścień dyscyplin nie są wykresem Vico, tylko własnym rysunkiem.
`Canvas { }` daje powierzchnię, na której liczymy piksele (`cell.toPx()`) i rysujemy kształty: `drawRoundRect`, `drawArc`.
`BoxWithConstraints` podaje dostępną szerokość (`maxWidth`), więc można policzyć rozmiar kwadratu: szerokość podzielona przez liczbę tygodni.

### `LazyColumn`: `item` i `items`
`LazyColumn` tworzy tylko te elementy, które widać. `item { }` to jeden element, `items(lista) { }` to wiele.
`key` pomaga Compose rozpoznać element po zmianie listy (u nas `key = { it.id }`).

### `plurals` (liczba mnoga)
Polski ma inne formy dla 1, 2–4 i 5+. Zamiast `"%d dni"` używamy `<plurals name="days">` w `strings.xml`
i `pluralStringResource(R.plurals.days, n, n)`, więc wychodzi „1 dzień”, ale „5 dni”.

### `BuildConfig.DEBUG`
Stała generowana przez Gradle: `true` w buildzie debug, `false` w release. Używamy jej, żeby przyciski
„Wczytaj dane demo” były tylko u nas, a nie u kogoś, kto dostanie APK. Wymaga `buildFeatures { buildConfig = true }`.
