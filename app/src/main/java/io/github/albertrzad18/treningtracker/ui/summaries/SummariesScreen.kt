package io.github.albertrzad18.treningtracker.ui.summaries

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
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
import io.github.albertrzad18.treningtracker.domain.passiveTotals
import io.github.albertrzad18.treningtracker.domain.summaries
import io.github.albertrzad18.treningtracker.ui.common.DeltaBadge
import io.github.albertrzad18.treningtracker.ui.common.DisciplineDot
import io.github.albertrzad18.treningtracker.ui.common.LabelText
import io.github.albertrzad18.treningtracker.ui.common.Panel
import io.github.albertrzad18.treningtracker.ui.common.SegmentedToggle
import io.github.albertrzad18.treningtracker.ui.common.ShareBar
import io.github.albertrzad18.treningtracker.ui.common.SkeletonBlock
import io.github.albertrzad18.treningtracker.ui.common.TtDivider
import io.github.albertrzad18.treningtracker.ui.common.TtIcons
import io.github.albertrzad18.treningtracker.ui.common.TtTopBar
import io.github.albertrzad18.treningtracker.ui.common.daysText
import io.github.albertrzad18.treningtracker.ui.common.labelRes
import io.github.albertrzad18.treningtracker.ui.common.localDateTime
import io.github.albertrzad18.treningtracker.ui.common.summaryText
import io.github.albertrzad18.treningtracker.ui.common.ttClickable
import io.github.albertrzad18.treningtracker.ui.format.formatCalories
import io.github.albertrzad18.treningtracker.ui.format.formatDate
import io.github.albertrzad18.treningtracker.ui.format.formatDistanceKm
import io.github.albertrzad18.treningtracker.ui.format.formatDuration
import io.github.albertrzad18.treningtracker.ui.format.formatHoursAndMinutes
import io.github.albertrzad18.treningtracker.ui.format.formatMonth
import io.github.albertrzad18.treningtracker.ui.format.formatNumberShort
import io.github.albertrzad18.treningtracker.ui.format.formatPercentAbs
import io.github.albertrzad18.treningtracker.ui.format.formatRangeFull
import io.github.albertrzad18.treningtracker.ui.format.pluralPl
import io.github.albertrzad18.treningtracker.ui.theme.LocalTtColors
import io.github.albertrzad18.treningtracker.ui.theme.TtMotion
import io.github.albertrzad18.treningtracker.ui.theme.TtShape
import io.github.albertrzad18.treningtracker.ui.theme.TtSpace
import io.github.albertrzad18.treningtracker.ui.theme.TtType
import io.github.albertrzad18.treningtracker.ui.theme.ttDuration
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
    private val dailyDao = (app as TreningApp).database.dailyStatsDao()
    private val _granularity = MutableStateFlow(SummaryGranularity.WEEK)
    val granularity: StateFlow<SummaryGranularity> = _granularity

    /** `null` = jeszcze się wczytuje. */
    val data: StateFlow<List<PeriodSummary>?> = combine(dao.observeAll(), dailyDao.observeAll(), _granularity) { entities, daily, g ->
        val stats = daily.map { it.toDomain() }
        summaries(entities.map { it.toDomain() }, g, LocalDate.now(), ZoneId.systemDefault())
            .map { it.copy(passive = stats.passiveTotals(it.range).takeUnless { p -> p.isEmpty }) }
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
    SummariesContent(
        granularity = granularity,
        periods = list,
        onGranularity = vm::setGranularity,
        onBack = onBack,
        onOpenWorkout = onOpenWorkout,
    )
}

