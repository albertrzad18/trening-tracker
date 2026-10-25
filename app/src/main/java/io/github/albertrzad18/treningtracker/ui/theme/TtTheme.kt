package io.github.albertrzad18.treningtracker.ui.theme

import android.provider.Settings
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.albertrzad18.treningtracker.R
import io.github.albertrzad18.treningtracker.domain.Discipline

/**
 * Tokeny kierunku „Panel”: stała paleta jasna i ciemna (bez dynamic color),
 * typografia IBM Plex Sans z cyframi tabelarycznymi, kształty, odstępy i czasy animacji.
 *
 * Kolory trzymamy w osobnym obiekcie [TtColors], a nie tylko w `MaterialTheme.colorScheme`,
 * bo apka ma własne role (`ink2`, `track`, `line`, kolory dyscyplin), których Material 3 nie zna.
 */
@Immutable
data class TtColors(
    val bg: Color,
    val surface: Color,
    val track: Color,
    val line: Color,
    val ink: Color,
    val ink2: Color,
    val accent: Color,
    val onAccent: Color,
    val pool: Color,
    val road: Color,
    val trainer: Color,
    val error: Color,
    val isDark: Boolean,
) {
    /** Kolor dyscypliny: basen niebieski, rower zielony, trenażer pomarańczowy. */
    fun of(discipline: Discipline): Color = when (discipline) {
        Discipline.POOL -> pool
        Discipline.ROAD -> road
        Discipline.TRAINER -> trainer
    }

    /** Tło kontenera dyscypliny: jej kolor rozcieńczony na tle ekranu (zaznaczony chip, obszar wykresu). */
    fun container(discipline: Discipline): Color =
        of(discipline).copy(alpha = if (isDark) 0.22f else 0.14f).compositeOver(bg)

    /** Strefy tętna 1–5: akcent z kryciem 20/40/60/80/100% na tle. */
    fun zone(index: Int): Color = accent.copy(alpha = index.coerceIn(1, 5) / 5f).compositeOver(bg)

    /** Nakładka dotyku: ink z kryciem 8%. */
    val press: Color get() = ink.copy(alpha = 0.08f)
}

val TtLight = TtColors(
    bg = Color(0xFFEEF0F3),
    surface = Color(0xFFFFFFFF),
    track = Color(0xFFE3E6EB),
    line = Color(0xFFD9DDE3),
    ink = Color(0xFF0F1216),
    ink2 = Color(0xFF59616E),
    accent = Color(0xFFB8326A),
    onAccent = Color(0xFFFFFFFF),
    pool = Color(0xFF2F6BFF),
    road = Color(0xFF1E9E5A),
    trainer = Color(0xFFE5730B),
    error = Color(0xFFC4372B),
    isDark = false,
)

val TtDark = TtColors(
    bg = Color(0xFF0D0F12),
    surface = Color(0xFF171A1F),
    track = Color(0xFF22262D),
    line = Color(0xFF2A2F37),
    ink = Color(0xFFE8EBEF),
    ink2 = Color(0xFF97A0AD),
    accent = Color(0xFFF07AA6),
    onAccent = Color(0xFF1A0A11),
    pool = Color(0xFF6E9BFF),
    road = Color(0xFF4CC985),
    trainer = Color(0xFFFF9E4A),
    error = Color(0xFFFF7A6B),
    isDark = true,
)

val LocalTtColors = staticCompositionLocalOf { TtLight }

/**
 * IBM Plex Sans jako jeden plik ze zmienną grubością (variable font).
 * `FontVariation` ustawia osi `wght`, więc 400/500/600 bierzemy z tego samego pliku.
 */
