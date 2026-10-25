package io.github.albertrzad18.treningtracker.ui.stats

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import io.github.albertrzad18.treningtracker.ui.charts.LineSpec
import io.github.albertrzad18.treningtracker.ui.charts.StatPanel
import io.github.albertrzad18.treningtracker.ui.charts.TtBarChart
import io.github.albertrzad18.treningtracker.ui.charts.TtLineChart
import io.github.albertrzad18.treningtracker.ui.common.DisciplineDot
import io.github.albertrzad18.treningtracker.ui.common.DisciplineFilterChips
import io.github.albertrzad18.treningtracker.ui.common.PeriodMenuChip
import io.github.albertrzad18.treningtracker.ui.common.SegmentedToggle
import io.github.albertrzad18.treningtracker.ui.common.ShareBar
import io.github.albertrzad18.treningtracker.ui.common.SkeletonBlock
import io.github.albertrzad18.treningtracker.ui.common.TtDivider
import io.github.albertrzad18.treningtracker.ui.common.labelRes
import io.github.albertrzad18.treningtracker.ui.common.ttClickable
import io.github.albertrzad18.treningtracker.ui.format.ValueUnit
import io.github.albertrzad18.treningtracker.ui.format.caloriesParts
import io.github.albertrzad18.treningtracker.ui.format.distanceParts
import io.github.albertrzad18.treningtracker.ui.format.formatDate
import io.github.albertrzad18.treningtracker.ui.format.formatDayShort
import io.github.albertrzad18.treningtracker.ui.format.formatDecimalShort
import io.github.albertrzad18.treningtracker.ui.format.formatHoursAndMinutes
import io.github.albertrzad18.treningtracker.ui.format.formatKmShort
import io.github.albertrzad18.treningtracker.ui.format.formatMinutesShort
import io.github.albertrzad18.treningtracker.ui.format.formatMonthShort
import io.github.albertrzad18.treningtracker.ui.format.formatMonthWithYear
import io.github.albertrzad18.treningtracker.ui.format.formatNumberShort
import io.github.albertrzad18.treningtracker.ui.format.formatPaceShort
import io.github.albertrzad18.treningtracker.ui.format.formatPercent
import io.github.albertrzad18.treningtracker.ui.theme.LocalTtColors
import io.github.albertrzad18.treningtracker.ui.theme.TtSpace
import io.github.albertrzad18.treningtracker.ui.theme.TtType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** Zakładki na ekranie Statystyki. */
enum class StatsTab { VOLUME, CALENDAR, SHARE, RHYTHM, PROGRESS }

data class StatsFilters(
    val disciplines: Set<Discipline> = emptySet(),
    val period: PeriodFilter = PeriodFilter.YEAR,
    val metric: StatsMetric = StatsMetric.TIME,
    val tab: StatsTab = StatsTab.VOLUME,
    val trendDiscipline: Discipline = Discipline.POOL,
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
        val shown = if (f.disciplines.isEmpty()) {
            Discipline.entries.toList()
        } else {
            Discipline.entries.filter { it in f.disciplines }
        }
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
            paceTrends = Discipline.entries.associateWith { selected.trend(it, TrendMetric.SPEED_OR_PACE, zone) },
            efficiencyTrends = Discipline.entries.associateWith { selected.trend(it, TrendMetric.EFFICIENCY, zone) },
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

    fun setTab(t: StatsTab) = _filters.update { it.copy(tab = t) }

    fun setTrendDiscipline(d: Discipline) = _filters.update { it.copy(trendDiscipline = d) }
}

@Composable
fun StatsScreen(vm: StatsViewModel = viewModel()) {
    val filters by vm.filters.collectAsStateWithLifecycle()
    val data by vm.data.collectAsStateWithLifecycle()
    StatsContent(
        filters = filters,
        data = data,
        onToggleDiscipline = vm::toggleDiscipline,
        onPeriod = vm::setPeriod,
        onTab = vm::setTab,
        onMetric = vm::setMetric,
        onTrendDiscipline = vm::setTrendDiscipline,
    )
}