/** Sam widok podsumowań: bez ViewModelu, więc da się go pokazać w `@Preview`. */
@Composable
internal fun SummariesContent(
    granularity: SummaryGranularity,
    periods: List<PeriodSummary>?,
    onGranularity: (SummaryGranularity) -> Unit,
    onBack: () -> Unit,
    onOpenWorkout: (String) -> Unit,
) {
    // Rozwinięty jest jeden okres naraz; po zmianie zakładki znów najnowszy.
    var openIndex by remember(granularity) { mutableStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        TtTopBar(stringResource(R.string.summaries_title), onBack)
        SegmentedToggle(
            options = SummaryGranularity.entries - SummaryGranularity.DAY,
            selected = granularity,
            label = { stringResource(it.labelRes()) },
            onSelect = onGranularity,
            modifier = Modifier.padding(horizontal = TtSpace.screenMargin).padding(bottom = TtSpace.x4),
        )
        if (periods == null) {
            Column(
                Modifier.padding(horizontal = TtSpace.screenMargin),
                verticalArrangement = Arrangement.spacedBy(TtSpace.panelGap),
            ) {
                SkeletonBlock(height = 220.dp, shape = TtShape.panel)
                repeat(3) { SkeletonBlock(height = 72.dp, shape = TtShape.panel) }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = TtSpace.screenMargin,
                    end = TtSpace.screenMargin,
                    bottom = TtSpace.x6,
                ),
                verticalArrangement = Arrangement.spacedBy(TtSpace.panelGap),
            ) {
                itemsIndexed(periods, key = { _, item -> item.range.start.toString() }) { index, summary ->
                    SummaryPanel(
                        summary = summary,
                        open = index == openIndex,
                        onToggle = { openIndex = if (openIndex == index) -1 else index },
                        onOpenWorkout = onOpenWorkout,
                    )
                }
            }
        }
    }
}

private fun SummaryGranularity.labelRes() = when (this) {
    SummaryGranularity.DAY -> R.string.dashboard_this_day
    SummaryGranularity.WEEK -> R.string.summary_tab_week
    SummaryGranularity.MONTH -> R.string.summary_tab_month
    SummaryGranularity.YEAR -> R.string.summary_tab_year
}

