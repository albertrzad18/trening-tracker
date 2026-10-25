package io.github.albertrzad18.treningtracker.ui.preview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.albertrzad18.treningtracker.R
import io.github.albertrzad18.treningtracker.ui.WelcomeScreen
import io.github.albertrzad18.treningtracker.ui.dashboard.DashboardContent
import io.github.albertrzad18.treningtracker.ui.detail.DetailContent
import io.github.albertrzad18.treningtracker.ui.goals.GoalsContent
import io.github.albertrzad18.treningtracker.ui.records.RecordsContent
import io.github.albertrzad18.treningtracker.ui.stats.StatsContent
import io.github.albertrzad18.treningtracker.ui.summaries.SummariesContent
import io.github.albertrzad18.treningtracker.ui.theme.LocalTtColors
import io.github.albertrzad18.treningtracker.ui.theme.TtTheme
import io.github.albertrzad18.treningtracker.ui.workouts.WorkoutsContent
import androidx.compose.ui.res.stringResource
import io.github.albertrzad18.treningtracker.domain.SummaryGranularity

/**
 * Podglądy ekranów w jasnym i ciemnym motywie, ze stanami: dane, ładowanie, pusto.
 * Każdy nowy ekran dodajemy tutaj — to najtańszy sposób, żeby sprawdzić oba motywy.
 */
private const val WIDTH = 360
private const val HEIGHT = 780

@Composable
private fun PreviewFrame(dark: Boolean, content: @Composable () -> Unit) {
    TtTheme(dark = dark) {
        Surface(Modifier.fillMaxSize(), color = LocalTtColors.current.bg) { content() }
    }
}

// --- Pulpit ---

@Preview(name = "Pulpit · dane · jasny", widthDp = WIDTH, heightDp = HEIGHT)
@Composable
private fun DashboardLight() = PreviewFrame(dark = false) {
    DashboardContent(Sample.dashboard, SummaryGranularity.DAY, {}, {}, {})
}

@Preview(name = "Pulpit · dane · ciemny", widthDp = WIDTH, heightDp = HEIGHT)
@Composable
private fun DashboardDark() = PreviewFrame(dark = true) {
    DashboardContent(Sample.dashboard, SummaryGranularity.DAY, {}, {}, {})
}

@Preview(name = "Pulpit · ładowanie", widthDp = WIDTH, heightDp = HEIGHT)
@Composable
private fun DashboardLoading() = PreviewFrame(dark = false) {
    DashboardContent(null, SummaryGranularity.DAY, {}, {}, {})
}

@Preview(name = "Pulpit · pusto · ciemny", widthDp = WIDTH, heightDp = HEIGHT)
@Composable
private fun DashboardEmptyPreview() = PreviewFrame(dark = true) {
    DashboardContent(Sample.dashboard.copy(isEmpty = true), SummaryGranularity.DAY, {}, {}, {})
}

// --- Treningi ---

@Preview(name = "Treningi · dane · jasny", widthDp = WIDTH, heightDp = HEIGHT)
@Composable
private fun WorkoutsLight() = PreviewFrame(dark = false) {
    WorkoutsContent(Sample.workoutFilters, emptySet(), Sample.workoutList, {}, {}, {}, {}, {})
}

@Preview(name = "Treningi · dane · ciemny", widthDp = WIDTH, heightDp = HEIGHT)
@Composable
private fun WorkoutsDark() = PreviewFrame(dark = true) {
    WorkoutsContent(Sample.workoutFilters, emptySet(), Sample.workoutList, {}, {}, {}, {}, {})
}

@Preview(name = "Treningi · ładowanie", widthDp = WIDTH, heightDp = HEIGHT)
@Composable
private fun WorkoutsLoading() = PreviewFrame(dark = false) {
    WorkoutsContent(Sample.workoutFilters, emptySet(), null, {}, {}, {}, {}, {})
}

@Preview(name = "Treningi · pusto", widthDp = WIDTH, heightDp = HEIGHT)
@Composable
private fun WorkoutsEmpty() = PreviewFrame(dark = false) {
    WorkoutsContent(
        Sample.workoutFilters,
        emptySet(),
        Sample.workoutList.copy(workouts = emptyList(), groups = emptyList()),
        {}, {}, {}, {}, {},
    )
}

// --- Statystyki ---

@Preview(name = "Statystyki · dane · jasny", widthDp = WIDTH, heightDp = HEIGHT)
@Composable
private fun StatsLight() = PreviewFrame(dark = false) {
    StatsContent(Sample.statsFilters, Sample.stats, {}, {}, {}, {}, {})
}

@Preview(name = "Statystyki · dane · ciemny", widthDp = WIDTH, heightDp = HEIGHT)
@Composable
private fun StatsDark() = PreviewFrame(dark = true) {
    StatsContent(Sample.statsFilters, Sample.stats, {}, {}, {}, {}, {})
}

