package pl.treningtracker.ui.theme

// Tokeny kierunku B „Panel”. Dynamic color (Material You) WYŁĄCZONY — stała paleta.
// Wartości z makiety HTML (source/TT Phone.dc.html, klasy .tt-b.tt-light / .tt-b.tt-dark).

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

@Immutable
data class TtColors(
    val bg: Color, val surface: Color, val track: Color, val line: Color,
    val ink: Color, val ink2: Color, val accent: Color, val onAccent: Color,
    val pool: Color, val road: Color, val trainer: Color, val error: Color,
    val isDark: Boolean,
) {
    // tło zaznaczonego chipu dyscypliny / profilu wysokości: kolor dyscypliny rozcieńczony na bg
    fun poolContainer() = pool.copy(alpha = if (isDark) .22f else .14f).compositeOver(bg)
    fun roadContainer() = road.copy(alpha = if (isDark) .22f else .14f).compositeOver(bg)
    fun trainerContainer() = trainer.copy(alpha = if (isDark) .22f else .14f).compositeOver(bg)
    // strefy tętna 1..5: akcent 20/40/60/80/100% na tle
    fun zone(n: Int): Color = accent.copy(alpha = n / 5f).compositeOver(bg)
}
private fun Color.compositeOver(bg: Color) = androidx.compose.ui.graphics.compositeOver(this, bg)

val TtLight = TtColors(
    bg = Color(0xFFEEF0F3), surface = Color(0xFFFFFFFF), track = Color(0xFFE3E6EB), line = Color(0xFFD9DDE3),
    ink = Color(0xFF0F1216), ink2 = Color(0xFF59616E), accent = Color(0xFFB8326A), onAccent = Color(0xFFFFFFFF),
    pool = Color(0xFF2F6BFF), road = Color(0xFF1E9E5A), trainer = Color(0xFFE5730B), error = Color(0xFFC4372B), isDark = false,
)
val TtDark = TtColors(
    bg = Color(0xFF0D0F12), surface = Color(0xFF171A1F), track = Color(0xFF22262D), line = Color(0xFF2A2F37),
    ink = Color(0xFFE8EBEF), ink2 = Color(0xFF97A0AD), accent = Color(0xFFF07AA6), onAccent = Color(0xFF1A0A11),
    pool = Color(0xFF6E9BFF), road = Color(0xFF4CC985), trainer = Color(0xFFFF9E4A), error = Color(0xFFFF7A6B), isDark = true,
)

val LocalTtColors = staticCompositionLocalOf { TtLight }

// Czcionka: IBM Plex Sans (400/500/600) + IBM Plex Mono — dodać do res/font z zestawem latin-ext (polskie znaki).
// val PlexSans = FontFamily(Font(R.font.ibm_plex_sans_regular), Font(R.font.ibm_plex_sans_medium, FontWeight.Medium), Font(R.font.ibm_plex_sans_semibold, FontWeight.SemiBold))
val PlexSans: FontFamily = FontFamily.Default // TODO: podmienić na IBM Plex Sans

// Cyfry tabelaryczne wszędzie: fontFeatureSettings = "tnum"
object TtType {
    private fun s(size: Int, line: Int, w: FontWeight, ls: Float = 0f) =
        TextStyle(fontFamily = PlexSans, fontSize = size.sp, lineHeight = line.sp, fontWeight = w, letterSpacing = ls.sp, fontFeatureSettings = "tnum")
    val heroXL = s(64, 64, FontWeight.SemiBold, -1.3f)   // „1 027” na Pulpicie
    val hero = s(44, 48, FontWeight.SemiBold, -0.7f)     // czas/dystans w szczegółach, suma okresu
    val headline = s(24, 30, FontWeight.SemiBold)        // tytuł ekranu
    val value = s(20, 26, FontWeight.SemiBold)           // wartości, rekordy
    val title = s(16, 22, FontWeight.SemiBold)           // tytuł sekcji
    val body = s(15, 22, FontWeight.Normal)
    val bodyS = s(13, 18, FontWeight.Normal)
    val label = s(12, 16, FontWeight.Medium, 0.7f)       // WERSALIKI (uppercase()) +6%
    val caption = s(12, 14, FontWeight.Medium, 0.1f)     // osie, daty pomocnicze (ten sam krój, nie mono)
}

object TtShape {
    val panel = RoundedCornerShape(20.dp)
    val map = RoundedCornerShape(16.dp)
    val sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val field = RoundedCornerShape(12.dp)
    val full = RoundedCornerShape(percent = 50)
}
object TtSpace { val x1 = 4.dp; val x2 = 8.dp; val x3 = 12.dp; val x4 = 16.dp; val x5 = 20.dp; val x6 = 24.dp; val x8 = 32.dp; val x12 = 48.dp
    val screenMargin = 16.dp; val panelGap = 12.dp; val panelPadding = 16.dp; val minTouch = 48.dp; val minRow = 56.dp }
object TtMotion { const val fast = 150; const val base = 250; const val chart = 450; const val ring = 600 } // ms, FastOutSlowInEasing, bez sprężyn

@Composable
fun TtTheme(dark: Boolean? = null, content: @Composable () -> Unit) {
    val isDark = dark ?: isSystemInDarkTheme()
    val c = if (isDark) TtDark else TtLight
    val scheme = (if (isDark) darkColorScheme() else lightColorScheme()).copy(
        primary = c.accent, onPrimary = c.onAccent, background = c.bg, onBackground = c.ink,
        surface = c.surface, onSurface = c.ink, surfaceContainer = c.surface, surfaceVariant = c.track,
        onSurfaceVariant = c.ink2, outlineVariant = c.line, outline = c.line, error = c.error,
    )
    CompositionLocalProvider(LocalTtColors provides c) { MaterialTheme(colorScheme = scheme, content = content) }
}