/** Nagłówek okresu: `19 paź 2026 – 25 paź 2026`, `październik 2026` albo `2026`. */
private fun PeriodSummary.title(): String = when (granularity) {
    SummaryGranularity.DAY -> formatDate(range.start)
    SummaryGranularity.WEEK -> formatRangeFull(range.start, range.endExclusive.minusDays(1))
    SummaryGranularity.MONTH -> formatMonth(YearMonth.from(range.start))
    SummaryGranularity.YEAR -> range.start.year.toString()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SummaryPanel(
    summary: PeriodSummary,
    open: Boolean,
    onToggle: () -> Unit,
    onOpenWorkout: (String) -> Unit,
) {
    val colors = LocalTtColors.current
    val duration = ttDuration(TtMotion.BASE)
    Panel {
        Row(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = TtSpace.minTouch)
                .ttClickable(onClick = onToggle),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TtSpace.x3),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (open) {
                    LabelText(summary.title())
                } else {
                    Text(summary.title(), style = TtType.body, color = colors.ink)
                    Text(
                        formatHoursAndMinutes(summary.totals.duration),
                        style = TtType.body.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.ink,
                    )
                }
            }
            if (!open) {
                ShareBar(
                    parts = summary.perDiscipline
                        .filter { it.totals.duration.seconds > 0 }
                        .map { colors.of(it.discipline) to it.totals.duration.seconds.toFloat() },
                    modifier = Modifier.width(48.dp),
                    height = 6.dp,
                )
                summary.durationChangePercent?.let { change ->
                    Row(
                        Modifier.width(56.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (change < 0) TtIcons.Down else TtIcons.Up,
                            contentDescription = null,
                            tint = colors.ink,
                            modifier = Modifier.height(16.dp),
                        )
                        Text(formatPercentAbs(change), style = TtType.bodyS, color = colors.ink, maxLines = 1)
                    }
                }
                Icon(TtIcons.ExpandMore, contentDescription = null, tint = colors.ink2)
            }
        }
        if (summary.count == 0) {
            Text(stringResource(R.string.summary_empty), style = TtType.bodyS, color = colors.ink2)
            return@Panel
        }
        AnimatedVisibility(
            visible = open,
            enter = fadeIn(tween(duration, easing = FastOutSlowInEasing)),
            exit = fadeOut(tween(duration, easing = FastOutSlowInEasing)),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(top = TtSpace.x2),
                verticalArrangement = Arrangement.spacedBy(TtSpace.x4),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(TtSpace.x1)) {
                    Text(
                        formatHoursAndMinutes(summary.totals.duration),
                        style = TtType.hero,
                        color = colors.ink,
                    )
                    val extra = listOfNotNull(
                        summary.totals.distanceM?.let(::formatDistanceKm),
                        summary.totals.caloriesKcal?.let(::formatCalories),
                    )
                    if (extra.isNotEmpty()) {
                        Text(extra.joinToString(" · "), style = TtType.body, color = colors.ink2)
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(TtSpace.x2),
                    verticalArrangement = Arrangement.spacedBy(TtSpace.x2),
                ) {
                    summary.durationChangePercent?.let {
                        DeltaBadge(it, text = stringResource(R.string.metric_time))
                    }
                    summary.distanceChangePercent?.let {
                        DeltaBadge(it, text = stringResource(R.string.metric_distance))
                    }
                    summary.caloriesChangePercent?.let {
                        DeltaBadge(it, text = stringResource(R.string.metric_calories))
                    }
                }
                ShareBar(
                    parts = summary.perDiscipline
                        .filter { it.totals.duration.seconds > 0 }
                        .map { colors.of(it.discipline) to it.totals.duration.seconds.toFloat() },
                    height = 10.dp,
                    gap = 2.dp,
                )
                Column {
                    summary.perDiscipline.filter { it.totals.count > 0 }.forEach { DisciplineRow(it) }
                }
                Text(facts(summary), style = TtType.bodyS, color = colors.ink2)
                summary.passive?.let { passive ->
                    val parts = listOfNotNull(
                        passive.steps?.let { stringResource(R.string.summary_steps, formatNumberShort(it.toDouble())) },
                        passive.kcal?.let { stringResource(R.string.summary_basal, formatCalories(it)) },
                    )
                    if (parts.isNotEmpty()) Text(parts.joinToString(" · "), style = TtType.bodyS, color = colors.ink2)
                }
                summary.best?.let { best ->
                    TtDivider()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 52.dp)
                            .ttClickable { onOpenWorkout(best.id) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(TtSpace.x2),
                    ) {
                        Column(Modifier.weight(1f)) {
                            LabelText(stringResource(R.string.summary_best_label))
                            Text(
                                "${stringResource(best.discipline.labelRes())}, " +
                                    "${formatDate(best.localDateTime(ZoneId.systemDefault()).toLocalDate())} · " +
                                    formatDuration(best.duration),
                                style = TtType.body,
                                color = colors.ink,
                            )
                        }
                        Icon(TtIcons.ChevronRight, contentDescription = null, tint = colors.ink2)
                    }
                }
            }
        }
    }
}

/** `3 treningi · 3 dni z treningiem · seria 2 dni` */
@Composable
private fun facts(summary: PeriodSummary): String = listOf(
    pluralPl(
        summary.count,
        stringResource(R.string.unit_workout_one),
        stringResource(R.string.unit_workout_few),
        stringResource(R.string.unit_workout_many),
    ),
    stringResource(R.string.summary_active_days_short, summary.activeDays),
    stringResource(R.string.summary_streak_short, daysText(summary.longestStreakDays)),
).joinToString(" · ")

@Composable
private fun DisciplineRow(item: DisciplineTotals) {
    val colors = LocalTtColors.current
    TtDivider()
    Row(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TtSpace.x3),
    ) {
        DisciplineDot(item.discipline)
        Text(
            stringResource(item.discipline.labelRes()),
            style = TtType.body,
            color = colors.ink,
            modifier = Modifier.width(76.dp),
        )
        Text(
            item.totals.summaryText(),
            style = TtType.bodyS,
            color = colors.ink2,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}