/** Sam widok statystyk: bez ViewModelu, więc da się go pokazać w `@Preview`. */
@Composable
internal fun StatsContent(
    filters: StatsFilters,
    data: StatsData?,
    onToggleDiscipline: (Discipline) -> Unit,
    onPeriod: (PeriodFilter) -> Unit,
    onTab: (StatsTab) -> Unit,
    onMetric: (StatsMetric) -> Unit,
    onTrendDiscipline: (Discipline) -> Unit,
) {
    val colors = LocalTtColors.current
    val d = data

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = TtSpace.screenMargin)
                .padding(top = TtSpace.x2, bottom = TtSpace.x4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DisciplineFilterChips(filters.disciplines, onToggleDiscipline, modifier = Modifier.weight(1f))
            PeriodMenuChip(filters.period, onPeriod)
        }
        TabRow(filters.tab, onTab)
        Spacer(Modifier.height(TtSpace.panelGap))
        when {
            d == null -> StatsSkeleton()
            d.isEmpty -> Text(
                stringResource(R.string.stats_empty),
                style = TtType.body,
                color = colors.ink2,
                modifier = Modifier.padding(horizontal = TtSpace.screenMargin).padding(top = 56.dp),
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(
                    start = TtSpace.screenMargin,
                    end = TtSpace.screenMargin,
                    bottom = TtSpace.x6,
                ),
                verticalArrangement = Arrangement.spacedBy(TtSpace.panelGap),
            ) {
                when (filters.tab) {
                    StatsTab.VOLUME -> item { VolumePanel(d, filters, onMetric) }
                    StatsTab.CALENDAR -> item { CalendarPanel(d) }
                    StatsTab.SHARE -> item { SharePanel(d) }
                    StatsTab.RHYTHM -> {
                        item { WeekdayPanel(d) }
                        item { TimeOfDayPanel(d) }
                    }
                    StatsTab.PROGRESS -> {
                        item { CumulativePanel(d) }
                        item { TrendPanel(d, filters, onTrendDiscipline) }
                        item { FormPanel(d) }
                    }
                }
            }
        }
    }
}

/** Zakładki tekstowe z podkreśleniem: przewijane w poziomie, bo pięć nazw nie zmieści się na 360 dp. */
@Composable
private fun TabRow(selected: StatsTab, onSelect: (StatsTab) -> Unit) {
    val colors = LocalTtColors.current
    Box(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().align(Alignment.BottomStart)) { TtDivider() }
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = TtSpace.screenMargin),
            horizontalArrangement = Arrangement.spacedBy(TtSpace.x5),
        ) {
            StatsTab.entries.forEach { tab ->
                val isSelected = tab == selected
                Column(
                    Modifier.ttClickable { onSelect(tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.height(44.dp), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(tab.labelRes()),
                            style = TtType.body.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            ),
                            color = if (isSelected) colors.ink else colors.ink2,
                            maxLines = 1,
                        )
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(if (isSelected) colors.ink else androidx.compose.ui.graphics.Color.Transparent),
                    )
                }
            }
        }
    }
}

private fun StatsTab.labelRes() = when (this) {
    StatsTab.VOLUME -> R.string.stats_tab_volume
    StatsTab.CALENDAR -> R.string.stats_tab_calendar
    StatsTab.SHARE -> R.string.stats_tab_share
    StatsTab.RHYTHM -> R.string.stats_tab_rhythm
    StatsTab.PROGRESS -> R.string.stats_tab_progress
}

/** Objętość: słupki warstwowe po miesiącach (albo dniach) z przełącznikiem metryki. */
@Composable
private fun VolumePanel(d: StatsData, filters: StatsFilters, onMetric: (StatsMetric) -> Unit) {
    val colors = LocalTtColors.current
    val metric = filters.metric
    var selected by remember(metric, d.buckets) { mutableStateOf<Int?>(null) }
    val slots = d.buckets.map { bucket ->
        d.shownDisciplines.map { discipline -> bucket.perDiscipline[discipline]?.metricValue(metric) ?: 0.0 }
    }
    val totals = slots.map { it.sum() }
    val sum = totals.sum()
    val withYear = d.buckets.map { it.start.year }.distinct().size > 1
    val step = (d.buckets.size / 6).coerceAtLeast(1)

    StatPanel(
        title = stringResource(R.string.chart_volume_title),
        subtitle = stringResource(
            when (metric) {
                StatsMetric.TIME -> R.string.chart_volume_desc_time
                StatsMetric.DISTANCE -> R.string.chart_volume_desc_distance
                StatsMetric.CALORIES -> R.string.chart_volume_desc_calories
            },
        ),
        value = selected?.let { index ->
            d.buckets.getOrNull(index)?.let { bucket ->
                ValueUnit(metric.shortLabel(totals[index]), bucket.label(withYear = true))
            }
        } ?: metric.bigValue(sum),
        toggle = {
            SegmentedToggle(
                options = StatsMetric.entries,
                selected = metric,
                label = { stringResource(it.labelRes()) },
                onSelect = onMetric,
            )
        },
    ) {
        TtBarChart(
            slots = slots,
            layerColors = d.shownDisciplines.map { colors.of(it) },
            labels = d.buckets.mapIndexed { index, bucket ->
                if (index % step == 0) bucket.label(withYear) else ""
            },
            peakLabel = totals.maxOrNull()?.takeIf { it > 0.0 }?.let { metric.shortLabel(it) },
            selectedIndex = selected,
            onSelect = { selected = it },
        )
    }
}