@Preview(name = "Statystyki · ładowanie", widthDp = WIDTH, heightDp = HEIGHT)
@Composable
private fun StatsLoading() = PreviewFrame(dark = false) {
    StatsContent(Sample.statsFilters, null, {}, {}, {}, {}, {})
}

@Preview(name = "Statystyki · pusto", widthDp = WIDTH, heightDp = HEIGHT)
@Composable
private fun StatsEmpty() = PreviewFrame(dark = false) {
    StatsContent(Sample.statsFilters, Sample.emptyStats, {}, {}, {}, {}, {})
}

// --- Szczegóły treningu ---

@Preview(name = "Szczegóły · jasny", widthDp = WIDTH, heightDp = 1200)
@Composable
private fun DetailLight() = PreviewFrame(dark = false) { DetailContent(Sample.detail) }

@Preview(name = "Szczegóły · ciemny", widthDp = WIDTH, heightDp = 1200)
@Composable
private fun DetailDark() = PreviewFrame(dark = true) { DetailContent(Sample.detail) }

// --- Podsumowania, Rekordy, Cele ---

@Preview(name = "Podsumowania · jasny", widthDp = WIDTH, heightDp = HEIGHT)
@Composable
private fun SummariesLight() = PreviewFrame(dark = false) {
    SummariesContent(SummaryGranularity.WEEK, Sample.summaries, {}, {}, {})
}

@Preview(name = "Podsumowania · ciemny", widthDp = WIDTH, heightDp = HEIGHT)
@Composable
private fun SummariesDark() = PreviewFrame(dark = true) {
    SummariesContent(SummaryGranularity.WEEK, Sample.summaries, {}, {}, {})
}

@Preview(name = "Podsumowania · ładowanie", widthDp = WIDTH, heightDp = HEIGHT)
@Composable
private fun SummariesLoading() = PreviewFrame(dark = false) {
    SummariesContent(SummaryGranularity.WEEK, null, {}, {}, {})
}

@Preview(name = "Rekordy · jasny", widthDp = WIDTH, heightDp = HEIGHT)
@Composable
private fun RecordsLight() = PreviewFrame(dark = false) { RecordsContent(Sample.records, {}, {}) }

@Preview(name = "Rekordy · ciemny", widthDp = WIDTH, heightDp = HEIGHT)
@Composable
private fun RecordsDark() = PreviewFrame(dark = true) { RecordsContent(Sample.records, {}, {}) }

@Preview(name = "Rekordy · pusto", widthDp = WIDTH, heightDp = HEIGHT)
@Composable
private fun RecordsEmpty() = PreviewFrame(dark = false) {
    RecordsContent(Sample.records.copy(isEmpty = true), {}, {})
}

@Preview(name = "Cele · jasny", widthDp = WIDTH, heightDp = HEIGHT)
@Composable
private fun GoalsLight() = PreviewFrame(dark = false) {
    GoalsContent(Sample.goalRows, {}, { _, _, _, _ -> }, {})
}

@Preview(name = "Cele · ciemny", widthDp = WIDTH, heightDp = HEIGHT)
@Composable
private fun GoalsDark() = PreviewFrame(dark = true) {
    GoalsContent(Sample.goalRows, {}, { _, _, _, _ -> }, {})
}

@Preview(name = "Cele · pusto", widthDp = WIDTH, heightDp = HEIGHT)
@Composable
private fun GoalsEmpty() = PreviewFrame(dark = false) {
    GoalsContent(emptyList(), {}, { _, _, _, _ -> }, {})
}

// --- Ekrany powitalne ---

@Preview(name = "Powitanie · brak uprawnień", widthDp = WIDTH, heightDp = HEIGHT)
@Composable
private fun WelcomePermissions() = PreviewFrame(dark = false) {
    WelcomeScreen(
        currentStep = 4,
        text = stringResource(R.string.gate_needs_permissions),
        buttonText = stringResource(R.string.gate_grant),
        onButton = {},
    )
}

@Preview(name = "Powitanie · brak Health Connect · ciemny", widthDp = WIDTH, heightDp = HEIGHT)
@Composable
private fun WelcomeNoHealthConnect() = PreviewFrame(dark = true) {
    WelcomeScreen(
        currentStep = 3,
        text = stringResource(R.string.gate_unavailable),
        buttonText = stringResource(R.string.gate_install),
        onButton = {},
    )
}

@Preview(name = "Powitanie · ładowanie", widthDp = WIDTH, heightDp = HEIGHT)
@Composable
private fun WelcomeLoading() = PreviewFrame(dark = false) {
    WelcomeScreen(currentStep = 4, text = stringResource(R.string.onboarding_loading), loading = true)
}
