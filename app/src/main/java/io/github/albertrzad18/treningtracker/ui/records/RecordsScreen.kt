package io.github.albertrzad18.treningtracker.ui.records

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
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
import io.github.albertrzad18.treningtracker.domain.Discipline
import io.github.albertrzad18.treningtracker.domain.Granularity
import io.github.albertrzad18.treningtracker.domain.PeriodRecord
import io.github.albertrzad18.treningtracker.domain.PersonalRecord
import io.github.albertrzad18.treningtracker.domain.RecordKind
import io.github.albertrzad18.treningtracker.domain.bestPeriod
import io.github.albertrzad18.treningtracker.domain.recordsFor
import io.github.albertrzad18.treningtracker.ui.common.BigValue
import io.github.albertrzad18.treningtracker.ui.common.DisciplineDot
import io.github.albertrzad18.treningtracker.ui.common.Panel
import io.github.albertrzad18.treningtracker.ui.common.SkeletonBlock
import io.github.albertrzad18.treningtracker.ui.common.TtDivider
import io.github.albertrzad18.treningtracker.ui.common.TtIcons
import io.github.albertrzad18.treningtracker.ui.common.TtTopBar
import io.github.albertrzad18.treningtracker.ui.common.labelRes
import io.github.albertrzad18.treningtracker.ui.common.ttClickable
import io.github.albertrzad18.treningtracker.ui.format.ValueUnit
import io.github.albertrzad18.treningtracker.ui.format.caloriesParts
import io.github.albertrzad18.treningtracker.ui.format.distanceParts
import io.github.albertrzad18.treningtracker.ui.format.elevationParts
import io.github.albertrzad18.treningtracker.ui.format.formatDate
import io.github.albertrzad18.treningtracker.ui.format.formatDuration
import io.github.albertrzad18.treningtracker.ui.format.formatHoursAndMinutes
import io.github.albertrzad18.treningtracker.ui.format.paceParts
import io.github.albertrzad18.treningtracker.ui.format.speedParts
import io.github.albertrzad18.treningtracker.ui.theme.LocalTtColors
import io.github.albertrzad18.treningtracker.ui.theme.TtShape
import io.github.albertrzad18.treningtracker.ui.theme.TtSpace
import io.github.albertrzad18.treningtracker.ui.theme.TtType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

/** Rekordy jednej dyscypliny plus najlepszy tydzień i miesiąc dla całości. */
data class RecordsData(
    val perDiscipline: Map<Discipline, List<PersonalRecord>>,
    val bestWeekDistance: PeriodRecord?,
    val bestWeekDuration: PeriodRecord?,
    val bestMonthDistance: PeriodRecord?,
    val bestMonthDuration: PeriodRecord?,
    val isEmpty: Boolean,
) {
    /** Ile rekordów w sumie: podtytuł wiersza w „Więcej”. */
    val count: Int get() = perDiscipline.values.sumOf { it.size }
}

class RecordsViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = (app as TreningApp).database.workoutDao()

    /** `null` = jeszcze się wczytuje. */
    val data: StateFlow<RecordsData?> = dao.observeAll().map { entities ->
        val zone = ZoneId.systemDefault()
        val all = entities.map { it.toDomain() }
        RecordsData(
            perDiscipline = Discipline.entries.associateWith { all.recordsFor(it, zone) },
            bestWeekDistance = all.bestPeriod(Granularity.WEEK, zone, byDistance = true),
            bestWeekDuration = all.bestPeriod(Granularity.WEEK, zone, byDistance = false),
            bestMonthDistance = all.bestPeriod(Granularity.MONTH, zone, byDistance = true),
            bestMonthDuration = all.bestPeriod(Granularity.MONTH, zone, byDistance = false),
            isEmpty = all.isEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun RecordsScreen(onBack: () -> Unit, onOpenWorkout: (String) -> Unit, vm: RecordsViewModel = viewModel()) {
    val data by vm.data.collectAsStateWithLifecycle()
    RecordsContent(data, onBack, onOpenWorkout)
}

/** Sam widok rekordów: bez ViewModelu, więc da się go pokazać w `@Preview`. */
@Composable
internal fun RecordsContent(data: RecordsData?, onBack: () -> Unit, onOpenWorkout: (String) -> Unit) {
    val colors = LocalTtColors.current
    Column(Modifier.fillMaxSize()) {
        TtTopBar(stringResource(R.string.records_title), onBack)
        val d = data
        when {
            d == null -> Column(
                Modifier.padding(horizontal = TtSpace.screenMargin),
                verticalArrangement = Arrangement.spacedBy(TtSpace.panelGap),
            ) {
                repeat(3) { SkeletonBlock(height = 180.dp, shape = TtShape.panel) }
            }
            d.isEmpty -> Text(
                stringResource(R.string.records_empty),
                style = TtType.body,
                color = colors.ink2,
                modifier = Modifier.padding(horizontal = TtSpace.screenMargin).padding(top = 64.dp),
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(
                    start = TtSpace.screenMargin,
                    end = TtSpace.screenMargin,
                    bottom = TtSpace.x6,
                ),
                verticalArrangement = Arrangement.spacedBy(TtSpace.panelGap),
            ) {
                d.perDiscipline.forEach { (discipline, records) ->
                    if (records.isNotEmpty()) {
                        item(key = discipline.name) { DisciplinePanel(discipline, records, onOpenWorkout) }
                    }
                }
                item { PeriodPanel(d) }
            }
        }
    }
}

@Composable
internal fun DisciplinePanel(
    discipline: Discipline,
    records: List<PersonalRecord>,
    onOpen: (String) -> Unit,
) {
    val colors = LocalTtColors.current
    val fresh = LocalDate.now().minusDays(7)
    Panel {
        Row(
            Modifier.padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DisciplineDot(discipline, size = 10.dp)
            Text(stringResource(discipline.labelRes()), style = TtType.title, color = colors.ink)
        }
        records.forEach { record ->
            TtDivider()
            Row(
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 60.dp)
                    .ttClickable { onOpen(record.workoutId) }
                    .padding(vertical = TtSpace.x2),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TtSpace.x3),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(TtSpace.x1)) {
                    Text(stringResource(record.kind.labelRes()), style = TtType.bodyS, color = colors.ink2)
                    if (!record.date.isBefore(fresh)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                TtIcons.Trophy,
                                contentDescription = null,
                                tint = colors.of(discipline),
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                stringResource(R.string.records_new),
                                style = TtType.labelPlain,
                                color = colors.of(discipline),
                            )
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    BigValue(record.parts(), style = TtType.value)
                    Text(formatDate(record.date), style = TtType.caption, color = colors.ink2)
                }
            }
        }
    }
}

@Composable
internal fun PeriodPanel(d: RecordsData) {
    val colors = LocalTtColors.current
    Panel {
        Text(
            stringResource(R.string.records_periods),
            style = TtType.title,
            color = colors.ink,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        PeriodRow(R.string.records_best_week_distance, d.bestWeekDistance, distance = true)
        PeriodRow(R.string.records_best_week_time, d.bestWeekDuration, distance = false)
        PeriodRow(R.string.records_best_month_distance, d.bestMonthDistance, distance = true)
        PeriodRow(R.string.records_best_month_time, d.bestMonthDuration, distance = false)
    }
}

@Composable
private fun PeriodRow(labelRes: Int, record: PeriodRecord?, distance: Boolean) {
    if (record == null) return
    val colors = LocalTtColors.current
    val value = if (distance) {
        record.distanceM?.let { distanceParts(it) }
    } else {
        ValueUnit(formatHoursAndMinutes(Duration.ofSeconds(record.durationSec)), null)
    } ?: return
    TtDivider()
    Row(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = 60.dp).padding(vertical = TtSpace.x2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TtSpace.x3),
    ) {
        Text(
            stringResource(labelRes),
            style = TtType.bodyS,
            color = colors.ink2,
            modifier = Modifier.weight(1f),
        )
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BigValue(value, style = TtType.value)
            Text(formatDate(record.range.start), style = TtType.caption, color = colors.ink2)
        }
    }
}

private fun RecordKind.labelRes() = when (this) {
    RecordKind.LONGEST_DISTANCE -> R.string.record_distance
    RecordKind.LONGEST_DURATION -> R.string.record_duration
    RecordKind.BEST_AVG_SPEED -> R.string.record_speed
    RecordKind.BEST_PACE -> R.string.record_pace
    RecordKind.MOST_CALORIES -> R.string.record_calories
    RecordKind.BIGGEST_ELEVATION -> R.string.record_elevation
}

/** Wartość rekordu rozbita na liczbę i jednostkę. */
private fun PersonalRecord.parts(): ValueUnit = when (kind) {
    RecordKind.LONGEST_DISTANCE -> distanceParts(value)
    RecordKind.LONGEST_DURATION -> ValueUnit(formatDuration(Duration.ofSeconds(value.toLong())), null)
    RecordKind.BEST_AVG_SPEED -> speedParts(value)
    RecordKind.BEST_PACE -> paceParts(value)
    RecordKind.MOST_CALORIES -> caloriesParts(value)
    RecordKind.BIGGEST_ELEVATION -> elevationParts(value)
}
