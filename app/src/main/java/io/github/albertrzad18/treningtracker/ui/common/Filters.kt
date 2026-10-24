package io.github.albertrzad18.treningtracker.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.albertrzad18.treningtracker.R
import io.github.albertrzad18.treningtracker.domain.Discipline
import io.github.albertrzad18.treningtracker.domain.PeriodFilter
import io.github.albertrzad18.treningtracker.domain.StatsMetric

@StringRes
fun PeriodFilter.labelRes(): Int = when (this) {
    PeriodFilter.WEEK -> R.string.period_week
    PeriodFilter.MONTH -> R.string.period_month
    PeriodFilter.YEAR -> R.string.period_year
    PeriodFilter.ALL -> R.string.period_all
}

@StringRes
fun StatsMetric.labelRes(): Int = when (this) {
    StatsMetric.TIME -> R.string.metric_label_time
    StatsMetric.DISTANCE -> R.string.metric_label_distance
    StatsMetric.CALORIES -> R.string.metric_label_calories
}

/** Chipy dyscyplin: wielokrotny wybór, pusty zbiór znaczy „wszystkie”. */
@Composable
fun DisciplineChips(selected: Set<Discipline>, onToggle: (Discipline) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Discipline.entries.forEach { d ->
            FilterChip(
                selected = d in selected,
                onClick = { onToggle(d) },
                label = { Text(stringResource(d.labelRes())) },
                leadingIcon = { DisciplineDot(d, size = 10.dp) },
            )
        }
    }
}

/** Chipy okresu: tydzień / miesiąc / rok / wszystko, jeden wybór. */
@Composable
fun PeriodChips(selected: PeriodFilter, onSelect: (PeriodFilter) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PeriodFilter.entries.forEach { p ->
            FilterChip(
                selected = p == selected,
                onClick = { onSelect(p) },
                label = { Text(stringResource(p.labelRes())) },
            )
        }
    }
}

/** Chipy metryki wykresu objętości: czas / dystans / kalorie. */
@Composable
fun MetricChips(selected: StatsMetric, onSelect: (StatsMetric) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatsMetric.entries.forEach { m ->
            FilterChip(
                selected = m == selected,
                onClick = { onSelect(m) },
                label = { Text(stringResource(m.labelRes())) },
            )
        }
    }
}
