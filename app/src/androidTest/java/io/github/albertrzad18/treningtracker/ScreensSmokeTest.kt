package io.github.albertrzad18.treningtracker

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.albertrzad18.treningtracker.domain.SummaryGranularity
import io.github.albertrzad18.treningtracker.domain.buildDashboard
import io.github.albertrzad18.treningtracker.ui.dashboard.DashboardContent
import io.github.albertrzad18.treningtracker.ui.preview.Sample
import io.github.albertrzad18.treningtracker.ui.theme.TtTheme
import io.github.albertrzad18.treningtracker.ui.workouts.WorkoutsContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Testy ekranów na urządzeniu: renderują prawdziwe composable z danymi przykładowymi
 * (bez Health Connect i bez bazy) i sprawdzają teksty oraz kliknięcia.
 * Uruchomienie: `./gradlew connectedDebugAndroidTest` przy podłączonym telefonie lub emulatorze.
 */
@RunWith(AndroidJUnit4::class)
class ScreensSmokeTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun weekDashboardShowsWeekLabelsAndOpensWorkout() {
        var opened: String? = null
        rule.setContent {
            TtTheme(dark = false) {
                DashboardContent(Sample.dashboard, SummaryGranularity.WEEK, { opened = it }, {}, {})
            }
        }
        rule.onNodeWithText("Przepalone").assertIsDisplayed()
        rule.onNodeWithText("Ostatnie treningi").assertIsDisplayed()
        rule.onNodeWithText("Basen").performClick()
        assert(opened != null)
    }

    @Test
    fun monthDashboardUsesMonthWording() {
        val month = buildDashboard(
            Sample.workouts, Sample.today, Sample.zone, granularity = SummaryGranularity.MONTH,
        )
        rule.setContent { TtTheme(dark = true) { DashboardContent(month, SummaryGranularity.MONTH, {}, {}, {}) } }
        rule.onNodeWithText("Ten miesiąc", substring = true, ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun emptyWorkoutListOffersClearingFilters() {
        var cleared = false
        rule.setContent {
            TtTheme(dark = false) {
                WorkoutsContent(
                    Sample.workoutFilters, emptySet(),
                    Sample.workoutList.copy(workouts = emptyList(), groups = emptyList()),
                    {}, {}, { cleared = true }, {}, {},
                )
            }
        }
        rule.onNodeWithText("Brak treningów dla wybranych filtrów.").assertIsDisplayed()
        rule.onNodeWithText("Wyczyść filtry").performClick()
        assert(cleared)
    }
}
