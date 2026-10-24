package io.github.albertrzad18.treningtracker.ui.detail

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.albertrzad18.treningtracker.R
import io.github.albertrzad18.treningtracker.TreningApp
import io.github.albertrzad18.treningtracker.data.db.SampleType
import io.github.albertrzad18.treningtracker.data.db.toDomain
import io.github.albertrzad18.treningtracker.data.db.toTrack
import io.github.albertrzad18.treningtracker.domain.AverageComparison
import io.github.albertrzad18.treningtracker.domain.Discipline
import io.github.albertrzad18.treningtracker.domain.HeartRateZone
import io.github.albertrzad18.treningtracker.domain.RouteTrack
import io.github.albertrzad18.treningtracker.domain.SamplePoint
import io.github.albertrzad18.treningtracker.domain.Workout
import io.github.albertrzad18.treningtracker.domain.compareToAverage
import io.github.albertrzad18.treningtracker.domain.heartRateZones
import io.github.albertrzad18.treningtracker.domain.timeInZones
import io.github.albertrzad18.treningtracker.ui.charts.ChartCard
import io.github.albertrzad18.treningtracker.ui.charts.ChartLine
import io.github.albertrzad18.treningtracker.ui.charts.MultiLineChart
import io.github.albertrzad18.treningtracker.ui.charts.RouteMap
import io.github.albertrzad18.treningtracker.ui.common.DisciplineDot
import io.github.albertrzad18.treningtracker.ui.common.color
import io.github.albertrzad18.treningtracker.ui.common.avgHeartRateText
import io.github.albertrzad18.treningtracker.ui.common.caloriesText
import io.github.albertrzad18.treningtracker.ui.common.distanceText
import io.github.albertrzad18.treningtracker.ui.common.durationText
import io.github.albertrzad18.treningtracker.ui.common.labelRes
import io.github.albertrzad18.treningtracker.ui.common.localDateTime
import io.github.albertrzad18.treningtracker.ui.common.paceOrSpeedText
import io.github.albertrzad18.treningtracker.ui.format.formatDateTime
import io.github.albertrzad18.treningtracker.ui.format.formatDecimalShort
import io.github.albertrzad18.treningtracker.ui.format.formatDistanceKm
import io.github.albertrzad18.treningtracker.ui.format.formatHeartRate
import io.github.albertrzad18.treningtracker.ui.format.formatHoursAndMinutes
import io.github.albertrzad18.treningtracker.ui.format.formatKmShort
import io.github.albertrzad18.treningtracker.ui.format.formatNumberShort
import io.github.albertrzad18.treningtracker.ui.format.formatPercentChange
import io.github.albertrzad18.treningtracker.ui.format.formatSpeedKmh
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Duration
import java.time.ZoneId

sealed interface DetailState {
    data object Loading : DetailState
    data object NotFound : DetailState
    data class Found(
        val workout: Workout,
        val comparison: AverageComparison?,
        val heartRate: List<SamplePoint>,
        val speed: List<SamplePoint>,
        val zones: List<HeartRateZone>,
        val timeInZones: List<Duration>,
        val route: RouteTrack,
    ) : DetailState
}

class WorkoutDetailViewModel(app: Application, handle: SavedStateHandle) : AndroidViewModel(app) {
    private val id: String = checkNotNull(handle["id"])
    private val treningApp = app as TreningApp
    private val dao = treningApp.database.workoutDao()
    private val sampleDao = treningApp.database.sampleDao()

