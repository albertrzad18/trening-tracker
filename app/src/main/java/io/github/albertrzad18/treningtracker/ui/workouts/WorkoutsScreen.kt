package io.github.albertrzad18.treningtracker.ui.workouts

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.albertrzad18.treningtracker.R
import io.github.albertrzad18.treningtracker.TreningApp
import io.github.albertrzad18.treningtracker.data.db.toDomain
import io.github.albertrzad18.treningtracker.domain.Discipline
import io.github.albertrzad18.treningtracker.domain.MonthGroup
import io.github.albertrzad18.treningtracker.domain.PeriodFilter
import io.github.albertrzad18.treningtracker.domain.SortColumn
import io.github.albertrzad18.treningtracker.domain.Workout
import io.github.albertrzad18.treningtracker.domain.WorkoutListResult
import io.github.albertrzad18.treningtracker.domain.buildWorkoutList
import io.github.albertrzad18.treningtracker.ui.common.DisciplineDot
import io.github.albertrzad18.treningtracker.ui.common.avgHeartRateText
import io.github.albertrzad18.treningtracker.ui.common.caloriesText
import io.github.albertrzad18.treningtracker.ui.common.distanceText
import io.github.albertrzad18.treningtracker.ui.common.durationText
import io.github.albertrzad18.treningtracker.ui.common.labelRes
import io.github.albertrzad18.treningtracker.ui.common.paceOrSpeedText
import io.github.albertrzad18.treningtracker.ui.common.summaryText
import io.github.albertrzad18.treningtracker.ui.format.formatDate
import io.github.albertrzad18.treningtracker.ui.format.formatDuration
import io.github.albertrzad18.treningtracker.ui.format.formatMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.ZoneId

data class WorkoutFilters(
    val disciplines: Set<Discipline> = emptySet(),
    val period: PeriodFilter = PeriodFilter.ALL,
    val sort: SortColumn = SortColumn.DATE,
    val ascending: Boolean = false,
)

class WorkoutsViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as TreningApp).database.workoutDao()
    private val _filters = MutableStateFlow(WorkoutFilters())
    val filters: StateFlow<WorkoutFilters> = _filters

    /** `null` = jeszcze się wczytuje. */
    val result: StateFlow<WorkoutListResult?> = combine(dao.observeAll(), _filters) { entities, f ->
        buildWorkoutList(
            all = entities.map { it.toDomain() },
            disciplines = f.disciplines,
            period = f.period,
            sort = f.sort,
            ascending = f.ascending,
            today = LocalDate.now(),
            zone = ZoneId.systemDefault(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun toggleDiscipline(d: Discipline) = _filters.update {
        it.copy(disciplines = if (d in it.disciplines) it.disciplines - d else it.disciplines + d)
    }

    fun setPeriod(p: PeriodFilter) = _filters.update { it.copy(period = p) }

    /** Kliknięcie w nagłówek: ta sama kolumna odwraca kierunek, nowa zaczyna od malejącego (najnowsze/największe). */
    fun onSortClick(column: SortColumn) = _filters.update {
        if (it.sort == column) it.copy(ascending = !it.ascending) else it.copy(sort = column, ascending = false)
    }
}

private val DATE_W = 100.dp
private val DOT_W = 28.dp
private val TIME_W = 76.dp
private val DIST_W = 84.dp
private val KCAL_W = 88.dp
private val SPEED_W = 108.dp
private val HR_W = 84.dp
private val TABLE_W = DATE_W + DOT_W + TIME_W + DIST_W + KCAL_W + SPEED_W + HR_W

@Composable
fun WorkoutsScreen(onOpenWorkout: (String) -> Unit, vm: WorkoutsViewModel = viewModel()) {
    val filters by vm.filters.collectAsStateWithLifecycle()
    val result by vm.result.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        FilterBar(filters, vm::toggleDiscipline, vm::setPeriod)
        val r = result
        when {
            r == null -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            r.workouts.isEmpty() -> Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
                Text(stringResource(R.string.list_empty), style = MaterialTheme.typography.bodyLarge)
            }
            else -> {
                val scroll = rememberScrollState()
                Box(Modifier.weight(1f).horizontalScroll(scroll)) {
                    Column(Modifier.width(TABLE_W)) {
                        HeaderRow(filters, vm::onSortClick)
                        HorizontalDivider()
                        LazyColumn(Modifier.weight(1f)) {
                            if (r.groups.isNotEmpty()) {
                                r.groups.forEach { group ->
                                    item(key = "h-${group.month}") { GroupHeader(group) }
                                    items(group.workouts, key = { it.id }) { WorkoutRow(it) { onOpenWorkout(it.id) } }
                                }
                            } else {
                                items(r.workouts, key = { it.id }) { WorkoutRow(it) { onOpenWorkout(it.id) } }
                            }
                        }
                    }
                }
                SummaryBar(r)
            }
        }
    }
}

