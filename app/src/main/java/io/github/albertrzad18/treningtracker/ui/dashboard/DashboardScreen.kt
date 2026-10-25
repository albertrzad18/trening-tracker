package io.github.albertrzad18.treningtracker.ui.dashboard

import android.app.Application
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import io.github.albertrzad18.treningtracker.data.sync.SyncScheduler
import io.github.albertrzad18.treningtracker.domain.DashboardData
import io.github.albertrzad18.treningtracker.domain.Discipline
import io.github.albertrzad18.treningtracker.domain.DisciplineTotals
import io.github.albertrzad18.treningtracker.domain.Workout
import io.github.albertrzad18.treningtracker.domain.buildDashboard
import io.github.albertrzad18.treningtracker.ui.common.BigValue
import io.github.albertrzad18.treningtracker.ui.common.DisciplineDot
import io.github.albertrzad18.treningtracker.ui.common.DeltaBadge
import io.github.albertrzad18.treningtracker.ui.common.LabelText
import io.github.albertrzad18.treningtracker.ui.common.LinkRow
import io.github.albertrzad18.treningtracker.ui.common.NumberedSteps
import io.github.albertrzad18.treningtracker.ui.common.Panel
import io.github.albertrzad18.treningtracker.ui.common.PrimaryButton
import io.github.albertrzad18.treningtracker.ui.common.SectionHeader
import io.github.albertrzad18.treningtracker.ui.common.ShareBar
import io.github.albertrzad18.treningtracker.ui.common.SkeletonBlock
import io.github.albertrzad18.treningtracker.ui.common.TtDivider
import io.github.albertrzad18.treningtracker.ui.common.TtIcons
import io.github.albertrzad18.treningtracker.ui.common.deltaWeekText
import io.github.albertrzad18.treningtracker.ui.common.labelRes
import io.github.albertrzad18.treningtracker.ui.common.localDateTime
import io.github.albertrzad18.treningtracker.ui.common.ttClickable
import io.github.albertrzad18.treningtracker.ui.format.caloriesParts
import io.github.albertrzad18.treningtracker.ui.format.formatCalories
import io.github.albertrzad18.treningtracker.ui.format.formatDateTime
import io.github.albertrzad18.treningtracker.ui.format.formatDistanceKm
import io.github.albertrzad18.treningtracker.ui.format.formatDuration
import io.github.albertrzad18.treningtracker.ui.format.formatHoursAndMinutes
import io.github.albertrzad18.treningtracker.ui.format.formatRangeShort
import io.github.albertrzad18.treningtracker.ui.goals.GoalProgressBlock
import io.github.albertrzad18.treningtracker.ui.goals.GoalsViewModel
import io.github.albertrzad18.treningtracker.ui.theme.LocalTtColors
import io.github.albertrzad18.treningtracker.ui.theme.TtShape
import io.github.albertrzad18.treningtracker.ui.theme.TtSpace
import io.github.albertrzad18.treningtracker.ui.theme.TtType
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
    DashboardContent(data, onOpenWorkout, onOpenSummaries, onOpenGoals)
}

@Composable
internal fun DashboardContent(
    data: DashboardData?,
    onOpenWorkout: (String) -> Unit,
    onOpenSummaries: () -> Unit,
    onOpenGoals: () -> Unit,
) {
    when {
        data == null -> DashboardSkeleton()
        data.isEmpty -> DashboardEmpty()
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = TtSpace.screenMargin,
                end = TtSpace.screenMargin,
                top = TtSpace.x2,
                bottom = TtSpace.x6,
            ),
            verticalArrangement = Arrangement.spacedBy(TtSpace.panelGap),
        ) {
            item { WeekPanel(data) }
            item { LatestPanel(data.latest, onOpenWorkout, onOpenSummaries) }
            item { GoalsPanel(onOpenGoals) }
        }
    }
}

/** Najważniejszy kafelek: ile przepalonych kalorii, podział na dyscypliny i porównanie z poprzednim tygodniem. */
@Composable
private fun WeekPanel(d: DashboardData) {
    val colors = LocalTtColors.current
    Panel {
        LabelText(
            stringResource(
                R.string.dashboard_week_label,
                formatRangeShort(d.weekRange.start, d.weekRange.endExclusive.minusDays(1)),
            ),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.dashboard_burned),
            style = TtType.body.copy(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium),
            color = colors.ink2,
        )
        Spacer(Modifier.height(TtSpace.x1))
        // Tydzień bez kalorii (sam trenażer bez danych) pokazuje czas, a nie zero.
        val kcal = d.week.caloriesKcal
        if (kcal != null) {
            BigValue(caloriesParts(kcal), style = TtType.heroXL)
        } else {
            BigValue(formatHoursAndMinutes(d.week.duration), style = TtType.heroXL)
        }
        Spacer(Modifier.height(10.dp))
        d.perDiscipline.filter { it.totals.count > 0 }.forEach { DisciplineSplitRow(it) }
        val change = d.caloriesChangePercent ?: d.durationChangePercent
        if (change != null) {
            Spacer(Modifier.height(TtSpace.x1))
            DeltaBadge(change, text = deltaWeekText(change))
        }
        Spacer(Modifier.height(TtSpace.x3))
        TtDivider()
        Spacer(Modifier.height(TtSpace.x4))
        WeekBars(d)
        Spacer(Modifier.height(TtSpace.x3))
        TtDivider()
        Spacer(Modifier.height(TtSpace.x4))
        DisciplineColumns(d.perDiscipline)
    }
}

