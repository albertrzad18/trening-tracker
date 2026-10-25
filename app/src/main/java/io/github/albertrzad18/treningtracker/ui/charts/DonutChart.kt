package io.github.albertrzad18.treningtracker.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.albertrzad18.treningtracker.domain.DisciplineShare
import io.github.albertrzad18.treningtracker.ui.common.color
import io.github.albertrzad18.treningtracker.ui.common.labelRes
import io.github.albertrzad18.treningtracker.ui.format.formatHoursAndMinutes
import io.github.albertrzad18.treningtracker.ui.format.formatPercent
import java.time.Duration

/** Pierścień z udziałem dyscyplin w łącznym czasie, z legendą i procentami obok. */
@Composable
fun DisciplineDonut(shares: List<DisciplineShare>, modifier: Modifier = Modifier) {
    val total = Duration.ofSeconds(shares.sumOf { it.duration.seconds })
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val palette = io.github.albertrzad18.treningtracker.ui.theme.LocalTtColors.current
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(140.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(140.dp)) {
                val stroke = Stroke(width = 24.dp.toPx())
                val inset = stroke.width / 2
                val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = stroke,
                )
                var start = -90f
                shares.forEach { share ->
                    // Mała przerwa między wycinkami, żeby kolory się nie zlewały.
                    val sweep = (share.fraction * 360f).toFloat()
                    drawArc(
                        color = palette.of(share.discipline),
                        startAngle = start + 1f,
                        sweepAngle = (sweep - 2f).coerceAtLeast(1f),
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = stroke,
                    )
                    start += sweep
                }
            }
            Text(formatHoursAndMinutes(total), style = MaterialTheme.typography.titleSmall)
        }
        Spacer(Modifier.width(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            shares.forEach { share ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    io.github.albertrzad18.treningtracker.ui.common.DisciplineDot(share.discipline, size = 10.dp)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(stringResource(share.discipline.labelRes()), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${formatPercent(share.fraction)} · ${formatHoursAndMinutes(share.duration)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
