package io.github.albertrzad18.treningtracker.ui.debug

import android.app.Application
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.albertrzad18.treningtracker.data.healthconnect.HealthConnectGateway
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

sealed interface DebugState {
    data object Loading : DebugState
    data object Unavailable : DebugState
    data class NeedsPermissions(val toRequest: Set<String>) : DebugState
    data class Report(val text: String) : DebugState
    data class Error(val message: String) : DebugState
}

class DebugViewModel(app: Application) : AndroidViewModel(app) {
    private val gateway = HealthConnectGateway(app)
    private val _state = MutableStateFlow<DebugState>(DebugState.Loading)
    val state: StateFlow<DebugState> = _state.asStateFlow()

    init {
        refresh()
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
