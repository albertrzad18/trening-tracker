package io.github.albertrzad18.treningtracker.ui.dashboard

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.albertrzad18.treningtracker.R
import io.github.albertrzad18.treningtracker.TreningApp
import io.github.albertrzad18.treningtracker.data.db.toDomain
import io.github.albertrzad18.treningtracker.data.sync.SyncScheduler
import io.github.albertrzad18.treningtracker.domain.DashboardData
import io.github.albertrzad18.treningtracker.domain.Discipline
import io.github.albertrzad18.treningtracker.domain.DisciplineTotals
import io.github.albertrzad18.treningtracker.domain.StatsMetric
import io.github.albertrzad18.treningtracker.domain.RecordKind
import io.github.albertrzad18.treningtracker.domain.Workout
import io.github.albertrzad18.treningtracker.domain.buildDashboard
import io.github.albertrzad18.treningtracker.domain.metricValue
import io.github.albertrzad18.treningtracker.ui.goals.GoalCard
import io.github.albertrzad18.treningtracker.ui.goals.GoalsViewModel
import io.github.albertrzad18.treningtracker.ui.records.RecordsViewModel
import io.github.albertrzad18.treningtracker.ui.charts.ChartCard
import io.github.albertrzad18.treningtracker.ui.charts.ChartSeries
import io.github.albertrzad18.treningtracker.ui.charts.StackedBarChart
import io.github.albertrzad18.treningtracker.ui.common.DisciplineDot
import io.github.albertrzad18.treningtracker.ui.common.color
import io.github.albertrzad18.treningtracker.ui.common.distanceText
import io.github.albertrzad18.treningtracker.ui.common.durationText
import io.github.albertrzad18.treningtracker.ui.common.labelRes
import io.github.albertrzad18.treningtracker.ui.common.localDateTime
import io.github.albertrzad18.treningtracker.ui.common.summaryText
import io.github.albertrzad18.treningtracker.ui.format.formatDateTime
import io.github.albertrzad18.treningtracker.ui.format.formatDayShort
import io.github.albertrzad18.treningtracker.ui.format.formatDistanceKm
import io.github.albertrzad18.treningtracker.ui.format.formatDuration
import io.github.albertrzad18.treningtracker.ui.format.formatMinutesShort
import io.github.albertrzad18.treningtracker.ui.format.formatPercentChange
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId

class DashboardViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as TreningApp).database.workoutDao()

    /** `null` = jeszcze się wczytuje. */
    val state: StateFlow<DashboardData?> = dao.observeAll()
        .map { list -> buildDashboard(list.map { it.toDomain() }, LocalDate.now(), ZoneId.systemDefault()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun DashboardScreen(
    onOpenWorkout: (String) -> Unit,
    onOpenSummaries: () -> Unit,
    onOpenGoals: () -> Unit,
    vm: DashboardViewModel = viewModel(),
) {
    val data by vm.state.collectAsStateWithLifecycle()
    val d = data
    when {
        d == null -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        d.isEmpty -> EmptyState()
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { WeekCard(d) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    d.perDiscipline.forEach { DisciplineTile(it, Modifier.weight(1f)) }
                }
            }
            item { SevenDayCard(d) }
            item { GoalsSection(onOpenGoals) }
            item { NewRecordsSection(onOpenWorkout) }
            item { Text(stringResource(R.string.dashboard_latest), style = MaterialTheme.typography.titleMedium) }
            items(d.latest, key = { it.id }) { WorkoutCard(it, onClick = { onOpenWorkout(it.id) }) }
            item {
                TextButton(onClick = onOpenSummaries) { Text(stringResource(R.string.dashboard_open_summaries)) }
            }
        }
    }
}

/** Postęp celów: najwyżej dwa, reszta na ekranie Cele. */
@Composable
private fun GoalsSection(onOpenGoals: () -> Unit, vm: GoalsViewModel = viewModel()) {
    val rows by vm.rows.collectAsStateWithLifecycle()
    val list = rows.orEmpty()
    if (list.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.dashboard_goals), style = MaterialTheme.typography.titleMedium)
        list.take(2).forEach { GoalCard(it) }
        if (list.size > 2) {
            TextButton(onClick = onOpenGoals) { Text(stringResource(R.string.more_goals)) }
        }
    }
}

