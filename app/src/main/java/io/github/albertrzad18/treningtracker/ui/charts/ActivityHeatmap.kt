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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.albertrzad18.treningtracker.R
import io.github.albertrzad18.treningtracker.domain.Discipline
import io.github.albertrzad18.treningtracker.domain.HeatmapData
import io.github.albertrzad18.treningtracker.ui.common.CaptionText
import io.github.albertrzad18.treningtracker.ui.format.formatMonthShort
import io.github.albertrzad18.treningtracker.ui.theme.LocalTtColors
import io.github.albertrzad18.treningtracker.ui.theme.TtColors
import java.time.YearMonth

/**
 * Kalendarz aktywności: kolumna to tydzień, kółko to dzień (od poniedziałku u góry).
 * Kolor bierzemy z dyscypliny, która zajęła w danym dniu najwięcej czasu, a nasycenie
 * z długości treningu (cztery poziomy). Dzień bez treningu zostaje torem.
 */
@Composable
fun ActivityHeatmap(data: HeatmapData, modifier: Modifier = Modifier) {
    val colors = LocalTtColors.current
    val gap = 3.dp
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        MonthLabels(data, gap)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Column(Modifier.width(18.dp)) {
                // Podpisujemy co drugi dzień tygodnia, żeby kolumna była wąska.
                listOf("pn", "", "śr", "", "pt", "", "nd").forEach { label ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (label.isNotEmpty()) CaptionText(label)
                    }
                }
            }
            Cells(data, colors, gap, Modifier.weight(1f))
        }
        Legend(colors)
    }
}

@Composable
private fun MonthLabels(data: HeatmapData, gap: androidx.compose.ui.unit.Dp) {
    // Nazwa miesiąca pojawia się nad pierwszym tygodniem, który do niego należy.
    val labels = data.weeks.map { week -> week.first().date }
    Row(Modifier.fillMaxWidth().padding(start = 24.dp), horizontalArrangement = Arrangement.spacedBy(gap)) {
        var previous: YearMonth? = null
        labels.forEach { date ->
            val month = YearMonth.from(date)
            val show = month != previous
            previous = month
            Box(Modifier.weight(1f)) {
                if (show) CaptionText(formatMonthShort(month))
            }
        }
    }
}

@Composable
private fun Cells(data: HeatmapData, colors: TtColors, gap: androidx.compose.ui.unit.Dp, modifier: Modifier) {
    BoxWithConstraints(modifier) {
        val weeks = data.weeks.size.coerceAtLeast(1)
        val cell = ((maxWidth - gap * (weeks - 1)) / weeks).coerceAtMost(14.dp)
        Canvas(Modifier.fillMaxWidth().height(cell * 7 + gap * 6)) {
            val cellPx = cell.toPx()
            val step = cellPx + gap.toPx()
            data.weeks.forEachIndexed { x, week ->
                week.forEachIndexed { y, day ->
                    val color = cellColor(day.minutes, day.dominant, data.maxMinutes, colors)
                    drawCircle(
                        color = color,
                        radius = cellPx / 2f,
                        center = Offset(x * step + cellPx / 2f, y * step + cellPx / 2f),
                    )
                }
            }
        }
    }
}

/** Cztery poziomy nasycenia: 30 / 55 / 80 / 100% koloru dyscypliny na tle. */
private fun cellColor(minutes: Int, discipline: Discipline?, max: Int, colors: TtColors): Color {
    if (minutes <= 0 || discipline == null) return colors.track
    val fraction = if (max <= 0) 1f else minutes.toFloat() / max
    val alpha = when {
        fraction <= 0.25f -> 0.30f
        fraction <= 0.50f -> 0.55f
        fraction <= 0.75f -> 0.80f
        else -> 1f
    }
    return colors.of(discipline).copy(alpha = alpha).compositeOver(colors.bg)
}

@Composable
private fun Legend(colors: TtColors) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Discipline.entries.forEach { discipline ->
            Box(Modifier.size(9.dp).background(colors.of(discipline), CircleShape))
        }
        CaptionText(stringResource(R.string.heatmap_legend))
    }
}