    val state: StateFlow<DetailState> = combine(
        dao.observeById(id),
        dao.observeAll(),
        sampleDao.observeSamples(id, SampleType.HEART_RATE),
        sampleDao.observeSamples(id, SampleType.SPEED),
        sampleDao.observeRoute(id),
    ) { entity, all, hr, speed, route ->
        if (entity == null) {
            DetailState.NotFound
        } else {
            val workout = entity.toDomain()
            val zones = heartRateZones(treningApp.settings.state.value.maxHeartRate)
            val heartRate = hr.map { it.toDomain() }
            DetailState.Found(
                workout = workout,
                comparison = compareToAverage(workout, all.map { it.toDomain() }),
                heartRate = heartRate,
                speed = speed.map { it.toDomain() },
                zones = zones,
                timeInZones = timeInZones(heartRate, zones),
                route = route.toTrack(),
            )
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
            is DetailState.Found -> DetailContent(s)
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
private fun DetailContent(state: DetailState.Found) {
    val w = state.workout
    val comparison = state.comparison
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
        if (state.heartRate.size >= 2) HeartRateCard(state.heartRate)
        if (state.timeInZones.any { !it.isZero }) ZonesCard(state.zones, state.timeInZones)
        if (state.speed.size >= 2) SpeedCard(state.speed, w.discipline)
        if (!state.route.isEmpty) RouteCard(state.route, w.discipline)
        comparison?.let { ComparisonCard(it) }
    }
}

/** Minuty od startu treningu: wspólna oś X dla wykresów przebiegu. */
private fun minutesFromStart(points: List<SamplePoint>): List<Pair<Double, Double>> {
    val start = points.first().timeMillis
    return points.map { (it.timeMillis - start) / 60_000.0 to it.value }
}

@Composable
private fun HeartRateCard(samples: List<SamplePoint>) {
    ChartCard(title = stringResource(R.string.detail_hr_chart)) {
        MultiLineChart(
            lines = listOf(ChartLine(MaterialTheme.colorScheme.error, minutesFromStart(samples))),
            xLabel = { x -> "${x.toInt()} min" },
            yLabel = ::formatNumberShort,
            height = 160.dp,
        )
    }
}

@Composable
private fun SpeedCard(samples: List<SamplePoint>, discipline: Discipline) {
    val inKmh = minutesFromStart(samples).map { (x, v) -> x to v * 3.6 }
    ChartCard(title = stringResource(R.string.detail_speed_chart)) {
        MultiLineChart(
            lines = listOf(ChartLine(discipline.color(), inKmh)),
            xLabel = { x -> "${x.toInt()} min" },
            yLabel = ::formatDecimalShort,
            height = 160.dp,
        )
    }
}

/** Czas w strefach tętna: pięć słupków z opisem zakresu bpm. */
@Composable
private fun ZonesCard(zones: List<HeartRateZone>, times: List<Duration>) {
    ChartCard(
        title = stringResource(R.string.detail_zones),
        description = stringResource(R.string.detail_zones_desc),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val longest = times.maxOf { it.seconds }.coerceAtLeast(1)
            zones.forEachIndexed { i, zone ->
                val seconds = times[i].seconds
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.detail_zone_label, zone.index, zone.fromBpm),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.width(96.dp),
                    )
                    Box(
                        Modifier
                            .weight(seconds.toFloat() / longest + 0.001f)
                            .height(16.dp)
                            .background(zoneColor(zone.index), RoundedCornerShape(4.dp)),
                    )
                    Spacer(Modifier.weight((1f - seconds.toFloat() / longest).coerceAtLeast(0.001f)))
                    Text(
                        formatHoursAndMinutes(times[i]),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.width(80.dp),
                    )
                }
            }
        }
    }
}

/** Strefy od spokojnej do najcięższej: zielona, żółta, pomarańczowa, czerwona, bordowa. */
private fun zoneColor(index: Int) = when (index) {
    1 -> Color(0xFF66BB6A)
    2 -> Color(0xFFD4E157)
    3 -> Color(0xFFFFB300)
    4 -> Color(0xFFF4511E)
    else -> Color(0xFFB71C1C)
}

@Composable
private fun RouteCard(track: RouteTrack, discipline: Discipline) {
    ChartCard(title = stringResource(R.string.detail_route)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RouteMap(track, discipline)
            val profile = track.elevationProfile
            if (profile.size >= 2) {
                Text(stringResource(R.string.detail_elevation_chart), style = MaterialTheme.typography.labelLarge)
                MultiLineChart(
                    lines = listOf(ChartLine(MaterialTheme.colorScheme.tertiary, profile)),
                    xLabel = { x -> formatKmShort(x) },
                    yLabel = { v -> "${v.toInt()} m" },
                    height = 140.dp,
                )
            }
        }
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
