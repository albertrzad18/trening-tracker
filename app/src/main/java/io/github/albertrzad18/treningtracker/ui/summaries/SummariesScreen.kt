package io.github.albertrzad18.treningtracker.ui.summaries

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.albertrzad18.treningtracker.R
import io.github.albertrzad18.treningtracker.TreningApp
import io.github.albertrzad18.treningtracker.data.db.toDomain
import io.github.albertrzad18.treningtracker.domain.DisciplineTotals
import io.github.albertrzad18.treningtracker.domain.PeriodSummary
import io.github.albertrzad18.treningtracker.domain.SummaryGranularity
import io.github.albertrzad18.treningtracker.domain.summaries
import io.github.albertrzad18.treningtracker.ui.common.DisciplineDot
import io.github.albertrzad18.treningtracker.ui.common.daysText
import io.github.albertrzad18.treningtracker.ui.common.durationText
import io.github.albertrzad18.treningtracker.ui.common.labelRes
import io.github.albertrzad18.treningtracker.ui.common.localDateTime
import io.github.albertrzad18.treningtracker.ui.common.summaryText
import io.github.albertrzad18.treningtracker.ui.format.formatDate
import io.github.albertrzad18.treningtracker.ui.format.formatDateTime
import io.github.albertrzad18.treningtracker.ui.format.formatMonth
import io.github.albertrzad18.treningtracker.ui.format.formatPercentChange
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class SummariesViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as TreningApp).database.workoutDao()
    private val _granularity = MutableStateFlow(SummaryGranularity.WEEK)
    val granularity: StateFlow<SummaryGranularity> = _granularity

    /** `null` = jeszcze się wczytuje. */
    val data: StateFlow<List<PeriodSummary>?> = combine(dao.observeAll(), _granularity) { entities, g ->
        summaries(entities.map { it.toDomain() }, g, LocalDate.now(), ZoneId.systemDefault())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setGranularity(g: SummaryGranularity) {
        _granularity.value = g
    }
}

@Composable
fun SummariesScreen(
    onBack: () -> Unit,
    onOpenWorkout: (String) -> Unit,
    vm: SummariesViewModel = viewModel(),
) {
    val granularity by vm.granularity.collectAsStateWithLifecycle()
    val list by vm.data.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(stringResource(R.string.summaries_title), style = MaterialTheme.typography.titleLarge)
        }
        PrimaryTabRow(selectedTabIndex = SummaryGranularity.entries.indexOf(granularity)) {
            SummaryGranularity.entries.forEach { g ->
                Tab(
                    selected = g == granularity,
                    onClick = { vm.setGranularity(g) },
                    text = { Text(stringResource(g.labelRes())) },
                )
            }
        }
        val periods = list
        if (periods == null) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(periods, key = { it.range.start.toString() }) { SummaryCard(it, onOpenWorkout) }
            }
        }
    }
}

private fun SummaryGranularity.labelRes() = when (this) {
    SummaryGranularity.WEEK -> R.string.summary_tab_week
    SummaryGranularity.MONTH -> R.string.summary_tab_month
    SummaryGranularity.YEAR -> R.string.summary_tab_year
}

/** Nagłówek okresu: `19 paź 2026 – 25 paź 2026`, `październik 2026` albo `2026`. */
@Composable
private fun PeriodSummary.title(): String = when (granularity) {
    SummaryGranularity.WEEK -> stringResource(
        R.string.summary_week_label,
        formatDate(range.start),
        formatDate(range.endExclusive.minusDays(1)),
    )
    SummaryGranularity.MONTH -> formatMonth(YearMonth.from(range.start))
    SummaryGranularity.YEAR -> range.start.year.toString()
}

@Composable
private fun SummaryCard(s: PeriodSummary, onOpenWorkout: (String) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(s.title(), style = MaterialTheme.typography.titleMedium)
            if (s.count == 0) {
                Text(
                    stringResource(R.string.summary_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(s.totals.summaryText(), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.summary_count, s.count), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.summary_active_days, s.activeDays), style = MaterialTheme.typography.bodyMedium)
                Text(
                    stringResource(R.string.summary_streak, daysText(s.longestStreakDays)),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Changes(s)
                HorizontalDivider()
                s.perDiscipline.filter { it.totals.count > 0 }.forEach { DisciplineRow(it) }
                s.best?.let { best ->
                    HorizontalDivider()
                    val label = stringResource(best.discipline.labelRes()) +
                        ", ${formatDate(best.localDateTime(ZoneId.systemDefault()).toLocalDate())} · ${best.durationText()}"
                    Text(
                        stringResource(R.string.summary_best, label),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.clickable { onOpenWorkout(best.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun Changes(s: PeriodSummary) {
    val lines = listOfNotNull(
        s.durationChangePercent?.let { stringResource(R.string.summary_change_duration, formatPercentChange(it)) },
        s.distanceChangePercent?.let { stringResource(R.string.summary_change_distance, formatPercentChange(it)) },
        s.caloriesChangePercent?.let { stringResource(R.string.summary_change_calories, formatPercentChange(it)) },
    )
    if (lines.isEmpty()) return
    Text(
        stringResource(R.string.summary_vs_previous) + ": " + lines.joinToString(" · "),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun DisciplineRow(d: DisciplineTotals) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        DisciplineDot(d.discipline, size = 10.dp)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(d.discipline.labelRes()), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(90.dp))
        Text(d.totals.summaryText(), style = MaterialTheme.typography.bodySmall)
    }
}