/** Kalendarz: heatmapa ostatnich 26 tygodni. */
@Composable
private fun CalendarPanel(d: StatsData) {
    StatPanel(
        title = stringResource(R.string.chart_heatmap_title),
        subtitle = stringResource(R.string.chart_heatmap_desc),
        value = ValueUnit(d.heatmap.activeDays.toString(), stringResource(R.string.chart_heatmap_unit)),
    ) {
        ActivityHeatmap(d.heatmap)
    }
}

/** Udział: jeden pasek i legenda z procentem oraz czasem. */
@Composable
private fun SharePanel(d: StatsData) {
    val colors = LocalTtColors.current
    val total = Duration.ofSeconds(d.shares.sumOf { it.duration.seconds })
    StatPanel(
        title = stringResource(R.string.chart_share_title),
        subtitle = stringResource(R.string.chart_share_desc),
        value = ValueUnit(formatHoursAndMinutes(total), stringResource(R.string.chart_share_unit)),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(TtSpace.x3)) {
            ShareBar(
                parts = d.shares.map { colors.of(it.discipline) to it.fraction.toFloat() },
                height = 16.dp,
                gap = 3.dp,
            )
            Column {
                d.shares.forEach { share ->
                    TtDivider()
                    Row(
                        Modifier.fillMaxWidth().defaultMinSize(minHeight = 44.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(TtSpace.x3),
                    ) {
                        DisciplineDot(share.discipline)
                        Text(
                            stringResource(share.discipline.labelRes()),
                            style = TtType.body,
                            color = colors.ink,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "${formatPercent(share.fraction)} · ${formatHoursAndMinutes(share.duration)}",
                            style = TtType.body.copy(fontWeight = FontWeight.Medium),
                            color = colors.ink,
                        )
                    }
                }
            }
        }
    }
}

/** Dni tygodnia: w które dni trenuję najwięcej. */
@Composable
private fun WeekdayPanel(d: StatsData) {
    val colors = LocalTtColors.current
    val days = DayOfWeek.entries
    val hours = days.map { (d.byDayOfWeek[it]?.duration?.toMinutes() ?: 0L) / 60.0 }
    val best = hours.withIndex().maxByOrNull { it.value }
    var selected by remember(hours) { mutableStateOf<Int?>(null) }

    StatPanel(
        title = stringResource(R.string.chart_weekday_title),
        subtitle = stringResource(R.string.chart_weekday_desc),
        value = selected?.let { index ->
            ValueUnit(formatNumberShort(hours[index]), "h · ${days[index].shortLabel()}")
        } ?: best?.takeIf { it.value > 0 }?.let {
            ValueUnit(formatNumberShort(it.value), stringResource(dayPhrase(days[it.index])))
        },
    ) {
        TtBarChart(
            slots = hours.map { listOf(it) },
            layerColors = listOf(singleColor(d, colors)),
            labels = days.map { it.shortLabel() },
            selectedIndex = selected,
            onSelect = { selected = it },
        )
    }
}

/** Pora dnia: o której zaczynam treningi. */
@Composable
private fun TimeOfDayPanel(d: StatsData) {
    val colors = LocalTtColors.current
    val hours = d.byTimeOfDay.map { it.totals.duration.toMinutes() / 60.0 }
    val best = hours.withIndex().maxByOrNull { it.value }
    var selected by remember(hours) { mutableStateOf<Int?>(null) }

    StatPanel(
        title = stringResource(R.string.chart_timeofday_title),
        subtitle = stringResource(R.string.chart_timeofday_desc),
        value = (selected ?: best?.index?.takeIf { best.value > 0 })?.let { index ->
            d.byTimeOfDay.getOrNull(index)?.let { bin ->
                ValueUnit(
                    formatNumberShort(hours[index]),
                    stringResource(R.string.chart_timeofday_unit, bin.fromHour, bin.toHour),
                )
            }
        },
    ) {
        TtBarChart(
            slots = hours.map { listOf(it) },
            layerColors = listOf(singleColor(d, colors)),
            labels = d.byTimeOfDay.map { "${it.fromHour}–${it.toHour}" },
            selectedIndex = selected,
            onSelect = { selected = it },
        )
    }
}

