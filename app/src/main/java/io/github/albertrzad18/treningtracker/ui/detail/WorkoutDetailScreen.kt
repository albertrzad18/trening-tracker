package io.github.albertrzad18.treningtracker.ui.detail

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.albertrzad18.treningtracker.R
import io.github.albertrzad18.treningtracker.TreningApp
import io.github.albertrzad18.treningtracker.data.db.toDomain
import io.github.albertrzad18.treningtracker.domain.AverageComparison
import io.github.albertrzad18.treningtracker.domain.Discipline
import io.github.albertrzad18.treningtracker.domain.Workout
import io.github.albertrzad18.treningtracker.domain.compareToAverage
import io.github.albertrzad18.treningtracker.ui.common.DisciplineDot
import io.github.albertrzad18.treningtracker.ui.common.avgHeartRateText
import io.github.albertrzad18.treningtracker.ui.common.caloriesText
import io.github.albertrzad18.treningtracker.ui.common.distanceText
import io.github.albertrzad18.treningtracker.ui.common.durationText
import io.github.albertrzad18.treningtracker.ui.common.labelRes
import io.github.albertrzad18.treningtracker.ui.common.localDateTime
import io.github.albertrzad18.treningtracker.ui.common.paceOrSpeedText
import io.github.albertrzad18.treningtracker.ui.format.formatDateTime
import io.github.albertrzad18.treningtracker.ui.format.formatDistanceKm
import io.github.albertrzad18.treningtracker.ui.format.formatHeartRate
import io.github.albertrzad18.treningtracker.ui.format.formatPercentChange
import io.github.albertrzad18.treningtracker.ui.format.formatSpeedKmh
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.ZoneId

sealed interface DetailState {
    data object Loading : DetailState
    data object NotFound : DetailState
    data class Found(val workout: Workout, val comparison: AverageComparison?) : DetailState
}

class WorkoutDetailViewModel(app: Application, handle: SavedStateHandle) : AndroidViewModel(app) {
    private val id: String = checkNotNull(handle["id"])
    private val dao = (app as TreningApp).database.workoutDao()

    val state: StateFlow<DetailState> = combine(dao.observeById(id), dao.observeAll()) { entity, all ->
        if (entity == null) {
            DetailState.NotFound
        } else {
            val workout = entity.toDomain()
            DetailState.Found(workout, compareToAverage(workout, all.map { it.toDomain() }))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailState.Loading)
}

@Composable
fun WorkoutDetailScreen(onBack: () -> Unit, vm: WorkoutDetailViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
        }
        when (val s = state) {
            DetailState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            DetailState.NotFound -> Box(Modifier.fillMaxSize().padding(24.dp), Alignment.Center) {
                Text(stringResource(R.string.detail_not_found))
            }
            is DetailState.Found -> DetailContent(s.workout, s.comparison)
        }
    }
}

private data class Metric(val label: Int, val value: String)

/** Tylko metryki, które mają wartość; reszta jest ukryta. */
private fun metricsOf(w: Workout): List<Metric> = listOfNotNull(
    Metric(R.string.metric_time, w.durationText()),
    w.distanceText()?.let { Metric(R.string.metric_distance, it) },
    w.caloriesText()?.let { Metric(R.string.metric_calories, it) },
    w.avgHeartRateText()?.let { Metric(R.string.metric_avg_hr, it) },
    w.maxHeartRate?.let { Metric(R.string.metric_max_hr, formatHeartRate(it)) },
    w.paceOrSpeedText()?.let { Metric(if (w.discipline == Discipline.POOL) R.string.metric_pace else R.string.metric_avg_speed, it) },
    if (w.discipline == Discipline.POOL) null else w.maxSpeedMps?.let { Metric(R.string.metric_max_speed, formatSpeedKmh(it * 3.6)) },
    w.elevationGainM?.let { Metric(R.string.metric_elevation, "%.0f m".format(it)) },
)

@Composable
private fun DetailContent(w: Workout, comparison: AverageComparison?) {
    val zone = ZoneId.systemDefault()
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DisciplineDot(w.discipline, size = 18.dp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(w.title ?: stringResource(w.discipline.labelRes()), style = MaterialTheme.typography.headlineSmall)
                Text(
                    formatDateTime(w.localDateTime(zone)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        metricsOf(w).chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { MetricCard(it, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        comparison?.let { ComparisonCard(it) }
    }
}

@Composable
private fun MetricCard(m: Metric, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text(stringResource(m.label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(m.value, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun ComparisonCard(c: AverageComparison) {
    val lines = listOfNotNull(
        c.durationChangePercent?.let { stringResource(R.string.detail_compare_duration, formatPercentChange(it)) },
        c.distanceChangePercent?.let { stringResource(R.string.detail_compare_distance, formatPercentChange(it)) },
    )
    if (lines.isEmpty()) return
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.detail_compare_title), style = MaterialTheme.typography.titleMedium)
            lines.forEach { Text(it, style = MaterialTheme.typography.bodyLarge) }
            Text(
                stringResource(R.string.detail_compare_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
