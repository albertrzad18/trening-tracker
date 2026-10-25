package io.github.albertrzad18.treningtracker.ui.theme

import androidx.compose.runtime.Composable
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme

/**
 * Motyw apki. Dynamic color (Material You) jest **wyłączony**: paleta jest stała,
 * jasna albo ciemna, i siedzi w [TtTheme] razem z typografią i odstępami.
 */
@Composable
fun TreningTrackerTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    TtTheme(dark = darkTheme) {
        // Wykresy (Vico) biorą kolory osi i opisów z motywu Material 3, czyli z naszej palety.
        ProvideVicoTheme(rememberM3VicoTheme(), content)
    }
}
