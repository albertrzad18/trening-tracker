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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.albertrzad18.treningtracker.ui.common.CaptionText
import io.github.albertrzad18.treningtracker.ui.theme.LocalTtColors
import io.github.albertrzad18.treningtracker.ui.theme.TtMotion
import io.github.albertrzad18.treningtracker.ui.theme.TtType
import io.github.albertrzad18.treningtracker.ui.theme.ttAnimationsEnabled

/**
 * Słupki warstwowe (po jednej warstwie na dyscyplinę), rysowane na `Canvas`.
 *
 * Bez osi Y i bez siatki: skalę tłumaczy kluczowa liczba nad wykresem i etykieta nad najwyższym
 * słupkiem. Słupki są w pełni zaokrąglone, między warstwami jest przerwa 1 dp, a wejście
 * trwa 450 ms z opóźnieniem 20 ms na kolejny słupek.
 */
@Composable
fun TtBarChart(
    slots: List<List<Double>>,
    layerColors: List<Color>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    height: Dp = 134.dp,
    widthFraction: Float = 0.4f,
    peakLabel: String? = null,
    selectedIndex: Int? = null,
    onSelect: ((Int?) -> Unit)? = null,
) {
    val colors = LocalTtColors.current
    val measurer = rememberTextMeasurer()
    val animated = ttAnimationsEnabled()
    val progress = remember { Animatable(if (animated) 0f else 1f) }
    LaunchedEffect(slots) {
        if (animated) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(TtMotion.CHART + slots.size * 20, easing = FastOutSlowInEasing))
        }
    }

    val totals = slots.map { layers -> layers.sum() }
    val max = (totals.maxOrNull() ?: 0.0).takeIf { it > 0.0 } ?: 1.0
    val peakIndex = totals.indexOf(totals.maxOrNull() ?: 0.0)

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
                .then(
                    if (onSelect == null) {
                        Modifier
                    } else {
                        Modifier.pointerInput(slots.size) {
                            detectTapGestures { offset ->
                                if (slots.isEmpty()) return@detectTapGestures
                                val index = (offset.x / (size.width / slots.size)).toInt()
                                    .coerceIn(0, slots.size - 1)
                                onSelect(if (index == selectedIndex) null else index)
                            }
                        }
                    },
                ),
        ) {
            val baselineY = size.height - 1.dp.toPx()
            val labelRoom = if (peakLabel == null) 0f else 18.dp.toPx()
            val plotHeight = baselineY - labelRoom
            val slotWidth = if (slots.isEmpty()) size.width else size.width / slots.size
            val barWidth = slotWidth * widthFraction
            val gap = 1.dp.toPx()
            val radius = CornerRadius(barWidth / 2f)

            selectedIndex?.let { index ->
                val center = slotWidth * (index + 0.5f)
                drawLine(colors.ink, Offset(center, 0f), Offset(center, baselineY), 1.dp.toPx())
            }

            slots.forEachIndexed { index, layers ->
                // Każdy słupek rośnie z własnym opóźnieniem, więc wykres „wchodzi” od lewej.
                val total = (TtMotion.CHART + slots.size * 20).toFloat()
                val start = index * 20f / total
                val slotProgress = ((progress.value - start) / (TtMotion.CHART / total)).coerceIn(0f, 1f)
                val center = slotWidth * (index + 0.5f)
                var bottom = baselineY
                layers.forEachIndexed { layer, value ->
                    if (value <= 0.0) return@forEachIndexed
                    val full = (value / max * plotHeight).toFloat()
                    val barHeight = (full * slotProgress - gap).coerceAtLeast(0f)
                    if (barHeight <= 0f) return@forEachIndexed
                    drawRoundRect(
                        color = layerColors.getOrElse(layer) { colors.ink },
                        topLeft = Offset(center - barWidth / 2f, bottom - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = radius,
                    )
                    bottom -= full * slotProgress
                }
            }

            drawLine(colors.line, Offset(0f, baselineY), Offset(size.width, baselineY), 1.dp.toPx())

            if (peakLabel != null && totals.isNotEmpty() && progress.value > 0.99f) {
                val center = slotWidth * (peakIndex + 0.5f)
                val top = baselineY - (totals[peakIndex] / max * plotHeight).toFloat() - 16.dp.toPx()
                val layout = measurer.measure(
                    peakLabel,
                    TtType.caption.copy(color = colors.ink, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                )
                drawText(layout, topLeft = Offset(center - layout.size.width / 2f, top.coerceAtLeast(0f)))
            }
        }
        if (labels.isNotEmpty()) {
            Row(Modifier.fillMaxWidth()) {
                labels.forEach { label ->
                    CaptionText(label, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }
            }
        }
    }
}
