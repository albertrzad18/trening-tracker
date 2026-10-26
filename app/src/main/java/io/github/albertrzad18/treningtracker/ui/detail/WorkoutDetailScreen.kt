package io.github.albertrzad18.treningtracker.ui.detail

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import io.github.albertrzad18.treningtracker.ui.charts.LineSpec
import io.github.albertrzad18.treningtracker.ui.charts.ReferenceLine
import io.github.albertrzad18.treningtracker.ui.charts.RouteMap
import io.github.albertrzad18.treningtracker.ui.charts.TtLineChart
import io.github.albertrzad18.treningtracker.ui.charts.ZoneBars
import io.github.albertrzad18.treningtracker.ui.common.BigValue
import io.github.albertrzad18.treningtracker.ui.common.DeltaBadge
import io.github.albertrzad18.treningtracker.ui.common.DisciplineDot
import io.github.albertrzad18.treningtracker.ui.common.LabelText
import io.github.albertrzad18.treningtracker.ui.common.MetricGrid
import io.github.albertrzad18.treningtracker.ui.common.MetricItem
import io.github.albertrzad18.treningtracker.ui.common.Panel
import io.github.albertrzad18.treningtracker.ui.common.SkeletonBlock
import io.github.albertrzad18.treningtracker.ui.common.TtTopBar
import io.github.albertrzad18.treningtracker.ui.common.deltaAverageText
import io.github.albertrzad18.treningtracker.ui.common.labelRes
import io.github.albertrzad18.treningtracker.ui.common.localDateTime
import io.github.albertrzad18.treningtracker.ui.format.ValueUnit
import io.github.albertrzad18.treningtracker.ui.format.caloriesParts
import io.github.albertrzad18.treningtracker.ui.format.distanceParts
import io.github.albertrzad18.treningtracker.ui.format.elevationParts
import io.github.albertrzad18.treningtracker.ui.format.formatDateTime
import io.github.albertrzad18.treningtracker.ui.format.formatDuration
import io.github.albertrzad18.treningtracker.ui.format.formatKmShort
import io.github.albertrzad18.treningtracker.ui.format.heartRateParts
import io.github.albertrzad18.treningtracker.ui.format.paceParts
import io.github.albertrzad18.treningtracker.ui.format.speedParts
import io.github.albertrzad18.treningtracker.ui.format.workoutDistanceParts
import io.github.albertrzad18.treningtracker.ui.theme.LocalTtColors
import io.github.albertrzad18.treningtracker.ui.theme.TtShape
import io.github.albertrzad18.treningtracker.ui.theme.TtSpace
import io.github.albertrzad18.treningtracker.ui.theme.TtType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Duration
import java.time.ZoneId
import kotlin.math.roundToInt

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

    /** Dane z bazy bez stref tętna: strefy zależą jeszcze od ustawień, więc dochodzą niżej. */
    private val fromDatabase = combine(
        dao.observeById(id),
        dao.observeAll(),
        sampleDao.observeSamples(id, SampleType.HEART_RATE),
        sampleDao.observeSamples(id, SampleType.SPEED),
        sampleDao.observeRoute(id),
    ) { entity, all, hr, speed, route ->
        if (entity == null) {
            null
        } else {
            val workout = entity.toDomain()
            DetailState.Found(
                workout = workout,
                comparison = compareToAverage(workout, all.map { it.toDomain() }),
                heartRate = hr.map { it.toDomain() },
                speed = speed.map { it.toDomain() },
                zones = emptyList(),
                timeInZones = emptyList(),
                route = route.toTrack(),
            )
        }
    }

    // Zmiana tętna maksymalnego w ustawieniach od razu przelicza strefy.
    val state: StateFlow<DetailState> = combine(fromDatabase, treningApp.settings.state) { found, settings ->
        if (found == null) {
            DetailState.NotFound
        } else {
            val zones = heartRateZones(settings.maxHeartRate)
            found.copy(zones = zones, timeInZones = timeInZones(found.heartRate, zones))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailState.Loading)
}

@Composable
fun WorkoutDetailScreen(onBack: () -> Unit, vm: WorkoutDetailViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val colors = LocalTtColors.current
    Column(Modifier.fillMaxSize()) {
        TtTopBar(title = "", onBack = onBack)
        when (val s = state) {
            DetailState.Loading -> DetailSkeleton()
            DetailState.NotFound -> Text(
                stringResource(R.string.detail_not_found),
                style = TtType.body,
                color = colors.ink2,
                modifier = Modifier.padding(horizontal = TtSpace.screenMargin).padding(top = 56.dp),
            )
            is DetailState.Found -> DetailContent(s)
        }
    }
}

