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

### Liczba mnoga po polsku
Polski ma inne formy dla 1, 2–4 i 5+. Androidowe `plurals` wybierają formę **według języka telefonu**,
więc na telefonie po angielsku „6” trafia do formy `other` i wychodzi „6 okresu”.
Dlatego odmieniamy sami (`pluralPl` w `Formatters.kt`), a w `strings.xml` trzymamy tylko same słowa.

```kotlin
pluralPl(6, "okres", "okresy", "okresów") // "6 okresów"
```

### `BuildConfig.DEBUG` (było przy danych demo)
Stała generowana przez Gradle: `true` w buildzie debug, `false` w release. Używamy jej, żeby przyciski
„Wczytaj dane demo” były tylko u nas, a nie u kogoś, kto dostanie APK. Wymaga `buildFeatures { buildConfig = true }`.

## Etap 3: więcej danych i mapa

### Migracja Room
Zmiana schematu bazy wymaga podbicia `version` i napisania migracji, inaczej apka wywala się przy starcie
(„Room cannot verify the data integrity”). Migracja to zwykły SQL; my w wersji 2 tylko dokładamy tabele,
więc zapisane treningi zostają nietknięte.

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("CREATE TABLE IF NOT EXISTS ...") }
}
Room.databaseBuilder(...).addMigrations(MIGRATION_1_2).build()
```

Najprostszy sposób na poprawny SQL: zbudować projekt i przepisać `createSql` z `app/schemas/....AppDatabase/2.json`.
Room porównuje schemat po migracji z tym plikiem, więc błąd od razu wychodzi przy uruchomieniu.

### Klucz obcy i `CASCADE`
`@ForeignKey(..., onDelete = CASCADE)` w `SampleEntity` mówi bazie: skasuj próbki razem z treningiem.
Dzięki temu nie trzeba o tym pamiętać w kodzie.

### `@Transaction`
Metoda DAO oznaczona `@Transaction` wykonuje kilka zapytań „wszystko albo nic”.
U nas `replaceSamples` najpierw kasuje stare próbki, potem wstawia nowe — przerwanie w połowie zostawiłoby pustkę.

### `AndroidView`: stary widok w Compose
Mapa (osmdroid) to zwykły `View` z Androida, nie Compose. `AndroidView(factory = { MapView(it) }, update = { ... })`
tworzy go raz i pozwala aktualizować przy zmianie danych. `factory` to tworzenie, `update` to odświeżanie.
Taki widok sam się nie przycina do swojego miejsca, dlatego dokładamy `Modifier.clip(...)`.

### Ustawienia w `SharedPreferences` + `StateFlow`
`AppSettings` czyta i zapisuje proste wartości (tętno maksymalne, długość basenu, motyw) i wystawia je jako
`StateFlow`, więc ekrany odświeżają się same, tak samo jak przy danych z Room.

### `FlowRow`
Jak `Row`, ale zawija elementy do następnej linii, gdy zabraknie miejsca. Używamy w oknie dodawania celu,
żeby chipy nie uciekały poza ekran.

## Etap 4: wydanie

### Ikona adaptacyjna
Ikona Androida 8+ ma dwie warstwy (tło i to, co na nim) rysowane jako `vector drawable`, czyli ścieżki,
a nie obrazek. Launcher sam przycina je do kształtu, jakiego używa system. Plik `mipmap-anydpi-v26/ic_launcher.xml`
spina warstwy, a `<monochrome>` daje wersję jednokolorową pod motywowane ikony.

### Podpisywanie APK
Każdy APK musi być podpisany kluczem z pliku keystore. Android pozwala na aktualizację apki tylko tym samym
kluczem, więc keystore trzeba zachować (kopia zapasowa!). Hasła trzymamy w `keystore.properties` poza repo
(wpis w `.gitignore`), a `build.gradle.kts` czyta je przy budowaniu wydania.

## Redesign: motyw i wykresy

### `CompositionLocal` (`LocalTtColors`)
Sposób na przekazanie wartości (u nas palety kolorów) w dół drzewa composable bez podawania jej jako parametr każdej funkcji. Motyw ustawia ją raz w `TtTheme`, a ekran czyta `LocalTtColors.current.ink`.

### `Canvas`
Composable do rysowania własnych kształtów: `drawCircle`, `drawRoundRect`, `drawPath`. Na nim zrobiliśmy wszystkie wykresy (`TtBarChart`, `TtLineChart`), więc mamy pełną kontrolę nad wyglądem.

### Stateless composable i `@Preview`
Ekran dzielimy na wersję z ViewModelem (`DashboardScreen`) i wersję tylko z danymi w parametrach (`DashboardContent`). Tę drugą można pokazać w `@Preview` z danymi przykładowymi, bez bazy.
