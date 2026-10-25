package io.github.albertrzad18.treningtracker.ui.theme

import androidx.compose.runtime.Composable

/**
 * Motyw apki. Dynamic color (Material You) jest **wyłączony**: paleta jest stała,
 * jasna albo ciemna, i siedzi w [TtTheme] razem z typografią, kształtami i odstępami.
 */
@Composable
fun TreningTrackerTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    TtTheme(dark = darkTheme, content = content)
}
