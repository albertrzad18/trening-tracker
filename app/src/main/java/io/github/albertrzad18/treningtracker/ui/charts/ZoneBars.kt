package io.github.albertrzad18.treningtracker.ui.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.albertrzad18.treningtracker.domain.HeartRateZone
import io.github.albertrzad18.treningtracker.ui.format.formatHoursAndMinutes
import io.github.albertrzad18.treningtracker.ui.theme.LocalTtColors
import io.github.albertrzad18.treningtracker.ui.theme.TtShape
import io.github.albertrzad18.treningtracker.ui.theme.TtSpace
import io.github.albertrzad18.treningtracker.ui.theme.TtType
import java.time.Duration

/**
 * Pięć stref tętna na wspólnej skali: numer strefy, pasek 8 dp i czas po prawej — wszystko w jednej linii.
 * Strefa, w której trening nie był, zostaje samym torem (bez zera w tekście… czas `0 min` mówi, że była pusta).
 */
@Composable
fun ZoneBars(zones: List<HeartRateZone>, times: List<Duration>, modifier: Modifier = Modifier) {
    val colors = LocalTtColors.current
    val longest = times.maxOfOrNull { it.seconds }?.coerceAtLeast(1) ?: 1
    Column(modifier.fillMaxWidth()) {
        zones.forEachIndexed { index, zone ->
            val seconds = times.getOrNull(index)?.seconds ?: 0L
            Row(
                Modifier.fillMaxWidth().defaultMinSize(minHeight = 36.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TtSpace.x3),
            ) {
                Text(
                    zone.index.toString(),
                    style = TtType.label,
                    color = if (seconds > 0) colors.ink else colors.ink2,
                    modifier = Modifier.width(18.dp),
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(TtShape.full)
                        .background(colors.track),
                ) {
                    if (seconds > 0) {
                        Box(
                            Modifier
                                .fillMaxWidth(seconds.toFloat() / longest)
                                .fillMaxHeight()
                                .clip(TtShape.full)
                                .background(colors.zone(zone.index)),
                        )
                    }
                }
                Text(
                    formatHoursAndMinutes(times.getOrNull(index) ?: Duration.ZERO),
                    style = TtType.bodyS.copy(fontWeight = FontWeight.Medium),
                    color = if (seconds > 0) colors.ink else colors.ink2,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier.width(74.dp),
                )
            }
        }
    }
}
