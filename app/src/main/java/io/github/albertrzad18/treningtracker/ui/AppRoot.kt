package io.github.albertrzad18.treningtracker.ui

import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import io.github.albertrzad18.treningtracker.ui.common.PrimaryButton
import io.github.albertrzad18.treningtracker.ui.common.ttClickable
import io.github.albertrzad18.treningtracker.domain.SummaryGranularity
import androidx.compose.foundation.layout.statusBarsPadding
import io.github.albertrzad18.treningtracker.ui.common.TtDivider
import io.github.albertrzad18.treningtracker.ui.common.TtIcons
import io.github.albertrzad18.treningtracker.ui.theme.LocalTtColors
import io.github.albertrzad18.treningtracker.ui.theme.TtMotion
import io.github.albertrzad18.treningtracker.ui.theme.TtShape
import io.github.albertrzad18.treningtracker.ui.theme.TtSpace
import io.github.albertrzad18.treningtracker.ui.theme.TtType
import io.github.albertrzad18.treningtracker.ui.theme.ttDuration
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    private val gatePrefs = app.getSharedPreferences("gate", android.content.Context.MODE_PRIVATE)
    private fun dailyAsked() = gatePrefs.getBoolean("daily_asked", false)

    /** Wywoływane po odpowiedzi użytkownika na prośbę o uprawnienia. */
    fun onPermissionResult() {
        gatePrefs.edit().putBoolean("daily_asked", true).apply()
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val gateway = treningApp.gateway
            if (gateway.sdkStatus() != HealthConnectClient.SDK_AVAILABLE) {
                _state.value = GateState.Unavailable
                return@launch
            }
            val granted = gateway.grantedPermissions()
            val exercise = HealthPermission.getReadPermission(ExerciseSessionRecord::class) in granted
            // Kroki i kalorie podstawowe doszły później: pytamy o nie jeden raz, a odmowa nie blokuje apki.
            val askForDaily = exercise && !granted.containsAll(gateway.dailyPermissions) && !dailyAsked()
            if (exercise && !askForDaily) {
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
    ) { gate.onPermissionResult() }

    val context = LocalContext.current
    Surface(modifier = Modifier.fillMaxSize(), color = LocalTtColors.current.bg) {
        when (val s = state) {
            GateState.Loading -> WelcomeScreen(
                currentStep = CHAIN_APP,
                text = stringResource(R.string.onboarding_loading),
                loading = true,
            )
            GateState.Unavailable -> WelcomeScreen(
                currentStep = CHAIN_HEALTH_CONNECT,
                text = stringResource(R.string.gate_unavailable),
                buttonText = stringResource(R.string.gate_install),
                onButton = { openHealthConnectInStore(context) },
            )
            is GateState.NeedsPermissions -> WelcomeScreen(
                currentStep = CHAIN_APP,
                text = stringResource(R.string.gate_needs_permissions),
                buttonText = stringResource(R.string.gate_grant),
                onButton = { permissionLauncher.launch(s.toRequest) },
            )
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

/** Który krok łańcucha danych jest bieżący na ekranie powitalnym. */
private const val CHAIN_HEALTH_CONNECT = 3
private const val CHAIN_APP = 4

/**
 * Ekran powitalny (brak uprawnień, brak Health Connect, ładowanie): to pierwsze, co widzi nowy
 * użytkownik, więc ma wyglądać jak powitanie, a nie jak błąd. Łańcuch kroków pokazuje,
 * skąd biorą się dane i na czym stoimy.
 */
@Composable
internal fun WelcomeScreen(
    currentStep: Int,
    text: String,
    buttonText: String? = null,
    onButton: (() -> Unit)? = null,
    loading: Boolean = false,
) {
    val colors = LocalTtColors.current
    val steps = listOf(
        stringResource(R.string.onboarding_step_watch),
        stringResource(R.string.onboarding_step_huawei_health),
        stringResource(R.string.onboarding_step_health_sync),
        stringResource(R.string.onboarding_step_health_connect),
        stringResource(R.string.onboarding_step_app),
    )
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = TtSpace.screenMargin)
            .padding(top = TtSpace.x5, bottom = TtSpace.x6),
    ) {
        Text(
            stringResource(R.string.onboarding_wordmark),
            style = TtType.label.copy(letterSpacing = 0.5.sp),
            color = colors.ink2,
        )
        Spacer(Modifier.height(40.dp))
        Text(
            stringResource(R.string.onboarding_headline),
            style = TtType.headline.copy(fontSize = 32.sp, lineHeight = 37.sp),
            color = colors.ink,
        )
        Spacer(Modifier.height(28.dp))
        Column(Modifier.fillMaxWidth()) {
            steps.forEachIndexed { index, step ->
                val done = index < currentStep
                val now = index == currentStep
                Row(
                    Modifier.fillMaxWidth().defaultMinSize(minHeight = 44.dp).height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(TtSpace.x4),
                ) {
                    Column(Modifier.width(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(24.dp)
                                .background(if (now) colors.ink else Color.Transparent, TtShape.full)
                                .border(1.5.dp, if (now) colors.ink else colors.line, TtShape.full),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (done) {
                                Icon(
                                    TtIcons.Check,
                                    contentDescription = null,
                                    tint = colors.ink2,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                        if (index < steps.lastIndex) {
                            Box(Modifier.width(1.5.dp).fillMaxHeight().background(colors.line))
                        }
                    }
                    Text(
                        step,
                        style = TtType.body.copy(fontWeight = if (now) FontWeight.SemiBold else FontWeight.Normal),
                        color = if (now) colors.ink else colors.ink2,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Column(verticalArrangement = Arrangement.spacedBy(TtSpace.x5)) {
            Text(text, style = TtType.body, color = colors.ink2)
            if (buttonText != null && onButton != null) {
                PrimaryButton(text = buttonText, onClick = onButton, modifier = Modifier.fillMaxWidth())
            }
            if (loading) {
                // Pasek postępu 2 dp zamiast kręciołka: ekran ma wyglądać jak powitanie.
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp).clip(TtShape.full),
                    color = colors.ink,
                    trackColor = colors.track,
                )
            }
        }
    }
}

private object Routes {
    const val DASHBOARD = "dashboard"
    const val WORKOUTS = "workouts"
    const val STATS = "stats"
    const val MONTH = "month"
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
    TopLevel(Routes.DASHBOARD, R.string.nav_dashboard, TtIcons.Home),
    TopLevel(Routes.MONTH, R.string.nav_month, TtIcons.Month),
    TopLevel(Routes.WORKOUTS, R.string.nav_workouts, TtIcons.Workouts),
    TopLevel(Routes.STATS, R.string.nav_stats, TtIcons.Stats),
    TopLevel(Routes.MORE, R.string.nav_more, TtIcons.Settings),
)

/**
 * Dolna nawigacja: cztery zakładki na płycie `surface`, nad nią linia 1 dp.
 * Wybrana pozycja ma wskaźnik w akcencie z kryciem 18% i tekst w kolorze `ink`.
 */
@Composable
private fun TtNavBar(selectedRoute: (String) -> Boolean, onSelect: (String) -> Unit) {
    val colors = LocalTtColors.current
    Column(Modifier.background(colors.surface).navigationBarsPadding()) {
        TtDivider()
        NavigationBar(
            containerColor = colors.surface,
            tonalElevation = 0.dp,
            windowInsets = WindowInsets(0),
            modifier = Modifier.height(72.dp),
        ) {
            topLevel.forEach { item ->
                NavigationBarItem(
                    selected = selectedRoute(item.route),
                    onClick = { onSelect(item.route) },
                    icon = { Icon(item.icon, contentDescription = null) },
                    label = {
                        Text(
                            stringResource(item.labelRes),
                            style = TtType.labelPlain.copy(fontSize = 11.sp, letterSpacing = 0.sp),
                            maxLines = 1,
                            softWrap = false,
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = colors.ink,
                        selectedTextColor = colors.ink,
                        unselectedIconColor = colors.ink2,
                        unselectedTextColor = colors.ink2,
                        indicatorColor = colors.accent.copy(alpha = 0.18f).compositeOver(colors.surface),
                    ),
                )
            }
        }
    }
}

@Composable
private fun MainScaffold() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val showBar = topLevel.any { item -> destination?.hierarchy?.any { it.route == item.route } == true }

    Scaffold(
        containerColor = LocalTtColors.current.bg,
        bottomBar = {
            if (showBar) {
                TtNavBar(
                    selectedRoute = { route -> destination?.hierarchy?.any { it.route == route } == true },
                    onSelect = { route ->
                        nav.navigate(route) {
                            popUpTo(Routes.DASHBOARD) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { padding ->
        // Przejście między ekranami: samo przygaszenie, bez przesuwania (250 ms).
        val fade = tween<Float>(ttDuration(TtMotion.BASE), easing = FastOutSlowInEasing)
        NavHost(
            nav,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(padding),
            enterTransition = { fadeIn(fade) },
            exitTransition = { fadeOut(fade) },
            popEnterTransition = { fadeIn(fade) },
            popExitTransition = { fadeOut(fade) },
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    period = SummaryGranularity.DAY,
                    onOpenWorkout = { nav.navigate(Routes.detail(it)) },
                    onOpenSummaries = { nav.navigate(Routes.SUMMARIES) },
                    onOpenGoals = { nav.navigate(Routes.GOALS) },
                )
            }
            composable(Routes.MONTH) {
                DashboardScreen(
                    period = SummaryGranularity.MONTH,
                    onOpenWorkout = { nav.navigate(Routes.detail(it)) },
                    onOpenSummaries = { nav.navigate(Routes.SUMMARIES) },
                    onOpenGoals = { nav.navigate(Routes.GOALS) },
                )
            }
            composable(Routes.WORKOUTS) { WorkoutsScreen(onOpenWorkout = { nav.navigate(Routes.detail(it)) }) }
            composable(Routes.STATS) { StatsScreen(onOpenWorkout = { nav.navigate(Routes.detail(it)) }) }
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
            composable(Routes.DEBUG) { DebugScreen(onBack = { nav.popBackStack() }) }
        }
    }
}
