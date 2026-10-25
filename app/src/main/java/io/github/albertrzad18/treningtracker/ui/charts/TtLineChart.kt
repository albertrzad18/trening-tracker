package io.github.albertrzad18.treningtracker.ui.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.albertrzad18.treningtracker.ui.common.CaptionText
import io.github.albertrzad18.treningtracker.ui.theme.LocalTtColors
import io.github.albertrzad18.treningtracker.ui.theme.TtMotion
import io.github.albertrzad18.treningtracker.ui.theme.TtType
import io.github.albertrzad18.treningtracker.ui.theme.ttAnimationsEnabled

/**
 * Jedna seria wykresu: punkty `x do y`, kolor, grubość i opcjonalne wypełnienie pod linią.
 * Z [dotsOnly] rysujemy same punkty (pojedyncze treningi na tle średniej kroczącej),
 * a [endLabel] podpisuje koniec linii, zamiast legendy pod wykresem.
 */
data class LineSpec(
    val points: List<Pair<Double, Double>>,
    val color: Color,
    val width: Dp = 2.5.dp,
    val areaAlpha: Float = 0f,
    val endDot: Boolean = false,
    val endLabel: String? = null,
    val dotsOnly: Boolean = false,
)

/** Pozioma linia odniesienia, np. średnia prędkość z opisem. */
data class ReferenceLine(val value: Double, val label: String?)

/**
 * Wykres liniowy rysowany na `Canvas`: bez osi i bez siatki, z opcjonalnym wypełnieniem pod linią.
 * Linia odsłania się od lewej w 450 ms. Dotknięcie wykresu wybiera najbliższy punkt
 * (pionowa linia i kropka 6 dp), a wartość pokazuje nagłówek panelu.
 */
@Composable
fun TtLineChart(
    lines: List<LineSpec>,
    modifier: Modifier = Modifier,
    height: Dp = 146.dp,
    reference: ReferenceLine? = null,
    xLabels: List<String> = emptyList(),
    selectedIndex: Int? = null,
    onSelect: ((Int?) -> Unit)? = null,
    zeroBased: Boolean = false,
) {
    val colors = LocalTtColors.current
    val measurer = rememberTextMeasurer()
    val animated = ttAnimationsEnabled()
    val progress = remember { Animatable(if (animated) 0f else 1f) }
    LaunchedEffect(lines) {
        if (animated) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(TtMotion.CHART, easing = FastOutSlowInEasing))
        }
    }

    val allY = lines.flatMap { line -> line.points.map { it.second } } + listOfNotNull(reference?.value)
    val minY = if (zeroBased) 0.0 else (allY.minOrNull() ?: 0.0)
    val maxY = allY.maxOrNull() ?: 1.0
    val spanY = (maxY - minY).takeIf { it > 0.0 } ?: 1.0
    val allX = lines.flatMap { line -> line.points.map { it.first } }
    val minX = allX.minOrNull() ?: 0.0
    val maxX = allX.maxOrNull() ?: 1.0
    val spanX = (maxX - minX).takeIf { it > 0.0 } ?: 1.0
    val main = lines.firstOrNull()?.points.orEmpty()

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
                .then(
                    if (onSelect == null) {
                        Modifier
                    } else {
                        Modifier.pointerInput(main.size) {
                            detectTapGestures { offset ->
                                if (main.isEmpty()) return@detectTapGestures
                                val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                val x = minX + fraction * spanX
                                val index = main.indices.minByOrNull { kotlin.math.abs(main[it].first - x) }
                                onSelect(if (index == selectedIndex) null else index)
                            }
                        }
                    },
                ),
        ) {
            val padding = 6.dp.toPx()
            val usableHeight = size.height - padding * 2
            fun px(point: Pair<Double, Double>) = Offset(
                x = (((point.first - minX) / spanX) * size.width).toFloat(),
                y = (padding + (1.0 - (point.second - minY) / spanY) * usableHeight).toFloat(),
            )

            reference?.let { line ->
                val y = px(minX to line.value).y
                drawLine(colors.ink2, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                line.label?.let { label ->
                    drawText(
                        textMeasurer = measurer,
                        text = label,
                        style = TtType.caption.copy(color = colors.ink2),
                        topLeft = Offset(0f, (y - 18.dp.toPx()).coerceAtLeast(0f)),
                    )
                }
            }

            lines.forEach { spec ->
                if (spec.points.size < 2) return@forEach
                val offsets = spec.points.map(::px)
                if (spec.dotsOnly) {
                    offsets.forEach { drawCircle(spec.color, radius = 2.dp.toPx(), center = it) }
                    return@forEach
                }
                if (spec.areaAlpha > 0f) {
                    val area = Path().apply {
                        moveTo(offsets.first().x, size.height)
                        offsets.forEach { lineTo(it.x, it.y) }
                        lineTo(offsets.last().x, size.height)
                        close()
                    }
                    clipRect(right = size.width * progress.value) {
                        drawPath(area, spec.color.copy(alpha = spec.areaAlpha))
                    }
                }
                val path = Path().apply {
                    moveTo(offsets.first().x, offsets.first().y)
                    offsets.drop(1).forEach { lineTo(it.x, it.y) }
                }
                clipRect(right = size.width * progress.value) {
                    drawPath(
                        path,
                        spec.color,
                        style = Stroke(
                            width = spec.width.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                }
                if (spec.endDot && progress.value > 0.99f) {
                    drawCircle(spec.color, radius = 4.dp.toPx(), center = offsets.last())
                }
                if (spec.endLabel != null && progress.value > 0.99f) {
                    val layout = measurer.measure(
                        spec.endLabel,
                        TtType.caption.copy(color = spec.color, fontWeight = FontWeight.SemiBold),
                    )
                    val last = offsets.last()
                    drawText(
                        layout,
                        topLeft = Offset(
                            x = (last.x - layout.size.width).coerceIn(0f, size.width - layout.size.width),
                            y = (last.y - layout.size.height - 4.dp.toPx()).coerceIn(0f, size.height - layout.size.height),
                        ),
                    )
                }
            }

            selectedIndex?.let { index ->
                main.getOrNull(index)?.let { point ->
                    val offset = px(point)
                    drawLine(colors.ink, Offset(offset.x, 0f), Offset(offset.x, size.height), 1.dp.toPx())
                    drawCircle(colors.ink, radius = 3.dp.toPx(), center = offset)
                }
            }
        }
        if (xLabels.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                xLabels.forEach { CaptionText(it) }
            }
        }
    }
}
