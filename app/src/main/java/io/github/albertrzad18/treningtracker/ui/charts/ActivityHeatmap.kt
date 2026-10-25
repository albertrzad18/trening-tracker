package io.github.albertrzad18.treningtracker.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.albertrzad18.treningtracker.R
import io.github.albertrzad18.treningtracker.domain.HeatmapData
import io.github.albertrzad18.treningtracker.ui.common.color

/**
 * Kalendarz aktywności: kolumna to tydzień, wiersz to dzień tygodnia (od poniedziałku u góry).
 * Im dłuższy trening, tym mocniejszy kolor; kolor bierzemy z dyscypliny pierwszego treningu dnia.
 */
@Composable
fun ActivityHeatmap(data: HeatmapData, modifier: Modifier = Modifier) {
    // Dzień bez treningu ma być widoczny jako pusty kwadrat, ale nie rzucać się w oczy.
    val emptyColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val palette = io.github.albertrzad18.treningtracker.ui.theme.LocalTtColors.current
    val gap = 2.dp
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val weeks = data.weeks.size.coerceAtLeast(1)
            val cell = ((maxWidth - gap * (weeks - 1)) / weeks).coerceAtMost(18.dp)
            Canvas(Modifier.fillMaxWidth().height(cell * 7 + gap * 6)) {
                val cellPx = cell.toPx()
                val step = cellPx + gap.toPx()
                val radius = CornerRadius(cellPx / 5f)
                data.weeks.forEachIndexed { x, week ->
                    week.forEachIndexed { y, day ->
                        val intensity = if (data.maxMinutes == 0) 0f else day.minutes.toFloat() / data.maxMinutes
                        val color = if (day.minutes == 0) {
                            emptyColor
                        } else {
                            // Najkrótsze treningi też mają być widoczne, dlatego dolny próg krycia.
                            palette.of(day.disciplines.first()).copy(alpha = 0.35f + 0.65f * intensity)
                        }
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(x * step, y * step),
                            size = Size(cellPx, cellPx),
                            cornerRadius = radius,
                        )
                    }
                }
            }
        }
        HeatmapScale(emptyColor)
    }
}

@Composable
private fun HeatmapScale(emptyColor: androidx.compose.ui.graphics.Color) {
    val sample = MaterialTheme.colorScheme.primary
    val steps = listOf(emptyColor, sample.copy(alpha = 0.35f), sample.copy(alpha = 0.6f), sample.copy(alpha = 0.85f), sample)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(R.string.heatmap_less),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        steps.forEach { Box(Modifier.size(10.dp).background(it, RoundedCornerShape(2.dp))) }
        Text(
            stringResource(R.string.heatmap_more),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
