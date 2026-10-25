package io.github.albertrzad18.treningtracker.ui.workouts

import android.app.Application
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import io.github.albertrzad18.treningtracker.domain.DateRange
import io.github.albertrzad18.treningtracker.domain.buildWorkoutList
import io.github.albertrzad18.treningtracker.domain.passiveTotals
import io.github.albertrzad18.treningtracker.domain.periodRange
import io.github.albertrzad18.treningtracker.ui.common.DisciplineFilterChips
import io.github.albertrzad18.treningtracker.ui.common.BigValue
import io.github.albertrzad18.treningtracker.ui.common.LabelText
import io.github.albertrzad18.treningtracker.ui.common.MetricGrid
import io.github.albertrzad18.treningtracker.ui.common.MetricItem
import io.github.albertrzad18.treningtracker.ui.common.Panel
import io.github.albertrzad18.treningtracker.ui.format.caloriesParts
import io.github.albertrzad18.treningtracker.ui.format.distanceParts
import io.github.albertrzad18.treningtracker.ui.format.formatNumberShort
import io.github.albertrzad18.treningtracker.ui.format.formatHoursAndMinutes
import io.github.albertrzad18.treningtracker.ui.common.PeriodMenuChip
import io.github.albertrzad18.treningtracker.ui.common.SecondaryButton
import io.github.albertrzad18.treningtracker.ui.common.SkeletonBlock
import io.github.albertrzad18.treningtracker.ui.common.TtChip
import io.github.albertrzad18.treningtracker.ui.common.TtDivider
import io.github.albertrzad18.treningtracker.ui.common.TtIcons
import io.github.albertrzad18.treningtracker.ui.common.avgHeartRateText
import io.github.albertrzad18.treningtracker.ui.common.caloriesText
import io.github.albertrzad18.treningtracker.ui.common.distanceText
import io.github.albertrzad18.treningtracker.ui.common.durationText
import io.github.albertrzad18.treningtracker.ui.common.paceOrSpeedText
import io.github.albertrzad18.treningtracker.ui.common.summaryText
import io.github.albertrzad18.treningtracker.ui.common.ttClickable
import io.github.albertrzad18.treningtracker.ui.format.formatDayMonth
import io.github.albertrzad18.treningtracker.ui.format.formatDayOfWeekShort
import io.github.albertrzad18.treningtracker.ui.format.formatDuration
import io.github.albertrzad18.treningtracker.ui.format.formatMonth
import io.github.albertrzad18.treningtracker.ui.format.formatTime
import io.github.albertrzad18.treningtracker.ui.format.pluralPl
import io.github.albertrzad18.treningtracker.ui.theme.LocalTtColors
import io.github.albertrzad18.treningtracker.ui.theme.TtShape
import io.github.albertrzad18.treningtracker.ui.theme.TtSpace
import io.github.albertrzad18.treningtracker.ui.theme.TtType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.ZoneId

data class WorkoutFilters(
    val disciplines: Set<Discipline> = emptySet(),
    val period: PeriodFilter = PeriodFilter.ALL,
    val sort: SortColumn = SortColumn.DATE,
    val ascending: Boolean = false,
) {
    /** Czy cokolwiek zawęża listę: wtedy sumy liczymy dla tego, co widać. */
    val isFiltered: Boolean get() = disciplines.isNotEmpty() || period != PeriodFilter.ALL
}

class WorkoutsViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as TreningApp).database.workoutDao()
    private val dailyDao = (app as TreningApp).database.dailyStatsDao()
    private val _filters = MutableStateFlow(WorkoutFilters())
    val filters: StateFlow<WorkoutFilters> = _filters

    /** Zwinięte miesiące; klucz to `YYYY-MM`. */
    private val _collapsed = MutableStateFlow(emptySet<String>())
    val collapsed: StateFlow<Set<String>> = _collapsed

    /** `null` = jeszcze się wczytuje. */
    val result: StateFlow<WorkoutListResult?> = combine(dao.observeAll(), dailyDao.observeAll(), _filters) { entities, daily, f ->
        val today = LocalDate.now()
        val result = buildWorkoutList(
            all = entities.map { it.toDomain() },
            disciplines = f.disciplines,
            period = f.period,
            sort = f.sort,
            ascending = f.ascending,
            today = today,
            zone = ZoneId.systemDefault(),
        )
        // Zakres kroków: wybrany okres, a dla „Wszystko” od pierwszego treningu do dziś.
        val range = periodRange(f.period, today)
            ?: DateRange(entities.minOfOrNull { it.startMillis }?.let { java.time.Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() } ?: today, today.plusDays(1))
        val passive = daily.map { it.toDomain() }.passiveTotals(range).takeUnless { it.isEmpty }
        result.copy(passive = passive)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun toggleDiscipline(d: Discipline) = _filters.update {
        it.copy(disciplines = if (d in it.disciplines) it.disciplines - d else it.disciplines + d)
    }

    fun setPeriod(p: PeriodFilter) = _filters.update { it.copy(period = p) }

    fun setSort(column: SortColumn) = _filters.update { it.copy(sort = column) }

    fun setAscending(ascending: Boolean) = _filters.update { it.copy(ascending = ascending) }

    fun clearFilters() {
        _filters.value = WorkoutFilters()
    }

    fun toggleMonth(key: String) = _collapsed.update { if (key in it) it - key else it + key }
}

@Composable
fun WorkoutsScreen(onOpenWorkout: (String) -> Unit, vm: WorkoutsViewModel = viewModel()) {
    val filters by vm.filters.collectAsStateWithLifecycle()
    val collapsed by vm.collapsed.collectAsStateWithLifecycle()
    val result by vm.result.collectAsStateWithLifecycle()
    WorkoutsContent(
        filters = filters,
        collapsed = collapsed,
        result = result,
        onToggleDiscipline = vm::toggleDiscipline,
        onPeriod = vm::setPeriod,
        onClear = vm::clearFilters,
        onToggleMonth = vm::toggleMonth,
        onOpenWorkout = onOpenWorkout,
    )
}

/** Sam widok listy: bez ViewModelu, więc da się go pokazać w `@Preview`. */
@Composable
internal fun WorkoutsContent(
    filters: WorkoutFilters,
    collapsed: Set<String>,
    result: WorkoutListResult?,
    onToggleDiscipline: (Discipline) -> Unit,
    onPeriod: (PeriodFilter) -> Unit,
    onClear: () -> Unit,
    onToggleMonth: (String) -> Unit,
    onOpenWorkout: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        FilterBar(
            filters = filters,
            onToggleDiscipline = onToggleDiscipline,
            onPeriod = onPeriod,
        )
        when {
            result == null -> ListSkeleton()
            result.workouts.isEmpty() -> EmptyList(onClear = onClear)
            else -> {
                WorkoutList(
                    result = result,
                    collapsed = collapsed,
                    onToggleMonth = onToggleMonth,
                    onOpenWorkout = onOpenWorkout,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

}

/** Chipy dyscyplin i menu okresu. */
@Composable
private fun FilterBar(
    filters: WorkoutFilters,
    onToggleDiscipline: (Discipline) -> Unit,
    onPeriod: (PeriodFilter) -> Unit,
) {
    val colors = LocalTtColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = TtSpace.screenMargin, end = TtSpace.screenMargin, top = TtSpace.x2, bottom = TtSpace.x1),
        verticalArrangement = Arrangement.spacedBy(TtSpace.x3),
    ) {
        io.github.albertrzad18.treningtracker.ui.common.FilterBar(
            filters.disciplines, onToggleDiscipline, filters.period, onPeriod,
        )
    }
}

/** `299 treningów`, `1 trening`, `3 treningi`. */
@Composable
private fun workoutsCountText(count: Int): String = pluralPl(
    count,
    stringResource(R.string.unit_workout_one),
    stringResource(R.string.unit_workout_few),
    stringResource(R.string.unit_workout_many),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WorkoutList(
    result: WorkoutListResult,
    collapsed: Set<String>,
    onToggleMonth: (String) -> Unit,
    onOpenWorkout: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = TtSpace.x6)) {
        item(key = "summary") { SummaryCard(result) }
        if (result.groups.isNotEmpty()) {
            result.groups.forEach { group ->
                val key = group.month.toString()
                stickyHeader(key = "h-$key") {
                    MonthHeader(
                        group = group,
                        collapsed = key in collapsed,
                        onClick = { onToggleMonth(key) },
                    )
                }
                if (key !in collapsed) {
                    items(group.workouts, key = { it.id }) { workout ->
                        WorkoutRow(workout) { onOpenWorkout(workout.id) }
                    }
                }
            }
        } else {
            items(result.workouts, key = { it.id }) { workout ->
                WorkoutRow(workout) { onOpenWorkout(workout.id) }
            }
        }
    }
}

