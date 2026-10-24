package io.github.albertrzad18.treningtracker.ui.debug

import android.app.Application
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.albertrzad18.treningtracker.TreningApp
import io.github.albertrzad18.treningtracker.data.demo.DEMO_ORIGIN
import io.github.albertrzad18.treningtracker.data.demo.demoSeries
import io.github.albertrzad18.treningtracker.data.demo.demoWorkouts
import io.github.albertrzad18.treningtracker.data.healthconnect.HealthConnectGateway
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
    data class Report(val text: String) : DebugState
    data class Error(val message: String) : DebugState
}

sealed interface DemoMessage {
    data class Added(val count: Int) : DemoMessage
    data object Removed : DemoMessage
}

class DebugViewModel(app: Application) : AndroidViewModel(app) {
    private val gateway = HealthConnectGateway(app)
    private val dao = (app as TreningApp).database.workoutDao()
    private val sampleDao = (app as TreningApp).database.sampleDao()
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
            // Przebiegi tylko dla ostatnich treningów: reszta i tak służy wykresom zbiorczym.
            demo.sortedByDescending { it.startMillis }.take(40).forEach { workout ->
                val series = demoSeries(workout)
                sampleDao.replaceSamples(workout.id, series.samples, series.route)
            }
            _demoMessage.value = DemoMessage.Added(demo.size)
        }
    }

    fun clearDemoData() {
        viewModelScope.launch {
            dao.deleteByOrigin(DEMO_ORIGIN)
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
        if (!granted.containsAll(gateway.readPermissions)) return DebugState.NeedsPermissions(wanted)

        // Bez uprawnienia do historii Health Connect pozwala czytać tylko 30 dni wstecz.
        val days = if (gateway.historyPermission in granted) 365 else 30
        val now = Instant.now()
        val sessions = gateway.readSessions(now.minus(days.toLong(), ChronoUnit.DAYS), now)
        val probes = sessions.map { gateway.probe(it, granted) }
        return DebugState.Report(buildReport(probes, days, getApplication<Application>().resources))
    }
}