@Composable
internal fun DetailContent(state: DetailState.Found) {
    val colors = LocalTtColors.current
    val workout = state.workout
    val zone = ZoneId.systemDefault()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = TtSpace.x6),
        verticalArrangement = Arrangement.spacedBy(TtSpace.panelGap),
    ) {
        Column(
            Modifier.padding(horizontal = TtSpace.screenMargin).padding(bottom = TtSpace.x1),
            verticalArrangement = Arrangement.spacedBy(TtSpace.x1),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DisciplineDot(workout.discipline, size = 10.dp)
                Text(
                    workout.title ?: stringResource(workout.discipline.labelRes()),
                    style = TtType.headline,
                    color = colors.ink,
                )
            }
            Text(formatDateTime(workout.localDateTime(zone)), style = TtType.bodyS, color = colors.ink2)
        }

        Box(Modifier.padding(horizontal = TtSpace.screenMargin)) { HeroPanel(workout, state.comparison) }

        val metrics = metricsOf(workout)
        if (metrics.isNotEmpty()) {
            Box(Modifier.padding(horizontal = TtSpace.screenMargin)) {
                Panel { MetricGrid(metrics) }
            }
        }

        if (state.heartRate.size >= 2) {
            Box(Modifier.padding(horizontal = TtSpace.screenMargin)) { HeartRatePanel(state.heartRate, workout) }
        }
        if (state.timeInZones.any { !it.isZero }) {
            Box(Modifier.padding(horizontal = TtSpace.screenMargin)) {
                Panel {
                    Text(stringResource(R.string.detail_zones), style = TtType.title, color = colors.ink)
                    Spacer(Modifier.height(TtSpace.x2))
                    Text(stringResource(R.string.detail_zones_desc), style = TtType.bodyS, color = colors.ink2)
                    Spacer(Modifier.height(TtSpace.x3))
                    ZoneBars(state.zones, state.timeInZones)
                }
            }
        }
        if (state.speed.size >= 2 && workout.discipline != Discipline.POOL) {
            Box(Modifier.padding(horizontal = TtSpace.screenMargin)) { SpeedPanel(state.speed, workout) }
        }
        if (!state.route.isEmpty) {
            Box(Modifier.padding(horizontal = TtSpace.screenMargin)) {
                Panel {
                    Text(stringResource(R.string.detail_route), style = TtType.title, color = colors.ink)
                    Spacer(Modifier.height(TtSpace.x3))
                    RouteMap(state.route, workout.discipline)
                }
            }
            val profile = state.route.elevationProfile
            if (profile.size >= 2) {
                Box(Modifier.padding(horizontal = TtSpace.screenMargin)) {
                    ElevationPanel(profile, workout.elevationGainM)
                }
            }
        }
    }
}

