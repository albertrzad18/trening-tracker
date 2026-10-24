package io.github.albertrzad18.treningtracker.ui.stats

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import io.github.albertrzad18.treningtracker.domain.Bucket
import io.github.albertrzad18.treningtracker.domain.CumulativePoint
import io.github.albertrzad18.treningtracker.domain.Discipline
import io.github.albertrzad18.treningtracker.domain.DisciplineShare
import io.github.albertrzad18.treningtracker.domain.Granularity
import io.github.albertrzad18.treningtracker.domain.HeatmapData
import io.github.albertrzad18.treningtracker.domain.PeriodFilter
import io.github.albertrzad18.treningtracker.domain.StatsMetric
import io.github.albertrzad18.treningtracker.domain.TimeOfDayBin
import io.github.albertrzad18.treningtracker.domain.Totals
import io.github.albertrzad18.treningtracker.domain.TrendMetric
import io.github.albertrzad18.treningtracker.domain.TrendPoint
import io.github.albertrzad18.treningtracker.domain.activityHeatmap
import io.github.albertrzad18.treningtracker.domain.bucketize
import io.github.albertrzad18.treningtracker.domain.byDayOfWeek
import io.github.albertrzad18.treningtracker.domain.byTimeOfDay
import io.github.albertrzad18.treningtracker.domain.cumulativeDistance
import io.github.albertrzad18.treningtracker.domain.disciplineShares
import io.github.albertrzad18.treningtracker.domain.granularityFor
import io.github.albertrzad18.treningtracker.domain.inRange
import io.github.albertrzad18.treningtracker.domain.metricValue
import io.github.albertrzad18.treningtracker.domain.statsRange
import io.github.albertrzad18.treningtracker.domain.trend
import io.github.albertrzad18.treningtracker.ui.charts.ActivityHeatmap
import io.github.albertrzad18.treningtracker.ui.charts.ChartCard
import io.github.albertrzad18.treningtracker.ui.charts.ChartLine
import io.github.albertrzad18.treningtracker.ui.charts.ChartSeries
import io.github.albertrzad18.treningtracker.ui.charts.ColorLegend
import io.github.albertrzad18.treningtracker.ui.charts.DisciplineDonut
import io.github.albertrzad18.treningtracker.ui.charts.DisciplineLegend
import io.github.albertrzad18.treningtracker.ui.charts.MultiLineChart
import io.github.albertrzad18.treningtracker.ui.charts.StackedBarChart
import io.github.albertrzad18.treningtracker.ui.common.DisciplineChips
import io.github.albertrzad18.treningtracker.ui.common.MetricChips
import io.github.albertrzad18.treningtracker.ui.common.PeriodChips
import io.github.albertrzad18.treningtracker.ui.common.color
import io.github.albertrzad18.treningtracker.ui.common.labelRes
import io.github.albertrzad18.treningtracker.ui.format.formatDayShort
import io.github.albertrzad18.treningtracker.ui.format.formatDecimalShort
import io.github.albertrzad18.treningtracker.ui.format.formatKmShort
import io.github.albertrzad18.treningtracker.ui.format.formatMinutesShort
import io.github.albertrzad18.treningtracker.ui.format.formatMonthShort
import io.github.albertrzad18.treningtracker.ui.format.formatMonthWithYear
import io.github.albertrzad18.treningtracker.ui.format.formatNumberShort
import io.github.albertrzad18.treningtracker.ui.format.formatPaceShort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class StatsFilters(
    val disciplines: Set<Discipline> = emptySet(),
    val period: PeriodFilter = PeriodFilter.YEAR,
    val metric: StatsMetric = StatsMetric.TIME,
)