/** Dystans narastająco: bieżący rok grubą linią na tle poprzedniego. */
@Composable
private fun CumulativePanel(d: StatsData) {
    val colors = LocalTtColors.current
    val current = d.cumulativeThisYear
    val previous = d.cumulativePreviousYear
    val lastKm = current.lastOrNull()?.km
    StatPanel(
        title = stringResource(R.string.chart_cumulative_title),
        subtitle = if (previous.size >= 2) {
            stringResource(R.string.chart_cumulative_desc, d.thisYear, d.thisYear - 1)
        } else {
            stringResource(R.string.chart_cumulative_desc_single, d.thisYear)
        },
        value = lastKm?.let { distanceParts(it * 1000) },
    ) {
        if (current.size < 2 && previous.size < 2) {
            Text(stringResource(R.string.chart_no_data), style = TtType.bodyS, color = colors.ink2)
        } else {
            TtLineChart(
                lines = listOfNotNull(
                    previous.takeIf { it.size >= 2 }?.let { points ->
                        LineSpec(
                            points = points.map { it.dayOfYear.toDouble() to it.km },
                            color = colors.ink2,
                            width = 1.5.dp,
                            endLabel = "${d.thisYear - 1} · ${formatNumberShort(points.last().km)}",
                        )
                    },
                    current.takeIf { it.size >= 2 }?.let { points ->
                        LineSpec(
                            points = points.map { it.dayOfYear.toDouble() to it.km },
                            color = colors.ink,
                            width = 2.5.dp,
                            areaAlpha = 0.12f,
                            endLabel = "${d.thisYear} · ${formatNumberShort(points.last().km)}",
                        )
                    },
                ),
                xLabels = listOf(1, 91, 182, 274, 365).map {
                    formatMonthShort(YearMonth.from(LocalDate.ofYearDay(d.thisYear, it)))
                },
                zeroBased = true,
            )
        }
    }
}

/** Trend: pojedyncze treningi punktami, średnia z pięciu grubą linią w kolorze dyscypliny. */
@Composable
private fun TrendPanel(d: StatsData, filters: StatsFilters, onDiscipline: (Discipline) -> Unit) {
    val colors = LocalTtColors.current
    val discipline = filters.trendDiscipline
    val points = d.paceTrends[discipline].orEmpty()
    val isPool = discipline == Discipline.POOL
    StatPanel(
        title = stringResource(R.string.chart_trend_title, stringResource(discipline.labelRes())),
        subtitle = stringResource(if (isPool) R.string.chart_trend_desc_pace else R.string.chart_trend_desc_speed),
        value = points.lastOrNull()?.let { last ->
            if (isPool) {
                ValueUnit(formatPaceShort(last.average), "/100 m")
            } else {
                ValueUnit(formatDecimalShort(last.average), "km/h")
            }
        },
        toggle = {
            SegmentedToggle(
                options = Discipline.entries,
                selected = discipline,
                label = { stringResource(it.labelRes()) },
                onSelect = onDiscipline,
            )
        },
    ) {
        if (points.size < 2) {
            Text(stringResource(R.string.chart_need_two), style = TtType.bodyS, color = colors.ink2)
        } else {
            TtLineChart(
                lines = listOf(
                    LineSpec(
                        points = points.mapIndexed { index, point -> index.toDouble() to point.value },
                        color = colors.ink2.copy(alpha = 0.5f),
                        dotsOnly = true,
                    ),
                    LineSpec(
                        points = points.mapIndexed { index, point -> index.toDouble() to point.average },
                        color = colors.of(discipline),
                        width = 2.5.dp,
                    ),
                ),
                xLabels = trendLabels(points),
            )
        }
    }
}