/** Wiersz dyscypliny w kafelku tygodnia: kropka, nazwa, czas, dystans i kalorie. */
@Composable
private fun DisciplineSplitRow(item: DisciplineTotals) {
    val colors = LocalTtColors.current
    Column {
        TtDivider()
        Row(
            Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TtSpace.x3),
        ) {
            DisciplineDot(item.discipline)
            Text(
                stringResource(item.discipline.labelRes()),
                style = TtType.body.copy(fontWeight = FontWeight.Medium),
                color = colors.ink,
                modifier = Modifier.weight(1f),
            )
            // Stałe szerokości, żeby liczby w kolejnych wierszach stały w jednej kolumnie.
            Text(
                formatHoursAndMinutes(item.totals.duration),
                style = TtType.body.copy(fontWeight = FontWeight.SemiBold),
                color = colors.ink,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier.width(68.dp),
            )
            Text(
                item.totals.distanceM?.let(::formatDistanceKm).orEmpty(),
                style = TtType.bodyS,
                color = colors.ink2,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier.width(48.dp),
            )
            Text(
                item.totals.caloriesKcal?.let(::formatCalories).orEmpty(),
                style = TtType.bodyS,
                color = colors.ink2,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier.width(56.dp),
            )
        }
    }
}

/**
 * Dwa paski czasu: ten tydzień i poprzedni, na wspólnej skali.
 * Krótszy tydzień jest węższy, więc od razu widać, czy było więcej czy mniej.
 */
@Composable
private fun WeekBars(d: DashboardData) {
    val colors = LocalTtColors.current
    val current = d.week.duration.seconds.toFloat()
    val previous = d.previousWeek.duration.seconds.toFloat()
    val longest = maxOf(current, previous, 1f)

    fun parts(list: List<DisciplineTotals>) = list
        .filter { it.totals.duration.seconds > 0 }
        .map { colors.of(it.discipline) to it.totals.duration.seconds.toFloat() }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.dashboard_this_week),
                    style = TtType.bodyS.copy(fontWeight = FontWeight.Medium),
                    color = colors.ink,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    formatHoursAndMinutes(d.week.duration),
                    style = TtType.bodyS.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.ink,
                )
            }
            ShareBar(parts(d.perDiscipline), widthFraction = current / longest)
        }
        if (previous > 0f) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.dashboard_prev_week),
                        style = TtType.bodyS,
                        color = colors.ink2,
                        modifier = Modifier.weight(1f),
                    )
                    Text(formatHoursAndMinutes(d.previousWeek.duration), style = TtType.bodyS, color = colors.ink2)
                }
                ShareBar(parts(d.previousPerDiscipline), widthFraction = previous / longest, alpha = 0.4f)
            }
        }
    }
}

/** Trzy kolumny dyscyplin: pasek w kolorze, nazwa, liczba treningów i jedna liczba pod spodem. */
@Composable
private fun DisciplineColumns(items: List<DisciplineTotals>) {
    val colors = LocalTtColors.current
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        items.forEachIndexed { index, item ->
            if (index > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(colors.line))
            val hasWorkouts = item.totals.count > 0
            Column(
                Modifier.weight(1f).padding(horizontal = TtSpace.x3),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(
                            if (hasWorkouts) colors.of(item.discipline) else colors.line,
                            RoundedCornerShape(2.dp),
                        ),
                )
                Spacer(Modifier.height(2.dp))
                LabelText(stringResource(item.discipline.labelRes()), color = colors.ink)
                Text(
                    item.totals.count.toString(),
                    style = TtType.value,
                    color = if (hasWorkouts) colors.ink else colors.ink2,
                )
                Text(
                    columnValue(item),
                    style = TtType.bodyS,
                    color = colors.ink2,
                    modifier = Modifier.heightIn(min = 18.dp),
                )
            }
        }
    }
}

/**
 * Liczba pod liczbą treningów: dystans dla basenu i roweru, czas dla trenażera.
 * Rower bez treningów pokazuje `0 km` — to jedyny zamówiony wyjątek od zasady „brak danych = brak pola”.
 */
