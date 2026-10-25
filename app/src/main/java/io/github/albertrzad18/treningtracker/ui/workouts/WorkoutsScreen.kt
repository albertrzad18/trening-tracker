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
import io.github.albertrzad18.treningtracker.ui.common.DisciplineFilterChips
import io.github.albertrzad18.treningtracker.ui.common.LabelText
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
    private val _filters = MutableStateFlow(WorkoutFilters())
    val filters: StateFlow<WorkoutFilters> = _filters

    /** Zwinięte miesiące; klucz to `YYYY-MM`. */
    private val _collapsed = MutableStateFlow(emptySet<String>())
    val collapsed: StateFlow<Set<String>> = _collapsed

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
    var sortSheet by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        FilterBar(
            filters = filters,
            count = result?.workouts?.size,
            onToggleDiscipline = vm::toggleDiscipline,
            onPeriod = vm::setPeriod,
            onOpenSort = { sortSheet = true },
        )
        val r = result
        when {
            r == null -> ListSkeleton()
            r.workouts.isEmpty() -> EmptyList(onClear = vm::clearFilters)
            else -> {
                WorkoutList(
                    result = r,
                    collapsed = collapsed,
                    onToggleMonth = vm::toggleMonth,
                    onOpenWorkout = onOpenWorkout,
                    modifier = Modifier.weight(1f),
                )
                SummaryBar(r)
            }
        }
    }

    if (sortSheet) {
        SortSheet(
            filters = filters,
            onDismiss = { sortSheet = false },
            onSort = vm::setSort,
            onAscending = vm::setAscending,
        )
    }
}

/** Chipy dyscyplin, menu okresu, chip sortowania i licznik treningów. */
@Composable
private fun FilterBar(
    filters: WorkoutFilters,
    count: Int?,
    onToggleDiscipline: (Discipline) -> Unit,
    onPeriod: (PeriodFilter) -> Unit,
    onOpenSort: () -> Unit,
) {
    val colors = LocalTtColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = TtSpace.screenMargin, end = TtSpace.screenMargin, top = TtSpace.x2, bottom = TtSpace.x1),
        verticalArrangement = Arrangement.spacedBy(TtSpace.x3),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DisciplineFilterChips(filters.disciplines, onToggleDiscipline, modifier = Modifier.weight(1f))
            PeriodMenuChip(filters.period, onPeriod)
        }
        Row(
            Modifier.fillMaxWidth().defaultMinSize(minHeight = 36.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TtChip(
                label = stringResource(filters.sort.labelRes()),
                onClick = onOpenSort,
                paddingEnd = 10.dp,
                trailing = {
                    Icon(
                        if (filters.ascending) TtIcons.Up else TtIcons.Down,
                        contentDescription = null,
                        tint = colors.ink,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
            Box(Modifier.weight(1f))
            if (count != null) {
                Text(workoutsCountText(count), style = TtType.bodyS, color = colors.ink2)
            }
        }
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
    LazyColumn(modifier.fillMaxSize()) {
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
            Row(horizontalArrangement = Arrangement.spacedBy(TtSpace.x3)) {
                Column(Modifier.width(64.dp)) {
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
                    )
                }
                Text(
                    workout.durationText(),
                    style = TtType.body.copy(fontWeight = FontWeight.Medium),
                    color = colors.ink,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    workout.distanceText().orEmpty(),
                    style = TtType.body,
                    color = colors.ink,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    workout.paceOrSpeedText().orEmpty(),
                    style = TtType.body,
                    color = colors.ink,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier.weight(1.2f),
                )
            }
            if (second.isNotEmpty()) Text(second, style = TtType.bodyS, color = colors.ink2, maxLines = 1)
        }
    }
}

/** Przyklejony pasek sumy: liczba treningów, sumy i średni czas treningu. */
@Composable
private fun SummaryBar(result: WorkoutListResult) {
    val colors = LocalTtColors.current
    Column(Modifier.fillMaxWidth().background(colors.surface)) {
        TtDivider()
        Column(
            Modifier.padding(horizontal = TtSpace.screenMargin, vertical = TtSpace.x3),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                LabelText(stringResource(R.string.list_sum, result.totals.count), color = colors.ink)
                Box(Modifier.weight(1f))
                result.averageDuration?.let {
                    Text(
                        stringResource(R.string.list_avg_short, formatDuration(it)),
                        style = TtType.bodyS,
                        color = colors.ink2,
                    )
                }
            }
            Text(
                result.totals.summaryText(),
                style = TtType.body.copy(fontWeight = FontWeight.Medium),
                color = colors.ink,
            )
        }
    }
}

/** Arkusz sortowania: klucz i kierunek. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun SortSheet(
    filters: WorkoutFilters,
    onDismiss: () -> Unit,
    onSort: (SortColumn) -> Unit,
    onAscending: (Boolean) -> Unit,
) {
    val colors = LocalTtColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = TtShape.sheet,
        containerColor = colors.surface,
        dragHandle = {
            Box(Modifier.fillMaxWidth().padding(top = TtSpace.x3), contentAlignment = Alignment.Center) {
                Box(Modifier.width(36.dp).height(4.dp).background(colors.line, RoundedCornerShape(2.dp)))
            }
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = TtSpace.screenMargin)
                .padding(top = TtSpace.x4, bottom = TtSpace.x8)
                .navigationBarsPadding(),
        ) {
            Text(stringResource(R.string.sort_title), style = TtType.title, color = colors.ink)
            Box(Modifier.height(TtSpace.x2))
            SortColumn.entries.forEach { column ->
                val selected = column == filters.sort
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(TtSpace.minTouch)
                        .clip(TtShape.field)
                        .ttClickable { onSort(column) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        Modifier
                            .size(20.dp)
                            .border(2.dp, if (selected) colors.ink else colors.line, TtShape.full),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Box(Modifier.size(10.dp).background(colors.ink, TtShape.full))
                        }
                    }
                    Text(stringResource(column.labelRes()), style = TtType.body, color = colors.ink)
                }
            }
            Box(Modifier.height(TtSpace.x4))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TtSpace.x2)) {
                listOf(true, false).forEach { ascending ->
                    val selected = filters.ascending == ascending
                    TtChip(
                        label = stringResource(
                            if (ascending) R.string.sort_ascending else R.string.sort_descending,
                        ),
                        onClick = { onAscending(ascending) },
                        selected = selected,
                        background = if (selected) colors.accent else Color.Transparent,
                        border = if (selected) colors.accent else colors.line,
                        textColor = if (selected) colors.onAccent else colors.ink,
                        leading = {
                            Icon(
                                if (ascending) TtIcons.Up else TtIcons.Down,
                                contentDescription = null,
                                tint = if (selected) colors.onAccent else colors.ink,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        modifier = Modifier.weight(1f).height(44.dp),
                    )
                }
            }
        }
    }
}

private fun SortColumn.labelRes() = when (this) {
    SortColumn.DATE -> R.string.col_date
    SortColumn.DISCIPLINE -> R.string.col_discipline
    SortColumn.DURATION -> R.string.col_time
    SortColumn.DISTANCE -> R.string.col_distance
    SortColumn.CALORIES -> R.string.col_calories
    SortColumn.SPEED -> R.string.col_speed
    SortColumn.HEART_RATE -> R.string.col_hr
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