/** Wszystko, co rysują wykresy na ekranie Statystyki. Liczone raz, w ViewModelu. */
data class StatsData(
    val isEmpty: Boolean,
    val buckets: List<Bucket>,
    val granularity: Granularity,
    val shownDisciplines: List<Discipline>,
    val heatmap: HeatmapData,
    val shares: List<DisciplineShare>,
    val byDayOfWeek: Map<DayOfWeek, Totals>,
    val byTimeOfDay: List<TimeOfDayBin>,
    val paceTrends: Map<Discipline, List<TrendPoint>>,
    val efficiencyTrends: Map<Discipline, List<TrendPoint>>,
    val thisYear: Int,
    val cumulativeThisYear: List<CumulativePoint>,
    val cumulativePreviousYear: List<CumulativePoint>,
)

class StatsViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as TreningApp).database.workoutDao()
    private val _filters = MutableStateFlow(StatsFilters())
    val filters: StateFlow<StatsFilters> = _filters

    /** `null` = jeszcze się wczytuje. */
    val data: StateFlow<StatsData?> = combine(dao.observeAll(), _filters) { entities, f ->
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val all = entities.map { it.toDomain() }
        val selected = if (f.disciplines.isEmpty()) all else all.filter { it.discipline in f.disciplines }
        val range = statsRange(selected, f.period, today, zone)
        val inPeriod = selected.inRange(range, zone)
        val granularity = granularityFor(f.period)
        val shown = if (f.disciplines.isEmpty()) Discipline.entries.toList() else Discipline.entries.filter { it in f.disciplines }
        StatsData(
            isEmpty = all.isEmpty(),
            buckets = inPeriod.bucketize(range, granularity, zone),
            granularity = granularity,
            shownDisciplines = shown,
            heatmap = selected.activityHeatmap(today, zone),
            shares = inPeriod.disciplineShares(),
            byDayOfWeek = inPeriod.byDayOfWeek(zone),
            byTimeOfDay = inPeriod.byTimeOfDay(zone),
            // Trendy i dystans narastająco liczymy z całej historii: na krótkim okresie nie byłoby czego pokazać.
            paceTrends = shown.associateWith { selected.trend(it, TrendMetric.SPEED_OR_PACE, zone) },
            efficiencyTrends = shown.associateWith { selected.trend(it, TrendMetric.EFFICIENCY, zone) },
            thisYear = today.year,
            cumulativeThisYear = selected.cumulativeDistance(today.year, zone, today),
            cumulativePreviousYear = selected.cumulativeDistance(today.year - 1, zone, today),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun toggleDiscipline(d: Discipline) = _filters.update {
        it.copy(disciplines = if (d in it.disciplines) it.disciplines - d else it.disciplines + d)
    }

    fun setPeriod(p: PeriodFilter) = _filters.update { it.copy(period = p) }

    fun setMetric(m: StatsMetric) = _filters.update { it.copy(metric = m) }
}

@Composable
fun StatsScreen(onOpenSummaries: () -> Unit, vm: StatsViewModel = viewModel()) {
    val filters by vm.filters.collectAsStateWithLifecycle()
    val data by vm.data.collectAsStateWithLifecycle()
    val d = data

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            DisciplineChips(filters.disciplines, vm::toggleDiscipline)
            PeriodChips(filters.period, vm::setPeriod)
        }
        when {
            d == null -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            d.isEmpty -> Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
                Text(stringResource(R.string.stats_empty), style = MaterialTheme.typography.bodyLarge)
            }
            else -> Charts(d, filters, vm::setMetric, onOpenSummaries)
        }
    }
}

@Composable
private fun Charts(
    d: StatsData,
    filters: StatsFilters,
    onMetric: (StatsMetric) -> Unit,
    onOpenSummaries: () -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { VolumeCard(d, filters, onMetric) }
        item {
            ChartCard(
                title = stringResource(R.string.chart_heatmap_title),
                description = stringResource(R.string.chart_heatmap_desc),
                hasData = !d.heatmap.isEmpty,
            ) { ActivityHeatmap(d.heatmap) }
        }
        item {
            ChartCard(
                title = stringResource(R.string.chart_share_title),
                description = stringResource(R.string.chart_share_desc),
                hasData = d.shares.isNotEmpty(),
            ) { DisciplineDonut(d.shares) }
        }
        item { WeekdayCard(d) }
        item { TimeOfDayCard(d) }
        item { CumulativeCard(d) }
        for (discipline in d.shownDisciplines) {
            // Trend i forma mają inne jednostki dla basenu i roweru, więc każda dyscyplina dostaje własną kartę.
            item(key = "trend-$discipline") { TrendCard(discipline, d.paceTrends[discipline].orEmpty()) }
            item(key = "eff-$discipline") { EfficiencyCard(discipline, d.efficiencyTrends[discipline].orEmpty()) }
        }
        item {
            TextButton(onClick = onOpenSummaries) { Text(stringResource(R.string.more_summaries)) }
        }
    }
}