@Composable
private fun columnValue(item: DisciplineTotals): String = when (item.discipline) {
    Discipline.ROAD -> formatDistanceKm(item.totals.distanceM ?: 0.0)
    Discipline.POOL -> item.totals.distanceM?.let(::formatDistanceKm).orEmpty()
    Discipline.TRAINER -> if (item.totals.duration.isZero) "" else formatHoursAndMinutes(item.totals.duration)
}

/** Ostatnie trzy treningi i link do podsumowań. */
@Composable
private fun LatestPanel(latest: List<Workout>, onOpenWorkout: (String) -> Unit, onOpenSummaries: () -> Unit) {
    val colors = LocalTtColors.current
    val zone = ZoneId.systemDefault()
    Panel {
        SectionHeader(stringResource(R.string.dashboard_latest))
        Spacer(Modifier.height(TtSpace.x2))
        latest.forEach { workout ->
            TtDivider()
            Row(
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 60.dp)
                    .ttClickable { onOpenWorkout(workout.id) }
                    .padding(vertical = TtSpace.x2),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TtSpace.x3),
            ) {
                DisciplineDot(workout.discipline)
                Column(Modifier.weight(1f)) {
                    Text(
                        workout.title ?: stringResource(workout.discipline.labelRes()),
                        style = TtType.body.copy(fontWeight = FontWeight.Medium),
                        color = colors.ink,
                    )
                    Text(formatDateTime(workout.localDateTime(zone)), style = TtType.bodyS, color = colors.ink2)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        formatDuration(workout.duration),
                        style = TtType.body.copy(fontWeight = FontWeight.Medium),
                        color = colors.ink,
                    )
                    // Trenażer zwykle nie ma dystansu, więc pokazujemy kalorie.
                    val second = workout.distanceM?.let(::formatDistanceKm)
                        ?: workout.caloriesKcal?.let(::formatCalories)
                    if (second != null) Text(second, style = TtType.bodyS, color = colors.ink2)
                }
            }
        }
        LinkRow(stringResource(R.string.dashboard_open_summaries), onOpenSummaries)
    }
}

/** Postęp celów: najwyżej dwa, reszta na ekranie Cele. Bez celów sekcja znika. */
@Composable
private fun GoalsPanel(onOpenGoals: () -> Unit, vm: GoalsViewModel = viewModel()) {
    val rows by vm.rows.collectAsStateWithLifecycle()
    val list = rows.orEmpty()
    if (list.isEmpty()) return
    Panel {
        SectionHeader(stringResource(R.string.dashboard_goals))
        list.take(2).forEach { row ->
            Spacer(Modifier.height(TtSpace.x5))
            GoalProgressBlock(row, showForecast = false, showStreak = false)
        }
        Spacer(Modifier.height(TtSpace.x4))
        LinkRow(stringResource(R.string.dashboard_all_goals), onOpenGoals, divider = false)
    }
}

/** Ładowanie: te same bloki co z danymi, tylko w kolorze toru. */
@Composable
private fun DashboardSkeleton() {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = TtSpace.screenMargin)
            .padding(top = TtSpace.x5),
        verticalArrangement = Arrangement.spacedBy(TtSpace.x5),
    ) {
        SkeletonBlock(height = 14.dp, width = 140.dp)
        SkeletonBlock(height = 72.dp, width = 260.dp)
        SkeletonBlock(height = 16.dp, width = 180.dp)
        SkeletonBlock(height = 28.dp, width = 230.dp, shape = TtShape.full)
        Row(horizontalArrangement = Arrangement.spacedBy(TtSpace.panelGap)) {
            repeat(3) { SkeletonBlock(height = 84.dp, modifier = Modifier.weight(1f)) }
        }
        SkeletonBlock(height = 140.dp)
        SkeletonBlock(height = 64.dp)
        SkeletonBlock(height = 64.dp)
    }
}

/** Pusto: co zrobić, żeby treningi się pojawiły. */
@Composable
private fun DashboardEmpty() {
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = TtSpace.screenMargin)
            .padding(top = 96.dp),
        verticalArrangement = Arrangement.spacedBy(TtSpace.x4),
    ) {
        Text(
            stringResource(R.string.dashboard_empty_title),
            style = TtType.headline,
            color = LocalTtColors.current.ink,
        )
        Text(
            stringResource(R.string.dashboard_empty_body),
            style = TtType.body,
            color = LocalTtColors.current.ink2,
        )
        PrimaryButton(
            text = stringResource(R.string.sync_now),
            onClick = { SyncScheduler.syncNow(context) },
            icon = TtIcons.Sync,
            modifier = Modifier.padding(top = TtSpace.x2),
        )
        NumberedSteps(
            listOf(
                stringResource(R.string.dashboard_empty_step_1),
                stringResource(R.string.dashboard_empty_step_2),
                stringResource(R.string.dashboard_empty_step_3),
            ),
            modifier = Modifier.padding(top = TtSpace.x4),
        )
    }
}