@Composable
private fun FilterBar(
    filters: WorkoutFilters,
    onToggleDiscipline: (Discipline) -> Unit,
    onPeriod: (PeriodFilter) -> Unit,
) {
    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Discipline.entries.forEach { d ->
                FilterChip(
                    selected = d in filters.disciplines,
                    onClick = { onToggleDiscipline(d) },
                    label = { Text(stringResource(d.labelRes())) },
                    leadingIcon = { DisciplineDot(d, size = 10.dp) },
                )
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PeriodFilter.entries.forEach { p ->
                FilterChip(
                    selected = p == filters.period,
                    onClick = { onPeriod(p) },
                    label = { Text(stringResource(p.labelRes())) },
                )
            }
        }
    }
}

private fun PeriodFilter.labelRes() = when (this) {
    PeriodFilter.WEEK -> R.string.period_week
    PeriodFilter.MONTH -> R.string.period_month
    PeriodFilter.YEAR -> R.string.period_year
    PeriodFilter.ALL -> R.string.period_all
}

@Composable
private fun HeaderRow(filters: WorkoutFilters, onSort: (SortColumn) -> Unit) {
    Row(Modifier.width(TABLE_W).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        HeaderCell(R.string.col_date, DATE_W, SortColumn.DATE, filters, onSort)
        Spacer(Modifier.width(DOT_W))
        HeaderCell(R.string.col_time, TIME_W, SortColumn.DURATION, filters, onSort)
        HeaderCell(R.string.col_distance, DIST_W, SortColumn.DISTANCE, filters, onSort)
        HeaderCell(R.string.col_calories, KCAL_W, SortColumn.CALORIES, filters, onSort)
        HeaderCell(R.string.col_speed, SPEED_W, SortColumn.SPEED, filters, onSort)
        HeaderCell(R.string.col_hr, HR_W, SortColumn.HEART_RATE, filters, onSort)
    }
}

@Composable
private fun HeaderCell(
    labelRes: Int,
    width: Dp,
    column: SortColumn,
    filters: WorkoutFilters,
    onSort: (SortColumn) -> Unit,
) {
    val arrow = if (filters.sort == column) (if (filters.ascending) " ▲" else " ▼") else ""
    Text(
        stringResource(labelRes) + arrow,
        modifier = Modifier.width(width).clickable { onSort(column) }.padding(horizontal = 6.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = if (filters.sort == column) FontWeight.Bold else FontWeight.Normal,
        maxLines = 1,
    )
}

@Composable
private fun GroupHeader(group: MonthGroup) {
    Row(
        Modifier.width(TABLE_W).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.list_group_total, formatMonth(group.month), group.totals.count),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.width(DATE_W + DOT_W + TIME_W),
        )
        Text(group.totals.summaryText(), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun WorkoutRow(w: Workout, onClick: () -> Unit) {
    val zone = ZoneId.systemDefault()
    Row(
        Modifier.width(TABLE_W).clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Cell(formatDate(w.startLocal(zone).toLocalDate()), DATE_W)
        Box(Modifier.width(DOT_W), Alignment.Center) { DisciplineDot(w.discipline) }
        Cell(w.durationText(), TIME_W)
        // Brakujące dane zostają puste, bez zer.
        Cell(w.distanceText().orEmpty(), DIST_W)
        Cell(w.caloriesText().orEmpty(), KCAL_W)
        Cell(w.paceOrSpeedText().orEmpty(), SPEED_W)
        Cell(w.avgHeartRateText().orEmpty(), HR_W)
    }
}

@Composable
private fun Cell(text: String, width: Dp) {
    Text(text, Modifier.width(width).padding(horizontal = 6.dp), style = MaterialTheme.typography.bodySmall, maxLines = 1)
}

@Composable
private fun SummaryBar(r: WorkoutListResult) {
    Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                stringResource(R.string.list_sum, r.totals.count) + ": " + r.totals.summaryText(),
                style = MaterialTheme.typography.labelLarge,
            )
            r.averageDuration?.let {
                Text(stringResource(R.string.list_average, formatDuration(it)), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