/** Etykieta słupka: dzień `24.10`, miesiąc `paź` albo `paź 26`, gdy wykres obejmuje różne lata. */
private fun Bucket.label(withYear: Boolean): String = when (granularity) {
    Granularity.DAY -> formatDayShort(start)
    Granularity.WEEK -> formatDayShort(start)
    Granularity.MONTH -> if (withYear) formatMonthWithYear(YearMonth.from(start)) else formatMonthShort(YearMonth.from(start))
}

@Composable
private fun VolumeCard(d: StatsData, filters: StatsFilters, onMetric: (StatsMetric) -> Unit) {
    val metric = filters.metric
    val series = d.shownDisciplines.map { discipline ->
        ChartSeries(
            color = discipline.color(),
            values = d.buckets.map { it.perDiscipline[discipline]?.metricValue(metric) ?: 0.0 },
        )
    }
    val hasData = series.any { s -> s.values.any { it > 0.0 } }
    val withYear = d.buckets.map { it.start.year }.distinct().size > 1
    ChartCard(
        title = stringResource(R.string.chart_volume_title),
        description = stringResource(
            when (metric) {
                StatsMetric.TIME -> R.string.chart_volume_desc_time
                StatsMetric.DISTANCE -> R.string.chart_volume_desc_distance
                StatsMetric.CALORIES -> R.string.chart_volume_desc_calories
            },
        ),
        hasData = hasData,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricChips(metric, onMetric)
            StackedBarChart(
                bars = series,
                xLabel = { i -> d.buckets.getOrNull(i)?.label(withYear).orEmpty() },
                yLabel = { v -> metric.axisLabel(v) },
                labelEvery = (d.buckets.size / 6).coerceAtLeast(1),
            )
            DisciplineLegend(d.shownDisciplines)
        }
    }
}

private fun StatsMetric.axisLabel(value: Double): String = when (this) {
    StatsMetric.TIME -> formatMinutesShort(value)
    StatsMetric.DISTANCE -> formatKmShort(value)
    StatsMetric.CALORIES -> formatNumberShort(value)
}

@Composable
private fun WeekdayCard(d: StatsData) {
    val days = DayOfWeek.entries
    val minutes = days.map { d.byDayOfWeek[it]?.duration?.toMinutes()?.toDouble() ?: 0.0 }
    ChartCard(
        title = stringResource(R.string.chart_weekday_title),
        description = stringResource(R.string.chart_weekday_desc),
        hasData = minutes.any { it > 0.0 },
    ) {
        StackedBarChart(
            bars = listOf(ChartSeries(MaterialTheme.colorScheme.primary, minutes)),
            xLabel = { i -> days.getOrNull(i)?.shortLabel().orEmpty() },
            yLabel = ::formatMinutesShort,
            height = 150.dp,
        )
    }
}

@Composable
private fun TimeOfDayCard(d: StatsData) {
    val minutes = d.byTimeOfDay.map { it.totals.duration.toMinutes().toDouble() }
    ChartCard(
        title = stringResource(R.string.chart_timeofday_title),
        description = stringResource(R.string.chart_timeofday_desc),
        hasData = minutes.any { it > 0.0 },
    ) {
        StackedBarChart(
            bars = listOf(ChartSeries(MaterialTheme.colorScheme.tertiary, minutes)),
            xLabel = { i -> d.byTimeOfDay.getOrNull(i)?.let { "${it.fromHour}–${it.toHour}" }.orEmpty() },
            yLabel = ::formatMinutesShort,
            height = 150.dp,
        )
    }
}

