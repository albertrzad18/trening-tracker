package io.github.albertrzad18.treningtracker.ui.records

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
import io.github.albertrzad18.treningtracker.domain.Granularity
import io.github.albertrzad18.treningtracker.domain.PeriodRecord
import io.github.albertrzad18.treningtracker.domain.PersonalRecord
import io.github.albertrzad18.treningtracker.domain.RecordKind
import io.github.albertrzad18.treningtracker.domain.bestPeriod
import io.github.albertrzad18.treningtracker.domain.recordsFor
import io.github.albertrzad18.treningtracker.ui.common.DisciplineDot
import io.github.albertrzad18.treningtracker.ui.common.labelRes
import io.github.albertrzad18.treningtracker.ui.format.formatDate
import io.github.albertrzad18.treningtracker.ui.format.formatDistanceKm
import io.github.albertrzad18.treningtracker.ui.format.formatDuration
import io.github.albertrzad18.treningtracker.ui.format.formatHoursAndMinutes
import io.github.albertrzad18.treningtracker.ui.format.formatCalories
import io.github.albertrzad18.treningtracker.ui.format.formatPacePer100m
import io.github.albertrzad18.treningtracker.ui.format.formatSpeedKmh
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Duration
import java.time.ZoneId

/** Rekordy jednej dyscypliny plus najlepszy tydzień i miesiąc dla całości. */
data class RecordsData(
    val perDiscipline: Map<Discipline, List<PersonalRecord>>,
    val bestWeekDistance: PeriodRecord?,
    val bestWeekDuration: PeriodRecord?,
    val bestMonthDistance: PeriodRecord?,
    val bestMonthDuration: PeriodRecord?,
    val isEmpty: Boolean,
)

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
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(stringResource(R.string.records_title), style = MaterialTheme.typography.titleLarge)
        }
        val d = data
        when {
            d == null -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            d.isEmpty -> Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
                Text(stringResource(R.string.records_empty), textAlign = TextAlign.Center)
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                d.perDiscipline.forEach { (discipline, records) ->
                    if (records.isNotEmpty()) {
                        item(key = discipline.name) { DisciplineCard(discipline, records, onOpenWorkout) }
                    }
                }
                item { PeriodCard(d) }
            }
        }
    }
}

@Composable
private fun DisciplineCard(discipline: Discipline, records: List<PersonalRecord>, onOpen: (String) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DisciplineDot(discipline, size = 12.dp)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(discipline.labelRes()), style = MaterialTheme.typography.titleMedium)
            }
            records.forEach { record ->
                Row(
                    Modifier.fillMaxWidth().clickable { onOpen(record.workoutId) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(record.kind.labelRes()),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text(record.valueText(), style = MaterialTheme.typography.titleSmall)
                        Text(
                            formatDate(record.date),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodCard(d: RecordsData) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.records_periods), style = MaterialTheme.typography.titleMedium)
            PeriodRow(R.string.records_best_week_distance, d.bestWeekDistance, distance = true)
            PeriodRow(R.string.records_best_week_time, d.bestWeekDuration, distance = false)
            HorizontalDivider()
            PeriodRow(R.string.records_best_month_distance, d.bestMonthDistance, distance = true)
            PeriodRow(R.string.records_best_month_time, d.bestMonthDuration, distance = false)
        }
    }
}

@Composable
private fun PeriodRow(labelRes: Int, record: PeriodRecord?, distance: Boolean) {
    if (record == null) return
    val value = if (distance) record.distanceM?.let(::formatDistanceKm) else formatHoursAndMinutes(Duration.ofSeconds(record.durationSec))
    if (value == null) return
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(labelRes), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(value, style = MaterialTheme.typography.titleSmall)
            Text(
                formatDate(record.range.start),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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

private fun PersonalRecord.valueText(): String = when (kind) {
    RecordKind.LONGEST_DISTANCE -> formatDistanceKm(value)
    RecordKind.LONGEST_DURATION -> formatDuration(Duration.ofSeconds(value.toLong()))
    RecordKind.BEST_AVG_SPEED -> formatSpeedKmh(value)
    RecordKind.BEST_PACE -> formatPacePer100m(value)
    RecordKind.MOST_CALORIES -> formatCalories(value)
    RecordKind.BIGGEST_ELEVATION -> "${value.toInt()} m"
}
