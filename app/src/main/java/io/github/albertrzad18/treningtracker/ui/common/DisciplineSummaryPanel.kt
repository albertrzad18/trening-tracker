package io.github.albertrzad18.treningtracker.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.albertrzad18.treningtracker.R
import io.github.albertrzad18.treningtracker.domain.Discipline
import io.github.albertrzad18.treningtracker.domain.DisciplineTotals
import io.github.albertrzad18.treningtracker.ui.format.caloriesParts
import io.github.albertrzad18.treningtracker.ui.format.distanceParts
import io.github.albertrzad18.treningtracker.ui.format.formatHoursAndMinutes
import io.github.albertrzad18.treningtracker.ui.format.paceParts
import io.github.albertrzad18.treningtracker.ui.format.speedParts
import io.github.albertrzad18.treningtracker.ui.format.workoutDistanceParts
import io.github.albertrzad18.treningtracker.ui.theme.TtSpace

/**
 * Karta dyscypliny: czas, liczba treningów i przepalone kalorie, a dla basenu i roweru dystans oraz
 * (basen) średnie tempo albo (rower) średnia prędkość. Brakujące dane są ukryte, nie pokazujemy zer.
 * Używana w zakładce Miesiąc i na ekranie Treningi, więc obie pokazują te same liczby.
 * Dyscyplin bez treningów nie pokazujemy wcale: wywołujący pomija je, zanim narysuje kartę.
 */
@Composable
fun DisciplineSummaryPanel(item: DisciplineTotals) {
    val totals = item.totals
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DisciplineDot(item.discipline)
            LabelText(stringResource(item.discipline.labelRes()))
        }
        Spacer(Modifier.height(TtSpace.x3))
        val hasDistance = item.discipline != Discipline.TRAINER
        val pace = totals.pacePer100mSec?.takeIf { item.discipline == Discipline.POOL }?.let { paceParts(it) }
        val avgDuration = totals.averageDuration?.let {
            MetricItem(stringResource(R.string.metric_avg_duration), formatHoursAndMinutes(it))
        }
        val items = listOfNotNull(
            MetricItem(stringResource(R.string.metric_time), formatHoursAndMinutes(totals.duration)),
            MetricItem(stringResource(R.string.metric_workouts), totals.count.toString()),
            totals.distanceM?.takeIf { hasDistance }
                ?.let { MetricItem(stringResource(R.string.metric_distance), distanceParts(it).value, "km") },
            pace?.let { MetricItem(stringResource(R.string.metric_avg_pace), it.value, it.unit) },
            totals.avgSpeedKmh?.takeIf { item.discipline == Discipline.ROAD }
                ?.let { MetricItem(stringResource(R.string.metric_avg_speed), speedParts(it).value, "km/h") },
            totals.caloriesKcal?.let { MetricItem(stringResource(R.string.dashboard_burned_month), caloriesParts(it).value, "kcal") },
            // Średni dystans jednego treningu (basen w metrach), a dla trenażera średni czas.
            if (hasDistance) {
                totals.averageDistanceM?.let { workoutDistanceParts(item.discipline, it) }
                    ?.let { MetricItem(stringResource(R.string.metric_avg_distance), it.value, it.unit) }
            } else {
                avgDuration
            },
        )
        // Siatka ma dwie kolumny: przy nieparzystej liczbie pól dokładamy średni czas, żeby nie było dziury.
        MetricGrid(if (items.size % 2 == 1 && avgDuration != null && avgDuration !in items) items + avgDuration else items)
    }
}