@Composable
private fun TrendCard(discipline: Discipline, points: List<TrendPoint>) {
    val isPool = discipline == Discipline.POOL
    ChartCard(
        title = stringResource(R.string.chart_trend_title, stringResource(discipline.labelRes())),
        description = stringResource(if (isPool) R.string.chart_trend_desc_pace else R.string.chart_trend_desc_speed),
        hasData = points.size >= 2,
        emptyText = stringResource(R.string.chart_need_two),
    ) {
        MultiLineChart(
            lines = listOf(
                ChartLine(discipline.color().copy(alpha = 0.4f), points.mapIndexed { i, p -> i.toDouble() to p.value }),
                ChartLine(discipline.color(), points.mapIndexed { i, p -> i.toDouble() to p.average }),
            ),
            xLabel = { x -> points.getOrNull(x.toInt())?.date?.let(::formatDayShort).orEmpty() },
            yLabel = { v -> if (isPool) formatPaceShort(v) else formatDecimalShort(v) },
        )
    }
}

@Composable
private fun EfficiencyCard(discipline: Discipline, points: List<TrendPoint>) {
    ChartCard(
        title = stringResource(R.string.chart_efficiency_title, stringResource(discipline.labelRes())),
        description = stringResource(R.string.chart_efficiency_desc),
        hasData = points.size >= 2,
        emptyText = stringResource(R.string.chart_need_two),
    ) {
        MultiLineChart(
            lines = listOf(
                ChartLine(discipline.color().copy(alpha = 0.4f), points.mapIndexed { i, p -> i.toDouble() to p.value }),
                ChartLine(discipline.color(), points.mapIndexed { i, p -> i.toDouble() to p.average }),
            ),
            xLabel = { x -> points.getOrNull(x.toInt())?.date?.let(::formatDayShort).orEmpty() },
            yLabel = ::formatDecimalShort,
            height = 150.dp,
        )
    }
}

@Composable
private fun CumulativeCard(d: StatsData) {
    val previous = d.cumulativePreviousYear
    val current = d.cumulativeThisYear
    ChartCard(
        title = stringResource(R.string.chart_cumulative_title),
        description = if (previous.isNotEmpty()) {
            stringResource(R.string.chart_cumulative_desc, d.thisYear, d.thisYear - 1)
        } else {
            stringResource(R.string.chart_cumulative_desc_single, d.thisYear)
        },
        hasData = current.size >= 2 || previous.size >= 2,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MultiLineChart(
                lines = listOfNotNull(
                    previous.takeIf { it.size >= 2 }?.let { points ->
                        ChartLine(MaterialTheme.colorScheme.outline, points.map { it.dayOfYear.toDouble() to it.km })
                    },
                    current.takeIf { it.size >= 2 }?.let { points ->
                        ChartLine(MaterialTheme.colorScheme.primary, points.map { it.dayOfYear.toDouble() to it.km })
                    },
                ),
                xLabel = { x ->
                    formatMonthShort(YearMonth.from(LocalDate.ofYearDay(d.thisYear, x.toInt().coerceIn(1, 365))))
                },
                yLabel = ::formatKmShort,
                zeroBased = true,
                // Oś X to dni roku, więc etykieta co dwa miesiące zamiast co dzień.
                labelEvery = 61,
            )
            ColorLegend(
                listOfNotNull(
                    MaterialTheme.colorScheme.primary to d.thisYear.toString(),
                    previous.takeIf { it.size >= 2 }
                        ?.let { MaterialTheme.colorScheme.outline to (d.thisYear - 1).toString() },
                ),
            )
        }
    }
}

/** `pon`, `wt`, ... po polsku. */
private fun DayOfWeek.shortLabel(): String =
    getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.forLanguageTag("pl-PL"))
