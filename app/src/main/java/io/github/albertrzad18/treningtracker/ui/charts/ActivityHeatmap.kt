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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.albertrzad18.treningtracker.R
import io.github.albertrzad18.treningtracker.domain.Discipline
import io.github.albertrzad18.treningtracker.domain.HeatmapData
import io.github.albertrzad18.treningtracker.ui.common.CaptionText
import io.github.albertrzad18.treningtracker.ui.format.formatMonthShort
import io.github.albertrzad18.treningtracker.ui.theme.LocalTtColors
import io.github.albertrzad18.treningtracker.ui.theme.TtColors
import java.time.YearMonth

private val LABEL_WIDTH = 24.dp
private val GAP = 3.dp
private val WEEKDAY_LABELS = listOf("pn", "", "śr", "", "pt", "", "nd")

/**
 * Kalendarz aktywności: kolumna to tydzień, kółko to dzień (od poniedziałku u góry).
 * Kolor bierzemy z dyscypliny, która zajęła w danym dniu najwięcej czasu, a nasycenie
 * z długości treningu (cztery poziomy). Dzień bez treningu zostaje torem.
 */
@Composable
fun ActivityHeatmap(data: HeatmapData, modifier: Modifier = Modifier) {
    val colors = LocalTtColors.current
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val weeks = data.weeks.size.coerceAtLeast(1)
        // Rozmiar kółka liczymy raz, żeby podpisy dni i siatka miały tę samą wysokość wierszy.
        val cell: Dp = ((maxWidth - LABEL_WIDTH - GAP * (weeks - 1)) / weeks).coerceAtMost(14.dp)
        val gridHeight = cell * 7 + GAP * 6
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MonthLabels(data, cell)
            Row {
                Column(Modifier.width(LABEL_WIDTH).height(gridHeight), verticalArrangement = Arrangement.spacedBy(GAP)) {
                    WEEKDAY_LABELS.forEach { label ->
                        Box(Modifier.height(cell), contentAlignment = Alignment.CenterStart) {
                            if (label.isNotEmpty()) CaptionText(label)
                        }
                    }
                }
                Canvas(Modifier.height(gridHeight).weight(1f)) {
                    val cellPx = cell.toPx()
                    val step = cellPx + GAP.toPx()
                    data.weeks.forEachIndexed { x, week ->
                        week.forEachIndexed { y, day ->
                            drawCircle(
                                color = cellColor(day.minutes, day.dominant, data.maxMinutes, colors),
                                radius = cellPx / 2f,
                                center = Offset(x * step + cellPx / 2f, y * step + cellPx / 2f),
                            )
                        }
                    }
                }
            }
            Legend(colors)
        }
    }
}

/** Nazwa miesiąca nad pierwszym tygodniem, który do niego należy; etykieta może wyjść poza swoją kolumnę. */
@Composable
private fun MonthLabels(data: HeatmapData, cell: Dp) {
    Row(Modifier.width(LABEL_WIDTH + (cell + GAP) * data.weeks.size)) {
        Box(Modifier.width(LABEL_WIDTH))
        var previous: YearMonth? = null
        data.weeks.forEach { week ->
            val month = YearMonth.from(week.first().date)
            val show = month != previous
            previous = month
            Box(Modifier.width(cell + GAP)) {
                if (show) {
                    CaptionText(
                        formatMonthShort(month),
                        modifier = Modifier.wrapContentWidth(align = Alignment.Start, unbounded = true),
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