/** Przyklejony nagłówek miesiąca: nazwa, liczba treningów, sumy i strzałka zwijania. */
@Composable
private fun MonthHeader(group: MonthGroup, collapsed: Boolean, onClick: () -> Unit) {
    val colors = LocalTtColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.bg)
            .ttClickable(onClick = onClick)
            .padding(start = TtSpace.screenMargin, end = TtSpace.screenMargin, top = 14.dp, bottom = TtSpace.x2),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(TtSpace.x2)) {
            Text(formatMonth(group.month), style = TtType.title, color = colors.ink, modifier = Modifier.weight(1f))
            LabelText(group.totals.count.toString())
            Icon(
                if (collapsed) TtIcons.ExpandMore else TtIcons.ExpandLess,
                contentDescription = null,
                tint = colors.ink2,
            )
        }
        Text(group.totals.summaryText(), style = TtType.bodyS, color = colors.ink2)
        TtDivider()
    }
}

/**
 * Wiersz listy: pasek dyscypliny, data z dniem tygodnia, czas, dystans i tempo albo prędkość.
 * Druga linia to kalorie i tętno. Brakujące wartości zostawiają puste miejsce, bez zer.
 */
@Composable
private fun WorkoutRow(workout: Workout, onClick: () -> Unit) {
    val colors = LocalTtColors.current
    val zone = ZoneId.systemDefault()
    val date = workout.startLocal(zone)
    val second = listOfNotNull(workout.caloriesText(), workout.avgHeartRateText()).joinToString(" · ")
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 60.dp)
            .ttClickable(onClick = onClick)
            .padding(horizontal = TtSpace.screenMargin, vertical = TtSpace.x3)
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(TtSpace.x3),
    ) {
        Box(
            Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(colors.of(workout.discipline), RoundedCornerShape(2.dp)),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.width(62.dp)) {
                    Text(
                        formatDayMonth(date.toLocalDate()),
                        style = TtType.body.copy(fontWeight = FontWeight.Medium),
                        color = colors.ink,
                        maxLines = 1,
                    )
                    Text(
                        "${formatDayOfWeekShort(date.toLocalDate())} ${formatTime(date.toLocalTime())}",
                        style = TtType.caption,
                        color = colors.ink2,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
                Text(
                    workout.durationText(),
                    style = TtType.body.copy(fontWeight = FontWeight.Medium),
                    color = colors.ink,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.width(62.dp),
                )
                Text(
                    workout.distanceText().orEmpty(),
                    style = TtType.body,
                    color = colors.ink,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.width(58.dp),
                )
                Text(
                    workout.paceOrSpeedText().orEmpty(),
                    style = TtType.body.copy(fontSize = 14.sp),
                    color = colors.ink,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.weight(1f),
                )
            }
            if (second.isNotEmpty()) Text(second, style = TtType.bodyS, color = colors.ink2, maxLines = 1)
        }
    }
}