/** Forma: kilometry na godzinę przy 100 uderzeniach tętna — tylko rower ma z czego to policzyć. */
@Composable
private fun FormPanel(d: StatsData) {
    val colors = LocalTtColors.current
    val points = d.efficiencyTrends[Discipline.ROAD].orEmpty()
    StatPanel(
        title = stringResource(R.string.chart_efficiency_title, stringResource(Discipline.ROAD.labelRes())),
        subtitle = stringResource(R.string.chart_efficiency_desc),
        value = points.lastOrNull()?.let { ValueUnit(formatDecimalShort(it.average), stringResource(R.string.chart_efficiency_unit)) },
    ) {
        if (points.size < 2) {
            Text(stringResource(R.string.chart_need_two), style = TtType.bodyS, color = colors.ink2)
        } else {
            TtLineChart(
                lines = listOf(
                    LineSpec(
                        points = points.mapIndexed { index, point -> index.toDouble() to point.value },
                        color = colors.ink2.copy(alpha = 0.5f),
                        dotsOnly = true,
                    ),
                    LineSpec(
                        points = points.mapIndexed { index, point -> index.toDouble() to point.average },
                        color = colors.of(Discipline.ROAD),
                        width = 2.5.dp,
                    ),
                ),
                xLabels = trendLabels(points),
            )
        }
    }
}

/** Trzy daty na osi X trendu: pierwszy, środkowy i ostatni trening. */
private fun trendLabels(points: List<TrendPoint>): List<String> {
    if (points.size < 2) return emptyList()
    return listOf(points.first(), points[points.size / 2], points.last()).map { formatDate(it.date) }
}

/** Słupki bez podziału na dyscypliny są w kolorze ink, a przy jednym wybranym filtrze w jej kolorze. */
private fun singleColor(d: StatsData, colors: io.github.albertrzad18.treningtracker.ui.theme.TtColors) =
    if (d.shownDisciplines.size == 1) colors.of(d.shownDisciplines.first()) else colors.ink

/** Etykieta słupka: dzień `24.10`, miesiąc `paź` albo `paź 26`, gdy wykres obejmuje różne lata. */
private fun Bucket.label(withYear: Boolean): String = when (granularity) {
    Granularity.DAY, Granularity.WEEK -> formatDayShort(start)
    Granularity.MONTH -> if (withYear) formatMonthWithYear(YearMonth.from(start)) else formatMonthShort(YearMonth.from(start))
}

/** Kluczowa liczba nad wykresem objętości. */
@Composable
private fun StatsMetric.bigValue(sum: Double): ValueUnit = when (this) {
    StatsMetric.TIME -> ValueUnit(formatHoursAndMinutes(Duration.ofMinutes(sum.toLong())), null)
    StatsMetric.DISTANCE -> distanceParts(sum * 1000)
    StatsMetric.CALORIES -> caloriesParts(sum)
}

/** Krótka wartość do etykiety nad najwyższym słupkiem i do wybranego słupka. */
private fun StatsMetric.shortLabel(value: Double): String = when (this) {
    StatsMetric.TIME -> formatMinutesShort(value)
    StatsMetric.DISTANCE -> formatKmShort(value)
    StatsMetric.CALORIES -> formatNumberShort(value)
}

private fun StatsMetric.labelRes() = when (this) {
    StatsMetric.TIME -> R.string.metric_label_time
    StatsMetric.DISTANCE -> R.string.metric_label_distance
    StatsMetric.CALORIES -> R.string.metric_label_calories
}

/** `h w soboty`, `h we wtorki` — jednostka przy najmocniejszym dniu tygodnia. */
private fun dayPhrase(day: DayOfWeek) = when (day) {
    DayOfWeek.MONDAY -> R.string.chart_weekday_unit_mon
    DayOfWeek.TUESDAY -> R.string.chart_weekday_unit_tue
    DayOfWeek.WEDNESDAY -> R.string.chart_weekday_unit_wed
    DayOfWeek.THURSDAY -> R.string.chart_weekday_unit_thu
    DayOfWeek.FRIDAY -> R.string.chart_weekday_unit_fri
    DayOfWeek.SATURDAY -> R.string.chart_weekday_unit_sat
    DayOfWeek.SUNDAY -> R.string.chart_weekday_unit_sun
}

/** `pon`, `wt`, ... po polsku. */
private fun DayOfWeek.shortLabel(): String =
    getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.forLanguageTag("pl-PL"))

@Composable
private fun StatsSkeleton() {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = TtSpace.screenMargin),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SkeletonBlock(height = 20.dp, width = 160.dp)
        SkeletonBlock(height = 14.dp, width = 240.dp)
        SkeletonBlock(height = 40.dp, width = 130.dp)
        SkeletonBlock(height = 150.dp)
        Spacer(Modifier.height(TtSpace.x1))
        SkeletonBlock(height = 20.dp, width = 180.dp)
        SkeletonBlock(height = 110.dp)
    }
}