val PlexSans: FontFamily = FontFamily(
    Font(R.font.ibm_plex_sans, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.ibm_plex_sans, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.ibm_plex_sans, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
)

/** IBM Plex Mono: tylko tam, gdzie liczy się stała szerokość znaku (nazwy typów w diagnostyce). */
val PlexMono: FontFamily = FontFamily(Font(R.font.ibm_plex_mono, FontWeight.Normal))

/** Styl tekstu z cyframi tabelarycznymi (`tnum`): liczby w kolumnach nie skaczą. */
object TtType {
    private fun style(size: Int, line: Int, weight: FontWeight, letterSpacing: Float = 0f) = TextStyle(
        fontFamily = PlexSans,
        fontSize = size.sp,
        lineHeight = line.sp,
        fontWeight = weight,
        letterSpacing = letterSpacing.sp,
        fontFeatureSettings = "tnum",
    )

    /** `1 027` na Pulpicie. */
    val heroXL = style(64, 64, FontWeight.SemiBold, -1.3f)

    /** Czas i dystans w szczegółach, suma okresu. */
    val hero = style(44, 48, FontWeight.SemiBold, -0.7f)
    val headline = style(24, 30, FontWeight.SemiBold, -0.24f)
    val value = style(20, 26, FontWeight.SemiBold, -0.2f)
    val title = style(16, 22, FontWeight.SemiBold)
    val body = style(15, 22, FontWeight.Normal)
    val bodyS = style(13, 18, FontWeight.Normal)

    /** WERSALIKI: tekst podajemy już wielkimi literami. */
    val label = style(12, 16, FontWeight.Medium, 0.7f)

    /** Ten sam stopień co [label], ale bez wersalików: segmenty, chipy, podpisy nawigacji. */
    val labelPlain = style(12, 16, FontWeight.Medium, 0.1f)

    /** Osie wykresów i daty pomocnicze. */
    val caption = style(12, 14, FontWeight.Medium, 0.1f)

    /** Nazwy typów danych w diagnostyce. */
    val mono = TextStyle(fontFamily = PlexMono, fontSize = 13.sp, lineHeight = 18.sp, fontFeatureSettings = "tnum")
}

object TtShape {
    val panel = RoundedCornerShape(20.dp)
    val map = RoundedCornerShape(16.dp)
    val sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val field = RoundedCornerShape(12.dp)
    val full = RoundedCornerShape(percent = 50)
}

object TtSpace {
    val x1 = 4.dp
    val x2 = 8.dp
    val x3 = 12.dp
    val x4 = 16.dp
    val x5 = 20.dp
    val x6 = 24.dp
    val x8 = 32.dp
    val x12 = 48.dp
    val screenMargin = 16.dp
    val panelGap = 12.dp
    val panelPadding = 16.dp
    val minTouch = 48.dp
    val minRow = 56.dp
}

/** Czasy animacji w ms. Krzywa zawsze `FastOutSlowInEasing`, bez sprężyn i odbić. */
object TtMotion {
    const val FAST = 150
    const val BASE = 250
    const val CHART = 450
    const val RING = 600
    const val SKELETON = 1200
}

/**
 * Czy system pozwala animować. Gdy w ustawieniach telefonu animacje są wyłączone
 * (`ANIMATOR_DURATION_SCALE` = 0), zwracamy `false` i wszystkie czasy zerujemy.
 */
@Composable
fun ttAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
        }.getOrDefault(true)
    }
}

/** Czas animacji z poszanowaniem systemowego „usuń animacje”. */
@Composable
fun ttDuration(millis: Int): Int = if (ttAnimationsEnabled()) millis else 0

/** Typografia Material 3 na naszych stylach: gotowe komponenty (przyciski, arkusze) też mają Plex i `tnum`. */
private fun ttTypography() = Typography(
    displayLarge = TtType.heroXL,
    displayMedium = TtType.hero,
    displaySmall = TtType.hero,
    headlineLarge = TtType.headline,
    headlineMedium = TtType.headline,
    headlineSmall = TtType.headline,
    titleLarge = TtType.value,
    titleMedium = TtType.title,
    titleSmall = TtType.title,
    bodyLarge = TtType.body,
    bodyMedium = TtType.body,
    bodySmall = TtType.bodyS,
    labelLarge = TtType.title,
    labelMedium = TtType.caption,
    labelSmall = TtType.caption,
)

/** Mapowanie naszej palety na role Material 3, żeby gotowe komponenty miały właściwe kolory. */
private fun TtColors.toColorScheme() = (if (isDark) darkColorScheme() else lightColorScheme()).copy(
    primary = accent,
    onPrimary = onAccent,
    primaryContainer = accent.copy(alpha = 0.18f).compositeOver(surface),
    onPrimaryContainer = ink,
    secondary = accent,
    onSecondary = onAccent,
    background = bg,
    onBackground = ink,
    surface = surface,
    onSurface = ink,
    surfaceVariant = track,
    onSurfaceVariant = ink2,
    surfaceContainer = surface,
    surfaceContainerHigh = surface,
    surfaceContainerHighest = surface,
    surfaceContainerLow = surface,
    surfaceContainerLowest = surface,
    inverseSurface = ink,
    inverseOnSurface = bg,
    outline = line,
    outlineVariant = line,
    error = error,
    onError = onAccent,
    scrim = ink.copy(alpha = 0.32f),
)

@Composable
fun TtTheme(dark: Boolean, content: @Composable () -> Unit) {
    val colors = if (dark) TtDark else TtLight
    CompositionLocalProvider(LocalTtColors provides colors) {
        MaterialTheme(
            colorScheme = colors.toColorScheme(),
            typography = ttTypography(),
            content = content,
        )
    }
}
