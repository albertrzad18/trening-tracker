package io.github.albertrzad18.treningtracker.ui.charts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.albertrzad18.treningtracker.ui.common.BigValue
import io.github.albertrzad18.treningtracker.ui.common.Panel
import io.github.albertrzad18.treningtracker.ui.format.ValueUnit
import io.github.albertrzad18.treningtracker.ui.theme.LocalTtColors
import io.github.albertrzad18.treningtracker.ui.theme.TtType

/**
 * Panel wykresu na Statystykach: tytuł, jedno zdanie wyjaśnienia, opcjonalny przełącznik,
 * kluczowa liczba i wykres. Kluczowa liczba zmienia się po dotknięciu słupka albo punktu.
 */
@Composable
fun StatPanel(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    value: ValueUnit? = null,
    toggle: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val colors = LocalTtColors.current
    Panel(modifier) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(TtSpaceGap)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = TtType.title, color = colors.ink)
                Text(subtitle, style = TtType.bodyS, color = colors.ink2)
            }
            toggle?.invoke()
            if (value != null) {
                BigValue(value, style = TtType.hero.copy(fontSize = 36.sp, lineHeight = 40.sp))
            }
            content()
        }
    }
}

private val TtSpaceGap = 12.dp
