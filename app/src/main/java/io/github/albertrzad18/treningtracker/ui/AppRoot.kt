package io.github.albertrzad18.treningtracker.ui

import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.albertrzad18.treningtracker.R
import io.github.albertrzad18.treningtracker.TreningApp
import io.github.albertrzad18.treningtracker.data.sync.SyncScheduler
import io.github.albertrzad18.treningtracker.ui.dashboard.DashboardScreen
import io.github.albertrzad18.treningtracker.ui.debug.DebugScreen
import io.github.albertrzad18.treningtracker.ui.detail.WorkoutDetailScreen
import io.github.albertrzad18.treningtracker.ui.goals.GoalsScreen
import io.github.albertrzad18.treningtracker.ui.more.MoreScreen
import io.github.albertrzad18.treningtracker.ui.records.RecordsScreen
import io.github.albertrzad18.treningtracker.ui.settings.SettingsScreen
import io.github.albertrzad18.treningtracker.ui.stats.StatsScreen
import io.github.albertrzad18.treningtracker.ui.summaries.SummariesScreen
import io.github.albertrzad18.treningtracker.ui.workouts.WorkoutsScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface GateState {
    data object Loading : GateState
    data object Unavailable : GateState
    data class NeedsPermissions(val toRequest: Set<String>) : GateState
    data object Ready : GateState
}

/** Sprawdza, czy Health Connect jest dostępny i czy mamy uprawnienie do odczytu treningów. */
class GateViewModel(app: Application) : AndroidViewModel(app) {
    private val treningApp = app as TreningApp
    private val _state = MutableStateFlow<GateState>(GateState.Loading)
    val state: StateFlow<GateState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val gateway = treningApp.gateway
            if (gateway.sdkStatus() != HealthConnectClient.SDK_AVAILABLE) {
                _state.value = GateState.Unavailable
                return@launch
            }
            val granted = gateway.grantedPermissions()
            if (HealthPermission.getReadPermission(ExerciseSessionRecord::class) in granted) {
                _state.value = GateState.Ready
                // Synchronizacja przy każdym otwarciu apki; w tle dba o nią cykliczne zadanie.
                SyncScheduler.syncNow(treningApp)
            } else {
                _state.value = GateState.NeedsPermissions(gateway.wantedPermissions())
            }
        }
    }
}

@Composable
fun AppRoot(gate: GateViewModel = viewModel()) {
    val state by gate.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { gate.refresh() }
    val permissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { gate.refresh() }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (val s = state) {
            GateState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            GateState.Unavailable -> {
                val context = LocalContext.current
                Message(stringResource(R.string.gate_unavailable)) {
                    Button(onClick = { openHealthConnectInStore(context) }) {
                        Text(stringResource(R.string.gate_install))
                    }
                }
            }
            is GateState.NeedsPermissions -> Message(stringResource(R.string.gate_needs_permissions)) {
                Button(onClick = { permissionLauncher.launch(s.toRequest) }) { Text(stringResource(R.string.gate_grant)) }
            }
            GateState.Ready -> MainScaffold()
        }
    }
}

/** Otwiera Health Connect w Sklepie Play razem z ekranem powitalnym. */
private fun openHealthConnectInStore(context: android.content.Context) {
    val uri = "market://details?id=com.google.android.apps.healthdata&url=healthconnect%3A%2F%2Fonboarding"
    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(uri)).apply {
        setPackage("com.android.vending")
        putExtra("overlay", true)
        putExtra("callerId", context.packageName)
    }
    // Gdy nie ma Sklepu Play (np. emulator bez usług Google), po prostu nic nie robimy.
    runCatching { context.startActivity(intent) }
}

@Composable
private fun Message(text: String, action: @Composable () -> Unit = {}) {
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
        action()
    }
}

private object Routes {
    const val DASHBOARD = "dashboard"
    const val WORKOUTS = "workouts"
    const val STATS = "stats"
    const val MORE = "more"
    const val DEBUG = "debug"
    const val SUMMARIES = "summaries"
    const val RECORDS = "records"
    const val GOALS = "goals"
    const val SETTINGS = "settings"
    const val DETAIL = "workout/{id}"
    fun detail(id: String) = "workout/${java.net.URLEncoder.encode(id, "UTF-8")}"
}

private data class TopLevel(val route: String, val labelRes: Int, val icon: ImageVector)

private val topLevel = listOf(
    TopLevel(Routes.DASHBOARD, R.string.nav_dashboard, Icons.Filled.Home),
    TopLevel(Routes.WORKOUTS, R.string.nav_workouts, Icons.AutoMirrored.Filled.List),
    TopLevel(Routes.STATS, R.string.nav_stats, Icons.Filled.DateRange),
    TopLevel(Routes.MORE, R.string.nav_more, Icons.Filled.MoreVert),
)

@Composable
private fun MainScaffold() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val showBar = topLevel.any { item -> destination?.hierarchy?.any { it.route == item.route } == true }

    Scaffold(
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    topLevel.forEach { item ->
                        NavigationBarItem(
                            selected = destination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                nav.navigate(item.route) {
                                    popUpTo(Routes.DASHBOARD) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Routes.DASHBOARD, modifier = Modifier.padding(padding)) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    onOpenWorkout = { nav.navigate(Routes.detail(it)) },
                    onOpenSummaries = { nav.navigate(Routes.SUMMARIES) },
                    onOpenGoals = { nav.navigate(Routes.GOALS) },
                )
            }
            composable(Routes.WORKOUTS) { WorkoutsScreen(onOpenWorkout = { nav.navigate(Routes.detail(it)) }) }
            composable(Routes.STATS) { StatsScreen(onOpenSummaries = { nav.navigate(Routes.SUMMARIES) }) }
            composable(Routes.MORE) {
                MoreScreen(
                    onOpenDiagnostics = { nav.navigate(Routes.DEBUG) },
                    onOpenSummaries = { nav.navigate(Routes.SUMMARIES) },
                    onOpenRecords = { nav.navigate(Routes.RECORDS) },
                    onOpenGoals = { nav.navigate(Routes.GOALS) },
                    onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                )
            }
            composable(Routes.RECORDS) {
                RecordsScreen(
                    onBack = { nav.popBackStack() },
                    onOpenWorkout = { nav.navigate(Routes.detail(it)) },
                )
            }
            composable(Routes.GOALS) { GoalsScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.SETTINGS) { SettingsScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.SUMMARIES) {
                SummariesScreen(
                    onBack = { nav.popBackStack() },
                    onOpenWorkout = { nav.navigate(Routes.detail(it)) },
                )
            }
            composable(Routes.DETAIL) { WorkoutDetailScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.DEBUG) { DebugScreen() }
        }
    }
}