/**
 * Karta z sumami dla widocznych treningów (po filtrach): czas, dystans, kalorie i średni czas.
 * Zastępuje dawny pasek na dole ekranu. Dystans i kalorie znikają, gdy nie ma z czego ich policzyć.
 */
@Composable
private fun SummaryCard(result: WorkoutListResult) {
    val colors = LocalTtColors.current
    val totals = result.totals
    Box(Modifier.padding(horizontal = TtSpace.screenMargin).padding(top = TtSpace.x2, bottom = TtSpace.x1)) {
        Panel {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                LabelText(stringResource(R.string.list_sum_label), modifier = Modifier.weight(1f))
                Text(workoutsCountText(totals.count), style = TtType.bodyS, color = colors.ink2)
            }
            Spacer(Modifier.height(TtSpace.x2))
            BigValue(formatHoursAndMinutes(totals.duration), null, style = TtType.hero, color = colors.ink)
            Spacer(Modifier.height(TtSpace.x3))
            TtDivider()
            Spacer(Modifier.height(TtSpace.x3))
            MetricGrid(
                listOfNotNull(
                    totals.distanceM?.let { MetricItem(stringResource(R.string.metric_distance), distanceParts(it).value, "km") },
                    totals.caloriesKcal?.let { MetricItem(stringResource(R.string.metric_calories), caloriesParts(it).value, "kcal") },
                    result.averageDuration?.let { MetricItem(stringResource(R.string.list_avg_label), formatDuration(it)) },
                    result.workouts.maxByOrNull { it.duration }?.let {
                        MetricItem(stringResource(R.string.list_longest_label), formatDuration(it.duration))
                    },
                    perWeek(result)?.let { MetricItem(stringResource(R.string.list_per_week_label), it, stringResource(R.string.list_per_week_unit)) },
                    result.passive?.steps?.let { MetricItem(stringResource(R.string.passive_steps), formatNumberShort(it.toDouble())) },
                    result.passive?.basalKcal?.let { MetricItem(stringResource(R.string.passive_basal), caloriesParts(it).value, "kcal") },
                    result.passive?.averageBasalKcal?.let { MetricItem(stringResource(R.string.passive_basal_avg), caloriesParts(it).value, "kcal") },
                ),
            )
        }
    }
}

/** Ile treningów tygodniowo w widocznym zakresie dat, z jednym miejscem po przecinku; `null` przy jednym dniu. */
private fun perWeek(result: WorkoutListResult): String? {
    val first = result.workouts.minOfOrNull { it.start } ?: return null
    val last = result.workouts.maxOfOrNull { it.start } ?: return null
    val weeks = java.time.Duration.between(first, last).toDays() / 7.0
    if (weeks < 1.0) return null
    return "%.1f".format(java.util.Locale.forLanguageTag("pl-PL"), result.workouts.size / weeks)
}

@Composable
private fun EmptyList(onClear: () -> Unit) {
    val colors = LocalTtColors.current
    Column(
        Modifier.fillMaxWidth().padding(horizontal = TtSpace.screenMargin).padding(top = 72.dp),
        verticalArrangement = Arrangement.spacedBy(TtSpace.x4),
    ) {
        Text(stringResource(R.string.list_empty), style = TtType.body, color = colors.ink2)
        SecondaryButton(text = stringResource(R.string.list_clear_filters), onClick = onClear)
    }
}

@Composable
private fun ListSkeleton() {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = TtSpace.screenMargin).padding(top = TtSpace.x3),
        verticalArrangement = Arrangement.spacedBy(TtSpace.x4),
    ) {
        SkeletonBlock(height = 18.dp, width = 160.dp)
        repeat(7) { SkeletonBlock(height = 44.dp) }
    }
}
