package io.github.albertrzad18.treningtracker.ui.debug

import android.app.Application
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.albertrzad18.treningtracker.TreningApp
import io.github.albertrzad18.treningtracker.data.demo.DEMO_ORIGIN
import io.github.albertrzad18.treningtracker.data.demo.demoDailyStats
import io.github.albertrzad18.treningtracker.data.demo.demoSeries
import io.github.albertrzad18.treningtracker.data.demo.demoTodayWorkout
import io.github.albertrzad18.treningtracker.data.demo.demoWorkouts
import io.github.albertrzad18.treningtracker.data.healthconnect.DataKind
import io.github.albertrzad18.treningtracker.data.healthconnect.HealthConnectGateway
import io.github.albertrzad18.treningtracker.data.healthconnect.RouteState
import io.github.albertrzad18.treningtracker.data.healthconnect.SessionProbe
import io.github.albertrzad18.treningtracker.domain.Discipline
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

sealed interface DebugState {
    data object Loading : DebugState
    data object Unavailable : DebugState
    data class NeedsPermissions(val toRequest: Set<String>) : DebugState
    data class Report(
        val text: String,
        val rows: List<DebugRow>,
        val from: LocalDate?,
        val to: LocalDate?,
    ) : DebugState
    data class Error(val message: String) : DebugState
}

/** Jeden wiersz tabeli diagnostyki: nazwa typu danych i liczba rekordów. */
data class DebugRow(val label: String, val count: Int)

sealed interface DemoMessage {
    data class Added(val count: Int) : DemoMessage
    data object Removed : DemoMessage
}

class DebugViewModel(app: Application) : AndroidViewModel(app) {
    private val gateway = HealthConnectGateway(app)
    private val dao = (app as TreningApp).database.workoutDao()
    private val sampleDao = (app as TreningApp).database.sampleDao()
    private val dailyDao = (app as TreningApp).database.dailyStatsDao()
    private val _state = MutableStateFlow<DebugState>(DebugState.Loading)
    val state: StateFlow<DebugState> = _state.asStateFlow()

    /** Komunikat po operacji na danych demo; `null`, gdy nie ma nic do pokazania. */
    private val _demoMessage = MutableStateFlow<DemoMessage?>(null)
    val demoMessage: StateFlow<DemoMessage?> = _demoMessage.asStateFlow()

    init {
        refresh()
    }

    /** Wrzuca do bazy sztuczne treningi z przebiegami i trasami (tylko build debug). */
    fun fillDemoData() {
        viewModelScope.launch {
            val demo = demoWorkouts(LocalDate.now(), ZoneId.systemDefault())
            dao.upsertAll(demo)
            dailyDao.upsertAll(demoDailyStats(LocalDate.now()))
            // Przebiegi tylko dla ostatnich treningów (plus kilka rowerowych, żeby była trasa na mapie).
            val newest = demo.sortedByDescending { it.startMillis }
            val withSeries = (newest.take(40) + newest.filter { it.discipline == Discipline.ROAD }.take(10)).distinct()
            withSeries.forEach { workout ->
                val series = demoSeries(workout)
                sampleDao.replaceSamples(workout.id, series.samples, series.route)
            }
            _demoMessage.value = DemoMessage.Added(demo.size)
        }
    }

    private var todayCount = 0

    /** Dodaje kolejny demo trening z dzisiaj (basen, potem trenażer), żeby zobaczyć kartę dnia. */
    fun addTodayWorkout() {
        viewModelScope.launch {
            val workout = demoTodayWorkout(todayCount, LocalDate.now(), ZoneId.systemDefault(), System.currentTimeMillis())
            if (workout != null) {
                dao.upsertAll(listOf(workout))
                todayCount++
            }
        }
    }

    fun clearDemoData() {
        viewModelScope.launch {
            dao.deleteByOrigin(DEMO_ORIGIN)
            todayCount = 0
            dailyDao.deleteByOrigin(DEMO_ORIGIN)
            _demoMessage.value = DemoMessage.Removed
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = DebugState.Loading
            _state.value = try {
                load()
            } catch (e: Exception) {
                DebugState.Error(e.message ?: e::class.java.simpleName)
            }
        }
    }

    private suspend fun load(): DebugState {
        if (gateway.sdkStatus() != HealthConnectClient.SDK_AVAILABLE) return DebugState.Unavailable

        val granted = gateway.grantedPermissions()
        val wanted = gateway.readPermissions + if (gateway.isHistoryAvailable()) setOf(gateway.historyPermission) else emptySet()
        if (!granted.containsAll(gateway.readPermissions - gateway.dailyPermissions)) return DebugState.NeedsPermissions(wanted)

        // Bez uprawnienia do historii Health Connect pozwala czytać tylko 30 dni wstecz.
        val days = if (gateway.historyPermission in granted) 365 else 30
        val now = Instant.now()
        val sessions = gateway.readSessions(now.minus(days.toLong(), ChronoUnit.DAYS), now)
        val probes = sessions.map { gateway.probe(it, granted) }
        val zone = ZoneId.systemDefault()
        return DebugState.Report(
            text = buildReport(probes, days, getApplication<Application>().resources),
            rows = reportRows(probes),
            from = probes.minByOrNull { it.start }?.start?.atZone(zone)?.toLocalDate(),
            to = probes.maxByOrNull { it.start }?.let { it.start.plusSeconds(it.durationSeconds) }
                ?.atZone(zone)?.toLocalDate(),
        )
    }

    /**
     * Tabela diagnostyki: nazwa typu danych z Health Connect i liczba rekordów.
     * Typy bez ani jednego rekordu też pokazujemy — to właśnie one mówią, czego brakuje.
     */
    private fun reportRows(probes: List<SessionProbe>): List<DebugRow> = buildList {
        add(DebugRow("ExerciseSessionRecord", probes.size))
        DataKind.entries.forEach { kind ->
            add(DebugRow(kind.recordClass.simpleName ?: kind.name, probes.sumOf { it.recordCounts[kind] ?: 0 }))
        }
        add(DebugRow("ExerciseSegment", probes.sumOf { it.segments }))
        add(DebugRow("ExerciseLap", probes.sumOf { it.laps }))
        add(DebugRow("ExerciseRoute", probes.count { it.route == RouteState.DATA }))
    }
}