/** Dwie najważniejsze liczby: czas i dystans, każda z porównaniem do średniej z tej dyscypliny. */
@Composable
private fun HeroPanel(workout: Workout, comparison: AverageComparison?) {
    Panel {
        Row(horizontalArrangement = Arrangement.spacedBy(TtSpace.x5)) {
            HeroValue(
                label = stringResource(R.string.metric_time),
                parts = ValueUnit(formatDuration(workout.duration), null),
                change = comparison?.durationChangePercent,
                modifier = Modifier.weight(1f),
            )
            workout.distanceM?.let { meters ->
                HeroValue(
                    label = stringResource(R.string.metric_distance),
                    parts = workoutDistanceParts(workout.discipline, meters),
                    change = comparison?.distanceChangePercent,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun HeroValue(
    label: String,
    parts: ValueUnit,
    change: Double?,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(TtSpace.x2)) {
        LabelText(label)
        BigValue(parts, style = TtType.hero)
        if (change != null) DeltaBadge(change, text = deltaAverageText(change))
    }
}

/** Tylko metryki, które mają wartość; reszta jest ukryta. Czas i dystans są wyżej, więc tu ich nie ma. */
@Composable
private fun metricsOf(w: Workout): List<MetricItem> = listOfNotNull(
    w.caloriesKcal?.let { MetricItem(stringResource(R.string.metric_calories), caloriesParts(it).value, "kcal") },
    w.avgHeartRate?.let { MetricItem(stringResource(R.string.metric_avg_hr), heartRateParts(it).value, "bpm") },
    w.maxHeartRate?.let { MetricItem(stringResource(R.string.metric_max_hr), heartRateParts(it).value, "bpm") },
    if (w.discipline == Discipline.POOL) {
        w.pacePer100mSec?.let {
            val parts = paceParts(it)
            MetricItem(stringResource(R.string.metric_pace), parts.value, parts.unit)
        }
    } else {
        w.avgSpeedKmh?.let { MetricItem(stringResource(R.string.metric_avg_speed), speedParts(it).value, "km/h") }
    },
    if (w.discipline == Discipline.POOL) {
        null
    } else {
        w.maxSpeedMps?.let {
            MetricItem(stringResource(R.string.metric_max_speed), speedParts(it * 3.6).value, "km/h")
        }
    },
    w.elevationGainM?.let { MetricItem(stringResource(R.string.metric_elevation), elevationParts(it).value, "m") },
)

/** Minuty od startu treningu: wspólna oś X dla wykresów przebiegu. */
private fun minutesFromStart(points: List<SamplePoint>): List<Pair<Double, Double>> {
    val start = points.first().timeMillis
    return points.map { (it.timeMillis - start) / 60_000.0 to it.value }
}

/** Tętno w czasie: linia w akcencie z wypełnieniem 12% i kropką na końcu. */
@Composable
private fun HeartRatePanel(samples: List<SamplePoint>, workout: Workout) {
    val colors = LocalTtColors.current
    val points = remember(samples) { minutesFromStart(samples) }
    var selected by remember { mutableStateOf<Int?>(null) }
    val selectedPoint = selected?.let { points.getOrNull(it) }

    ChartPanel(
        title = stringResource(R.string.detail_hr_chart),
        value = selectedPoint?.let { ValueUnit(it.second.roundToInt().toString(), minuteUnit(it.first)) }
            ?: workout.avgHeartRate?.let { ValueUnit(it.toString(), stringResource(R.string.detail_hr_avg_unit)) },
    ) {
        TtLineChart(
            lines = listOf(
                LineSpec(points, colors.accent, width = 2.5.dp, areaAlpha = 0.12f, endDot = true),
            ),
            xLabels = minuteLabels(points),
            selectedIndex = selected,
            onSelect = { selected = it },
        )
    }
}

/** Prędkość w czasie: linia w kolorze dyscypliny i pozioma linia średniej. */
@Composable
private fun SpeedPanel(samples: List<SamplePoint>, workout: Workout) {
    val colors = LocalTtColors.current
    val points = remember(samples) { minutesFromStart(samples).map { (x, value) -> x to value * 3.6 } }
    var selected by remember { mutableStateOf<Int?>(null) }
    val selectedPoint = selected?.let { points.getOrNull(it) }
    val average = workout.avgSpeedKmh

    ChartPanel(
        title = stringResource(R.string.detail_speed_chart),
        value = selectedPoint?.let { ValueUnit(speedParts(it.second).value, minuteUnit(it.first)) }
            ?: average?.let { ValueUnit(speedParts(it).value, "km/h") },
    ) {
        TtLineChart(
            lines = listOf(LineSpec(points, colors.of(workout.discipline), width = 2.5.dp)),
            reference = average?.let { ReferenceLine(it, speedParts(it).let { p -> "${p.value} ${p.unit}" }) },
            xLabels = minuteLabels(points),
            selectedIndex = selected,
            onSelect = { selected = it },
        )
    }
}

/** Profil wysokości: linia z wypełnieniem 12%, oś X w kilometrach. */
@Composable
private fun ElevationPanel(profile: List<Pair<Double, Double>>, gain: Double?) {
    val colors = LocalTtColors.current
    ChartPanel(
        title = stringResource(R.string.detail_elevation_chart),
        value = gain?.let { ValueUnit(elevationParts(it).value, "m") },
    ) {
        TtLineChart(
            lines = listOf(LineSpec(profile, colors.ink2, width = 2.dp, areaAlpha = 0.12f)),
            xLabels = profile.takeIf { it.size >= 2 }?.let { list ->
                val last = list.last().first
                listOf(0.0, last / 2, last).map(::formatKmShort)
            }.orEmpty(),
        )
    }
}

/** Panel wykresu: tytuł po lewej, kluczowa liczba po prawej, pod nimi wykres. */
@Composable
private fun ChartPanel(title: String, value: ValueUnit?, content: @Composable () -> Unit) {
    val colors = LocalTtColors.current
    Panel {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(title, style = TtType.title, color = colors.ink, modifier = Modifier.weight(1f))
            if (value != null) BigValue(value, style = TtType.value)
        }
        Spacer(Modifier.height(10.dp))
        content()
    }
}

/** `· 12 min` — podpis wybranego punktu obok wartości. */
private fun minuteUnit(minutes: Double): String = "· ${minutes.roundToInt()} min"

/** Trzy podpisy osi X: start, połowa i koniec treningu. */
private fun minuteLabels(points: List<Pair<Double, Double>>): List<String> {
    if (points.size < 2) return emptyList()
    val last = points.last().first
    return listOf(0.0, last / 2, last).map { "${it.roundToInt()} min" }
}

@Composable
private fun DetailSkeleton() {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = TtSpace.screenMargin),
        verticalArrangement = Arrangement.spacedBy(TtSpace.x4),
    ) {
        SkeletonBlock(height = 24.dp, width = 160.dp)
        SkeletonBlock(height = 14.dp, width = 120.dp)
        SkeletonBlock(height = 120.dp, shape = TtShape.panel)
        SkeletonBlock(height = 180.dp, shape = TtShape.panel)
        SkeletonBlock(height = 180.dp, shape = TtShape.panel)
    }
}