/** Rekordy pobite w ostatnim tygodniu; gdy nie ma żadnego, sekcja znika. */
@Composable
private fun NewRecordsSection(onOpenWorkout: (String) -> Unit, vm: RecordsViewModel = viewModel()) {
    val data by vm.data.collectAsStateWithLifecycle()
    val since = LocalDate.now().minusDays(7)
    val fresh = data?.perDiscipline?.values?.flatten().orEmpty().filter { !it.date.isBefore(since) }
    if (fresh.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.dashboard_new_records), style = MaterialTheme.typography.titleMedium)
        fresh.forEach { record ->
            Card(Modifier.fillMaxWidth().clickable { onOpenWorkout(record.workoutId) }) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    DisciplineDot(record.discipline, size = 10.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(
                            R.string.dashboard_record_line,
                            stringResource(record.discipline.labelRes()),
                            stringResource(record.kind.shortLabelRes()),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

private fun RecordKind.shortLabelRes() = when (this) {
    RecordKind.LONGEST_DISTANCE -> R.string.record_distance
    RecordKind.LONGEST_DURATION -> R.string.record_duration
    RecordKind.BEST_AVG_SPEED -> R.string.record_speed
    RecordKind.BEST_PACE -> R.string.record_pace
    RecordKind.MOST_CALORIES -> R.string.record_calories
    RecordKind.BIGGEST_ELEVATION -> R.string.record_elevation
}

/** Mini-wykres: minuty treningu na każdy z ostatnich 7 dni, kolorowane dyscypliną. */
@Composable
private fun SevenDayCard(d: DashboardData) {
    val series = Discipline.entries.map { discipline ->
        ChartSeries(
            color = discipline.color(),
            values = d.lastSevenDays.map { it.perDiscipline[discipline]?.metricValue(StatsMetric.TIME) ?: 0.0 },
        )
    }
    ChartCard(
        title = stringResource(R.string.dashboard_last_seven),
        hasData = series.any { s -> s.values.any { it > 0.0 } },
    ) {
        StackedBarChart(
            bars = series,
            xLabel = { i -> d.lastSevenDays.getOrNull(i)?.start?.let(::formatDayShort).orEmpty() },
            yLabel = ::formatMinutesShort,
            height = 140.dp,
        )
    }
}

@Composable
private fun EmptyState() {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.dashboard_empty_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.dashboard_empty_body), style = MaterialTheme.typography.bodyMedium)
        Button(onClick = { SyncScheduler.syncNow(context) }) { Text(stringResource(R.string.sync_now)) }
    }
}

@Composable
private fun WeekCard(d: DashboardData) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.dashboard_week), style = MaterialTheme.typography.titleMedium)
            Text(formatDuration(d.week.duration), style = MaterialTheme.typography.displaySmall)
            // Dystans i kalorie tylko, gdy są (bez zer).
            val extra = listOfNotNull(
                d.week.distanceM?.let(::formatDistanceKm),
                d.week.caloriesKcal?.let { io.github.albertrzad18.treningtracker.ui.format.formatCalories(it) },
            )
            if (extra.isNotEmpty()) Text(extra.joinToString(" · "), style = MaterialTheme.typography.bodyLarge)
            d.durationChangePercent?.let {
                Text(
                    stringResource(R.string.dashboard_vs_prev, formatPercentChange(it)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DisciplineTile(week: DisciplineTotals, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column {
            Box(Modifier.fillMaxWidth().height(4.dp).background(week.discipline.color()))
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DisciplineDot(week.discipline)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(week.discipline.labelRes()), style = MaterialTheme.typography.labelLarge, color = week.discipline.color())
                }
                Text(stringResource(R.string.tile_count, week.totals.count), style = MaterialTheme.typography.bodyMedium)
                week.totals.distanceM?.let { Text(formatDistanceKm(it), style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
fun WorkoutCard(workout: Workout, onClick: () -> Unit) {
    val zone = ZoneId.systemDefault()
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            DisciplineDot(workout.discipline, size = 16.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(workout.title ?: stringResource(workout.discipline.labelRes()), style = MaterialTheme.typography.titleSmall)
                Text(
                    formatDateTime(workout.localDateTime(zone)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(workout.durationText(), style = MaterialTheme.typography.titleSmall)
                workout.distanceText()?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}
