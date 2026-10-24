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
